package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileRule;
import com.smartfilemanager.service.CustomRulesManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 파일 정리 규칙 추가/수정 다이얼로그 컨트롤러
 * 사용자가 직관적으로 규칙을 생성하고 편집할 수 있는 GUI를 제공합니다
 */
public class RuleDialogController implements Initializable {

    // ===============================
    // 📋 FXML UI 컴포넌트들
    // ===============================

    @FXML private Label dialogTitleLabel;
    
    // 입력 필드들
    @FXML private TextField ruleNameField;
    @FXML private TextField extensionsField;
    @FXML private TextField targetFolderField;
    @FXML private Spinner<Integer> prioritySpinner;
    @FXML private TextArea descriptionArea;
    @FXML private CheckBox enabledCheckBox;
    
    // 버튼들
    @FXML private Button browseFolderButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    // 오류 라벨들
    @FXML private Label ruleNameErrorLabel;
    @FXML private Label extensionsErrorLabel;
    @FXML private Label targetFolderErrorLabel;
    
    // 미리보기 관련
    @FXML private TextField previewTestFileField;
    @FXML private Label previewResultLabel;

    // ===============================
    // 📝 클래스 필드들
    // ===============================

    private Stage dialogStage;
    private FileRule editingRule;  // 수정 모드일 때 사용
    private boolean saveClicked = false;
    private CustomRulesManager rulesManager;

    // ===============================
    // 🚀 초기화 메서드들
    // ===============================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpinner();
        setupValidation();
        setupKeyboardShortcuts();
        
