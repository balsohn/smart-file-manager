package com.smartfilemanager.controller;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.*;
import com.smartfilemanager.ui.AboutDialog;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.HelpDialog;
import com.smartfilemanager.ui.UIFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.smartfilemanager.ui.UIFactory.showInfoDialog;

/**
 * 메인 화면 컨트롤러
 * FXML과 연동되어 사용자 인터페이스를 관리합니다
 */
public class MainController implements Initializable {

    // FXML UI 컴포넌트들
    @FXML private TableView<FileInfo> fileTable;
    @FXML private TableColumn<FileInfo, String> nameColumn;
    @FXML private TableColumn<FileInfo, String> categoryColumn;
    @FXML private TableColumn<FileInfo, String> sizeColumn;
    @FXML private TableColumn<FileInfo, String> statusColumn;
    @FXML private TableColumn<FileInfo, String> dateColumn;

    @FXML private Button scanButton;
    @FXML private Button organizeButton;
    @FXML private Button settingsButton;

    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Label progressLabel;
    @FXML private Label statsLabel;
    @FXML private Label statisticsLabel;

    @FXML private VBox fileDetailPanel;
    @FXML private Label detailContent;

    @FXML private Label aiStatusIndicator;
    @FXML private Label currentFileLabel;
    @FXML private TableColumn<FileInfo, Double> confidenceColumn;
    @FXML private MenuItem batchAIAnalysisMenuItem;
    @FXML private Button aiAnalysisButton;

    @FXML private Button monitoringToggleButton;
    @FXML private Label monitoringStatusLabel;
    @FXML private Label monitoringFolderLabel;

    // 서비스들
    private FileScanService fileScanService;
    private FileOrganizerService fileOrganizerService;
    private UndoService undoService;
    private DuplicateDetectorService duplicateDetectorService;
    private CleanupDetectorService cleanupDetectorService;
    private ConfigService configService;
    private FileAnalysisService fileAnalysisService;
    private FileWatcherService fileWatcherService;
    private boolean isMonitoringActive = false;

    // 데이터
    private ObservableList<FileInfo> fileList;
    private FileDetailManager fileDetailManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[INFO] MainController 초기화 시작");

        // 데이터 초기화
        fileList = FXCollections.observableArrayList();

        // 서비스 초기화
        initializeServices();

        // 테이블 설정
        setupTable();

        // 파일 상세 정보 관리자 초기화
        fileDetailManager = new FileDetailManager(fileDetailPanel);

        // 단축키 설정
        setupKeyboardShortcuts();

        // 리스너 설정
        setupListeners();

        // 초기 UI 상태 설정
        updateUI();

