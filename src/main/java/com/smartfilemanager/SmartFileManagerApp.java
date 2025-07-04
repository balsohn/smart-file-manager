package com.smartfilemanager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
        titleLabel.setStyle(
                "-fx-font-size: 28px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2c3e50;"
        );

        // 부제목
        Label subtitleLabel = new Label("AI 기반 파일 자동 정리 도구");
        subtitleLabel.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-text-fill: #7f8c8d;"
        );

        // 상태 라벨
        Label statusLabel = new Label("프로젝트 초기화 완료 ✅");
        statusLabel.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: #27ae60; " +
                        "-fx-font-weight: bold;"
        );

        root.getChildren().addAll(titleLabel, subtitleLabel, statusLabel);

        // 씬 생성
        Scene scene = new Scene(root, 600, 400);

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