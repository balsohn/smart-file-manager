package com.smartfilemanager.controller;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileRule;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.service.CustomRulesManager;
import com.smartfilemanager.ui.ThemeManager;
import com.smartfilemanager.ui.UIFactory;
import com.smartfilemanager.util.AIAnalyzer;
import com.smartfilemanager.util.FileTypeDetector;
import com.smartfilemanager.util.StartupManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 설정 화면 컨트롤러 (AI 분석 완전 통합 버전)
 * 사용자가 애플리케이션 설정을 변경할 수 있도록 합니다
 */
public class SettingsController implements Initializable {

    // ===============================
    // 📋 FXML UI 컴포넌트들
    // ===============================

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

    // 정리 규칙 설정 탭
    @FXML private CheckBox useCustomRulesCheckBox;
    @FXML private TextField customRulesFilePathField;
    @FXML private Button browseRulesFileButton;
    
    @FXML private TableView<FileRule> rulesTableView;
    @FXML private TableColumn<FileRule, Boolean> ruleEnabledColumn;
    @FXML private TableColumn<FileRule, String> ruleNameColumn;
    @FXML private TableColumn<FileRule, String> ruleExtensionsColumn;
    @FXML private TableColumn<FileRule, String> ruleTargetFolderColumn;
    @FXML private TableColumn<FileRule, Integer> rulePriorityColumn;
    
    @FXML private Button addRuleButton;
    @FXML private Button editRuleButton;
    @FXML private Button deleteRuleButton;
    @FXML private Button importRulesButton;
    @FXML private Button exportRulesButton;
    @FXML private Button reloadRulesButton;
    
    @FXML private TextField previewFileNameField;
    @FXML private Label previewResultLabel;
    
    @FXML private Label totalRulesLabel;
    @FXML private Label enabledRulesLabel;
    @FXML private Label totalExtensionsLabel;
    @FXML private Label conflictingExtensionsLabel;

    // 버튼들
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    // AI 관련 UI 컴포넌트들
    @FXML private Label aiStatusLabel;
    @FXML private Button testApiKeyButton;

    // 서비스와 상태
    private ConfigService configService;
    private AppConfig originalConfig;
    private Stage settingsStage;
    private CustomRulesManager customRulesManager;

    // ===============================
    // 🚀 초기화 메서드들
    // ===============================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[INFO] 설정 화면 초기화 시작");

        // 서비스 초기화
        configService = new ConfigService();
        originalConfig = configService.getCurrentConfig();

        // UI 컴포넌트 설정
        setupSpinners();
        setupComboBoxes();
        setupEventHandlers();
        setupAIEventHandlers();
        setupRulesManagement();

        // 현재 설정 값으로 UI 초기화
        loadConfigToUI(originalConfig);

        setInitialUIStates();
        updateUIStatesAfterLoad();

        System.out.println("[SUCCESS] 설정 화면 초기화 완료");
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
     * 기본 이벤트 핸들러 설정
     */
    private void setupEventHandlers() {
        // 폴더 선택 버튼들
        browseScanFolderButton.setOnAction(e -> handleBrowseScanFolder());
        browseOrganizationFolderButton.setOnAction(e -> handleBrowseOrganizationFolder());

        // 중복 파일 탐지 체크박스 변경 시 관련 옵션들 활성화/비활성화
        enableDuplicateDetectionCheckBox.setOnAction(e -> {
            boolean duplicateEnabled = enableDuplicateDetectionCheckBox.isSelected();
            autoResolveDuplicatesCheckBox.setDisable(!duplicateEnabled);
            duplicateStrategyComboBox.setDisable(!duplicateEnabled || !autoResolveDuplicatesCheckBox.isSelected());
        });

        // 자동 중복 해결 체크박스 변경 시 전략 콤보박스 활성화/비활성화
        autoResolveDuplicatesCheckBox.setOnAction(e -> {
            boolean autoResolve = autoResolveDuplicatesCheckBox.isSelected();
            duplicateStrategyComboBox.setDisable(!autoResolve);
        });

        // Windows 시작프로그램 체크박스 이벤트
        startWithWindowsCheckBox.setOnAction(e -> {
            boolean shouldStart = startWithWindowsCheckBox.isSelected();
            handleStartupToggle(shouldStart);
        });

        // 테마 변경 이벤트
        themeComboBox.setOnAction(e -> {
            String selectedTheme = themeComboBox.getValue();
            if (selectedTheme != null) {
                handleThemeChange();
            }
        });

        // 자동 정리 체크박스 이벤트
        autoOrganizeCheckBox.setOnAction(e -> {
            boolean autoEnabled = autoOrganizeCheckBox.isSelected();
            realTimeMonitoringCheckBox.setDisable(!autoEnabled);
            if (!autoEnabled) {
                realTimeMonitoringCheckBox.setSelected(false);
            }
        });
    }

