package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.DuplicateGroup;
import com.smartfilemanager.service.CleanupDetectorService;
import com.smartfilemanager.service.FileScanService;
import com.smartfilemanager.service.DuplicateDetectorService;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.UIFactory;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Event handlers with duplicate detection
 */
public class EventHandlers {

    private final Stage primaryStage;
    private final ObservableList<FileInfo> fileList;

    // UI components
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Button organizeButton;
    private TableView<FileInfo> fileTable;

    // Services
    private FileScanService fileScanService;
    private FileDetailManager fileDetailManager;
    private DuplicateDetectorService duplicateDetectorService;
    private CleanupDetectorService cleanupDetectorService;

    public EventHandlers(Stage primaryStage, ObservableList<FileInfo> fileList) {
        this.primaryStage = primaryStage;
        this.fileList = fileList;
        this.duplicateDetectorService = new DuplicateDetectorService();
        this.cleanupDetectorService = new CleanupDetectorService();
    }

    /**
     * Set UI components (called after initialization)
     */
    public void setUIComponents(ProgressBar progressBar, Label statusLabel, Label progressLabel,
                                Button organizeButton, TableView<FileInfo> fileTable, VBox detailPanel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.organizeButton = organizeButton;
        this.fileTable = fileTable;

        // Initialize services
        initializeServices(detailPanel);

        // Setup table selection listener
        setupTableSelectionListener();
    }

    /**
     * Initialize services
     */
    private void initializeServices(VBox detailPanel) {
        if (progressBar == null || statusLabel == null || progressLabel == null) {
            throw new IllegalStateException("UI components must be set before initializing services");
        }

        this.fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        this.fileDetailManager = new FileDetailManager(detailPanel);
    }

