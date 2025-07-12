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
        titleLabel.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 26));
        titleLabel.getStyleClass().add("about-title");

        // 버전 정보
        Label versionLabel = new Label("버전 1.0.0");
        versionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        versionLabel.getStyleClass().add("about-version");

        // 설명
        Label descriptionLabel = new Label("AI 기반 스마트 파일 정리 도구");
        descriptionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        descriptionLabel.getStyleClass().add("about-description");

        // 기능 목록
        VBox featuresBox = new VBox(5);
        featuresBox.setAlignment(Pos.CENTER_LEFT);

        Label featuresTitle = new Label("✨ 주요 기능:");
        featuresTitle.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 16));
        featuresTitle.getStyleClass().add("about-section-title");

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
            featureLabel.setFont(Font.font("Segoe UI Emoji", 14));
            featureLabel.getStyleClass().add("about-feature");
            featuresBox.getChildren().add(featureLabel);
        }

        // 기술 스택
        VBox techBox = new VBox(5);
        techBox.setAlignment(Pos.CENTER_LEFT);

        Label techTitle = new Label("🛠️ 기술 스택:");
        techTitle.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 16));
        techTitle.getStyleClass().add("about-section-title");

        Label techLabel = new Label("• JavaFX 17.0.2 + Lombok + SQLite + Gson");
        techLabel.setFont(Font.font("Segoe UI", 14));
        techLabel.getStyleClass().add("about-tech");

        techBox.getChildren().addAll(techTitle, techLabel);

        // 개발자 정보
        VBox developerBox = new VBox(5);
        developerBox.setAlignment(Pos.CENTER);

        Label developerLabel = new Label("개발: Smart File Manager Team");
        developerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        developerLabel.getStyleClass().add("about-developer");

        Label copyrightLabel = new Label("© 2024 All rights reserved");
        copyrightLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        copyrightLabel.getStyleClass().add("about-copyright");

        developerBox.getChildren().addAll(developerLabel, copyrightLabel);

        // GitHub 링크
        Hyperlink githubLink = new Hyperlink("🔗 GitHub에서 소스코드 보기");
        githubLink.setFont(Font.font("Segoe UI Emoji", 14));
        githubLink.getStyleClass().add("about-github-link");
        githubLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        java.net.URI.create("https://github.com/balsohn/smart-file-manager"));
            } catch (Exception ex) {
                System.out.println("브라우저를 열 수 없습니다: " + ex.getMessage());
            }
        });

        // 시스템 정보
        VBox systemBox = new VBox(3);
        systemBox.setAlignment(Pos.CENTER_LEFT);

        Label systemTitle = new Label("💻 시스템 정보:");
        systemTitle.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 14));
        systemTitle.getStyleClass().add("about-section-title");

        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        Label javaLabel = new Label("Java: " + javaVersion);
        Label osLabel = new Label("OS: " + osName + " " + osVersion);

        javaLabel.setFont(Font.font("Segoe UI", 12));
        osLabel.setFont(Font.font("Segoe UI", 12));
        javaLabel.getStyleClass().add("about-system-info");
        osLabel.getStyleClass().add("about-system-info");

        systemBox.getChildren().addAll(systemTitle, javaLabel, osLabel);

        // 닫기 버튼
        Button closeButton = new Button("확인");
        closeButton.setPrefWidth(100);
        closeButton.getStyleClass().add("about-close-button");
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
        
        // About 창 Scene을 ThemeManager에 등록 (자동으로 현재 테마 적용됨)
        ThemeManager.registerScene(scene);
        
        // About 창이 닫힐 때 Scene 등록 해제
        aboutStage.setOnHidden(event -> ThemeManager.unregisterScene(scene));
        
        aboutStage.showAndWait();
    }
}