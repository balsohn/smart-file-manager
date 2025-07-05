package com.smartfilemanager.ui;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.util.Messages;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * UI 컴포넌트 생성을 담당하는 팩토리 클래스
 * 메인 애플리케이션의 UI 생성 로직을 분리합니다.
 */
public class UIFactory {

    /**
     * 메뉴바 생성
     */
    public static MenuBar createMenuBar(Stage primaryStage, Runnable onOpenFolder,
                                        Runnable onSettings, Runnable onScanFiles,
                                        Runnable onOrganizeFiles, Runnable onFindDuplicates,
                                        Runnable onAbout, Runnable onHelpTopics) {
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
        openItem.setOnAction(e -> onOpenFolder.run());
        settingsItem.setOnAction(e -> onSettings.run());
        exitItem.setOnAction(e -> System.exit(0));

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
        scanItem.setOnAction(e -> onScanFiles.run());
        organizeItem.setOnAction(e -> onOrganizeFiles.run());
        duplicatesItem.setOnAction(e -> onFindDuplicates.run());

        toolsMenu.getItems().addAll(scanItem, organizeItem, new SeparatorMenuItem(), duplicatesItem);

        // Help Menu
        Menu helpMenu = new Menu(Messages.get("menu.help"));
        MenuItem aboutItem = new MenuItem("About");
        MenuItem helpTopicsItem = new MenuItem("Help Topics");

        // 이벤트 핸들러
        aboutItem.setOnAction(e -> onAbout.run());
        helpTopicsItem.setOnAction(e -> onHelpTopics.run());

        helpMenu.getItems().addAll(helpTopicsItem, new SeparatorMenuItem(), aboutItem);

        menuBar.getMenus().addAll(fileMenu, toolsMenu, helpMenu);
        return menuBar;
    }

    /**
     * 제목 섹션 생성
     */
    public static VBox createTitleSection() {
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

    /**
     * 버튼 섹션 생성
     */
    public static HBox createButtonSection(Runnable onScanFiles, Runnable onOrganizeFiles, Runnable onSettings) {
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.setPadding(new Insets(20, 0, 20, 0));

        // 스캔 버튼
        Button scanButton = new Button(Messages.get("button.scan"));
        scanButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        scanButton.setPrefWidth(120);
        scanButton.setPrefHeight(40);
        scanButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 5px;");
        scanButton.setOnAction(e -> onScanFiles.run());

        // 정리 버튼
        Button organizeButton = new Button(Messages.get("button.organize"));
        organizeButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        organizeButton.setPrefWidth(120);
        organizeButton.setPrefHeight(40);
        organizeButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5px;");
        organizeButton.setOnAction(e -> onOrganizeFiles.run());
        organizeButton.setDisable(true); // 처음에는 비활성화

        // 설정 버튼
        Button settingsButton = new Button(Messages.get("button.settings"));
        settingsButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        settingsButton.setPrefWidth(120);
        settingsButton.setPrefHeight(40);
        settingsButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5px;");
        settingsButton.setOnAction(e -> onSettings.run());

        buttonSection.getChildren().addAll(scanButton, organizeButton, settingsButton);

        // organizeButton을 외부에서 접근할 수 있도록 ID 설정
        organizeButton.setId("organizeButton");

        return buttonSection;
    }

    /**
     * 파일 테이블 섹션 생성
     */
    public static VBox createTableSection(ObservableList<FileInfo> fileList) {
        VBox tableSection = new VBox(10);

        // 테이블 제목
        Label tableTitle = new Label("File List");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setStyle("-fx-text-fill: #495057;");

        // 테이블 생성
        TableView<FileInfo> fileTable = new TableView<>();
        fileTable.setPrefHeight(300);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 테이블 컬럼들 (Lombok FileInfo에 맞게 수정)
        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("File Name");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFileName()));

        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Category");
        typeColumn.setPrefWidth(100);
        typeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDetectedCategory()));

        TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(100);
        sizeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedFileSize()));

        TableColumn<FileInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(120);
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        // 상태별 색상 표시
        statusColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // ProcessingStatus enum의 색상 코드 사용
                    TableRow<FileInfo> currentRow = getTableRow();
                    if (currentRow != null && currentRow.getItem() != null) {
                        ProcessingStatus status = currentRow.getItem().getStatus();
                        setStyle("-fx-text-fill: " + status.getColorCode() + ";");
                    }
                }
            }
        });

        fileTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn, statusColumn);

        // 빈 테이블 플레이스홀더
        Label placeholder = new Label("No files scanned yet. Click 'Scan Folder' to begin.");
        placeholder.setStyle("-fx-text-fill: #6c757d;");
        fileTable.setPlaceholder(placeholder);

        // 파일 목록 바인딩
        fileTable.setItems(fileList);

        // 테이블에 ID 설정 (외부에서 접근용)
        fileTable.setId("fileTable");

        tableSection.getChildren().addAll(tableTitle, fileTable);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        return tableSection;
    }

    /**
     * 상태 섹션 생성
     */
    public static HBox createStatusSection() {
        HBox statusSection = new HBox(15);
        statusSection.setPadding(new Insets(10, 0, 0, 0));
        statusSection.setAlignment(Pos.CENTER_LEFT);

        // 상태 라벨 (고정 폭 설정)
        Label statusLabel = new Label(Messages.get("app.status.ready"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #28a745;");
        statusLabel.setPrefWidth(300);  // 폭을 300px로 고정
        statusLabel.setMaxWidth(300);   // 최대 폭도 300px로 제한
        statusLabel.setId("statusLabel"); // ID 설정

        // 진행률 바
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(20);
        progressBar.setVisible(false); // 처음에는 숨김
        progressBar.setId("progressBar"); // ID 설정

        // 진행률 라벨
        Label progressLabel = new Label("");
        progressLabel.setFont(Font.font("System", 12));
        progressLabel.setStyle("-fx-text-fill: #6c757d;");
        progressLabel.setVisible(false); // 처음에는 숨김
        progressLabel.setId("progressLabel"); // ID 설정

        statusSection.getChildren().addAll(statusLabel, progressBar, progressLabel);
        return statusSection;
    }

    /**
     * 정보 다이얼로그 표시
     */
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 확인 다이얼로그 표시
     */
    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }
}