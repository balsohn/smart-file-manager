package com.smartfilemanager.controller;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.ui.UIFactory;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 설정 화면 컨트롤러
 * 사용자가 애플리케이션 설정을 변경할 수 있도록 합니다
 */
public class SettingsController implements Initializable {

    // FXML UI 컴포넌트들
    @FXML private TabPane settingsTabPane;

    // 기본 설정 탭
    @FXML private TextField defaultScanFolderField;
    @FXML private TextField organizationFolderField;
    @FXML private Button browseScanFolderButton;
    @FXML private Button browseOrganizationFolderButton;

    @FXML private CheckBox autoOrganizeCheckBox;
    @FXML private CheckBox realTimeMonitoringCheckBox;
    @FXML private CheckBox showNotificationsCheckBox;

    @FXML private CheckBox organizeByDateCheckBox;
    @FXML private CheckBox createSubfoldersCheckBox;
    @FXML private CheckBox backupBeforeOrganizingCheckBox;

    // 중복 파일 설정 탭
    @FXML private CheckBox enableDuplicateDetectionCheckBox;
    @FXML private CheckBox autoResolveDuplicatesCheckBox;
    @FXML private ComboBox<String> duplicateStrategyComboBox;

    // 성능 설정 탭
    @FXML private Spinner<Integer> maxFileSizeSpinner;
    @FXML private Spinner<Integer> monitoringIntervalSpinner;
    @FXML private Spinner<Integer> maxFileCountSpinner;
    @FXML private CheckBox enableContentAnalysisCheckBox;
    @FXML private CheckBox enableAIAnalysisCheckBox;
    @FXML private PasswordField aiApiKeyField;

    // UI 설정 탭
    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox minimizeToTrayCheckBox;
    @FXML private CheckBox startWithWindowsCheckBox;
    @FXML private CheckBox debugModeCheckBox;

    // 버튼들
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    // 서비스와 상태
    private ConfigService configService;
    private AppConfig originalConfig;
    private Stage settingsStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 서비스 초기화
        configService = new ConfigService();
        originalConfig = configService.getCurrentConfig();

        // UI 컴포넌트 설정
        setupSpinners();
        setupComboBoxes();
        setupEventHandlers();

        // 현재 설정 값으로 UI 초기화
        loadConfigToUI(originalConfig);

