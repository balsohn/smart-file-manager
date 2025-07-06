package com.smartfilemanager.test;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * AI Analysis Demo - 독립 실행 가능
 * 실제 AI API 없이 AI 분석 기능을 시뮬레이션합니다
 */
public class AIAnalysisDemo extends Application {

    private TextArea resultArea;
    private TextField fileNameField;
    private ComboBox<String> analysisTypeCombo;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🤖 AI Analysis Demo - Smart File Manager");

        // 메인 레이아웃
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        // 헤더
        Label headerLabel = new Label("🤖 AI 파일 분석 데모");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descLabel = new Label("실제 AI API 없이 분석 기능을 체험해보세요");
        descLabel.setStyle("-fx-text-fill: gray;");

        // 입력 영역
        VBox inputSection = createInputSection();

        // 결과 영역
        VBox resultSection = createResultSection();

        // 버튼 영역
        HBox buttonSection = createButtonSection();

        mainLayout.getChildren().addAll(
                headerLabel, descLabel,
                new Separator(),
                inputSection,
                buttonSection,
                resultSection
        );

        Scene scene = new Scene(new ScrollPane(mainLayout), 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // 예제 데이터 설정
        setupExampleData();
    }

    private VBox createInputSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");

        Label sectionTitle = new Label("📁 파일 정보 입력");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        // 파일명 입력
        Label fileLabel = new Label("파일명:");
        fileNameField = new TextField();
        fileNameField.setPromptText("예: meeting_notes_2024.pdf");

        // 분석 유형 선택
        Label typeLabel = new Label("분석 유형:");
        analysisTypeCombo = new ComboBox<>();
        analysisTypeCombo.getItems().addAll(
                "스마트 분류 (기본)",
                "심층 콘텐츠 분석",
                "파일명 패턴 분석",
                "업무용 문서 분석",
                "미디어 파일 분석"
        );
        analysisTypeCombo.setValue("스마트 분류 (기본)");

        section.getChildren().addAll(
                sectionTitle,
                fileLabel, fileNameField,
                typeLabel, analysisTypeCombo
        );

