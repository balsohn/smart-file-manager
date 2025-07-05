package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.service.FileScanService;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.UIFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * 이벤트 핸들러들을 모아놓은 클래스 (개선된 버전)
 * 메인 애플리케이션의 이벤트 처리 로직을 분리합니다.
 */
public class EventHandlers {

    private final Stage primaryStage;
    private final ObservableList<FileInfo> fileList;

    // UI 컴포넌트들
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Button organizeButton;
    private TableView<FileInfo> fileTable;

    // 서비스들
    private FileScanService fileScanService;
    private FileDetailManager fileDetailManager;

    public EventHandlers(Stage primaryStage, ObservableList<FileInfo> fileList) {
        this.primaryStage = primaryStage;
        this.fileList = fileList;
    }

    /**
     * UI 컴포넌트들 설정 (초기화 후 호출)
     */
    public void setUIComponents(ProgressBar progressBar, Label statusLabel, Label progressLabel,
                                Button organizeButton, TableView<FileInfo> fileTable, VBox detailPanel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.organizeButton = organizeButton;
        this.fileTable = fileTable;

        // 서비스들 초기화
        initializeServices(detailPanel);

        // 테이블 선택 이벤트 설정
        setupTableSelectionListener();
    }

    /**
     * 서비스들 초기화
     */
    private void initializeServices(VBox detailPanel) {
        if (progressBar == null || statusLabel == null || progressLabel == null) {
            throw new IllegalStateException("UI components must be set before initializing services");
        }

        this.fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        this.fileDetailManager = new FileDetailManager(detailPanel);
    }

