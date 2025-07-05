package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.DuplicateGroup;
import com.smartfilemanager.model.DuplicateType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 중복 파일 탐지 서비스
 * 해시 기반 정확한 중복과 파일명 기반 유사 파일을 찾습니다
 */
public class DuplicateDetectorService {

    private static final int BUFFER_SIZE = 8192;
    private static final double SIMILARITY_THRESHOLD = 0.7; // 70% 이상 유사하면 중복으로 판단

    /**
     * 파일 목록에서 중복 파일 그룹들을 찾습니다
     */
    public List<DuplicateGroup> findDuplicates(List<FileInfo> files) {
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();

        System.out.println("[INFO] 중복 파일 탐지 시작: " + files.size() + "개 파일 분석");

        // 1단계: 파일 크기별로 1차 필터링
        Map<Long, List<FileInfo>> sizeGroups = groupBySize(files);
        System.out.println("[INFO] 크기별 그룹화 완료: " + sizeGroups.size() + "개 그룹");

        // 2단계: 같은 크기 파일들에 대해 해시 비교로 정확한 중복 찾기
        List<DuplicateGroup> exactDuplicates = findExactDuplicates(sizeGroups);
        duplicateGroups.addAll(exactDuplicates);

        // 3단계: 파일명 유사도 기반으로 유사 파일 찾기
        List<DuplicateGroup> similarFiles = findSimilarFiles(files, exactDuplicates);
        duplicateGroups.addAll(similarFiles);

        // 4단계: 각 그룹에 대해 추천 시스템 적용
        duplicateGroups.forEach(this::generateRecommendations);

        // 결과 요약 출력
        printDuplicateAnalysisSummary(duplicateGroups);

        return duplicateGroups;
    }

    /**
     * 파일 크기별로 그룹화
     */
    private Map<Long, List<FileInfo>> groupBySize(List<FileInfo> files) {
        return files.stream()
                .filter(file -> file.getFileSize() > 0) // 0바이트 파일 제외
                .collect(Collectors.groupingBy(FileInfo::getFileSize));
    }

    /**
     * 해시값을 이용한 정확한 중복 파일 찾기
     */
    private List<DuplicateGroup> findExactDuplicates(Map<Long, List<FileInfo>> sizeGroups) {
        List<DuplicateGroup> exactGroups = new ArrayList<>();

        for (Map.Entry<Long, List<FileInfo>> entry : sizeGroups.entrySet()) {
            List<FileInfo> sameSize = entry.getValue();

            // 같은 크기의 파일이 2개 이상인 경우만 해시 계산
            if (sameSize.size() > 1) {
                Map<String, List<FileInfo>> hashGroups = groupByHash(sameSize);

                // 같은 해시값을 가진 파일들이 2개 이상이면 중복
                for (Map.Entry<String, List<FileInfo>> hashEntry : hashGroups.entrySet()) {
                    List<FileInfo> duplicates = hashEntry.getValue();

                    if (duplicates.size() > 1) {
                        DuplicateGroup group = DuplicateGroup.createExact(duplicates, hashEntry.getKey());
                        group.setDescription("파일 내용이 완전히 동일한 중복 파일");
                        exactGroups.add(group);

                        System.out.println("[EXACT] " + duplicates.size() + "개 중복 파일 발견: " +
                                duplicates.get(0).getFileName());
                    }
                }
            }
        }

        return exactGroups;
    }

    /**
     * 파일들을 해시값별로 그룹화
     */
    private Map<String, List<FileInfo>> groupByHash(List<FileInfo> files) {
        Map<String, List<FileInfo>> hashGroups = new HashMap<>();

        for (FileInfo file : files) {
            try {
                String hash = calculateFileHash(file.getFilePath());
                hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
            } catch (Exception e) {
                System.err.println("[ERROR] 해시 계산 실패: " + file.getFileName() + " - " + e.getMessage());
            }
        }

        return hashGroups;
    }

    /**
     * 파일의 MD5 해시값 계산
     */
    private String calculateFileHash(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
        }

