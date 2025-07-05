package com.smartfilemanager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 애플리케이션 정보를 표시하는 다이얼로그
 */
public class AboutDialog {

    public static void show(Stage ownerStage) {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.initOwner(ownerStage);
        aboutStage.setTitle("정보 - Smart File Manager");
        aboutStage.setResizable(false);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setAlignment(Pos.CENTER);

        // 애플리케이션 아이콘/제목
        Label titleLabel = new Label("🗂️ Smart File Manager");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 버전 정보
        Label versionLabel = new Label("버전 1.0.0");
        versionLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        versionLabel.setStyle("-fx-text-fill: #6c757d;");

        // 설명
        Label descriptionLabel = new Label("AI 기반 스마트 파일 정리 도구");
        descriptionLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        descriptionLabel.setStyle("-fx-text-fill: #495057;");

        // 기능 목록
        VBox featuresBox = new VBox(5);
        featuresBox.setAlignment(Pos.CENTER_LEFT);

        Label featuresTitle = new Label("✨ 주요 기능:");
        featuresTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        String[] features = {
                "📁 스마트 파일 분류 및 정리",
                "🔄 중복 파일 자동 탐지",
                "🧹 불필요한 파일 정리",
                "↩️ 안전한 되돌리기 기능",
                "📊 상세한 분석 통계",
                "⚙️ 사용자 정의 설정"
        };

        featuresBox.getChildren().add(featuresTitle);
        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setFont(Font.font("System", 12));
            featureLabel.setStyle("-fx-text-fill: #495057;");
            featuresBox.getChildren().add(featureLabel);
        }

        // 기술 스택
        VBox techBox = new VBox(5);
        techBox.setAlignment(Pos.CENTER_LEFT);

        Label techTitle = new Label("🛠️ 기술 스택:");
        techTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label techLabel = new Label("• JavaFX 21.0.2 + Lombok + SQLite + Gson");
        techLabel.setFont(Font.font("System", 12));
        techLabel.setStyle("-fx-text-fill: #495057;");

        techBox.getChildren().addAll(techTitle, techLabel);

        // 개발자 정보
        VBox developerBox = new VBox(5);
        developerBox.setAlignment(Pos.CENTER);

        Label developerLabel = new Label("개발: Smart File Manager Team");
        developerLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        developerLabel.setStyle("-fx-text-fill: #6c757d;");

        Label copyrightLabel = new Label("© 2024 All rights reserved");
        copyrightLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        copyrightLabel.setStyle("-fx-text-fill: #adb5bd;");

        developerBox.getChildren().addAll(developerLabel, copyrightLabel);

        // GitHub 링크 (선택사항)
        Hyperlink githubLink = new Hyperlink("🔗 GitHub에서 소스코드 보기");
        githubLink.setFont(Font.font("System", 12));
        githubLink.setOnAction(e -> {
            // 실제 GitHub URL로 변경 필요
            try {
                java.awt.Desktop.getDesktop().browse(
                        java.net.URI.create("https://github.com/your-username/smart-file-manager"));
            } catch (Exception ex) {
                System.out.println("브라우저를 열 수 없습니다: " + ex.getMessage());
            }
        });

        // 시스템 정보
        VBox systemBox = new VBox(3);
        systemBox.setAlignment(Pos.CENTER_LEFT);

        Label systemTitle = new Label("💻 시스템 정보:");
        systemTitle.setFont(Font.font("System", FontWeight.BOLD, 12));

        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        Label javaLabel = new Label("Java: " + javaVersion);
        Label osLabel = new Label("OS: " + osName + " " + osVersion);

        javaLabel.setFont(Font.font("System", 10));
        osLabel.setFont(Font.font("System", 10));
        javaLabel.setStyle("-fx-text-fill: #6c757d;");
        osLabel.setStyle("-fx-text-fill: #6c757d;");

        systemBox.getChildren().addAll(systemTitle, javaLabel, osLabel);

        // 닫기 버튼
        Button closeButton = new Button("확인");
        closeButton.setPrefWidth(100);
        closeButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 5px;");
        closeButton.setOnAction(e -> aboutStage.close());

        // 레이아웃 구성
        content.getChildren().addAll(
                titleLabel,
                versionLabel,
                descriptionLabel,
                new Label(), // 공백
                featuresBox,
                new Label(), // 공백
                techBox,
                new Label(), // 공백
                githubLink,
                new Label(), // 공백
                systemBox,
                new Label(), // 공백
                developerBox,
                new Label(), // 공백
                closeButton
        );

        Scene scene = new Scene(content, 450, 650);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }
}