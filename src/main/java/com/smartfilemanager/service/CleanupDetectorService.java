package com.smartfilemanager.service;

import com.smartfilemanager.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 불필요한 파일 탐지 서비스
 * 다양한 유형의 정리 대상 파일들을 자동으로 찾아냅니다
 */
public class CleanupDetectorService {

    private static final Set<String> TEMP_EXTENSIONS = Set.of(
            "tmp", "temp", "cache", "bak", "old", "~"
    );

    private static final Set<String> INSTALLER_EXTENSIONS = Set.of(
            "exe", "msi", "dmg", "pkg", "deb", "rpm", "app"
    );

    private static final Set<String> LOG_EXTENSIONS = Set.of(
            "log", "txt"
    );

    private static final Set<String> TEMP_FILENAMES = Set.of(
            "thumbs.db", ".ds_store", "desktop.ini", "folder.jpg", "folder.png",
            "albumartsmall.jpg", "albumart.jpg", "ehthumbs.db", "ehthumbs_vista.db"
    );

    private static final Set<String> CACHE_DIRECTORIES = Set.of(
            "cache", "temp", "tmp", "temporary", "recent", "appdata\\local\\temp",
            "application data\\temp", ".cache", ".tmp"
    );

    private final DuplicateDetectorService duplicateDetectorService;

    public CleanupDetectorService() {
        this.duplicateDetectorService = new DuplicateDetectorService();
    }

    /**
     * 파일 목록에서 모든 정리 후보들을 찾습니다
     */
    public List<CleanupCandidate> findCleanupCandidates(List<FileInfo> files) {
        List<CleanupCandidate> candidates = new ArrayList<>();

        System.out.println("[INFO] 불필요한 파일 탐지 시작: " + files.size() + "개 파일 분석");

        try {
            // 1. 임시 파일 탐지
            candidates.addAll(findTempFiles(files));

            // 2. 빈 파일 탐지
            candidates.addAll(findEmptyFiles(files));

            // 3. 중복 파일 탐지 (기존 서비스 활용)
            candidates.addAll(findDuplicateFiles(files));

            // 4. 캐시 파일 탐지
            candidates.addAll(findCacheFiles(files));

            // 5. 로그 파일 탐지
            candidates.addAll(findLogFiles(files));

            // 6. 오래된 설치 파일 탐지
            candidates.addAll(findOldInstallers(files));

            // 7. 백업 파일 탐지
            candidates.addAll(findBackupFiles(files));

            // 8. 대용량 미사용 파일 탐지
            candidates.addAll(findLargeUnusedFiles(files));

        } catch (Exception e) {
            System.err.println("[ERROR] 파일 탐지 중 오류: " + e.getMessage());
        }

        // 중복 제거 및 정렬
        candidates = deduplicateAndSort(candidates);

        // 결과 요약 출력
        printCleanupSummary(candidates);

        return candidates;
    }

    /**
     * 임시 파일 탐지
     */
    private List<CleanupCandidate> findTempFiles(List<FileInfo> files) {
        List<CleanupCandidate> tempFiles = new ArrayList<>();

        for (FileInfo file : files) {
            String fileName = file.getFileName().toLowerCase();
            String extension = file.getFileExtension().toLowerCase();
            String filePath = file.getFilePath().toLowerCase();

            // 확장자 기반 탐지
            if (TEMP_EXTENSIONS.contains(extension)) {
                tempFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.TEMP_FILES,
                        SafetyLevel.SAFE,
                        "임시 파일 확장자: ." + extension
                ));
                continue;
            }

