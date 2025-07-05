package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.service.FileScanService;
import com.smartfilemanager.ui.UIFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * 이벤트 핸들러들을 모아놓은 클래스
 * 메인 애플리케이션의 이벤트 처리 로직을 분리합니다.
 */
public class EventHandlers {

    private final Stage primaryStage;
    private final FileScanService fileScanService;
    private final ObservableList<FileInfo> fileList;

    // UI 컴포넌트들
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Button organizeButton;

    public EventHandlers(Stage primaryStage, ObservableList<FileInfo> fileList) {
        this.primaryStage = primaryStage;
        this.fileList = fileList;

        // FileScanService는 UI 컴포넌트들이 설정된 후에 초기화
        this.fileScanService = null; // 나중에 초기화
    }

    /**
     * UI 컴포넌트들 설정 (초기화 후 호출)
     */
    public void setUIComponents(ProgressBar progressBar, Label statusLabel, Label progressLabel, Button organizeButton) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.organizeButton = organizeButton;
    }

    /**
     * FileScanService 초기화 (UI 컴포넌트 설정 후 호출)
     */
    public FileScanService initializeFileScanService() {
        if (progressBar == null || statusLabel == null || progressLabel == null) {
            throw new IllegalStateException("UI components must be set before initializing FileScanService");
        }
        return new FileScanService(progressBar, statusLabel, progressLabel, fileList);
    }

    /**
     * 폴더 열기 핸들러
     */
    public void handleOpenFolder() {
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
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            System.out.println("[INFO] Selected folder: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory); // 선택된 폴더 스캔 시작
        } else {
            System.out.println("[INFO] Folder selection cancelled");
        }
    }

    /**
     * 파일 스캔 시작
     */
    private void startFileScan(File directory) {
        FileScanService scanService = initializeFileScanService();
        scanService.startFileScan(directory);

        // 스캔이 완료되면 정리 버튼 활성화 (추후 구현)
        // 현재는 임시로 활성화
        if (organizeButton != null) {
            organizeButton.setDisable(false);
        }
    }

    /**
     * 설정 핸들러
     */
    public void handleSettings() {
        System.out.println("[INFO] Settings clicked");
        UIFactory.showInfoDialog("Settings", "Settings dialog will be implemented later.");
    }

    /**
     * 파일 스캔 핸들러 (스캔 버튼용)
     */
    public void handleScanFiles() {
        System.out.println("[INFO] Scan Files clicked");
        handleOpenFolder(); // 스캔 버튼은 폴더 선택과 동일한 동작
    }

    /**
     * 파일 정리 핸들러
     */
    public void handleOrganizeFiles() {
        System.out.println("[INFO] Organize Files clicked");

        // 분석된 파일이 있는지 확인
        long analyzedCount = fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .count();

        if (analyzedCount == 0) {
            UIFactory.showInfoDialog("No Files to Organize", "Please scan a folder first to find files to organize.");
            return;
        }

        // 확인 다이얼로그
        boolean confirmed = UIFactory.showConfirmDialog(
                "Organize Files",
                "Are you sure you want to organize " + analyzedCount + " files?\nThis action cannot be undone."
        );

        if (confirmed) {
            UIFactory.showInfoDialog("Organize Files", "File organization feature will be implemented in the next phase.");
        }
    }

    /**
     * 중복 파일 찾기 핸들러
     */
    public void handleFindDuplicates() {
        System.out.println("[INFO] Find Duplicates clicked");
        UIFactory.showInfoDialog("Find Duplicates", "Duplicate detection feature will be implemented later.");
    }

    /**
     * 정보 핸들러
     */
    public void handleAbout() {
        UIFactory.showInfoDialog("About Smart File Manager",
                "Smart File Manager v1.0\n" +
                        "AI-powered File Organization Tool\n\n" +
                        "Built with JavaFX\n" +
                        "© 2024 Smart File Manager");
    }

    /**
     * 도움말 핸들러
     */
    public void handleHelpTopics() {
        UIFactory.showInfoDialog("Help", "Help documentation will be available soon.");
    }
}