        return section;
    }

    private VBox createResultSection() {
        VBox section = new VBox(10);

        Label resultTitle = new Label("📊 AI 분석 결과");
        resultTitle.setStyle("-fx-font-weight: bold;");

        resultArea = new TextArea();
        resultArea.setPrefRowCount(12);
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-font-family: monospace;");

        section.getChildren().addAll(resultTitle, resultArea);
        return section;
    }

    private HBox createButtonSection() {
        HBox section = new HBox(10);

        Button analyzeBtn = new Button("🤖 AI 분석 실행");
        analyzeBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeBtn.setOnAction(e -> performAIAnalysis());

        Button exampleBtn = new Button("📋 예제 보기");
        exampleBtn.setOnAction(e -> showExamples());

        Button clearBtn = new Button("🗑️ 결과 지우기");
        clearBtn.setOnAction(e -> resultArea.clear());

        section.getChildren().addAll(analyzeBtn, exampleBtn, clearBtn);
        return section;
    }

    private void performAIAnalysis() {
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            resultArea.setText("❌ 파일명을 입력해주세요.");
            return;
        }

        String analysisType = analysisTypeCombo.getValue();

        resultArea.setText("🤖 AI 분석 중...\n\n");

        // 시뮬레이션 지연
        new Thread(() -> {
            try {
                Thread.sleep(1500); // 1.5초 대기

                javafx.application.Platform.runLater(() -> {
                    String result = simulateAIAnalysis(fileName, analysisType);
                    resultArea.setText(result);
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private String simulateAIAnalysis(String fileName, String analysisType) {
        StringBuilder result = new StringBuilder();
        result.append("🤖 AI 분석 결과\n");
        result.append("=".repeat(40)).append("\n\n");

        result.append("📁 파일명: ").append(fileName).append("\n");
        result.append("🔍 분석 유형: ").append(analysisType).append("\n\n");

        // 파일 분석 시뮬레이션
        AIAnalysisResult analysis = analyzeFileName(fileName);

        result.append("📊 분류 결과:\n");
        result.append("  • 메인 카테고리: ").append(analysis.category).append("\n");
        result.append("  • 서브 카테고리: ").append(analysis.subCategory).append("\n");
        result.append("  • 추천 폴더: ").append(analysis.suggestedPath).append("\n");
        result.append("  • 신뢰도: ").append(analysis.confidence).append("%\n\n");

        result.append("🏷️ AI 태그:\n");
        for (String tag : analysis.tags) {
            result.append("  • ").append(tag).append("\n");
        }

        result.append("\n💡 AI 추천 사항:\n");
        for (String recommendation : analysis.recommendations) {
            result.append("  • ").append(recommendation).append("\n");
        }

        result.append("\n⚡ 처리 시간: 1.2초\n");
        result.append("🎯 분석 완료!\n");

        return result.toString();
    }

    private AIAnalysisResult analyzeFileName(String fileName) {
        String lower = fileName.toLowerCase();

        // 파일명 패턴 분석
        if (lower.contains("meeting") || lower.contains("회의")) {
            return new AIAnalysisResult(
                    "Documents", "Meeting Notes", "Documents/Work/Meetings", 92,
                    new String[]{"business", "meeting", "work", "notes"},
                    new String[]{"월별 폴더로 세분화 추천", "중요도별 태그 추가 고려"}
            );
        }

        if (lower.contains("screenshot") || lower.contains("스크린샷")) {
            return new AIAnalysisResult(
                    "Images", "Screenshots", "Images/Work/Screenshots", 88,
                    new String[]{"screenshot", "work", "capture", "technical"},
                    new String[]{"날짜별 정리 권장", "프로젝트별 분류 고려"}
            );
        }

        if (lower.contains("photo") || lower.contains("사진")) {
            return new AIAnalysisResult(
                    "Images", "Photos", "Images/Personal/Photos", 90,
                    new String[]{"photo", "personal", "memory", "image"},
                    new String[]{"연도별 정리 권장", "이벤트별 분류 고려"}
            );
        }

        if (lower.contains("video") || lower.contains("tutorial")) {
            return new AIAnalysisResult(
                    "Videos", "Educational", "Videos/Learning", 85,
                    new String[]{"video", "education", "tutorial", "learning"},
                    new String[]{"주제별 분류 권장", "시리즈별 그룹화 고려"}
            );
        }

        if (lower.contains("report") || lower.contains("보고서")) {
            return new AIAnalysisResult(
                    "Documents", "Reports", "Documents/Work/Reports", 94,
                    new String[]{"report", "business", "analysis", "data"},
                    new String[]{"분기별 정리 권장", "부서별 분류 고려"}
            );
        }

        // 기본 분류
        String extension = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";

        return switch (extension) {
            case "pdf", "doc", "docx", "txt" -> new AIAnalysisResult(
                    "Documents", "General", "Documents/General", 75,
                    new String[]{"document", "text", "file"},
                    new String[]{"내용 기반 분류 권장", "키워드 태그 추가 고려"}
            );
            case "jpg", "png", "gif" -> new AIAnalysisResult(
                    "Images", "General", "Images/General", 80,
                    new String[]{"image", "picture", "visual"},
                    new String[]{"날짜별 정리 권장", "앨범별 분류 고려"}
            );
            case "mp4", "avi", "mkv" -> new AIAnalysisResult(
                    "Videos", "General", "Videos/General", 78,
                    new String[]{"video", "media", "entertainment"},
                    new String[]{"장르별 분류 권장", "해상도별 정리 고려"}
            );
            default -> new AIAnalysisResult(
                    "Others", "Uncategorized", "Others/Unsorted", 60,
                    new String[]{"file", "unknown", "misc"},
                    new String[]{"수동 검토 필요", "파일 타입 확인 권장"}
            );
        };
    }

    private void showExamples() {
        String examples = """
        📋 AI 분석 예제 파일들:
        
        📄 문서 파일:
        • meeting_notes_2024_Q1.pdf → Documents/Work/Meetings
        • financial_report_december.xlsx → Documents/Finance/Reports
        • resume_john_doe.docx → Documents/Personal/Resume
        
        🖼️ 이미지 파일:
        • screenshot_bug_report.png → Images/Work/Screenshots
        • vacation_paris_2024.jpg → Images/Personal/Travel
        • logo_company_new.png → Images/Work/Assets
        
        🎬 비디오 파일:
        • javascript_tutorial_advanced.mp4 → Videos/Learning/Programming
        • family_dinner_video.mov → Videos/Personal/Family
        • product_demo_presentation.mp4 → Videos/Work/Presentations
        
        🎵 오디오 파일:
        • meeting_recording_jan15.mp3 → Audio/Work/Recordings
        • classical_music_collection.flac → Audio/Music/Classical
        
        위 예제 중 하나를 입력해서 테스트해보세요!
        """;

        resultArea.setText(examples);
    }

    private void setupExampleData() {
        fileNameField.setText("meeting_notes_2024_Q1.pdf");
        resultArea.setText("👆 위의 예제 파일명으로 AI 분석을 테스트해보세요!\n\n" +
                "또는 '📋 예제 보기' 버튼을 클릭해서 더 많은 예제를 확인할 수 있습니다.");
    }

    // 내부 결과 클래스
    private static class AIAnalysisResult {
        final String category;
        final String subCategory;
        final String suggestedPath;
        final int confidence;
        final String[] tags;
        final String[] recommendations;

        AIAnalysisResult(String category, String subCategory, String suggestedPath,
                         int confidence, String[] tags, String[] recommendations) {
            this.category = category;
            this.subCategory = subCategory;
            this.suggestedPath = suggestedPath;
            this.confidence = confidence;
            this.tags = tags;
            this.recommendations = recommendations;
        }
    }

    public static void main(String[] args) {
        System.out.println("🤖 AI Analysis Demo Starting...");
        System.out.println("This demo simulates AI file analysis without requiring API keys.");
        launch(args);
    }
}