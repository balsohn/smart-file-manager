package com.smartfilemanager.util;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.FileAnalysisService;
import com.smartfilemanager.util.AIAnalyzer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 분석 기능 테스트 및 사용 예제
 *
 * 실제 사용 전에 OpenAI API 키를 설정해야 합니다:
 * 1. https://platform.openai.com 에서 API 키 생성
 * 2. 아래 API_KEY 상수에 키 입력
 * 3. 테스트 실행
 */
public class AIAnalysisTestExample {

    // ⚠️ 실제 API 키로 교체하세요
    private static final String API_KEY = "sk-your-api-key-here";

    public static void main(String[] args) {
        System.out.println("🤖 Smart File Manager - AI 분석 테스트");
        System.out.println("=".repeat(60));

        if (API_KEY.equals("sk-your-api-key-here")) {
            System.out.println("⚠️ API 키를 설정해주세요!");
            System.out.println("1. https://platform.openai.com 에서 API 키 생성");
            System.out.println("2. API_KEY 상수에 키 입력");
            return;
        }

        // 1. 기본 AIAnalyzer 테스트
        testBasicAIAnalyzer();

        // 2. 통합 FileAnalysisService 테스트
        testIntegratedAnalysis();

        // 3. 배치 분석 테스트
        testBatchAnalysis();

        System.out.println("\n🎉 모든 테스트 완료!");
    }

    /**
     * 기본 AIAnalyzer 기능 테스트
     */
    private static void testBasicAIAnalyzer() {
        System.out.println("\n📋 1. 기본 AIAnalyzer 테스트");

        AIAnalyzer aiAnalyzer = new AIAnalyzer();
        aiAnalyzer.setApiKey(API_KEY);

        // API 키 유효성 검사
        System.out.println("   API 키 검증 중...");
        boolean isValidKey = aiAnalyzer.validateApiKey();
        System.out.println("   API 키 유효성: " + (isValidKey ? "✅ 유효" : "❌ 무효"));

        if (!isValidKey) {
            System.out.println("   API 키가 유효하지 않아 테스트를 중단합니다.");
            return;
        }

        // 테스트용 파일 정보 생성
        FileInfo testFile = createTestFileInfo(
                "meeting_notes_2024_Q1_financial_report.pdf",
                "pdf",
                1024 * 1024 * 2, // 2MB
                "Documents"
        );

        System.out.println("   테스트 파일: " + testFile.getFileName());

        // AI 분석 실행
        System.out.println("   AI 분석 실행 중...");
        String aiResponse = aiAnalyzer.analyzeFile(testFile);

        if (aiResponse != null) {
            System.out.println("   AI 응답 수신: ✅");
            System.out.println("   응답 길이: " + aiResponse.length() + " 문자");

            // 응답 적용
            boolean applied = aiAnalyzer.applyAIAnalysis(testFile, aiResponse);
            System.out.println("   분석 결과 적용: " + (applied ? "✅ 성공" : "❌ 실패"));

            if (applied) {
                printFileAnalysisResult(testFile);
            }
        } else {
            System.out.println("   AI 응답 없음: ❌");
        }

        // 설정 요약 출력
        System.out.println("\n   " + aiAnalyzer.getConfigSummary().replace("\n", "\n   "));
    }

