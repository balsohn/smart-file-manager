package com.smartfilemanager.manager;

import com.smartfilemanager.constants.FileConstants;
import com.smartfilemanager.constants.MessageConstants;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.CleanupCandidate;
import com.smartfilemanager.model.DuplicateGroup;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.*;
import com.smartfilemanager.util.TaskUtils;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 파일 작업(스캔, 정리, 중복 검사 등) 처리를 관리하는 클래스
 */
public class FileOperationHandler {
    
    private final FileScanService fileScanService;
    private final FileOrganizerService fileOrganizerService;
    private final UndoService undoService;
    private final DuplicateDetectorService duplicateDetectorService;
    private final CleanupDetectorService cleanupDetectorService;
    private final FileAnalysisService fileAnalysisService;
    private final FileWatcherService fileWatcherService;
    private final UIUpdateManager uiUpdateManager;
    private final DialogManager dialogManager;
    private final java.util.function.Supplier<Stage> stageSupplier;
    
    private boolean isMonitoringActive = false;
    
    public FileOperationHandler(FileScanService fileScanService,
                               FileOrganizerService fileOrganizerService,
                               UndoService undoService,
                               DuplicateDetectorService duplicateDetectorService,
                               CleanupDetectorService cleanupDetectorService,
                               FileAnalysisService fileAnalysisService,
                               FileWatcherService fileWatcherService,
                               UIUpdateManager uiUpdateManager,
                               DialogManager dialogManager,
                               java.util.function.Supplier<Stage> stageSupplier) {
        this.fileScanService = fileScanService;
        this.fileOrganizerService = fileOrganizerService;
        this.undoService = undoService;
        this.duplicateDetectorService = duplicateDetectorService;
        this.cleanupDetectorService = cleanupDetectorService;
        this.fileAnalysisService = fileAnalysisService;
        this.fileWatcherService = fileWatcherService;
        this.uiUpdateManager = uiUpdateManager;
        this.dialogManager = dialogManager;
        this.stageSupplier = stageSupplier;
    }
    
    /**
     * 폴더 선택 및 파일 스캔 시작
     */
    public void handleOpenFolder(List<FileInfo> fileList) {
        File selectedDirectory = selectDirectory(MessageConstants.DialogTitles.SELECT_FOLDER, 
                                                FileConstants.DefaultFolders.DOWNLOADS);
        if (selectedDirectory != null) {
            startFileScan(selectedDirectory, fileList);
        }
    }
    
    /**
     * 파일 정리 처리
     */
    public void handleOrganizeFiles(List<FileInfo> fileList) {
        List<FileInfo> filesToOrganize = getProcessableFiles(fileList);
        if (filesToOrganize.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_ORGANIZE_FILES, 
                                       MessageConstants.DialogMessages.SCAN_FIRST);
            return;
        }
        