        byte[] hash = md5.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /**
     * 파일명 유사도 기반 유사 파일 찾기
     */
    private List<DuplicateGroup> findSimilarFiles(List<FileInfo> files, List<DuplicateGroup> exactDuplicates) {
        List<DuplicateGroup> similarGroups = new ArrayList<>();

        // 이미 정확한 중복으로 분류된 파일들은 제외
        Set<String> exactDuplicateFiles = exactDuplicates.stream()
                .flatMap(group -> group.getFiles().stream())
                .map(FileInfo::getFilePath)
                .collect(Collectors.toSet());

        List<FileInfo> remainingFiles = files.stream()
                .filter(file -> !exactDuplicateFiles.contains(file.getFilePath()))
                .collect(Collectors.toList());

        // 파일명 유사도 비교
        for (int i = 0; i < remainingFiles.size(); i++) {
            for (int j = i + 1; j < remainingFiles.size(); j++) {
                FileInfo file1 = remainingFiles.get(i);
                FileInfo file2 = remainingFiles.get(j);

                double similarity = calculateNameSimilarity(file1.getFileName(), file2.getFileName());

                // 유사도가 임계값 이상이고, 확장자가 같은 경우
                if (similarity >= SIMILARITY_THRESHOLD &&
                        file1.getFileExtension().equals(file2.getFileExtension())) {

                    // 이미 다른 그룹에 포함되지 않은 경우만
                    if (!isAlreadyInSimilarGroup(file1, file2, similarGroups)) {
                        DuplicateGroup group = DuplicateGroup.createSimilar(Arrays.asList(file1, file2), similarity);
                        group.setDescription(String.format("파일명이 %.0f%% 유사한 파일", similarity * 100));
                        similarGroups.add(group);

                        System.out.println("[SIMILAR] 유사 파일 발견: " + file1.getFileName() +
                                " ↔ " + file2.getFileName() + " (유사도: " +
                                String.format("%.0f%%", similarity * 100) + ")");
                    }
                }
            }
        }

        return similarGroups;
    }

    /**
     * 파일명 유사도 계산 (레벤슈타인 거리 기반)
     */
    private double calculateNameSimilarity(String name1, String name2) {
        // 확장자 제거 및 정규화
        String cleanName1 = cleanFileName(name1);
        String cleanName2 = cleanFileName(name2);

        // 레벤슈타인 거리 계산
        int distance = levenshteinDistance(cleanName1, cleanName2);
        int maxLength = Math.max(cleanName1.length(), cleanName2.length());

        // 유사도 = 1 - (거리 / 최대길이)
        return maxLength == 0 ? 0.0 : 1.0 - (double) distance / maxLength;
    }

