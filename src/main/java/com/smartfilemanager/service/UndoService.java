package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 파일 정리 되돌리기 서비스
 * 정리된 파일들을 원래 위치로 되돌립니다
 */
public class UndoService {

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;

    public UndoService(ProgressBar progressBar, Label statusLabel, Label progressLabel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
    }

    /**
     * 정리된 파일들을 원래 위치로 되돌리기
     */
    public Task<Integer> undoOrganizationAsync(List<FileInfo> organizedFiles) {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int successCount = 0;
                int totalFiles = organizedFiles.size();

                System.out.println("[정보] " + totalFiles + "개 파일 되돌리기 시작");

                // UI 업데이트 - 되돌리기 시작
                Platform.runLater(() -> updateUIForUndoStart(totalFiles));

                for (int i = 0; i < organizedFiles.size(); i++) {
                    FileInfo fileInfo = organizedFiles.get(i);

                    try {
                        // 파일 되돌리기 실행
                        undoSingleFile(fileInfo);

                        // 상태를 분석됨으로 변경
                        fileInfo.setStatus(ProcessingStatus.ANALYZED);
                        fileInfo.setProcessedAt(LocalDateTime.now());

                        successCount++;

                        System.out.println("[성공] 되돌리기 완료: " + fileInfo.getFileName() + " -> " + fileInfo.getOriginalLocation());

                    } catch (Exception e) {
                        // 상태를 실패로 변경
                        fileInfo.setStatus(ProcessingStatus.FAILED);
                        fileInfo.setErrorMessage("되돌리기 실패: " + e.getMessage());

                        System.err.println("[오류] 되돌리기 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
                    }

                    // 진행률 업데이트
                    final int currentProgress = i + 1;

                    Platform.runLater(() -> {
                        double progress = (double) currentProgress / totalFiles;
                        progressBar.setProgress(progress);
                        progressLabel.setText(currentProgress + " / " + totalFiles + " 파일 되돌림");
                        statusLabel.setText("되돌리는 중: " + fileInfo.getFileName());
                    });

                    // 진행률 표시를 위한 작은 지연
                    Thread.sleep(100);
                }

                return successCount;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> updateUIForUndoComplete(getValue(), organizedFiles.size()));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> updateUIForUndoError(getException()));
            }
        };
    }

    /**
     * 단일 파일 되돌리기
     */
    private void undoSingleFile(FileInfo fileInfo) throws IOException {
        // 현재 파일 위치
        Path currentPath = Paths.get(fileInfo.getFilePath());

        // 원래 위치 계산
        Path originalDir = Paths.get(fileInfo.getOriginalLocation());
        Path originalPath = originalDir.resolve(fileInfo.getFileName());

        // 원래 디렉토리가 없으면 생성
        if (!Files.exists(originalDir)) {
            Files.createDirectories(originalDir);
        }

        // 원래 위치에 같은 이름의 파일이 있으면 처리
        if (Files.exists(originalPath)) {
            // 백업 파일명 생성
            String backupName = createBackupFileName(fileInfo.getFileName());
            Path backupPath = originalDir.resolve(backupName);

            // 기존 파일을 백업으로 이동
            Files.move(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[백업] 기존 파일 백업: " + originalPath + " -> " + backupPath);
        }

        // 파일을 원래 위치로 이동
        Files.move(currentPath, originalPath, StandardCopyOption.REPLACE_EXISTING);

        // FileInfo 업데이트
        fileInfo.setFilePath(originalPath.toString());
        fileInfo.setSuggestedPath(null); // 제안 경로 초기화

        System.out.println("[이동] " + currentPath + " -> " + originalPath);
    }

    /**
     * 백업 파일명 생성
     */
    private String createBackupFileName(String originalFileName) {
        String baseName = getBaseName(originalFileName);
        String extension = getFileExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if (extension.isEmpty()) {
            return baseName + "_backup_" + timestamp;
        } else {
            return baseName + "_backup_" + timestamp + "." + extension;
        }
    }

    /**
     * 확장자를 제외한 파일명 반환
     */
    private String getBaseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
    }

    /**
     * 파일 확장자 반환
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }

    /**
     * 되돌리기 가능한 파일들 필터링
     */
    public static List<FileInfo> getUndoableFiles(List<FileInfo> fileList) {
        return fileList.stream()
                .filter(file -> file.getStatus() == ProcessingStatus.ORGANIZED)
                .filter(file -> file.getOriginalLocation() != null)
                .filter(file -> !file.getFilePath().equals(file.getOriginalLocation() + "/" + file.getFileName()))
                .collect(Collectors.toList());
    }

    /**
     * 되돌리기 시작 시 UI 업데이트
     */
    private void updateUIForUndoStart(int totalFiles) {
        statusLabel.setText("파일 되돌리기를 시작합니다...");
        statusLabel.setStyle("-fx-text-fill: #ff6b35; -fx-font-weight: bold;"); // 주황색

        progressBar.setProgress(0);
        progressBar.setVisible(true);

        progressLabel.setText("0 / " + totalFiles + " 파일 되돌림");
        progressLabel.setVisible(true);
    }

    /**
     * 되돌리기 완료 시 UI 업데이트
     */
    private void updateUIForUndoComplete(int successCount, int totalFiles) {
        int failedCount = totalFiles - successCount;

        String message = String.format("되돌리기 완료: %d개 성공, %d개 실패", successCount, failedCount);
        statusLabel.setText(message);

        if (failedCount == 0) {
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // 초록색
        } else {
            statusLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;"); // 노란색
        }

        progressBar.setProgress(1.0);
        progressLabel.setText("되돌리기가 성공적으로 완료되었습니다");

        System.out.println("[완료] 파일 되돌리기 완료: " + successCount + "/" + totalFiles + " 성공");
    }

    /**
     * 되돌리기 실패 시 UI 업데이트
     */
    private void updateUIForUndoError(Throwable error) {
        statusLabel.setText("되돌리기 실패: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // 빨간색

        progressBar.setProgress(0);
        progressLabel.setText("되돌리기 실패");

        System.err.println("[오류] 파일 되돌리기 실패: " + error.getMessage());
        error.printStackTrace();
    }
}