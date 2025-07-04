package com.smartfilemanager;

import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class SmartFileManagerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 메인 레이아웃을 BorderPane으로 변경
        BorderPane root = new BorderPane();

        // 메뉴바 생성
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // 중앙 컨텐츠 영역
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        // 씬 생성
        Scene scene = new Scene(root, 800, 600);

        // 스테이지 설정
        primaryStage.setTitle(Messages.get("app.window.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.centerOnScreen();
        primaryStage.show();

        System.out.println("🚀 " + Messages.get("app.title") + " Started!");
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu(Messages.get("menu.file"));
        MenuItem openItem = new MenuItem("Open Folder...");
        MenuItem settingsItem = new MenuItem("Settings");
        MenuItem exitItem = new MenuItem("Exit");

        // 이벤트 핸들러
        openItem.setOnAction(e -> handleOpenFolder());
        settingsItem.setOnAction(e -> handleSettings());
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), settingsItem, new SeparatorMenuItem(), exitItem);

        // Tools Menu
        Menu toolsMenu = new Menu(Messages.get("menu.tools"));
        MenuItem scanItem = new MenuItem("Scan Files");
        MenuItem organizeItem = new MenuItem("Organize Files");
        MenuItem duplicatesItem = new MenuItem("Find Duplicates");

        // 이벤트 핸들러
        scanItem.setOnAction(e -> handleScanFiles());
        organizeItem.setOnAction(e -> handleOrganizeFiles());
        duplicatesItem.setOnAction(e -> handleFindDuplicates());

        toolsMenu.getItems().addAll(scanItem, organizeItem, new SeparatorMenuItem(), duplicatesItem);

        // Help Menu
        Menu helpMenu = new Menu(Messages.get("menu.help"));
        MenuItem aboutItem = new MenuItem("About");
        MenuItem helpTopicsItem = new MenuItem("Help Topics");

        // 이벤트 핸들러
        aboutItem.setOnAction(e -> handleAbout());
        helpTopicsItem.setOnAction(e -> handleHelpTopics());

        helpMenu.getItems().addAll(helpTopicsItem, new SeparatorMenuItem(), aboutItem);

        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);
        return menuBar;
    }

    private VBox createCenterContent() {
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(30));
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setStyle("-fx-background-color: #f8f9fa;");

        // 제목
        Label titleLabel = new Label(Messages.get("app.title"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 부제목
        Label subtitleLabel = new Label(Messages.get("app.subtitle"));
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d;");

        // 상태 라벨
        Label statusLabel = new Label(Messages.get("app.status.ready"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #28a745;");

        centerContent.getChildren().addAll(titleLabel, subtitleLabel, statusLabel);
        return centerContent;
    }

    // 메뉴 이벤트 핸들러들 (일단 기본 구현)
    private void handleOpenFolder() {
        System.out.println("📁 Open Folder clicked");
        showInfoDialog("Open Folder", "Folder selection feature will be implemented next.");
    }

    private void handleSettings() {
        System.out.println("⚙️ Settings clicked");
        showInfoDialog("Settings", "Settings dialog will be implemented later.");
    }

    private void handleScanFiles() {
        System.out.println("🔍 Scan Files clicked");
        showInfoDialog("Scan Files", "File scanning feature will be implemented next.");
    }

    private void handleOrganizeFiles() {
        System.out.println("🗂️ Organize Files clicked");
        showInfoDialog("Organize Files", "File organization feature will be implemented later.");
    }

    private void handleFindDuplicates() {
        System.out.println("🔍 Find Duplicates clicked");
        showInfoDialog("Find Duplicates", "Duplicate detection feature will be implemented later.");
    }

    private void handleAbout() {
        showInfoDialog("About Smart File Manager",
                "Smart File Manager v1.0\n" +
                        "AI-powered File Organization Tool\n\n" +
                        "Built with JavaFX\n" +
                        "© 2024 Smart File Manager");
    }

    private void handleHelpTopics() {
        showInfoDialog("Help", "Help documentation will be available soon.");
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}