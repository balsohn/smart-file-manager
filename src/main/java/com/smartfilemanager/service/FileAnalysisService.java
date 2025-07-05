package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일 분석 서비스
 * 파일의 메타데이터, 내용, 패턴을 분석해서 정확한 분류를 수행합니다
 */
public class FileAnalysisService {

    /**
     * 파일을 종합적으로 분석해서 FileInfo 생성
     */
    public FileInfo analyzeFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            // 1. 기본 파일 정보 생성
            FileInfo fileInfo = createBasicFileInfo(file, path);

            // 2. 메타데이터 추출
            extractMetadata(fileInfo, path);

            // 3. MIME 타입 감지
            detectMimeType(fileInfo, path);

            // 4. 기본 카테고리 분류
            classifyBasicCategory(fileInfo);

            // 5. 스마트 서브카테고리 분류
            classifySmartSubCategory(fileInfo);

            // 6. 파일명 패턴 분석
            analyzeFileNamePatterns(fileInfo);

            // 7. 내용 기반 분석 (선택적)
            if (shouldAnalyzeContent(fileInfo)) {
                analyzeFileContent(fileInfo);
            }

            // 8. 신뢰도 점수 계산
            calculateConfidenceScore(fileInfo);

            // 9. 제안 경로 생성
            generateSuggestedPath(fileInfo);

            fileInfo.setStatus(ProcessingStatus.ANALYZED);
            fileInfo.setProcessedAt(LocalDateTime.now());

