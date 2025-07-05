package com.smartfilemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정리 후보 파일을 나타내는 모델 클래스
 * 불필요한 파일들을 안전성 등급과 함께 관리합니다
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleanupCandidate {

    private String filePath;                    // 파일 경로
    private String fileName;                    // 파일명
    private long fileSize;                      // 파일 크기
    private LocalDateTime lastModified;         // 마지막 수정일
    private LocalDateTime lastAccessed;         // 마지막 접근일 (가능한 경우)

    // 정리 분류
    private CleanupCategory category;           // 정리 카테고리
    private SafetyLevel safetyLevel;           // 안전성 등급
    private String reason;                      // 정리 추천 이유
    private double confidenceScore;             // 신뢰도 점수 (0.0 ~ 1.0)

    // 메타데이터
    private String description;                 // 설명
    private List<String> tags;                 // 태그들 (예: "temp", "duplicate", "old")
    private boolean isDirectory;               // 디렉토리 여부
    private long childCount;                   // 하위 파일 개수 (디렉토리인 경우)

    // 상태
    private boolean isSelected;                // 삭제 선택 여부
    private boolean isProcessed;               // 처리 완료 여부
    private String processResult;              // 처리 결과

    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 포맷
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    /**
     * 안전성 등급에 따른 색상 코드 반환
     */
    public String getSafetyColor() {
        if (safetyLevel == null) return "#6c757d";

        switch (safetyLevel) {
            case SAFE: return "#28a745";        // 초록색 - 안전
            case LIKELY_SAFE: return "#17a2b8"; // 파란색 - 아마 안전
            case CAUTION: return "#ffc107";     // 노란색 - 주의
            case USER_DECISION: return "#fd7e14"; // 주황색 - 사용자 판단
            default: return "#6c757d";          // 회색 - 알 수 없음
        }
    }

    /**
     * 안전성 등급 아이콘 반환
     */
    public String getSafetyIcon() {
        if (safetyLevel == null) return "❓";

        switch (safetyLevel) {
            case SAFE: return "✅";
            case LIKELY_SAFE: return "🟢";
            case CAUTION: return "⚠️";
            case USER_DECISION: return "❓";
            default: return "❓";
        }
    }

    /**
     * 카테고리 아이콘 반환
     */
    public String getCategoryIcon() {
        if (category == null) return "📄";

        switch (category) {
            case TEMP_FILES: return "🗂️";
            case DUPLICATE_FILES: return "🔄";
            case EMPTY_FILES: return "📭";
            case OLD_INSTALLERS: return "📦";
            case CACHE_FILES: return "💾";
            case LOG_FILES: return "📋";
            case BACKUP_FILES: return "💽";
            case LARGE_UNUSED: return "📊";
            default: return "📄";
        }
    }

    /**
     * 간단한 요약 정보 반환
     */
    public String getSummary() {
        return String.format("%s %s (%s) - %s",
                getSafetyIcon(),
                fileName,
                getFormattedFileSize(),
                reason != null ? reason : category.getDisplayName());
    }

    /**
     * 파일이 오래되었는지 확인 (30일 이상)
     */
    public boolean isOldFile() {
        if (lastModified == null) return false;
        return lastModified.isBefore(LocalDateTime.now().minusDays(30));
    }

    /**
     * 파일이 매우 오래되었는지 확인 (90일 이상)
     */
    public boolean isVeryOldFile() {
        if (lastModified == null) return false;
        return lastModified.isBefore(LocalDateTime.now().minusDays(90));
    }

    /**
     * 대용량 파일인지 확인 (100MB 이상)
     */
    public boolean isLargeFile() {
        return fileSize >= 100 * 1024 * 1024; // 100MB
    }

    /**
     * 임시 파일인지 확인
     */
    public boolean isTempFile() {
        return category == CleanupCategory.TEMP_FILES;
    }

    /**
     * 안전하게 삭제할 수 있는지 확인
     */
    public boolean isSafeToDelete() {
        return safetyLevel == SafetyLevel.SAFE || safetyLevel == SafetyLevel.LIKELY_SAFE;
    }

    /**
     * 정리 후보 생성 팩토리 메서드
     */
    public static CleanupCandidate create(String filePath, CleanupCategory category,
                                          SafetyLevel safetyLevel, String reason) {
        java.io.File file = new java.io.File(filePath);

        return CleanupCandidate.builder()
                .filePath(filePath)
                .fileName(file.getName())
                .fileSize(file.length())
                .lastModified(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.lastModified()),
                        java.time.ZoneId.systemDefault()))
                .category(category)
                .safetyLevel(safetyLevel)
                .reason(reason)
                .isDirectory(file.isDirectory())
                .isSelected(safetyLevel == SafetyLevel.SAFE) // 안전한 파일은 기본 선택
                .isProcessed(false)
                .confidenceScore(calculateConfidenceScore(category, safetyLevel))
                .build();
    }

    /**
     * 신뢰도 점수 계산
     */
    private static double calculateConfidenceScore(CleanupCategory category, SafetyLevel safetyLevel) {
        double baseScore = 0.5;

        // 카테고리별 기본 신뢰도
        switch (category) {
            case TEMP_FILES: baseScore = 0.9; break;
            case CACHE_FILES: baseScore = 0.8; break;
            case EMPTY_FILES: baseScore = 0.9; break;
            case DUPLICATE_FILES: baseScore = 0.7; break;
            case OLD_INSTALLERS: baseScore = 0.6; break;
            case LOG_FILES: baseScore = 0.7; break;
            case BACKUP_FILES: baseScore = 0.5; break;
            case LARGE_UNUSED: baseScore = 0.4; break;
            default: baseScore = 0.3; break;
        }

        // 안전성 등급에 따른 조정
        switch (safetyLevel) {
            case SAFE: return Math.min(baseScore + 0.1, 1.0);
            case LIKELY_SAFE: return baseScore;
            case CAUTION: return Math.max(baseScore - 0.2, 0.1);
            case USER_DECISION: return Math.max(baseScore - 0.3, 0.1);
            default: return 0.1;
        }
    }
}