package com.smartfilemanager.service;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.util.FileOperationSafety;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.collections.ObservableList;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 파일 스캔 담당 서비스 클래스 (강화된 분석 기능 포함)
 * 백그라운드에서 파일 스캔 작업을 수행합니다.
 */
public class FileScanService {

    // 절대 건드리면 안 되는 핵심 시스템 디렉토리
    private static final Set<String> CRITICAL_SYSTEM_DIRECTORIES = Set.of(
        "system32", "windows", "boot", "recovery", 
        "$recycle.bin", "system volume information", 
        "windows.old", "perflogs", "documents and settings"
    );

    // 경고와 함께 스캔은 허용하지만 신중하게 처리할 디렉토리들
    private static final Set<String> SENSITIVE_DIRECTORIES = Set.of(
        "program files", "program files (x86)", "programdata"
    );

    // 절대 스캔하면 안 되는 시스템 루트 경로들
    private static final Set<String> CRITICAL_SYSTEM_ROOTS = Set.of(
        "c:\\windows", "c:\\system32"
    );

    // 콜백 인터페이스
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total, String currentFile);
    }

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private ObservableList<FileInfo> fileList;
    private FileAnalysisService analysisService;
    private FileOperationSafety safetyChecker;
    
    // 콜백 필드
    private ProgressCallback progressCallback;

    public FileScanService(ProgressBar progressBar, Label statusLabel, Label progressLabel, ObservableList<FileInfo> fileList) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.fileList = fileList;
        this.analysisService = new FileAnalysisService();
        this.safetyChecker = new FileOperationSafety();
    }
    
    /**
     * 진행률 콜백 설정
     */
    public void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    /**
     * 디렉토리가 스캔하기에 안전한지 확인
     */
    private boolean isDirectorySafeToScan(File directory) {
        String dirPath = directory.getAbsolutePath().toLowerCase();
        
        // 절대 건드리면 안 되는 핵심 시스템 디렉토리 확인
        if (CRITICAL_SYSTEM_DIRECTORIES.stream().anyMatch(dirPath::contains)) {
            System.err.println("[SAFETY] 핵심 시스템 디렉토리로 스캔이 거부됨: " + directory.getAbsolutePath());
            return false;
        }
        
        // 절대 스캔하면 안 되는 시스템 루트 경로 확인
        if (CRITICAL_SYSTEM_ROOTS.stream().anyMatch(dirPath::startsWith)) {
            System.err.println("[SAFETY] 시스템 루트 디렉토리로 스캔이 거부됨: " + directory.getAbsolutePath());
            return false;
        }
        
        // 민감한 디렉토리는 경고와 함께 허용
        if (SENSITIVE_DIRECTORIES.stream().anyMatch(dirPath::contains)) {
            System.out.println("[WARNING] 민감한 디렉토리를 스캔합니다. 시스템 파일들은 자동으로 보호됩니다: " + directory.getAbsolutePath());
            // 스캔은 허용하되 파일 단위에서 더 엄격하게 검사
        }
        
        return true;
    }

    /**
     * 폴더 스캔을 백그라운드에서 시작합니다.
     */
    public void startFileScan(File directory) {
        // 안전성 검사 먼저 수행
        if (!isDirectorySafeToScan(directory)) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ 보안상 스캔할 수 없는 디렉토리입니다");
                statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                progressLabel.setText("스캔이 보안상 차단되었습니다");
            });
            return;
        }

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
                            // 스캔용 안전성 검사 (더 관대한 기준)
                            Path filePath = Paths.get(file.getAbsolutePath());
                            if (!safetyChecker.isSafeToScan(filePath)) {
                                System.out.println("[SAFETY] 보호된 파일 스캔에서 제외: " + file.getName());
                                processedFiles++;
                                continue; // 안전하지 않은 파일은 건너뛰기
                            }

                            // 강화된 파일 분석 사용
                            FileInfo fileInfo = analysisService.analyzeFile(file.getAbsolutePath());
                            fileInfoList.add(fileInfo);

                            // 진행률 업데이트
                            processedFiles++;
                            final int currentProgress = processedFiles;

                            // 콜백 호출
                            if (progressCallback != null) {
                                progressCallback.onProgress(currentProgress, totalFiles, file.getName());
                            }
                            
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
    
    /**
     * 동기식 파일 스캔 메서드 (FileOperationController에서 사용)
     */
    public List<FileInfo> scanFiles(java.nio.file.Path directory) throws Exception {
        System.out.println("[정보] 동기식 파일 스캔 시작: " + directory);
        
        // 디렉토리 안전성 검사
        if (!isDirectorySafeToScan(directory.toFile())) {
            throw new SecurityException("보안상 스캔할 수 없는 디렉토리입니다: " + directory);
        }
        
        List<FileInfo> scannedFiles = new ArrayList<>();
        int skippedFiles = 0;
        
        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(directory)) {
            List<java.nio.file.Path> filePaths = paths
                .filter(java.nio.file.Files::isRegularFile)
                .collect(java.util.stream.Collectors.toList());
            
            int totalFiles = filePaths.size();
            int current = 0;
            
            for (java.nio.file.Path filePath : filePaths) {
                current++;
                
                try {
                    // 스캔용 안전성 검사 (더 관대한 기준)
                    if (!safetyChecker.isSafeToScan(filePath)) {
                        System.out.println("[SAFETY] 보호된 파일 스캔에서 제외: " + filePath.getFileName());
                        skippedFiles++;
                        
                        // 진행률 콜백 호출 (건너뛴 파일도 포함)
                        if (progressCallback != null) {
                            progressCallback.onProgress(current, totalFiles, filePath.getFileName().toString() + " (건너뜀)");
                        }
                        continue;
                    }

                    // FileInfo 객체 생성
                    FileInfo fileInfo = analysisService.analyzeFile(filePath.toString());
                    scannedFiles.add(fileInfo);
                    
                    // 진행률 콜백 호출
                    if (progressCallback != null) {
                        progressCallback.onProgress(current, totalFiles, filePath.getFileName().toString());
                    }
                    
                } catch (Exception e) {
                    System.err.println("[경고] 파일 분석 실패: " + filePath + " - " + e.getMessage());
                }
            }
            
            System.out.println("[성공] " + scannedFiles.size() + "개 파일 스캔 완료 (" + skippedFiles + "개 보호된 파일 제외)");
            return scannedFiles;
            
        } catch (Exception e) {
            System.err.println("[오류] 파일 스캔 실패: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (analysisService != null) {
            // FileAnalysisService 종료 로직이 있다면 호출
        }
        System.out.println("[정보] FileScanService 종료됨");
    }
}