        System.out.println("[INFO] 설정 화면이 초기화되었습니다.");
    }

    /**
     * Spinner 컴포넌트들 설정
     */
    private void setupSpinners() {
        // 최대 파일 크기 Spinner (1MB ~ 10GB)
        maxFileSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10240, 100, 10));
        maxFileSizeSpinner.setEditable(true);

        // 모니터링 간격 Spinner (1초 ~ 1시간)
        monitoringIntervalSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3600, 5, 1));
        monitoringIntervalSpinner.setEditable(true);

        // 최대 파일 수 Spinner (10개 ~ 10만개)
        maxFileCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100000, 1000, 100));
        maxFileCountSpinner.setEditable(true);
    }

    /**
     * ComboBox 컴포넌트들 설정
     */
    private void setupComboBoxes() {
        // 중복 해결 전략
        duplicateStrategyComboBox.getItems().addAll(
                "사용자에게 물어보기",
                "최신 파일 유지",
                "큰 파일 유지",
                "작은 파일 유지"
        );
        duplicateStrategyComboBox.setValue("사용자에게 물어보기");

        // 언어 설정
        languageComboBox.getItems().addAll(
                "한국어",
                "English"
        );
        languageComboBox.setValue("한국어");

        // 테마 설정
        themeComboBox.getItems().addAll(
                "밝은 테마",
                "어두운 테마",
                "시스템 설정 따르기"
        );
        themeComboBox.setValue("밝은 테마");
    }

    /**
     * 이벤트 핸들러들 설정
     */
    private void setupEventHandlers() {
        // 자동 정리 체크박스와 실시간 모니터링 연동
        autoOrganizeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                realTimeMonitoringCheckBox.setSelected(false);
            }
            realTimeMonitoringCheckBox.setDisable(!newVal);
        });

        // 중복 탐지와 자동 해결 연동
        enableDuplicateDetectionCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            autoResolveDuplicatesCheckBox.setDisable(!newVal);
            duplicateStrategyComboBox.setDisable(!newVal || !autoResolveDuplicatesCheckBox.isSelected());
        });

        autoResolveDuplicatesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            duplicateStrategyComboBox.setDisable(!newVal || !enableDuplicateDetectionCheckBox.isSelected());
        });

        // AI 분석과 API 키 연동
        enableAIAnalysisCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            aiApiKeyField.setDisable(!newVal);
        });

        // 초기 상태 설정 (기본값에 따라)
        setInitialUIStates();
    }

    /**
     * 초기 UI 상태 설정
     */
    private void setInitialUIStates() {
        // 자동 정리가 비활성화되어 있으면 실시간 모니터링도 비활성화
        realTimeMonitoringCheckBox.setDisable(!autoOrganizeCheckBox.isSelected());

        // 중복 탐지가 비활성화되어 있으면 관련 컨트롤들도 비활성화
        autoResolveDuplicatesCheckBox.setDisable(!enableDuplicateDetectionCheckBox.isSelected());
        duplicateStrategyComboBox.setDisable(!enableDuplicateDetectionCheckBox.isSelected() ||
                !autoResolveDuplicatesCheckBox.isSelected());

        // AI 분석이 비활성화되어 있으면 API 키 필드도 비활성화
        aiApiKeyField.setDisable(!enableAIAnalysisCheckBox.isSelected());
    }

    /**
     * 설정 값을 UI에 로드
     */
    private void loadConfigToUI(AppConfig config) {
        // 기본 설정
        defaultScanFolderField.setText(config.getDefaultScanFolder());
        organizationFolderField.setText(config.getOrganizationRootFolder());

        autoOrganizeCheckBox.setSelected(config.isAutoOrganizeEnabled());
        realTimeMonitoringCheckBox.setSelected(config.isRealTimeMonitoring());
        showNotificationsCheckBox.setSelected(config.isShowNotifications());

        organizeByDateCheckBox.setSelected(config.isOrganizeByDate());
        createSubfoldersCheckBox.setSelected(config.isCreateSubfolders());
        backupBeforeOrganizingCheckBox.setSelected(config.isBackupBeforeOrganizing());

        // 중복 파일 설정
        enableDuplicateDetectionCheckBox.setSelected(config.isEnableDuplicateDetection());
        autoResolveDuplicatesCheckBox.setSelected(config.isAutoResolveDuplicates());

        // 중복 해결 전략 매핑
        String strategy = config.getDuplicateResolutionStrategy();
        switch (strategy) {
            case "KEEP_NEWEST": duplicateStrategyComboBox.setValue("최신 파일 유지"); break;
            case "KEEP_LARGEST": duplicateStrategyComboBox.setValue("큰 파일 유지"); break;
            case "KEEP_SMALLEST": duplicateStrategyComboBox.setValue("작은 파일 유지"); break;
            default: duplicateStrategyComboBox.setValue("사용자에게 물어보기"); break;
        }

        // 성능 설정
        maxFileSizeSpinner.getValueFactory().setValue(config.getMaxFileSizeForAnalysis());
        monitoringIntervalSpinner.getValueFactory().setValue(config.getMonitoringInterval());
        maxFileCountSpinner.getValueFactory().setValue(config.getMaxFileCount());

        enableContentAnalysisCheckBox.setSelected(config.isEnableContentAnalysis());
        enableAIAnalysisCheckBox.setSelected(config.isEnableAIAnalysis());
        aiApiKeyField.setText(config.getAiApiKey() != null ? config.getAiApiKey() : "");

        // UI 설정
        languageComboBox.setValue("ko".equals(config.getLanguage()) ? "한국어" : "English");
        themeComboBox.setValue("dark".equals(config.getTheme()) ? "어두운 테마" : "밝은 테마");
        minimizeToTrayCheckBox.setSelected(config.isMinimizeToTray());
        startWithWindowsCheckBox.setSelected(config.isStartWithWindows());
        debugModeCheckBox.setSelected(config.isDebugMode());

        // 설정 로드 후 UI 상태 업데이트
        updateUIStatesAfterLoad();
    }

    /**
     * 설정 로드 후 UI 상태 업데이트
     */
    private void updateUIStatesAfterLoad() {
        // 자동 정리 상태에 따라 실시간 모니터링 활성화/비활성화
        realTimeMonitoringCheckBox.setDisable(!autoOrganizeCheckBox.isSelected());

        // 중복 탐지 상태에 따라 관련 컨트롤들 활성화/비활성화
        autoResolveDuplicatesCheckBox.setDisable(!enableDuplicateDetectionCheckBox.isSelected());
        duplicateStrategyComboBox.setDisable(!enableDuplicateDetectionCheckBox.isSelected() ||
                !autoResolveDuplicatesCheckBox.isSelected());

        // AI 분석 상태에 따라 API 키 필드 활성화/비활성화
        aiApiKeyField.setDisable(!enableAIAnalysisCheckBox.isSelected());
    }

    /**
     * UI 값을 설정 객체로 변환
     */
    private AppConfig getConfigFromUI() {
        AppConfig config = new AppConfig();

        // 기본 설정
        config.setDefaultScanFolder(defaultScanFolderField.getText());
        config.setOrganizationRootFolder(organizationFolderField.getText());

        config.setAutoOrganizeEnabled(autoOrganizeCheckBox.isSelected());
        config.setRealTimeMonitoring(realTimeMonitoringCheckBox.isSelected());
        config.setShowNotifications(showNotificationsCheckBox.isSelected());

        config.setOrganizeByDate(organizeByDateCheckBox.isSelected());
        config.setCreateSubfolders(createSubfoldersCheckBox.isSelected());
        config.setBackupBeforeOrganizing(backupBeforeOrganizingCheckBox.isSelected());

        // 중복 파일 설정
        config.setEnableDuplicateDetection(enableDuplicateDetectionCheckBox.isSelected());
        config.setAutoResolveDuplicates(autoResolveDuplicatesCheckBox.isSelected());

        // 중복 해결 전략 매핑
        String selectedStrategy = duplicateStrategyComboBox.getValue();
        switch (selectedStrategy) {
            case "최신 파일 유지": config.setDuplicateResolutionStrategy("KEEP_NEWEST"); break;
            case "큰 파일 유지": config.setDuplicateResolutionStrategy("KEEP_LARGEST"); break;
            case "작은 파일 유지": config.setDuplicateResolutionStrategy("KEEP_SMALLEST"); break;
            default: config.setDuplicateResolutionStrategy("ASK_USER"); break;
        }

        // 성능 설정
        config.setMaxFileSizeForAnalysis(maxFileSizeSpinner.getValue());
        config.setMonitoringInterval(monitoringIntervalSpinner.getValue());
        config.setMaxFileCount(maxFileCountSpinner.getValue());

        config.setEnableContentAnalysis(enableContentAnalysisCheckBox.isSelected());
        config.setEnableAIAnalysis(enableAIAnalysisCheckBox.isSelected());

        String apiKey = aiApiKeyField.getText();
        config.setAiApiKey(apiKey.trim().isEmpty() ? null : apiKey.trim());

        // UI 설정
        config.setLanguage("한국어".equals(languageComboBox.getValue()) ? "ko" : "en");
        config.setTheme("어두운 테마".equals(themeComboBox.getValue()) ? "dark" : "light");
        config.setMinimizeToTray(minimizeToTrayCheckBox.isSelected());
        config.setStartWithWindows(startWithWindowsCheckBox.isSelected());
        config.setDebugMode(debugModeCheckBox.isSelected());

        return config;
    }

    /**
     * 스캔 폴더 찾아보기
     */
    @FXML
    private void handleBrowseScanFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("🔍 기본 스캔 폴더 선택");

        String currentPath = defaultScanFolderField.getText();
        if (!currentPath.isEmpty() && new File(currentPath).exists()) {
            chooser.setInitialDirectory(new File(currentPath));
        }

        File selectedDir = chooser.showDialog(getStage());
        if (selectedDir != null) {
            defaultScanFolderField.setText(selectedDir.getAbsolutePath());
        }
    }

    /**
     * 정리 폴더 찾아보기
     */
    @FXML
    private void handleBrowseOrganizationFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("📦 정리 폴더 선택");

        String currentPath = organizationFolderField.getText();
        if (!currentPath.isEmpty() && new File(currentPath).exists()) {
            chooser.setInitialDirectory(new File(currentPath));
        }

        File selectedDir = chooser.showDialog(getStage());
        if (selectedDir != null) {
            organizationFolderField.setText(selectedDir.getAbsolutePath());
        }
    }

    /**
     * 설정 저장
     */
    @FXML
    private void handleSave() {
        try {
            AppConfig newConfig = getConfigFromUI();

            // 설정 유효성 검증
            if (!newConfig.isValid()) {
                UIFactory.showInfoDialog("❌ 설정 오류",
                        "입력된 설정이 유효하지 않습니다.\n" +
                                "폴더 경로와 숫자 값들을 확인해주세요.");
                return;
            }

            // 설정 저장
            if (configService.saveConfig(newConfig)) {
                UIFactory.showInfoDialog("💾 저장 완료",
                        "설정이 성공적으로 저장되었습니다.\n" +
                                "일부 설정은 애플리케이션을 다시 시작해야 적용됩니다.");

                closeWindow();
            } else {
                UIFactory.showInfoDialog("❌ 저장 실패",
                        "설정 저장에 실패했습니다.\n" +
                                "폴더 권한을 확인하거나 다시 시도해주세요.");
            }

        } catch (Exception e) {
            UIFactory.showInfoDialog("❌ 오류",
                    "설정 저장 중 오류가 발생했습니다:\n" + e.getMessage());
        }
    }

    /**
     * 기본값으로 복원
     */
    @FXML
    private void handleReset() {
        boolean confirmed = UIFactory.showConfirmDialog("🔄 설정 초기화",
                "모든 설정을 기본값으로 초기화하시겠습니까?\n" +
                        "현재 설정은 백업됩니다.");

        if (confirmed) {
            AppConfig defaultConfig = AppConfig.createDefault();
            loadConfigToUI(defaultConfig);

            UIFactory.showInfoDialog("🔄 초기화 완료",
                    "설정이 기본값으로 초기화되었습니다.\n" +
                            "'저장' 버튼을 클릭해서 적용하세요.");
        }
    }

    /**
     * 취소 (창 닫기)
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * 설정 창 닫기
     */
    private void closeWindow() {
        if (settingsStage != null) {
            settingsStage.close();
        }
    }

    /**
     * 현재 Stage 반환
     */
    private Stage getStage() {
        if (settingsStage == null) {
            settingsStage = (Stage) saveButton.getScene().getWindow();
        }
        return settingsStage;
    }

    /**
     * Stage 설정 (외부에서 호출)
     */
    public void setStage(Stage stage) {
        this.settingsStage = stage;
    }
}