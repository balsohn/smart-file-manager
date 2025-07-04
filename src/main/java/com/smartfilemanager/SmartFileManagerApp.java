package com.smartfilemanager;

import com.smartfilemanager.model.FileInfo;  // FileInfo 모델 클래스 임포트
import com.smartfilemanager.model.ProcessingStatus;  // ProcessingStatus 열거형 임포트
import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;  // 백그라운드 작업을 위한 Task 클래스 추가
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
import javafx.stage.DirectoryChooser;  // 폴더 선택 다이얼로그를 위한 DirectoryChooser 추가
import javafx.stage.Stage;

import java.io.File;  // 파일 및 폴더 작업을 위한 File 클래스 추가
import java.nio.file.Files;  // 파일 속성 읽기를 위한 Files 클래스 추가
import java.nio.file.Path;  // 파일 경로 작업을 위한 Path 클래스 추가
import java.time.LocalDateTime;  // 날짜/시간 처리를 위한 LocalDateTime 추가
import java.util.ArrayList;  // 동적 리스트를 위한 ArrayList 추가
import java.util.List;  // 리스트 인터페이스 추가

public class SmartFileManagerApp extends Application {

    // UI 컴포넌트들을 클래스 필드로 선언 (다른 메서드에서 접근하기 위해)
    private Label statusLabel;          // 상태 표시 라벨
    private ProgressBar progressBar;    // 진행률 바
    private Label progressLabel;        // 진행률 텍스트 라벨
    private Button organizeButton;      // 정리 버튼 (스캔 후 활성화)
    private TableView<FileInfo> fileTable;  // 파일 목록 테이블 (나중에 FileInfo 클래스 생성 예정)

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
        Scene scene = new Scene(root, 900, 700);

