package com.smartfilemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 중복 파일 그룹을 나타내는 모델 클래스
 * 동일하거나 유사한 파일들을 그룹으로 관리합니다
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateGroup {

    private String groupId;                    // 그룹 고유 ID
    private List<FileInfo> files;              // 중복된 파일들
    private DuplicateType type;                // 중복 타입 (정확/유사)
    private double similarityScore;            // 유사도 점수 (0.0 ~ 1.0)
    private LocalDateTime detectedAt;          // 탐지 시간

    // 추천 시스템
    private FileInfo recommendedKeep;          // 보관할 파일 추천
    private String recommendationReason;      // 추천 이유
    private List<FileInfo> recommendedDelete; // 삭제할 파일들

    // 통계 정보
    private long totalSize;                    // 그룹 총 크기
    private long duplicateSize;               // 중복으로 낭비되는 크기
    private int fileCount;                    // 파일 개수

    // 메타데이터
    private String hashValue;                 // 파일 해시값 (정확한 중복인 경우)
    private String description;               // 그룹 설명

    /**
     * 그룹 생성 시 자동으로 통계 계산
     */
    public void calculateStatistics() {
        if (files == null || files.isEmpty()) {
            return;
        }

        this.fileCount = files.size();
        this.totalSize = files.stream().mapToLong(FileInfo::getFileSize).sum();

        // 중복 크기 = 전체 크기 - 보관할 파일 1개 크기
        if (!files.isEmpty()) {
            long singleFileSize = files.get(0).getFileSize();
            this.duplicateSize = totalSize - singleFileSize;
        }

        // 그룹 ID가 없으면 생성
        if (groupId == null) {
            this.groupId = UUID.randomUUID().toString();
        }

        // 탐지 시간이 없으면 현재 시간으로 설정
        if (detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }
    }

    /**
     * 용량 절약 효과 포맷팅
     */
    public String getFormattedSavings() {
        return formatFileSize(duplicateSize);
    }

    /**
     * 전체 크기 포맷팅
     */
    public String getFormattedTotalSize() {
        return formatFileSize(totalSize);
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

    /**
     * 그룹 요약 정보 반환
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        if (type == DuplicateType.EXACT) {
            summary.append("🔄 정확한 중복: ");
        } else {
            summary.append("🔍 유사한 파일: ");
        }

        summary.append(fileCount).append("개 파일, ");
        summary.append("절약 가능: ").append(getFormattedSavings());

        if (type == DuplicateType.SIMILAR) {
            summary.append(" (유사도: ").append(String.format("%.0f%%", similarityScore * 100)).append(")");
        }

        return summary.toString();
    }

    /**
     * 추천 파일이 있는지 확인
     */
    public boolean hasRecommendation() {
        return recommendedKeep != null;
    }

    /**
     * 중복 그룹이 유효한지 확인
     */
    public boolean isValid() {
        return files != null && files.size() >= 2;
    }

    /**
     * 파일명들을 간단한 문자열로 반환
     */
    public String getFileNames() {
        if (files == null || files.isEmpty()) {
            return "";
        }

        return files.stream()
                .map(FileInfo::getFileName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * 기본 생성자로 그룹 생성
     */
    public static DuplicateGroup create(List<FileInfo> files, DuplicateType type) {
        DuplicateGroup group = DuplicateGroup.builder()
                .files(files)
                .type(type)
                .build();

        group.calculateStatistics();
        return group;
    }

    /**
     * 정확한 중복 그룹 생성
     */
    public static DuplicateGroup createExact(List<FileInfo> files, String hashValue) {
        DuplicateGroup group = create(files, DuplicateType.EXACT);
        group.setHashValue(hashValue);
        group.setSimilarityScore(1.0);
        return group;
    }

    /**
     * 유사 파일 그룹 생성
     */
    public static DuplicateGroup createSimilar(List<FileInfo> files, double similarityScore) {
        DuplicateGroup group = create(files, DuplicateType.SIMILAR);
        group.setSimilarityScore(similarityScore);
        return group;
    }
}