        if (dialogManager.showOrganizeConfirmDialog(filesToOrganize)) {
            startFileOrganization(filesToOrganize);
        }
    }
    
    /**
     * 파일 되돌리기 처리
     */
    public void handleUndoOrganization(List<FileInfo> fileList) {
        List<FileInfo> undoableFiles = UndoService.getUndoableFiles(new ArrayList<>(fileList));
        if (undoableFiles.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_UNDO_FILES, 
                                       MessageConstants.DialogMessages.NO_ORGANIZED_FILES);
            return;
        }
        
        if (dialogManager.showUndoConfirmDialog(undoableFiles)) {
            startUndoProcess(undoableFiles);
        }
    }
    
    /**
     * 중복 파일 검사 처리
     */
    public void handleFindDuplicates(List<FileInfo> fileList) {
        if (fileList.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_FILES, 
                                       MessageConstants.DialogMessages.SCAN_FILES_FIRST);
            return;
        }
        startDuplicateDetection(fileList);
    }
    
    /**
     * 불필요한 파일 검사 처리
     */
    public void handleCleanupFiles(List<FileInfo> fileList) {
        if (fileList.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_FILES, 
                                       MessageConstants.DialogMessages.SCAN_FILES_FIRST);
            return;
        }
        startCleanupDetection(fileList);
    }
    
    /**
     * 모니터링 시작
     */
    public void startMonitoring() {
        File selectedFolder = selectDirectory(MessageConstants.DialogTitles.SELECT_MONITORING_FOLDER, 
                                            System.getProperty("user.home"));
        if (selectedFolder == null) return;
        
        if (fileWatcherService.startWatching(selectedFolder.getAbsolutePath())) {
            isMonitoringActive = true;
            uiUpdateManager.updateMonitoringUI(true);
            uiUpdateManager.updateMonitoringFolder(selectedFolder.getAbsolutePath());
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.MONITORING_START, 
                                       MessageConstants.DialogMessages.MONITORING_STARTED);
        }
    }
    
    /**
     * 모니터링 중지
     */
    public void stopMonitoring() {
        if (fileWatcherService != null) {
            fileWatcherService.stopWatching();
        }
        isMonitoringActive = false;
        uiUpdateManager.updateMonitoringUI(false);
        uiUpdateManager.hideMonitoringInfo();
    }
    
    /**
     * AI 배치 분석 처리
     */
    public void performBatchAIAnalysis(List<FileInfo> fileList) {
        if (!fileAnalysisService.isAIAnalysisAvailable()) {
            dialogManager.showWarningDialog(MessageConstants.DialogTitles.AI_UNAVAILABLE, 
                                          MessageConstants.DialogMessages.AI_NOT_ENABLED);
            return;
        }
        
        List<FileInfo> analyzedFiles = getAnalyzedFiles(fileList);
        if (analyzedFiles.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_AI_FILES, 
                                       MessageConstants.DialogMessages.NO_AI_REANALYSIS);
            return;
        }
        
        if (dialogManager.showAIAnalysisConfirmDialog(analyzedFiles.size())) {
            executeBatchAIAnalysis(analyzedFiles);
        }
    }
    
    /**
     * 모니터링 상태 반환
     */
    public boolean isMonitoringActive() {
        return isMonitoringActive;
    }
    
    /**
     * 파일 워처 서비스 종료
     */
    public void shutdown() {
        if (fileWatcherService != null) {
            fileWatcherService.shutdown();
        }
    }
    
    // Private helper methods
    
    private File selectDirectory(String title, String defaultPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        
        String path = defaultPath != null ? defaultPath : 
                     System.getProperty("user.home") + File.separator + FileConstants.DefaultFolders.DOWNLOADS;
        File defaultDir = new File(path);
        if (defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }
        
        Stage stage = stageSupplier.get();
        return chooser.showDialog(stage);
    }
    
    private void startFileScan(File directory, List<FileInfo> fileList) {
        uiUpdateManager.updateStatusLabel(MessageConstants.StatusMessages.SCANNING_FILES);
        fileScanService.startFileScan(directory);
    }
    
    private void startFileOrganization(List<FileInfo> filesToOrganize) {
        String targetRootPath = selectOrganizationFolder();
        if (targetRootPath == null) return;
        
        Task<Integer> organizeTask = fileOrganizerService.organizeFilesAsync(filesToOrganize, targetRootPath);
        setupTaskHandlers(organizeTask, "정리", filesToOrganize.size());
    }
    
    private void startUndoProcess(List<FileInfo> undoableFiles) {
        Task<Integer> undoTask = undoService.undoOrganizationAsync(undoableFiles);
        setupUndoTaskHandlers(undoTask, undoableFiles.size());
    }
    
    private void startDuplicateDetection(List<FileInfo> fileList) {
        uiUpdateManager.updateStatusLabel(MessageConstants.StatusMessages.ANALYZING_DUPLICATES);
        uiUpdateManager.updateProgress(-1, "중복 파일 분석 중...");
        
        Task<List<DuplicateGroup>> duplicateTask = createDuplicateTask(fileList);
        setupDuplicateTaskHandlers(duplicateTask);
        TaskUtils.runTaskAsync(duplicateTask);
    }
    
    private void startCleanupDetection(List<FileInfo> fileList) {
        uiUpdateManager.updateStatusLabel(MessageConstants.StatusMessages.ANALYZING_CLEANUP);
        uiUpdateManager.updateProgress(-1, "불필요한 파일 분석 중...");
        
        Task<List<CleanupCandidate>> cleanupTask = createCleanupTask(fileList);
        setupCleanupTaskHandlers(cleanupTask);
        TaskUtils.runTaskAsync(cleanupTask);
    }
    
    private void executeBatchAIAnalysis(List<FileInfo> files) {
        uiUpdateManager.updateAIAnalysisButton(true);
        uiUpdateManager.updateStatusLabel(MessageConstants.StatusMessages.AI_BATCH_ANALYSIS_START);
        
        Task<Integer> batchAnalysisTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                // AI 분석 로직 구현 필요
                return 0;
            }
            
            @Override
            protected void succeeded() {
                uiUpdateManager.updateStatusLabel(MessageConstants.StatusMessages.AI_BATCH_ANALYSIS_COMPLETE);
                uiUpdateManager.updateAIAnalysisButton(false);
                uiUpdateManager.updateUI();
            }
            
            @Override
            protected void failed() {
                uiUpdateManager.updateStatusLabel("AI 배치 분석 실패");
                uiUpdateManager.updateAIAnalysisButton(false);
                dialogManager.showErrorDialog(MessageConstants.DialogTitles.ERROR, 
                                            "AI 배치 분석 중 오류가 발생했습니다");
            }
        };
        
        TaskUtils.runTaskAsync(batchAnalysisTask);
    }
    
    private String selectOrganizationFolder() {
        File selectedDirectory = selectDirectory(MessageConstants.DialogTitles.SELECT_TARGET_FOLDER, 
                                                System.getProperty("user.home") + File.separator + FileConstants.DefaultFolders.DESKTOP);
        return selectedDirectory != null ? 
               selectedDirectory.getAbsolutePath() + File.separator + FileConstants.DefaultFolders.ORGANIZED_FOLDER_SUFFIX : null;
    }
    
    private List<FileInfo> getProcessableFiles(List<FileInfo> fileList) {
        return fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .collect(Collectors.toList());
    }
    
    private List<FileInfo> getAnalyzedFiles(List<FileInfo> fileList) {
        return fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ANALYZED)
                .collect(Collectors.toList());
    }
    
    private Task<List<DuplicateGroup>> createDuplicateTask(List<FileInfo> fileList) {
        return new Task<List<DuplicateGroup>>() {
            @Override
            protected List<DuplicateGroup> call() throws Exception {
                List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                return duplicateDetectorService.findDuplicates(filesToAnalyze);
            }
        };
    }
    
    private Task<List<CleanupCandidate>> createCleanupTask(List<FileInfo> fileList) {
        return new Task<List<CleanupCandidate>>() {
            @Override
            protected List<CleanupCandidate> call() throws Exception {
                List<FileInfo> filesToAnalyze = new ArrayList<>(fileList);
                return cleanupDetectorService.findCleanupCandidates(filesToAnalyze);
            }
        };
    }
    
    private void setupTaskHandlers(Task<Integer> task, String operation, int fileCount) {
        task.setOnSucceeded(e -> {
            String resultMessage = TaskUtils.createSuccessMessage(operation, task.getValue());
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.COMPLETE, resultMessage);
            uiUpdateManager.updateUI();
        });
        
        task.setOnFailed(e -> {
            String errorMessage = TaskUtils.createErrorMessage(operation, task.getException());
            dialogManager.showErrorDialog(MessageConstants.DialogTitles.FAILED, errorMessage);
        });
        
        TaskUtils.runTaskAsync(task);
    }
    
    private void setupUndoTaskHandlers(Task<Integer> task, int fileCount) {
        task.setOnSucceeded(e -> {
            String resultMessage = TaskUtils.createSuccessMessage("파일 되돌리기", task.getValue());
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.COMPLETE, resultMessage);
            uiUpdateManager.updateUI();
        });
        
        task.setOnFailed(e -> {
            String errorMessage = TaskUtils.createErrorMessage("파일 되돌리기", task.getException());
            dialogManager.showErrorDialog(MessageConstants.DialogTitles.FAILED, errorMessage);
        });
        
        TaskUtils.runTaskAsync(task);
    }
    
    private void setupDuplicateTaskHandlers(Task<List<DuplicateGroup>> task) {
        task.setOnSucceeded(e -> {
            uiUpdateManager.updateProgress(0, "중복 파일 분석 완료");
            showDuplicateResults(task.getValue());
        });
        
        task.setOnFailed(e -> {
            uiUpdateManager.updateProgress(0, "중복 파일 분석 실패");
            dialogManager.showErrorDialog(MessageConstants.DialogTitles.FAILED, 
                                        TaskUtils.createErrorMessage("중복 파일 분석", task.getException()));
        });
    }
    
    private void setupCleanupTaskHandlers(Task<List<CleanupCandidate>> task) {
        task.setOnSucceeded(e -> {
            uiUpdateManager.updateProgress(0, "불필요한 파일 분석 완료");
            showCleanupResults(task.getValue());
        });
        
        task.setOnFailed(e -> {
            uiUpdateManager.updateProgress(0, "불필요한 파일 분석 실패");
            dialogManager.showErrorDialog(MessageConstants.DialogTitles.FAILED, 
                                        TaskUtils.createErrorMessage("불필요한 파일 분석", task.getException()));
        });
    }
    
    private void showDuplicateResults(List<DuplicateGroup> duplicateGroups) {
        if (duplicateGroups.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_DUPLICATES, 
                                       MessageConstants.DialogMessages.NO_DUPLICATES_FOUND);
        } else {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.DUPLICATES_FOUND, 
                                       String.format(MessageConstants.DialogMessages.DUPLICATES_RESULT, duplicateGroups.size()));
        }
    }
    
    private void showCleanupResults(List<CleanupCandidate> candidates) {
        if (candidates.isEmpty()) {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.NO_CLEANUP, 
                                       MessageConstants.DialogMessages.NO_CLEANUP_FOUND);
        } else {
            dialogManager.showInfoDialog(MessageConstants.DialogTitles.CLEANUP_FOUND, 
                                       String.format(MessageConstants.DialogMessages.CLEANUP_RESULT, candidates.size()));
        }
    }
}