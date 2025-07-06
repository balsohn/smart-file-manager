package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 파일 정리 서비스
 * 실제 파일 이동 및 폴더 구조화를 담당합니다
 */
public class FileOrganizerService {

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;

    public FileOrganizerService(ProgressBar progressBar, Label statusLabel, Label progressLabel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
    }

    /**
     * 여러 파일을 백그라운드에서 정리
     */
    public Task<Integer> organizeFilesAsync(List<FileInfo> filesToOrganize, String targetRootPath) {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int successCount = 0;
                int totalFiles = filesToOrganize.size();

                System.out.println("[정보] " + totalFiles + "개 파일 정리 시작, 대상 폴더: " + targetRootPath);

                // UI 업데이트 - 정리 시작
                Platform.runLater(() -> updateUIForOrganizeStart(totalFiles));

                for (int i = 0; i < filesToOrganize.size(); i++) {
                    FileInfo fileInfo = filesToOrganize.get(i);

                    try {
                        // 상태를 정리 중으로 변경
                        fileInfo.setStatus(ProcessingStatus.ORGANIZING);

                        // 단일 파일 정리
                        organizeFile(fileInfo, targetRootPath);

                        // 상태를 정리 완료로 변경
                        fileInfo.setStatus(ProcessingStatus.ORGANIZED);
                        fileInfo.setProcessedAt(LocalDateTime.now());

                        successCount++;

                        System.out.println("[성공] 정리 완료: " + fileInfo.getFileName() + " -> " + fileInfo.getSuggestedPath());

                    } catch (Exception e) {
                        // 상태를 실패로 변경
                        fileInfo.setStatus(ProcessingStatus.FAILED);
                        fileInfo.setErrorMessage(e.getMessage());

                        System.err.println("[오류] 정리 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
                    }

                    // 진행률 업데이트
                    final int currentProgress = i + 1;
                    final int currentSuccessCount = successCount;

                    Platform.runLater(() -> {
                        double progress = (double) currentProgress / totalFiles;
                        progressBar.setProgress(progress);
                        progressLabel.setText(currentProgress + " / " + totalFiles + " 파일 처리됨");
                        statusLabel.setText("정리 중: " + fileInfo.getFileName());
                    });

                    // 진행률 표시를 위한 작은 지연
                    Thread.sleep(100);
                }

                return successCount;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> updateUIForOrganizeComplete(getValue(), filesToOrganize.size()));
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> updateUIForOrganizeError(getException()));
            }
        };
    }

    /**
     * 단일 파일 정리
     */
    public void organizeFile(FileInfo fileInfo, String targetRootPath) throws IOException {
        // 1. 대상 폴더 경로 결정
        String categoryPath = determineTargetPath(fileInfo, targetRootPath);

        // 2. 대상 디렉토리가 없으면 생성
        Path targetDir = Paths.get(categoryPath);
        Files.createDirectories(targetDir);

        // 3. 파일명 충돌 해결
        String finalFileName = resolveFileNameConflict(targetDir, fileInfo.getFileName());
        Path targetFilePath = targetDir.resolve(finalFileName);

        // 4. 파일 이동
        Path sourceFilePath = Paths.get(fileInfo.getFilePath());
        Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        // 5. 파일 정보 업데이트
        fileInfo.setFilePath(targetFilePath.toString());
        fileInfo.setSuggestedPath(categoryPath);

        System.out.println("[이동] " + sourceFilePath + " -> " + targetFilePath);
    }

    /**
     * 카테고리에 따른 대상 폴더 경로 결정
     */
    private String determineTargetPath(FileInfo fileInfo, String rootPath) {
        StringBuilder pathBuilder = new StringBuilder(rootPath);

        // 카테고리 폴더 추가
        String category = fileInfo.getDetectedCategory();
        if (category == null || category.isEmpty()) {
            category = "Others";
        }

        pathBuilder.append(File.separator).append(category);

        // 서브카테고리가 있으면 추가
        String subCategory = fileInfo.getDetectedSubCategory();
        if (subCategory != null && !subCategory.isEmpty()) {
            pathBuilder.append(File.separator).append(subCategory);
        }

        // 날짜별 정리 옵션
        if (shouldOrganizeByDate(category)) {
            LocalDateTime fileDate = fileInfo.getModifiedDate();
            if (fileDate != null) {
                pathBuilder.append(File.separator)
                        .append(fileDate.getYear())
                        .append(File.separator)
                        .append(String.format("%02d-%s",
                                fileDate.getMonthValue(),
                                fileDate.getMonth().name().substring(0, 3)));
            }
        }

        return pathBuilder.toString();
    }

    /**
     * 날짜별로 정리할지 확인
     */
    private boolean shouldOrganizeByDate(String category) {
        // 이미지와 비디오는 날짜별로 정리
        return "Images".equals(category) || "Videos".equals(category);
    }

    /**
     * 파일명 충돌을 숫자 추가로 해결
     */
    private String resolveFileNameConflict(Path targetDir, String originalFileName) {
        Path targetPath = targetDir.resolve(originalFileName);

        if (!Files.exists(targetPath)) {
            return originalFileName;
        }

        // 파일명과 확장자 분리
        String baseName = getBaseName(originalFileName);
        String extension = getFileExtension(originalFileName);

        int counter = 1;
        while (Files.exists(targetPath)) {
            String newFileName;
            if (extension.isEmpty()) {
                newFileName = baseName + " (" + counter + ")";
            } else {
                newFileName = baseName + " (" + counter + ")." + extension;
            }
            targetPath = targetDir.resolve(newFileName);
            counter++;
        }

        return targetPath.getFileName().toString();
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
     * 정리 시작 시 UI 업데이트
     */
    private void updateUIForOrganizeStart(int totalFiles) {
        statusLabel.setText("파일 정리를 시작합니다...");
        statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;");

        progressBar.setProgress(0);
        progressBar.setVisible(true);

        progressLabel.setText("0 / " + totalFiles + " 파일 처리됨");
        progressLabel.setVisible(true);
    }

    /**
     * 정리 완료 시 UI 업데이트
     */
    private void updateUIForOrganizeComplete(int successCount, int totalFiles) {
        int failedCount = totalFiles - successCount;

        String message = String.format("정리 완료: %d개 성공, %d개 실패", successCount, failedCount);
        statusLabel.setText(message);

        if (failedCount == 0) {
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // 초록색
        } else {
            statusLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;"); // 노란색
        }

        progressBar.setProgress(1.0);
        progressLabel.setText("정리가 성공적으로 완료되었습니다");

        System.out.println("[완료] 파일 정리 완료: " + successCount + "/" + totalFiles + " 성공");
    }

    /**
     * 정리 실패 시 UI 업데이트
     */
    private void updateUIForOrganizeError(Throwable error) {
        statusLabel.setText("정리 실패: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // 빨간색

        progressBar.setProgress(0);
        progressLabel.setText("정리 실패");

        System.err.println("[오류] 파일 정리 실패: " + error.getMessage());
        error.printStackTrace();
    }
}