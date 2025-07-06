package com.smartfilemanager.test;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.service.FileWatcherService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 실시간 모니터링 시스템 전용 테스트
 * 폴더 감시 및 자동 정리 기능을 독립적으로 테스트합니다
 */
public class MonitoringSystemTest extends Application {

    private FileWatcherService fileWatcher;
    private ObservableList<FileInfo> monitoredFiles;

    // UI 컴포넌트
    private Label statusLabel;
    private Label folderLabel;
    private Button startButton;
    private Button stopButton;
    private Button selectFolderButton;
    private ListView<String> activityLog;
    private TableView<FileInfo> fileTable;

    private String selectedFolder;

    @Override
    public void start(Stage primaryStage) {
        System.out.println("🔍 실시간 모니터링 시스템 테스트 시작");

        // 기본 모니터링 폴더 설정
        selectedFolder = System.getProperty("user.home") + File.separator + "Downloads";

        // 데이터 초기화
        monitoredFiles = FXCollections.observableArrayList();

        // FileWatcher 초기화
        initializeFileWatcher();

        // UI 생성
        VBox root = createUI();

        // 씬 설정
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("🔍 실시간 모니터링 테스트");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 종료 시 정리
        primaryStage.setOnCloseRequest(e -> {
            if (fileWatcher != null) {
                fileWatcher.shutdown();
            }
            System.out.println("🔍 모니터링 테스트 종료");
        });

        // 초기 상태 메시지
        addLogMessage("✅ 모니터링 시스템 준비 완료");
        addLogMessage("📁 기본 폴더: " + selectedFolder);
        addLogMessage("▶️ '모니터링 시작' 버튼을 클릭하세요");
    }

    /**
     * FileWatcher 초기화 및 콜백 설정
     */
    private void initializeFileWatcher() {
        fileWatcher = new FileWatcherService();

        // 콜백 설정
        fileWatcher.setStatusUpdateCallback(this::updateStatus);
        fileWatcher.setNewFileCallback(this::handleNewFile);
        fileWatcher.setFileList(monitoredFiles);

        // 설정 로드
        try {
            ConfigService configService = new ConfigService();
            AppConfig config = configService.loadConfig();

            // 테스트용 설정 조정
            config.setAutoOrganizeEnabled(true);
            config.setShowNotifications(true);
            config.setOrganizationRootFolder(selectedFolder + File.separator + "Organized");

            fileWatcher.updateConfig(config);

        } catch (Exception e) {
            System.err.println("설정 로드 실패: " + e.getMessage());
        }
    }

    /**
     * UI 생성
     */
    private VBox createUI() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // 헤더
        Label titleLabel = new Label("🔍 실시간 폴더 모니터링 테스트");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 폴더 선택 영역
        VBox folderSection = createFolderSection();

        // 컨트롤 버튼 영역
        VBox controlSection = createControlSection();

        // 상태 표시 영역
        VBox statusSection = createStatusSection();

        // 파일 테이블
        VBox tableSection = createTableSection();

        // 활동 로그
        VBox logSection = createLogSection();

        root.getChildren().addAll(
                titleLabel,
                new Separator(),
                folderSection,
                controlSection,
                statusSection,
                new Separator(),
                tableSection,
                logSection
        );

