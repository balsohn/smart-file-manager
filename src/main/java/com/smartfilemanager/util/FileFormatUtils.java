package com.smartfilemanager.util;

import com.smartfilemanager.constants.FileConstants;
import com.smartfilemanager.model.FileInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 파일 포맷팅 관련 유틸리티 클래스
 */
public final class FileFormatUtils {
    
    private FileFormatUtils() {
        // 인스턴스 생성 방지
    }
    
    /**
     * 파일 크기를 읽기 쉬운 형태로 포맷팅
     */
    public static String formatFileSize(long bytes) {
        if (bytes < FileConstants.FileSizeUnits.KB) {
            return bytes + " B";
        }
        if (bytes < FileConstants.FileSizeUnits.MB) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < FileConstants.FileSizeUnits.GB) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * 날짜를 테이블 표시용으로 포맷팅
     */
    public static String formatTableDate(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(FileConstants.DateFormats.TABLE_DATE_FORMAT));
        } catch (Exception e) {
            return "-";
        }
    }
    
    /**
     * 신뢰도를 백분율로 포맷팅
     */
    public static String formatConfidence(double confidence) {
        return String.format("%.0f%%", confidence * 100);
    }
    
    /**
     * 파일 이름을 안전하게 가져오기
     */
    public static String getFileNameSafe(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFileName() == null) {
            return "알 수 없는 파일";
        }
        
        String fileName = fileInfo.getFileName();
        // 파일명이 너무 길면 줄이기
        if (fileName.length() > 50) {
            String extension = getFileExtension(fileName);
            String baseName = getBaseName(fileName);
            if (baseName.length() > 40) {
                baseName = baseName.substring(0, 40) + "...";
            }
            fileName = extension.isEmpty() ? baseName : baseName + "." + extension;
        }
        
        return fileName;
    }
    
    /**
     * 카테고리 정보를 포맷팅 (카테고리/서브카테고리)
     */
    public static String formatCategory(FileInfo fileInfo) {
        if (fileInfo == null) return FileConstants.Categories.UNKNOWN;
        
        String category = fileInfo.getDetectedCategory() != null ? 
                         fileInfo.getDetectedCategory() : FileConstants.Categories.UNKNOWN;
        String subCategory = fileInfo.getDetectedSubCategory();
        
        return (subCategory != null && !subCategory.isEmpty() && 
                !subCategory.equals(FileConstants.SubCategories.GENERAL))
            ? category + "/" + subCategory : category;
    }
    
    /**
     * 상태 표시 이름 가져오기
     */
    public static String getStatusDisplayName(FileInfo fileInfo) {
        return fileInfo != null && fileInfo.getStatus() != null ? 
               fileInfo.getStatus().getDisplayName() : "대기중";
    }
    
    /**
     * 파일명에서 확장자 제거
     */
    public static String getBaseName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
    }
    
    /**
     * 파일명에서 확장자 추출
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
    
    /**
     * 폴더별 날짜 형식 생성 (예: 01-JAN)
     */
    public static String formatFolderMonth(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return String.format(FileConstants.DateFormats.FOLDER_MONTH_FORMAT,
                dateTime.getMonthValue(),
                dateTime.getMonth().name().substring(0, 3));
    }
    
    /**
     * 진행률 메시지 포맷팅
     */
    public static String formatProgress(int current, int total) {
        return String.format("%d / %d 파일 처리됨", current, total);
    }
    
    /**
     * AI 분석 결과 포함 신뢰도 표시
     */
    public static String formatConfidenceWithAI(double confidence, boolean isAIAnalyzed) {
        String confidenceText = formatConfidence(confidence);
        if (isAIAnalyzed) {
            confidenceText += " " + com.smartfilemanager.constants.UIConstants.GeneralIcons.AI_ANALYZED;
        }
        return confidenceText;
    }
    
    /**
     * 파일 개수와 크기를 포함한 통계 메시지 포맷팅
     */
    public static String formatFileStats(long fileCount, long totalSize) {
        return String.format("%d files (%s)", fileCount, formatFileSize(totalSize));
    }
    
    /**
     * AI 분석 포함 파일 통계 메시지 포맷팅
     */
    public static String formatFileStatsWithAI(long fileCount, long totalSize, long aiAnalyzedCount) {
        String baseStats = formatFileStats(fileCount, totalSize);
        if (aiAnalyzedCount > 0) {
            baseStats += String.format(" • %d개 AI 분석됨", aiAnalyzedCount);
        }
        return baseStats;
    }
}