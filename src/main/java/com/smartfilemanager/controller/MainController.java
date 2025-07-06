package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.*;
import com.smartfilemanager.ui.AboutDialog;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.HelpDialog;
import com.smartfilemanager.ui.UIFactory;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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

    // 서비스들
    private FileScanService fileScanService;
    private FileOrganizerService fileOrganizerService;
    private UndoService undoService;
    private DuplicateDetectorService duplicateDetectorService;
    private CleanupDetectorService cleanupDetectorService;

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
        fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        fileOrganizerService = new FileOrganizerService(progressBar, statusLabel, progressLabel);
        undoService = new UndoService(progressBar, statusLabel, progressLabel);
        duplicateDetectorService = new DuplicateDetectorService();
        cleanupDetectorService = new CleanupDetectorService();
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
            UIFactory.showInfoDialog("📋 정리할 파일 없음",
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
            UIFactory.showInfoDialog("↩️ 되돌릴 파일 없음",
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
            UIFactory.showInfoDialog("📋 파일 없음",
                    "먼저 폴더를 스캔해서 중복 파일을 찾아주세요.");
            return;
        }

        startDuplicateDetection();
    }

    @FXML
    private void handleCleanupFiles() {
        System.out.println("[INFO] 불필요한 파일 정리 버튼 클릭");

        if (fileList.isEmpty()) {
            UIFactory.showInfoDialog("📋 파일 없음",
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
            UIFactory.showInfoDialog("❌ 오류", "정보 창을 표시할 수 없습니다.");
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
            UIFactory.showInfoDialog("❌ 오류", "도움말 창을 표시할 수 없습니다.");
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
            UIFactory.showInfoDialog("❌ 오류",
                    "설정 창을 열 수 없습니다:\n" + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] 설정 창에서 예기치 않은 오류: " + e.getMessage());
            UIFactory.showInfoDialog("❌ 오류",
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

    @FXML
    private void handleExit() {
        System.out.println("[INFO] 애플리케이션 종료");
        Platform.exit();
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

            UIFactory.showInfoDialog("🎉 정리 완료", resultMessage);
        });

        organizeTask.setOnFailed(e -> {
            Throwable exception = organizeTask.getException();
            UIFactory.showInfoDialog("❌ 정리 실패",
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

            UIFactory.showInfoDialog("🎉 되돌리기 완료", resultMessage);
        });

        undoTask.setOnFailed(e -> {
            Throwable exception = undoTask.getException();
            UIFactory.showInfoDialog("❌ 되돌리기 실패",
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
                        UIFactory.showInfoDialog("❌ 분석 실패",
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
            UIFactory.showInfoDialog("🎉 중복 파일 없음",
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

        UIFactory.showInfoDialog("🔄 중복 파일 발견!", message.toString());
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
                        UIFactory.showInfoDialog("❌ 분석 실패",
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
            UIFactory.showInfoDialog("🎉 불필요한 파일 없음",
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

        UIFactory.showInfoDialog("🧹 불필요한 파일 발견!", message.toString());
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

}