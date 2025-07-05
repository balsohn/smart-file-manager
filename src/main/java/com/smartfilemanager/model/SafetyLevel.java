package com.smartfilemanager.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 삭제 안전성 등급을 나타내는 열거형
 * 각 등급별로 다른 확인 절차를 거칩니다
 */
@Getter
@RequiredArgsConstructor
public enum SafetyLevel {

    SAFE("안전", "확실히 삭제해도 안전한 파일",
            "#28a745", "✅", true),

    LIKELY_SAFE("아마 안전", "삭제해도 문제없을 가능성이 높은 파일",
            "#17a2b8", "🟢", true),

    CAUTION("주의", "삭제 전 주의깊게 확인이 필요한 파일",
            "#ffc107", "⚠️", false),

    USER_DECISION("사용자 판단", "사용자가 직접 판단해야 하는 파일",
            "#fd7e14", "❓", false);

    private final String displayName;      // 화면 표시명
    private final String description;      // 설명
    private final String colorCode;        // UI 색상 코드
    private final String icon;             // 아이콘
    private final boolean autoSelectable;  // 자동 선택 가능 여부

    /**
     * 안전성 등급에 따른 확인 메시지 반환
     */
    public String getConfirmationMessage() {
        switch (this) {
            case SAFE:
                return "이 파일들은 안전하게 삭제할 수 있습니다.";
            case LIKELY_SAFE:
                return "이 파일들은 삭제해도 문제없을 것으로 예상됩니다.";
            case CAUTION:
                return "이 파일들을 삭제하기 전에 내용을 확인해주세요.";
            case USER_DECISION:
                return "이 파일들은 신중하게 검토한 후 삭제 여부를 결정해주세요.";
            default:
                return "파일 삭제 여부를 확인해주세요.";
        }
    }

    /**
     * 안전성 등급에 따른 권장 작업 반환
     */
    public String getRecommendedAction() {
        switch (this) {
            case SAFE:
                return "즉시 삭제";
            case LIKELY_SAFE:
                return "삭제 권장";
            case CAUTION:
                return "검토 후 삭제";
            case USER_DECISION:
                return "사용자 판단";
            default:
                return "수동 확인";
        }
    }

    /**
     * 우선순위 반환 (낮을수록 우선 처리)
     */
    public int getPriority() {
        switch (this) {
            case SAFE: return 1;
            case LIKELY_SAFE: return 2;
            case CAUTION: return 3;
            case USER_DECISION: return 4;
            default: return 5;
        }
    }

    /**
     * 백업 필요 여부
     */
    public boolean needsBackup() {
        return this == CAUTION || this == USER_DECISION;
    }

    /**
     * 등급명으로 찾기
     */
    public static SafetyLevel fromDisplayName(String displayName) {
        for (SafetyLevel level : values()) {
            if (level.getDisplayName().equals(displayName)) {
                return level;
            }
        }
        return USER_DECISION;
    }
}