        System.out.println("[SUCCESS] MainController 초기화 완료");
    }

    /**
     * 서비스들 초기화
     */
    private void initializeServices() {
        // 기존 서비스들
        fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        fileOrganizerService = new FileOrganizerService(progressBar, statusLabel, progressLabel);
        undoService = new UndoService(progressBar, statusLabel, progressLabel);
        duplicateDetectorService = new DuplicateDetectorService();
        cleanupDetectorService = new CleanupDetectorService();

        // 추가된 서비스들
        configService = new ConfigService();
        fileAnalysisService = new FileAnalysisService();

        // AI 분석 초기화
        initializeAIAnalysis();
    }

    private void initializeFileWatcher() {
        fileWatcherService = new FileWatcherService();

        // 콜백 설정
        fileWatcherService.setStatusUpdateCallback(this::updateMonitoringStatus);
        fileWatcherService.setNewFileCallback(this::handleNewFileDetected);
        fileWatcherService.setFileList(fileList);

        // UI 초기 상태 설정
        updateMonitoringUI();
    }

    /**
     * 테이블 설정
     */
    private void setupTable() {
        // 컬럼 셀 값 팩토리 설정
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFileName()));

        categoryColumn.setCellValueFactory(cellData -> {
            FileInfo fileInfo = cellData.getValue();
            String category = fileInfo.getDetectedCategory() != null ? fileInfo.getDetectedCategory() : "Unknown";
            String subCategory = fileInfo.getDetectedSubCategory();

            if (subCategory != null && !subCategory.equals("General")) {
                return new javafx.beans.property.SimpleStringProperty(category + "/" + subCategory);
            } else {
                return new javafx.beans.property.SimpleStringProperty(category);
            }
        });

        sizeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedFileSize()));

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getModifiedDate() != null) {
                String formattedDate = cellData.getValue().getModifiedDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return new javafx.beans.property.SimpleStringProperty(formattedDate);
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });

        // 상태 컬럼 색상 설정
        statusColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    FileInfo fileInfo = getTableView().getItems().get(getIndex());
                    ProcessingStatus status = fileInfo.getStatus();

                    setText(getStatusIcon(status) + " " + status.getDisplayName());
                    setStyle("-fx-text-fill: " + status.getColorCode() + "; -fx-font-weight: bold;");
                }
            }
        });

        // 파일명 컬럼에 아이콘 추가
        nameColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    FileInfo fileInfo = getTableView().getItems().get(getIndex());
                    String icon = getFileIcon(fileInfo.getDetectedCategory());
                    setText(icon + " " + item);
                }
            }
        });

        // 데이터 바인딩
        fileTable.setItems(fileList);

        // 정렬 가능하게 설정
        nameColumn.setSortable(true);
        categoryColumn.setSortable(true);
        sizeColumn.setSortable(true);
        statusColumn.setSortable(true);
        dateColumn.setSortable(true);

        // 기본 정렬: 파일명 오름차순
        fileTable.getSortOrder().add(nameColumn);
    }

    /**
     * 리스너 설정
     */
    private void setupListeners() {
        // 파일 리스트 변경 감지
        fileList.addListener((ListChangeListener<FileInfo>) change -> {
            updateStatistics();
            updateOrganizeButtonState();
        });

        // 테이블 선택 감지
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            fileDetailManager.updateFileDetails(newSelection);
        });
    }

    /**
     * UI 상태 업데이트
     */
    private void updateUI() {
        updateStatistics();
        updateOrganizeButtonState();
    }

    /**
     * 통계 정보 업데이트
     */
    private void updateStatistics() {
        if (fileList.isEmpty()) {
            statsLabel.setText("0 files");
            statisticsLabel.setText("분석된 파일: 0개 | 정리된 파일: 0개 | 절약된 공간: 0 B");
            return;
        }

        long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        long analyzedCount = fileList.stream().filter(f -> f.getStatus().isCompleted()).count();
        long organizedCount = fileList.stream().filter(f -> f.getStatus() == ProcessingStatus.ORGANIZED).count();

        statsLabel.setText(String.format("%d files (%s) • %d analyzed",
                fileList.size(), formattedSize, analyzedCount));

        statisticsLabel.setText(String.format("분석된 파일: %d개 | 정리된 파일: %d개 | 총 크기: %s",
                analyzedCount, organizedCount, formattedSize));
    }

    /**
     * 정리 버튼 상태 업데이트
     */
    private void updateOrganizeButtonState() {
        boolean hasProcessableFiles = fileList.stream().anyMatch(file -> file.getStatus().isProcessable());
        organizeButton.setDisable(!hasProcessableFiles);
    }

    /**
     * 상태 라벨 업데이트 (누락된 메서드 추가)
     */
    private void updateStatusLabel(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Alert 표시 (누락된 메서드 추가)
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =================
    // 이벤트 핸들러들
    // =================

    @FXML
    private void handleOpenFolder() {
        System.out.println("[INFO] 폴더 열기 클릭");

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("정리할 폴더 선택");

        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        File selectedDirectory = directoryChooser.showDialog(getCurrentStage());

        if (selectedDirectory != null) {
            System.out.println("[INFO] 선택된 폴더: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory);
        } else {
            System.out.println("[INFO] 폴더 선택 취소됨");
        }
    }

    @FXML
    private void handleScanFiles() {
        System.out.println("[INFO] 파일 스캔 버튼 클릭");
        handleOpenFolder();
    }

    @FXML
    private void handleOrganizeFiles() {
        System.out.println("[INFO] 파일 정리 버튼 클릭");

        List<FileInfo> filesToOrganize = fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .collect(Collectors.toList());

        if (filesToOrganize.isEmpty()) {
            showInfoDialog("📋 정리할 파일 없음",
                    "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.");
            return;
        }

        // 확인 다이얼로그 표시
        boolean confirmed = showOrganizeConfirmDialog(filesToOrganize);
        if (confirmed) {
            startFileOrganization(filesToOrganize);
        }
    }

    @FXML
    private void handleUndoOrganization() {
        System.out.println("[INFO] 정리 되돌리기 버튼 클릭");

        List<FileInfo> undoableFiles = UndoService.getUndoableFiles(new ArrayList<>(fileList));

        if (undoableFiles.isEmpty()) {
            showInfoDialog("↩️ 되돌릴 파일 없음",
                    "정리된 파일이 없습니다.\n파일을 먼저 정리한 후 되돌리기를 사용할 수 있습니다.");
            return;
        }

        boolean confirmed = showUndoConfirmDialog(undoableFiles);
        if (confirmed) {
            startUndoProcess(undoableFiles);
        }
    }

    @FXML
    private void handleFindDuplicates() {
        System.out.println("[INFO] 중복 파일 찾기 버튼 클릭");

        if (fileList.isEmpty()) {
            showInfoDialog("📋 파일 없음",
                    "먼저 폴더를 스캔해서 중복 파일을 찾아주세요.");
            return;
        }

        startDuplicateDetection();
    }

    @FXML
    private void handleCleanupFiles() {
        System.out.println("[INFO] 불필요한 파일 정리 버튼 클릭");

        if (fileList.isEmpty()) {
            showInfoDialog("📋 파일 없음",
                    "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.");
            return;
        }

        startCleanupDetection();
    }

    /**
     * 정보(About) 창 표시
     */
    @FXML
    private void handleAbout() {
        System.out.println("[INFO] About 다이얼로그 표시");
        try {
            AboutDialog.show(getCurrentStage());
        } catch (Exception e) {
            System.err.println("[ERROR] About 다이얼로그 표시 실패: " + e.getMessage());
            showInfoDialog("❌ 오류", "정보 창을 표시할 수 없습니다.");
        }
    }

    /**
     * 도움말 창 표시
     */
    @FXML
    private void handleHelpTopics() {
        System.out.println("[INFO] 도움말 창 표시");
        try {
            HelpDialog.show(getCurrentStage());
        } catch (Exception e) {
            System.err.println("[ERROR] 도움말 창 표시 실패: " + e.getMessage());
            showInfoDialog("❌ 오류", "도움말 창을 표시할 수 없습니다.");
        }
    }

    /**
     * 설정 창 열기
     */
    @FXML
    private void handleSettings() {
        System.out.println("[INFO] 설정 버튼 클릭됨");

        try {
            // FXML 파일 로드
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent settingsRoot = loader.load();

            // 컨트롤러 참조
            SettingsController settingsController = loader.getController();

            // 설정 창 생성
            Stage settingsStage = new Stage();
            settingsStage.setTitle("⚙️ Smart File Manager - 설정");
            settingsStage.setScene(new Scene(settingsRoot, 800, 600));
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(getCurrentStage());
            settingsStage.setResizable(true);
            settingsStage.setMinWidth(700);
            settingsStage.setMinHeight(500);

            // 컨트롤러에 스테이지 전달
            settingsController.setStage(settingsStage);

            // 창 표시 (모달)
            settingsStage.showAndWait();

            System.out.println("[INFO] 설정 창이 닫혔습니다");

        } catch (IOException e) {
            System.err.println("[ERROR] 설정 창 로드 실패: " + e.getMessage());
            showInfoDialog("❌ 오류",
                    "설정 창을 열 수 없습니다:\n" + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] 설정 창에서 예기치 않은 오류: " + e.getMessage());
            showInfoDialog("❌ 오류",
                    "설정 창에서 오류가 발생했습니다:\n" + e.getMessage());
        }
    }

    /**
     * 키보드 단축키 F1으로 도움말 열기
     */
    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            Stage stage = getCurrentStage();
            if (stage != null && stage.getScene() != null) {
                Scene scene = stage.getScene();

                // Scene의 Accelerators를 사용하는 방식 (더 표준적)

                // F1: 도움말
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F1),
                        () -> {
                            System.out.println("[SHORTCUT] F1 - 도움말 실행");
                            handleHelpTopics();
                        }
                );

                // F5: 파일 스캔
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F5),
                        () -> {
                            System.out.println("[SHORTCUT] F5 - 파일 스캔 실행");
                            handleScanFiles();
                        }
                );

                // F6: 파일 정리
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F6),
                        () -> {
                            System.out.println("[SHORTCUT] F6 - 파일 정리 실행");
                            handleOrganizeFiles();
                        }
                );

                // F7: 중복 파일 찾기
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F7),
                        () -> {
                            System.out.println("[SHORTCUT] F7 - 중복 파일 찾기 실행");
                            handleFindDuplicates();
                        }
                );

                // F8: 불필요한 파일 정리
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F8),
                        () -> {
                            System.out.println("[SHORTCUT] F8 - 불필요한 파일 정리 실행");
                            handleCleanupFiles();
                        }
                );

                // Ctrl+O: 폴더 열기
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                        () -> {
                            System.out.println("[SHORTCUT] Ctrl+O - 폴더 열기 실행");
                            handleOpenFolder();
                        }
                );

                // Ctrl+Z: 되돌리기
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                        () -> {
                            System.out.println("[SHORTCUT] Ctrl+Z - 되돌리기 실행");
                            handleUndoOrganization();
                        }
                );

                // Ctrl+Shift+S: 설정
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        () -> {
                            System.out.println("[SHORTCUT] Ctrl+Shift+S - 설정 실행");
                            handleSettings();
                        }
                );

                System.out.println("[SUCCESS] 키보드 단축키 설정 완료");
                System.out.println("          F1=도움말, F5=스캔, F6=정리, F7=중복찾기, F8=정리");
                System.out.println("          Ctrl+O=폴더열기, Ctrl+Z=되돌리기, Ctrl+Shift+S=설정");
            } else {
                System.out.println("[WARNING] 씬이 준비되지 않아 키보드 단축키를 나중에 설정합니다");
            }
        });
    }

    /**
     * 실시간 모니터링 토글 버튼 핸들러
     */
    @FXML
    private void handleMonitoringToggle() {
        if (isMonitoringActive) {
            stopMonitoring();
        } else {
            startMonitoring();
        }
    }

    /**
     * 실시간 모니터링 시작
     */
    private void startMonitoring() {
        AppConfig config = configService.loadConfig();
        String monitoringFolder = config.getDefaultScanFolder();

        if (monitoringFolder == null || monitoringFolder.trim().isEmpty()) {
            // 기본 다운로드 폴더 사용
            monitoringFolder = System.getProperty("user.home") + File.separator + "Downloads";
        }

        // 폴더 선택 다이얼로그 (사용자가 원하는 경우)
        if (!config.isAutoOrganizeEnabled()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("모니터링할 폴더 선택");
            chooser.setInitialDirectory(new File(monitoringFolder));

            File selectedFolder = chooser.showDialog(monitoringToggleButton.getScene().getWindow());
            if (selectedFolder == null) {
                return; // 사용자가 취소
            }
            monitoringFolder = selectedFolder.getAbsolutePath();
        }

        // 모니터링 시작
        boolean started = fileWatcherService.startWatching(monitoringFolder);

        if (started) {
            isMonitoringActive = true;
            updateMonitoringUI();

            // 설정에 저장
            config.setDefaultScanFolder(monitoringFolder);
            config.setRealTimeMonitoring(true);
            configService.saveConfig(config);

            updateMonitoringStatus("실시간 모니터링 활성화됨");

            showInfoDialog("모니터링 시작",
                    "실시간 폴더 모니터링이 시작되었습니다.\n폴더: " + monitoringFolder);

        } else {
            showAlert("모니터링 시작 실패",
                    "폴더 모니터링을 시작할 수 없습니다.\n폴더 경로를 확인해주세요.",
                    Alert.AlertType.ERROR);
        }
    }

    /**
     * 실시간 모니터링 중지
     */
    private void stopMonitoring() {
        fileWatcherService.stopWatching();
        isMonitoringActive = false;
        updateMonitoringUI();

        // 설정 업데이트
        AppConfig config = configService.loadConfig();
        config.setRealTimeMonitoring(false);
        configService.saveConfig(config);

        updateMonitoringStatus("실시간 모니터링 중지됨");
    }

    /**
     * 새 파일 감지 시 호출되는 콜백
     */
    private void handleNewFileDetected(FileInfo newFile) {
        Platform.runLater(() -> {
            // 테이블 업데이트 (이미 fileWatcherService에서 fileList에 추가함)
            fileTable.refresh();

            // 통계 업데이트
            updateStatistics();

            // 상태 메시지 업데이트
            updateStatusLabel("새 파일 감지: " + newFile.getFileName());

            // 자동 정리가 완료된 경우 성공 메시지
            if (newFile.getStatus() == ProcessingStatus.ORGANIZED) {
                showTemporaryMessage("파일 자동 정리: " + newFile.getFileName() + " → " +
                        newFile.getDetectedCategory());
            }
        });
    }

    /**
     * 모니터링 상태 업데이트
     */
    private void updateMonitoringStatus(String message) {
        Platform.runLater(() -> {
            if (monitoringStatusLabel != null) {
                monitoringStatusLabel.setText(message);
            }

            // 상태바에도 표시
            updateStatusLabel(message);
        });
    }

    /**
     * 모니터링 UI 상태 업데이트
     */
    private void updateMonitoringUI() {
        Platform.runLater(() -> {
            if (monitoringToggleButton != null) {
                if (isMonitoringActive) {
                    monitoringToggleButton.setText("🛑 모니터링 중지");
                    monitoringToggleButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                } else {
                    monitoringToggleButton.setText("▶️ 모니터링 시작");
                    monitoringToggleButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
                }
            }

            if (monitoringFolderLabel != null) {
                String folder = fileWatcherService.getWatchedDirectory();
                if (folder != null) {
                    monitoringFolderLabel.setText("📁 " + folder);
                } else {
                    monitoringFolderLabel.setText("📁 모니터링 중인 폴더 없음");
                }
            }
        });
    }

    /**
     * 일시적 메시지 표시 (3초 후 사라짐)
     */
    private void showTemporaryMessage(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

            // 3초 후 원래 상태로 복원
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                statusLabel.setStyle("-fx-text-fill: #333;");
                updateStatistics(); // 통계로 다시 표시
            }));
            timeline.play();
        }
    }

    /**
     * 모니터링 설정 변경 핸들러 (설정 창에서 호출)
     */
    public void updateMonitoringConfig(AppConfig newConfig) {
        if (fileWatcherService != null) {
            fileWatcherService.updateConfig(newConfig);

            // 모니터링 상태가 설정과 다르면 동기화
            if (newConfig.isRealTimeMonitoring() && !isMonitoringActive) {
                startMonitoring();
            } else if (!newConfig.isRealTimeMonitoring() && isMonitoringActive) {
                stopMonitoring();
            }
        }
    }

    @FXML
    private void handleExit() {
        // 모니터링 서비스 종료
        if (fileWatcherService != null) {
            fileWatcherService.shutdown();
        }

        Platform.exit();
        System.exit(0);
    }

    // ===================
    // 비즈니스 로직 메서드들
    // ===================

    /**
     * 파일 스캔 시작
     */
    private void startFileScan(File directory) {
        // 기존 선택 해제 및 상세 정보 숨기기
        fileTable.getSelectionModel().clearSelection();
        fileDetailManager.hideDetails();

        // 정리 버튼 비활성화
        organizeButton.setDisable(true);

        // 파일 스캔 시작
        fileScanService.startFileScan(directory);

        // 리스트 변경 리스너 (정리 버튼 활성화용)
        fileList.addListener((ListChangeListener<FileInfo>) change -> {
            boolean hasProcessableFiles = fileList.stream().anyMatch(file -> file.getStatus().isProcessable());
            organizeButton.setDisable(!hasProcessableFiles);
        });
    }

    /**
     * 정리 확인 다이얼로그 표시
     */
    private boolean showOrganizeConfirmDialog(List<FileInfo> filesToOrganize) {
        long totalFiles = fileList.size();
        long totalSize = filesToOrganize.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        java.util.Map<String, Long> categoryCount = filesToOrganize.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        StringBuilder message = new StringBuilder();
        message.append("📦 ").append(filesToOrganize.size()).append("개 파일을 정리할 준비가 되었습니다 (전체 ").append(totalFiles).append("개 중).\n");
        message.append("📏 총 크기: ").append(formattedSize).append("\n\n");
        message.append("📂 카테고리별 파일:\n");

        categoryCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> message.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("개 파일\n"));

        message.append("\n🗂️ 정리된 파일들이 다음 폴더 구조로 이동됩니다:\n");
        message.append("  📁 Documents/\n");
        message.append("  🖼️ Images/\n");
        message.append("  🎥 Videos/\n");
        message.append("  🎵 Audio/\n");
        message.append("  📦 Archives/\n");
        message.append("  📄 Others/\n\n");
        message.append("❓ 계속하시겠습니까?");

        return UIFactory.showConfirmDialog("📦 파일 정리", message.toString());
    }

    /**
     * 파일 정리 시작
     */
    private void startFileOrganization(List<FileInfo> filesToOrganize) {
        String targetRootPath = selectOrganizationFolder();

        if (targetRootPath == null) {
            System.out.println("[INFO] 사용자가 정리 폴더 선택을 취소함");
            return;
        }

        System.out.println("[INFO] 정리 대상 폴더: " + targetRootPath);

        javafx.concurrent.Task<Integer> organizeTask = fileOrganizerService.organizeFilesAsync(filesToOrganize, targetRootPath);

        organizeTask.setOnSucceeded(e -> {
            Integer successCount = organizeTask.getValue();

            // 테이블 새로고침
            fileTable.refresh();

            String resultMessage = String.format(
                    "🎉 파일 정리가 완료되었습니다!\n\n" +
                            "✅ 성공: %d개 파일\n" +
                            "❌ 실패: %d개 파일\n\n" +
                            "📁 정리된 파일들을 %s 폴더에서 확인할 수 있습니다.",
                    successCount,
                    filesToOrganize.size() - successCount,
                    targetRootPath
            );

            showInfoDialog("🎉 정리 완료", resultMessage);
        });

        organizeTask.setOnFailed(e -> {
            Throwable exception = organizeTask.getException();
            showInfoDialog("❌ 정리 실패",
                    "파일 정리 중 오류가 발생했습니다:\n" + exception.getMessage());
        });

        Thread organizeThread = new Thread(organizeTask);
        organizeThread.setDaemon(true);
        organizeThread.start();
    }

    /**
     * 정리 폴더 선택
     */
    private String selectOrganizationFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("📦 정리된 파일을 저장할 폴더 선택");

        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Desktop");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        File selectedDirectory = directoryChooser.showDialog(getCurrentStage());

        if (selectedDirectory != null) {
            return selectedDirectory.getAbsolutePath() + File.separator + "SmartFileManager_Organized";
        }

        return null;
    }

    /**
     * 되돌리기 확인 다이얼로그
     */
    private boolean showUndoConfirmDialog(List<FileInfo> undoableFiles) {
        long totalSize = undoableFiles.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        StringBuilder message = new StringBuilder();
        message.append("↩️ ").append(undoableFiles.size()).append("개의 정리된 파일을 원래 위치로 되돌리시겠습니까?\n");
        message.append("📏 총 크기: ").append(formattedSize).append("\n\n");

        message.append("📋 되돌릴 파일들:\n");
        undoableFiles.stream()
                .limit(5)
                .forEach(file -> message.append("  • ").append(file.getFileName()).append("\n"));

        if (undoableFiles.size() > 5) {
            message.append("  ... 그 외 ").append(undoableFiles.size() - 5).append("개\n");
        }

        message.append("\n⚠️ 주의: 원래 위치에 같은 이름의 파일이 있으면 백업됩니다.\n");
        message.append("❓ 계속하시겠습니까?");

        return UIFactory.showConfirmDialog("↩️ 파일 되돌리기", message.toString());
    }

    /**
     * 되돌리기 프로세스 시작
     */
    private void startUndoProcess(List<FileInfo> undoableFiles) {
        javafx.concurrent.Task<Integer> undoTask = undoService.undoOrganizationAsync(undoableFiles);

        undoTask.setOnSucceeded(e -> {
            Integer successCount = undoTask.getValue();

            // 테이블 새로고침
            fileTable.refresh();

            String resultMessage = String.format(
                    "🎉 파일 되돌리기가 완료되었습니다!\n\n" +
                            "✅ 성공: %d개 파일\n" +
                            "❌ 실패: %d개 파일\n\n" +
                            "📁 파일들이 원래 위치로 되돌려졌습니다.\n" +
                            "🔄 이제 다시 스캔하고 정리할 수 있습니다.",
                    successCount,
                    undoableFiles.size() - successCount
            );

            showInfoDialog("🎉 되돌리기 완료", resultMessage);
        });

        undoTask.setOnFailed(e -> {
            Throwable exception = undoTask.getException();
            showInfoDialog("❌ 되돌리기 실패",
                    "파일 되돌리기 중 오류가 발생했습니다:\n" + exception.getMessage());
        });

        Thread undoThread = new Thread(undoTask);
        undoThread.setDaemon(true);
        undoThread.start();
    }

    /**
     * 중복 파일 탐지 시작
     */
    private void startDuplicateDetection() {
        statusLabel.setText("🔍 중복 파일을 분석하고 있습니다...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // 불확정 진행률

        Task<List<com.smartfilemanager.model.DuplicateGroup>> duplicateTask =
                new Task<List<com.smartfilemanager.model.DuplicateGroup>>() {
                    @Override
                    protected List<com.smartfilemanager.model.DuplicateGroup> call() throws Exception {
                        List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                        return duplicateDetectorService.findDuplicates(filesToAnalyze);
                    }

                    @Override
                    protected void succeeded() {
                        List<com.smartfilemanager.model.DuplicateGroup> duplicateGroups = getValue();

                        progressBar.setVisible(false);
                        statusLabel.setText("🔍 중복 파일 분석 완료");

                        showDuplicateResults(duplicateGroups);
                    }

                    @Override
                    protected void failed() {
                        progressBar.setVisible(false);
                        statusLabel.setText("❌ 중복 파일 분석 실패");

                        Throwable exception = getException();
                        showInfoDialog("❌ 분석 실패",
                                "중복 파일 분석 중 오류가 발생했습니다:\n" + exception.getMessage());
                    }
                };

        Thread duplicateThread = new Thread(duplicateTask);
        duplicateThread.setDaemon(true);
        duplicateThread.start();
    }

    /**
     * 중복 파일 결과 표시
     */
    private void showDuplicateResults(List<com.smartfilemanager.model.DuplicateGroup> duplicateGroups) {
        if (duplicateGroups.isEmpty()) {
            showInfoDialog("🎉 중복 파일 없음",
                    "🔍 분석 결과 중복된 파일을 찾지 못했습니다.\n\n" +
                            "✅ 모든 파일이 고유한 파일입니다!\n" +
                            "📊 분석된 파일: " + fileList.size() + "개");
            return;
        }

        // 통계 계산 및 결과 표시
        long exactDuplicates = duplicateGroups.stream()
                .filter(g -> g.getType() == com.smartfilemanager.model.DuplicateType.EXACT)
                .count();
        long similarFiles = duplicateGroups.stream()
                .filter(g -> g.getType() == com.smartfilemanager.model.DuplicateType.SIMILAR)
                .count();

        long totalDuplicateFiles = duplicateGroups.stream()
                .mapToLong(g -> g.getFiles().size())
                .sum();

        long totalSavings = duplicateGroups.stream()
                .mapToLong(com.smartfilemanager.model.DuplicateGroup::getDuplicateSize)
                .sum();

        StringBuilder message = new StringBuilder();
        message.append("🔄 중복 파일 분석 결과\n\n");
        message.append("📊 발견된 중복 그룹: ").append(duplicateGroups.size()).append("개\n");
        message.append("  • 🎯 정확한 중복: ").append(exactDuplicates).append("개 그룹\n");
        message.append("  • 🔍 유사한 파일: ").append(similarFiles).append("개 그룹\n\n");
        message.append("📁 중복 파일 개수: ").append(totalDuplicateFiles).append("개\n");
        message.append("💾 절약 가능 용량: ").append(formatFileSize(totalSavings)).append("\n\n");
        message.append("🚀 향후 버전에서는 중복 파일 관리 UI가 제공될 예정입니다!");

        showInfoDialog("🔄 중복 파일 발견!", message.toString());
    }

    /**
     * 불필요한 파일 정리 탐지 시작
     */
    private void startCleanupDetection() {
        statusLabel.setText("🧹 불필요한 파일을 분석하고 있습니다...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);

        Task<List<com.smartfilemanager.model.CleanupCandidate>> cleanupTask =
                new Task<List<com.smartfilemanager.model.CleanupCandidate>>() {
                    @Override
                    protected List<com.smartfilemanager.model.CleanupCandidate> call() throws Exception {
                        List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                        return cleanupDetectorService.findCleanupCandidates(filesToAnalyze);
                    }

                    @Override
                    protected void succeeded() {
                        List<com.smartfilemanager.model.CleanupCandidate> cleanupCandidates = getValue();

                        progressBar.setVisible(false);
                        statusLabel.setText("🧹 불필요한 파일 분석 완료");

                        showCleanupResults(cleanupCandidates);
                    }

                    @Override
                    protected void failed() {
                        progressBar.setVisible(false);
                        statusLabel.setText("❌ 불필요한 파일 분석 실패");

                        Throwable exception = getException();
                        showInfoDialog("❌ 분석 실패",
                                "불필요한 파일 분석 중 오류가 발생했습니다:\n" + exception.getMessage());
                    }
                };

        Thread cleanupThread = new Thread(cleanupTask);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * 불필요한 파일 정리 결과 표시
     */
    private void showCleanupResults(List<com.smartfilemanager.model.CleanupCandidate> candidates) {
        if (candidates.isEmpty()) {
            showInfoDialog("🎉 불필요한 파일 없음",
                    "🧹 분석 결과 정리할 불필요한 파일을 찾지 못했습니다.\n\n" +
                            "✅ 시스템이 이미 깔끔한 상태입니다!\n" +
                            "📊 분석된 파일: " + fileList.size() + "개");
            return;
        }

        long totalFiles = candidates.size();
        long totalSize = candidates.stream()
                .mapToLong(com.smartfilemanager.model.CleanupCandidate::getFileSize)
                .sum();

        StringBuilder message = new StringBuilder();
        message.append("🧹 불필요한 파일 분석 결과\n\n");
        message.append("📊 발견된 정리 후보: ").append(totalFiles).append("개 파일\n");
        message.append("💾 총 절약 가능 용량: ").append(formatFileSize(totalSize)).append("\n\n");
        message.append("🚀 향후 버전에서는 실제 파일 삭제 기능이 제공될 예정입니다!");

        showInfoDialog("🧹 불필요한 파일 발견!", message.toString());
    }

    // =================
    // 헬퍼 메서드들
    // =================

    private String getStatusIcon(ProcessingStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case SCANNING: return "🔍";
            case ANALYZED: return "✅";
            case ORGANIZING: return "📦";
            case ORGANIZED: return "🎯";
            case FAILED: return "❌";
            case SKIPPED: return "⏭️";
            default: return "❓";
        }
    }

    private String getFileIcon(String category) {
        if (category == null) return "📄";
        switch (category.toLowerCase()) {
            case "images": return "🖼️";
            case "documents": return "📄";
            case "videos": return "🎥";
            case "audio": return "🎵";
            case "archives": return "📦";
            case "applications": return "⚙️";
            case "code": return "💻";
            default: return "📄";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private Stage getCurrentStage() {
        return (Stage) fileTable.getScene().getWindow();
    }

    /**
     * AI 분석 초기화 (완성된 버전)
     */
    private void initializeAIAnalysis() {
        AppConfig config = configService.getCurrentConfig();

        if (config.isEnableAIAnalysis() && config.getAiApiKey() != null) {
            // AI 분석이 활성화된 경우
            fileAnalysisService.refreshConfig(); // 설정 새로고침

            boolean aiAvailable = fileAnalysisService.isAIAnalysisAvailable();
            if (aiAvailable) {
                updateStatusLabel("AI 분석이 활성화되었습니다 🤖");

                // AI 상태를 UI에 표시 (있다면)
                if (aiStatusIndicator != null) {
                    aiStatusIndicator.setText("AI 활성");
                    aiStatusIndicator.getStyleClass().removeAll("status-inactive", "status-error");
                    aiStatusIndicator.getStyleClass().add("status-active");
                }

                System.out.println("[AI] ✅ AI 분석 시스템 준비 완료");
            } else {
                updateStatusLabel("AI 분석 설정에 문제가 있습니다");
                showAIConfigurationAlert();
            }
        } else {
            // AI 분석이 비활성화된 경우
            if (aiStatusIndicator != null) {
                aiStatusIndicator.setText("AI 비활성");
                aiStatusIndicator.getStyleClass().removeAll("status-active", "status-error");
                aiStatusIndicator.getStyleClass().add("status-inactive");
            }
            System.out.println("[INFO] AI 분석이 비활성화되어 있습니다");
        }

        // AI 관련 메뉴/버튼 상태 업데이트
        updateAIMenusAndButtons();
    }

    /**
     * AI 설정 문제 알림 (완성된 버전)
     */
    private void showAIConfigurationAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("AI 분석 설정 확인");
        alert.setHeaderText("AI 분석 기능에 문제가 있습니다");
        alert.setContentText("AI 분석이 활성화되어 있지만 API 키가 유효하지 않습니다.\n설정에서 API 키를 확인해주세요.");

        ButtonType settingsButton = new ButtonType("설정 열기");
        ButtonType cancelButton = new ButtonType("나중에", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(settingsButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == settingsButton) {
            handleSettings();
        }
    }

    /**
     * 파일 스캔 시 AI 분석 상태 표시 (완성된 버전)
     */
    private void updateScanProgressWithAI(int current, int total, String currentFile, boolean aiEnabled) {
        double progress = (double) current / total;
        progressBar.setProgress(progress);

        String statusText = String.format("스캔 중... %d/%d", current, total);
        if (aiEnabled) {
            statusText += " (AI 분석 포함) 🤖";
        }

        updateStatusLabel(statusText);

        // 현재 처리 중인 파일명 표시
        if (currentFileLabel != null && currentFile != null) {
            String fileName = Paths.get(currentFile).getFileName().toString();
            if (fileName.length() > 50) {
                fileName = fileName.substring(0, 47) + "...";
            }
            currentFileLabel.setText("처리 중: " + fileName);
        }
    }

    /**
     * AI 분석 결과가 포함된 파일 테이블 업데이트 (완성된 버전)
     */
    private void updateFileTableWithAI() {
        // 신뢰도 컬럼에 AI 분석 여부 표시
        if (confidenceColumn != null) {
            confidenceColumn.setCellFactory(column -> new TableCell<FileInfo, Double>() {
                @Override
                protected void updateItem(Double confidence, boolean empty) {
                    super.updateItem(confidence, empty);

                    if (empty || confidence == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        // 신뢰도 백분율로 표시
                        String confidenceText = String.format("%.0f%%", confidence * 100);

                        // AI 분석이 적용된 파일인지 확인
                        FileInfo fileInfo = getTableView().getItems().get(getIndex());
                        boolean hasAIAnalysis = isAIAnalyzed(fileInfo);

                        if (hasAIAnalysis) {
                            confidenceText += " 🤖";
                        }

                        setText(confidenceText);

                        // 신뢰도에 따른 색상 표시
                        if (confidence >= 0.8) {
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;"); // 녹색
                        } else if (confidence >= 0.6) {
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;"); // 주황색
                        } else {
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;"); // 빨간색
                        }
                    }
                }
            });
        }
    }

    /**
     * 파일이 AI 분석되었는지 확인 (완성된 버전)
     */
    private boolean isAIAnalyzed(FileInfo fileInfo) {
        // AI 분석된 파일은 보통 키워드에 "ai-analyzed" 마커가 있음
        if (fileInfo.getKeywords() != null && fileInfo.getKeywords().contains("ai-analyzed")) {
            return true;
        }

        // 설명에 "AI 분석:" 이 포함된 경우
        if (fileInfo.getDescription() != null &&
                fileInfo.getDescription().contains("AI 분석:")) {
            return true;
        }

        // 키워드 수가 많은 경우 (AI가 추가했을 가능성)
        if (fileInfo.getKeywords() != null && fileInfo.getKeywords().size() > 8) {
            return true;
        }

        // 신뢰도가 매우 높은 경우 (AI 분석 결과일 가능성)
        return fileInfo.getConfidenceScore() > 0.9;
    }

    /**
     * AI 분석 통계 표시 (완성된 버전)
     */
    private void updateStatisticsWithAI() {
        long totalFiles = fileList.size();
        long aiAnalyzedFiles = fileList.stream()
                .mapToLong(file -> isAIAnalyzed(file) ? 1 : 0)
                .sum();

        long organizedFiles = fileList.stream()
                .mapToLong(file -> file.getStatus() == ProcessingStatus.ORGANIZED ? 1 : 0)
                .sum();

        long totalSize = fileList.stream()
                .mapToLong(FileInfo::getFileSize)
                .sum();

        // AI 분석 비율 계산
        double aiRatio = totalFiles > 0 ? (double) aiAnalyzedFiles / totalFiles * 100 : 0;

        String statsText;
        if (aiAnalyzedFiles > 0) {
            statsText = String.format(
                    "총 %d개 파일 | %d개 정리됨 | 🤖 AI 분석: %d개 (%.0f%%) | 총 크기: %s",
                    totalFiles, organizedFiles, aiAnalyzedFiles, aiRatio, formatFileSize(totalSize)
            );
        } else {
            statsText = String.format(
                    "총 %d개 파일 | %d개 정리됨 | 총 크기: %s",
                    totalFiles, organizedFiles, formatFileSize(totalSize)
            );
        }

        statisticsLabel.setText(statsText);
    }

    /**
     * AI 분석 배치 실행 (완성된 버전)
     */
    @FXML
    private void handleBatchAIAnalysis() {
        List<FileInfo> unanalyzedFiles = fileList.stream()
                .filter(file -> !isAIAnalyzed(file))
                .filter(file -> file.getStatus() != ProcessingStatus.FAILED)
                .collect(Collectors.toList());

        if (unanalyzedFiles.isEmpty()) {
            showAlert("알림", "AI 분석이 필요한 파일이 없습니다.\n모든 파일이 이미 분석되었거나 AI 분석에 적합하지 않습니다.", Alert.AlertType.INFORMATION);
            return;
        }

        // AI 분석 가능 여부 확인
        if (!fileAnalysisService.isAIAnalysisAvailable()) {
            showAlert("오류", "AI 분석이 활성화되지 않았습니다.\n설정에서 AI 분석을 활성화하고 유효한 API 키를 입력해주세요.", Alert.AlertType.ERROR);
            return;
        }

        // 비용 계산 및 확인 다이얼로그
        double estimatedCost = unanalyzedFiles.size() * 0.005; // 파일당 약 0.005원

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("AI 배치 분석");
        confirmAlert.setHeaderText(unanalyzedFiles.size() + "개 파일을 AI로 분석하시겠습니까?");

        String contentText = String.format(
                "🤖 AI 분석 대상: %d개 파일\n" +
                        "💰 예상 비용: 약 %.3f원 (파일당 0.005원)\n" +
                        "⏱️ 예상 시간: %d~%d분\n\n" +
                        "⚠️ 이 작업은 OpenAI API를 사용하며 실제 비용이 발생할 수 있습니다.\n" +
                        "계속하시겠습니까?",
                unanalyzedFiles.size(),
                estimatedCost,
                unanalyzedFiles.size() / 30, // 30개/분 가정
                unanalyzedFiles.size() / 20  // 20개/분 가정
        );

        confirmAlert.setContentText(contentText);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runBatchAIAnalysisAsync(unanalyzedFiles);
        }
    }

    /**
     * 비동기 AI 배치 분석 실행 (완성된 버전)
     */
    private void runBatchAIAnalysisAsync(List<FileInfo> files) {
        // 버튼 비활성화
        if (aiAnalysisButton != null) {
            aiAnalysisButton.setDisable(true);
            aiAnalysisButton.setText("AI 분석 중...");
        }

        Task<Integer> batchAnalysisTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int processed = 0;
                int successful = 0;

                updateMessage("AI 배치 분석을 시작합니다...");

                for (FileInfo file : files) {
                    try {
                        // 개별 파일 재분석 (AI 포함)
                        FileInfo reanalyzed = fileAnalysisService.analyzeFile(file.getFilePath());

                        if (reanalyzed != null && reanalyzed.getStatus() != ProcessingStatus.FAILED) {
                            // 분석 결과를 기존 FileInfo에 적용
                            updateFileInfoFromReanalysis(file, reanalyzed);
                            successful++;
                        }

                        processed++;

                        // 진행률 업데이트
                        final int currentProgress = processed;
                        final int successCount = successful;

                        Platform.runLater(() -> {
                            double progress = (double) currentProgress / files.size();
                            progressBar.setProgress(progress);

                            String statusText = String.format("AI 분석 중... %d/%d (성공: %d개) 🤖",
                                    currentProgress, files.size(), successCount);
                            updateStatusLabel(statusText);

                            // 현재 파일명 표시
                            if (currentFileLabel != null) {
                                currentFileLabel.setText("분석 중: " + file.getFileName());
                            }

                            // 테이블 갱신
                            fileTable.refresh();
                        });

                        // API 호출 제한을 위한 지연 (TPM 제한 고려)
                        Thread.sleep(300); // 0.3초 지연으로 안전하게

                    } catch (InterruptedException e) {
                        System.out.println("[INFO] AI 배치 분석이 중단되었습니다");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[ERROR] AI 배치 분석 중 오류: " + file.getFileName() + " - " + e.getMessage());
                    }
                }

                return successful;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    Integer successCount = getValue();

                    updateStatusLabel("AI 배치 분석 완료! 🎉");
                    progressBar.setProgress(1.0);

                    if (currentFileLabel != null) {
                        currentFileLabel.setText("");
                    }

                    updateStatisticsWithAI();
                    fileTable.refresh();

                    // 결과 알림
                    String resultMessage = String.format(
                            "🎉 AI 배치 분석이 완료되었습니다!\n\n" +
                                    "✅ 성공적으로 분석된 파일: %d개\n" +
                                    "❌ 분석 실패한 파일: %d개\n\n" +
                                    "🤖 AI 분석을 통해 파일 분류 정확도가 향상되었습니다.",
                            successCount, files.size() - successCount
                    );

                    showAlert("완료", resultMessage, Alert.AlertType.INFORMATION);

                    // 버튼 상태 복원
                    if (aiAnalysisButton != null) {
                        aiAnalysisButton.setDisable(false);
                        aiAnalysisButton.setText("AI 배치 분석");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatusLabel("AI 배치 분석 실패");
                    progressBar.setProgress(0);

                    if (currentFileLabel != null) {
                        currentFileLabel.setText("");
                    }

                    Throwable exception = getException();
                    String errorMessage = "AI 배치 분석 중 오류가 발생했습니다:\n" +
                            (exception != null ? exception.getMessage() : "알 수 없는 오류");

                    showAlert("오류", errorMessage, Alert.AlertType.ERROR);

                    // 버튼 상태 복원
                    if (aiAnalysisButton != null) {
                        aiAnalysisButton.setDisable(false);
                        aiAnalysisButton.setText("AI 배치 분석");
                    }
                });
            }
        };

        Thread batchThread = new Thread(batchAnalysisTask);
        batchThread.setDaemon(true);
        batchThread.start();
    }

    /**
     * 재분석 결과를 기존 FileInfo에 적용 (완성된 버전)
     */
    private void updateFileInfoFromReanalysis(FileInfo original, FileInfo reanalyzed) {
        // AI 분석으로 향상된 정보들을 기존 객체에 적용
        if (reanalyzed.getDetectedCategory() != null) {
            original.setDetectedCategory(reanalyzed.getDetectedCategory());
        }

        if (reanalyzed.getDetectedSubCategory() != null) {
            original.setDetectedSubCategory(reanalyzed.getDetectedSubCategory());
        }

        if (reanalyzed.getConfidenceScore() > original.getConfidenceScore()) {
            original.setConfidenceScore(reanalyzed.getConfidenceScore());
        }

        if (reanalyzed.getDescription() != null) {
            // 기존 설명과 AI 분석 결과 결합
            String currentDesc = original.getDescription();
            if (currentDesc != null) {
                original.setDescription(currentDesc + "\n\n" + reanalyzed.getDescription());
            } else {
                original.setDescription(reanalyzed.getDescription());
            }
        }

        if (reanalyzed.getKeywords() != null && !reanalyzed.getKeywords().isEmpty()) {
            List<String> originalKeywords = original.getKeywords();
            if (originalKeywords == null) {
                originalKeywords = new ArrayList<>();
                original.setKeywords(originalKeywords);
            }

            // 새로운 키워드들 추가 (중복 제거)
            for (String keyword : reanalyzed.getKeywords()) {
                if (!originalKeywords.contains(keyword)) {
                    originalKeywords.add(keyword);
                }
            }
        }

        if (reanalyzed.getSuggestedPath() != null) {
            original.setSuggestedPath(reanalyzed.getSuggestedPath());
        }

        original.setStatus(ProcessingStatus.ANALYZED);
        original.setProcessedAt(LocalDateTime.now());
    }

    /**
     * AI 분석 설정 상태에 따른 메뉴/버튼 활성화 (완성된 버전)
     */
    private void updateAIMenusAndButtons() {
        boolean aiAvailable = fileAnalysisService.isAIAnalysisAvailable();

        // AI 관련 메뉴 항목들 활성화/비활성화
        if (batchAIAnalysisMenuItem != null) {
            batchAIAnalysisMenuItem.setDisable(!aiAvailable);
        }

        if (aiAnalysisButton != null) {
            aiAnalysisButton.setDisable(!aiAvailable);
            aiAnalysisButton.setText(aiAvailable ? "AI 배치 분석" : "AI 비활성");
        }

        // 툴팁 업데이트
        if (aiAnalysisButton != null) {
            String tooltipText = aiAvailable ?
                    "선택된 파일들을 AI로 재분석합니다\n(OpenAI API 사용, 비용 발생 가능)" :
                    "AI 분석을 사용하려면 설정에서 활성화해주세요";
            aiAnalysisButton.setTooltip(new Tooltip(tooltipText));
        }
    }

    /**
     * 설정이 변경된 후 AI 상태 새로고침 (완성된 버전)
     */
    public void refreshAIConfiguration() {
        System.out.println("[AI] AI 설정 새로고침 시작");

        initializeAIAnalysis();
        updateAIMenusAndButtons();
        updateFileTableWithAI();
        updateStatisticsWithAI();

        System.out.println("[AI] AI 설정 새로고침 완료");
    }

    /**
     * AI 관련 컨텍스트 메뉴 추가 (완성된 버전)
     */
    private void setupAIContextMenu() {
        if (fileTable != null) {
            ContextMenu contextMenu = new ContextMenu();

            // 기존 컨텍스트 메뉴 항목들...

            // AI 분석 메뉴 항목
            MenuItem aiAnalyzeItem = new MenuItem("🤖 AI로 재분석");
            aiAnalyzeItem.setOnAction(e -> {
                FileInfo selectedFile = fileTable.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    analyzeSelectedFileWithAI(selectedFile);
                }
            });

            // AI 분석 상태에 따라 활성화/비활성화
            aiAnalyzeItem.setDisable(!fileAnalysisService.isAIAnalysisAvailable());

            contextMenu.getItems().add(aiAnalyzeItem);
            fileTable.setContextMenu(contextMenu);
        }
    }

    /**
     * 선택된 파일을 AI로 재분석 (완성된 버전)
     */
    private void analyzeSelectedFileWithAI(FileInfo selectedFile) {
        if (!fileAnalysisService.isAIAnalysisAvailable()) {
            showAlert("오류", "AI 분석이 활성화되지 않았습니다.", Alert.AlertType.ERROR);
            return;
        }

        // 확인 다이얼로그
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("AI 재분석");
        confirmAlert.setHeaderText("선택된 파일을 AI로 재분석하시겠습니까?");
        confirmAlert.setContentText("파일: " + selectedFile.getFileName() + "\n예상 비용: 약 0.005원");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            Task<FileInfo> analyzeTask = new Task<FileInfo>() {
                @Override
                protected FileInfo call() throws Exception {
                    updateMessage("AI 분석 중...");
                    return fileAnalysisService.analyzeFile(selectedFile.getFilePath());
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        FileInfo reanalyzed = getValue();
                        if (reanalyzed != null && reanalyzed.getStatus() != ProcessingStatus.FAILED) {
                            updateFileInfoFromReanalysis(selectedFile, reanalyzed);
                            fileTable.refresh();
                            updateStatisticsWithAI();

                            showAlert("완료", "AI 재분석이 완료되었습니다.\n신뢰도: " +
                                            String.format("%.0f%%", selectedFile.getConfidenceScore() * 100),
                                    Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("실패", "AI 재분석에 실패했습니다.", Alert.AlertType.ERROR);
                        }
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        showAlert("오류", "AI 재분석 중 오류가 발생했습니다:\n" +
                                getException().getMessage(), Alert.AlertType.ERROR);
                    });
                }
            };

            Thread analyzeThread = new Thread(analyzeTask);
            analyzeThread.setDaemon(true);
            analyzeThread.start();
        }
    }

    /**
     * AI 분석 비용 계산기 (완성된 버전)
     */
    private double calculateAICost(int fileCount) {
        // 파일당 평균 비용 (추정치)
        double costPerFile = 0.005; // 0.005원
        return fileCount * costPerFile;
    }

    /**
     * AI 분석 결과 요약 표시 (완성된 버전)
     */
    private void showAIAnalysisSummary() {
        long totalFiles = fileList.size();
        long aiAnalyzedFiles = fileList.stream()
                .filter(this::isAIAnalyzed)
                .count();

        if (aiAnalyzedFiles == 0) {
            showAlert("정보", "AI 분석된 파일이 없습니다.", Alert.AlertType.INFORMATION);
            return;
        }

        // 카테고리별 분석 결과
        Map<String, Long> categoryStats = fileList.stream()
                .filter(this::isAIAnalyzed)
                .collect(Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        Collectors.counting()
                ));

        // 평균 신뢰도 계산
        double avgConfidence = fileList.stream()
                .filter(this::isAIAnalyzed)
                .mapToDouble(FileInfo::getConfidenceScore)
                .average()
                .orElse(0.0);

        StringBuilder summary = new StringBuilder();
        summary.append("🤖 AI 분석 결과 요약\n\n");
        summary.append("📊 분석된 파일: ").append(aiAnalyzedFiles).append("개 / ").append(totalFiles).append("개 총\n");
        summary.append("📈 평균 신뢰도: ").append(String.format("%.1f%%", avgConfidence * 100)).append("\n\n");
        summary.append("📂 카테고리별 분포:\n");

        categoryStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> summary.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("개\n"));

        double estimatedCost = calculateAICost((int) aiAnalyzedFiles);
        summary.append("\n💰 예상 사용 비용: 약 ").append(String.format("%.3f", estimatedCost)).append("원");

        Alert summaryAlert = new Alert(Alert.AlertType.INFORMATION);
        summaryAlert.setTitle("AI 분석 요약");
        summaryAlert.setHeaderText("AI 분석 결과 요약");
        summaryAlert.setContentText(summary.toString());
        summaryAlert.getDialogPane().setPrefWidth(400);
        summaryAlert.showAndWait();
    }

    /**
     * AI 분석 메뉴 항목 핸들러
     */
    @FXML
    private void handleShowAISummary() {
        showAIAnalysisSummary();
    }

    /**
     * AI 상태 표시 업데이트
     */
    private void updateAIStatusDisplay() {
        if (fileAnalysisService.isAIAnalysisAvailable()) {
            // 상태바나 라벨에 AI 상태 표시
            if (aiStatusIndicator != null) {
                aiStatusIndicator.setText("🤖 AI 활성");
                aiStatusIndicator.setStyle("-fx-text-fill: #2e7d32;");
            }
        } else {
            if (aiStatusIndicator != null) {
                aiStatusIndicator.setText("AI 비활성");
                aiStatusIndicator.setStyle("-fx-text-fill: #757575;");
            }
        }
    }
}