    /**
     * 통합 FileAnalysisService 테스트
     */
    private static void testIntegratedAnalysis() {
        System.out.println("\n🔧 2. 통합 FileAnalysisService 테스트");

        // 실제 파일 경로들 (존재하지 않을 수 있음)
        String[] testFilePaths = {
                "C:\\Users\\User\\Downloads\\screenshot_2024_01_15.png",
                "C:\\Users\\User\\Documents\\resume_john_doe.pdf",
                "C:\\Users\\User\\Downloads\\music_video_concert.mp4"
        };

        FileAnalysisService analysisService = new FileAnalysisService();

        for (String filePath : testFilePaths) {
            System.out.println("   분석 대상: " + filePath);

            try {
                // 실제 파일이 없는 경우 가상 분석 실행
                FileInfo fileInfo = createTestFileFromPath(filePath);

                // AI가 포함된 분석 실행 시뮬레이션
                System.out.println("   → 기본 분석 완료");
                System.out.println("   → 카테고리: " + fileInfo.getDetectedCategory());
                System.out.println("   → 서브카테고리: " + fileInfo.getDetectedSubCategory());
                System.out.println("   → 신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()));

            } catch (Exception e) {
                System.out.println("   → 분석 실패: " + e.getMessage());
            }
        }

        System.out.println("   AI 분석 가능 여부: " + analysisService.isAIAnalysisAvailable());
    }

    /**
     * 배치 분석 테스트
     */
    private static void testBatchAnalysis() {
        System.out.println("\n📦 3. 배치 분석 테스트");

        AIAnalyzer aiAnalyzer = new AIAnalyzer();
        aiAnalyzer.setApiKey(API_KEY);

        // 테스트용 파일 목록 생성
        List<FileInfo> testFiles = Arrays.asList(
                createTestFileInfo("photo_vacation_2024.jpg", "jpg", 5 * 1024 * 1024, "Images"),
                createTestFileInfo("tutorial_java_programming.mp4", "mp4", 100 * 1024 * 1024, "Videos"),
                createTestFileInfo("financial_statement_Q4.xlsx", "xlsx", 2 * 1024 * 1024, "Spreadsheets"),
                createTestFileInfo("meeting_audio_recording.mp3", "mp3", 15 * 1024 * 1024, "Audio")
        );

        System.out.println("   배치 분석 대상: " + testFiles.size() + "개 파일");

        try {
            // 배치 분석 실행 (시뮬레이션)
            System.out.println("   배치 분석 시작...");

            int successCount = 0;
            for (FileInfo file : testFiles) {
                System.out.println("   → 분석 중: " + file.getFileName());

                // 실제 환경에서는 aiAnalyzer.analyzeBatch() 사용
                String response = aiAnalyzer.analyzeFile(file);
                if (response != null && aiAnalyzer.applyAIAnalysis(file, response)) {
                    successCount++;
                    System.out.println("     ✅ 성공 (신뢰도: " +
                            String.format("%.2f", file.getConfidenceScore()) + ")");
                } else {
                    System.out.println("     ❌ 실패");
                }

                // API 호출 제한 방지
                Thread.sleep(500);
            }

            System.out.println("   배치 분석 완료: " + successCount + "/" + testFiles.size() + " 성공");

        } catch (Exception e) {
            System.out.println("   배치 분석 오류: " + e.getMessage());
        }
    }

    /**
     * 테스트용 FileInfo 객체 생성
     */
    private static FileInfo createTestFileInfo(String fileName, String extension, long size, String category) {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setFilePath("C:\\Test\\" + fileName);
        fileInfo.setFileName(fileName);
        fileInfo.setFileExtension(extension);
        fileInfo.setFileSize(size);
        fileInfo.setFormattedFileSize(formatFileSize(size));
        fileInfo.setDetectedCategory(category);
        fileInfo.setMimeType(getMimeTypeFromExtension(extension));
        fileInfo.setCreatedDate(LocalDateTime.now().minusDays(7));
        fileInfo.setModifiedDate(LocalDateTime.now().minusDays(3));
        fileInfo.setStatus(ProcessingStatus.PENDING);
        fileInfo.setConfidenceScore(0.6); // 기본 신뢰도
        fileInfo.setKeywords(new ArrayList<>());

        // 파일명에서 기본 키워드 추출
        String[] words = fileName.toLowerCase().split("[^a-z0-9가-힣]+");
        for (String word : words) {
            if (word.length() > 2) {
                fileInfo.getKeywords().add(word);
            }
        }

        return fileInfo;
    }

    /**
     * 파일 경로에서 테스트용 FileInfo 생성
     */
    private static FileInfo createTestFileFromPath(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        String extension = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf(".") + 1) : "";

        // 파일 타입에 따른 대략적인 크기와 카테고리 추정
        long estimatedSize = estimateFileSizeByExtension(extension);
        String estimatedCategory = estimateCategoryByExtension(extension);

        return createTestFileInfo(fileName, extension, estimatedSize, estimatedCategory);
    }