        // 스테이지 설정
        primaryStage.setTitle(Messages.get("app.window.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
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
        organizeButton = new Button(Messages.get("button.organize")); // 필드에 할당
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

        // 테이블 생성 (필드에 할당)
        fileTable = new TableView<>(); // 필드에 할당
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
        HBox statusSection = new HBox(15);
        statusSection.setPadding(new Insets(10, 0, 0, 0));
        statusSection.setAlignment(Pos.CENTER_LEFT);

        // 상태 라벨 (필드에 할당)
        statusLabel = new Label(Messages.get("app.status.ready")); // 필드에 할당
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #28a745;");

        // 진행률 바 (필드에 할당)
        progressBar = new ProgressBar(0); // 필드에 할당
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(20);
        progressBar.setVisible(false); // 처음에는 숨김

        // 진행률 라벨 (필드에 할당)
        progressLabel = new Label(""); // 필드에 할당
        progressLabel.setFont(Font.font("System", 12));
        progressLabel.setStyle("-fx-text-fill: #6c757d;");
        progressLabel.setVisible(false); // 처음에는 숨김

        statusSection.getChildren().addAll(statusLabel, progressBar, progressLabel);
        return statusSection;
    }

    // 메뉴 이벤트 핸들러들 - 폴더 선택 기능 구현
    private void handleOpenFolder() {
        System.out.println("[INFO] Open Folder clicked");

        // DirectoryChooser 생성 및 설정
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Organize");

        // 기본 폴더를 사용자의 홈 디렉토리로 설정
        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads"); // Downloads 폴더를 기본으로
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome)); // Downloads가 없으면 홈 디렉토리
        }

        // 폴더 선택 다이얼로그 표시
        File selectedDirectory = directoryChooser.showDialog(statusLabel.getScene().getWindow());

        if (selectedDirectory != null) {
            System.out.println("[INFO] Selected folder: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory); // 선택된 폴더 스캔 시작
        } else {
            System.out.println("[INFO] Folder selection cancelled");
        }
    }

    private void handleSettings() {
        System.out.println("[INFO] Settings clicked");
        showInfoDialog("Settings", "Settings dialog will be implemented later.");
    }

    private void handleScanFiles() {
        System.out.println("[INFO] Scan Files clicked");
        handleOpenFolder(); // 스캔 버튼은 폴더 선택과 동일한 동작
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

    // 파일 스캔 기능 구현 (백그라운드에서 실행)
    private void startFileScan(File directory) {
        // UI 상태 업데이트 (스캔 시작)
        updateUIForScanStart();

        // 백그라운드 Task 생성
        Task<List<FileInfo>> scanTask = new Task<List<FileInfo>>() {
            @Override
            protected List<FileInfo> call() throws Exception {
                List<FileInfo> fileInfoList = new ArrayList<>();

                // 디렉토리 내 모든 파일 스캔
                File[] files = directory.listFiles();
                if (files == null) {
                    return fileInfoList; // 빈 리스트 반환
                }

                int totalFiles = files.length;
                int processedFiles = 0;

                System.out.println("[INFO] Starting scan of " + totalFiles + " items");

                for (File file : files) {
                    if (file.isFile()) { // 파일만 처리 (디렉토리 제외)
                        try {
                            // 파일 정보 생성 (임시로 간단한 문자열 사용, 나중에 FileInfo 클래스로 교체)
                            FileInfo fileInfo = createFileInfo(file);
                            fileInfoList.add(fileInfo);

                            // 진행률 업데이트
                            processedFiles++;
                            final int currentProgress = processedFiles;

                            // UI 업데이트 (JavaFX Application Thread에서 실행)
                            Platform.runLater(() -> {
                                double progress = (double) currentProgress / totalFiles;
                                progressBar.setProgress(progress);
                                progressLabel.setText(currentProgress + " / " + totalFiles + " files processed");
                                statusLabel.setText("Scanning: " + file.getName());
                            });

                            // 시뮬레이션을 위한 약간의 지연 (실제로는 제거해도 됨)
                            Thread.sleep(50);

                        } catch (Exception e) {
                            System.err.println("[ERROR] Failed to process file: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }

                return fileInfoList;
            }

            @Override
            protected void succeeded() {
                // 스캔 성공 시 UI 업데이트
                List<FileInfo> result = getValue();
                Platform.runLater(() -> updateUIForScanComplete(result));
            }

            @Override
            protected void failed() {
                // 스캔 실패 시 UI 업데이트
                Platform.runLater(() -> updateUIForScanError(getException()));
            }
        };

        // 백그라운드 스레드에서 Task 실행
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true); // 애플리케이션 종료 시 함께 종료
        scanThread.start();
    }

    // 임시 FileInfo 생성 메서드 (Lombok FileInfo 사용)
    private FileInfo createFileInfo(File file) {
        // Lombok 빌더 패턴 사용
        return FileInfo.defaultBuilder()
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .originalLocation(file.getAbsolutePath())
                .fileSize(file.length())
                .fileExtension(getFileExtension(file.getName()))
                .detectedCategory(detectCategoryFromExtension(file.getName()))
                .status(ProcessingStatus.ANALYZED)
                .processedAt(LocalDateTime.now())
                .build();
    }

    // 파일 확장자 추출 헬퍼 메서드
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    // 확장자로 기본 카테고리 감지
    private String detectCategoryFromExtension(String fileName) {
        String extension = getFileExtension(fileName);

        // 이미지 파일
        if (extension.matches("jpg|jpeg|png|gif|bmp|svg|webp")) {
            return "Images";
        }
        // 문서 파일
        if (extension.matches("pdf|doc|docx|txt|rtf|odt")) {
            return "Documents";
        }
        // 비디오 파일
        if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm")) {
            return "Videos";
        }
        // 오디오 파일
        if (extension.matches("mp3|wav|flac|aac|m4a|ogg")) {
            return "Audio";
        }
        // 압축 파일
        if (extension.matches("zip|rar|7z|tar|gz|bz2")) {
            return "Archives";
        }

        return "Others";
    }

    // 스캔 시작 시 UI 업데이트
    private void updateUIForScanStart() {
        statusLabel.setText("Scanning files...");
        statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;"); // 파란색으로 변경

        progressBar.setProgress(0);
        progressBar.setVisible(true);

        progressLabel.setText("0 / 0 files processed");
        progressLabel.setVisible(true);

        organizeButton.setDisable(true); // 정리 버튼 비활성화
    }

    // 스캔 완료 시 UI 업데이트
    private void updateUIForScanComplete(List<FileInfo> fileList) {
        statusLabel.setText("Scan completed: " + fileList.size() + " files found");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // 초록색으로 변경

        progressBar.setProgress(1.0);
        progressLabel.setText("Scan completed successfully");

        // 파일이 있으면 정리 버튼 활성화
        if (!fileList.isEmpty()) {
            organizeButton.setDisable(false);
        }

        // TODO: 나중에 실제 테이블에 데이터 표시
        System.out.println("[SUCCESS] Scanned " + fileList.size() + " files");
        for (FileInfo info : fileList) {
            System.out.println("  - " + info.getFileName() + " (" +
                    info.getDetectedCategory() + ", " +
                    info.getFormattedFileSize() + ")");
        }
    }

    // 스캔 오류 시 UI 업데이트
    private void updateUIForScanError(Throwable error) {
        statusLabel.setText("Scan failed: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // 빨간색으로 변경

        progressBar.setProgress(0);
        progressLabel.setText("Scan failed");

        System.err.println("[ERROR] Scan failed: " + error.getMessage());
        error.printStackTrace();
    }

    // 파일 크기 포맷팅 유틸리티 메서드
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static void main(String[] args) {
        launch(args);
    }
}