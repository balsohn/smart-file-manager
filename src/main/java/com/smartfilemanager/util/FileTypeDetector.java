package com.smartfilemanager.util;

import com.smartfilemanager.service.CustomRulesManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 고급 파일 타입 감지 유틸리티
 * 확장자뿐만 아니라 파일 헤더(매직 넘버)를 분석해서 정확한 파일 타입을 감지합니다
 */
public class FileTypeDetector {

    // 파일 시그니처 (매직 넘버) 매핑
    private static final Map<String, FileTypeInfo> FILE_SIGNATURES = new HashMap<>();
    
    // 커스텀 규칙 매니저 (정적 인스턴스)
    private static CustomRulesManager customRulesManager;

    static {
        // 이미지 파일들
        FILE_SIGNATURES.put("FFD8FF", new FileTypeInfo("JPEG", "image/jpeg", "Images"));
        FILE_SIGNATURES.put("89504E47", new FileTypeInfo("PNG", "image/png", "Images"));
        FILE_SIGNATURES.put("47494638", new FileTypeInfo("GIF", "image/gif", "Images"));
        FILE_SIGNATURES.put("424D", new FileTypeInfo("BMP", "image/bmp", "Images"));
        FILE_SIGNATURES.put("49492A00", new FileTypeInfo("TIFF", "image/tiff", "Images"));
        FILE_SIGNATURES.put("52494646", new FileTypeInfo("WEBP", "image/webp", "Images"));

        // 비디오 파일들
        FILE_SIGNATURES.put("66747970", new FileTypeInfo("MP4", "video/mp4", "Videos"));
        FILE_SIGNATURES.put("000001BA", new FileTypeInfo("MPEG", "video/mpeg", "Videos"));
        FILE_SIGNATURES.put("52494646", new FileTypeInfo("AVI", "video/x-msvideo", "Videos")); // RIFF
        FILE_SIGNATURES.put("1A45DFA3", new FileTypeInfo("MKV", "video/x-matroska", "Videos"));
        FILE_SIGNATURES.put("464C5601", new FileTypeInfo("FLV", "video/x-flv", "Videos"));

        // 오디오 파일들
        FILE_SIGNATURES.put("494433", new FileTypeInfo("MP3", "audio/mpeg", "Audio")); // ID3
        FILE_SIGNATURES.put("FFF3", new FileTypeInfo("MP3", "audio/mpeg", "Audio")); // MPEG-1 Layer 3
        FILE_SIGNATURES.put("52494646", new FileTypeInfo("WAV", "audio/wav", "Audio")); // RIFF
        FILE_SIGNATURES.put("664C6143", new FileTypeInfo("FLAC", "audio/flac", "Audio"));
        FILE_SIGNATURES.put("4F676753", new FileTypeInfo("OGG", "audio/ogg", "Audio"));

        // 문서 파일들
        FILE_SIGNATURES.put("25504446", new FileTypeInfo("PDF", "application/pdf", "Documents"));
        FILE_SIGNATURES.put("D0CF11E0", new FileTypeInfo("DOC", "application/msword", "Documents")); // MS Office
        FILE_SIGNATURES.put("504B0304", new FileTypeInfo("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Documents")); // ZIP-based
        FILE_SIGNATURES.put("7B5C7274", new FileTypeInfo("RTF", "application/rtf", "Documents"));

        // 압축 파일들
        FILE_SIGNATURES.put("504B0304", new FileTypeInfo("ZIP", "application/zip", "Archives"));
        FILE_SIGNATURES.put("526172211A07", new FileTypeInfo("RAR", "application/x-rar-compressed", "Archives"));
        FILE_SIGNATURES.put("377ABCAF271C", new FileTypeInfo("7Z", "application/x-7z-compressed", "Archives"));
        FILE_SIGNATURES.put("1F8B", new FileTypeInfo("GZIP", "application/gzip", "Archives"));
        FILE_SIGNATURES.put("425A68", new FileTypeInfo("BZIP2", "application/x-bzip2", "Archives"));

        // 실행 파일들
        FILE_SIGNATURES.put("4D5A", new FileTypeInfo("EXE", "application/x-msdownload", "Applications"));
        FILE_SIGNATURES.put("CAFEBABE", new FileTypeInfo("CLASS", "application/java-vm", "Applications"));
        FILE_SIGNATURES.put("7F454C46", new FileTypeInfo("ELF", "application/x-executable", "Applications"));

        // 기타
        FILE_SIGNATURES.put("89504E47", new FileTypeInfo("ICO", "image/x-icon", "Images"));
        FILE_SIGNATURES.put("3C3F786D6C", new FileTypeInfo("XML", "application/xml", "Documents"));
        FILE_SIGNATURES.put("3C68746D6C", new FileTypeInfo("HTML", "text/html", "Documents"));
    }

