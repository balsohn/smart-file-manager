package com.smartfilemanager.service;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.util.AIAnalyzer;
import com.smartfilemanager.util.FileTypeDetector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일 분석 서비스 (AI 분석 연동 완성 버전)
 * 파일의 메타데이터, 내용, 패턴을 분석해서 정확한 분류를 수행합니다
 */
public class FileAnalysisService {

    private final ConfigService configService;
    private final AIAnalyzer aiAnalyzer;
    private AppConfig currentConfig;

    // AI 분석 관련 필드
    private boolean aiAnalysisEnabled = false;
    private String aiApiKey = null;

    public FileAnalysisService() {
        this.configService = new ConfigService();
        this.aiAnalyzer = new AIAnalyzer();
        this.currentConfig = configService.getCurrentConfig();

        // AI 설정 초기화
        initializeAI();
    }

    /**
     * AI 분석기 초기화
     */
    private void initializeAI() {
        if (currentConfig.isEnableAIAnalysis() && currentConfig.getAiApiKey() != null) {
            this.aiAnalysisEnabled = true;
            this.aiApiKey = currentConfig.getAiApiKey();

            aiAnalyzer.setApiKey(currentConfig.getAiApiKey());
            aiAnalyzer.setModel(currentConfig.getAiModel() != null ?
                    currentConfig.getAiModel() : "gpt-3.5-turbo");

            System.out.println("[AI] AI 분석 시스템 초기화 완료");
            System.out.println(aiAnalyzer.getConfigSummary());

            // API 키 유효성 검사 (비동기)
            validateApiKeyAsync();
        } else {
            this.aiAnalysisEnabled = false;
            this.aiApiKey = null;
            System.out.println("[INFO] AI 분석이 비활성화되어 있습니다.");
        }
    }

    /**
     * 비동기로 API 키 유효성 검사
     */
    private void validateApiKeyAsync() {
        new Thread(() -> {
            try {
                boolean isValid = aiAnalyzer.validateApiKey();
                if (isValid) {
                    System.out.println("[AI] ✅ API 키 검증 성공 - AI 분석 준비 완료");
                } else {
                    System.err.println("[WARNING] ❌ AI API 키가 유효하지 않습니다.");
                    this.aiAnalysisEnabled = false;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] API 키 검증 중 오류: " + e.getMessage());
                this.aiAnalysisEnabled = false;
            }
        }).start();
    }

    /**
     * AI 분석이 사용 가능한지 확인
     */
    public boolean isAIAnalysisAvailable() {
        AppConfig config = configService.getCurrentConfig();
        return config.isEnableAIAnalysis() &&
                config.getAiApiKey() != null &&
                !config.getAiApiKey().trim().isEmpty() &&
                this.aiAnalysisEnabled;
    }

    /**
     * AI 설정 새로고침
     */
    public void refreshConfig() {
        try {
            AppConfig config = configService.getCurrentConfig();
            this.aiAnalysisEnabled = config.isEnableAIAnalysis();
            this.aiApiKey = config.getAiApiKey();
            this.currentConfig = config;

            initializeAI();

            System.out.println("[AI] 설정 새로고침: AI 활성화=" + aiAnalysisEnabled +
                    ", API 키 설정됨=" + (aiApiKey != null && !aiApiKey.trim().isEmpty()));
        } catch (Exception e) {
            System.err.println("[AI] 설정 새로고침 실패: " + e.getMessage());
            this.aiAnalysisEnabled = false;
            this.aiApiKey = null;
        }
    }

    /**
     * 파일을 종합적으로 분석해서 FileInfo 생성 (AI 분석 포함)
     */
    public FileInfo analyzeFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            if (!file.exists() || !file.isFile()) {
                throw new IOException("파일이 존재하지 않습니다: " + filePath);
            }

            System.out.println("[분석] 파일 분석 시작: " + file.getName());

            // 1. 기본 파일 정보 생성
            FileInfo fileInfo = createBasicFileInfo(file, path);

            // 2. 메타데이터 추출
            extractMetadata(fileInfo, path);

            // 3. MIME 타입 감지
            detectMimeType(fileInfo, path);

            // 4. 기본 카테고리 분류 (확장자 기반)
            classifyBasicCategory(fileInfo);

            // 5. 파일명 분석
            analyzeFileName(fileInfo);

            // 6. 세부 카테고리 분류
            classifyDetailedCategory(fileInfo);

