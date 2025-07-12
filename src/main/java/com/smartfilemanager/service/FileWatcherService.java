package com.smartfilemanager.service;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 실시간 폴더 모니터링 서비스
 * 지정된 폴더의 변경사항을 감시하고 자동으로 파일을 분석/정리합니다
 */
public class FileWatcherService {

    // 변경 타입 열거형
    public enum ChangeType {
        CREATED, MODIFIED, DELETED
    }

    // 파일 변경 콜백 인터페이스
    @FunctionalInterface
    public interface FileChangeCallback {
        void onFileChanged(Path filePath, ChangeType changeType);
    }

    private WatchService watchService;
    private boolean isWatching = false;
    private ExecutorService watcherExecutor;
    private Path watchedDirectory;
    
    // 콜백 필드
    private FileChangeCallback fileChangeCallback;

    // 서비스 의존성
    private final FileAnalysisService analysisService;
    private final FileOrganizerService organizerService;
    private final ConfigService configService;

    // UI 업데이트 콜백
    private Consumer<String> statusUpdateCallback;
    private Consumer<FileInfo> newFileCallback;
    private ObservableList<FileInfo> fileList;

    // 설정
    private AppConfig currentConfig;

    public FileWatcherService() {
        this.analysisService = new FileAnalysisService();
        this.organizerService = new FileOrganizerService(null, null, null);
        this.configService = new ConfigService();
        this.watcherExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FileWatcher-Thread");
            t.setDaemon(true);
            return t;
        });

        loadConfig();
    }
    
    /**
     * 파일 변경 콜백 설정
     */
    public void setFileChangeCallback(FileChangeCallback fileChangeCallback) {
        this.fileChangeCallback = fileChangeCallback;
    }

    /**
     * 폴더 모니터링 시작
     */
    public boolean startWatching(String directoryPath) {
        if (isWatching) {
            System.out.println("[WATCHER] 이미 모니터링 중입니다: " + watchedDirectory);
            return false;
        }

        try {
            // WatchService 초기화
            watchService = FileSystems.getDefault().newWatchService();
            watchedDirectory = Paths.get(directoryPath);

            // 디렉토리 존재 확인
            if (!Files.exists(watchedDirectory) || !Files.isDirectory(watchedDirectory)) {
                updateStatus("오류: 폴더가 존재하지 않습니다 - " + directoryPath);
                return false;
            }

            // 폴더 등록 (생성, 수정, 삭제 이벤트 모니터링)
            watchedDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            isWatching = true;

            // 백그라운드에서 모니터링 시작
            watcherExecutor.submit(this::watchLoop);

            updateStatus("실시간 모니터링 시작: " + directoryPath);
            System.out.println("[WATCHER] 폴더 모니터링 시작: " + directoryPath);

            return true;

        } catch (IOException e) {
            updateStatus("모니터링 시작 실패: " + e.getMessage());
            System.err.println("[WATCHER] 모니터링 시작 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 폴더 모니터링 중지
     */
    public void stopWatching() {
        if (!isWatching) {
            return;
        }

        isWatching = false;

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            System.err.println("[WATCHER] WatchService 종료 중 오류: " + e.getMessage());
        }

        updateStatus("실시간 모니터링 중지됨");
        System.out.println("[WATCHER] 폴더 모니터링 중지");
    }

    /**
     * 메인 모니터링 루프
     */
    private void watchLoop() {
        while (isWatching) {
            try {
                // 이벤트 대기 (타임아웃 설정)
                WatchKey key = watchService.poll(java.util.concurrent.TimeUnit.SECONDS.toMillis(1),
                        java.util.concurrent.TimeUnit.MILLISECONDS);

                if (key == null) {
                    continue; // 타임아웃, 다시 시도
                }

                // 이벤트 처리
                for (WatchEvent<?> event : key.pollEvents()) {
                    processWatchEvent(event);
                }

                // WatchKey 초기화
                boolean valid = key.reset();
                if (!valid) {
                    updateStatus("모니터링 폴더가 삭제되어 모니터링을 중지합니다");
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[WATCHER] 모니터링 중 오류: " + e.getMessage());
                updateStatus("모니터링 중 오류 발생: " + e.getMessage());
            }
        }

        isWatching = false;
    }

    /**
     * 파일 시스템 이벤트 처리
     */
    private void processWatchEvent(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        Path eventPath = (Path) event.context();
        Path fullPath = watchedDirectory.resolve(eventPath);

        // 파일인지 확인 (디렉토리 제외)
        if (!Files.isRegularFile(fullPath)) {
            return;
        }

        String fileName = eventPath.toString();
        System.out.println("[WATCHER] 이벤트 감지: " + kind.name() + " - " + fileName);

        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            handleNewFile(fullPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            handleModifiedFile(fullPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            handleDeletedFile(fullPath);
        }
    }

    /**
     * 새 파일 생성 처리
     */
    private void handleNewFile(Path filePath) {
        updateStatus("새 파일 발견: " + filePath.getFileName());

        // 파일이 완전히 쓰여질 때까지 대기
        CompletableFuture.runAsync(() -> {
            try {
                waitForFileCompletion(filePath);

                if (!Files.exists(filePath)) {
                    return; // 파일이 삭제됨
                }

                // 파일 분석
                FileInfo fileInfo = analysisService.analyzeFile(filePath.toString());
                fileInfo.setStatus(ProcessingStatus.ANALYZED);

                // UI 스레드에서 파일 리스트 업데이트
                Platform.runLater(() -> {
                    if (fileList != null) {
                        fileList.add(fileInfo);
                    }

                    if (newFileCallback != null) {
                        newFileCallback.accept(fileInfo);
                    }
                });

                // 자동 정리 실행 (설정에 따라)
                if (currentConfig.isAutoOrganizeEnabled() &&
                        fileInfo.getConfidenceScore() >= 0.7) {

                    performAutoOrganization(fileInfo);
                }

                updateStatus("새 파일 처리 완료: " + fileInfo.getFileName());

            } catch (Exception e) {
                System.err.println("[WATCHER] 새 파일 처리 실패: " + filePath + " - " + e.getMessage());
                updateStatus("파일 처리 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 파일 수정 처리
     */
    private void handleModifiedFile(Path filePath) {
        // 수정된 파일은 재분석하지 않음 (성능상 이유)
        // 필요시 설정에서 활성화 가능
        if (currentConfig.isEnableContentAnalysis()) {
            updateStatus("파일 수정됨: " + filePath.getFileName());
        }
    }

    /**
     * 파일 삭제 처리
     */
    private void handleDeletedFile(Path filePath) {
        updateStatus("파일 삭제됨: " + filePath.getFileName());

        // UI에서 해당 파일 제거
        Platform.runLater(() -> {
            if (fileList != null) {
                fileList.removeIf(file -> file.getFilePath().equals(filePath.toString()));
            }
        });
    }

    /**
     * 파일이 완전히 쓰여질 때까지 대기
     */
    private void waitForFileCompletion(Path filePath) throws InterruptedException {
        long previousSize = -1;
        long currentSize = 0;
        int stableCount = 0;

        // 최대 30초 대기, 3번 연속 같은 크기면 완료로 간주
        int maxAttempts = 30;
        int attempts = 0;

        while (stableCount < 3 && attempts < maxAttempts) {
            try {
                if (!Files.exists(filePath)) {
                    break; // 파일이 삭제됨
                }

                currentSize = Files.size(filePath);

                if (currentSize == previousSize && currentSize > 0) {
                    stableCount++;
                } else {
                    stableCount = 0;
                }

                previousSize = currentSize;
                Thread.sleep(1000); // 1초 대기
                attempts++;

            } catch (IOException e) {
                // 파일이 아직 사용 중일 수 있음
                Thread.sleep(1000);
                attempts++;
            }
        }

        if (attempts >= maxAttempts) {
            System.out.println("[WATCHER] 파일 완료 대기 타임아웃: " + filePath);
        }
    }

    /**
     * 자동 정리 실행
     */
    private void performAutoOrganization(FileInfo fileInfo) {
        try {
            String targetPath = currentConfig.getOrganizationRootFolder();
            if (targetPath == null || targetPath.trim().isEmpty()) {
                targetPath = System.getProperty("user.home") + "/OrganizedFiles";
            }

            // 파일 정리 수행
            organizerService.organizeFile(fileInfo, targetPath);

            fileInfo.setStatus(ProcessingStatus.ORGANIZED);
            fileInfo.setProcessedAt(LocalDateTime.now());

            updateStatus("자동 정리 완료: " + fileInfo.getFileName() + " → " + fileInfo.getSuggestedPath());

            // 알림 표시 (설정에 따라)
            if (currentConfig.isShowNotifications()) {
                showNotification("파일 자동 정리",
                        fileInfo.getFileName() + "이(가) " + fileInfo.getDetectedCategory() + " 폴더로 이동되었습니다.");
            }

        } catch (Exception e) {
            fileInfo.setStatus(ProcessingStatus.FAILED);
            fileInfo.setErrorMessage(e.getMessage());

            System.err.println("[WATCHER] 자동 정리 실패: " + fileInfo.getFileName() + " - " + e.getMessage());
            updateStatus("자동 정리 실패: " + e.getMessage());
        }
    }

    /**
     * 알림 표시
     */
    private void showNotification(String title, String message) {
        // 시스템 알림 또는 UI 알림
        Platform.runLater(() -> {
            // TODO: SystemTrayManager 또는 JavaFX Notification 구현
            System.out.println("[NOTIFICATION] " + title + ": " + message);
        });
    }

    /**
     * 상태 업데이트
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            if (statusUpdateCallback != null) {
                statusUpdateCallback.accept(message);
            }
        });
    }

    /**
     * 설정 로드
     */
    private void loadConfig() {
        try {
            this.currentConfig = configService.loadConfig();
        } catch (Exception e) {
            System.err.println("[WATCHER] 설정 로드 실패: " + e.getMessage());
            this.currentConfig = new AppConfig(); // 기본 설정 사용
        }
    }

    // Getters and Setters

    public boolean isWatching() {
        return isWatching;
    }

    public String getWatchedDirectory() {
        return watchedDirectory != null ? watchedDirectory.toString() : null;
    }

    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }

    public void setNewFileCallback(Consumer<FileInfo> callback) {
        this.newFileCallback = callback;
    }

    public void setFileList(ObservableList<FileInfo> fileList) {
        this.fileList = fileList;
    }

    public void updateConfig(AppConfig config) {
        this.currentConfig = config;
    }

    /**
     * 서비스 종료 시 호출
     */
    public void shutdown() {
        stopWatching();

        if (watcherExecutor != null && !watcherExecutor.isShutdown()) {
            watcherExecutor.shutdown();
            try {
                if (!watcherExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    watcherExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                watcherExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}