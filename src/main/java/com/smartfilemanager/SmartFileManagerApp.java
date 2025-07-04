package com.smartfilemanager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SmartFileManagerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 메인 레이아웃
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f8f9fa;");

        // 제목
        Label titleLabel = new Label("🗂️ Smart File Manager");
        titleLabel.getStyleClass().add("title-label");

        // 부제목
        Label subtitleLabel = new Label("AI 기반 파일 자동 정리 도구");
        subtitleLabel.getStyleClass().add("subtitle-label");

        // 상태 라벨
        Label statusLabel = new Label("프로젝트 초기화 완료 ✅");
        statusLabel.getStyleClass().add("status-ready");

        // 버튼들 (CSS 테스트용)
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button primaryBtn = new Button("📁 스캔");
        primaryBtn.getStyleClass().addAll("button", "button-primary");

        Button successBtn = new Button("🗂️ 정리");
        successBtn.getStyleClass().addAll("button", "button-success");

        Button warningBtn = new Button("⚙️ 설정");
        warningBtn.getStyleClass().addAll("button", "button-warning");

        buttonBox.getChildren().addAll(primaryBtn, successBtn, warningBtn);

        root.getChildren().addAll(titleLabel, subtitleLabel, statusLabel, buttonBox);

        // 씬 생성
        Scene scene = new Scene(root, 600, 400);

        // CSS 스타일시트 적용
        scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );

        // CSS 스타일시트 적용
        scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );

        // 스테이지 설정
        primaryStage.setTitle("Smart File Manager v1.0");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();

        System.out.println("🚀 Smart File Manager 시작됨!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}