package com.smartfilemanager.controller;

import com.smartfilemanager.manager.*;
import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.service.*;
import com.smartfilemanager.ui.AboutDialog;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.HelpDialog;
import com.smartfilemanager.ui.OrganizePreviewDialog;
import com.smartfilemanager.ui.PreviewDialog;
import com.smartfilemanager.ui.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 메인 화면 컨트롤러 - 리팩토링된 버전
 * 각 기능을 전담하는 매니저 클래스들을 사용하여 코드 복잡도를 줄였습니다.
 */
public class MainController implements Initializable {

    // FXML 컴포넌트들
    @FXML private TableView<FileInfo> fileTable;
    @FXML private TableColumn<FileInfo, String> nameColumn, categoryColumn, sizeColumn, statusColumn, dateColumn;
    @FXML private TableColumn<FileInfo, Double> confidenceColumn;
    @FXML private TableColumn<FileInfo, Boolean> selectColumn;
    @FXML private Button scanButton, organizeButton, settingsButton, aiAnalysisButton, monitoringToggleButton;
    @FXML private Button selectAllButton, deselectAllButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel, progressLabel, statsLabel, statisticsLabel, detailContent;
    @FXML private Label aiStatusIndicator, currentFileLabel, monitoringStatusLabel, monitoringFolderLabel;
    @FXML private Label selectedCountLabel;
    @FXML private VBox fileDetailPanel;
    @FXML private HBox monitoringInfoBox;
    @FXML private MenuItem batchAIAnalysisMenuItem, realTimeMonitoringMenuItem;
    @FXML private TitledPane detailTitledPane;

    // 서비스들
    private FileScanService fileScanService;
    private FileOrganizerService fileOrganizerService;
    private UndoService undoService;
    private DuplicateDetectorService duplicateDetectorService;
    private CleanupDetectorService cleanupDetectorService;
    private ConfigService configService;
    private FileAnalysisService fileAnalysisService;
    private FileWatcherService fileWatcherService;
    
    // 매니저들
    private UIUpdateManager uiUpdateManager;
    private TableConfigManager tableConfigManager;
    private FileOperationHandler fileOperationHandler;
    private DialogManager dialogManager;
    private ThemeManager themeManager;
    
    // 데이터
    private ObservableList<FileInfo> fileList;
    private FileDetailManager fileDetailManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[INFO] MainController 초기화 시작");

        fileList = FXCollections.observableArrayList();
        initializeServices();
        initializeManagers();
        setupTable();
        setupKeyboardShortcuts();
        setupListeners();
        updateUI();

