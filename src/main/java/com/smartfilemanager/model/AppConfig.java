package com.smartfilemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 애플리케이션 설정을 저장하는 모델 클래스
 * 사용자의 선호도와 동작 방식을 커스터마이징할 수 있습니다
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    // 기본 경로 설정
    private String defaultScanFolder;           // 기본 스캔 폴더 (Downloads)
    private String organizationRootFolder;      // 정리된 파일 저장 폴더

    // 자동 기능 설정
    private boolean autoOrganizeEnabled;        // 자동 정리 활성화
    private boolean realTimeMonitoring;         // 실시간 폴더 감시
    private boolean showNotifications;          // 알림 표시

    // 분류 및 정리 설정
    private boolean organizeByDate;             // 날짜별 세부 분류
    private boolean createSubfolders;           // 서브카테고리 폴더 생성
    private boolean backupBeforeOrganizing;    // 정리 전 백업 생성

    // 중복 파일 설정
    private boolean enableDuplicateDetection;  // 중복 파일 탐지 활성화
    private boolean autoResolveDuplicates;     // 자동 중복 해결
    private String duplicateResolutionStrategy; // "KEEP_NEWEST", "KEEP_LARGEST", "ASK_USER"

    // 파일 크기 및 성능 설정
    private int maxFileSizeForAnalysis;         // 분석할 최대 파일 크기 (MB)
    private int monitoringInterval;             // 폴더 감시 간격 (초)
    private int maxFileCount;                   // 한 번에 처리할 최대 파일 수

    // UI 설정
    private boolean minimizeToTray;             // 시스템 트레이로 최소화
    private boolean startWithWindows;          // Windows 시작 시 자동 실행
    private String language;                    // 언어 설정 ("ko", "en")
    private String theme;                       // 테마 설정 ("light", "dark")

    // 고급 설정
    private boolean enableContentAnalysis;      // 파일 내용 분석 활성화
    private boolean enableAIAnalysis;          // AI 분석 활성화 (향후 확장)
    private String aiApiKey;                   // AI API 키 (선택사항)
    private boolean debugMode;                 // 디버그 모드

    /**
     * AI 분석 관련 설정
     */
    private String aiModel;                    // AI 모델명 (gpt-3.5-turbo, gpt-4 등)

    /**
     * 커스텀 규칙 설정
     */
    private boolean useCustomRules;            // 커스텀 규칙 사용 여부
    private String customRulesFilePath;        // 커스텀 규칙 파일 경로




    /**
     * 기본 설정값으로 AppConfig 생성
     */
    public static AppConfig createDefault() {
        String userHome = System.getProperty("user.home");

        return AppConfig.builder()
                // 기본 경로
                .defaultScanFolder(userHome + "\\Downloads")
                .organizationRootFolder(userHome + "\\Desktop\\SmartFileManager_Organized")

                // 자동 기능 (보수적 기본값 - 안전성 우선)
                .autoOrganizeEnabled(false)          // 기본적으로 비활성화
                .realTimeMonitoring(false)           // 자동 정리가 꺼져있으면 감시도 꺼짐
                .showNotifications(true)             // 알림은 활성화

                // 분류 설정 (사용자 친화적)
                .organizeByDate(true)                // 날짜별 정리 활성화
                .createSubfolders(true)              // 서브폴더 생성 활성화
                .backupBeforeOrganizing(false)       // 백업은 기본적으로 비활성화 (속도 우선)

                // 중복 파일 설정
                .enableDuplicateDetection(true)      // 중복 탐지는 활성화
                .autoResolveDuplicates(false)        // 자동 해결은 비활성화 (안전성)
                .duplicateResolutionStrategy("ASK_USER")  // 사용자에게 물어보기

                // 성능 설정 (합리적 기본값)
                .maxFileSizeForAnalysis(100)         // 100MB
                .monitoringInterval(5)               // 5초
                .maxFileCount(1000)                  // 1000개 파일

                // UI 설정
                .minimizeToTray(false)               // 트레이 최소화 비활성화
                .startWithWindows(false)             // 자동 시작 비활성화
                .language("ko")                      // 한국어
                .theme("light")                      // 밝은 테마

                // 고급 설정 (기본적으로 보수적)
                .enableContentAnalysis(true)        // 내용 분석은 활성화
                .enableAIAnalysis(false)             // AI 분석은 비활성화 (API 키 필요)
                .debugMode(false)                    // 디버그 모드 비활성화

                // AI
                .enableAIAnalysis(false)             // AI 분석은 비활성화 (API 키 필요)
                .aiApiKey(null)                      // API 키 없음
                .aiModel("gpt-3.5-turbo")           // 기본 AI 모델

                // 커스텀 규칙
                .useCustomRules(true)                // 커스텀 규칙 기본 활성화
                .customRulesFilePath(userHome + "\\.smartfilemanager\\custom-rules.json")  // 기본 규칙 파일 경로

                .build();
    }

    /**
     * 설정 유효성 검증
     */
    public boolean isValid() {
        // 필수 폴더 경로 확인
        if (defaultScanFolder == null || defaultScanFolder.trim().isEmpty()) {
            return false;
        }

        if (organizationRootFolder == null || organizationRootFolder.trim().isEmpty()) {
            return false;
        }

        // 숫자 범위 확인
        if (maxFileSizeForAnalysis < 1 || maxFileSizeForAnalysis > 10240) { // 1MB ~ 10GB
            return false;
        }

        if (monitoringInterval < 1 || monitoringInterval > 3600) { // 1초 ~ 1시간
            return false;
        }

        if (maxFileCount < 10 || maxFileCount > 100000) { // 10개 ~ 10만개
            return false;
        }

        return true;
    }

    /**
     * 설정 요약 정보 반환
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("📁 스캔 폴더: ").append(defaultScanFolder).append("\n");
        summary.append("📦 정리 폴더: ").append(organizationRootFolder).append("\n");
        summary.append("🤖 자동 정리: ").append(autoOrganizeEnabled ? "활성화" : "비활성화").append("\n");
        summary.append("📅 날짜별 정리: ").append(organizeByDate ? "활성화" : "비활성화").append("\n");
        summary.append("🔍 중복 탐지: ").append(enableDuplicateDetection ? "활성화" : "비활성화").append("\n");
        summary.append("📏 최대 분석 크기: ").append(maxFileSizeForAnalysis).append("MB\n");
        summary.append("📝 커스텀 규칙: ").append(useCustomRules ? "사용" : "미사용").append("\n");

        return summary.toString();
    }
}