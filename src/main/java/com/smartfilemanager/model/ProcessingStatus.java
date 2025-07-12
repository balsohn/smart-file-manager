package com.smartfilemanager.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 처리 상태를 나타내는 열거형
 */
@Getter
@RequiredArgsConstructor
public enum ProcessingStatus {

    PENDING("Pending", "대기 중", "#6c757d"),           // 스캔 대기
    SCANNING("Scanning", "스캔 중", "#007bff"),         // 현재 스캔 중
    SCANNED("Scanned", "스캔 완료", "#20c997"),         // 스캔 완료, 분석 대기
    ANALYZED("Analyzed", "분석 완료", "#17a2b8"),       // 분석 완료, 정리 대기
    ORGANIZING("Organizing", "정리 중", "#ffc107"),     // 현재 정리 중
    ORGANIZED("Organized", "정리 완료", "#28a745"),     // 정리 완료
    FAILED("Failed", "실패", "#dc3545"),               // 처리 실패
    SKIPPED("Skipped", "건너뜀", "#6f42c1");           // 사용자가 건너뜀

    private final String displayName;    // 영어 표시명
    private final String koreanName;     // 한국어 표시명 (나중에 사용)
    private final String colorCode;      // UI에서 사용할 색상 코드

    /**
     * 상태가 처리 가능한 상태인지 확인
     */
    public boolean isProcessable() {
        return this == ANALYZED;
    }

    /**
     * 상태가 완료된 상태인지 확인
     */
    public boolean isCompleted() {
        return this == ORGANIZED || this == SKIPPED;
    }

    /**
     * 상태가 오류 상태인지 확인
     */
    public boolean isError() {
        return this == FAILED;
    }

    /**
     * 상태가 진행 중인지 확인
     */
    public boolean isInProgress() {
        return this == SCANNING || this == ORGANIZING;
    }
}