    /**
     * Setup table selection event
     */
    private void setupTableSelectionListener() {
        if (fileTable != null) {
            fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                fileDetailManager.updateFileDetails(newSelection);
            });
        }
    }

    /**
     * Open folder handler
     */
    public void handleOpenFolder() {
        System.out.println("[INFO] Open folder button clicked");

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder to organize");

        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            System.out.println("[INFO] Selected folder: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory);
        } else {
            System.out.println("[INFO] Folder selection cancelled");
        }
    }

    /**
     * Start file scan
     */
    private void startFileScan(File directory) {
        if (fileTable != null) {
            fileTable.getSelectionModel().clearSelection();
        }
        fileDetailManager.hideDetails();

        if (organizeButton != null) {
            organizeButton.setDisable(true);
        }

        fileScanService.startFileScan(directory);

        fileList.addListener((javafx.collections.ListChangeListener<FileInfo>) change -> {
            if (organizeButton != null) {
                boolean hasProcessableFiles = fileList.stream().anyMatch(file -> file.getStatus().isProcessable());
                organizeButton.setDisable(!hasProcessableFiles);
            }
        });
    }

    /**
     * Settings handler
     */
    public void handleSettings() {
        System.out.println("[INFO] Settings button clicked");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent settingsRoot = loader.load();

            SettingsController settingsController = loader.getController();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("⚙️ Smart File Manager - 설정");
            settingsStage.setScene(new Scene(settingsRoot, 800, 600));
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(primaryStage);
            settingsStage.setResizable(true);
            settingsStage.setMinWidth(700);
            settingsStage.setMinHeight(500);

            settingsController.setStage(settingsStage);
            settingsStage.showAndWait();

            System.out.println("[INFO] Settings window closed");

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load settings window: " + e.getMessage());
            UIFactory.showInfoDialog("❌ 오류",
                    "설정 창을 열 수 없습니다:\n" + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error in settings: " + e.getMessage());
            UIFactory.showInfoDialog("❌ 오류",
                    "설정 창에서 오류가 발생했습니다:\n" + e.getMessage());
        }
    }

    /**
     * File scan handler (for scan button)
     */
    public void handleScanFiles() {
        System.out.println("[INFO] File scan button clicked");
        handleOpenFolder();
    }

    /**
     * File organization handler
     */
    public void handleOrganizeFiles() {
        System.out.println("[INFO] File organize button clicked");

        List<FileInfo> filesToOrganize = fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .collect(java.util.stream.Collectors.toList());

        if (filesToOrganize.isEmpty()) {
            UIFactory.showInfoDialog("📋 정리할 파일 없음",
                    "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.");
            return;
        }

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

        boolean confirmed = UIFactory.showConfirmDialog("📦 파일 정리", message.toString());

        if (confirmed) {
            startFileOrganization(filesToOrganize);
        }
    }

    /**
     * Start file organization process
     */
    private void startFileOrganization(List<FileInfo> filesToOrganize) {
        String targetRootPath = selectOrganizationFolder();

        if (targetRootPath == null) {
            System.out.println("[INFO] User cancelled organization folder selection");
            return;
        }

        System.out.println("[INFO] Organization target folder: " + targetRootPath);

        com.smartfilemanager.service.FileOrganizerService organizerService =
                new com.smartfilemanager.service.FileOrganizerService(progressBar, statusLabel, progressLabel);

        javafx.concurrent.Task<Integer> organizeTask = organizerService.organizeFilesAsync(filesToOrganize, targetRootPath);

        organizeTask.setOnSucceeded(e -> {
            Integer successCount = organizeTask.getValue();

            if (fileTable != null) {
                fileTable.refresh();
            }

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
     * Select organization folder
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

        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            return selectedDirectory.getAbsolutePath() + File.separator + "SmartFileManager_Organized";
        }

        return null;
    }

    /**
     * Find duplicates handler - ENHANCED VERSION!
     */
    public void handleFindDuplicates() {
        System.out.println("[INFO] Find duplicates button clicked");

        if (fileList.isEmpty()) {
            UIFactory.showInfoDialog("📋 파일 없음",
                    "먼저 폴더를 스캔해서 중복 파일을 찾아주세요.");
            return;
        }

        // 중복 탐지 시작 알림
        statusLabel.setText("🔍 중복 파일을 분석하고 있습니다...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // 불확정 진행률

        // 백그라운드에서 중복 파일 탐지 실행
        Task<List<DuplicateGroup>> duplicateTask = new Task<List<DuplicateGroup>>() {
            @Override
            protected List<DuplicateGroup> call() throws Exception {
                // 현재 파일 목록의 복사본을 만들어서 분석
                List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                return duplicateDetectorService.findDuplicates(filesToAnalyze);
            }

            @Override
            protected void succeeded() {
                List<DuplicateGroup> duplicateGroups = getValue();

                // UI 업데이트
                progressBar.setVisible(false);
                statusLabel.setText("🔍 중복 파일 분석 완료");

                // 결과 표시
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

        // 백그라운드 스레드에서 실행
        Thread duplicateThread = new Thread(duplicateTask);
        duplicateThread.setDaemon(true);
        duplicateThread.start();
    }

    /**
     * Show duplicate detection results
     */
    private void showDuplicateResults(List<DuplicateGroup> duplicateGroups) {
        if (duplicateGroups.isEmpty()) {
            UIFactory.showInfoDialog("🎉 중복 파일 없음",
                    "🔍 분석 결과 중복된 파일을 찾지 못했습니다.\n\n" +
                            "✅ 모든 파일이 고유한 파일입니다!\n" +
                            "📊 분석된 파일: " + fileList.size() + "개");
            return;
        }

        // 통계 계산
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

        // 결과 메시지 생성
        StringBuilder message = new StringBuilder();
        message.append("🔄 중복 파일 분석 결과\n\n");

        message.append("📊 발견된 중복 그룹: ").append(duplicateGroups.size()).append("개\n");
        message.append("  • 🎯 정확한 중복: ").append(exactDuplicates).append("개 그룹\n");
        message.append("  • 🔍 유사한 파일: ").append(similarFiles).append("개 그룹\n\n");

        message.append("📁 중복 파일 개수: ").append(totalDuplicateFiles).append("개\n");
        message.append("💾 절약 가능 용량: ").append(formatFileSize(totalSavings)).append("\n\n");

        message.append("🎯 상위 중복 그룹:\n");
        duplicateGroups.stream()
                .sorted((g1, g2) -> Long.compare(g2.getDuplicateSize(), g1.getDuplicateSize()))
                .limit(5)
                .forEach(group -> {
                    message.append("  • ").append(group.getSummary()).append("\n");
                    if (group.hasRecommendation()) {
                        message.append("    💡 추천: ").append(group.getRecommendedKeep().getFileName())
                                .append(" (").append(group.getRecommendationReason()).append(")\n");
                    }
                });

        if (duplicateGroups.size() > 5) {
            message.append("  ... 그 외 ").append(duplicateGroups.size() - 5).append("개 그룹\n");
        }

        message.append("\n🚀 향후 버전에서는 중복 파일 관리 UI가 제공될 예정입니다!");

        UIFactory.showInfoDialog("🔄 중복 파일 발견!", message.toString());
    }

    /**
     * About handler
     */
    public void handleAbout() {
        StringBuilder about = new StringBuilder();
        about.append("🗂️ Smart File Manager v1.0\n");
        about.append("🤖 AI 기반 스마트 파일 정리 도구\n\n");

        about.append("📊 현재 통계:\n");
        if (!fileList.isEmpty()) {
            long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
            about.append("  📋 분석된 파일: ").append(fileList.size()).append("개\n");
            about.append("  📏 총 크기: ").append(formatFileSize(totalSize)).append("\n");

            long organizedCount = fileList.stream().filter(f -> f.getStatus().isCompleted()).count();
            about.append("  🎯 정리된 파일: ").append(organizedCount).append("개\n");
        } else {
            about.append("  📝 아직 분석된 파일이 없습니다\n");
        }

        about.append("\n🛠️ JavaFX로 제작\n");
        about.append("© 2024 Smart File Manager");

        UIFactory.showInfoDialog("ℹ️ Smart File Manager 정보", about.toString());
    }

    /**
     * Help topics handler
     */
    public void handleHelpTopics() {
        StringBuilder help = new StringBuilder();
        help.append("📖 Smart File Manager 도움말\n\n");

        help.append("🚀 빠른 시작:\n");
        help.append("  1. 🔍 'Scan Folder'를 클릭해서 파일 분석\n");
        help.append("  2. 📋 파일 목록과 카테고리 확인\n");
        help.append("  3. 📦 'Organize Files'를 클릭해서 정리\n\n");

        help.append("💡 팁:\n");
        help.append("  • 🖱️ 파일에서 우클릭하면 상세 메뉴\n");
        help.append("  • 📊 컬럼 헤더 클릭으로 정렬\n");
        help.append("  • 📄 파일 선택하면 상세 정보 표시\n");
        help.append("  • ⚙️ 설정에서 분류 규칙 커스터마이징\n");
        help.append("  • ↩️ Ctrl+Z로 정리 작업 되돌리기\n");
        help.append("  • 🔄 F7로 중복 파일 찾기\n");
        help.append("  • 🧹 F8로 불필요한 파일 정리\n\n");

        help.append("⌨️ 키보드 단축키:\n");
        help.append("  • Ctrl+O: 📁 폴더 열기\n");
        help.append("  • F5: 🔍 파일 스캔\n");
        help.append("  • F6: 📦 파일 정리\n");
        help.append("  • F7: 🔄 중복 파일 찾기\n");
        help.append("  • F8: 🧹 불필요한 파일 정리\n");
        help.append("  • Ctrl+Z: ↩️ 정리 되돌리기\n");
        help.append("  • Ctrl+,: ⚙️ 설정 열기\n");
        help.append("  • Ctrl+Q: 🚪 종료\n\n");

        help.append("🆘 문제 해결:\n");
        help.append("  • 파일이 정리되지 않을 때: 권한 확인\n");
        help.append("  • 스캔이 느릴 때: 설정에서 파일 크기 제한 조정\n");
        help.append("  • 분류가 부정확할 때: 파일명에 키워드 포함\n");
        help.append("  • 중복 탐지가 느릴 때: 파일 수가 많은 경우 시간이 소요됨\n");

        UIFactory.showInfoDialog("📖 도움말", help.toString());
    }

    /**
     * Undo organization handler
     */
    public void handleUndoOrganization() {
        System.out.println("[INFO] Undo organization button clicked");

        List<FileInfo> undoableFiles = com.smartfilemanager.service.UndoService.getUndoableFiles(
                new ArrayList<>(fileList)
        );

        if (undoableFiles.isEmpty()) {
            UIFactory.showInfoDialog("↩️ 되돌릴 파일 없음",
                    "정리된 파일이 없습니다.\n" +
                            "파일을 먼저 정리한 후 되돌리기를 사용할 수 있습니다.");
            return;
        }

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

        boolean confirmed = UIFactory.showConfirmDialog("↩️ 파일 되돌리기", message.toString());

        if (confirmed) {
            startUndoProcess(undoableFiles);
        }
    }

    /**
     * Start undo process
     */
    private void startUndoProcess(List<FileInfo> undoableFiles) {
        com.smartfilemanager.service.UndoService undoService =
                new com.smartfilemanager.service.UndoService(progressBar, statusLabel, progressLabel);

        javafx.concurrent.Task<Integer> undoTask = undoService.undoOrganizationAsync(undoableFiles);

        undoTask.setOnSucceeded(e -> {
            Integer successCount = undoTask.getValue();

            if (fileTable != null) {
                fileTable.refresh();
            }

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
     * Cleanup files handler - NEW FEATURE!
     */
    public void handleCleanupFiles() {
        System.out.println("[INFO] Cleanup files button clicked");

        if (fileList.isEmpty()) {
            UIFactory.showInfoDialog("📋 파일 없음",
                    "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.");
            return;
        }

        // 불필요한 파일 탐지 시작 알림
        statusLabel.setText("🧹 불필요한 파일을 분석하고 있습니다...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // 불확정 진행률

        // 백그라운드에서 불필요한 파일 탐지 실행
        Task<List<com.smartfilemanager.model.CleanupCandidate>> cleanupTask =
                new Task<List<com.smartfilemanager.model.CleanupCandidate>>() {
                    @Override
                    protected List<com.smartfilemanager.model.CleanupCandidate> call() throws Exception {
                        // 현재 파일 목록의 복사본을 만들어서 분석
                        List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                        return cleanupDetectorService.findCleanupCandidates(filesToAnalyze);
                    }

                    @Override
                    protected void succeeded() {
                        List<com.smartfilemanager.model.CleanupCandidate> cleanupCandidates = getValue();

                        // UI 업데이트
                        progressBar.setVisible(false);
                        statusLabel.setText("🧹 불필요한 파일 분석 완료");

                        // 결과 표시
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

        // 백그라운드 스레드에서 실행
        Thread cleanupThread = new Thread(cleanupTask);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * Show cleanup detection results
     */
    private void showCleanupResults(List<com.smartfilemanager.model.CleanupCandidate> candidates) {
        if (candidates.isEmpty()) {
            UIFactory.showInfoDialog("🎉 불필요한 파일 없음",
                    "🧹 분석 결과 정리할 불필요한 파일을 찾지 못했습니다.\n\n" +
                            "✅ 시스템이 이미 깔끔한 상태입니다!\n" +
                            "📊 분석된 파일: " + fileList.size() + "개");
            return;
        }

        // 안전성 등급별 통계 계산
        Map<com.smartfilemanager.model.SafetyLevel, List<com.smartfilemanager.model.CleanupCandidate>>
                safetyGroups = candidates.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.smartfilemanager.model.CleanupCandidate::getSafetyLevel));

        // 카테고리별 통계 계산
        Map<com.smartfilemanager.model.CleanupCategory, List<com.smartfilemanager.model.CleanupCandidate>>
                categoryGroups = candidates.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.smartfilemanager.model.CleanupCandidate::getCategory));

        long totalFiles = candidates.size();
        long totalSize = candidates.stream()
                .mapToLong(com.smartfilemanager.model.CleanupCandidate::getFileSize)
                .sum();

        long safeSize = safetyGroups.getOrDefault(
                        com.smartfilemanager.model.SafetyLevel.SAFE, java.util.Collections.emptyList())
                .stream().mapToLong(com.smartfilemanager.model.CleanupCandidate::getFileSize).sum();

        long likelySafeSize = safetyGroups.getOrDefault(
                        com.smartfilemanager.model.SafetyLevel.LIKELY_SAFE, java.util.Collections.emptyList())
                .stream().mapToLong(com.smartfilemanager.model.CleanupCandidate::getFileSize).sum();

        // 결과 메시지 생성
        StringBuilder message = new StringBuilder();
        message.append("🧹 불필요한 파일 분석 결과\n\n");

        message.append("📊 발견된 정리 후보: ").append(totalFiles).append("개 파일\n");
        message.append("💾 총 절약 가능 용량: ").append(formatFileSize(totalSize)).append("\n\n");

        message.append("🛡️ 안전성 등급별 분류:\n");
        message.append("  ✅ 안전한 삭제: ").append(formatFileSize(safeSize)).append("\n");
        message.append("  🟢 삭제 권장: ").append(formatFileSize(likelySafeSize)).append("\n");
        message.append("  ⚠️ 주의 필요: ").append(formatFileSize(totalSize - safeSize - likelySafeSize)).append("\n\n");

        message.append("📂 카테고리별 분석:\n");
        categoryGroups.entrySet().stream()
                .sorted(Map.Entry.<com.smartfilemanager.model.CleanupCategory,
                        List<com.smartfilemanager.model.CleanupCandidate>>comparingByKey(
                        (c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority())))
                .limit(6) // 상위 6개 카테고리만 표시
                .forEach(entry -> {
                    com.smartfilemanager.model.CleanupCategory category = entry.getKey();
                    List<com.smartfilemanager.model.CleanupCandidate> files = entry.getValue();
                    long categorySize = files.stream().mapToLong(
                            com.smartfilemanager.model.CleanupCandidate::getFileSize).sum();

                    message.append("  ").append(category.getCategoryIcon()).append(" ")
                            .append(category.getDisplayName()).append(": ")
                            .append(files.size()).append("개 (")
                            .append(formatFileSize(categorySize)).append(")\n");
                });

        if (categoryGroups.size() > 6) {
            message.append("  ... 그 외 ").append(categoryGroups.size() - 6).append("개 카테고리\n");
        }

        message.append("\n🎯 상위 정리 후보:\n");
        candidates.stream()
                .sorted((c1, c2) -> Long.compare(c2.getFileSize(), c1.getFileSize()))
                .limit(5)
                .forEach(candidate -> {
                    message.append("  ").append(candidate.getSafetyIcon()).append(" ")
                            .append(candidate.getFileName()).append(" (")
                            .append(candidate.getFormattedFileSize()).append(") - ")
                            .append(candidate.getReason()).append("\n");
                });

        if (candidates.size() > 5) {
            message.append("  ... 그 외 ").append(candidates.size() - 5).append("개 파일\n");
        }

        message.append("\n🚀 향후 버전에서는 실제 파일 삭제 기능이 제공될 예정입니다!");

        UIFactory.showInfoDialog("🧹 불필요한 파일 발견!", message.toString());
    }

    /**
     * File size formatting utility
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Get file detail manager for testing
     */
    public FileDetailManager getFileDetailManager() {
        return fileDetailManager;
    }
}