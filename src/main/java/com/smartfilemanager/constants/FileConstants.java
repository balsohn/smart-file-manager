package com.smartfilemanager.constants;

/**
 * 파일 처리 관련 상수들을 관리하는 클래스
 */
public final class FileConstants {
    
    private FileConstants() {
        // 인스턴스 생성 방지
    }
    
    // 신뢰도 임계값
    public static final class ConfidenceThresholds {
        public static final double HIGH = 0.8;    // 80% 이상
        public static final double MEDIUM = 0.6;  // 60% 이상
        public static final double AI_ANALYZED = 0.9;  // AI 분석 완료 판단 기준
    }
    
    // 파일 카테고리
    public static final class Categories {
        public static final String DOCUMENTS = "Documents";
        public static final String IMAGES = "Images";
        public static final String VIDEOS = "Videos";
        public static final String AUDIO = "Audio";
        public static final String ARCHIVES = "Archives";
        public static final String APPLICATIONS = "Applications";
        public static final String OTHERS = "Others";
        public static final String UNKNOWN = "Unknown";
    }
    
    // 서브 카테고리
    public static final class SubCategories {
        public static final String GENERAL = "General";
    }
    
    // 날짜별 정리 대상 카테고리
    public static final String[] DATE_ORGANIZED_CATEGORIES = {
        Categories.IMAGES,
        Categories.VIDEOS
    };
    
    // 파일 크기 단위
    public static final class FileSizeUnits {
        public static final long BYTE = 1;
        public static final long KB = 1024;
        public static final long MB = 1024 * 1024;
        public static final long GB = 1024 * 1024 * 1024;
    }
    
    // AI 분석 관련
    public static final class AIAnalysis {
        public static final String AI_ANALYZED_KEYWORD = "ai-analyzed";
        public static final String AI_ANALYSIS_PREFIX = "AI 분석:";
        public static final int MIN_AI_KEYWORDS_COUNT = 8;
    }
    
    // 기본 폴더 이름
    public static final class DefaultFolders {
        public static final String ORGANIZED_FOLDER_SUFFIX = "SmartFileManager_Organized";
        public static final String DOWNLOADS = "Downloads";
        public static final String DESKTOP = "Desktop";
    }
    
    // 처리 지연 시간 (밀리초)
    public static final class ProcessingDelays {
        public static final long FILE_SCAN_DELAY = 50;
        public static final long FILE_ORGANIZE_DELAY = 100;
        public static final long UI_MESSAGE_DISPLAY = 3000;  // 3초
    }
    
    // 날짜 포맷
    public static final class DateFormats {
        public static final String TABLE_DATE_FORMAT = "MM-dd HH:mm";
        public static final String FOLDER_MONTH_FORMAT = "%02d-%s";
    }
}