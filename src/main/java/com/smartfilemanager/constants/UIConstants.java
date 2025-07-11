package com.smartfilemanager.constants;

/**
 * UI 관련 상수들을 관리하는 클래스
 */
public final class UIConstants {
    
    private UIConstants() {
        // 인스턴스 생성 방지
    }
    
    // 파일 카테고리별 아이콘
    public static final class FileIcons {
        public static final String DOCUMENTS = "📄";
        public static final String IMAGES = "🖼️";
        public static final String VIDEOS = "🎬";
        public static final String AUDIO = "🎵";
        public static final String ARCHIVES = "📦";
        public static final String APPLICATIONS = "⚙️";
        public static final String DEFAULT = "📁";
        public static final String UNKNOWN = "📄";
    }
    
    // 처리 상태별 아이콘
    public static final class StatusIcons {
        public static final String PENDING = "⏳";
        public static final String SCANNING = "🔍";
        public static final String ANALYZED = "✅";
        public static final String ORGANIZING = "📦";
        public static final String ORGANIZED = "🎯";
        public static final String FAILED = "❌";
        public static final String SKIPPED = "⏭️";
        public static final String DEFAULT = "⏸️";
    }
    
    // 상태별 색상
    public static final class StatusColors {
        public static final String PENDING = "#6c757d";
        public static final String SCANNING = "#007bff";
        public static final String ANALYZED = "#17a2b8";
        public static final String ORGANIZING = "#ffc107";
        public static final String ORGANIZED = "#28a745";
        public static final String FAILED = "#dc3545";
        public static final String SKIPPED = "#6f42c1";
        public static final String DEFAULT = "#6c757d";
    }
    
    // 신뢰도별 색상
    public static final class ConfidenceColors {
        public static final String HIGH = "#2e7d32";      // >= 80%
        public static final String MEDIUM = "#f57c00";    // >= 60%
        public static final String LOW = "#d32f2f";       // < 60%
    }
    
    // AI 상태 스타일 클래스
    public static final class AIStatusStyles {
        public static final String ACTIVE = "status-active";
        public static final String INACTIVE = "status-inactive";
        public static final String ERROR = "status-error";
    }
    
    // 기타 UI 아이콘
    public static final class GeneralIcons {
        public static final String AI_ANALYZED = "🤖";
        public static final String MONITORING_ACTIVE = "⚡";
        public static final String FOLDER = "📁";
        public static final String FILE = "📄";
        public static final String SUCCESS = "🎉";
        public static final String WARNING = "⚠️";
        public static final String INFO = "ℹ️";
        public static final String SETTINGS = "⚙️";
        public static final String STATISTICS = "📊";
        public static final String CLEANUP = "🧹";
        public static final String DUPLICATE = "🔄";
        public static final String UNDO = "↩️";
        public static final String ORGANIZE = "📦";
    }
    
    // 테이블 컬럼 너비
    public static final class TableColumnWidths {
        public static final int NAME_COLUMN = 280;
        public static final int NAME_COLUMN_MIN = 200;
        public static final int CATEGORY_COLUMN = 140;
        public static final int SIZE_COLUMN = 90;
        public static final int STATUS_COLUMN = 110;
        public static final int CONFIDENCE_COLUMN = 90;
        public static final int DATE_COLUMN = 130;
    }
}