            // 7. 기본 신뢰도 설정
            calculateBasicConfidence(fileInfo);

            // 8. AI 분석 적용 (설정이 켜져있다면)
            if (isAIAnalysisAvailable()) {
                enhanceWithAI(fileInfo);
            }

            // 9. 추천 경로 결정
            determineSuggestedPath(fileInfo);

            // 10. 상태 설정
            fileInfo.setStatus(ProcessingStatus.ANALYZED);
            fileInfo.setProcessedAt(LocalDateTime.now());

            System.out.println("[분석] 분석 완료: " + fileInfo.getFileName() +
                    " → " + fileInfo.getDetectedCategory() +
                    " (신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()) + ")");

            return fileInfo;

        } catch (Exception e) {
            System.err.println("[ERROR] 파일 분석 실패: " + filePath + " - " + e.getMessage());

            // 에러 FileInfo 생성
            FileInfo errorInfo = new FileInfo();
            errorInfo.setFilePath(filePath);
            errorInfo.setFileName(Paths.get(filePath).getFileName().toString());
            errorInfo.setStatus(ProcessingStatus.FAILED);
            errorInfo.setErrorMessage(e.getMessage());
            errorInfo.setDetectedCategory("Unknown");
            errorInfo.setConfidenceScore(0.0);

            return errorInfo;
        }
    }

    /**
     * AI를 사용한 파일 분석 (실제 OpenAI API 연동)
     */
    public FileInfo analyzeFileWithAI(String filePath) {
        if (!isAIAnalysisAvailable()) {
            throw new IllegalStateException("AI 분석이 활성화되지 않았습니다");
        }

        try {
            // 기본 분석 먼저 수행
            FileInfo fileInfo = analyzeFile(filePath);

            // AI 분석이 가치 있는 파일인지 확인
            if (!isAIAnalysisWorthwhile(fileInfo)) {
                System.out.println("[AI] AI 분석 비적합 파일: " + fileInfo.getFileName());
                return fileInfo;
            }

            System.out.println("[AI] 🤖 AI 분석 시작: " + fileInfo.getFileName());

            // 실제 AI 분석 수행
            String aiResponse = aiAnalyzer.analyzeFile(fileInfo);

            if (aiResponse != null) {
                boolean applied = aiAnalyzer.applyAIAnalysis(fileInfo, aiResponse);

                if (applied) {
                    // AI 분석이 성공한 경우 신뢰도 향상
                    double currentScore = fileInfo.getConfidenceScore();
                    double enhancedScore = Math.min(currentScore + 0.2, 1.0);
                    fileInfo.setConfidenceScore(enhancedScore);

                    System.out.println("[AI] ✅ AI 분석 적용 완료: " + fileInfo.getFileName() +
                            " (신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()) + ")");

                    // AI 분석 마커 추가
                    if (fileInfo.getKeywords() == null) {
                        fileInfo.setKeywords(new ArrayList<>());
                    }
                    if (!fileInfo.getKeywords().contains("ai-analyzed")) {
                        fileInfo.getKeywords().add("ai-analyzed");
                    }
                } else {
                    System.out.println("[AI] ⚠️ AI 분석 응답 파싱 실패: " + fileInfo.getFileName());
                }
            } else {
                System.out.println("[AI] ⚠️ AI 분석 응답 없음: " + fileInfo.getFileName());
            }

            return fileInfo;

        } catch (Exception e) {
            System.err.println("[ERROR] AI 파일 분석 실패: " + filePath + " - " + e.getMessage());
            throw new RuntimeException("AI 분석 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 분석 시뮬레이션 (실제 API 연동 전까지 사용)
     */
    private void simulateAIAnalysis(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName().toLowerCase();
        String extension = fileInfo.getFileExtension().toLowerCase();

        // 파일명과 확장자를 기반으로 더 정확한 분류 시뮬레이션
        if (fileName.contains("invoice") || fileName.contains("receipt") || fileName.contains("bill")) {
            fileInfo.setDetectedCategory("Documents");
            fileInfo.setDetectedSubCategory("Financial");
            fileInfo.setConfidenceScore(0.95);
            fileInfo.setDescription("AI 분석: 재무 관련 문서로 분류됨");
        } else if (fileName.contains("resume") || fileName.contains("cv")) {
            fileInfo.setDetectedCategory("Documents");
            fileInfo.setDetectedSubCategory("Resume");
            fileInfo.setConfidenceScore(0.92);
            fileInfo.setDescription("AI 분석: 이력서로 분류됨");
        } else if (fileName.contains("screenshot") || fileName.contains("screen")) {
            fileInfo.setDetectedCategory("Images");
            fileInfo.setDetectedSubCategory("Screenshots");
            fileInfo.setConfidenceScore(0.88);
            fileInfo.setDescription("AI 분석: 스크린샷으로 분류됨");
        } else if (fileName.contains("wallpaper") || fileName.contains("background")) {
            fileInfo.setDetectedCategory("Images");
            fileInfo.setDetectedSubCategory("Wallpapers");
            fileInfo.setConfidenceScore(0.85);
            fileInfo.setDescription("AI 분석: 배경화면으로 분류됨");
        } else {
            // 기본 확장자 기반 분류에 AI 신뢰도 보너스 적용
            classifyBasicCategory(fileInfo);
            fileInfo.setConfidenceScore(Math.min(fileInfo.getConfidenceScore() + 0.15, 1.0));
            fileInfo.setDescription("AI 분석: 향상된 분류 신뢰도");
        }

        // AI 키워드 추가
        List<String> aiKeywords = generateAIKeywords(fileName, extension);
        if (fileInfo.getKeywords() == null) {
            fileInfo.setKeywords(new ArrayList<>());
        }
        fileInfo.getKeywords().addAll(aiKeywords);
    }

    /**
     * AI 키워드 생성 시뮬레이션
     */
    private List<String> generateAIKeywords(String fileName, String extension) {
        List<String> keywords = new ArrayList<>();

        // 파일명에서 키워드 추출
        String[] words = fileName.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ").split("\\s+");
        for (String word : words) {
            if (word.length() > 2) {
                keywords.add(word.toLowerCase());
            }
        }

        // 확장자 기반 키워드
        switch (extension) {
            case "pdf":
                keywords.add("document");
                keywords.add("portable");
                break;
            case "jpg":
            case "jpeg":
            case "png":
                keywords.add("image");
                keywords.add("photo");
                break;
            case "mp4":
            case "avi":
                keywords.add("video");
                keywords.add("media");
                break;
        }

        return keywords;
    }

    /**
     * 기본 파일 정보 생성
     */
    private FileInfo createBasicFileInfo(File file, Path path) throws IOException {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setFilePath(path.toString());
        fileInfo.setFileName(file.getName());
        fileInfo.setOriginalLocation(file.getParent());
        fileInfo.setFileSize(file.length());
        fileInfo.setFileExtension(getFileExtension(file.getName()));

        // 날짜 정보
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        fileInfo.setCreatedDate(LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault()));
        fileInfo.setModifiedDate(LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

        // 키워드 리스트 초기화
        fileInfo.setKeywords(new ArrayList<>());

        return fileInfo;
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

        // 숨김 파일 여부 확인
        if (Files.isHidden(path)) {
            fileInfo.getKeywords().add("hidden");
        }

        // 읽기 전용 파일 확인
        if (!Files.isWritable(path)) {
            fileInfo.getKeywords().add("readonly");
        }
    }

    /**
     * MIME 타입 감지
     */
    private void detectMimeType(FileInfo fileInfo, Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            fileInfo.setMimeType(mimeType != null ? mimeType : "application/octet-stream");
        } catch (IOException e) {
            // MIME 타입 감지 실패는 치명적이지 않음
            fileInfo.setMimeType("application/octet-stream");
        }
    }

    /**
     * 기본 카테고리 분류 (커스텀 규칙 우선 적용)
     */
    private void classifyBasicCategory(FileInfo fileInfo) {
        // 1. 커스텀 규칙 사용 시 우선 적용
        if (FileTypeDetector.isCustomRulesEnabled()) {
            String customCategory = FileTypeDetector.detectCategoryWithCustomRules(fileInfo.getFilePath());
            if (customCategory != null && !customCategory.equals("Others")) {
                fileInfo.setDetectedCategory(customCategory);
                return;
            }
        }
        
        // 2. 기본 확장자 기반 분류 (폴백)
        String extension = fileInfo.getFileExtension().toLowerCase();

        // 문서 파일
        if (extension.matches("pdf|doc|docx|txt|rtf|odt|pages")) {
            fileInfo.setDetectedCategory("Documents");
        }
        // 스프레드시트
        else if (extension.matches("xls|xlsx|csv|ods|numbers")) {
            fileInfo.setDetectedCategory("Spreadsheets");
        }
        // 프레젠테이션
        else if (extension.matches("ppt|pptx|odp|key")) {
            fileInfo.setDetectedCategory("Presentations");
        }
        // 이미지
        else if (extension.matches("jpg|jpeg|png|gif|bmp|tiff|webp|svg|ico|heic|raw")) {
            fileInfo.setDetectedCategory("Images");
        }
        // 비디오
        else if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ogv")) {
            fileInfo.setDetectedCategory("Videos");
        }
        // 오디오
        else if (extension.matches("mp3|wav|flac|aac|ogg|wma|m4a|opus")) {
            fileInfo.setDetectedCategory("Audio");
        }
        // 압축파일
        else if (extension.matches("zip|rar|7z|tar|gz|bz2|xz")) {
            fileInfo.setDetectedCategory("Archives");
        }
        // 실행파일
        else if (extension.matches("exe|msi|dmg|pkg|deb|rpm|app")) {
            fileInfo.setDetectedCategory("Applications");
        }
        // 코드 파일
        else if (extension.matches("java|py|js|html|css|cpp|c|h|php|rb|go|rs")) {
            fileInfo.setDetectedCategory("Code");
        }
        // 폰트 파일
        else if (extension.matches("ttf|otf|woff|woff2|eot")) {
            fileInfo.setDetectedCategory("Fonts");
        }
        // 전자책
        else if (extension.matches("epub|mobi|azw|azw3|fb2")) {
            fileInfo.setDetectedCategory("Ebooks");
        }
        // 기타
        else {
            fileInfo.setDetectedCategory("Others");
        }
    }

    /**
     * 파일명 분석
     */
    private void analyzeFileName(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName().toLowerCase();

        // 날짜 패턴 추출
        Pattern datePattern = Pattern.compile("(\\d{4})[\\-_](\\d{2})[\\-_](\\d{2})");
        Matcher dateMatcher = datePattern.matcher(fileName);

        if (dateMatcher.find()) {
            String year = dateMatcher.group(1);
            String month = dateMatcher.group(2);
            fileInfo.getKeywords().add("date:" + year + "-" + month);
        }

        // 키워드 추출 (단어 기반)
        String[] words = fileName.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ").split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && !word.matches("\\d+")) {
                fileInfo.getKeywords().add(word.toLowerCase());
            }
        }

        // 제목 추출 (확장자 제거 후 정리)
        String title = fileName.substring(0, fileName.lastIndexOf('.') > 0 ?
                fileName.lastIndexOf('.') : fileName.length());
        title = title.replaceAll("[\\-_]", " ");
        fileInfo.setExtractedTitle(title);
    }

    /**
     * 세부 카테고리 분류
     */
    private void classifyDetailedCategory(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName().toLowerCase();
        String category = fileInfo.getDetectedCategory();

        if ("Images".equals(category)) {
            if (fileName.contains("screenshot") || fileName.contains("screen shot")) {
                fileInfo.setDetectedSubCategory("Screenshots");
            } else if (fileName.contains("wallpaper") || fileName.contains("background")) {
                fileInfo.setDetectedSubCategory("Wallpapers");
            } else if (fileName.contains("meme") || fileName.contains("funny")) {
                fileInfo.setDetectedSubCategory("Memes");
            } else if (fileName.matches(".*\\d{8}.*") || fileName.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                fileInfo.setDetectedSubCategory("Photos");
            } else {
                fileInfo.setDetectedSubCategory("General");
            }
        } else if ("Documents".equals(category)) {
            if (fileName.contains("resume") || fileName.contains("cv")) {
                fileInfo.setDetectedSubCategory("Resume");
            } else if (fileName.contains("invoice") || fileName.contains("receipt")) {
                fileInfo.setDetectedSubCategory("Financial");
            } else if (fileName.contains("manual") || fileName.contains("guide")) {
                fileInfo.setDetectedSubCategory("Manuals");
            } else if (fileName.contains("report")) {
                fileInfo.setDetectedSubCategory("Reports");
            } else {
                fileInfo.setDetectedSubCategory("General");
            }
        } else if ("Videos".equals(category)) {
            if (fileName.contains("tutorial") || fileName.contains("course")) {
                fileInfo.setDetectedSubCategory("Educational");
            } else if (fileName.contains("movie") || fileName.contains("film")) {
                fileInfo.setDetectedSubCategory("Movies");
            } else if (fileName.contains("tv") || fileName.contains("episode")) {
                fileInfo.setDetectedSubCategory("TV Shows");
            } else {
                fileInfo.setDetectedSubCategory("General");
            }
        } else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 기본 신뢰도 계산
     */
    private void calculateBasicConfidence(FileInfo fileInfo) {
        double confidence = 0.5; // 기본 신뢰도

        // 확장자 기반 신뢰도
        if (!fileInfo.getFileExtension().isEmpty()) {
            confidence += 0.3;
        }

        // 파일명에 의미있는 키워드가 있으면 신뢰도 증가
        if (fileInfo.getKeywords().size() > 2) {
            confidence += 0.1;
        }

        // 세부 카테고리가 있으면 신뢰도 증가
        if (!"General".equals(fileInfo.getDetectedSubCategory())) {
            confidence += 0.1;
        }

        fileInfo.setConfidenceScore(Math.min(confidence, 1.0));
    }

    /**
     * AI로 분석 향상
     */
    private void enhanceWithAI(FileInfo fileInfo) {
        try {
            if (!isAIAnalysisAvailable()) {
                return;
            }

            // AI 분석이 가치 있는 파일인지 확인
            if (!isAIAnalysisWorthwhile(fileInfo)) {
                return;
            }

            System.out.println("[AI] 🤖 AI 분석 시작: " + fileInfo.getFileName());

            // 실제 AI 분석 수행
            String aiResponse = aiAnalyzer.analyzeFile(fileInfo);

            if (aiResponse != null) {
                boolean applied = aiAnalyzer.applyAIAnalysis(fileInfo, aiResponse);

                if (applied) {
                    // AI 분석이 성공한 경우 신뢰도 향상
                    double currentScore = fileInfo.getConfidenceScore();
                    double enhancedScore = Math.min(currentScore + 0.2, 1.0);
                    fileInfo.setConfidenceScore(enhancedScore);

                    System.out.println("[AI] ✅ AI 분석 완료: " + fileInfo.getFileName() +
                            " (향상된 신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()) + ")");

                    // AI 분석 마커 추가
                    if (fileInfo.getKeywords() == null) {
                        fileInfo.setKeywords(new ArrayList<>());
                    }
                    if (!fileInfo.getKeywords().contains("ai-analyzed")) {
                        fileInfo.getKeywords().add("ai-analyzed");
                    }
                } else {
                    System.out.println("[AI] ⚠️ AI 분석 응답 파싱 실패: " + fileInfo.getFileName());
                }
            } else {
                System.out.println("[AI] ⚠️ AI 분석 응답 없음: " + fileInfo.getFileName());
            }

        } catch (Exception e) {
            System.err.println("[ERROR] AI 분석 중 오류: " + fileInfo.getFileName() + " - " + e.getMessage());
            // AI 분석 실패는 전체 분석을 중단하지 않음
        }
    }

    /**
     * AI 분석이 가치 있는 파일 타입인지 확인
     */
    private boolean isAIAnalysisWorthwhile(FileInfo fileInfo) {
        if (fileInfo == null) {
            return false;
        }

        String extension = fileInfo.getFileExtension().toLowerCase();

        // 문서 파일: AI가 내용을 분석해서 정확한 분류 가능
        if (extension.matches("pdf|doc|docx|txt|rtf|odt|ppt|pptx|xls|xlsx")) {
            return true;
        }

        // 이미지 파일: 스크린샷, 사진 등 구분 가능
        if (extension.matches("jpg|jpeg|png|gif|bmp|webp|tiff")) {
            return true;
        }

        // 비디오 파일: 제목으로 콘텐츠 유형 판단 가능
        if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm")) {
            return true;
        }

        // 코드 파일: 프로젝트 유형이나 언어별 분류 가능
        if (extension.matches("java|py|js|html|css|cpp|c|h|php|rb|go|rs")) {
            return true;
        }

        // 기타 확장자가 모호한 경우
        if (extension.isEmpty() || extension.equals("tmp") ||
                extension.equals("dat") || extension.equals("unknown")) {
            return true;
        }

        return false;
    }

    /**
     * 추천 경로 결정
     */
    private void determineSuggestedPath(FileInfo fileInfo) {
        AppConfig config = configService.getCurrentConfig();
        String basePath = config.getOrganizationRootFolder();
        String category = fileInfo.getDetectedCategory();
        String subCategory = fileInfo.getDetectedSubCategory();

        StringBuilder pathBuilder = new StringBuilder(basePath);
        pathBuilder.append(File.separator).append(category);

        if (subCategory != null && !subCategory.isEmpty() && !"General".equals(subCategory)) {
            pathBuilder.append(File.separator).append(subCategory);
        }

        // 날짜별 정리 (설정에 따라)
        if (config.isOrganizeByDate()) {
            LocalDateTime fileDate = fileInfo.getModifiedDate();
            pathBuilder.append(File.separator)
                    .append(fileDate.getYear())
                    .append(File.separator)
                    .append(String.format("%02d-%s",
                            fileDate.getMonthValue(),
                            fileDate.getMonth().name().substring(0, 3)));
        }

        fileInfo.setSuggestedPath(pathBuilder.toString());
    }

    /**
     * 배치 파일 분석 (AI 포함)
     */
    public List<FileInfo> analyzeFiles(List<String> filePaths) {
        List<FileInfo> results = new ArrayList<>();

        System.out.println("[분석] 배치 파일 분석 시작: " + filePaths.size() + "개 파일");

        for (String filePath : filePaths) {
            FileInfo analyzed = analyzeFile(filePath);
            results.add(analyzed);
        }

        // AI 분석된 파일 수 확인
        long aiAnalyzedCount = results.stream()
                .filter(f -> f.getKeywords() != null && f.getKeywords().contains("ai-analyzed"))
                .count();

        System.out.println("[분석] 배치 분석 완료: " + results.size() + "개 파일, AI 분석: " + aiAnalyzedCount + "개");

        return results;
    }

    /**
     * AI 분석기 상태 확인
     */
    public boolean isAIEnabled() {
        return aiAnalyzer != null && aiAnalyzer.isEnabled();
    }

    /**
     * 배치 AI 분석
     */
    public Map<String, String> performBatchAIAnalysis(List<FileInfo> files) {
        if (!isAIAnalysisAvailable()) {
            System.out.println("[INFO] AI 분석이 비활성화되어 있습니다.");
            return new HashMap<>();
        }

        return aiAnalyzer.analyzeBatch(files);
    }

    /**
     * AI 설정 요약 정보
     */
    public String getAIConfigSummary() {
        if (aiAnalyzer != null) {
            return aiAnalyzer.getConfigSummary();
        }
        return "AI 분석기가 초기화되지 않았습니다.";
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }

    // 진행률 콜백 인터페이스 (FileScanService와 동일)
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total, String currentFile);
    }
    
    private ProgressCallback progressCallback;
    
    /**
     * 진행률 콜백 설정
     */
    public void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }
    
    /**
     * 배치 분석 메서드 (FileOperationController에서 사용)
     */
    public void analyzeBatch(javafx.collections.ObservableList<com.smartfilemanager.model.FileInfo> fileList) throws Exception {
        System.out.println("[정보] AI 배치 분석 시작: " + fileList.size() + "개 파일");
        
        if (!aiAnalysisEnabled) {
            throw new Exception("AI 분석이 활성화되지 않았습니다. 설정에서 AI API 키를 확인해주세요.");
        }
        
        int totalFiles = fileList.size();
        int processedCount = 0;
        
        for (com.smartfilemanager.model.FileInfo fileInfo : fileList) {
            try {
                processedCount++;
                
                // AI 분석 수행
                if (fileInfo.getDetectedCategory() == null || fileInfo.getDetectedCategory().equals("Others")) {
                    // 기본 분석이 되지 않은 파일에 대해 AI 분석 수행
                    enhanceWithAI(fileInfo);
                    System.out.println("[AI 분석] " + fileInfo.getFileName() + " → " + fileInfo.getDetectedCategory());
                }
                
                // 진행률 콜백 호출
                if (progressCallback != null) {
                    progressCallback.onProgress(processedCount, totalFiles, fileInfo.getFileName());
                }
                
                // AI API 호출 제한을 위한 지연
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.err.println("[경고] AI 분석 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
            }
        }
        
        System.out.println("[완료] AI 배치 분석 완료: " + processedCount + "/" + totalFiles + " 파일 처리");
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        if (aiAnalyzer != null) {
            aiAnalyzer.shutdown();
        }
        System.out.println("[종료] 파일 분석 서비스 종료");
    }
}