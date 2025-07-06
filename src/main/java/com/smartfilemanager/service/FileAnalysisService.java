package com.smartfilemanager.service;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.util.AIAnalyzer;

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
 * 파일 분석 서비스 (AI 분석 연동 완성 버전)
 * 파일의 메타데이터, 내용, 패턴을 분석해서 정확한 분류를 수행합니다
 */
public class FileAnalysisService {

    private final ConfigService configService;
    private final AIAnalyzer aiAnalyzer;
    private AppConfig currentConfig;

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
            aiAnalyzer.setApiKey(currentConfig.getAiApiKey());
            aiAnalyzer.setModel(currentConfig.getAiModel() != null ?
                    currentConfig.getAiModel() : "gpt-3.5-turbo");

            System.out.println("[AI] AI 분석 시스템 초기화 완료");
            System.out.println(aiAnalyzer.getConfigSummary());

            // API 키 유효성 검사 (비동기)
            validateApiKeyAsync();
        } else {
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
                }
            } catch (Exception e) {
                System.err.println("[ERROR] API 키 검증 중 오류: " + e.getMessage());
            }
        }).start();
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

            // 8. 신뢰도 점수 계산 (AI 분석 전)
            calculateBasicConfidenceScore(fileInfo);

            // 9. AI 분석 (설정이 활성화된 경우)
            if (shouldUseAIAnalysis(fileInfo)) {
                enhanceWithAIAnalysis(fileInfo);
            }

            // 10. 최종 신뢰도 점수 재계산
            calculateFinalConfidenceScore(fileInfo);

            // 11. 추천 경로 생성
            generateSuggestedPath(fileInfo);

            fileInfo.setStatus(ProcessingStatus.ANALYZED);
            fileInfo.setProcessedAt(LocalDateTime.now());

            System.out.println("[분석] ✅ 파일 분석 완료: " + fileInfo.getFileName() +
                    " (카테고리: " + fileInfo.getDetectedCategory() +
                    ", 신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()) + ")");

            return fileInfo;

        } catch (Exception e) {
            System.err.println("[ERROR] 파일 분석 실패: " + filePath + " - " + e.getMessage());

            FileInfo errorInfo = new FileInfo();
            errorInfo.setFilePath(filePath);
            errorInfo.setFileName(Paths.get(filePath).getFileName().toString());
            errorInfo.setStatus(ProcessingStatus.FAILED);
            errorInfo.setErrorMessage(e.getMessage());
            return errorInfo;
        }
    }

    /**
     * AI 분석 사용 여부 판단
     */
    private boolean shouldUseAIAnalysis(FileInfo fileInfo) {
        // AI 분석이 비활성화된 경우
        if (!aiAnalyzer.isEnabled()) {
            return false;
        }

        // 신뢰도가 이미 매우 높은 경우 AI 분석 생략 (성능 최적화)
        if (fileInfo.getConfidenceScore() > 0.95) {
            System.out.println("[AI] 신뢰도가 높아 AI 분석 생략: " + fileInfo.getFileName());
            return false;
        }

        // 파일 크기가 너무 큰 경우 제외
        long maxSizeForAI = currentConfig.getMaxFileSizeForAnalysis() * 1024 * 1024L; // MB to bytes
        if (fileInfo.getFileSize() > maxSizeForAI) {
            System.out.println("[AI] 파일 크기 초과로 AI 분석 생략: " + fileInfo.getFileName());
            return false;
        }

        // AI 분석이 유용한 파일 타입만 선택
        String extension = fileInfo.getFileExtension().toLowerCase();
        boolean worthwhile = isAIAnalysisWorthwhile(extension);

        if (!worthwhile) {
            System.out.println("[AI] AI 분석 비적합 파일 타입: " + fileInfo.getFileName());
        }

        return worthwhile;
    }

    /**
     * AI 분석이 가치 있는 파일 타입인지 확인
     */
    private boolean isAIAnalysisWorthwhile(String extension) {
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
        if (extension.isEmpty() || extension.equals("tmp") || extension.equals("dat") || extension.equals("unknown")) {
            return true;
        }

        return false;
    }

    /**
     * AI 분석으로 파일 정보 향상
     */
    private void enhanceWithAIAnalysis(FileInfo fileInfo) {
        try {
            System.out.println("[AI] 🤖 AI 분석 시작: " + fileInfo.getFileName());

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

        } catch (Exception e) {
            System.err.println("[ERROR] AI 분석 중 오류: " + fileInfo.getFileName() + " - " + e.getMessage());
            // AI 분석 실패는 전체 분석을 중단하지 않음
        }
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
            fileInfo.addKeyword("hidden");
        }

        // 읽기 전용 파일 확인
        if (!Files.isWritable(path)) {
            fileInfo.addKeyword("readonly");
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
     * 기본 카테고리 분류
     */
    private void classifyBasicCategory(FileInfo fileInfo) {
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
        else if (extension.matches("zip|rar|7z|tar|gz|bz2|xz|lz|lzma")) {
            fileInfo.setDetectedCategory("Archives");
        }
        // 실행파일
        else if (extension.matches("exe|msi|dmg|app|deb|rpm|pkg")) {
            fileInfo.setDetectedCategory("Applications");
        }
        // 코드 파일
        else if (extension.matches("java|py|js|html|css|cpp|c|h|php|rb|go|rs|kt|swift|ts")) {
            fileInfo.setDetectedCategory("Code");
        }
        // 시스템 파일
        else if (extension.matches("dll|sys|ini|cfg|conf|log")) {
            fileInfo.setDetectedCategory("System");
        }
        // 기타
        else {
            fileInfo.setDetectedCategory("Others");
        }
    }

    /**
     * 스마트 서브카테고리 분류
     */
    private void classifySmartSubCategory(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName().toLowerCase();
        String category = fileInfo.getDetectedCategory();

        switch (category) {
            case "Images":
                analyzeImageSubCategory(fileInfo, fileName);
                break;
            case "Documents":
                analyzeDocumentSubCategory(fileInfo, fileName);
                break;
            case "Videos":
                analyzeVideoSubCategory(fileInfo, fileName);
                break;
            case "Audio":
                analyzeAudioSubCategory(fileInfo, fileName);
                break;
            case "Code":
                analyzeCodeSubCategory(fileInfo, fileName);
                break;
        }
    }

    /**
     * 이미지 서브카테고리 분석
     */
    private void analyzeImageSubCategory(FileInfo fileInfo, String fileName) {
        if (fileName.contains("screenshot") || fileName.contains("screen shot") ||
                fileName.contains("capture") || fileName.contains("snap") || fileName.startsWith("screenshot")) {
            fileInfo.setDetectedSubCategory("Screenshots");
        }
        else if (fileName.contains("wallpaper") || fileName.contains("background") ||
                fileName.contains("desktop") || fileName.contains("wp")) {
            fileInfo.setDetectedSubCategory("Wallpapers");
        }
        else if (fileName.contains("profile") || fileName.contains("avatar") ||
                fileName.contains("headshot")) {
            fileInfo.setDetectedSubCategory("Profiles");
        }
        else if (fileName.contains("icon") || fileName.contains("logo") ||
                fileName.contains("favicon")) {
            fileInfo.setDetectedSubCategory("Icons");
        }
        else if (fileName.contains("meme") || fileName.contains("funny") ||
                fileName.contains("comic")) {
            fileInfo.setDetectedSubCategory("Memes");
        }
        else if (fileName.matches(".*\\d{8}.*") || fileName.matches(".*\\d{4}-\\d{2}-\\d{2}.*") ||
                fileName.matches(".*img_\\d+.*") || fileName.matches(".*dsc\\d+.*")) {
            fileInfo.setDetectedSubCategory("Photos");
        }
        else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 문서 서브카테고리 분석
     */
    private void analyzeDocumentSubCategory(FileInfo fileInfo, String fileName) {
        if (fileName.contains("resume") || fileName.contains("cv") || fileName.contains("이력서")) {
            fileInfo.setDetectedSubCategory("Resume");
        }
        else if (fileName.contains("invoice") || fileName.contains("receipt") ||
                fileName.contains("bill") || fileName.contains("계산서") || fileName.contains("영수증")) {
            fileInfo.setDetectedSubCategory("Financial");
        }
        else if (fileName.contains("manual") || fileName.contains("guide") ||
                fileName.contains("instruction") || fileName.contains("설명서") || fileName.contains("매뉴얼")) {
            fileInfo.setDetectedSubCategory("Manuals");
        }
        else if (fileName.contains("report") || fileName.contains("analysis") ||
                fileName.contains("보고서") || fileName.contains("분석")) {
            fileInfo.setDetectedSubCategory("Reports");
        }
        else if (fileName.contains("contract") || fileName.contains("agreement") ||
                fileName.contains("계약") || fileName.contains("협약")) {
            fileInfo.setDetectedSubCategory("Legal");
        }
        else if (fileName.contains("homework") || fileName.contains("assignment") ||
                fileName.contains("과제") || fileName.contains("숙제")) {
            fileInfo.setDetectedSubCategory("Educational");
        }
        else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 비디오 서브카테고리 분석
     */
    private void analyzeVideoSubCategory(FileInfo fileInfo, String fileName) {
        if (fileName.contains("tutorial") || fileName.contains("course") ||
                fileName.contains("lesson") || fileName.contains("lecture") ||
                fileName.contains("강의") || fileName.contains("튜토리얼")) {
            fileInfo.setDetectedSubCategory("Educational");
        }
        else if (fileName.contains("movie") || fileName.contains("film") ||
                fileName.contains("영화") || fileName.contains("cinema")) {
            fileInfo.setDetectedSubCategory("Movies");
        }
        else if (fileName.contains("tv") || fileName.contains("episode") ||
                fileName.contains("series") || fileName.contains("드라마") || fileName.contains("시리즈")) {
            fileInfo.setDetectedSubCategory("TV Shows");
        }
        else if (fileName.contains("music") || fileName.contains("concert") ||
                fileName.contains("뮤직비디오") || fileName.contains("콘서트")) {
            fileInfo.setDetectedSubCategory("Music Videos");
        }
        else if (fileName.contains("clip") || fileName.contains("short") ||
                fileName.contains("클립") || fileName.contains("쇼츠")) {
            fileInfo.setDetectedSubCategory("Clips");
        }
        else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 오디오 서브카테고리 분석
     */
    private void analyzeAudioSubCategory(FileInfo fileInfo, String fileName) {
        if (fileName.contains("podcast") || fileName.contains("interview") ||
                fileName.contains("팟캐스트") || fileName.contains("인터뷰")) {
            fileInfo.setDetectedSubCategory("Podcasts");
        }
        else if (fileName.contains("audiobook") || fileName.contains("book") ||
                fileName.contains("오디오북") || fileName.contains("책")) {
            fileInfo.setDetectedSubCategory("Audiobooks");
        }
        else if (fileName.contains("music") || fileName.contains("song") ||
                fileName.contains("음악") || fileName.contains("노래")) {
            fileInfo.setDetectedSubCategory("Music");
        }
        else if (fileName.contains("voice") || fileName.contains("memo") ||
                fileName.contains("음성") || fileName.contains("메모")) {
            fileInfo.setDetectedSubCategory("Voice Memos");
        }
        else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 코드 서브카테고리 분석
     */
    private void analyzeCodeSubCategory(FileInfo fileInfo, String fileName) {
        String extension = fileInfo.getFileExtension().toLowerCase();

        if (extension.matches("java|kt|scala")) {
            fileInfo.setDetectedSubCategory("Java/Kotlin");
        }
        else if (extension.matches("py|pyw")) {
            fileInfo.setDetectedSubCategory("Python");
        }
        else if (extension.matches("js|ts|jsx|tsx")) {
            fileInfo.setDetectedSubCategory("JavaScript/TypeScript");
        }
        else if (extension.matches("html|css|scss|sass")) {
            fileInfo.setDetectedSubCategory("Web");
        }
        else if (extension.matches("cpp|c|h|hpp")) {
            fileInfo.setDetectedSubCategory("C/C++");
        }
        else if (extension.matches("go|rs|swift")) {
            fileInfo.setDetectedSubCategory("Modern Languages");
        }
        else {
            fileInfo.setDetectedSubCategory("General");
        }
    }

    /**
     * 파일명 패턴 분석
     */
    private void analyzeFileNamePatterns(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName();
        List<String> keywords = fileInfo.getKeywords();

        // 날짜 패턴 추출
        extractDatePatterns(fileName, keywords);

        // 버전 패턴 추출
        extractVersionPatterns(fileName, keywords);

        // 단어 키워드 추출
        extractWordKeywords(fileName, keywords);

        // 제목 정리
        String title = fileName.substring(0, fileName.lastIndexOf('.') > 0 ?
                fileName.lastIndexOf('.') : fileName.length());
        title = title.replaceAll("[\\-_]", " ").trim();
        fileInfo.setExtractedTitle(title);
    }

    /**
     * 날짜 패턴 추출
     */
    private void extractDatePatterns(String fileName, List<String> keywords) {
        // YYYY-MM-DD 형식
        Pattern datePattern1 = Pattern.compile("(\\d{4})[\\-_](\\d{2})[\\-_](\\d{2})");
        Matcher matcher1 = datePattern1.matcher(fileName);
        if (matcher1.find()) {
            keywords.add("date:" + matcher1.group(1) + "-" + matcher1.group(2) + "-" + matcher1.group(3));
        }

        // YYYYMMDD 형식
        Pattern datePattern2 = Pattern.compile("(\\d{8})");
        Matcher matcher2 = datePattern2.matcher(fileName);
        if (matcher2.find()) {
            String dateStr = matcher2.group(1);
            keywords.add("date:" + dateStr.substring(0, 4) + "-" +
                    dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8));
        }
    }

    /**
     * 버전 패턴 추출
     */
    private void extractVersionPatterns(String fileName, List<String> keywords) {
        Pattern versionPattern = Pattern.compile("v(\\d+\\.\\d+(?:\\.\\d+)?)");
        Matcher matcher = versionPattern.matcher(fileName.toLowerCase());
        if (matcher.find()) {
            keywords.add("version:" + matcher.group(1));
        }
    }

    /**
     * 단어 키워드 추출
     */
    private void extractWordKeywords(String fileName, List<String> keywords) {
        // 특수문자 제거 후 단어 분리
        String[] words = fileName.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ").split("\\s+");

        for (String word : words) {
            if (word.length() > 2 && !isCommonWord(word.toLowerCase())) {
                if (!keywords.contains(word.toLowerCase())) {
                    keywords.add(word.toLowerCase());
                }
            }
        }
    }

    /**
     * 일반적인 단어인지 확인 (키워드에서 제외)
     */
    private boolean isCommonWord(String word) {
        String[] commonWords = {"the", "and", "for", "are", "but", "not", "you", "all",
                "can", "had", "her", "was", "one", "our", "out", "day",
                "get", "has", "him", "his", "how", "man", "new", "now",
                "old", "see", "two", "way", "who", "boy", "did", "its",
                "let", "put", "say", "she", "too", "use", "file", "document"};

        return Arrays.asList(commonWords).contains(word);
    }

    /**
     * 파일 내용 분석 여부 판단
     */
    private boolean shouldAnalyzeContent(FileInfo fileInfo) {
        // 파일 크기 제한
        long maxSize = currentConfig.getMaxFileSizeForAnalysis() * 1024 * 1024L; // MB to bytes
        if (fileInfo.getFileSize() > maxSize) {
            return false;
        }

        // 내용 분석이 유용한 파일 타입
        String extension = fileInfo.getFileExtension().toLowerCase();
        return extension.matches("txt|log|md|readme|json|xml|csv");
    }

    /**
     * 파일 내용 분석
     */
    private void analyzeFileContent(FileInfo fileInfo) {
        try {
            String extension = fileInfo.getFileExtension().toLowerCase();

            if ("txt".equals(extension) || "log".equals(extension) || "md".equals(extension)) {
                String content = Files.readString(Paths.get(fileInfo.getFilePath()));
                analyzeTextContent(fileInfo, content);
            }
            // 추후 PDF, DOC 등은 별도의 라이브러리 추가 가능

        } catch (Exception e) {
            System.err.println("[WARNING] 내용 분석 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
        }
    }

    /**
     * 텍스트 내용 분석
     */
    private void analyzeTextContent(FileInfo fileInfo, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 첫 200자를 설명으로 사용
        String description = content.substring(0, Math.min(content.length(), 200)).trim();
        fileInfo.setDescription(description);

        // 간단한 키워드 추출
        String[] words = content.toLowerCase().split("\\s+");
        List<String> keywords = fileInfo.getKeywords();

        for (String word : words) {
            if (word.length() > 4 && !isCommonWord(word) && keywords.size() < 15) {
                if (!keywords.contains(word)) {
                    keywords.add(word);
                }
            }
        }
    }

    /**
     * 기본 신뢰도 점수 계산 (AI 분석 전)
     */
    private void calculateBasicConfidenceScore(FileInfo fileInfo) {
        double score = 0.0;

        // 확장자 기반 점수 (기본)
        if (!fileInfo.getFileExtension().isEmpty()) {
            score += 0.3;
        }

        // 카테고리 분류 점수
        if (fileInfo.getDetectedCategory() != null && !fileInfo.getDetectedCategory().equals("Others")) {
            score += 0.4;
        }

        // 서브카테고리 분류 점수
        if (fileInfo.getDetectedSubCategory() != null && !fileInfo.getDetectedSubCategory().equals("General")) {
            score += 0.2;
        }

        // 키워드 추출 점수
        if (fileInfo.getKeywords() != null && !fileInfo.getKeywords().isEmpty()) {
            score += 0.1;
        }

        fileInfo.setConfidenceScore(Math.min(score, 1.0));
    }

    /**
     * 최종 신뢰도 점수 재계산 (AI 분석 후)
     */
    private void calculateFinalConfidenceScore(FileInfo fileInfo) {
        // AI 분석이 적용된 경우 신뢰도는 이미 조정됨
        if (fileInfo.getKeywords() != null && fileInfo.getKeywords().contains("ai-analyzed")) {
            // AI 분석 결과가 있으면 추가 보너스
            double currentScore = fileInfo.getConfidenceScore();
            fileInfo.setConfidenceScore(Math.min(currentScore + 0.05, 1.0));
        }
    }

    /**
     * 추천 경로 생성
     */
    private void generateSuggestedPath(FileInfo fileInfo) {
        StringBuilder pathBuilder = new StringBuilder();

        // 루트 폴더
        pathBuilder.append(currentConfig.getOrganizationRootFolder());

        // 카테고리 폴더
        if (fileInfo.getDetectedCategory() != null) {
            pathBuilder.append(File.separator).append(fileInfo.getDetectedCategory());
        }

        // 서브카테고리 폴더
        if (fileInfo.getDetectedSubCategory() != null &&
                !fileInfo.getDetectedSubCategory().equals("General")) {
            pathBuilder.append(File.separator).append(fileInfo.getDetectedSubCategory());
        }

        // 날짜별 정리 (설정에 따라)
        if (currentConfig.isOrganizeByDate()) {
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
     * 설정 새로고침
     */
    public void refreshConfig() {
        this.currentConfig = configService.getCurrentConfig();
        initializeAI();
        System.out.println("[설정] 파일 분석 서비스 설정 새로고침 완료");
    }

    /**
     * AI 분석기 상태 확인
     */
    public boolean isAIAnalysisAvailable() {
        return aiAnalyzer.isEnabled();
    }

    /**
     * AI 설정 요약 정보
     */
    public String getAIConfigSummary() {
        return aiAnalyzer.getConfigSummary();
    }

    /**
     * 분석 통계 정보
     */
    public String getAnalysisStats() {
        return String.format("파일 분석 서비스 - AI 분석: %s",
                isAIAnalysisAvailable() ? "활성" : "비활성");
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
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