    /**
     * 확장자별 예상 파일 크기
     */
    private static long estimateFileSizeByExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "png", "jpg", "jpeg" -> 2 * 1024 * 1024; // 2MB
            case "mp4", "avi", "mkv" -> 100 * 1024 * 1024; // 100MB
            case "mp3", "wav" -> 5 * 1024 * 1024; // 5MB
            case "pdf", "doc", "docx" -> 1024 * 1024; // 1MB
            case "txt" -> 50 * 1024; // 50KB
            default -> 1024 * 1024; // 1MB
        };
    }

    /**
     * 확장자별 예상 카테고리
     */
    private static String estimateCategoryByExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "png", "jpg", "jpeg", "gif", "bmp" -> "Images";
            case "mp4", "avi", "mkv", "mov" -> "Videos";
            case "mp3", "wav", "flac" -> "Audio";
            case "pdf", "doc", "docx", "txt" -> "Documents";
            case "xls", "xlsx", "csv" -> "Spreadsheets";
            case "zip", "rar", "7z" -> "Archives";
            default -> "Others";
        };
    }

    /**
     * 확장자별 MIME 타입
     */
    private static String getMimeTypeFromExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            case "txt" -> "text/plain";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    /**
     * 파일 분석 결과 출력
     */
    private static void printFileAnalysisResult(FileInfo fileInfo) {
        System.out.println("   📄 분석 결과:");
        System.out.println("     파일명: " + fileInfo.getFileName());
        System.out.println("     카테고리: " + fileInfo.getDetectedCategory());
        System.out.println("     서브카테고리: " + fileInfo.getDetectedSubCategory());
        System.out.println("     신뢰도: " + String.format("%.2f", fileInfo.getConfidenceScore()));

        if (fileInfo.getKeywords() != null && !fileInfo.getKeywords().isEmpty()) {
            System.out.println("     키워드: " + String.join(", ", fileInfo.getKeywords()));
        }

        if (fileInfo.getDescription() != null) {
            String desc = fileInfo.getDescription();
            if (desc.length() > 100) {
                desc = desc.substring(0, 97) + "...";
            }
            System.out.println("     설명: " + desc);
        }

        if (fileInfo.getSuggestedPath() != null) {
            System.out.println("     추천 경로: " + fileInfo.getSuggestedPath());
        }
    }

    /**
     * 파일 크기 포맷팅
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

/**
 * 간단한 AI 분석 데모 (API 키 없이 실행 가능)
 */
class AIAnalysisDemoWithoutAPI {

    public static void main(String[] args) {
        System.out.println("🎭 AI 분석 데모 (API 키 불필요)");
        System.out.println("=".repeat(50));

        // 가상의 AI 분석 결과 시뮬레이션
        demonstrateAIAnalysis();
    }

    private static void demonstrateAIAnalysis() {
        String[] testFiles = {
                "meeting_notes_Q1_2024.pdf",
                "vacation_photo_paris.jpg",
                "javascript_tutorial_advanced.mp4",
                "financial_report_december.xlsx",
                "screenshot_bug_report.png"
        };

        System.out.println("💡 AI 분석이 다음과 같이 파일을 분류합니다:\n");

        for (String fileName : testFiles) {
            System.out.println("📁 " + fileName);

            // 가상의 AI 분석 결과
            String[] analysis = simulateAIAnalysis(fileName);

            System.out.println("   → 카테고리: " + analysis[0]);
            System.out.println("   → 서브카테고리: " + analysis[1]);
            System.out.println("   → 추천 폴더: " + analysis[2]);
            System.out.println("   → AI 태그: " + analysis[3]);
            System.out.println("   → 신뢰도: " + analysis[4]);
            System.out.println();
        }

        System.out.println("✨ 실제 AI 분석을 사용하려면:");
        System.out.println("   1. OpenAI API 키 획득");
        System.out.println("   2. 설정에서 AI 분석 활성화");
        System.out.println("   3. API 키 입력 및 테스트");
    }

    private static String[] simulateAIAnalysis(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.contains("meeting") || lower.contains("notes")) {
            return new String[]{
                    "Documents",
                    "Meeting Notes",
                    "Documents/Work/Meetings",
                    "business, meeting, notes, work",
                    "92%"
            };
        } else if (lower.contains("photo") || lower.contains("vacation")) {
            return new String[]{
                    "Images",
                    "Photos",
                    "Images/Personal/Travel",
                    "vacation, travel, personal, photo",
                    "88%"
            };
        } else if (lower.contains("tutorial") || lower.contains("javascript")) {
            return new String[]{
                    "Videos",
                    "Educational",
                    "Videos/Learning/Programming",
                    "tutorial, education, programming, javascript",
                    "95%"
            };
        } else if (lower.contains("financial") || lower.contains("report")) {
            return new String[]{
                    "Spreadsheets",
                    "Financial",
                    "Documents/Finance/Reports",
                    "financial, business, report, data",
                    "90%"
            };
        } else if (lower.contains("screenshot") || lower.contains("bug")) {
            return new String[]{
                    "Images",
                    "Screenshots",
                    "Images/Work/Screenshots",
                    "screenshot, work, technical, bug",
                    "87%"
            };
        } else {
            return new String[]{
                    "Others",
                    "General",
                    "Others/Unsorted",
                    "file, document",
                    "65%"
            };
        }
    }
}