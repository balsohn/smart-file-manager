package com.smartfilemanager.util;

import com.smartfilemanager.model.FileInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 기반 파일 분석 유틸리티 - 완성된 버전
 * OpenAI API를 사용해서 파일 내용과 메타데이터를 분석하여 더 정확한 분류를 수행합니다
 */
public class AIAnalyzer {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int MAX_CONTENT_LENGTH = 2000; // API 토큰 제한 고려
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;
    private final Gson gson;
    private String apiKey;
    private String model;
    private boolean enabled;

    public AIAnalyzer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
        this.model = DEFAULT_MODEL;
        this.enabled = false;
    }

    /**
     * API 키 설정
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        this.enabled = (apiKey != null && !apiKey.trim().isEmpty() && isValidApiKeyFormat(apiKey));

        if (this.enabled) {
            System.out.println("[AI] API 키가 설정되었습니다. 모델: " + model);
        }
    }

    /**
     * 모델 설정
     */
    public void setModel(String model) {
        this.model = model != null ? model : DEFAULT_MODEL;
    }

    /**
     * AI 분석 활성화 여부 확인
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * API 키 형식 검증
     */
    private boolean isValidApiKeyFormat(String apiKey) {
        return apiKey != null && apiKey.trim().startsWith("sk-") && apiKey.trim().length() > 20;
    }

    /**
     * 파일을 AI로 분석하여 향상된 분류 정보를 반환합니다
     */
    public String analyzeFile(FileInfo fileInfo) {
        if (!enabled) {
            System.out.println("[INFO] AI 분석이 비활성화되어 있습니다.");
            return null;
        }

        try {
            System.out.println("[AI] 파일 분석 시작: " + fileInfo.getFileName());

            // 분석용 프롬프트 생성
            String prompt = createAnalysisPrompt(fileInfo);

            // OpenAI API 호출
            String response = callOpenAIAPI(prompt);

            if (response != null) {
                System.out.println("[AI] 파일 분석 완료: " + fileInfo.getFileName());
                return response;
            }

            return null;

        } catch (Exception e) {
            System.err.println("[ERROR] AI 분석 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 파일 분석용 프롬프트 생성
     */
    private String createAnalysisPrompt(FileInfo fileInfo) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("다음 파일을 분석해서 정확한 카테고리와 서브카테고리를 제안해주세요:\n\n");

        // 기본 파일 정보
        prompt.append("파일명: ").append(fileInfo.getFileName()).append("\n");
        prompt.append("확장자: ").append(fileInfo.getFileExtension()).append("\n");
        prompt.append("크기: ").append(fileInfo.getFormattedFileSize()).append("\n");

        if (fileInfo.getMimeType() != null) {
            prompt.append("MIME 타입: ").append(fileInfo.getMimeType()).append("\n");
        }

        // 기존 분석 결과
        if (fileInfo.getDetectedCategory() != null) {
            prompt.append("기존 카테고리: ").append(fileInfo.getDetectedCategory()).append("\n");
        }

        if (fileInfo.getDetectedSubCategory() != null) {
            prompt.append("기존 서브카테고리: ").append(fileInfo.getDetectedSubCategory()).append("\n");
        }

        // 추출된 메타데이터
        if (fileInfo.getExtractedTitle() != null) {
            prompt.append("추출된 제목: ").append(fileInfo.getExtractedTitle()).append("\n");
        }

        if (fileInfo.getExtractedAuthor() != null) {
            prompt.append("작성자: ").append(fileInfo.getExtractedAuthor()).append("\n");
        }

        if (fileInfo.getDescription() != null) {
            String description = fileInfo.getDescription();
            if (description.length() > MAX_CONTENT_LENGTH) {
                description = description.substring(0, MAX_CONTENT_LENGTH) + "...";
            }
            prompt.append("내용 미리보기: ").append(description).append("\n");
        }

        // 키워드
        if (fileInfo.getKeywords() != null && !fileInfo.getKeywords().isEmpty()) {
            prompt.append("키워드: ").append(String.join(", ", fileInfo.getKeywords())).append("\n");
        }

        prompt.append("\n다음 JSON 형식으로만 응답해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"category\": \"추천 카테고리\",\n");
        prompt.append("  \"subcategory\": \"추천 서브카테고리\",\n");
        prompt.append("  \"confidence\": 0.95,\n");
        prompt.append("  \"reasoning\": \"분류 근거 설명\",\n");
        prompt.append("  \"tags\": [\"태그1\", \"태그2\"],\n");
        prompt.append("  \"suggested_folder\": \"추천 폴더명\"\n");
        prompt.append("}\n\n");

        prompt.append("가능한 카테고리: Documents, Images, Videos, Audio, Archives, Applications, Code, Others\n");
        prompt.append("Documents의 서브카테고리: Work, Personal, Financial, Legal, Educational, Manuals\n");
        prompt.append("Images의 서브카테고리: Photos, Screenshots, Wallpapers, Icons, Memes\n");
        prompt.append("Videos의 서브카테고리: Movies, TV Shows, Educational, Clips, Personal\n");

        return prompt.toString();
    }

    /**
     * OpenAI API 호출
     */
    private String callOpenAIAPI(String prompt) throws IOException, InterruptedException {
        // 요청 본문 생성
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", 500);
        requestBody.addProperty("temperature", 0.3); // 일관성을 위해 낮은 temperature

        // 메시지 배열 생성
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);
        requestBody.add("messages", messages);

        // HTTP 요청 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        try {
            // API 호출
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseOpenAIResponse(response.body());
            } else if (response.statusCode() == 401) {
                System.err.println("[ERROR] OpenAI API 키가 유효하지 않습니다.");
                this.enabled = false; // API 키 문제 시 자동 비활성화
                return null;
            } else if (response.statusCode() == 429) {
                System.err.println("[ERROR] OpenAI API 사용량 한도 초과");
                return null;
            } else {
                System.err.println("[ERROR] OpenAI API 오류: " + response.statusCode() + " - " + response.body());
                return null;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] OpenAI API 네트워크 오류: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("[ERROR] OpenAI API 호출 타임아웃");
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * OpenAI API 응답 파싱
     */
    private String parseOpenAIResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                JsonObject firstChoice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                String content = message.get("content").getAsString();

                return content.trim();
            }

            return null;

        } catch (Exception e) {
            System.err.println("[ERROR] OpenAI 응답 파싱 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * AI 분석 결과를 FileInfo에 적용
     */
    public boolean applyAIAnalysis(FileInfo fileInfo, String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return false;
        }

        try {
            // JSON 응답에서 실제 JSON 부분 추출
            String jsonPart = extractJsonFromResponse(aiResponse);
            if (jsonPart == null) {
                System.err.println("[WARNING] AI 응답에서 JSON을 찾을 수 없습니다: " + aiResponse);
                return false;
            }

            JsonObject analysis = JsonParser.parseString(jsonPart).getAsJsonObject();

            // 카테고리 적용
            if (analysis.has("category")) {
                String category = analysis.get("category").getAsString();
                if (isValidCategory(category)) {
                    fileInfo.setDetectedCategory(category);
                    System.out.println("[AI] 카테고리 업데이트: " + category);
                }
            }

            // 서브카테고리 적용
            if (analysis.has("subcategory")) {
                String subcategory = analysis.get("subcategory").getAsString();
                fileInfo.setDetectedSubCategory(subcategory);
                System.out.println("[AI] 서브카테고리 업데이트: " + subcategory);
            }

            // 신뢰도 적용
            if (analysis.has("confidence")) {
                double confidence = analysis.get("confidence").getAsDouble();
                fileInfo.setConfidenceScore(Math.min(confidence, 1.0));
                System.out.println("[AI] 신뢰도 업데이트: " + String.format("%.2f", confidence));
            }

            // 추가 설명 적용
            if (analysis.has("reasoning")) {
                String reasoning = analysis.get("reasoning").getAsString();
                String currentDesc = fileInfo.getDescription();
                String newDesc = currentDesc != null ?
                        currentDesc + "\n\nAI 분석: " + reasoning :
                        "AI 분석: " + reasoning;
                fileInfo.setDescription(newDesc);
            }

            // 태그 추가
            if (analysis.has("tags") && analysis.get("tags").isJsonArray()) {
                JsonArray tags = analysis.getAsJsonArray("tags");
                List<String> keywords = fileInfo.getKeywords();
                if (keywords == null) {
                    keywords = new ArrayList<>();
                    fileInfo.setKeywords(keywords);
                }

                for (int i = 0; i < tags.size(); i++) {
                    String tag = tags.get(i).getAsString();
                    if (!keywords.contains(tag)) {
                        keywords.add(tag);
                    }
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] AI 분석 결과 적용 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 응답에서 JSON 부분 추출
     */
    private String extractJsonFromResponse(String response) {
        // JSON 시작과 끝을 찾기
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return null;
    }

    /**
     * 유효한 카테고리인지 확인
     */
    private boolean isValidCategory(String category) {
        String[] validCategories = {
                "Documents", "Images", "Videos", "Audio", "Archives",
                "Applications", "Code", "Others"
        };

        return Arrays.asList(validCategories).contains(category);
    }

    /**
     * 여러 파일을 배치로 분석
     */
    public Map<String, String> analyzeBatch(List<FileInfo> files) {
        Map<String, String> results = new HashMap<>();

        if (!enabled) {
            System.out.println("[INFO] AI 분석이 비활성화되어 있습니다.");
            return results;
        }

        System.out.println("[AI] 배치 분석 시작: " + files.size() + "개 파일");

        for (FileInfo file : files) {
            try {
                String result = analyzeFile(file);
                if (result != null) {
                    results.put(file.getFilePath(), result);
                    applyAIAnalysis(file, result);
                }

                // API 호출 제한을 위한 지연 (TPM 제한 고려)
                Thread.sleep(200);

            } catch (Exception e) {
                System.err.println("[ERROR] 배치 분석 중 오류: " + file.getFileName() + " - " + e.getMessage());
            }
        }

        System.out.println("[AI] 배치 분석 완료: " + results.size() + "개 성공");
        return results;
    }

    /**
     * API 키 유효성 검사
     */
    public boolean validateApiKey() {
        if (!enabled) {
            return false;
        }

        try {
            // 간단한 테스트 요청
            String testPrompt = "Hello, please respond with exactly 'OK'";
            String response = callOpenAIAPI(testPrompt);

            boolean isValid = response != null && response.toLowerCase().contains("ok");

            if (!isValid) {
                this.enabled = false; // 유효하지 않으면 비활성화
                System.err.println("[ERROR] API 키 검증 실패");
            } else {
                System.out.println("[SUCCESS] API 키 검증 성공");
            }

            return isValid;

        } catch (Exception e) {
            System.err.println("[ERROR] API 키 검증 중 오류: " + e.getMessage());
            this.enabled = false;
            return false;
        }
    }

    /**
     * 설정 요약 정보
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("AI 분석 설정:\n");
        summary.append("  활성화: ").append(enabled ? "예" : "아니오").append("\n");
        summary.append("  모델: ").append(model).append("\n");
        summary.append("  API 키: ").append(apiKey != null ? "설정됨 (길이: " + apiKey.length() + "자)" : "미설정").append("\n");

        if (enabled) {
            summary.append("  상태: 사용 가능").append("\n");
        } else {
            summary.append("  상태: 비활성화 (API 키 확인 필요)").append("\n");
        }

        return summary.toString();
    }

    /**
     * AI 분석기 통계 정보
     */
    public String getUsageStats() {
        return String.format("AI 분석기 상태: %s, 모델: %s",
                enabled ? "활성" : "비활성", model);
    }

    /**
     * 리소스 정리
     */
    public void shutdown() {
        System.out.println("[AI] AIAnalyzer 종료");
        // HttpClient는 자동으로 정리됨
    }
}