            return fileInfo;

        } catch (Exception e) {
            System.err.println("[오류] 파일 분석 실패: " + filePath + " - " + e.getMessage());
            return createErrorFileInfo(filePath, e.getMessage());
        }
    }

    /**
     * 기본 파일 정보 생성
     */
    private FileInfo createBasicFileInfo(File file, Path path) throws IOException {
        return FileInfo.defaultBuilder()
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .originalLocation(file.getParent())
                .fileSize(file.length())
                .fileExtension(extractFileExtension(file.getName()))
                .build();
    }

    /**
     * 메타데이터 추출 (생성일, 수정일, 파일 속성)
     */
    private void extractMetadata(FileInfo fileInfo, Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        LocalDateTime createdTime = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
        LocalDateTime modifiedTime = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        fileInfo.setCreatedDate(createdTime);
        fileInfo.setModifiedDate(modifiedTime);

        System.out.println("[메타데이터] " + fileInfo.getFileName() +
                " - 생성: " + createdTime.toLocalDate() +
                ", 수정: " + modifiedTime.toLocalDate());
    }

    /**
     * MIME 타입 정확한 감지
     */
    private void detectMimeType(FileInfo fileInfo, Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null) {
                fileInfo.setMimeType(mimeType);
                System.out.println("[MIME타입] " + fileInfo.getFileName() + " - " + mimeType);
            } else {
                // 확장자 기반 추정
                fileInfo.setMimeType(estimateMimeTypeFromExtension(fileInfo.getFileExtension()));
            }
        } catch (IOException e) {
            System.err.println("[경고] MIME 타입 감지 실패: " + fileInfo.getFileName());
            fileInfo.setMimeType("application/octet-stream");
        }
    }

    /**
     * 기본 카테고리 분류 (확장자 및 MIME 타입 기반)
     */
    private void classifyBasicCategory(FileInfo fileInfo) {
        String extension = fileInfo.getFileExtension().toLowerCase();
        String mimeType = fileInfo.getMimeType();

        // MIME 타입 우선 분류
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                fileInfo.setDetectedCategory("Images");
                return;
            } else if (mimeType.startsWith("video/")) {
                fileInfo.setDetectedCategory("Videos");
                return;
            } else if (mimeType.startsWith("audio/")) {
                fileInfo.setDetectedCategory("Audio");
                return;
            } else if (mimeType.startsWith("text/") ||
                    mimeType.equals("application/pdf") ||
                    mimeType.contains("document")) {
                fileInfo.setDetectedCategory("Documents");
                return;
            }
        }

        // 확장자 기반 분류
        if (extension.matches("jpg|jpeg|png|gif|bmp|svg|webp|tiff|ico")) {
            fileInfo.setDetectedCategory("Images");
        } else if (extension.matches("pdf|doc|docx|txt|rtf|odt|hwp")) {
            fileInfo.setDetectedCategory("Documents");
        } else if (extension.matches("xls|xlsx|csv|ods")) {
            fileInfo.setDetectedCategory("Spreadsheets");
        } else if (extension.matches("ppt|pptx|odp")) {
            fileInfo.setDetectedCategory("Presentations");
        } else if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm|m4v")) {
            fileInfo.setDetectedCategory("Videos");
        } else if (extension.matches("mp3|wav|flac|aac|m4a|ogg|wma")) {
            fileInfo.setDetectedCategory("Audio");
        } else if (extension.matches("zip|rar|7z|tar|gz|bz2|xz")) {
            fileInfo.setDetectedCategory("Archives");
        } else if (extension.matches("exe|msi|dmg|pkg|deb|rpm|app")) {
            fileInfo.setDetectedCategory("Applications");
        } else if (extension.matches("java|js|py|cpp|c|html|css|json|xml|php|rb|go|rs")) {
            fileInfo.setDetectedCategory("Code");
        } else {
            fileInfo.setDetectedCategory("Others");
        }
    }

    /**
     * 스마트 서브카테고리 분류
     */
    private void classifySmartSubCategory(FileInfo fileInfo) {
        String category = fileInfo.getDetectedCategory();
        String fileName = fileInfo.getFileName().toLowerCase();

        switch (category) {
            case "Images":
                fileInfo.setDetectedSubCategory(analyzeImageSubCategory(fileInfo, fileName));
                break;
            case "Documents":
                fileInfo.setDetectedSubCategory(analyzeDocumentSubCategory(fileInfo, fileName));
                break;
            case "Videos":
                fileInfo.setDetectedSubCategory(analyzeVideoSubCategory(fileInfo, fileName));
                break;
            case "Archives":
                fileInfo.setDetectedSubCategory(analyzeArchiveSubCategory(fileInfo, fileName));
                break;
            case "Code":
                fileInfo.setDetectedSubCategory(analyzeCodeSubCategory(fileInfo));
                break;
        }
    }

    /**
     * 이미지 서브카테고리 분석
     */
    private String analyzeImageSubCategory(FileInfo fileInfo, String fileName) {
        // 스크린샷 패턴
        if (fileName.matches(".*(screenshot|screen.shot|스크린샷|캡처|capture).*") ||
                fileName.matches(".*\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}.*")) {
            return "Screenshots";
        }

        // 아이콘 패턴 (작은 크기 + 특정 확장자)
        if ((fileName.contains("icon") || fileName.contains("아이콘")) ||
                (fileInfo.getFileSize() < 50 * 1024 &&
                        fileInfo.getFileExtension().matches("png|ico|svg"))) {
            return "Icons";
        }

        // 배경화면 패턴
        if (fileName.matches(".*(wallpaper|background|배경|벽지|desktop).*")) {
            return "Wallpapers";
        }

        // 사진 패턴 (카메라 파일명 또는 날짜 포함)
        if (fileName.matches(".*(img_|dsc_|photo_|사진|\\d{8}|\\d{4}-\\d{2}-\\d{2}).*")) {
            return "Photos";
        }

        // 큰 이미지는 사진일 가능성
        if (fileInfo.getFileSize() > 1 * 1024 * 1024) { // 1MB 이상
            return "Photos";
        }

        return "General";
    }

    /**
     * 문서 서브카테고리 분석
     */
    private String analyzeDocumentSubCategory(FileInfo fileInfo, String fileName) {
        // 업무 관련
        if (fileName.matches(".*(report|보고서|계획|plan|project|프로젝트|회의|meeting|업무|business).*")) {
            return "Work";
        }

        // 이력서/자소서
        if (fileName.matches(".*(resume|cv|이력서|자기소개서|자소서|경력).*")) {
            return "Resume";
        }

        // 금융/영수증
        if (fileName.matches(".*(receipt|invoice|영수증|계산서|청구서|입금|출금|은행|bank|finance).*")) {
            return "Financial";
        }

        // 매뉴얼/가이드
        if (fileName.matches(".*(manual|guide|매뉴얼|가이드|설명서|사용법|tutorial|howto).*")) {
            return "Manuals";
        }

        // 학습/교육
        if (fileName.matches(".*(study|학습|교육|강의|lecture|course|교재|textbook).*")) {
            return "Educational";
        }

        // 계약/법률
        if (fileName.matches(".*(contract|계약|법률|legal|agreement|약관).*")) {
            return "Legal";
        }

        return "General";
    }

    /**
     * 비디오 서브카테고리 분석
     */
    private String analyzeVideoSubCategory(FileInfo fileInfo, String fileName) {
        // 영화 (대용량 또는 특정 패턴)
        if (fileName.matches(".*(movie|film|영화|cinema).*") ||
                fileInfo.getFileSize() > 700 * 1024 * 1024) { // 700MB 이상
            return "Movies";
        }

        // TV 시리즈
        if (fileName.matches(".*(s\\d+e\\d+|시즌|season|episode|에피소드|드라마).*")) {
            return "TV Shows";
        }

        // 교육/튜토리얼
        if (fileName.matches(".*(tutorial|강의|lecture|course|교육|학습|howto|lesson).*")) {
            return "Educational";
        }

        // 짧은 클립
        if (fileInfo.getFileSize() < 100 * 1024 * 1024) { // 100MB 미만
            return "Clips";
        }

        return "General";
    }

    /**
     * 압축파일 서브카테고리 분석
     */
    private String analyzeArchiveSubCategory(FileInfo fileInfo, String fileName) {
        if (fileName.matches(".*(backup|백업|bak).*")) {
            return "Backups";
        } else if (fileName.matches(".*(setup|install|설치|installer|software).*")) {
            return "Software";
        } else if (fileName.matches(".*(game|게임|mod).*")) {
            return "Games";
        }
        return "General";
    }

    /**
     * 코드 파일 서브카테고리 분석
     */
    private String analyzeCodeSubCategory(FileInfo fileInfo) {
        String extension = fileInfo.getFileExtension().toLowerCase();
        switch (extension) {
            case "java": return "Java";
            case "js": case "ts": return "JavaScript";
            case "py": return "Python";
            case "cpp": case "c": case "h": return "C/C++";
            case "html": case "css": return "Web";
            case "json": case "xml": case "yaml": case "yml": return "Config";
            case "sql": return "Database";
            default: return "General";
        }
    }

    /**
     * 파일명 패턴 분석 (날짜, 키워드 추출)
     */
    private void analyzeFileNamePatterns(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName();
        List<String> keywords = new ArrayList<>();

        // 날짜 패턴 추출
        extractDatePatterns(fileName, keywords);

        // 의미있는 단어 추출
        extractMeaningfulWords(fileName, keywords);

        // 특수 패턴 감지
        detectSpecialPatterns(fileName, keywords);

        // 제목 추출 (확장자 제거 후 정리)
        String title = fileName.substring(0, fileName.lastIndexOf('.') > 0 ? fileName.lastIndexOf('.') : fileName.length());
        title = title.replaceAll("[\\-_\\.]", " ").trim();
        fileInfo.setExtractedTitle(title);

        System.out.println("[패턴분석] " + fileInfo.getFileName() + " - 키워드: " + keywords + ", 제목: " + title);
    }

    /**
     * 날짜 패턴 추출
     */
    private void extractDatePatterns(String fileName, List<String> keywords) {
        Pattern datePattern = Pattern.compile("(\\d{4})[\\-_](\\d{2})[\\-_](\\d{2})");
        Matcher matcher = datePattern.matcher(fileName);

        if (matcher.find()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            keywords.add("date:" + year + "-" + month);
        }

        // 8자리 숫자 날짜 (20240115 형태)
        Pattern datePattern2 = Pattern.compile("(\\d{8})");
        Matcher matcher2 = datePattern2.matcher(fileName);
        if (matcher2.find()) {
            String dateStr = matcher2.group(1);
            if (dateStr.startsWith("20")) { // 2000년대 이후
                keywords.add("date:" + dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6));
            }
        }
    }

    /**
     * 의미있는 단어 추출
     */
    private void extractMeaningfulWords(String fileName, List<String> keywords) {
        String[] words = fileName.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ").split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && !word.matches("\\d+")) {
                keywords.add(word.toLowerCase());
            }
        }
    }

    /**
     * 특수 패턴 감지
     */
    private void detectSpecialPatterns(String fileName, List<String> keywords) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.contains("download")) keywords.add("downloaded");
        if (lowerName.contains("temp") || lowerName.contains("tmp")) keywords.add("temporary");
        if (lowerName.contains("copy") || lowerName.contains("복사")) keywords.add("copy");
        if (lowerName.contains("new") || lowerName.contains("신규")) keywords.add("new");
    }

    /**
     * 파일 내용 분석 여부 결정
     */
    private boolean shouldAnalyzeContent(FileInfo fileInfo) {
        // 텍스트 파일만 내용 분석
        String extension = fileInfo.getFileExtension().toLowerCase();
        return extension.matches("txt|md|json|xml|html|css|js") &&
                fileInfo.getFileSize() < 1024 * 1024; // 1MB 미만
    }

    /**
     * 파일 내용 분석 (텍스트 파일 대상)
     */
    private void analyzeFileContent(FileInfo fileInfo) {
        try {
            Path path = Paths.get(fileInfo.getFilePath());
            List<String> lines = Files.readAllLines(path);

            if (!lines.isEmpty()) {
                // 첫 몇 줄을 설명으로 설정
                StringBuilder description = new StringBuilder();
                for (int i = 0; i < Math.min(3, lines.size()); i++) {
                    description.append(lines.get(i)).append(" ");
                }
                fileInfo.setDescription(description.toString().trim());

                // JSON, XML 등 특수 파일 타입 감지
                String content = String.join(" ", lines);
                if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
                    fileInfo.setDetectedSubCategory("JSON");
                } else if (content.trim().startsWith("<") && content.trim().endsWith(">")) {
                    fileInfo.setDetectedSubCategory("XML");
                }
            }

            System.out.println("[내용분석] " + fileInfo.getFileName() + " - " + lines.size() + "줄");

        } catch (IOException e) {
            System.err.println("[경고] 파일 내용 분석 실패: " + fileInfo.getFileName());
        }
    }

    /**
     * 신뢰도 점수 계산
     */
    private void calculateConfidenceScore(FileInfo fileInfo) {
        double score = 0.4; // 기본 점수

        // 확장자와 MIME 타입이 일치하면 +0.2
        if (fileInfo.getMimeType() != null && isExtensionMimeTypeMatched(fileInfo)) {
            score += 0.2;
        }

        // 서브카테고리가 있고 "General"이 아니면 +0.3
        if (fileInfo.getDetectedSubCategory() != null &&
                !fileInfo.getDetectedSubCategory().equals("General")) {
            score += 0.3;
        }

        // 의미있는 파일명 패턴이 있으면 +0.1
        if (fileInfo.getExtractedTitle() != null && fileInfo.getExtractedTitle().length() > 5) {
            score += 0.1;
        }

        fileInfo.setConfidenceScore(Math.min(score, 1.0));
    }

    /**
     * 제안 경로 생성
     */
    private void generateSuggestedPath(FileInfo fileInfo) {
        StringBuilder pathBuilder = new StringBuilder();

        // 기본 경로: 카테고리
        pathBuilder.append(fileInfo.getDetectedCategory());

        // 서브카테고리 추가
        if (fileInfo.getDetectedSubCategory() != null &&
                !fileInfo.getDetectedSubCategory().equals("General")) {
            pathBuilder.append("/").append(fileInfo.getDetectedSubCategory());
        }

        fileInfo.setSuggestedPath(pathBuilder.toString());
    }

    // 헬퍼 메서드들

    private String extractFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }

    private String estimateMimeTypeFromExtension(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            case "html": return "text/html";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            default: return "application/octet-stream";
        }
    }

    private boolean isExtensionMimeTypeMatched(FileInfo fileInfo) {
        String extension = fileInfo.getFileExtension();
        String mimeType = fileInfo.getMimeType();

        if (mimeType == null) return false;

        return mimeType.equals(estimateMimeTypeFromExtension(extension));
    }

    private FileInfo createErrorFileInfo(String filePath, String errorMessage) {
        FileInfo errorInfo = new FileInfo();
        errorInfo.setFilePath(filePath);
        errorInfo.setFileName(Paths.get(filePath).getFileName().toString());
        errorInfo.setStatus(ProcessingStatus.FAILED);
        errorInfo.setErrorMessage(errorMessage);
        return errorInfo;
    }
}