    /**
     * AI 설정 이벤트 핸들러 설정
     */
    private void setupAIEventHandlers() {
        // AI 분석 체크박스 이벤트
        enableAIAnalysisCheckBox.setOnAction(e -> {
            boolean enabled = enableAIAnalysisCheckBox.isSelected();

            // API 키 필드 활성화/비활성화
            aiApiKeyField.setDisable(!enabled);

            // 테스트 버튼 활성화/비활성화
            if (testApiKeyButton != null) {
                testApiKeyButton.setDisable(!enabled);
            }

            if (enabled) {
                // AI 분석 활성화 시 도움말 표시
                aiApiKeyField.setPromptText("OpenAI API 키를 입력하세요 (예: sk-...)");

                // API 키가 없으면 포커스 이동
                if (aiApiKeyField.getText() == null || aiApiKeyField.getText().trim().isEmpty()) {
                    Platform.runLater(() -> aiApiKeyField.requestFocus());
                }
            } else {
                aiApiKeyField.setPromptText("AI 분석을 활성화하면 사용할 수 있습니다");
                aiApiKeyField.clear();
            }

            updateAIStatusLabel();
        });

        // API 키 입력 이벤트
        aiApiKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateAIStatusLabel();

            // API 키 형식 검증
            if (newValue != null && !newValue.trim().isEmpty()) {
                validateApiKeyFormat(newValue.trim());
            }
        });

        // API 키 테스트 버튼
        if (testApiKeyButton != null) {
            testApiKeyButton.setOnAction(this::handleTestApiKey);
        }
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

        // 시작프로그램 기능 사용 가능 여부에 따른 UI 설정
        if (!StartupManager.isSupported()) {
            startWithWindowsCheckBox.setDisable(true);
            startWithWindowsCheckBox.setTooltip(new Tooltip("Windows에서만 지원되는 기능입니다"));
            System.out.println("[INFO] 시작프로그램 기능 비활성화 (Windows 아님)");
        } else {
            startWithWindowsCheckBox.setDisable(false);
            startWithWindowsCheckBox.setTooltip(new Tooltip("Windows 시작 시 자동으로 실행"));
            System.out.println("[INFO] 시작프로그램 기능 활성화");
        }
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
        if (testApiKeyButton != null) {
            testApiKeyButton.setDisable(!enableAIAnalysisCheckBox.isSelected());
        }
    }

    // ===============================
    // 📋 설정 데이터 로드/저장 메서드들
    // ===============================

    /**
     * 설정을 UI에 로드
     */
    private void loadConfigToUI(AppConfig config) {
        // 기본 설정들
        defaultScanFolderField.setText(config.getDefaultScanFolder() != null ?
                config.getDefaultScanFolder() : "");
        organizationFolderField.setText(config.getOrganizationRootFolder() != null ?
                config.getOrganizationRootFolder() : "");

        // 체크박스들
        autoOrganizeCheckBox.setSelected(config.isAutoOrganizeEnabled());
        realTimeMonitoringCheckBox.setSelected(config.isRealTimeMonitoring());
        showNotificationsCheckBox.setSelected(config.isShowNotifications());
        organizeByDateCheckBox.setSelected(config.isOrganizeByDate());
        createSubfoldersCheckBox.setSelected(config.isCreateSubfolders());
        backupBeforeOrganizingCheckBox.setSelected(config.isBackupBeforeOrganizing());

        // 중복 파일 설정
        enableDuplicateDetectionCheckBox.setSelected(config.isEnableDuplicateDetection());
        autoResolveDuplicatesCheckBox.setSelected(config.isAutoResolveDuplicates());
        loadDuplicateStrategyToComboBox(config.getDuplicateResolutionStrategy());

        // 성능 설정
        maxFileSizeSpinner.getValueFactory().setValue(config.getMaxFileSizeForAnalysis());
        monitoringIntervalSpinner.getValueFactory().setValue(config.getMonitoringInterval());
        maxFileCountSpinner.getValueFactory().setValue(config.getMaxFileCount());
        enableContentAnalysisCheckBox.setSelected(config.isEnableContentAnalysis());

        // AI 설정 로드
        loadAISettingsFromConfig(config);

        // UI 설정
        loadLanguageToComboBox(config.getLanguage());
        loadThemeToComboBox(config.getTheme());
        minimizeToTrayCheckBox.setSelected(config.isMinimizeToTray());
        debugModeCheckBox.setSelected(config.isDebugMode());

        // Windows 시작프로그램 설정 로드
        loadStartupSettings(config);
        
        // 커스텀 규칙 설정 로드
        loadCustomRulesSettings(config);
    }

    /**
     * UI 값을 설정 객체로 변환
     */
    private AppConfig getConfigFromUI() {
        AppConfig config = new AppConfig();

        // 기본 설정
        config.setDefaultScanFolder(defaultScanFolderField.getText().trim());
        config.setOrganizationRootFolder(organizationFolderField.getText().trim());

        config.setAutoOrganizeEnabled(autoOrganizeCheckBox.isSelected());
        config.setRealTimeMonitoring(realTimeMonitoringCheckBox.isSelected());
        config.setShowNotifications(showNotificationsCheckBox.isSelected());

        config.setOrganizeByDate(organizeByDateCheckBox.isSelected());
        config.setCreateSubfolders(createSubfoldersCheckBox.isSelected());
        config.setBackupBeforeOrganizing(backupBeforeOrganizingCheckBox.isSelected());

        // 중복 파일 설정
        config.setEnableDuplicateDetection(enableDuplicateDetectionCheckBox.isSelected());
        config.setAutoResolveDuplicates(autoResolveDuplicatesCheckBox.isSelected());
        config.setDuplicateResolutionStrategy(getDuplicateStrategyFromComboBox());

        // 성능 설정
        config.setMaxFileSizeForAnalysis(maxFileSizeSpinner.getValue());
        config.setMonitoringInterval(monitoringIntervalSpinner.getValue());
        config.setMaxFileCount(maxFileCountSpinner.getValue());
        config.setEnableContentAnalysis(enableContentAnalysisCheckBox.isSelected());

        // AI 설정 적용
        applyAISettingsToConfig(config);

        // UI 설정
        config.setLanguage(getLanguageFromComboBox());
        config.setTheme(getThemeFromComboBox());
        config.setMinimizeToTray(minimizeToTrayCheckBox.isSelected());
        config.setDebugMode(debugModeCheckBox.isSelected());

        // Windows 시작프로그램 설정
        if (StartupManager.isSupported()) {
            config.setStartWithWindows(startWithWindowsCheckBox.isSelected());
        } else {
            config.setStartWithWindows(false);
        }

        // 커스텀 규칙 설정
        config.setUseCustomRules(useCustomRulesCheckBox.isSelected());
        config.setCustomRulesFilePath(customRulesFilePathField.getText().trim());

        return config;
    }

    // ===============================
    // 🎯 FXML 이벤트 핸들러들
    // ===============================

    /**
     * 스캔 폴더 찾아보기
     */
    @FXML
    private void handleBrowseScanFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("기본 스캔 폴더 선택");

        String currentPath = defaultScanFolderField.getText().trim();
        if (!currentPath.isEmpty() && Files.exists(Paths.get(currentPath))) {
            directoryChooser.setInitialDirectory(new File(currentPath));
        } else {
            File defaultDir = new File(System.getProperty("user.home"), "Downloads");
            if (defaultDir.exists()) {
                directoryChooser.setInitialDirectory(defaultDir);
            }
        }

        File selectedDirectory = directoryChooser.showDialog(getStage());
        if (selectedDirectory != null) {
            defaultScanFolderField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * 정리 폴더 찾아보기
     */
    @FXML
    private void handleBrowseOrganizationFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("파일 정리 폴더 선택");

        String currentPath = organizationFolderField.getText().trim();
        if (!currentPath.isEmpty() && Files.exists(Paths.get(currentPath))) {
            directoryChooser.setInitialDirectory(new File(currentPath));
        } else {
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        File selectedDirectory = directoryChooser.showDialog(getStage());
        if (selectedDirectory != null) {
            organizationFolderField.setText(selectedDirectory.getAbsolutePath());
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
            if (!validateConfig(newConfig)) {
                return;
            }

            // 시작프로그램 설정 동기화 검증
            syncStartupSettings(newConfig);

            // AI 설정 추가 검증
            if (!validateAISettings(newConfig)) {
                return;
            }

            // 설정 저장
            if (configService.saveConfig(newConfig)) {
                String message = buildSaveSuccessMessage(newConfig);
                UIFactory.showInfoDialog("💾 저장 완료", message);
                closeWindow();
            } else {
                UIFactory.showInfoDialog("❌ 저장 실패",
                        "설정 저장에 실패했습니다.\n폴더 권한을 확인하거나 다시 시도해주세요.");
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
                "모든 설정을 기본값으로 초기화하시겠습니까?\n현재 설정은 백업됩니다.");

        if (confirmed) {
            AppConfig defaultConfig = AppConfig.createDefault();
            loadConfigToUI(defaultConfig);
            resetAISettings();

            UIFactory.showInfoDialog("🔄 초기화 완료",
                    "설정이 기본값으로 초기화되었습니다.\n'저장' 버튼을 클릭해서 적용하세요.");
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
     * AI 설정 도움말 표시
     */
    @FXML
    private void handleAIHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("AI 분석 도움말");
        helpAlert.setHeaderText("AI 분석 기능에 대한 설명");

        String helpText = """
        🤖 AI 분석 기능
        
        • AI 분석을 활성화하면 OpenAI의 GPT 모델을 사용해서
          파일을 더 정확하게 분류할 수 있습니다
        
        • API 키가 필요합니다:
          1. https://platform.openai.com 에 회원가입
          2. API Keys 메뉴에서 새 키 생성
          3. 생성된 키를 여기에 입력
        
        • 분석되는 정보:
          - 파일명과 확장자
          - 파일 크기와 생성일
          - 추출된 메타데이터
          (실제 파일 내용은 전송되지 않습니다)
        
        • API 비용:
          - 파일당 약 0.001~0.01원 정도
          - 월 사용량 제한 설정 권장
        
        ⚠️ 주의사항:
        • 개인 API 키를 안전하게 보관하세요
        • 민감한 파일명이 있다면 비활성화하세요
        """;

        helpAlert.setContentText(helpText);
        helpAlert.getDialogPane().setPrefWidth(500);
        helpAlert.showAndWait();
    }

    /**
     * API 키 테스트 이벤트 핸들러
     */
    @FXML
    private void handleTestApiKey(ActionEvent event) {
        String apiKey = aiApiKeyField.getText();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            showAlert("오류", "API 키를 입력해주세요", Alert.AlertType.ERROR);
            return;
        }

        if (!isValidApiKeyFormat(apiKey.trim())) {
            showAlert("오류", "API 키 형식이 올바르지 않습니다.\nOpenAI API 키는 'sk-'로 시작해야 합니다.", Alert.AlertType.ERROR);
            return;
        }

        testApiKeyAsync(apiKey.trim());
    }

    // ===============================
    // 🤖 AI 분석 관련 메서드들
    // ===============================

    /**
     * AppConfig에서 AI 설정을 UI로 로드
     */
    private void loadAISettingsFromConfig(AppConfig config) {
        enableAIAnalysisCheckBox.setSelected(config.isEnableAIAnalysis());

        String apiKey = config.getAiApiKey();
        if (apiKey != null) {
            aiApiKeyField.setText(apiKey);
        } else {
            aiApiKeyField.clear();
        }

        updateAIStatusLabel();
    }

    /**
     * AI 설정을 UI에서 AppConfig로 적용
     */
    private void applyAISettingsToConfig(AppConfig config) {
        config.setEnableAIAnalysis(enableAIAnalysisCheckBox.isSelected());

        String apiKey = aiApiKeyField.getText();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            config.setAiApiKey(apiKey.trim());
        } else {
            config.setAiApiKey(null);
        }

        config.setAiModel("gpt-3.5-turbo");
    }

    /**
     * AI 상태 라벨 업데이트
     */
    private void updateAIStatusLabel() {
        if (!enableAIAnalysisCheckBox.isSelected()) {
            setAIStatusLabel("AI 분석이 비활성화되어 있습니다", "warning");
            return;
        }

        String apiKey = aiApiKeyField.getText();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            setAIStatusLabel("API 키가 필요합니다", "error");
            return;
        }

        if (!isValidApiKeyFormat(apiKey.trim())) {
            setAIStatusLabel("API 키 형식이 올바르지 않습니다", "error");
            return;
        }

        setAIStatusLabel("AI 분석이 활성화되어 있습니다 (테스트 필요)", "success");
    }

    /**
     * AI 상태 라벨 설정
     */
    private void setAIStatusLabel(String message, String type) {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(message);
            aiStatusLabel.getStyleClass().removeAll("status-success", "status-warning", "status-error");
            aiStatusLabel.getStyleClass().add("status-" + type);
        } else {
            System.out.println("[AI Status] " + message);
        }
    }

    /**
     * API 키 형식 검증
     */
    private boolean isValidApiKeyFormat(String apiKey) {
        return apiKey.startsWith("sk-") && apiKey.length() > 20;
    }

    /**
     * API 키 형식 실시간 검증
     */
    private void validateApiKeyFormat(String apiKey) {
        if (!isValidApiKeyFormat(apiKey)) {
            if (aiApiKeyField.getTooltip() == null) {
                Tooltip tooltip = new Tooltip("OpenAI API 키는 'sk-'로 시작해야 합니다");
                aiApiKeyField.setTooltip(tooltip);
            }

            if (!aiApiKeyField.getStyleClass().contains("text-field-error")) {
                aiApiKeyField.getStyleClass().add("text-field-error");
            }
        } else {
            aiApiKeyField.setTooltip(null);
            aiApiKeyField.getStyleClass().remove("text-field-error");
        }
    }

    /**
     * 비동기 API 키 테스트
     */
    private void testApiKeyAsync(String apiKey) {
        if (testApiKeyButton != null) {
            testApiKeyButton.setDisable(true);
            testApiKeyButton.setText("테스트 중...");
        }

        setAIStatusLabel("API 키를 테스트하고 있습니다...", "warning");

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    AIAnalyzer testAnalyzer = new AIAnalyzer();
                    testAnalyzer.setApiKey(apiKey);
                    return testAnalyzer.validateApiKey();
                } catch (Exception e) {
                    System.err.println("[ERROR] API 키 테스트 실패: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean isValid = getValue();

                    if (isValid) {
                        setAIStatusLabel("✅ API 키가 유효합니다!", "success");
                        showAlert("성공", "API 키가 유효합니다.\nAI 분석 기능을 사용할 수 있습니다.", Alert.AlertType.INFORMATION);
                    } else {
                        setAIStatusLabel("❌ API 키가 유효하지 않습니다", "error");
                        showAlert("오류", "API 키가 유효하지 않습니다.\n다시 확인해주세요.", Alert.AlertType.ERROR);
                    }

                    if (testApiKeyButton != null) {
                        testApiKeyButton.setDisable(false);
                        testApiKeyButton.setText("테스트");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setAIStatusLabel("❌ API 키 테스트 실패", "error");
                    showAlert("오류", "API 키 테스트 중 오류가 발생했습니다:\n" + getException().getMessage(), Alert.AlertType.ERROR);

                    if (testApiKeyButton != null) {
                        testApiKeyButton.setDisable(false);
                        testApiKeyButton.setText("테스트");
                    }
                });
            }
        };

        Thread testThread = new Thread(testTask);
        testThread.setDaemon(true);
        testThread.start();
    }

    /**
     * AI 설정 초기화
     */
    private void resetAISettings() {
        enableAIAnalysisCheckBox.setSelected(false);
        aiApiKeyField.clear();
        aiApiKeyField.setDisable(true);

        if (testApiKeyButton != null) {
            testApiKeyButton.setDisable(true);
        }

        setAIStatusLabel("AI 분석이 비활성화되어 있습니다", "warning");
    }

    /**
     * AI 설정 검증
     */
    private boolean validateAISettings(AppConfig config) {
        if (config.isEnableAIAnalysis()) {
            if (config.getAiApiKey() == null || config.getAiApiKey().trim().isEmpty()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("AI 설정 확인");
                confirmAlert.setHeaderText("AI 분석이 활성화되어 있지만 API 키가 없습니다");
                confirmAlert.setContentText("API 키 없이 저장하시겠습니까?\n(AI 분석 기능은 사용할 수 없습니다)");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return false;
                }
            } else if (!isValidApiKeyFormat(config.getAiApiKey())) {
                showAlert("오류", "API 키 형식이 올바르지 않습니다.\n'sk-'로 시작하는 유효한 OpenAI API 키를 입력해주세요.", Alert.AlertType.ERROR);
                return false;
            }
        }
        return true;
    }

    // ===============================
    // 🖥️ 테마 관리 메서드들
    // ===============================

    /**
     * 테마 변경 처리
     */
    private void handleThemeChange() {
        String selectedTheme = themeComboBox.getValue();
        if (selectedTheme != null) {
            String themeId = selectedTheme.toLowerCase().contains("dark") ? "dark" : "light";

            try {
                ThemeManager.applyThemeById(themeId);
                System.out.println("[INFO] 테마 변경됨: " + selectedTheme);
                showThemePreview(selectedTheme);
            } catch (Exception e) {
                System.err.println("[ERROR] 테마 적용 실패: " + e.getMessage());
                String currentThemeId = ThemeManager.getCurrentThemeId();
                loadThemeToComboBox(currentThemeId);
            }
        }
    }

    /**
     * 테마 미리보기 메시지
     */
    private void showThemePreview(String themeName) {
        themeComboBox.setTooltip(new Tooltip("현재 적용됨: " + themeName));
    }

    /**
     * 테마를 콤보박스에 로드
     */
    private void loadThemeToComboBox(String themeId) {
        if (themeId == null) themeId = "light";

        if (themeId.equals("dark")) {
            themeComboBox.setValue("어두운 테마");
        } else {
            themeComboBox.setValue("밝은 테마");
        }
    }

    /**
     * 콤보박스에서 테마 ID 추출
     */
    private String getThemeFromComboBox() {
        String selectedTheme = themeComboBox.getValue();
        if (selectedTheme != null) {
            return selectedTheme.toLowerCase().contains("dark") ? "dark" : "light";
        }
        return "light";
    }

    // ===============================
    // 🌐 언어 설정 메서드들
    // ===============================

    /**
     * 언어를 콤보박스에 로드
     */
    private void loadLanguageToComboBox(String languageCode) {
        if (languageCode == null) languageCode = "ko";

        switch (languageCode) {
            case "ko":
                languageComboBox.setValue("한국어");
                break;
            case "en":
                languageComboBox.setValue("English");
                break;
            default:
                languageComboBox.setValue("한국어");
                break;
        }
    }

    /**
     * 콤보박스에서 언어 코드 추출
     */
    private String getLanguageFromComboBox() {
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage != null) {
            if (selectedLanguage.equals("English")) {
                return "en";
            } else {
                return "ko";
            }
        }
        return "ko";
    }

    // ===============================
    // 📁 중복 파일 설정 메서드들
    // ===============================

    /**
     * 중복 해결 전략을 콤보박스에 로드
     */
    private void loadDuplicateStrategyToComboBox(String strategy) {
        if (strategy == null) strategy = "ASK_USER";

        switch (strategy) {
            case "KEEP_NEWEST":
                duplicateStrategyComboBox.setValue("최신 파일 유지");
                break;
            case "KEEP_LARGEST":
                duplicateStrategyComboBox.setValue("큰 파일 유지");
                break;
            case "KEEP_SMALLEST":
                duplicateStrategyComboBox.setValue("작은 파일 유지");
                break;
            case "ASK_USER":
            default:
                duplicateStrategyComboBox.setValue("사용자에게 물어보기");
                break;
        }
    }

    /**
     * 콤보박스에서 중복 해결 전략 추출
     */
    private String getDuplicateStrategyFromComboBox() {
        String selectedStrategy = duplicateStrategyComboBox.getValue();
        if (selectedStrategy != null) {
            switch (selectedStrategy) {
                case "최신 파일 유지": return "KEEP_NEWEST";
                case "큰 파일 유지": return "KEEP_LARGEST";
                case "작은 파일 유지": return "KEEP_SMALLEST";
                default: return "ASK_USER";
            }
        }
        return "ASK_USER";
    }

    // ===============================
    // 🚀 시작프로그램 관리 메서드들
    // ===============================

    /**
     * Windows 시작프로그램 설정 로드
     */
    private void loadStartupSettings(AppConfig config) {
        if (StartupManager.isSupported()) {
            boolean configValue = config.isStartWithWindows();
            boolean actuallyRegistered = StartupManager.isRegistered();

            if (configValue != actuallyRegistered) {
                System.out.println("[WARNING] 시작프로그램 설정 불일치 - 실제 상태로 동기화");
                config.setStartWithWindows(actuallyRegistered);
            }

            startWithWindowsCheckBox.setSelected(actuallyRegistered);
            startWithWindowsCheckBox.setDisable(false);
        } else {
            startWithWindowsCheckBox.setSelected(false);
            startWithWindowsCheckBox.setDisable(true);
            startWithWindowsCheckBox.setTooltip(new Tooltip("Windows에서만 지원되는 기능입니다"));
        }
    }

    /**
     * Windows 시작프로그램 등록/해제 처리
     */
    private void handleStartupToggle(boolean enable) {
        if (!StartupManager.isSupported()) {
            UIFactory.showInfoDialog("❌ 지원되지 않음",
                    "Windows 시작프로그램 기능은 Windows에서만 지원됩니다.");
            startWithWindowsCheckBox.setSelected(false);
            return;
        }

        try {
            boolean success;

            if (enable) {
                String executablePath = StartupManager.getCurrentExecutablePath();
                if (executablePath == null) {
                    throw new Exception("실행 파일 경로를 확인할 수 없습니다");
                }

                success = StartupManager.register(executablePath);
                if (success) {
                    UIFactory.showInfoDialog("✅ 등록 완료",
                            "Windows 시작 시 Smart File Manager가 자동으로 실행됩니다.\n\n" +
                                    "💡 팁: 시스템 트레이로 시작하려면 '트레이로 최소화' 옵션도 활성화하세요.");
                } else {
                    throw new Exception("시작프로그램 등록에 실패했습니다");
                }
            } else {
                success = StartupManager.unregister();
                if (success) {
                    UIFactory.showInfoDialog("✅ 해제 완료",
                            "Windows 시작프로그램에서 제거되었습니다.\n" +
                                    "이제 시스템 시작 시 자동으로 실행되지 않습니다.");
                } else {
                    throw new Exception("시작프로그램 해제에 실패했습니다");
                }
            }

            startWithWindowsCheckBox.setSelected(enable);

        } catch (Exception e) {
            System.err.println("[ERROR] 시작프로그램 설정 실패: " + e.getMessage());
            startWithWindowsCheckBox.setSelected(!enable);

            UIFactory.showInfoDialog("❌ 설정 실패",
                    "시작프로그램 설정 중 오류가 발생했습니다:\n\n" +
                            e.getMessage() + "\n\n" +
                            "💡 관리자 권한으로 실행하거나 나중에 다시 시도해보세요.");
        }
    }

    /**
     * 시작프로그램 설정 동기화
     */
    private void syncStartupSettings(AppConfig newConfig) {
        if (StartupManager.isSupported()) {
            boolean configWantsStartup = newConfig.isStartWithWindows();
            boolean actuallyRegistered = StartupManager.isRegistered();

            if (configWantsStartup != actuallyRegistered) {
                System.out.println("[WARNING] 시작프로그램 설정 불일치 감지 - 자동 동기화");
                newConfig.setStartWithWindows(actuallyRegistered);
            }
        }
    }

    // ===============================
    // ✅ 유효성 검증 메서드들
    // ===============================

    /**
     * 설정 유효성 검증
     */
    private boolean validateConfig(AppConfig config) {
        if (!config.isValid()) {
            UIFactory.showInfoDialog("❌ 설정 오류",
                    "입력된 설정이 유효하지 않습니다.\n" +
                            "폴더 경로와 숫자 값들을 확인해주세요.");
            return false;
        }
        return true;
    }

    /**
     * 저장 성공 메시지 생성
     */
    private String buildSaveSuccessMessage(AppConfig config) {
        StringBuilder message = new StringBuilder("설정이 성공적으로 저장되었습니다.");

        if (config.isEnableAIAnalysis() && config.getAiApiKey() != null) {
            message.append("\n\n🤖 AI 분석 기능이 활성화됩니다.");
        }

        message.append("\n일부 설정은 애플리케이션을 다시 시작해야 적용됩니다.");
        return message.toString();
    }

    // ===============================
    // 🔧 헬퍼 및 유틸리티 메서드들
    // ===============================

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

    /**
     * 커스텀 규칙 설정을 UI에 로드
     */
    private void loadCustomRulesSettings(AppConfig config) {
        useCustomRulesCheckBox.setSelected(config.isUseCustomRules());
        customRulesFilePathField.setText(config.getCustomRulesFilePath() != null ?
                config.getCustomRulesFilePath() : "");
        
        // 커스텀 규칙 사용 여부에 따라 UI 상태 업데이트
        boolean enabled = config.isUseCustomRules();
        customRulesFilePathField.setDisable(!enabled);
        browseRulesFileButton.setDisable(!enabled);
        rulesTableView.setDisable(!enabled);
        addRuleButton.setDisable(!enabled);
        editRuleButton.setDisable(!enabled);
        deleteRuleButton.setDisable(!enabled);
        importRulesButton.setDisable(!enabled);
        exportRulesButton.setDisable(!enabled);
        reloadRulesButton.setDisable(!enabled);
        previewFileNameField.setDisable(!enabled);
        
        // 커스텀 규칙 매니저 경로 업데이트
        if (enabled && config.getCustomRulesFilePath() != null && !config.getCustomRulesFilePath().trim().isEmpty()) {
            try {
                customRulesManager.setRulesFilePath(config.getCustomRulesFilePath());
                loadRulesToTable();
                updateRulesStatistics();
            } catch (Exception e) {
                System.err.println("[ERROR] 커스텀 규칙 파일 로드 실패: " + e.getMessage());
            }
        }
    }

    // ===============================
    // 📝 규칙 관리 메서드들
    // ===============================

    /**
     * 규칙 관리 UI 설정
     */
    private void setupRulesManagement() {
        // 커스텀 규칙 매니저 초기화
        initializeCustomRulesManager();
        
        // 테이블 컬럼 설정
        setupRulesTableColumns();
        
        // 이벤트 핸들러 설정
        setupRulesEventHandlers();
        
        // 초기 데이터 로드
        loadRulesToTable();
        updateRulesStatistics();
    }

    /**
     * 커스텀 규칙 매니저 초기화
     */
    private void initializeCustomRulesManager() {
        try {
            customRulesManager = new CustomRulesManager();
            System.out.println("[INFO] 커스텀 규칙 매니저 초기화 완료");
        } catch (Exception e) {
            System.err.println("[ERROR] 커스텀 규칙 매니저 초기화 실패: " + e.getMessage());
            showAlert("오류", "규칙 관리 시스템 초기화에 실패했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * 규칙 테이블 컬럼 설정
     */
    private void setupRulesTableColumns() {
        // 활성화 컬럼 (체크박스)
        ruleEnabledColumn.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        ruleEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(ruleEnabledColumn));
        ruleEnabledColumn.setEditable(true);
        
        // 규칙명 컬럼
        ruleNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // 확장자 컬럼 (리스트를 문자열로 변환)
        ruleExtensionsColumn.setCellValueFactory(cellData -> {
            List<String> extensions = cellData.getValue().getExtensions();
            String extensionsStr = extensions != null ? String.join(", ", extensions) : "";
            return new javafx.beans.property.SimpleStringProperty(extensionsStr);
        });
        
        // 타겟 폴더 컬럼
        ruleTargetFolderColumn.setCellValueFactory(new PropertyValueFactory<>("targetFolder"));
        
        // 우선순위 컬럼
        rulePriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        
        // 테이블 편집 가능하게 설정
        rulesTableView.setEditable(true);
    }

    /**
     * 규칙 관리 이벤트 핸들러 설정
     */
    private void setupRulesEventHandlers() {
        // 커스텀 규칙 사용 체크박스
        useCustomRulesCheckBox.setOnAction(e -> {
            boolean enabled = useCustomRulesCheckBox.isSelected();
            customRulesFilePathField.setDisable(!enabled);
            browseRulesFileButton.setDisable(!enabled);
            rulesTableView.setDisable(!enabled);
            addRuleButton.setDisable(!enabled);
            editRuleButton.setDisable(!enabled);
            deleteRuleButton.setDisable(!enabled);
            importRulesButton.setDisable(!enabled);
            exportRulesButton.setDisable(!enabled);
            reloadRulesButton.setDisable(!enabled);
            previewFileNameField.setDisable(!enabled);
        });

        // 테이블 선택 변경 시 버튼 상태 업데이트
        rulesTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editRuleButton.setDisable(!hasSelection);
            deleteRuleButton.setDisable(!hasSelection);
        });
    }

    /**
     * 규칙 테이블에 데이터 로드
     */
    private void loadRulesToTable() {
        if (customRulesManager != null) {
            rulesTableView.getItems().setAll(customRulesManager.getAllRules());
        }
    }

    /**
     * 규칙 통계 업데이트
     */
    private void updateRulesStatistics() {
        if (customRulesManager != null) {
            Map<String, Object> stats = customRulesManager.getRulesStatistics();
            totalRulesLabel.setText("전체 규칙: " + stats.get("totalRules") + "개");
            enabledRulesLabel.setText("활성화: " + stats.get("enabledRules") + "개");
            totalExtensionsLabel.setText("지원 확장자: " + stats.get("totalExtensions") + "개");
            conflictingExtensionsLabel.setText("충돌: " + stats.get("conflictingExtensions") + "개");
        }
    }

    // ===============================
    // 📝 규칙 관리 이벤트 핸들러들
    // ===============================

    @FXML
    private void handleBrowseRulesFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("규칙 파일 선택");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON 파일", "*.json")
        );
        
        File selectedFile = fileChooser.showOpenDialog(settingsStage);
        if (selectedFile != null) {
            customRulesFilePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleAddRule() {
        try {
            FileRule newRule = showRuleDialog(null);
            if (newRule != null) {
                customRulesManager.addRule(newRule);
                loadRulesToTable();
                updateRulesStatistics();
                showAlert("성공", "새 규칙이 추가되었습니다.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("오류", "규칙 추가 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEditRule() {
        FileRule selectedRule = rulesTableView.getSelectionModel().getSelectedItem();
        if (selectedRule == null) {
            showAlert("선택 오류", "수정할 규칙을 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            FileRule editedRule = showRuleDialog(selectedRule);
            if (editedRule != null) {
                customRulesManager.updateRule(editedRule);
                loadRulesToTable();
                updateRulesStatistics();
                showAlert("성공", "규칙이 수정되었습니다.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("오류", "규칙 수정 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteRule() {
        FileRule selectedRule = rulesTableView.getSelectionModel().getSelectedItem();
        if (selectedRule == null) {
            showAlert("선택 오류", "삭제할 규칙을 선택해주세요.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("규칙 삭제 확인");
        confirmAlert.setHeaderText("규칙을 삭제하시겠습니까?");
        confirmAlert.setContentText("규칙명: " + selectedRule.getName());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                customRulesManager.deleteRule(selectedRule.getId());
                loadRulesToTable();
                updateRulesStatistics();
                showAlert("성공", "규칙이 삭제되었습니다.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("오류", "규칙 삭제 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleImportRules() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("규칙 파일 가져오기");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON 파일", "*.json")
        );
        
        File selectedFile = fileChooser.showOpenDialog(settingsStage);
        if (selectedFile != null) {
            try {
                customRulesManager.importRules(selectedFile.getAbsolutePath());
                loadRulesToTable();
                updateRulesStatistics();
                showAlert("성공", "규칙을 가져왔습니다.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("오류", "규칙 가져오기 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleExportRules() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("규칙 파일 내보내기");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON 파일", "*.json")
        );
        fileChooser.setInitialFileName("custom-rules-export.json");
        
        File selectedFile = fileChooser.showSaveDialog(settingsStage);
        if (selectedFile != null) {
            try {
                customRulesManager.exportRules(selectedFile.getAbsolutePath());
                showAlert("성공", "규칙을 내보냈습니다.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("오류", "규칙 내보내기 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleReloadRules() {
        try {
            customRulesManager.loadRules();
            loadRulesToTable();
            updateRulesStatistics();
            showAlert("성공", "규칙을 새로고침했습니다.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("오류", "규칙 새로고침 중 오류가 발생했습니다: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handlePreviewFileNameChanged(KeyEvent event) {
        String fileName = previewFileNameField.getText();
        if (fileName != null && !fileName.trim().isEmpty()) {
            if (customRulesManager != null && useCustomRulesCheckBox.isSelected()) {
                String category = customRulesManager.determineCategory(fileName);
                previewResultLabel.setText(category + " 폴더로 이동됩니다");
                
                // 카테고리에 따라 스타일 변경
                if ("Others".equals(category)) {
                    previewResultLabel.setStyle("-fx-text-fill: #ff6b6b;");
                } else {
                    previewResultLabel.setStyle("-fx-text-fill: #51cf66;");
                }
            } else {
                previewResultLabel.setText("커스텀 규칙이 비활성화되었습니다");
                previewResultLabel.setStyle("-fx-text-fill: #868e96;");
            }
        } else {
            previewResultLabel.setText("파일명을 입력하세요");
            previewResultLabel.setStyle("-fx-text-fill: #868e96;");
        }
    }

    /**
     * 규칙 다이얼로그 표시
     */
    private FileRule showRuleDialog(FileRule editingRule) {
        try {
            // FXML 로드
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rule-dialog.fxml"));
            Parent root = loader.load();
            
            // 컨트롤러 설정
            RuleDialogController dialogController = loader.getController();
            
            // 다이얼로그 스테이지 생성
            Stage dialogStage = new Stage();
            dialogStage.setTitle(editingRule == null ? "새 규칙 추가" : "규칙 수정");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(settingsStage);
            dialogStage.setResizable(false);
            
            // 씬 설정
            Scene scene = new Scene(root);
            scene.getStylesheets().add("/css/styles.css");
            dialogStage.setScene(scene);
            
            // 컨트롤러 초기화
            dialogController.setDialogStage(dialogStage);
            dialogController.setRulesManager(customRulesManager);
            
            if (editingRule == null) {
                dialogController.setAddMode();
            } else {
                dialogController.setEditMode(editingRule);
            }
            
            // 다이얼로그 표시
            dialogStage.showAndWait();
            
            // 결과 반환
            if (dialogController.isSaveClicked()) {
                return dialogController.getRule();
            } else {
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR] 규칙 다이얼로그 로드 실패: " + e.getMessage());
            e.printStackTrace();
            showAlert("오류", "다이얼로그를 열 수 없습니다: " + e.getMessage(), Alert.AlertType.ERROR);
            return null;
        }
    }

    /**
     * Alert 표시 헬퍼 메서드
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}