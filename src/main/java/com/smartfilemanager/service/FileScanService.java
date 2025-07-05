package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.collections.ObservableList;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 파일 스캔 담당 서비스 클래스
 * 백그라운드에서 파일 스캔 작업을 수행합니다.
 */
public class FileScanService {

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private ObservableList<FileInfo> fileList;

    public FileScanService(ProgressBar progressBar, Label statusLabel, Label progressLabel, ObservableList<FileInfo> fileList) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.fileList = fileList;
    }

    /**
     * 폴더 스캔을 백그라운드에서 시작합니다.
     */
    public void startFileScan(File directory) {
        // UI 상태 업데이트 (스캔 시작)
        updateUIForScanStart();

        // 백그라운드 Task 생성
        Task<List<FileInfo>> scanTask = new Task<List<FileInfo>>() {
            @Override
            protected List<FileInfo> call() throws Exception {
                List<FileInfo> fileInfoList = new ArrayList<>();

                // 디렉토리 내 모든 파일 스캔
                File[] files = directory.listFiles();
                if (files == null) {
                    return fileInfoList; // 빈 리스트 반환
                }

                int totalFiles = files.length;
                int processedFiles = 0;

                System.out.println("[INFO] Starting scan of " + totalFiles + " items");

                for (File file : files) {
                    if (file.isFile()) { // 파일만 처리 (디렉토리 제외)
                        try {
                            // 파일 정보 생성
                            FileInfo fileInfo = createFileInfo(file);
                            fileInfoList.add(fileInfo);

                            // 진행률 업데이트
                            processedFiles++;
                            final int currentProgress = processedFiles;

                            // UI 업데이트 (JavaFX Application Thread에서 실행)
                            Platform.runLater(() -> {
                                double progress = (double) currentProgress / totalFiles;
                                progressBar.setProgress(progress);
                                progressLabel.setText(currentProgress + " / " + totalFiles + " files processed");
                                statusLabel.setText("Scanning: " + file.getName());
                            });

                            // 시뮬레이션을 위한 약간의 지연 (실제로는 제거해도 됨)
                            Thread.sleep(50);

                        } catch (Exception e) {
                            System.err.println("[ERROR] Failed to process file: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }

                return fileInfoList;
            }

            @Override
            protected void succeeded() {
                // 스캔 성공 시 UI 업데이트
                List<FileInfo> result = getValue();
                Platform.runLater(() -> updateUIForScanComplete(result));
            }

            @Override
            protected void failed() {
                // 스캔 실패 시 UI 업데이트
                Platform.runLater(() -> updateUIForScanError(getException()));
            }
        };

        // 백그라운드 스레드에서 Task 실행
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true); // 애플리케이션 종료 시 함께 종료
        scanThread.start();
    }

    /**
     * 파일 정보 생성 (Lombok FileInfo 사용)
     */
    private FileInfo createFileInfo(File file) {
        try {
            // 실제 파일의 수정 날짜 가져오기
            java.nio.file.Path path = file.toPath();
            java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);

            LocalDateTime createdTime = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), java.time.ZoneId.systemDefault());
            LocalDateTime modifiedTime = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());

            return FileInfo.defaultBuilder()
                    .fileName(file.getName())
                    .filePath(file.getAbsolutePath())
                    .originalLocation(file.getAbsolutePath())
                    .fileSize(file.length())
                    .fileExtension(getFileExtension(file.getName()))
                    .detectedCategory(detectCategoryFromExtension(file.getName()))
                    .createdDate(createdTime)
                    .modifiedDate(modifiedTime)
                    .status(ProcessingStatus.ANALYZED)
                    .processedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read file attributes for: " + file.getName() + " - " + e.getMessage());

            // 오류 발생 시 기본값으로 생성
            return FileInfo.defaultBuilder()
                    .fileName(file.getName())
                    .filePath(file.getAbsolutePath())
                    .originalLocation(file.getAbsolutePath())
                    .fileSize(file.length())
                    .fileExtension(getFileExtension(file.getName()))
                    .detectedCategory(detectCategoryFromExtension(file.getName()))
                    .status(ProcessingStatus.ANALYZED)
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    /**
     * 확장자로 기본 카테고리 감지
     */
    private String detectCategoryFromExtension(String fileName) {
        String extension = getFileExtension(fileName);

        // 이미지 파일
        if (extension.matches("jpg|jpeg|png|gif|bmp|svg|webp")) {
            return "Images";
        }
        // 문서 파일
        if (extension.matches("pdf|doc|docx|txt|rtf|odt")) {
            return "Documents";
        }
        // 비디오 파일
        if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm")) {
            return "Videos";
        }
        // 오디오 파일
        if (extension.matches("mp3|wav|flac|aac|m4a|ogg")) {
            return "Audio";
        }
        // 압축 파일
        if (extension.matches("zip|rar|7z|tar|gz|bz2")) {
            return "Archives";
        }

        return "Others";
    }

    /**
     * 스캔 시작 시 UI 업데이트
     */
    private void updateUIForScanStart() {
        statusLabel.setText("Scanning files...");
        statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;"); // 파란색으로 변경

        progressBar.setProgress(0);
        progressBar.setVisible(true);  // 스캔 시작 시 표시

        progressLabel.setText("0 / 0 files processed");
        progressLabel.setVisible(true);  // 스캔 시작 시 표시
    }

    /**
     * 스캔 완료 시 UI 업데이트
     */
    private void updateUIForScanComplete(List<FileInfo> fileInfoList) {
        statusLabel.setText("Scan completed: " + fileInfoList.size() + " files found");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // 초록색으로 변경

        progressBar.setProgress(1.0);
        progressLabel.setText("Scan completed successfully");

        // 다양한 상태 시뮬레이션을 위해 일부 파일의 상태 변경
        addVariousStatusForTesting(fileInfoList);

        // 파일 목록을 UI 테이블에 추가
        fileList.clear();
        fileList.addAll(fileInfoList);

        System.out.println("[SUCCESS] Scanned " + fileInfoList.size() + " files");
        for (FileInfo info : fileInfoList) {
            System.out.println("  - " + info.getFileName() + " (" +
                    info.getDetectedCategory() + ", " +
                    info.getFormattedFileSize() + ", " +
                    info.getStatus().getDisplayName() + ")");
        }
    }

    /**
     * 테스트를 위해 다양한 상태 추가
     */
    private void addVariousStatusForTesting(List<FileInfo> fileInfoList) {
        if (fileInfoList.size() >= 5) {
            // 첫 번째 파일: ORGANIZED
            fileInfoList.get(0).setStatus(ProcessingStatus.ORGANIZED);

            // 두 번째 파일: PENDING
            fileInfoList.get(1).setStatus(ProcessingStatus.PENDING);

            // 세 번째 파일: FAILED
            if (fileInfoList.size() > 2) {
                fileInfoList.get(2).setStatus(ProcessingStatus.FAILED);
                fileInfoList.get(2).setErrorMessage("Permission denied");
            }

            // 네 번째 파일: SKIPPED
            if (fileInfoList.size() > 3) {
                fileInfoList.get(3).setStatus(ProcessingStatus.SKIPPED);
            }

            // 다섯 번째 파일: ORGANIZING
            if (fileInfoList.size() > 4) {
                fileInfoList.get(4).setStatus(ProcessingStatus.ORGANIZING);
            }
        }

        System.out.println("[INFO] Added various status types for testing purposes");
    }

    /**
     * 스캔 오류 시 UI 업데이트
     */
    private void updateUIForScanError(Throwable error) {
        statusLabel.setText("Scan failed: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // 빨간색으로 변경

        progressBar.setProgress(0);
        progressLabel.setText("Scan failed");

        System.err.println("[ERROR] Scan failed: " + error.getMessage());
        error.printStackTrace();
    }
}