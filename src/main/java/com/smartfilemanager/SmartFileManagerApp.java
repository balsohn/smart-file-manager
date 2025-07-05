package com.smartfilemanager;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SmartFileManagerApp extends Application {

    // UI 컴포넌트들
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button organizeButton;
    private TableView<FileInfo> fileTable;
    private Label statisticsLabel;

    // 데이터 모델
    private ObservableList<FileInfo> fileList;

    // 상태 관리
    private boolean isScanning = false;
    private String lastScannedFolder = "";

    @Override
    public void start(Stage primaryStage) {
        // 데이터 초기화
        fileList = FXCollections.observableArrayList();

        // 메인 레이아웃
        BorderPane root = new BorderPane();

        // 메뉴바 생성
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // 중앙 컨텐츠 영역
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        // 씬 생성 및 CSS 적용
        Scene scene = new Scene(root, 1000, 750);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("[WARNING] CSS 파일을 로드할 수 없습니다.");
        }

        // 스테이지 설정
        primaryStage.setTitle(Messages.get("app.window.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
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

        Label titleLabel = new Label(Messages.get("app.title"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

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
        organizeButton = new Button(Messages.get("button.organize"));
        organizeButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        organizeButton.setPrefWidth(120);
        organizeButton.setPrefHeight(40);
        organizeButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5px;");
        organizeButton.setOnAction(e -> handleOrganizeFiles());
        organizeButton.setDisable(true);

        // 설정 버튼
        Button settingsButton = new Button(Messages.get("button.settings"));
        settingsButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        settingsButton.setPrefWidth(120);
        settingsButton.setPrefHeight(40);
        settingsButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 5px;");
        settingsButton.setOnAction(e -> handleSettings());

        // 새로고침 버튼 (재스캔)
        Button refreshButton = new Button("Refresh");
        refreshButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        refreshButton.setPrefWidth(120);
        refreshButton.setPrefHeight(40);
        refreshButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-background-radius: 5px;");
        refreshButton.setOnAction(e -> handleRefresh());
        refreshButton.setDisable(true);

        buttonSection.getChildren().addAll(scanButton, organizeButton, refreshButton, settingsButton);
        return buttonSection;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(10);

        // 테이블 제목과 통계
        HBox tableHeader = new HBox(10);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("File Analysis Results");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setStyle("-fx-text-fill: #495057;");

        statisticsLabel = new Label("No files scanned");
        statisticsLabel.setFont(Font.font("System", 12));
        statisticsLabel.setStyle("-fx-text-fill: #6c757d;");

        tableHeader.getChildren().addAll(tableTitle, new Label(" | "), statisticsLabel);

        // 테이블 생성
        fileTable = new TableView<>();
        fileTable.setPrefHeight(350);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 테이블에 데이터 바인딩
        fileTable.setItems(fileList);

        // 테이블 컬럼 설정
        setupTableColumns();

        // 빈 테이블 플레이스홀더
        Label placeholder = new Label("No files scanned yet.\nClick 'Scan Folder' to analyze files in a directory.");
        placeholder.setStyle("-fx-text-fill: #6c757d; -fx-text-alignment: center;");
        fileTable.setPlaceholder(placeholder);

        // 테이블 선택 이벤트
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showFileDetails(newSelection);
            }
        });

        tableSection.getChildren().addAll(tableHeader, fileTable);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        return tableSection;
    }

    private void setupTableColumns() {
        // 파일명 컬럼
        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("File Name");
        nameColumn.setPrefWidth(250);
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFileName()));

        // 카테고리 컬럼
        TableColumn<FileInfo, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setPrefWidth(120);
        categoryColumn.setCellValueFactory(cellData -> {
            String category = cellData.getValue().getDetectedCategory();
            return new SimpleStringProperty(category != null ? category : "Unknown");
        });

        // 서브카테고리 컬럼
        TableColumn<FileInfo, String> subCategoryColumn = new TableColumn<>("Sub Category");
        subCategoryColumn.setPrefWidth(120);
        subCategoryColumn.setCellValueFactory(cellData -> {
            String subCategory = cellData.getValue().getDetectedSubCategory();
            return new SimpleStringProperty(subCategory != null ? subCategory : "-");
        });

        // 파일 크기 컬럼
        TableColumn<FileInfo, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(100);
        sizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedFileSize()));

        // 상태 컬럼 (색상 표시 포함)
        TableColumn<FileInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(120);
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

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

                    // 현재 행의 FileInfo 객체 가져오기
                    FileInfo fileInfo = getTableRow().getItem();
                    if (fileInfo != null) {
                        ProcessingStatus status = fileInfo.getStatus();
                        switch (status) {
                            case PENDING:
                                setStyle("-fx-text-fill: #6c757d; -fx-font-weight: normal;");
                                break;
                            case SCANNING:
                                setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;");
                                break;
                            case ANALYZED:
                                setStyle("-fx-text-fill: #17a2b8; -fx-font-weight: bold;");
                                break;
                            case ORGANIZING:
                                setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                                break;
                            case ORGANIZED:
                                setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                                break;
                            case FAILED:
                                setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                                break;
                            case SKIPPED:
                                setStyle("-fx-text-fill: #6f42c1; -fx-font-weight: normal;");
                                break;
                        }
                    }
                }
            }
        });

        // 신뢰도 컬럼
        TableColumn<FileInfo, String> confidenceColumn = new TableColumn<>("Confidence");
        confidenceColumn.setPrefWidth(90);
        confidenceColumn.setCellValueFactory(cellData -> {
            double confidence = cellData.getValue().getConfidenceScore();
            return new SimpleStringProperty(String.format("%.1f%%", confidence * 100));
        });

        // 제안 경로 컬럼
        TableColumn<FileInfo, String> suggestedPathColumn = new TableColumn<>("Suggested Path");
        suggestedPathColumn.setPrefWidth(200);
        suggestedPathColumn.setCellValueFactory(cellData -> {
            String path = cellData.getValue().getSuggestedPath();
            return new SimpleStringProperty(path != null ? path : "Not determined");
        });

        fileTable.getColumns().addAll(nameColumn, categoryColumn, subCategoryColumn, sizeColumn, statusColumn, confidenceColumn, suggestedPathColumn);
    }

    private HBox createStatusSection() {
        HBox statusSection = new HBox(15);
        statusSection.setPadding(new Insets(10, 0, 0, 0));
        statusSection.setAlignment(Pos.CENTER_LEFT);

        // 상태 라벨
        statusLabel = new Label(Messages.get("app.status.ready"));
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #28a745;");
        statusLabel.setPrefWidth(350);
        statusLabel.setMaxWidth(350);

        // 진행률 바
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(20);
        progressBar.setVisible(false);

        // 진행률 라벨
        progressLabel = new Label("");
        progressLabel.setFont(Font.font("System", 12));
        progressLabel.setStyle("-fx-text-fill: #6c757d;");
        progressLabel.setVisible(false);

        statusSection.getChildren().addAll(statusLabel, progressBar, progressLabel);
        return statusSection;
    }

    // ========== 이벤트 핸들러들 ==========

    private void handleOpenFolder() {
        System.out.println("[INFO] Open Folder clicked");

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Organize");

        // 기본 폴더 설정
        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        File selectedDirectory = directoryChooser.showDialog(statusLabel.getScene().getWindow());

        if (selectedDirectory != null) {
            System.out.println("[INFO] Selected folder: " + selectedDirectory.getAbsolutePath());
            lastScannedFolder = selectedDirectory.getAbsolutePath();
            startFileScan(selectedDirectory);
        } else {
            System.out.println("[INFO] Folder selection cancelled");
        }
    }

    private void handleSettings() {
        System.out.println("[INFO] Settings clicked");
        showInfoDialog("Settings", "Settings dialog will be implemented in the next phase.\n\nPlanned features:\n- Auto-organize settings\n- File type rules\n- Folder structure customization");
    }

    private void handleScanFiles() {
        System.out.println("[INFO] Scan Files clicked");
        handleOpenFolder();
    }

    private void handleOrganizeFiles() {
        System.out.println("[INFO] Organize Files clicked");

        if (fileList.isEmpty()) {
            showInfoDialog("No Files", "Please scan a folder first to analyze files.");
            return;
        }

        List<FileInfo> analyzedFiles = fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ANALYZED)
                .collect(Collectors.toList());

        if (analyzedFiles.isEmpty()) {
            showInfoDialog("No Files Ready", "No files are ready for organization.\nPlease complete the analysis first.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Organize Files");
        confirmAlert.setHeaderText("Ready to organize " + analyzedFiles.size() + " files");
        confirmAlert.setContentText("This will move files to their suggested locations.\nThis action cannot be undone automatically.\n\nProceed?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implement actual file organization in next phase
                showInfoDialog("Feature Coming Soon", "File organization will be implemented in Phase 4.\n\nFor now, you can see the suggested paths in the table.");
            }
        });
    }

    private void handleFindDuplicates() {
        System.out.println("[INFO] Find Duplicates clicked");

        if (fileList.isEmpty()) {
            showInfoDialog("No Files", "Please scan a folder first to find duplicates.");
            return;
        }

        showInfoDialog("Feature Coming Soon", "Duplicate detection will be implemented in Phase 6.\n\nThis feature will:\n- Find exact file duplicates\n- Detect similar files\n- Suggest which files to keep");
    }

    private void handleRefresh() {
        if (!lastScannedFolder.isEmpty()) {
            File folder = new File(lastScannedFolder);
            if (folder.exists()) {
                startFileScan(folder);
            } else {
                showInfoDialog("Folder Not Found", "The previously scanned folder no longer exists.\nPlease select a new folder.");
            }
        }
    }

    private void handleAbout() {
        showInfoDialog("About Smart File Manager",
                Messages.get("app.title") + " v1.0\n" +
                        Messages.get("app.subtitle") + "\n\n" +
                        "Built with JavaFX and Lombok\n" +
                        "© 2025 Smart File Manager\n\n" +
                        "Current Features:\n" +
                        "✓ File scanning and analysis\n" +
                        "✓ Category detection\n" +
                        "✓ Path suggestion\n" +
                        "⏳ File organization (Phase 4)\n" +
                        "⏳ Duplicate detection (Phase 6)");
    }

    private void handleHelpTopics() {
        showInfoDialog("Help", "Smart File Manager Help\n\n" +
                "1. Click 'Scan Folder' to analyze files\n" +
                "2. Review the analysis results in the table\n" +
                "3. Files are automatically categorized\n" +
                "4. Suggested paths show where files will be moved\n" +
                "5. Use 'Organize Files' to execute the organization\n\n" +
                "Status meanings:\n" +
                "• Pending: Waiting to be processed\n" +
                "• Analyzed: Ready for organization\n" +
                "• Organized: Successfully moved\n" +
                "• Failed: Error occurred\n\n" +
                "Documentation: Coming soon!");
    }

    // ========== 파일 스캔 기능 ==========

    private void startFileScan(File directory) {
        if (isScanning) {
            System.out.println("[WARNING] Scan already in progress");
            return;
        }

        updateUIForScanStart();

        Task<List<FileInfo>> scanTask = new Task<List<FileInfo>>() {
            @Override
            protected List<FileInfo> call() throws Exception {
                List<FileInfo> scannedFiles = new ArrayList<>();

                File[] files = directory.listFiles();
                if (files == null) {
                    return scannedFiles;
                }

                int totalFiles = (int) java.util.Arrays.stream(files)
                        .filter(File::isFile)
                        .count();
                int processedFiles = 0;

                System.out.println("[INFO] Starting scan of " + totalFiles + " files in " + directory.getAbsolutePath());

                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            FileInfo fileInfo = createFileInfo(file);
                            scannedFiles.add(fileInfo);

                            processedFiles++;
                            final int currentProgress = processedFiles;

                            Platform.runLater(() -> {
                                double progress = (double) currentProgress / totalFiles;
                                progressBar.setProgress(progress);
                                progressLabel.setText(currentProgress + " / " + totalFiles + " files analyzed");
                                statusLabel.setText("Analyzing: " + file.getName());

                                // 실시간으로 테이블에 파일 추가
                                fileList.add(fileInfo);
                                updateStatistics();
                            });

                            // 시뮬레이션 지연 (실제로는 파일 분석 시간)
                            Thread.sleep(100);

                        } catch (Exception e) {
                            System.err.println("[ERROR] Failed to analyze file: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }

                return scannedFiles;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> updateUIForScanComplete(getValue()));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> updateUIForScanError(getException()));
            }
        };

        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private FileInfo createFileInfo(File file) {
        return FileInfo.defaultBuilder()
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .originalLocation(file.getParent())
                .fileSize(file.length())
                .fileExtension(getFileExtension(file.getName()))
                .detectedCategory(detectCategoryFromExtension(file.getName()))
                .detectedSubCategory(detectSubCategory(file.getName()))
                .suggestedPath(generateSuggestedPath(file.getName()))
                .confidenceScore(calculateConfidenceScore(file.getName()))
                .status(ProcessingStatus.ANALYZED)
                .processedAt(LocalDateTime.now())
                .build();
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    private String detectCategoryFromExtension(String fileName) {
        String extension = getFileExtension(fileName);

        if (extension.matches("jpg|jpeg|png|gif|bmp|svg|webp|tiff|ico")) {
            return "Images";
        }
        if (extension.matches("pdf|doc|docx|txt|rtf|odt|xls|xlsx|ppt|pptx")) {
            return "Documents";
        }
        if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp")) {
            return "Videos";
        }
        if (extension.matches("mp3|wav|flac|aac|m4a|ogg|wma")) {
            return "Audio";
        }
        if (extension.matches("zip|rar|7z|tar|gz|bz2|xz")) {
            return "Archives";
        }
        if (extension.matches("exe|msi|dmg|pkg|deb|rpm|app")) {
            return "Applications";
        }
        if (extension.matches("iso|img|vhd|vmdk")) {
            return "Disk Images";
        }

        return "Others";
    }

    private String detectSubCategory(String fileName) {
        String lowerName = fileName.toLowerCase();
        String category = detectCategoryFromExtension(fileName);

        switch (category) {
            case "Images":
                if (lowerName.contains("screenshot") || lowerName.contains("screen")) {
                    return "Screenshots";
                }
                if (lowerName.contains("wallpaper") || lowerName.contains("background")) {
                    return "Wallpapers";
                }
                if (lowerName.matches(".*\\d{8}.*") || lowerName.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                    return "Photos";
                }
                break;

            case "Documents":
                if (lowerName.contains("resume") || lowerName.contains("cv")) {
                    return "Resume";
                }
                if (lowerName.contains("invoice") || lowerName.contains("receipt")) {
                    return "Financial";
                }
                if (lowerName.contains("manual") || lowerName.contains("guide")) {
                    return "Manuals";
                }
                if (lowerName.contains("report")) {
                    return "Reports";
                }
                break;

            case "Videos":
                if (lowerName.contains("tutorial") || lowerName.contains("course")) {
                    return "Educational";
                }
                if (lowerName.contains("movie") || lowerName.contains("film")) {
                    return "Movies";
                }
                break;
        }

        return null;
    }

    private String generateSuggestedPath(String fileName) {
        String userHome = System.getProperty("user.home");
        String category = detectCategoryFromExtension(fileName);
        String subCategory = detectSubCategory(fileName);

        StringBuilder path = new StringBuilder(userHome)
                .append(File.separator)
                .append("Organized Files")
                .append(File.separator)
                .append(category);

        if (subCategory != null) {
            path.append(File.separator).append(subCategory);
        }

        return path.toString();
    }

    private double calculateConfidenceScore(String fileName) {
        String extension = getFileExtension(fileName);

        // 확장자 기반 기본 신뢰도
        double baseScore = 0.7;

        // 알려진 확장자면 높은 신뢰도
        if (!extension.equals("unknown")) {
            baseScore = 0.8;
        }

        // 서브카테고리가 감지되면 신뢰도 증가
        if (detectSubCategory(fileName) != null) {
            baseScore += 0.15;
        }

        // 파일명에 의미있는 키워드가 있으면 신뢰도 증가
        String lowerName = fileName.toLowerCase();
        if (lowerName.matches(".*[a-zA-Z가-힣]{3,}.*")) {
            baseScore += 0.05;
        }

        return Math.min(baseScore, 1.0);
    }

    // ========== UI 상태 업데이트 메서드들 ==========

    private void updateUIForScanStart() {
        isScanning = true;
        statusLabel.setText("Scanning files...");
        statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;");

        progressBar.setProgress(0);
        progressBar.setVisible(true);

        progressLabel.setText("0 / 0 files processed");
        progressLabel.setVisible(true);

        organizeButton.setDisable(true);

        // 이전 스캔 결과 클리어
        fileList.clear();
        updateStatistics();
    }

    private void updateUIForScanComplete(List<FileInfo> fileInfoList) {
        isScanning = false;

        statusLabel.setText("Scan completed: " + fileInfoList.size() + " files analyzed");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

        progressBar.setProgress(1.0);
        progressLabel.setText("Analysis completed successfully");

        // 분석된 파일이 있으면 정리 버튼 활성화
        long analyzedCount = fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ANALYZED)
                .count();

        if (analyzedCount > 0) {
            organizeButton.setDisable(false);
        }

        // 새로고침 버튼 활성화
        ((Button) statusLabel.getScene().lookup(".button:contains('Refresh')")).setDisable(false);

        updateStatistics();

        System.out.println("[SUCCESS] Scanned " + fileInfoList.size() + " files");
        System.out.println("[INFO] Files by category:");

        // 카테고리별 통계 출력
        fileList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        file -> file.getDetectedCategory() != null ? file.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()))
                .forEach((category, count) ->
                        System.out.println("  - " + category + ": " + count + " files"));
    }

    private void updateUIForScanError(Throwable error) {
        isScanning = false;

        statusLabel.setText("Scan failed: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        progressBar.setProgress(0);
        progressLabel.setText("Scan failed");

        System.err.println("[ERROR] Scan failed: " + error.getMessage());
        error.printStackTrace();
    }

    private void updateStatistics() {
        if (fileList.isEmpty()) {
            statisticsLabel.setText("No files scanned");
            return;
        }

        long totalFiles = fileList.size();
        long analyzedFiles = fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ANALYZED)
                .count();

        long organizedFiles = fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ORGANIZED)
                .count();

        long totalSize = fileList.stream()
                .mapToLong(FileInfo::getFileSize)
                .sum();

        // 카테고리별 통계
        java.util.Map<String, Long> categoryStats = fileList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        file -> file.getDetectedCategory() != null ? file.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()));

        String topCategory = categoryStats.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("None");

        statisticsLabel.setText(String.format(
                "Total: %d files (%s) | Analyzed: %d | Most common: %s (%d files)",
                totalFiles, formatFileSize(totalSize), analyzedFiles,
                topCategory, categoryStats.getOrDefault(topCategory, 0L)
        ));
    }

    private void showFileDetails(FileInfo fileInfo) {
        // 선택된 파일의 상세 정보를 상태바에 표시
        String details = String.format(
                "Selected: %s | Category: %s | Size: %s | Confidence: %.1f%%",
                fileInfo.getFileName(),
                fileInfo.getDetectedCategory(),
                fileInfo.getFormattedFileSize(),
                fileInfo.getConfidenceScore() * 100
        );

        statusLabel.setText(details);
        statusLabel.setStyle("-fx-text-fill: #495057; -fx-font-weight: normal;");
    }

    // ========== 유틸리티 메서드들 ==========

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 다이얼로그 크기 조정
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}