    /**
     * 파일명 정리 (확장자 제거, 특수문자 정리, 소문자 변환)
     */
    private String cleanFileName(String fileName) {
        // 확장자 제거
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExt = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;

        // 특수문자를 공백으로 변환하고 소문자로 변환
        return nameWithoutExt.toLowerCase()
                .replaceAll("[^a-zA-Z0-9가-힣]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 레벤슈타인 거리 계산
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        // 초기화
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        // 동적 프로그래밍
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        dp[i - 1][j] + 1,      // 삭제
                        Math.min(
                                dp[i][j - 1] + 1,  // 삽입
                                dp[i - 1][j - 1] + cost // 교체
                        )
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 이미 유사 그룹에 포함되어 있는지 확인
     */
    private boolean isAlreadyInSimilarGroup(FileInfo file1, FileInfo file2, List<DuplicateGroup> groups) {
        return groups.stream()
                .anyMatch(group -> group.getFiles().contains(file1) && group.getFiles().contains(file2));
    }

    /**
     * 추천 시스템: 어떤 파일을 보관하고 어떤 파일을 삭제할지 추천
     */
    private void generateRecommendations(DuplicateGroup group) {
        List<FileInfo> files = group.getFiles();
        if (files.size() < 2) {
            return;
        }

        // 추천 우선순위:
        // 1. 가장 최근 수정된 파일
        // 2. 파일명이 가장 명확한 파일 (숫자나 특수문자가 적은)
        // 3. 가장 큰 파일 (고화질, 고품질일 가능성)
        // 4. 가장 접근하기 쉬운 위치의 파일

        FileInfo recommended = files.stream()
                .max(Comparator
                        .comparing(FileInfo::getModifiedDate)           // 최근 수정일
                        .thenComparing(f -> getFileNameQuality(f.getFileName())) // 파일명 품질
                        .thenComparing(FileInfo::getFileSize)           // 파일 크기
                        .thenComparing(f -> getLocationScore(f.getFilePath()))   // 위치 점수
                )
                .orElse(files.get(0));

        group.setRecommendedKeep(recommended);

        // 삭제 추천 목록 (보관 파일 제외)
        List<FileInfo> toDelete = files.stream()
                .filter(f -> !f.equals(recommended))
                .collect(Collectors.toList());
        group.setRecommendedDelete(toDelete);

        // 추천 이유 생성
        group.setRecommendationReason(generateRecommendationReason(recommended, files));
    }

    /**
     * 파일명 품질 점수 계산
     */
    private int getFileNameQuality(String fileName) {
        int score = 0;

        // 의미있는 단어가 포함되어 있으면 점수 증가
        if (fileName.matches(".*[a-zA-Z가-힣]{3,}.*")) {
            score += 10;
        }

        // 숫자나 특수문자가 많으면 점수 감소 (copy, temp 등의 표시)
        long digitCount = fileName.chars().filter(Character::isDigit).count();
        score -= digitCount / 2;

        // 복사본을 나타내는 단어들이 있으면 점수 감소
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("copy") || lowerName.contains("복사") ||
                lowerName.contains("temp") || lowerName.contains("임시") ||
                lowerName.contains("backup") || lowerName.contains("백업")) {
            score -= 20;
        }

        // 적당한 길이의 파일명이 좋음
        if (fileName.length() > 10 && fileName.length() < 50) {
            score += 5;
        }

        return score;
    }

    /**
     * 파일 위치 점수 계산 (접근하기 쉬운 위치일수록 높은 점수)
     */
    private int getLocationScore(String filePath) {
        int score = 0;
        String lowerPath = filePath.toLowerCase();

        // 바탕화면이나 문서 폴더는 높은 점수
        if (lowerPath.contains("desktop") || lowerPath.contains("바탕화면")) {
            score += 10;
        }
        if (lowerPath.contains("documents") || lowerPath.contains("문서")) {
            score += 8;
        }

        // 다운로드 폴더는 중간 점수
        if (lowerPath.contains("downloads") || lowerPath.contains("다운로드")) {
            score += 5;
        }

        // 임시 폴더나 휴지통은 낮은 점수
        if (lowerPath.contains("temp") || lowerPath.contains("임시") ||
                lowerPath.contains("trash") || lowerPath.contains("휴지통")) {
            score -= 10;
        }

        // 경로가 짧을수록 (루트에 가까울수록) 높은 점수
        long separatorCount = filePath.chars().filter(ch -> ch == File.separatorChar).count();
        score += Math.max(0, 10 - (int) separatorCount);

        return score;
    }

    /**
     * 추천 이유 생성
     */
    private String generateRecommendationReason(FileInfo recommended, List<FileInfo> allFiles) {
        List<String> reasons = new ArrayList<>();

        // 최신 파일인지 확인
        boolean isNewest = allFiles.stream()
                .allMatch(f -> !f.getModifiedDate().isAfter(recommended.getModifiedDate()));
        if (isNewest) {
            reasons.add("가장 최근 파일");
        }

        // 가장 큰 파일인지 확인
        boolean isLargest = allFiles.stream()
                .allMatch(f -> f.getFileSize() <= recommended.getFileSize());
        if (isLargest && recommended.getFileSize() > 0) {
            reasons.add("가장 큰 파일");
        }

        // 파일명이 가장 적절한지 확인
        boolean hasBestName = allFiles.stream()
                .allMatch(f -> getFileNameQuality(f.getFileName()) <= getFileNameQuality(recommended.getFileName()));
        if (hasBestName) {
            reasons.add("파일명이 적절함");
        }

        // 좋은 위치에 있는지 확인
        boolean hasBestLocation = allFiles.stream()
                .allMatch(f -> getLocationScore(f.getFilePath()) <= getLocationScore(recommended.getFilePath()));
        if (hasBestLocation) {
            reasons.add("접근하기 좋은 위치");
        }

        return reasons.isEmpty() ? "임의 선택" : String.join(", ", reasons);
    }

    /**
     * 중복 파일 분석 결과 요약 출력
     */
    private void printDuplicateAnalysisSummary(List<DuplicateGroup> groups) {
        if (groups.isEmpty()) {
            System.out.println("[INFO] 중복 파일이 발견되지 않았습니다.");
            return;
        }

        System.out.println("\n=== 🔄 중복 파일 분석 결과 ===");

        long exactGroups = groups.stream().filter(g -> g.getType() == DuplicateType.EXACT).count();
        long similarGroups = groups.stream().filter(g -> g.getType() == DuplicateType.SIMILAR).count();

        System.out.println("📊 발견된 중복 그룹: " + groups.size() + "개");
        System.out.println("  • 정확한 중복: " + exactGroups + "개 그룹");
        System.out.println("  • 유사한 파일: " + similarGroups + "개 그룹");

        long totalFiles = groups.stream().mapToLong(g -> g.getFiles().size()).sum();
        long totalSavings = groups.stream().mapToLong(DuplicateGroup::getDuplicateSize).sum();

        System.out.println("📁 중복 파일 개수: " + totalFiles + "개");
        System.out.println("💾 절약 가능 용량: " + formatFileSize(totalSavings));

        System.out.println("\n🎯 상위 중복 그룹:");
        groups.stream()
                .sorted((g1, g2) -> Long.compare(g2.getDuplicateSize(), g1.getDuplicateSize()))
                .limit(5)
                .forEach(group -> {
                    System.out.println("  • " + group.getSummary());
                    if (group.hasRecommendation()) {
                        System.out.println("    추천: " + group.getRecommendedKeep().getFileName() +
                                " (" + group.getRecommendationReason() + ")");
                    }
                });

        System.out.println("=============================\n");
    }

    /**
     * 파일 크기 포맷팅 유틸리티
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}