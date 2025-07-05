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
     * 파일 테이블 섹션 생성 (강화된 버전)
     */
    public static VBox createTableSection(ObservableList<FileInfo> fileList) {
        VBox tableSection = new VBox(10);

        // 테이블 헤더 (제목 + 통계)
        HBox tableHeader = createTableHeader(fileList);

        // 테이블 생성
        TableView<FileInfo> fileTable = createEnhancedFileTable(fileList);

        // 파일 상세 정보 패널
        VBox detailPanel = createFileDetailPanel();

        tableSection.getChildren().addAll(tableHeader, fileTable, detailPanel);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        return tableSection;
    }

    /**
     * 테이블 헤더 생성 (제목 + 통계 정보)
     */
    private static HBox createTableHeader(ObservableList<FileInfo> fileList) {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        // 테이블 제목
        Label tableTitle = new Label("File List");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setStyle("-fx-text-fill: #495057;");

        // 통계 라벨
        Label statsLabel = new Label("0 files");
        statsLabel.setId("statsLabel");
        statsLabel.setFont(Font.font("System", 12));
        statsLabel.setStyle("-fx-text-fill: #6c757d;");

        // 실시간 통계 업데이트
        fileList.addListener((javafx.collections.ListChangeListener<FileInfo>) change -> {
            updateStatsLabel(statsLabel, fileList);
        });

        header.getChildren().addAll(tableTitle, statsLabel);
        return header;
    }

    /**
     * 강화된 파일 테이블 생성
     */
    private static TableView<FileInfo> createEnhancedFileTable(ObservableList<FileInfo> fileList) {
        TableView<FileInfo> fileTable = new TableView<>();
        fileTable.setPrefHeight(300);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.setId("fileTable");

        // 컬럼들 생성
        TableColumn<FileInfo, String> nameColumn = createNameColumn();
        TableColumn<FileInfo, String> typeColumn = createTypeColumn();
        TableColumn<FileInfo, String> sizeColumn = createSizeColumn();
        TableColumn<FileInfo, String> statusColumn = createStatusColumn();
        TableColumn<FileInfo, String> dateColumn = createDateColumn();

        // 정렬 가능하도록 설정
        nameColumn.setSortable(true);
        typeColumn.setSortable(true);
        sizeColumn.setSortable(true);
        statusColumn.setSortable(true);
        dateColumn.setSortable(true);

        // 기본 정렬: 파일명 오름차순
        fileTable.getSortOrder().add(nameColumn);

        fileTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn, statusColumn, dateColumn);

        // 빈 테이블 플레이스홀더
        Label placeholder = new Label("📁 No files scanned yet.\n\nClick 'Scan Folder' to analyze files in a directory.");
        placeholder.setStyle("-fx-text-fill: #6c757d; -fx-text-alignment: center;");
        fileTable.setPlaceholder(placeholder);

        // 파일 목록 바인딩
        fileTable.setItems(fileList);

        // 행 선택 이벤트 (상세 정보 표시용)
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateFileDetailPanel(newSelection);
            }
        });

        // 컨텍스트 메뉴 추가
        fileTable.setContextMenu(createTableContextMenu(fileTable));

        return fileTable;
    }

    /**
     * 파일명 컬럼 생성
     */
    private static TableColumn<FileInfo, String> createNameColumn() {
        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("File Name");
        nameColumn.setPrefWidth(250);
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFileName()));

        // 파일명에 아이콘 추가
        nameColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    FileInfo fileInfo = getTableView().getItems().get(getIndex());
                    String icon = getFileIcon(fileInfo.getDetectedCategory());
                    setText(icon + " " + item);
                }
            }
        });

        return nameColumn;
    }

    /**
     * 카테고리 컬럼 생성
     */
    private static TableColumn<FileInfo, String> createTypeColumn() {
        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("Category");
        typeColumn.setPrefWidth(120);
        typeColumn.setCellValueFactory(cellData -> {
            String category = cellData.getValue().getDetectedCategory();
            return new javafx.beans.property.SimpleStringProperty(category != null ? category : "Unknown");
        });
        return typeColumn;
    }

    /**
     * 크기 컬럼 생성
     */
    private static TableColumn<FileInfo, String> createSizeColumn() {
        TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(100);
        sizeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedFileSize()));

        // 크기 기준 정렬을 위해 Comparator 설정
        sizeColumn.setComparator((size1, size2) -> {
            // 문자열을 바이트로 변환해서 비교
            long bytes1 = parseSizeToBytes(size1);
            long bytes2 = parseSizeToBytes(size2);
            return Long.compare(bytes1, bytes2);
        });

        return sizeColumn;
    }

    /**
     * 상태 컬럼 생성 (색상 표시 포함)
     */
    private static TableColumn<FileInfo, String> createStatusColumn() {
        TableColumn<FileInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(120);
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        statusColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            // 모든 상태 스타일 클래스 목록을 미리 정의해둡니다.
            private final String[] statusClasses = {"status-pending", "status-organizing", "status-organized", "status-failed", "status-analyzed", "status-skipped"};

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // 셀이 재사용될 때를 대비해 이전 스타일을 모두 제거합니다.
                getStyleClass().removeAll(statusClasses);
                getStyleClass().remove("status-cell");

                if (empty || item == null) {
                    setText(null);
                } else {
                    // 1. 공통 스타일 클래스를 먼저 추가합니다.
                    getStyleClass().add("status-cell");

                    FileInfo fileInfo = getTableView().getItems().get(getIndex());
                    ProcessingStatus status = fileInfo.getStatus();

                    // 2. 상태에 맞는 특정 스타일 클래스를 추가합니다.
                    // 예: status가 ORGANIZED면 "status-organized" 클래스가 추가됩니다.
                    String statusClassName = "status-" + status.name().toLowerCase();
                    getStyleClass().add(statusClassName);

                    // 텍스트 설정은 그대로 유지합니다.
                    String statusText = getStatusIcon(status) + " " + status.getDisplayName();
                    setText(statusText);
                }
            }
        });

        return statusColumn;
    }

    /**
     * 날짜 컬럼 생성
     */
    private static TableColumn<FileInfo, String> createDateColumn() {
        TableColumn<FileInfo, String> dateColumn = new TableColumn<>("Modified");
        dateColumn.setPrefWidth(140);
        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getModifiedDate() != null) {
                String formattedDate = cellData.getValue().getModifiedDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return new javafx.beans.property.SimpleStringProperty(formattedDate);
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });
        return dateColumn;
    }

    /**
     * 파일 상세 정보 패널 생성
     */
    private static VBox createFileDetailPanel() {
        VBox detailPanel = new VBox(5);
        detailPanel.setPadding(new Insets(10));
        detailPanel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1px;");
        detailPanel.setMaxHeight(100);
        detailPanel.setId("fileDetailPanel");

        Label detailTitle = new Label("File Details");
        detailTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        detailTitle.setStyle("-fx-text-fill: #495057;");

        Label detailContent = new Label("Select a file to view details");
        detailContent.setId("detailContent");
        detailContent.setStyle("-fx-text-fill: #6c757d;");
        detailContent.setWrapText(true);

        detailPanel.getChildren().addAll(detailTitle, detailContent);
        detailPanel.setVisible(false); // 처음에는 숨김

        return detailPanel;
    }

    /**
     * 테이블 컨텍스트 메뉴 생성
     */
    private static ContextMenu createTableContextMenu(TableView<FileInfo> fileTable) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem viewDetails = new MenuItem("🔍 View Details");
        viewDetails.setOnAction(e -> {
            FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFileDetailsDialog(selected);
            }
        });

        MenuItem openLocation = new MenuItem("📂 Open File Location");
        openLocation.setOnAction(e -> {
            FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openFileLocation(selected);
            }
        });

        MenuItem copyPath = new MenuItem("📋 Copy File Path");
        copyPath.setOnAction(e -> {
            FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.getFilePath());
            }
        });

        contextMenu.getItems().addAll(viewDetails, openLocation, new SeparatorMenuItem(), copyPath);
        return contextMenu;
    }

    // 헬퍼 메서드들

    private static void updateStatsLabel(Label statsLabel, ObservableList<FileInfo> fileList) {
        if (fileList.isEmpty()) {
            statsLabel.setText("0 files");
            return;
        }

        long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        long analyzedCount = fileList.stream().filter(f -> f.getStatus().isCompleted()).count();

        statsLabel.setText(String.format("%d files (%s) • %d analyzed",
                fileList.size(), formattedSize, analyzedCount));
    }

    private static String getFileIcon(String category) {
        if (category == null) return "[FILE]";
        switch (category.toLowerCase()) {
            case "images": return "🖼️";
            case "documents": return "📄";
            case "videos": return "🎥";
            case "audio": return "🎵";
            case "archives": return "📦";
            default: return "📄";
        }
    }

    private static String getStatusIcon(ProcessingStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case SCANNING: return "🔍";
            case ANALYZED: return "✅";
            case ORGANIZING: return "⚙️";
            case ORGANIZED: return "🎯";
            case FAILED: return "❌";
            case SKIPPED: return "⏭️";
            default: return "❓";
        }
    }

    private static long parseSizeToBytes(String sizeStr) {
        try {
            if (sizeStr.endsWith(" B")) {
                return Long.parseLong(sizeStr.replace(" B", ""));
            } else if (sizeStr.endsWith(" KB")) {
                return (long) (Double.parseDouble(sizeStr.replace(" KB", "")) * 1024);
            } else if (sizeStr.endsWith(" MB")) {
                return (long) (Double.parseDouble(sizeStr.replace(" MB", "")) * 1024 * 1024);
            } else if (sizeStr.endsWith(" GB")) {
                return (long) (Double.parseDouble(sizeStr.replace(" GB", "")) * 1024 * 1024 * 1024);
            }
        } catch (NumberFormatException e) {
            // 파싱 실패 시 0 반환
        }
        return 0;
    }

    private static void updateFileDetailPanel(FileInfo fileInfo) {
        // This method will be handled by FileDetailManager
        // Just log the selection for now
        System.out.println("[INFO] Selected file: " + fileInfo.getFileName());
    }

    private static void showFileDetailsDialog(FileInfo fileInfo) {
        StringBuilder details = new StringBuilder();
        details.append("File Name: ").append(fileInfo.getFileName()).append("\n");
        details.append("Path: ").append(fileInfo.getFilePath()).append("\n");
        details.append("Size: ").append(fileInfo.getFormattedFileSize()).append("\n");
        details.append("Category: ").append(fileInfo.getDetectedCategory()).append("\n");
        details.append("Extension: ").append(fileInfo.getFileExtension()).append("\n");
        details.append("Status: ").append(fileInfo.getStatus().getDisplayName()).append("\n");
        if (fileInfo.getModifiedDate() != null) {
            details.append("Modified: ").append(fileInfo.getModifiedDate().toString()).append("\n");
        }

        showInfoDialog("File Details", details.toString());
    }

    private static void openFileLocation(FileInfo fileInfo) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                Runtime.getRuntime().exec("explorer /select," + fileInfo.getFilePath());
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open -R " + fileInfo.getFilePath());
            } else {
                // Linux
                Runtime.getRuntime().exec("xdg-open " + fileInfo.getOriginalLocation());
            }
        } catch (Exception e) {
            showInfoDialog("Error", "Could not open file location: " + e.getMessage());
        }
    }

    private static void copyToClipboard(String text) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
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