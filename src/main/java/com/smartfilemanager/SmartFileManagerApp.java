package com.smartfilemanager;

import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

        System.out.println("[START] " + Messages.get("app.title") + " Started!");
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu(Messages.get("menu.file"));
        MenuItem openItem = new MenuItem("Open Folder...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        // 이벤트 핸들러
        openItem.setOnAction(e -> handleOpenFolder());
        settingsItem.setOnAction(e -> handleSettings());
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), settingsItem, new SeparatorMenuItem(), exitItem);

        // Tools Menu
        Menu toolsMenu = new Menu(Messages.get("menu.tools"));
        MenuItem scanItem = new MenuItem("Scan Files");
        scanItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));

        MenuItem organizeItem = new MenuItem("Organize Files");
        organizeItem.setAccelerator(new KeyCodeCombination(KeyCode.F6));

        MenuItem duplicatesItem = new MenuItem("Find Duplicates");
        duplicatesItem.setAccelerator(new KeyCodeCombination(KeyCode.F7));

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
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setStyle("-fx-background-color: #f8f9fa;");

        // 제목 섹션
        VBox titleSection = createTitleSection();

        // 버튼 섹션
        HBox buttonSection = createButtonSection();

        // 파일 목록 테이블 섹션
        VBox tableSection = createTableSection();

        // 상태 섹션
        HBox statusSection = createStatusSection();

        centerContent.getChildren().addAll(titleSection, buttonSection, tableSection, statusSection);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        return centerContent;
    }

    private VBox createTitleSection() {
        VBox titleSection = new VBox(10);
        titleSection.setAlignment(Pos.CENTER);

        // 제목
        Label titleLabel = new Label(Messages.get("app.title"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        // 부제목
        Label subtitleLabel = new Label(Messages.get("app.subtitle"));
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d;");

        titleSection.getChildren().addAll(titleLabel, subtitleLabel);
        return titleSection;
    }

    private HBox createButtonSection() {
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.setPadding(new Insets(20, 0, 20, 0));

        // 스캔 버튼
        Button scanButton = new Button(Messages.get("button.scan"));
        scanButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        scanButton.setPrefWidth(120);
        scanButton.setPrefHeight(40);
        scanButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 5px;");
        scanButton.setOnAction(e -> handleScanFiles());

        // 정리 버튼
        Button organizeButton = new Button(Messages.get("button.organize"));
        organizeButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        organizeButton.setPrefWidth(120);
        organizeButton.setPrefHeight(40);
        organizeButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5px;");
        organizeButton.setOnAction(e -> handleOrganizeFiles());
        organizeButton.setDisable(true); // 처음에는 비활성화

        // 설정 버튼
        Button settingsButton = new Button(Messages.get("button.settings"));
        settingsButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        settingsButton.setPrefWidth(120);
        settingsButton.setPrefHeight(40);
        settingsButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5px;");
        settingsButton.setOnAction(e -> handleSettings());

        buttonSection.getChildren().addAll(scanButton, organizeButton, settingsButton);
        return buttonSection;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(10);

        // 테이블 제목
        Label tableTitle = new Label("File List");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setStyle("-fx-text-fill: #495057;");

        // 테이블 생성 (일단 기본 구조만)
        TableView<String> fileTable = new TableView<>();
        fileTable.setPrefHeight(300);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 테이블 컬럼들
        TableColumn<String, String> nameColumn = new TableColumn<>("File Name");
        nameColumn.setPrefWidth(200);

        TableColumn<String, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setPrefWidth(100);

        TableColumn<String, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(100);

        TableColumn<String, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(120);

        fileTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn, statusColumn);

        // 빈 테이블 플레이스홀더
        Label placeholder = new Label("No files scanned yet. Click 'Scan Folder' to begin.");
        placeholder.setStyle("-fx-text-fill: #6c757d;");
        fileTable.setPlaceholder(placeholder);

        tableSection.getChildren().addAll(tableTitle, fileTable);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        return tableSection;
    }

    private HBox createStatusSection() {
        HBox statusSection = new HBox();
        statusSection.setPadding(new Insets(10, 0, 0, 0));
        statusSection.setAlignment(Pos.CENTER_LEFT);

        // 상태 라벨
        Label statusLabel = new Label(Messages.get("app.status.ready"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #28a745;");

        statusSection.getChildren().add(statusLabel);
        return statusSection;
    }

    // 메뉴 이벤트 핸들러들 (이모지 제거)
    private void handleOpenFolder() {
        System.out.println("[INFO] Open Folder clicked");
        showInfoDialog("Open Folder", "Folder selection feature will be implemented next.");
    }

    private void handleSettings() {
        System.out.println("[INFO] Settings clicked");
        showInfoDialog("Settings", "Settings dialog will be implemented later.");
    }

    private void handleScanFiles() {
        System.out.println("[INFO] Scan Files clicked");
        showInfoDialog("Scan Files", "File scanning feature will be implemented next.");
    }

    private void handleOrganizeFiles() {
        System.out.println("[INFO] Organize Files clicked");
        showInfoDialog("Organize Files", "File organization feature will be implemented later.");
    }

    private void handleFindDuplicates() {
        System.out.println("[INFO] Find Duplicates clicked");
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