    /**
     * 파일 타입 정보를 저장하는 내부 클래스
     */
    public static class FileTypeInfo {
        private final String type;
        private final String mimeType;
        private final String category;

        public FileTypeInfo(String type, String mimeType, String category) {
            this.type = type;
            this.mimeType = mimeType;
            this.category = category;
        }

        public String getType() { return type; }
        public String getMimeType() { return mimeType; }
        public String getCategory() { return category; }
    }

    /**
     * 파일의 정확한 타입을 감지합니다
     */
    public static FileTypeInfo detectFileType(String filePath) {
        try {
            Path path = Paths.get(filePath);

            // 1. 파일 헤더로 타입 감지 시도
            FileTypeInfo headerType = detectByHeader(path);
            if (headerType != null) {
                return headerType;
            }

            // 2. 확장자로 타입 감지
            FileTypeInfo extensionType = detectByExtension(path);
            if (extensionType != null) {
                return extensionType;
            }

            // 3. 내용 분석으로 타입 감지
            return detectByContent(path);

        } catch (Exception e) {
            System.err.println("[ERROR] 파일 타입 감지 실패: " + filePath + " - " + e.getMessage());
            return new FileTypeInfo("UNKNOWN", "application/octet-stream", "Others");
        }
    }

    /**
     * 파일 헤더(매직 넘버)로 타입 감지
     */
    private static FileTypeInfo detectByHeader(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) < 4) {
            return null;
        }

        byte[] header = new byte[12]; // 처음 12바이트 읽기
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            int bytesRead = fis.read(header);
            if (bytesRead < 2) return null;

            String hexHeader = bytesToHex(header);

            // 정확한 매칭 시도
            for (Map.Entry<String, FileTypeInfo> entry : FILE_SIGNATURES.entrySet()) {
                String signature = entry.getKey();
                if (hexHeader.startsWith(signature)) {
                    System.out.println("[DETECT] 헤더 기반 감지: " + path.getFileName() + " -> " + entry.getValue().getType());
                    return entry.getValue();
                }
            }

            // 특별한 경우들 처리
            return detectSpecialCases(header, hexHeader);

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 특별한 경우들 처리 (복합 시그니처)
     */
    private static FileTypeInfo detectSpecialCases(byte[] header, String hexHeader) {
        // ZIP 기반 파일들 구분
        if (hexHeader.startsWith("504B0304")) {
            // ZIP 파일인데 내용으로 세부 타입 구분
            return new FileTypeInfo("ZIP", "application/zip", "Archives");
        }

        // RIFF 기반 파일들 구분
        if (hexHeader.startsWith("52494646")) {
            if (hexHeader.contains("57415645")) { // "WAVE"
                return new FileTypeInfo("WAV", "audio/wav", "Audio");
            } else if (hexHeader.contains("41564920")) { // "AVI "
                return new FileTypeInfo("AVI", "video/x-msvideo", "Videos");
            } else if (hexHeader.contains("57454250")) { // "WEBP"
                return new FileTypeInfo("WEBP", "image/webp", "Images");
            }
        }

        // Office 파일들 (OLE2 기반)
        if (hexHeader.startsWith("D0CF11E0")) {
            return new FileTypeInfo("DOC", "application/msword", "Documents");
        }

        return null;
    }

    /**
     * 확장자로 타입 감지
     */
    private static FileTypeInfo detectByExtension(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == -1) return null;

        String extension = fileName.substring(lastDot + 1);

        // 확장자 매핑
        Map<String, FileTypeInfo> extensionMap = Map.of(
                "txt", new FileTypeInfo("TXT", "text/plain", "Documents"),
                "md", new FileTypeInfo("MARKDOWN", "text/markdown", "Documents"),
                "json", new FileTypeInfo("JSON", "application/json", "Documents"),
                "csv", new FileTypeInfo("CSV", "text/csv", "Documents"),
                "log", new FileTypeInfo("LOG", "text/plain", "Documents"),
                "cfg", new FileTypeInfo("CONFIG", "text/plain", "Documents"),
                "ini", new FileTypeInfo("INI", "text/plain", "Documents"),
                "bat", new FileTypeInfo("BATCH", "application/x-bat", "Applications"),
                "sh", new FileTypeInfo("SHELL", "application/x-sh", "Applications"),
                "py", new FileTypeInfo("PYTHON", "text/x-python", "Code")
        );

        return extensionMap.get(extension);
    }

    /**
     * 내용 분석으로 타입 감지
     */
    private static FileTypeInfo detectByContent(Path path) {
        try {
            // 파일이 너무 크면 처음 1KB만 확인
            long fileSize = Files.size(path);
            int readSize = (int) Math.min(fileSize, 1024);

            byte[] content = new byte[readSize];
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                fis.read(content);
            }

            // 텍스트 파일 여부 확인
            if (isTextContent(content)) {
                String textContent = new String(content, "UTF-8");

                // 특정 텍스트 포맷 감지
                if (textContent.trim().startsWith("<?xml")) {
                    return new FileTypeInfo("XML", "application/xml", "Documents");
                }
                if (textContent.trim().startsWith("<!DOCTYPE html") ||
                        textContent.toLowerCase().contains("<html")) {
                    return new FileTypeInfo("HTML", "text/html", "Documents");
                }
                if (textContent.trim().startsWith("{") && textContent.trim().endsWith("}")) {
                    return new FileTypeInfo("JSON", "application/json", "Documents");
                }
                if (textContent.contains("#!/bin/bash") || textContent.contains("#!/bin/sh")) {
                    return new FileTypeInfo("SHELL", "application/x-sh", "Applications");
                }
                if (textContent.contains("#!/usr/bin/env python") || textContent.contains("import ")) {
                    return new FileTypeInfo("PYTHON", "text/x-python", "Code");
                }

                return new FileTypeInfo("TXT", "text/plain", "Documents");
            }

            return new FileTypeInfo("BINARY", "application/octet-stream", "Others");

        } catch (Exception e) {
            return new FileTypeInfo("UNKNOWN", "application/octet-stream", "Others");
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 내용이 텍스트인지 확인
     */
    private static boolean isTextContent(byte[] content) {
        int printableCount = 0;
        int totalCount = content.length;

        for (byte b : content) {
            // 인쇄 가능한 ASCII 문자 + 개행, 탭, 캐리지 리턴
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                printableCount++;
            } else if (b == 0) {
                // null 바이트가 있으면 바이너리일 가능성이 높음
                return false;
            }
        }

        // 인쇄 가능한 문자가 70% 이상이면 텍스트로 간주
        return (double) printableCount / totalCount > 0.7;
    }

    /**
     * MIME 타입 감지
     */
    public static String detectMimeType(String filePath) {
        FileTypeInfo typeInfo = detectFileType(filePath);
        return typeInfo != null ? typeInfo.getMimeType() : "application/octet-stream";
    }

    /**
     * 파일 카테고리 감지 (커스텀 규칙 우선 적용)
     */
    public static String detectFileCategory(String filePath) {
        // 1. 커스텀 규칙 먼저 확인
        String customCategory = detectCategoryWithCustomRules(filePath);
        if (customCategory != null && !customCategory.equals("Others")) {
            return customCategory;
        }
        
        // 2. 기본 헤더/확장자 기반 감지
        FileTypeInfo typeInfo = detectFileType(filePath);
        return typeInfo != null ? typeInfo.getCategory() : "Others";
    }
    
    /**
     * 커스텀 규칙을 사용한 카테고리 감지
     */
    public static String detectCategoryWithCustomRules(String filePath) {
        if (customRulesManager == null) {
            return "Others";
        }
        
        String fileName = Paths.get(filePath).getFileName().toString();
        return customRulesManager.determineCategory(fileName);
    }

    /**
     * 파일 타입명 감지
     */
    public static String detectFileTypeName(String filePath) {
        FileTypeInfo typeInfo = detectFileType(filePath);
        return typeInfo != null ? typeInfo.getType() : "UNKNOWN";
    }

    /**
     * 파일이 이미지인지 확인
     */
    public static boolean isImageFile(String filePath) {
        String category = detectFileCategory(filePath);
        return "Images".equals(category);
    }

    /**
     * 파일이 문서인지 확인
     */
    public static boolean isDocumentFile(String filePath) {
        String category = detectFileCategory(filePath);
        return "Documents".equals(category);
    }

    /**
     * 파일이 실행 가능한지 확인
     */
    public static boolean isExecutableFile(String filePath) {
        String category = detectFileCategory(filePath);
        return "Applications".equals(category);
    }

    /**
     * 파일이 압축 파일인지 확인
     */
    public static boolean isArchiveFile(String filePath) {
        String category = detectFileCategory(filePath);
        return "Archives".equals(category);
    }

    /**
     * 상세한 파일 정보 반환
     */
    public static Map<String, String> getDetailedFileInfo(String filePath) {
        Map<String, String> info = new HashMap<>();
        FileTypeInfo typeInfo = detectFileType(filePath);

        if (typeInfo != null) {
            info.put("type", typeInfo.getType());
            info.put("mimeType", typeInfo.getMimeType());
            info.put("category", typeInfo.getCategory());
        }

        try {
            Path path = Paths.get(filePath);
            info.put("size", String.valueOf(Files.size(path)));
            info.put("lastModified", Files.getLastModifiedTime(path).toString());
            info.put("readable", String.valueOf(Files.isReadable(path)));
            info.put("writable", String.valueOf(Files.isWritable(path)));
            info.put("executable", String.valueOf(Files.isExecutable(path)));
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        return info;
    }
    
    /**
     * 커스텀 규칙 매니저 초기화
     */
    public static void initializeCustomRules(String rulesFilePath) {
        try {
            if (rulesFilePath != null && !rulesFilePath.trim().isEmpty()) {
                customRulesManager = new CustomRulesManager(rulesFilePath);
            } else {
                customRulesManager = new CustomRulesManager();
            }
            System.out.println("[FileTypeDetector] 커스텀 규칙 초기화 완료: " + customRulesManager.getRulesFilePath());
        } catch (Exception e) {
            System.err.println("[ERROR] 커스텀 규칙 초기화 실패: " + e.getMessage());
            customRulesManager = null;
        }
    }
    
    /**
     * 커스텀 규칙 매니저 조회
     */
    public static CustomRulesManager getCustomRulesManager() {
        return customRulesManager;
    }
    
    /**
     * 커스텀 규칙 사용 여부 확인
     */
    public static boolean isCustomRulesEnabled() {
        return customRulesManager != null;
    }
    
    /**
     * 커스텀 규칙 매니저 재로드
     */
    public static void reloadCustomRules() {
        if (customRulesManager != null) {
            customRulesManager.loadRules();
            System.out.println("[FileTypeDetector] 커스텀 규칙 재로드 완료");
        }
    }
    
    /**
     * 커스텀 규칙을 고려한 파일 카테고리 감지 (설정 기반)
     */
    public static String detectFileCategoryWithConfig(String filePath, boolean useCustomRules) {
        if (useCustomRules && customRulesManager != null) {
            return detectFileCategory(filePath);  // 커스텀 규칙 우선 적용
        } else {
            // 기본 헤더/확장자 기반 감지만 사용
            FileTypeInfo typeInfo = detectFileType(filePath);
            return typeInfo != null ? typeInfo.getCategory() : "Others";
        }
    }
}