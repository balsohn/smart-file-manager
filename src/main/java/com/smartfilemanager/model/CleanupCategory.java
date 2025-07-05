package com.smartfilemanager.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정리 파일 카테고리를 나타내는 열거형
 * 각 카테고리별로 다른 처리 방식을 적용합니다
 */
@Getter
@RequiredArgsConstructor
public enum CleanupCategory {

    TEMP_FILES("임시 파일", "시스템이나 프로그램이 생성한 임시 파일들",
            "*.tmp, *.temp, Thumbs.db, .DS_Store 등"),

    DUPLICATE_FILES("중복 파일", "동일한 내용을 가진 중복된 파일들",
            "해시값이 동일하거나 파일명이 유사한 파일"),

    EMPTY_FILES("빈 파일", "크기가 0바이트인 빈 파일이나 빈 폴더",
            "0바이트 파일, 내용이 없는 폴더"),

    OLD_INSTALLERS("오래된 설치파일", "이미 설치되었거나 오래된 프로그램 설치파일",
            "*.exe, *.msi, *.dmg 등의 설치 파일"),

    CACHE_FILES("캐시 파일", "브라우저나 프로그램이 생성한 캐시 파일들",
            "브라우저 캐시, 프로그램 캐시 디렉토리"),

    LOG_FILES("로그 파일", "시스템이나 프로그램이 생성한 로그 파일들",
            "*.log, *.txt 형태의 로그 파일"),

    BACKUP_FILES("백업 파일", "자동으로 생성된 백업 파일들",
            "*.bak, *~ 형태의 백업 파일"),

    LARGE_UNUSED("대용량 미사용", "오랫동안 접근하지 않은 대용량 파일들",
            "90일 이상 미접근 + 100MB 이상 파일"),

    OTHER("기타", "기타 정리 대상 파일들",
            "위 카테고리에 해당하지 않는 파일들");

    private final String displayName;       // 화면 표시명
    private final String description;       // 설명
    private final String examples;          // 예시

    /**
     * 카테고리별 기본 안전성 등급 반환
     */
    public SafetyLevel getDefaultSafetyLevel() {
        switch (this) {
            case TEMP_FILES:
            case CACHE_FILES:
            case EMPTY_FILES:
                return SafetyLevel.SAFE;

            case LOG_FILES:
            case DUPLICATE_FILES:
                return SafetyLevel.LIKELY_SAFE;

            case OLD_INSTALLERS:
            case BACKUP_FILES:
                return SafetyLevel.CAUTION;

            case LARGE_UNUSED:
            case OTHER:
                return SafetyLevel.USER_DECISION;

            default:
                return SafetyLevel.USER_DECISION;
        }
    }

    /**
     * 카테고리별 우선순위 반환 (낮을수록 우선)
     */
    public int getPriority() {
        switch (this) {
            case TEMP_FILES: return 1;      // 가장 우선
            case CACHE_FILES: return 2;
            case EMPTY_FILES: return 3;
            case DUPLICATE_FILES: return 4;
            case LOG_FILES: return 5;
            case BACKUP_FILES: return 6;
            case OLD_INSTALLERS: return 7;
            case LARGE_UNUSED: return 8;
            case OTHER: return 9;           // 가장 나중
            default: return 10;
        }
    }

    /**
     * 카테고리별 예상 절약 효과 (상대적 점수)
     */
    public double getExpectedSavingsScore() {
        switch (this) {
            case CACHE_FILES: return 0.9;      // 보통 많은 용량
            case LARGE_UNUSED: return 0.8;     // 큰 파일들
            case DUPLICATE_FILES: return 0.7;  // 중복 제거 효과
            case LOG_FILES: return 0.6;        // 로그가 많이 쌓임
            case OLD_INSTALLERS: return 0.5;   // 가끔 큰 파일
            case BACKUP_FILES: return 0.4;     // 백업 파일들
            case TEMP_FILES: return 0.3;       // 보통 작은 파일들
            case EMPTY_FILES: return 0.1;      // 0바이트
            case OTHER: return 0.2;            // 예측 어려움
            default: return 0.1;
        }
    }

    /**
     * 카테고리별 아이콘 반환
     */
    public String getCategoryIcon() {
        switch (this) {
            case TEMP_FILES: return "🗂️";
            case DUPLICATE_FILES: return "🔄";
            case EMPTY_FILES: return "📭";
            case OLD_INSTALLERS: return "📦";
            case CACHE_FILES: return "💾";
            case LOG_FILES: return "📋";
            case BACKUP_FILES: return "💽";
            case LARGE_UNUSED: return "📊";
            case OTHER: return "📄";
            default: return "📄";
        }
    }

    /**
     * 카테고리명으로 찾기
     */
    public static CleanupCategory fromDisplayName(String displayName) {
        for (CleanupCategory category : values()) {
            if (category.getDisplayName().equals(displayName)) {
                return category;
            }
        }
        return OTHER;
    }
}