        return root;
    }

    /**
     * 폴더 선택 영역
     */
    private VBox createFolderSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("📁 모니터링 폴더");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        folderLabel = new Label("폴더: " + selectedFolder);
        folderLabel.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-border-color: #dee2e6;");

        selectFolderButton = new Button("📂 폴더 변경");
        selectFolderButton.setOnAction(e -> selectFolder());

        section.getChildren().addAll(sectionTitle, folderLabel, selectFolderButton);
        return section;
    }

    /**
     * 컨트롤 버튼 영역
     */
    private VBox createControlSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("🎮 모니터링 제어");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        startButton = new Button("▶️ 모니터링 시작");
        startButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setOnAction(e -> startMonitoring());

        stopButton = new Button("⏹️ 모니터링 중지");
        stopButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setOnAction(e -> stopMonitoring());
        stopButton.setDisable(true);

        Button testButton = new Button("🧪 테스트 파일 생성");
        testButton.setOnAction(e -> createTestFile());

        section.getChildren().addAll(sectionTitle, startButton, stopButton, testButton);
        return section;
    }

    /**
     * 상태 표시 영역
     */
    private VBox createStatusSection() {
        VBox section = new VBox(5);

        Label sectionTitle = new Label("📊 상태 정보");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        statusLabel = new Label("⏸️ 모니터링 대기 중");
        statusLabel.setStyle("-fx-padding: 10; -fx-background-color: #e9ecef; -fx-border-color: #ced4da;");

        section.getChildren().addAll(sectionTitle, statusLabel);
        return section;
    }

    /**
     * 파일 테이블 영역
     */
    private VBox createTableSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("📋 감지된 파일 목록");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        fileTable = new TableView<>();
        fileTable.setItems(monitoredFiles);
        fileTable.setPrefHeight(150);

        // 컬럼 설정
        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("파일명");
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFileName()));
        nameColumn.setPrefWidth(200);

        TableColumn<FileInfo, String> categoryColumn = new TableColumn<>("카테고리");
        categoryColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDetectedCategory()));
        categoryColumn.setPrefWidth(100);

        TableColumn<FileInfo, String> statusColumn = new TableColumn<>("상태");
        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setPrefWidth(100);

        TableColumn<FileInfo, String> timeColumn = new TableColumn<>("감지 시간");
        timeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getCreatedDate();
            String timeStr = time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "";
            return new javafx.beans.property.SimpleStringProperty(timeStr);
        });
        timeColumn.setPrefWidth(100);

        fileTable.getColumns().addAll(nameColumn, categoryColumn, statusColumn, timeColumn);

        section.getChildren().addAll(sectionTitle, fileTable);
        return section;
    }

    /**
     * 활동 로그 영역
     */
    private VBox createLogSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("📝 활동 로그");
        sectionTitle.setStyle("-fx-font-weight: bold;");

        activityLog = new ListView<>();
        activityLog.setPrefHeight(100);

        Button clearLogButton = new Button("🗑️ 로그 지우기");
        clearLogButton.setOnAction(e -> activityLog.getItems().clear());

        section.getChildren().addAll(sectionTitle, activityLog, clearLogButton);
        return section;
    }

    /**
     * 폴더 선택
     */
    private void selectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("모니터링할 폴더 선택");
        chooser.setInitialDirectory(new File(selectedFolder));

        File selected = chooser.showDialog(selectFolderButton.getScene().getWindow());
        if (selected != null) {
            selectedFolder = selected.getAbsolutePath();
            folderLabel.setText("폴더: " + selectedFolder);
            addLogMessage("📁 폴더 변경: " + selectedFolder);
        }
    }

    /**
     * 모니터링 시작
     */
    private void startMonitoring() {
        boolean started = fileWatcher.startWatching(selectedFolder);

        if (started) {
            startButton.setDisable(true);
            stopButton.setDisable(false);
            selectFolderButton.setDisable(true);

            addLogMessage("▶️ 모니터링 시작: " + selectedFolder);
            addLogMessage("💡 이제 폴더에 파일을 추가해보세요!");
        } else {
            addLogMessage("❌ 모니터링 시작 실패");
        }
    }

    /**
     * 모니터링 중지
     */
    private void stopMonitoring() {
        fileWatcher.stopWatching();

        startButton.setDisable(false);
        stopButton.setDisable(true);
        selectFolderButton.setDisable(false);

        addLogMessage("⏹️ 모니터링 중지됨");
    }

    /**
     * 테스트 파일 생성
     */
    private void createTestFile() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "test_file_" + timestamp + ".txt";
            Path testFile = Paths.get(selectedFolder, fileName);

            String content = "테스트 파일입니다.\n생성 시간: " + LocalDateTime.now() + "\n\n" +
                    "이 파일은 실시간 모니터링 테스트용으로 자동 생성되었습니다.";

            Files.write(testFile, content.getBytes());

            addLogMessage("🧪 테스트 파일 생성: " + fileName);

        } catch (IOException e) {
            addLogMessage("❌ 테스트 파일 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 상태 업데이트 콜백
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            addLogMessage("📊 " + message);
        });
    }

    /**
     * 새 파일 감지 콜백
     */
    private void handleNewFile(FileInfo fileInfo) {
        Platform.runLater(() -> {
            fileTable.refresh();
            addLogMessage("🔍 새 파일 감지: " + fileInfo.getFileName() +
                    " (" + fileInfo.getDetectedCategory() + ")");

            if (fileInfo.getStatus().toString().equals("ORGANIZED")) {
                addLogMessage("✅ 자동 정리 완료: " + fileInfo.getFileName());
            }
        });
    }

    /**
     * 로그 메시지 추가
     */
    private void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            activityLog.getItems().add(0, "[" + timestamp + "] " + message);

            // 로그가 너무 많으면 오래된 것 제거
            if (activityLog.getItems().size() > 50) {
                activityLog.getItems().remove(50, activityLog.getItems().size());
            }
        });
    }

    public static void main(String[] args) {
        System.out.println("🔍 실시간 모니터링 시스템 테스트 시작...");
        launch(args);
    }
}