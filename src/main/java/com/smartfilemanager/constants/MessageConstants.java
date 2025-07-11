package com.smartfilemanager.constants;

/**
 * 사용자 인터페이스 메시지 상수들을 관리하는 클래스
 */
public final class MessageConstants {
    
    private MessageConstants() {
        // 인스턴스 생성 방지
    }
    
    // 상태 메시지
    public static final class StatusMessages {
        public static final String SCANNING_FILES = "파일을 스캔하고 있습니다...";
        public static final String SCAN_COMPLETE = "스캔 완료: %d개 파일 발견";
        public static final String SCAN_FAILED = "스캔 실패: %s";
        public static final String ORGANIZING_FILES = "파일 정리를 시작합니다...";
        public static final String ORGANIZE_COMPLETE = "정리 완료: %d개 성공, %d개 실패";
        public static final String ORGANIZE_FAILED = "정리 실패: %s";
        public static final String ANALYZING_DUPLICATES = "🔍 중복 파일을 분석하고 있습니다...";
        public static final String ANALYZING_CLEANUP = "🧹 불필요한 파일을 분석하고 있습니다...";
        public static final String AI_BATCH_ANALYSIS_START = "AI 배치 분석을 시작합니다... 🤖";
        public static final String AI_BATCH_ANALYSIS_COMPLETE = "AI 배치 분석 완료! 🎉";
    }
    
    // 다이얼로그 제목
    public static final class DialogTitles {
        public static final String SELECT_FOLDER = "정리할 폴더 선택";
        public static final String SELECT_TARGET_FOLDER = "📦 정리된 파일을 저장할 폴더 선택";
        public static final String ORGANIZE_FILES = "📦 파일 정리";
        public static final String UNDO_FILES = "↩️ 파일 되돌리기";
        public static final String NO_FILES = "📋 파일 없음";
        public static final String NO_ORGANIZE_FILES = "📋 정리할 파일 없음";
        public static final String NO_UNDO_FILES = "↩️ 되돌릴 파일 없음";
        public static final String DUPLICATES_FOUND = "🔄 중복 파일 발견!";
        public static final String NO_DUPLICATES = "🎉 중복 파일 없음";
        public static final String CLEANUP_FOUND = "🧹 불필요한 파일 발견!";
        public static final String NO_CLEANUP = "🎉 불필요한 파일 없음";
        public static final String AI_UNAVAILABLE = "AI 분석 불가";
        public static final String NO_AI_FILES = "분석할 파일 없음";
        public static final String AI_BATCH_ANALYSIS = "AI 배치 분석";
        public static final String MONITORING_START = "모니터링 시작";
        public static final String SELECT_MONITORING_FOLDER = "모니터링할 폴더 선택";
        public static final String SETTINGS = "⚙️ Smart File Manager - 설정";
        public static final String STATISTICS = "📊 파일 정리 통계";
        public static final String ERROR = "❌ 오류";
        public static final String COMPLETE = "완료";
        public static final String FAILED = "실패";
    }
    
    // 다이얼로그 메시지
    public static final class DialogMessages {
        public static final String SCAN_FIRST = "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.";
        public static final String SCAN_FILES_FIRST = "먼저 폴더를 스캔해주세요.";
        public static final String ORGANIZE_CONFIRM = "📦 %d개 파일을 정리할 준비가 되었습니다.\n📏 총 크기: %s\n\n❓ 계속하시겠습니까?";
        public static final String UNDO_CONFIRM = "↩️ %d개의 정리된 파일을 원래 위치로 되돌리시겠습니까?\n📏 총 크기: %s\n\n❓ 계속하시겠습니까?";
        public static final String ORGANIZE_SUCCESS = "🎉 %s가 완료되었습니다!\n✅ 성공: %d개 파일";
        public static final String NO_ORGANIZED_FILES = "정리된 파일이 없습니다.";
        public static final String DUPLICATES_RESULT = "%d개의 중복 그룹을 발견했습니다.";
        public static final String NO_DUPLICATES_FOUND = "분석 결과 중복된 파일을 찾지 못했습니다.";
        public static final String CLEANUP_RESULT = "%d개의 정리 후보를 발견했습니다.";
        public static final String NO_CLEANUP_FOUND = "분석 결과 정리할 불필요한 파일을 찾지 못했습니다.";
        public static final String AI_NOT_ENABLED = "AI 분석이 활성화되지 않았습니다.";
        public static final String NO_AI_REANALYSIS = "AI로 재분석할 파일이 없습니다.";
        public static final String AI_ANALYSIS_CONFIRM = "%d개 파일을 AI로 재분석하시겠습니까?\n이 작업은 OpenAI API를 사용하며 비용이 발생할 수 있습니다.";
        public static final String MONITORING_STARTED = "실시간 폴더 모니터링이 시작되었습니다.";
        public static final String SETTINGS_OPEN_ERROR = "설정 창을 열 수 없습니다:\n%s";
        public static final String STATISTICS_OPEN_ERROR = "통계 창을 열 수 없습니다: %s";
        public static final String THEME_CHANGE_ERROR = "테마 변경 중 오류가 발생했습니다: %s";
        public static final String AI_SUMMARY_EMPTY = "아직 AI로 분석된 파일이 없습니다.\n'AI 분석' 버튼을 사용해서 파일을 분석해보세요.";
    }
    
    // UI 레이블
    public static final class UILabels {
        public static final String AI_ACTIVE = "🤖 AI 활성";
        public static final String AI_INACTIVE = "AI 비활성";
        public static final String MONITORING_STOP = "⏹️ 모니터링 중지";
        public static final String MONITORING_START = "⚡ 모니터링 시작";
        public static final String AI_ANALYZING = "AI 분석 중...";
        public static final String AI_ANALYZE = "🤖 AI 분석";
        public static final String FILE_DETECTED = "감지됨: %s";
        public static final String FILE_AUTO_ORGANIZED = "파일 자동 정리: %s → %s";
        public static final String NEW_FILE_DETECTED = "새 파일 감지: %s";
    }
    
    // 통계 레이블
    public static final class StatisticsLabels {
        public static final String EMPTY_STATS = "0 files";
        public static final String STATS_FORMAT = "%d files (%s)";
        public static final String STATS_WITH_AI = "%d files (%s) • %d개 AI 분석됨";
        public static final String STATISTICS_FORMAT = "분석된 파일: %d개 | 정리된 파일: %d개 | 총 크기: %s";
    }
    
    // 진행 상황 메시지
    public static final class ProgressMessages {
        public static final String FILES_PROCESSED = "%d / %d 파일 처리됨";
        public static final String SCAN_COMPLETE = "스캔이 성공적으로 완료되었습니다";
        public static final String ORGANIZE_COMPLETE = "정리가 성공적으로 완료되었습니다";
        public static final String SCAN_FAILED = "스캔 실패";
        public static final String ORGANIZE_FAILED = "정리 실패";
        public static final String PROCESSING = "처리 중: %s";
        public static final String ORGANIZING = "정리 중: %s";
        public static final String ANALYZING = "분석 중: %s";
    }
    
    // 테마 메시지
    public static final class ThemeMessages {
        public static final String LIGHT_THEME_APPLIED = "라이트 테마가 적용되었습니다 ☀️";
        public static final String DARK_THEME_APPLIED = "다크 테마가 적용되었습니다 🌙";
    }
    
    // AI 요약 메시지
    public static final class AISummaryMessages {
        public static final String TITLE = "🤖 AI 분석 결과 요약";
        public static final String ANALYZED_FILES = "📊 분석된 파일: %d개";
        public static final String TOTAL_FILES = "📈 전체 파일: %d개";
    }
}