            // 파일명 기반 탐지
            if (TEMP_FILENAMES.contains(fileName)) {
                tempFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.TEMP_FILES,
                        SafetyLevel.SAFE,
                        "시스템 임시 파일: " + fileName
                ));
                continue;
            }

            // 경로 기반 탐지
            if (isInTempDirectory(filePath)) {
                tempFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.TEMP_FILES,
                        SafetyLevel.SAFE,
                        "임시 폴더 내 파일"
                ));
                continue;
            }

            // 패턴 기반 탐지
            if (fileName.startsWith("~$") || fileName.endsWith("~") ||
                    fileName.contains(".tmp.") || fileName.contains("temp")) {
                tempFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.TEMP_FILES,
                        SafetyLevel.LIKELY_SAFE,
                        "임시 파일 패턴"
                ));
            }
        }

        System.out.println("[TEMP] 임시 파일 " + tempFiles.size() + "개 발견");
        return tempFiles;
    }

    /**
     * 빈 파일 및 폴더 탐지
     */
    private List<CleanupCandidate> findEmptyFiles(List<FileInfo> files) {
        List<CleanupCandidate> emptyFiles = new ArrayList<>();

        for (FileInfo file : files) {
            // 0바이트 파일
            if (file.getFileSize() == 0) {
                emptyFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.EMPTY_FILES,
                        SafetyLevel.SAFE,
                        "0바이트 빈 파일"
                ));
            }
        }

        // 빈 폴더 탐지 (추가 검사)
        Set<String> parentDirs = files.stream()
                .map(f -> new File(f.getFilePath()).getParent())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String dirPath : parentDirs) {
            File dir = new File(dirPath);
            if (dir.isDirectory() && isEmptyDirectory(dir)) {
                emptyFiles.add(CleanupCandidate.create(
                        dirPath,
                        CleanupCategory.EMPTY_FILES,
                        SafetyLevel.LIKELY_SAFE,
                        "빈 폴더"
                ));
            }
        }

        System.out.println("[EMPTY] 빈 파일/폴더 " + emptyFiles.size() + "개 발견");
        return emptyFiles;
    }

    /**
     * 중복 파일 탐지 (기존 서비스 활용)
     */
    private List<CleanupCandidate> findDuplicateFiles(List<FileInfo> files) {
        List<CleanupCandidate> duplicates = new ArrayList<>();

        try {
            List<DuplicateGroup> duplicateGroups = duplicateDetectorService.findDuplicates(files);

            for (DuplicateGroup group : duplicateGroups) {
                if (group.getRecommendedDelete() != null) {
                    for (FileInfo fileToDelete : group.getRecommendedDelete()) {
                        SafetyLevel safety = group.getType() == DuplicateType.EXACT ?
                                SafetyLevel.LIKELY_SAFE : SafetyLevel.CAUTION;

                        duplicates.add(CleanupCandidate.create(
                                fileToDelete.getFilePath(),
                                CleanupCategory.DUPLICATE_FILES,
                                safety,
                                group.getType() == DuplicateType.EXACT ?
                                        "정확한 중복 파일" : "유사한 파일 (" +
                                        String.format("%.0f%%", group.getSimilarityScore() * 100) + " 유사)"
                        ));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 중복 파일 탐지 실패: " + e.getMessage());
        }

        System.out.println("[DUPLICATE] 중복 파일 " + duplicates.size() + "개 발견");
        return duplicates;
    }

    /**
     * 캐시 파일 탐지
     */
    private List<CleanupCandidate> findCacheFiles(List<FileInfo> files) {
        List<CleanupCandidate> cacheFiles = new ArrayList<>();

        for (FileInfo file : files) {
            String filePath = file.getFilePath().toLowerCase();
            String fileName = file.getFileName().toLowerCase();

            // 캐시 디렉토리 내 파일
            if (isInCacheDirectory(filePath)) {
                cacheFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.CACHE_FILES,
                        SafetyLevel.SAFE,
                        "캐시 디렉토리 내 파일"
                ));
                continue;
            }

            // 브라우저 캐시 패턴
            if (filePath.contains("appdata\\local\\google\\chrome\\user data\\default\\cache") ||
                    filePath.contains("appdata\\local\\mozilla\\firefox\\profiles") ||
                    filePath.contains("appdata\\local\\microsoft\\edge\\user data\\default\\cache")) {
                cacheFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.CACHE_FILES,
                        SafetyLevel.SAFE,
                        "브라우저 캐시 파일"
                ));
                continue;
            }

            // 캐시 파일 패턴
            if (fileName.contains("cache") || fileName.contains("cached")) {
                cacheFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.CACHE_FILES,
                        SafetyLevel.LIKELY_SAFE,
                        "캐시 파일 패턴"
                ));
            }
        }

        System.out.println("[CACHE] 캐시 파일 " + cacheFiles.size() + "개 발견");
        return cacheFiles;
    }

    /**
     * 로그 파일 탐지
     */
    private List<CleanupCandidate> findLogFiles(List<FileInfo> files) {
        List<CleanupCandidate> logFiles = new ArrayList<>();

        for (FileInfo file : files) {
            String fileName = file.getFileName().toLowerCase();
            String extension = file.getFileExtension().toLowerCase();

            // 로그 파일 확장자
            if (LOG_EXTENSIONS.contains(extension)) {
                // 파일 내용으로 로그 파일인지 확인
                if (isLogFile(file)) {
                    SafetyLevel safety = file.getFileSize() > 50 * 1024 * 1024 ? // 50MB 이상
                            SafetyLevel.LIKELY_SAFE : SafetyLevel.CAUTION;

                    logFiles.add(CleanupCandidate.create(
                            file.getFilePath(),
                            CleanupCategory.LOG_FILES,
                            safety,
                            "로그 파일 (" + file.getFormattedFileSize() + ")"
                    ));
                }
                continue;
            }

            // 로그 파일 패턴
            if (fileName.contains("log") && !fileName.contains("dialog") && !fileName.contains("catalog")) {
                logFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.LOG_FILES,
                        SafetyLevel.CAUTION,
                        "로그 파일 패턴"
                ));
            }
        }

        System.out.println("[LOG] 로그 파일 " + logFiles.size() + "개 발견");
        return logFiles;
    }

    /**
     * 오래된 설치 파일 탐지
     */
    private List<CleanupCandidate> findOldInstallers(List<FileInfo> files) {
        List<CleanupCandidate> installers = new ArrayList<>();

        for (FileInfo file : files) {
            String extension = file.getFileExtension().toLowerCase();
            String fileName = file.getFileName().toLowerCase();

            // 설치 파일 확장자
            if (INSTALLER_EXTENSIONS.contains(extension)) {
                // 30일 이상 된 설치 파일
                if (file.getModifiedDate() != null &&
                        file.getModifiedDate().isBefore(LocalDateTime.now().minusDays(30))) {

                    SafetyLevel safety = isLikelyInstaller(fileName) ?
                            SafetyLevel.CAUTION : SafetyLevel.USER_DECISION;

                    installers.add(CleanupCandidate.create(
                            file.getFilePath(),
                            CleanupCategory.OLD_INSTALLERS,
                            safety,
                            "30일 이상 된 설치 파일"
                    ));
                }
            }
        }

        System.out.println("[INSTALLER] 오래된 설치 파일 " + installers.size() + "개 발견");
        return installers;
    }

    /**
     * 백업 파일 탐지
     */
    private List<CleanupCandidate> findBackupFiles(List<FileInfo> files) {
        List<CleanupCandidate> backupFiles = new ArrayList<>();

        for (FileInfo file : files) {
            String fileName = file.getFileName().toLowerCase();
            String extension = file.getFileExtension().toLowerCase();

            // 백업 파일 패턴
            if (extension.equals("bak") || fileName.endsWith("~") ||
                    fileName.contains("backup") || fileName.contains("백업") ||
                    fileName.startsWith("copy of") || fileName.contains(" - 복사본")) {

                backupFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.BACKUP_FILES,
                        SafetyLevel.CAUTION,
                        "백업 파일 패턴"
                ));
            }
        }

        System.out.println("[BACKUP] 백업 파일 " + backupFiles.size() + "개 발견");
        return backupFiles;
    }

    /**
     * 대용량 미사용 파일 탐지
     */
    private List<CleanupCandidate> findLargeUnusedFiles(List<FileInfo> files) {
        List<CleanupCandidate> largeFiles = new ArrayList<>();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // 90일 기준

        for (FileInfo file : files) {
            // 100MB 이상이고 90일 이상 미접근
            if (file.getFileSize() >= 100 * 1024 * 1024 && // 100MB
                    file.getModifiedDate() != null &&
                    file.getModifiedDate().isBefore(cutoffDate)) {

                largeFiles.add(CleanupCandidate.create(
                        file.getFilePath(),
                        CleanupCategory.LARGE_UNUSED,
                        SafetyLevel.USER_DECISION,
                        "90일 이상 미접근 대용량 파일 (" + file.getFormattedFileSize() + ")"
                ));
            }
        }

        System.out.println("[LARGE] 대용량 미사용 파일 " + largeFiles.size() + "개 발견");
        return largeFiles;
    }

    // 헬퍼 메서드들

    private boolean isInTempDirectory(String filePath) {
        return CACHE_DIRECTORIES.stream()
                .anyMatch(tempDir -> filePath.contains(tempDir.toLowerCase()));
    }

    private boolean isInCacheDirectory(String filePath) {
        return filePath.contains("cache") || filePath.contains("temp") ||
                filePath.contains("appdata\\local\\temp");
    }

    private boolean isEmptyDirectory(File dir) {
        String[] contents = dir.list();
        return contents != null && contents.length == 0;
    }

    private boolean isLogFile(FileInfo file) {
        // 간단한 휴리스틱: 파일명에 날짜나 로그 관련 키워드가 있는지 확인
        String fileName = file.getFileName().toLowerCase();
        return fileName.contains("log") || fileName.matches(".*\\d{4}-\\d{2}-\\d{2}.*") ||
                fileName.contains("error") || fileName.contains("debug") || fileName.contains("trace");
    }

    private boolean isLikelyInstaller(String fileName) {
        return fileName.contains("setup") || fileName.contains("install") ||
                fileName.contains("installer") || fileName.contains("설치");
    }

    /**
     * 중복 제거 및 정렬
     */
    private List<CleanupCandidate> deduplicateAndSort(List<CleanupCandidate> candidates) {
        // 파일 경로 기준으로 중복 제거
        Map<String, CleanupCandidate> uniqueCandidates = new LinkedHashMap<>();

        for (CleanupCandidate candidate : candidates) {
            String path = candidate.getFilePath();
            if (!uniqueCandidates.containsKey(path) ||
                    uniqueCandidates.get(path).getSafetyLevel().getPriority() >
                            candidate.getSafetyLevel().getPriority()) {
                uniqueCandidates.put(path, candidate);
            }
        }

        // 카테고리별, 안전성별, 크기별 정렬
        return uniqueCandidates.values().stream()
                .sorted(Comparator
                        .comparing((CleanupCandidate c) -> c.getCategory().getPriority())
                        .thenComparing(c -> c.getSafetyLevel().getPriority())
                        .thenComparing(CleanupCandidate::getFileSize, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * 정리 결과 요약 출력
     */
    private void printCleanupSummary(List<CleanupCandidate> candidates) {
        if (candidates.isEmpty()) {
            System.out.println("[INFO] 정리할 불필요한 파일이 발견되지 않았습니다.");
            return;
        }

        System.out.println("\n=== 🧹 불필요한 파일 분석 결과 ===");

        // 카테고리별 통계
        Map<CleanupCategory, List<CleanupCandidate>> categoryGroups =
                candidates.stream().collect(Collectors.groupingBy(CleanupCandidate::getCategory));

        long totalSize = candidates.stream().mapToLong(CleanupCandidate::getFileSize).sum();
        long safeSize = candidates.stream()
                .filter(c -> c.getSafetyLevel() == SafetyLevel.SAFE)
                .mapToLong(CleanupCandidate::getFileSize).sum();

        System.out.println("📊 발견된 정리 후보: " + candidates.size() + "개 파일");
        System.out.println("💾 총 절약 가능 용량: " + formatFileSize(totalSize));
        System.out.println("✅ 안전한 삭제 가능: " + formatFileSize(safeSize));

        System.out.println("\n📂 카테고리별 분석:");
        categoryGroups.entrySet().stream()
                .sorted(Map.Entry.<CleanupCategory, List<CleanupCandidate>>comparingByKey(
                        (c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority())))
                .forEach(entry -> {
                    CleanupCategory category = entry.getKey();
                    List<CleanupCandidate> files = entry.getValue();
                    long categorySize = files.stream().mapToLong(CleanupCandidate::getFileSize).sum();

                    System.out.println("  " + category.getCategoryIcon() + " " +
                            category.getDisplayName() + ": " +
                            files.size() + "개 (" + formatFileSize(categorySize) + ")");
                });

        System.out.println("=====================================\n");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}