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
 * 파일 스캔 담당 서비스 클래스 (강화된 분석 기능 포함)
 * 백그라운드에서 파일 스캔 작업을 수행합니다.
 */
public class FileScanService {

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private ObservableList<FileInfo> fileList;
    private FileAnalysisService analysisService;

    public FileScanService(ProgressBar progressBar, Label statusLabel, Label progressLabel, ObservableList<FileInfo> fileList) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.fileList = fileList;
        this.analysisService = new FileAnalysisService();
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

                System.out.println("[정보] " + totalFiles + "개 항목 스캔 시작");

                for (File file : files) {
                    if (file.isFile()) { // 파일만 처리 (디렉토리 제외)
                        try {
                            // 강화된 파일 분석 사용
                            FileInfo fileInfo = analysisService.analyzeFile(file.getAbsolutePath());
                            fileInfoList.add(fileInfo);

                            // 진행률 업데이트
                            processedFiles++;
                            final int currentProgress = processedFiles;

                            // UI 업데이트 (JavaFX Application Thread에서 실행)
                            Platform.runLater(() -> {
                                double progress = (double) currentProgress / totalFiles;
                                progressBar.setProgress(progress);
                                progressLabel.setText(currentProgress + " / " + totalFiles + " 파일 처리됨");
                                statusLabel.setText("분석 중: " + file.getName());
                            });

                            // 시뮬레이션을 위한 약간의 지연 (실제로는 제거해도 됨)
                            Thread.sleep(50);

                        } catch (Exception e) {
                            System.err.println("[오류] 파일 처리 실패: " + file.getName() + " - " + e.getMessage());
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
     * 스캔 시작 시 UI 업데이트
     */
    private void updateUIForScanStart() {
        statusLabel.setText("파일을 스캔하고 있습니다...");
        statusLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;"); // 파란색으로 변경

        progressBar.setProgress(0);
        progressBar.setVisible(true);  // 스캔 시작 시 표시

        progressLabel.setText("0 / 0 파일 처리됨");
        progressLabel.setVisible(true);  // 스캔 시작 시 표시
    }

    /**
     * 스캔 완료 시 UI 업데이트
     */
    private void updateUIForScanComplete(List<FileInfo> fileInfoList) {
        statusLabel.setText("스캔 완료: " + fileInfoList.size() + "개 파일 발견");
        statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // 초록색으로 변경

        progressBar.setProgress(1.0);
        progressLabel.setText("스캔이 성공적으로 완료되었습니다");

        // 파일 상태는 실제 처리 과정에서 업데이트됨

        // 파일 목록을 UI 테이블에 추가
        fileList.clear();
        fileList.addAll(fileInfoList);

        System.out.println("[성공] " + fileInfoList.size() + "개 파일 스캔됨");

        // 분석 결과 요약 출력
        printAnalysisSummary(fileInfoList);
    }

    /**
     * 분석 결과 요약 출력
     */
    private void printAnalysisSummary(List<FileInfo> fileInfoList) {
        System.out.println("\n=== 📊 파일 분석 요약 ===");

        // 카테고리별 통계
        java.util.Map<String, Long> categoryStats = fileInfoList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("📂 카테고리별 파일:");
        categoryStats.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + "개"));

        // 서브카테고리 통계 (상위 5개)
        java.util.Map<String, Long> subCategoryStats = fileInfoList.stream()
                .filter(f -> f.getDetectedSubCategory() != null && !f.getDetectedSubCategory().equals("General"))
                .collect(java.util.stream.Collectors.groupingBy(
                        FileInfo::getDetectedSubCategory,
                        java.util.stream.Collectors.counting()
                ));

        if (!subCategoryStats.isEmpty()) {
            System.out.println("\n🎯 주요 서브카테고리:");
            subCategoryStats.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + "개"));
        }

        // 신뢰도 평균
        double avgConfidence = fileInfoList.stream()
                .mapToDouble(FileInfo::getConfidenceScore)
                .average()
                .orElse(0.0);

        System.out.println("\n🎯 평균 분류 신뢰도: " + String.format("%.1f%%", avgConfidence * 100));

        // 샘플 파일들 (분류가 잘 된 파일들)
        System.out.println("\n📋 분석 샘플:");
        fileInfoList.stream()
                .filter(f -> f.getDetectedSubCategory() != null && !f.getDetectedSubCategory().equals("General"))
                .limit(5)
                .forEach(f -> System.out.println("  • " + f.getFileName() +
                        " → " + f.getDetectedCategory() + "/" + f.getDetectedSubCategory() +
                        " (" + String.format("%.0f%%", f.getConfidenceScore() * 100) + ")"));

        System.out.println("========================\n");
    }


    /**
     * 스캔 오류 시 UI 업데이트
     */
    private void updateUIForScanError(Throwable error) {
        statusLabel.setText("스캔 실패: " + error.getMessage());
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // 빨간색으로 변경

        progressBar.setProgress(0);
        progressLabel.setText("스캔 실패");

        System.err.println("[오류] 스캔 실패: " + error.getMessage());
        error.printStackTrace();
    }
}