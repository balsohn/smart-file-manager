package com.smartfilemanager.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 파일 내용 분석 유틸리티
 * 다양한 파일 형식의 텍스트, 메타데이터, 키워드를 추출합니다
 */
public class ContentAnalyzer {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "은", "는", "이", "가", "을", "를", "에", "에서", "로", "으로", "와", "과", "의", "도"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"
    );

    /**
     * 파일에서 텍스트 내용을 추출합니다
     */
    public String extractTextContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String extension = getFileExtension(filePath).toLowerCase();

            switch (extension) {
                case "txt":
                case "md":
                case "log":
                    return extractPlainText(path);

                case "pdf":
                    return extractPdfText(path);

                case "doc":
                case "docx":
                    return extractWordText(path);

                case "html":
                case "htm":
                    return extractHtmlText(path);

                case "xml":
                    return extractXmlText(path);

                case "json":
                    return extractJsonText(path);

                case "csv":
                    return extractCsvText(path);

                case "rtf":
                    return extractRtfText(path);

                default:
                    // 텍스트 파일인지 확인
                    if (isTextFile(path)) {
                        return extractPlainText(path);
                    }
                    return null;
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 텍스트 추출 실패: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 일반 텍스트 파일 읽기
     */
    private String extractPlainText(Path path) throws IOException {
        // 인코딩 자동 감지 시도
        byte[] bytes = Files.readAllBytes(path);
        String encoding = detectEncoding(bytes);

        try {
            return new String(bytes, encoding);
        } catch (Exception e) {
            // UTF-8로 재시도
            return Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * PDF 텍스트 추출 (간단한 구현)
     */
    private String extractPdfText(Path path) {
        // 실제로는 Apache PDFBox나 iText 라이브러리 사용 권장
        // 여기서는 간단한 구현만 제공
        System.out.println("[WARNING] PDF 텍스트 추출은 제한적입니다. Apache PDFBox 라이브러리 사용을 권장합니다.");

        try {
            // PDF의 기본 메타데이터만 추출
            return "PDF 파일 - 상세 텍스트 추출을 위해서는 Apache PDFBox 라이브러리가 필요합니다.";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Word 문서 텍스트 추출 (간단한 구현)
     */
    private String extractWordText(Path path) {
        // 실제로는 Apache POI 라이브러리 사용 권장
        System.out.println("[WARNING] Word 문서 텍스트 추출은 제한적입니다. Apache POI 라이브러리 사용을 권장합니다.");

        try {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".docx")) {
                // DOCX는 ZIP 형태이므로 간단한 XML 파싱 시도 가능
                return "DOCX 파일 - 상세 텍스트 추출을 위해서는 Apache POI 라이브러리가 필요합니다.";
            } else {
                return "DOC 파일 - 상세 텍스트 추출을 위해서는 Apache POI 라이브러리가 필요합니다.";
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * HTML 텍스트 추출
     */
    private String extractHtmlText(Path path) throws IOException {
        String html = Files.readString(path);

        // 간단한 HTML 태그 제거
        String text = html.replaceAll("<script[^>]*>.*?</script>", "")  // 스크립트 제거
                .replaceAll("<style[^>]*>.*?</style>", "")     // 스타일 제거
                .replaceAll("<[^>]+>", "")                     // 모든 HTML 태그 제거
                .replaceAll("&nbsp;", " ")                     // HTML 엔티티 변환
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"");

        return cleanupText(text);
    }

    /**
     * XML 텍스트 추출
     */
    private String extractXmlText(Path path) throws IOException {
        String xml = Files.readString(path);

        // XML 태그 제거하고 텍스트 내용만 추출
        String text = xml.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ");

        return cleanupText(text);
    }

    /**
     * JSON 텍스트 추출
     */
    private String extractJsonText(Path path) throws IOException {
        String json = Files.readString(path);

        // JSON 구조에서 값들만 추출
        StringBuilder text = new StringBuilder();

        // 간단한 값 추출 (문자열 값들)
        Pattern stringPattern = Pattern.compile("\"([^\"]+)\"\\s*:");
        Matcher matcher = stringPattern.matcher(json);

        while (matcher.find()) {
            text.append(matcher.group(1)).append(" ");
        }

        // 문자열 값들 추출
        Pattern valuePattern = Pattern.compile(":\\s*\"([^\"]+)\"");
        Matcher valueMatcher = valuePattern.matcher(json);

        while (valueMatcher.find()) {
            text.append(valueMatcher.group(1)).append(" ");
        }

        return cleanupText(text.toString());
    }

    /**
     * CSV 텍스트 추출
     */
    private String extractCsvText(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        StringBuilder text = new StringBuilder();

        for (String line : lines.subList(0, Math.min(lines.size(), 10))) { // 처음 10줄만
            // CSV 값들을 공백으로 분리
            String[] values = line.split(",");
            for (String value : values) {
                text.append(value.replaceAll("\"", "").trim()).append(" ");
            }
        }

        return cleanupText(text.toString());
    }

    /**
     * RTF 텍스트 추출 (간단한 구현)
     */
    private String extractRtfText(Path path) throws IOException {
        String rtf = Files.readString(path);

        // RTF 제어 코드 제거
        String text = rtf.replaceAll("\\\\[a-zA-Z]+\\d*\\s?", "")  // RTF 명령어 제거
                .replaceAll("\\{|\\}", "")                // 중괄호 제거
                .replaceAll("\\\\", "");                  // 백슬래시 제거

        return cleanupText(text);
    }

    /**
     * 키워드 추출
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 텍스트 정규화
        String normalizedText = text.toLowerCase()
                .replaceAll("[^a-zA-Z가-힣0-9\\s]", " ")
                .replaceAll("\\s+", " ");

        // 단어 분리 및 필터링
        List<String> keywords = Arrays.stream(normalizedText.split("\\s+"))
                .filter(word -> word.length() > 2)  // 3글자 이상
                .filter(word -> !STOP_WORDS.contains(word))  // 불용어 제외
                .filter(word -> !word.matches("\\d+"))  // 숫자만 있는 단어 제외
                .collect(Collectors.toList());

        // 빈도수 계산 및 상위 키워드 반환
        Map<String, Long> frequency = keywords.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)  // 상위 20개
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 작성자 정보 추출
     */
    public String extractAuthor(String filePath) {
        try {
            String content = extractTextContent(filePath);
            if (content == null) return null;

            // 일반적인 작성자 패턴들
            Pattern[] authorPatterns = {
                    Pattern.compile("(?i)author[:\\s]+([\\w\\s]+)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?i)written\\s+by[:\\s]+([\\w\\s]+)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?i)작성자[:\\s]+([\\w\\s가-힣]+)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?i)저자[:\\s]+([\\w\\s가-힣]+)", Pattern.CASE_INSENSITIVE)
            };

            for (Pattern pattern : authorPatterns) {
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 제목 추출
     */
    public String extractTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        String[] lines = content.split("\n");

        // 첫 번째 의미있는 줄을 제목으로 간주
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 3 && trimmed.length() < 100) {
                // HTML 태그나 특수 문자가 없는 경우
                if (!trimmed.matches(".*[<>{}\\[\\]]+.*")) {
                    return trimmed;
                }
            }
        }

        return null;
    }

    /**
     * 이메일 주소 추출
     */
    public List<String> extractEmails(String text) {
        if (text == null) return new ArrayList<>();

        List<String> emails = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);

        while (matcher.find()) {
            emails.add(matcher.group());
        }

        return emails;
    }

    /**
     * URL 추출
     */
    public List<String> extractUrls(String text) {
        if (text == null) return new ArrayList<>();

        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);

        while (matcher.find()) {
            urls.add(matcher.group());
        }

        return urls;
    }

    /**
     * 전화번호 추출
     */
    public List<String> extractPhoneNumbers(String text) {
        if (text == null) return new ArrayList<>();

        List<String> phones = new ArrayList<>();
        Matcher matcher = PHONE_PATTERN.matcher(text);

        while (matcher.find()) {
            phones.add(matcher.group());
        }

        return phones;
    }

    /**
     * 파일이 텍스트 파일인지 확인
     */
    private boolean isTextFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);

            // 파일이 너무 크면 처음 1KB만 확인
            int checkSize = Math.min(bytes.length, 1024);

            // 텍스트 파일 여부 판단 (null 바이트가 적고 인쇄 가능한 문자가 많으면 텍스트)
            int printableCount = 0;
            int nullCount = 0;

            for (int i = 0; i < checkSize; i++) {
                byte b = bytes[i];
                if (b == 0) {
                    nullCount++;
                } else if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                    printableCount++;
                }
            }

            double printableRatio = (double) printableCount / checkSize;
            double nullRatio = (double) nullCount / checkSize;

            return printableRatio > 0.7 && nullRatio < 0.1;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 인코딩 감지 (간단한 구현)
     */
    private String detectEncoding(byte[] bytes) {
        // BOM 확인
        if (bytes.length >= 3 &&
                bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            return "UTF-8";
        }

        if (bytes.length >= 2 &&
                bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE) {
            return "UTF-16LE";
        }

        if (bytes.length >= 2 &&
                bytes[0] == (byte)0xFE && bytes[1] == (byte)0xFF) {
            return "UTF-16BE";
        }

        // 한국어 포함 여부 확인으로 인코딩 추측
        String testUtf8 = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (testUtf8.matches(".*[가-힣]+.*")) {
            return "UTF-8";
        }

        // 기본값
        return "UTF-8";
    }

    /**
     * 텍스트 정리
     */
    private String cleanupText(String text) {
        if (text == null) return null;

        return text.replaceAll("\\s+", " ")  // 중복 공백 제거
                .replaceAll("^\\s+|\\s+$", "")  // 앞뒤 공백 제거
                .trim();
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return (lastDot == -1) ? "" : filePath.substring(lastDot + 1);
    }

    /**
     * 문서 언어 감지 (간단한 구현)
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        // 한국어 감지
        if (text.matches(".*[가-힣]+.*")) {
            return "ko";
        }

        // 영어 감지 (기본값)
        return "en";
    }

    /**
     * 문서 카테고리 추측 (내용 기반)
     */
    public String guessDocumentCategory(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        String lowerText = text.toLowerCase();

        // 이력서/자소서 키워드
        if (lowerText.matches(".*(resume|cv|이력서|자기소개서|경력|학력|자소서).*")) {
            return "resume";
        }

        // 계약서/법률 문서
        if (lowerText.matches(".*(contract|agreement|법률|계약|약관|조항).*")) {
            return "legal";
        }

        // 보고서
        if (lowerText.matches(".*(report|analysis|보고서|분석|연구).*")) {
            return "report";
        }

        // 매뉴얼/가이드
        if (lowerText.matches(".*(manual|guide|tutorial|매뉴얼|가이드|설명서).*")) {
            return "manual";
        }

        // 재무/회계
        if (lowerText.matches(".*(invoice|receipt|financial|재무|회계|영수증|계산서).*")) {
            return "financial";
        }

        return "general";
    }
}