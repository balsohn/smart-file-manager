package com.smartfilemanager.util;

import com.smartfilemanager.constants.UIConstants;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;

/**
 * 파일 아이콘 관련 유틸리티 클래스
 */
public final class FileIconUtils {
    
    private FileIconUtils() {
        // 인스턴스 생성 방지
    }
    
    /**
     * 파일 카테고리에 따른 아이콘 반환
     */
    public static String getFileIcon(String category) {
        if (category == null) return UIConstants.FileIcons.UNKNOWN;
        
        switch (category.toLowerCase()) {
            case "documents":
                return UIConstants.FileIcons.DOCUMENTS;
            case "images":
                return UIConstants.FileIcons.IMAGES;
            case "videos":
                return UIConstants.FileIcons.VIDEOS;
            case "audio":
                return UIConstants.FileIcons.AUDIO;
            case "archives":
                return UIConstants.FileIcons.ARCHIVES;
            case "applications":
                return UIConstants.FileIcons.APPLICATIONS;
            default:
                return UIConstants.FileIcons.DEFAULT;
        }
    }
    
    /**
     * FileInfo 객체로부터 파일 아이콘 반환
     */
    public static String getFileIcon(FileInfo fileInfo) {
        if (fileInfo == null) return UIConstants.FileIcons.UNKNOWN;
        return getFileIcon(fileInfo.getDetectedCategory());
    }
    
    /**
     * 처리 상태에 따른 아이콘 반환
     */
    public static String getStatusIcon(ProcessingStatus status) {
        if (status == null) return UIConstants.StatusIcons.DEFAULT;
        
        switch (status) {
            case PENDING:
                return UIConstants.StatusIcons.PENDING;
            case SCANNING:
                return UIConstants.StatusIcons.SCANNING;
            case ANALYZED:
                return UIConstants.StatusIcons.ANALYZED;
            case ORGANIZING:
                return UIConstants.StatusIcons.ORGANIZING;
            case ORGANIZED:
                return UIConstants.StatusIcons.ORGANIZED;
            case FAILED:
                return UIConstants.StatusIcons.FAILED;
            case SKIPPED:
                return UIConstants.StatusIcons.SKIPPED;
            default:
                return UIConstants.StatusIcons.DEFAULT;
        }
    }
    
    /**
     * 처리 상태에 따른 색상 반환
     */
    public static String getStatusColor(ProcessingStatus status) {
        if (status == null) return UIConstants.StatusColors.DEFAULT;
        
        switch (status) {
            case PENDING:
                return UIConstants.StatusColors.PENDING;
            case SCANNING:
                return UIConstants.StatusColors.SCANNING;
            case ANALYZED:
                return UIConstants.StatusColors.ANALYZED;
            case ORGANIZING:
                return UIConstants.StatusColors.ORGANIZING;
            case ORGANIZED:
                return UIConstants.StatusColors.ORGANIZED;
            case FAILED:
                return UIConstants.StatusColors.FAILED;
            case SKIPPED:
                return UIConstants.StatusColors.SKIPPED;
            default:
                return UIConstants.StatusColors.DEFAULT;
        }
    }
    
    /**
     * 신뢰도에 따른 색상 반환
     */
    public static String getConfidenceColor(double confidence) {
        if (confidence >= com.smartfilemanager.constants.FileConstants.ConfidenceThresholds.HIGH) {
            return UIConstants.ConfidenceColors.HIGH;
        } else if (confidence >= com.smartfilemanager.constants.FileConstants.ConfidenceThresholds.MEDIUM) {
            return UIConstants.ConfidenceColors.MEDIUM;
        } else {
            return UIConstants.ConfidenceColors.LOW;
        }
    }
    
    /**
     * 파일명과 아이콘을 조합한 표시 텍스트 생성
     */
    public static String formatFileNameWithIcon(FileInfo fileInfo) {
        if (fileInfo == null) return UIConstants.FileIcons.UNKNOWN + " 알 수 없는 파일";
        
        String icon = getFileIcon(fileInfo);
        String fileName = FileFormatUtils.getFileNameSafe(fileInfo);
        return icon + " " + fileName;
    }
    
    /**
     * 상태와 아이콘을 조합한 표시 텍스트 생성
     */
    public static String formatStatusWithIcon(ProcessingStatus status, String statusText) {
        if (status == null || statusText == null) return UIConstants.StatusIcons.DEFAULT + " 대기중";
        
        String icon = getStatusIcon(status);
        return icon + " " + statusText;
    }
    
    /**
     * AI 분석 여부 확인 (아이콘 표시용)
     */
    public static boolean isAIAnalyzed(FileInfo fileInfo) {
        if (fileInfo == null) return false;
        
        return (fileInfo.getKeywords() != null && 
                fileInfo.getKeywords().contains(com.smartfilemanager.constants.FileConstants.AIAnalysis.AI_ANALYZED_KEYWORD)) ||
               (fileInfo.getDescription() != null && 
                fileInfo.getDescription().contains(com.smartfilemanager.constants.FileConstants.AIAnalysis.AI_ANALYSIS_PREFIX)) ||
               (fileInfo.getKeywords() != null && 
                fileInfo.getKeywords().size() > com.smartfilemanager.constants.FileConstants.AIAnalysis.MIN_AI_KEYWORDS_COUNT) ||
               fileInfo.getConfidenceScore() > com.smartfilemanager.constants.FileConstants.ConfidenceThresholds.AI_ANALYZED;
    }
}