        System.out.println("[INFO] 규칙 다이얼로그 초기화 완료");
    }

    /**
     * Spinner 설정
     */
    private void setupSpinner() {
        prioritySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 50));
        prioritySpinner.setEditable(true);
    }

    /**
     * 실시간 유효성 검증 설정
     */
    private void setupValidation() {
        // 규칙명 변경 시 검증
        ruleNameField.textProperty().addListener((obs, oldVal, newVal) -> validateRuleName());
        
        // 확장자 변경 시 검증
        extensionsField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateExtensions();
            updatePreview();
        });
        
        // 타겟 폴더 변경 시 검증
        targetFolderField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateTargetFolder();
            updatePreview();
        });
        
        // 우선순위 변경 시 미리보기 업데이트
        prioritySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        
        // 활성화 상태 변경 시 미리보기 업데이트
        enabledCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
    }

    /**
     * 키보드 단축키 설정
     */
    private void setupKeyboardShortcuts() {
        // Enter 키로 저장
        saveButton.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleSave();
            }
        });
        
        // Esc 키로 취소
        cancelButton.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ESCAPE")) {
                handleCancel();
            }
        });
    }

    // ===============================
    // 🎯 공개 메서드들 (외부에서 호출)
    // ===============================

    /**
     * 다이얼로그 스테이지 설정
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // ESC 키로 창 닫기
        dialogStage.getScene().setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ESCAPE")) {
                handleCancel();
            }
        });
    }

    /**
     * 규칙 매니저 설정
     */
    public void setRulesManager(CustomRulesManager rulesManager) {
        this.rulesManager = rulesManager;
    }

    /**
     * 새 규칙 추가 모드로 설정
     */
    public void setAddMode() {
        dialogTitleLabel.setText("📝 파일 정리 규칙 추가");
        editingRule = null;
        
        // 기본값 설정
        ruleNameField.setText("");
        extensionsField.setText("");
        targetFolderField.setText("");
        prioritySpinner.getValueFactory().setValue(50);
        descriptionArea.setText("");
        enabledCheckBox.setSelected(true);
        
        // 포커스 설정
        ruleNameField.requestFocus();
    }
    
    /**
     * 확장자를 미리 채워서 새 규칙 추가 모드로 설정
     */
    public void setPrefilledExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return;
        }
        
        // 확장자에서 점(.) 제거
        String cleanExtension = extension.startsWith(".") ? extension.substring(1) : extension;
        
        // 확장자 필드에 미리 채우기
        extensionsField.setText(cleanExtension.toLowerCase());
        
        // 규칙명 자동 제안
        String suggestedName = generateRuleName(cleanExtension);
        ruleNameField.setText(suggestedName);
        
        // 타겟 폴더 자동 제안
        String suggestedFolder = generateTargetFolder(cleanExtension);
        targetFolderField.setText(suggestedFolder);
        
        // 미리보기 업데이트
        updatePreview();
        
        // 타겟 폴더 필드에 포커스 (확장자는 이미 채워졌으므로)
        targetFolderField.requestFocus();
        targetFolderField.selectAll();
        
        System.out.println("[INFO] 확장자 미리 채움: " + cleanExtension + " → " + suggestedFolder);
    }
    
    /**
     * 확장자 기반 규칙명 자동 생성
     */
    private String generateRuleName(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return "이미지 파일";
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
            case "wmv":
                return "동영상 파일";
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                return "음악 파일";
            case "pdf":
            case "doc":
            case "docx":
                return "문서 파일";
            case "zip":
            case "rar":
            case "7z":
                return "압축 파일";
            case "exe":
            case "msi":
                return "실행 파일";
            default:
                return extension.toUpperCase() + " 파일";
        }
    }
    
    /**
     * 확장자 기반 타겟 폴더 자동 제안
     */
    private String generateTargetFolder(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
            case "heic":
                return "Images";
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
            case "wmv":
            case "flv":
                return "Videos";
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
                return "Audio";
            case "pdf":
            case "doc":
            case "docx":
            case "txt":
            case "rtf":
                return "Documents";
            case "xls":
            case "xlsx":
            case "csv":
                return "Spreadsheets";
            case "ppt":
            case "pptx":
                return "Presentations";
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "Archives";
            case "exe":
            case "msi":
            case "dmg":
            case "pkg":
                return "Applications";
            case "java":
            case "py":
            case "js":
            case "html":
            case "css":
            case "cpp":
            case "c":
                return "Code";
            default:
                return "Others";
        }
    }

    /**
     * 규칙 수정 모드로 설정
     */
    public void setEditMode(FileRule rule) {
        dialogTitleLabel.setText("✏️ 파일 정리 규칙 수정");
        editingRule = rule;
        
        // 기존 값 로드
        ruleNameField.setText(rule.getName());
        extensionsField.setText(String.join(", ", rule.getExtensions()));
        targetFolderField.setText(rule.getTargetFolder());
        prioritySpinner.getValueFactory().setValue(rule.getPriority());
        descriptionArea.setText(rule.getDescription() != null ? rule.getDescription() : "");
        enabledCheckBox.setSelected(rule.isEnabled());
        
        // 포커스 설정
        ruleNameField.requestFocus();
        ruleNameField.selectAll();
    }

    /**
     * 저장 버튼이 클릭되었는지 확인
     */
    public boolean isSaveClicked() {
        return saveClicked;
    }

    /**
     * 생성/수정된 규칙 반환
     */
    public FileRule getRule() {
        if (!saveClicked) return null;
        
        FileRule rule = editingRule != null ? editingRule : new FileRule();
        
        // ID 설정 (새 규칙일 때만)
        if (editingRule == null) {
            rule.setId(UUID.randomUUID().toString());
            rule.setCreatedDate(LocalDateTime.now().toString());
        }
        
        // 공통 필드 설정
        rule.setName(ruleNameField.getText().trim());
        rule.setExtensions(parseExtensions(extensionsField.getText()));
        rule.setTargetFolder(targetFolderField.getText().trim());
        rule.setPriority(prioritySpinner.getValue());
        rule.setDescription(descriptionArea.getText().trim());
        rule.setEnabled(enabledCheckBox.isSelected());
        rule.setModifiedDate(LocalDateTime.now().toString());
        
        return rule;
    }

    // ===============================
    // 🎯 FXML 이벤트 핸들러들
    // ===============================

    @FXML
    private void handleBrowseFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("정리할 폴더 선택");
        
        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            targetFolderField.setText(selectedDirectory.getName());
        }
    }

    @FXML
    private void handlePreviewUpdate(KeyEvent event) {
        updatePreview();
    }

    @FXML
    private void handleSave() {
        if (validateAllFields()) {
            saveClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        saveClicked = false;
        dialogStage.close();
    }

    // ===============================
    // 🔍 유효성 검증 메서드들
    // ===============================

    /**
     * 모든 필드 유효성 검증
     */
    private boolean validateAllFields() {
        boolean isValid = true;
        
        isValid &= validateRuleName();
        isValid &= validateExtensions();
        isValid &= validateTargetFolder();
        
        if (!isValid) {
            showAlert("입력 오류", "빨간색으로 표시된 필드들을 확인해주세요.", Alert.AlertType.WARNING);
        }
        
        return isValid;
    }

    /**
     * 규칙명 검증
     */
    private boolean validateRuleName() {
        String name = ruleNameField.getText().trim();
        
        if (name.isEmpty()) {
            showFieldError(ruleNameErrorLabel, "규칙명을 입력해주세요.");
            return false;
        }
        
        if (name.length() < 2) {
            showFieldError(ruleNameErrorLabel, "규칙명은 2글자 이상 입력해주세요.");
            return false;
        }
        
        hideFieldError(ruleNameErrorLabel);
        return true;
    }

    /**
     * 확장자 검증
     */
    private boolean validateExtensions() {
        String extensions = extensionsField.getText().trim();
        
        if (extensions.isEmpty()) {
            showFieldError(extensionsErrorLabel, "확장자를 입력해주세요.");
            return false;
        }
        
        List<String> extList = parseExtensions(extensions);
        if (extList.isEmpty()) {
            showFieldError(extensionsErrorLabel, "올바른 확장자를 입력해주세요.");
            return false;
        }
        
        // 확장자 형식 검증
        for (String ext : extList) {
            if (!ext.matches("^[a-zA-Z0-9]+$")) {
                showFieldError(extensionsErrorLabel, "확장자는 영문과 숫자만 사용할 수 있습니다: " + ext);
                return false;
            }
        }
        
        // 중복 확장자 검증 (다른 규칙과의 충돌)
        if (rulesManager != null) {
            String conflictMessage = checkExtensionConflicts(extList);
            if (conflictMessage != null) {
                showFieldError(extensionsErrorLabel, conflictMessage);
                return false;
            }
        }
        
        hideFieldError(extensionsErrorLabel);
        return true;
    }

    /**
     * 타겟 폴더 검증
     */
    private boolean validateTargetFolder() {
        String folder = targetFolderField.getText().trim();
        
        if (folder.isEmpty()) {
            showFieldError(targetFolderErrorLabel, "타겟 폴더를 입력해주세요.");
            return false;
        }
        
        // 폴더명 형식 검증 (기본적인 검증)
        if (folder.contains("..") || folder.startsWith("/") || folder.startsWith("\\")) {
            showFieldError(targetFolderErrorLabel, "올바른 폴더명을 입력해주세요.");
            return false;
        }
        
        hideFieldError(targetFolderErrorLabel);
        return true;
    }

    // ===============================
    // 🛠️ 유틸리티 메서드들
    // ===============================

    /**
     * 확장자 문자열 파싱
     */
    private List<String> parseExtensions(String extensionsText) {
        return Arrays.stream(extensionsText.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(ext -> !ext.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 확장자 충돌 검사
     */
    private String checkExtensionConflicts(List<String> extensions) {
        if (rulesManager == null) return null;
        
        for (String ext : extensions) {
            var conflictingRules = rulesManager.getAllRules().stream()
                    .filter(rule -> !rule.getId().equals(editingRule != null ? editingRule.getId() : ""))
                    .filter(rule -> rule.isEnabled())
                    .filter(rule -> rule.containsExtension(ext))
                    .collect(Collectors.toList());
            
            if (!conflictingRules.isEmpty()) {
                FileRule firstConflict = conflictingRules.get(0);
                return String.format("확장자 '%s'가 이미 '%s' 규칙에서 사용 중입니다.", ext, firstConflict.getName());
            }
        }
        
        return null;
    }

    /**
     * 미리보기 업데이트
     */
    private void updatePreview() {
        String testFileName = previewTestFileField.getText().trim();
        
        if (testFileName.isEmpty()) {
            previewResultLabel.setText("파일명을 입력하여 테스트해보세요");
            previewResultLabel.setStyle("-fx-text-fill: #868e96;");
            return;
        }
        
        if (!enabledCheckBox.isSelected()) {
            previewResultLabel.setText("규칙이 비활성화되어 적용되지 않습니다");
            previewResultLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }
        
        // 확장자 추출
        int lastDot = testFileName.lastIndexOf('.');
        if (lastDot == -1) {
            previewResultLabel.setText("확장자가 없는 파일입니다");
            previewResultLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }
        
        String extension = testFileName.substring(lastDot + 1).toLowerCase();
        List<String> ruleExtensions = parseExtensions(extensionsField.getText());
        
        if (ruleExtensions.contains(extension)) {
            String targetFolder = targetFolderField.getText().trim();
            if (!targetFolder.isEmpty()) {
                previewResultLabel.setText(targetFolder + " 폴더로 이동됩니다");
                previewResultLabel.setStyle("-fx-text-fill: #51cf66;");
            } else {
                previewResultLabel.setText("타겟 폴더를 입력해주세요");
                previewResultLabel.setStyle("-fx-text-fill: #ff6b6b;");
            }
        } else {
            previewResultLabel.setText("이 규칙에 해당하지 않습니다");
            previewResultLabel.setStyle("-fx-text-fill: #868e96;");
        }
    }

    /**
     * 필드 오류 표시
     */
    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #ff6b6b;");
    }

    /**
     * 필드 오류 숨기기
     */
    private void hideFieldError(Label errorLabel) {
        errorLabel.setVisible(false);
    }

    /**
     * Alert 표시 헬퍼 메서드
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }
}