        System.out.println("[SUCCESS] MainController 초기화 완료");
    }

    // ===============================
    // 초기화 메서드들
    // ===============================

    private void initializeServices() {
        fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        fileOrganizerService = new FileOrganizerService(progressBar, statusLabel, progressLabel);
        undoService = new UndoService(progressBar, statusLabel, progressLabel);
        duplicateDetectorService = new DuplicateDetectorService();
        cleanupDetectorService = new CleanupDetectorService();
        configService = new ConfigService();
        fileAnalysisService = new FileAnalysisService();
        
        initializeFileWatcher();
        initializeAIAnalysis();
    }

    private void initializeManagers() {
        dialogManager = new DialogManager();
        
        uiUpdateManager = new UIUpdateManager(
            statusLabel, progressLabel, statsLabel, statisticsLabel, aiStatusIndicator, currentFileLabel,
            monitoringStatusLabel, monitoringFolderLabel, organizeButton, monitoringToggleButton, aiAnalysisButton,
            progressBar, monitoringInfoBox, realTimeMonitoringMenuItem, fileAnalysisService, fileList
        );
        
        tableConfigManager = new TableConfigManager(
            fileTable, nameColumn, categoryColumn, sizeColumn, statusColumn, dateColumn, confidenceColumn, selectColumn
        );
        
        fileOperationHandler = new FileOperationHandler(
            fileScanService, fileOrganizerService, undoService, duplicateDetectorService, cleanupDetectorService,
            fileAnalysisService, fileWatcherService, uiUpdateManager, dialogManager, this::getCurrentStage
        );
        
        themeManager = new ThemeManager();
    }

    private void initializeFileWatcher() {
        fileWatcherService = new FileWatcherService();
        fileWatcherService.setStatusUpdateCallback(this::updateMonitoringStatus);
        fileWatcherService.setNewFileCallback(this::handleNewFileDetected);
        fileWatcherService.setFileList(fileList);
    }

    private void initializeAIAnalysis() {
        AppConfig config = configService.getCurrentConfig();
        if (config.isEnableAIAnalysis() && config.getAiApiKey() != null) {
            fileAnalysisService.refreshConfig();
            uiUpdateManager.updateAIStatusIndicator();
        }
    }

    private void setupTable() {
        tableConfigManager.setupTable(fileList);
        tableConfigManager.setSelectionChangeCallback(this::updateSelectedCount);
        fileDetailManager = new FileDetailManager(fileDetailPanel);
        setupTableRowFactory();
    }
    
    private void setupTableRowFactory() {
        fileTable.setRowFactory(tv -> {
            TableRow<FileInfo> row = new TableRow<>();
            
            // 더블클릭 이벤트
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    FileInfo fileInfo = row.getItem();
                    showPreviewDialog(fileInfo);
                }
            });
            
            // 컨텍스트 메뉴 설정 (동적 생성)
            ContextMenu contextMenu = createDynamicContextMenu(row);
            
            // 행이 비어있지 않을 때만 컨텍스트 메뉴 표시
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu)
            );
            
            return row;
        });
    }
    
    /**
     * 파일에 따라 동적으로 컨텍스트 메뉴 생성
     */
    private ContextMenu createDynamicContextMenu(TableRow<FileInfo> row) {
        ContextMenu contextMenu = new ContextMenu();
        
        // 메뉴 아이템들을 동적으로 생성
        MenuItem ruleMenuItem = createRuleMenuItem(row);
        MenuItem openLocationItem = createOpenLocationMenuItem(row);
        
        contextMenu.getItems().addAll(ruleMenuItem, openLocationItem);
        return contextMenu;
    }
    
    /**
     * 규칙 관련 메뉴 아이템 생성 (동적)
     */
    private MenuItem createRuleMenuItem(TableRow<FileInfo> row) {
        FileInfo fileInfo = row.getItem();
        
        if (fileInfo == null || fileInfo.getFileExtension() == null || fileInfo.getFileExtension().trim().isEmpty()) {
            MenuItem defaultItem = new MenuItem("📝 새 규칙 만들기");
            defaultItem.setOnAction(event -> showRuleDialog(fileInfo, null));
            return defaultItem;
        }
        
        // 해당 확장자에 대한 기존 규칙 찾기
        com.smartfilemanager.service.CustomRulesManager rulesManager = new com.smartfilemanager.service.CustomRulesManager();
        java.util.Optional<com.smartfilemanager.model.FileRule> existingRule = rulesManager.findRuleForExtension(fileInfo.getFileExtension());
        
        MenuItem ruleMenuItem;
        if (existingRule.isPresent()) {
            // 기존 규칙이 있는 경우 - 수정 모드
            String extension = fileInfo.getFileExtension().toLowerCase();
            String targetFolder = existingRule.get().getTargetFolder();
            ruleMenuItem = new MenuItem("✏️ " + extension + " 규칙 수정하기 (" + targetFolder + ")");
            ruleMenuItem.setOnAction(event -> showRuleDialog(fileInfo, existingRule.get()));
        } else {
            // 기존 규칙이 없는 경우 - 새 규칙 생성
            String extension = fileInfo.getFileExtension().toLowerCase();
            ruleMenuItem = new MenuItem("📝 " + extension + " 파일 규칙 만들기");
            ruleMenuItem.setOnAction(event -> showRuleDialog(fileInfo, null));
        }
        
        return ruleMenuItem;
    }
    
    /**
     * 파일 위치 열기 메뉴 아이템 생성
     */
    private MenuItem createOpenLocationMenuItem(TableRow<FileInfo> row) {
        MenuItem openLocationItem = new MenuItem("📁 파일 위치 열기");
        openLocationItem.setOnAction(event -> {
            FileInfo selectedFile = row.getItem();
            if (selectedFile != null) {
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(selectedFile.getOriginalLocation()));
                } catch (Exception e) {
                    dialogManager.showErrorDialog("오류", "파일 위치를 열 수 없습니다: " + e.getMessage());
                }
            }
        });
        return openLocationItem;
    }

    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            Stage stage = getCurrentStage();
            if (stage != null && stage.getScene() != null) {
                Scene scene = stage.getScene();
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::handleHelpTopics);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), this::handleSettings);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::handleScanFiles);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F6), this::handleStatistics);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F7), this::handleShowAISummary);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F8), this::handleOrganizeFiles);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::handleOpenFolder);
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::handleUndoOrganization);
            }
        });
    }

    private void setupListeners() {
        fileList.addListener((ListChangeListener<FileInfo>) change -> uiUpdateManager.updateUI());
        tableConfigManager.setSelectionListener((obs, oldSelection, newSelection) -> 
            fileDetailManager.updateFileDetails(newSelection));
    }

    private void updateUI() {
        uiUpdateManager.updateUI();
        
        // 선택된 파일 수 업데이트 (스캔 완료 후에도 호출됨)
        updateSelectedCount();
    }

    // ===============================
    // 선택 관련 메서드들
    // ===============================
    
    @FXML
    private void handleSelectAll() {
        fileList.forEach(file -> file.setSelected(true));
        tableConfigManager.refreshTable();
        updateSelectedCount();
    }
    
    @FXML
    private void handleDeselectAll() {
        fileList.forEach(file -> file.setSelected(false));
        tableConfigManager.refreshTable();
        updateSelectedCount();
    }
    
    private void updateSelectedCount() {
        long selectedCount = fileList.stream().mapToLong(file -> file.isSelected() ? 1 : 0).sum();
        selectedCountLabel.setText("선택된 파일: " + selectedCount + "개");
        
        // 선택된 파일이 있으면 정리 버튼 활성화
        boolean hasSelectedFiles = selectedCount > 0;
        organizeButton.setDisable(!hasSelectedFiles);
        
        System.out.println("선택된 파일 수 업데이트: " + selectedCount + "개");
    }
    
    // ===============================
    // FXML 이벤트 핸들러들
    // ===============================

    @FXML
    private void handleOpenFolder() {
        tableConfigManager.clearSelection();
        fileDetailManager.hideDetails();
        fileOperationHandler.handleOpenFolder(fileList);
    }

    @FXML private void handleScanFiles() { handleOpenFolder(); }

    @FXML
    private void handleOrganizeFiles() {
        // 선택된 파일이 있는지 확인
        List<FileInfo> selectedFiles = fileList.stream()
            .filter(FileInfo::isSelected)
            .collect(java.util.stream.Collectors.toList());
        
        List<FileInfo> filesToOrganize = selectedFiles.isEmpty() ? fileList : selectedFiles;
        
        // 정리할 파일이 없는 경우
        if (filesToOrganize.isEmpty()) {
            dialogManager.showInfoDialog("정리할 파일이 없습니다", "먼저 파일을 스캔해주세요.");
            return;
        }
        
        // 미리보기 다이얼로그 표시
        boolean confirmed = OrganizePreviewDialog.showPreview(getCurrentStage(), filesToOrganize);
        
        if (confirmed) {
            // 사용자가 확인한 경우에만 실제 정리 수행
            fileOperationHandler.handleOrganizeFiles(filesToOrganize);
        }
    }

    @FXML
    private void handleUndoOrganization() {
        fileOperationHandler.handleUndoOrganization(fileList);
    }

    @FXML
    private void handleFindDuplicates() {
        fileOperationHandler.handleFindDuplicates(fileList);
    }

    @FXML
    private void handleCleanupFiles() {
        fileOperationHandler.handleCleanupFiles(fileList);
    }

    @FXML
    private void handleSettings() {
        showSettingsDialog();
    }

    @FXML
    private void handleStatistics() {
        showStatisticsDialog();
    }

    @FXML
    private void handleAbout() {
        AboutDialog.show(getCurrentStage());
    }

    @FXML
    private void handleHelpTopics() {
        HelpDialog.show(getCurrentStage());
    }

    @FXML
    private void handleThemeToggle() {
        themeManager.toggleTheme(getCurrentStage().getScene(), uiUpdateManager);
    }

    @FXML
    private void handleMonitoringToggle() {
        if (fileOperationHandler.isMonitoringActive()) {
            fileOperationHandler.stopMonitoring();
        } else {
            fileOperationHandler.startMonitoring();
        }
    }

    @FXML
    private void handleBatchAIAnalysis() {
        fileOperationHandler.performBatchAIAnalysis(fileList);
    }

    @FXML
    private void handleShowAISummary() {
        long aiAnalyzedCount = fileList.stream()
            .mapToLong(file -> com.smartfilemanager.util.FileIconUtils.isAIAnalyzed(file) ? 1 : 0)
            .sum();
        dialogManager.showAISummaryDialog(aiAnalyzedCount, fileList.size());
    }

    @FXML
    private void handleExit() {
        fileOperationHandler.shutdown();
        Platform.exit();
        System.exit(0);
    }

    // ===============================
    // 다이얼로그 표시 메서드들
    // ===============================
    
    /**
     * 규칙 다이얼로그 표시 (생성/수정 모드 지원)
     * @param selectedFile 선택된 파일 정보
     * @param existingRule 기존 규칙 (수정 모드인 경우), null이면 생성 모드
     */
    private void showRuleDialog(FileInfo selectedFile, com.smartfilemanager.model.FileRule existingRule) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rule-dialog.fxml"));
            Parent ruleDialogRoot = loader.load();

            RuleDialogController ruleDialogController = loader.getController();

            Stage ruleDialogStage = new Stage();
            String dialogTitle = existingRule != null ? "✏️ 파일 정리 규칙 수정" : "📝 파일 정리 규칙 추가";
            ruleDialogStage.setTitle(dialogTitle);
            ruleDialogStage.setScene(new Scene(ruleDialogRoot, 600, 700));
            ruleDialogStage.initModality(Modality.APPLICATION_MODAL);
            ruleDialogStage.initOwner(getCurrentStage());
            ruleDialogStage.setResizable(false);

            // 다이얼로그 컨트롤러 설정
            ruleDialogController.setDialogStage(ruleDialogStage);
            ruleDialogController.setRulesManager(new com.smartfilemanager.service.CustomRulesManager());
            
            if (existingRule != null) {
                // 수정 모드: 기존 규칙 데이터 로드
                ruleDialogController.setEditMode(existingRule);
                System.out.println("[INFO] 규칙 수정 모드: " + existingRule.getName());
            } else {
                // 생성 모드: 선택된 파일의 확장자로 미리 채우기
                ruleDialogController.setAddMode();
                if (selectedFile != null && selectedFile.getFileExtension() != null && !selectedFile.getFileExtension().isEmpty()) {
                    ruleDialogController.setPrefilledExtension(selectedFile.getFileExtension());
                    System.out.println("[INFO] 새 규칙 생성 모드: " + selectedFile.getFileExtension());
                }
            }

            ruleDialogStage.showAndWait();

            // 규칙이 저장되었으면 화면 새로고침
            if (ruleDialogController.isSaveClicked()) {
                String action = existingRule != null ? "수정" : "생성";
                System.out.println("[INFO] 규칙이 " + action + "되었습니다. 화면을 새로고침합니다.");
                // 필요시 파일 리스트 새로고침 등의 작업 수행
            }

        } catch (IOException e) {
            String errorTitle = existingRule != null ? "규칙 수정 오류" : "규칙 생성 오류";
            String errorMessage = (existingRule != null ? "규칙 수정" : "규칙 생성") + " 창을 열 수 없습니다:\n" + e.getMessage();
            dialogManager.showErrorDialog(errorTitle, errorMessage);
        }
    }

    private void showSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent settingsRoot = loader.load();

            SettingsController settingsController = loader.getController();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("⚙️ Smart File Manager - 설정");
            settingsStage.setScene(new Scene(settingsRoot, 800, 600));
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(getCurrentStage());
            settingsStage.setResizable(true);
            settingsStage.setMinWidth(700);
            settingsStage.setMinHeight(500);

            if (settingsController != null) {
                settingsController.setStage(settingsStage);
            }

            settingsStage.showAndWait();

        } catch (IOException e) {
            dialogManager.showErrorDialog("설정 오류", "설정 창을 열 수 없습니다:\n" + e.getMessage());
        }
    }

    private void showStatisticsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics.fxml"));
            Parent statisticsRoot = loader.load();

            Object controller = loader.getController();
            if (controller instanceof StatisticsController) {
                ((StatisticsController) controller).updateFileList(fileList);
            }

            Stage statisticsStage = new Stage();
            statisticsStage.setTitle("📊 파일 정리 통계");
            statisticsStage.setScene(new Scene(statisticsRoot, 1200, 800));
            statisticsStage.initModality(Modality.APPLICATION_MODAL);
            statisticsStage.initOwner(getCurrentStage());
            statisticsStage.show();

        } catch (IOException e) {
            dialogManager.showErrorDialog("통계 오류", "통계 창을 열 수 없습니다: " + e.getMessage());
        }
    }

    // ===============================
    // 콜백 메서드들
    // ===============================

    private void handleNewFileDetected(FileInfo newFile) {
        uiUpdateManager.handleNewFileDetected(newFile);
        tableConfigManager.refreshTable();
    }

    private void updateMonitoringStatus(String message) {
        uiUpdateManager.updateMonitoringStatus(message, fileOperationHandler.isMonitoringActive());
    }

    // ===============================
    // 공개 메서드들
    // ===============================

    public void updateMonitoringConfig(AppConfig newConfig) {
        if (fileWatcherService != null) {
            fileWatcherService.updateConfig(newConfig);
            boolean shouldBeActive = newConfig.isRealTimeMonitoring();
            boolean currentlyActive = fileOperationHandler.isMonitoringActive();
            
            if (shouldBeActive != currentlyActive) {
                if (shouldBeActive) {
                    fileOperationHandler.startMonitoring();
                } else {
                    fileOperationHandler.stopMonitoring();
                }
            }
        }
    }

    public void refreshAIConfiguration() {
        fileAnalysisService.refreshConfig();
        uiUpdateManager.updateAIStatusIndicator();
        tableConfigManager.refreshTable();
        uiUpdateManager.updateUI();
    }

    // ===============================
    // 미리보기 다이얼로그
    // ===============================
    
    private void showPreviewDialog(FileInfo fileInfo) {
        PreviewDialog.showPreview(getCurrentStage(), fileInfo);
    }

    // ===============================
    // 헬퍼 메서드들
    // ===============================

    private Stage getCurrentStage() {
        try {
            if (fileTable != null && fileTable.getScene() != null) {
                return (Stage) fileTable.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("[WARNING] getCurrentStage() failed: " + e.getMessage());
        }
        return null;
    }
}