    /**
     * 테이블 선택 이벤트 설정
     */
    private void setupTableSelectionListener() {
        if (fileTable != null) {
            fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                fileDetailManager.updateFileDetails(newSelection);
            });
        }
    }

    /**
     * 폴더 열기 핸들러
     */
    public void handleOpenFolder() {
        System.out.println("[정보] 폴더 열기 버튼 클릭됨");

        // DirectoryChooser 생성 및 설정
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("정리할 폴더 선택");

        // 기본 폴더를 사용자의 홈 디렉토리로 설정
        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        // 폴더 선택 다이얼로그 표시
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            System.out.println("[정보] 선택된 폴더: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory);
        } else {
            System.out.println("[정보] 폴더 선택이 취소되었습니다");
        }
    }

    /**
     * 파일 스캔 시작
     */
    private void startFileScan(File directory) {
        // 기존 선택 해제 및 상세 정보 숨김
        if (fileTable != null) {
            fileTable.getSelectionModel().clearSelection();
        }
        fileDetailManager.hideDetails();

        // 정리 버튼 비활성화
        if (organizeButton != null) {
            organizeButton.setDisable(true);
        }

        // 스캔 시작
        fileScanService.startFileScan(directory);

        // 스캔 완료 후 정리 버튼 활성화를 위한 리스너 추가
        fileList.addListener((javafx.collections.ListChangeListener<FileInfo>) change -> {
            if (organizeButton != null) {
                boolean hasProcessableFiles = fileList.stream().anyMatch(file -> file.getStatus().isProcessable());
                organizeButton.setDisable(!hasProcessableFiles);
            }
        });
    }

    /**
     * 설정 핸들러
     */
    public void handleSettings() {
        System.out.println("[정보] 설정 버튼 클릭됨");
        UIFactory.showInfoDialog("설정", "설정 다이얼로그는 나중에 구현될 예정입니다.");
    }

    /**
     * 파일 스캔 핸들러 (스캔 버튼용)
     */
    public void handleScanFiles() {
        System.out.println("[정보] 파일 스캔 버튼 클릭됨");
        handleOpenFolder();
    }

    /**
     * 파일 정리 핸들러 (실제 파일 정리 기능 포함)
     */
    public void handleOrganizeFiles() {
        System.out.println("[정보] 파일 정리 버튼 클릭됨");

        // 정리할 파일이 있는지 확인
        List<FileInfo> filesToOrganize = fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .collect(java.util.stream.Collectors.toList());

        if (filesToOrganize.isEmpty()) {
            UIFactory.showInfoDialog("정리할 파일 없음", "먼저 폴더를 스캔해서 정리할 파일을 찾아주세요.");
            return;
        }

        // 상세 통계 정보
        long totalFiles = fileList.size();
        long totalSize = filesToOrganize.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        // 카테고리별 분석
        java.util.Map<String, Long> categoryCount = filesToOrganize.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        StringBuilder message = new StringBuilder();
        message.append(filesToOrganize.size()).append("개 파일을 정리할 준비가 되었습니다 (전체 ").append(totalFiles).append("개 중).\n");
        message.append("총 크기: ").append(formattedSize).append("\n\n");
        message.append("카테고리별 파일:\n");

        categoryCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> message.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("개 파일\n"));

        message.append("\n정리된 파일들이 다음 폴더 구조로 이동됩니다:\n");
        message.append("Documents/\n");
        message.append("Images/\n");
        message.append("Videos/\n");
        message.append("Audio/\n");
        message.append("Archives/\n");
        message.append("Others/\n\n");
        message.append("계속하시겠습니까?");

        // 확인 다이얼로그
        boolean confirmed = UIFactory.showConfirmDialog("파일 정리", message.toString());

        if (confirmed) {
            // 실제 파일 정리 시작
            startFileOrganization(filesToOrganize);
        }
    }

    /**
     * 실제 파일 정리 프로세스 시작
     */
    private void startFileOrganization(List<FileInfo> filesToOrganize) {
        // 정리 대상 폴더 선택
        String targetRootPath = selectOrganizationFolder();

        if (targetRootPath == null) {
            System.out.println("[정보] 사용자가 정리 폴더 선택을 취소했습니다");
            return;
        }

        System.out.println("[정보] 정리 대상 폴더: " + targetRootPath);

        // FileOrganizerService 생성 및 실행
        com.smartfilemanager.service.FileOrganizerService organizerService =
                new com.smartfilemanager.service.FileOrganizerService(progressBar, statusLabel, progressLabel);

        // 백그라운드에서 파일 정리 실행
        javafx.concurrent.Task<Integer> organizeTask = organizerService.organizeFilesAsync(filesToOrganize, targetRootPath);

        // 정리 완료 후 UI 업데이트
        organizeTask.setOnSucceeded(e -> {
            Integer successCount = organizeTask.getValue();

            // 테이블 새로고침 (상태 변경 반영)
            if (fileTable != null) {
                fileTable.refresh();
            }

            // 성공 메시지 표시
            String resultMessage = String.format(
                    "파일 정리가 완료되었습니다!\n\n" +
                            "성공: %d개 파일\n" +
                            "실패: %d개 파일\n\n" +
                            "정리된 파일들을 %s 폴더에서 확인할 수 있습니다.",
                    successCount,
                    filesToOrganize.size() - successCount,
                    targetRootPath
            );

            UIFactory.showInfoDialog("정리 완료", resultMessage);
        });

        organizeTask.setOnFailed(e -> {
            Throwable exception = organizeTask.getException();
            UIFactory.showInfoDialog("정리 실패", "파일 정리 중 오류가 발생했습니다:\n" + exception.getMessage());
        });

        // 백그라운드 스레드에서 실행
        Thread organizeThread = new Thread(organizeTask);
        organizeThread.setDaemon(true);
        organizeThread.start();
    }

    /**
     * 정리할 폴더 선택
     */
    private String selectOrganizationFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("정리된 파일을 저장할 폴더 선택");

        // 기본 폴더를 사용자의 홈 디렉토리로 설정
        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Desktop"); // 바탕화면을 기본으로
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            // "SmartFileManager_Organized" 폴더를 생성해서 그 안에 정리
            return selectedDirectory.getAbsolutePath() + File.separator + "SmartFileManager_Organized";
        }

        return null;
    }

    /**
     * 중복 파일 찾기 핸들러 (개선된 버전)
     */
    public void handleFindDuplicates() {
        System.out.println("[정보] 중복 파일 찾기 버튼 클릭됨");

        if (fileList.isEmpty()) {
            UIFactory.showInfoDialog("파일 없음", "먼저 폴더를 스캔해서 중복 파일을 찾아주세요.");
            return;
        }

        // 임시로 중복 가능성 분석
        long duplicateCandidates = fileList.stream()
                .collect(java.util.stream.Collectors.groupingBy(FileInfo::getFileSize))
                .values().stream()
                .filter(group -> group.size() > 1)
                .mapToLong(java.util.List::size)
                .sum();

        if (duplicateCandidates == 0) {
            UIFactory.showInfoDialog("중복 파일 없음", "파일 크기를 기준으로 중복 가능성이 있는 파일을 찾지 못했습니다.");
        } else {
            UIFactory.showInfoDialog("중복 파일 가능성",
                    "동일한 크기의 파일 " + duplicateCandidates + "개를 발견했습니다.\n" +
                            "고급 중복 탐지 기능은 Phase 6에서 구현될 예정입니다.");
        }
    }

    /**
     * 정보 핸들러
     */
    public void handleAbout() {
        StringBuilder about = new StringBuilder();
        about.append("Smart File Manager v1.0\n");
        about.append("AI 기반 파일 정리 도구\n\n");

        about.append("현재 통계:\n");
        if (!fileList.isEmpty()) {
            long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
            about.append("• 분석된 파일: ").append(fileList.size()).append("개\n");
            about.append("• 총 크기: ").append(formatFileSize(totalSize)).append("\n");

            long organizedCount = fileList.stream().filter(f -> f.getStatus().isCompleted()).count();
            about.append("• 정리된 파일: ").append(organizedCount).append("개\n");
        } else {
            about.append("• 아직 분석된 파일이 없습니다\n");
        }

        about.append("\nJavaFX로 제작\n");
        about.append("© 2024 Smart File Manager");

        UIFactory.showInfoDialog("Smart File Manager 정보", about.toString());
    }

    /**
     * 도움말 핸들러
     */
    public void handleHelpTopics() {
        StringBuilder help = new StringBuilder();
        help.append("Smart File Manager 도움말\n\n");

        help.append("빠른 시작:\n");
        help.append("1. 'Scan Folder'를 클릭해서 파일 분석\n");
        help.append("2. 파일 목록과 카테고리 확인\n");
        help.append("3. 'Organize Files'를 클릭해서 정리\n\n");

        help.append("팁:\n");
        help.append("• 파일에서 우클릭하면 상세 메뉴\n");
        help.append("• 컬럼 헤더 클릭으로 정렬\n");
        help.append("• 파일 선택하면 상세 정보 표시\n");
        help.append("• Ctrl+O로 빠른 폴더 선택\n\n");

        help.append("키보드 단축키:\n");
        help.append("• Ctrl+O: 폴더 열기\n");
        help.append("• F5: 파일 스캔\n");
        help.append("• F6: 파일 정리\n");
        help.append("• F7: 중복 파일 찾기\n");
        help.append("• Ctrl+Q: 종료\n");

        UIFactory.showInfoDialog("도움말", help.toString());
    }

    /**
     * 파일 크기 포맷팅 유틸리티
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 파일 상세 정보 관리자 반환 (테스트용)
     */
    public FileDetailManager getFileDetailManager() {
        return fileDetailManager;
    }
}