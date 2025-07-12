package com.smartfilemanager.manager;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * UI 업데이트를 중앙에서 관리하는 매니저 클래스
 * 모든 UI 업데이트를 JavaFX Application Thread에서 안전하게 처리합니다.
 */
public class UIUpdateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(UIUpdateManager.class);
    
    // UI 컴포넌트들
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private TableView<?> tableView;
    
    // 상태 관리
    private volatile boolean isShutdown = false;
    
    /**
     * 기본 생성자
     */
    public UIUpdateManager() {
        logger.debug("UIUpdateManager 초기화됨");
    }
    
    // ===============================
    // Setter 메서드들 (UI 컴포넌트 등록)
    // ===============================
    
    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }
    
    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
    }
    
    public void setProgressLabel(Label progressLabel) {
        this.progressLabel = progressLabel;
    }
    
    public void setTableView(TableView<?> tableView) {
        this.tableView = tableView;
    }
    
    // ===============================
    // UI 업데이트 메서드들
    // ===============================
    
    /**
     * UI 스레드에서 안전하게 실행
     */
    public void runOnUIThread(Runnable action) {
        if (isShutdown) {
            logger.warn("UIUpdateManager가 종료된 상태에서 UI 업데이트 시도");
            return;
        }
        
        if (Platform.isFxApplicationThread()) {
            try {
                action.run();
            } catch (Exception e) {
                logger.error("UI 업데이트 실행 중 오류", e);
            }
        } else {
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    logger.error("UI 업데이트 실행 중 오류", e);
                }
            });
        }
    }
    
    /**
     * 비동기 UI 업데이트
     */
    public CompletableFuture<Void> runOnUIThreadAsync(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        runOnUIThread(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    // ===============================
    // 진행률 관리
    // ===============================
    
    /**
     * 진행률 업데이트
     */
    public void updateProgress(double progress, String message) {
        runOnUIThread(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
            if (progressLabel != null && message != null) {
                progressLabel.setText(message);
            }
        });
    }
    
    /**
     * 진행률 업데이트 (메시지만)
     */
    public void updateProgressMessage(String message) {
        runOnUIThread(() -> {
            if (progressLabel != null) {
                progressLabel.setText(message != null ? message : "");
            }
        });
    }
    
    /**
     * 진행률 초기화
     */
    public void resetProgress() {
        updateProgress(0.0, "");
    }
    
    /**
     * 진행률 완료
     */
    public void completeProgress() {
        updateProgress(1.0, "완료");
    }
    
    /**
     * 진행률 숨기기
     */
    public void hideProgress() {
        runOnUIThread(() -> {
            if (progressBar != null) {
                progressBar.setProgress(0.0);
                progressBar.setVisible(false);
            }
            if (progressLabel != null) {
                progressLabel.setText("");
                progressLabel.setVisible(false);
            }
        });
    }
    
    /**
     * 진행률 표시
     */
    public void showProgress() {
        runOnUIThread(() -> {
            if (progressBar != null) {
                progressBar.setVisible(true);
            }
            if (progressLabel != null) {
                progressLabel.setVisible(true);
            }
        });
    }
    
    // ===============================
    // 상태 관리
    // ===============================
    
    /**
     * 상태 메시지 업데이트
     */
    public void updateStatus(String message) {
        updateStatus(message, StatusType.INFO);
    }
    
    /**
     * 상태 메시지 업데이트 (타입 지정)
     */
    public void updateStatus(String message, StatusType type) {
        runOnUIThread(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message != null ? message : "");
                
                // 기존 상태 스타일 제거
                statusLabel.getStyleClass().removeAll(
                    "status-info", "status-success", "status-warning", 
                    "status-error", "status-processing"
                );
                
                // 새 상태 스타일 적용
                statusLabel.getStyleClass().add("status-" + type.name().toLowerCase());
            }
        });
        
        // 로깅
        switch (type) {
            case SUCCESS:
                logger.info("상태 업데이트 (성공): {}", message);
                break;
            case WARNING:
                logger.warn("상태 업데이트 (경고): {}", message);
                break;
            case ERROR:
                logger.error("상태 업데이트 (오류): {}", message);
                break;
            case PROCESSING:
                logger.debug("상태 업데이트 (처리중): {}", message);
                break;
            default:
                logger.debug("상태 업데이트: {}", message);
                break;
        }
    }
    
    /**
     * 상태 초기화
     */
    public void clearStatus() {
        updateStatus("준비됨", StatusType.INFO);
    }
    
    // ===============================
    // 테이블 관리
    // ===============================
    
    /**
     * 테이블 새로고침
     */
    public void refreshTable() {
        runOnUIThread(() -> {
            if (tableView != null) {
                tableView.refresh();
                logger.debug("테이블 새로고침 완료");
            }
        });
    }
    
    /**
     * 테이블 선택 해제
     */
    public void clearTableSelection() {
        runOnUIThread(() -> {
            if (tableView != null) {
                tableView.getSelectionModel().clearSelection();
                logger.debug("테이블 선택 해제됨");
            }
        });
    }
    
    /**
     * 테이블 전체 선택
     */
    public void selectAllTable() {
        runOnUIThread(() -> {
            if (tableView != null) {
                tableView.getSelectionModel().selectAll();
                logger.debug("테이블 전체 선택됨");
            }
        });
    }
    
    // ===============================
    // 배치 업데이트
    // ===============================
    
    /**
     * 여러 UI 업데이트를 배치로 처리
     */
    public void batchUpdate(Runnable... updates) {
        runOnUIThread(() -> {
            for (Runnable update : updates) {
                try {
                    update.run();
                } catch (Exception e) {
                    logger.error("배치 업데이트 중 오류", e);
                }
            }
        });
    }
    
    /**
     * 상태와 진행률을 동시에 업데이트
     */
    public void updateStatusAndProgress(String status, StatusType statusType, 
                                       double progress, String progressMessage) {
        runOnUIThread(() -> {
            updateStatus(status, statusType);
            updateProgress(progress, progressMessage);
        });
    }
    
    // ===============================
    // 콜백 지원
    // ===============================
    
    /**
     * 진행률 콜백 생성
     */
    public Consumer<Double> createProgressCallback(String baseMessage) {
        return progress -> {
            String message = baseMessage != null ? 
                String.format("%s (%.0f%%)", baseMessage, progress * 100) : 
                String.format("진행률: %.0f%%", progress * 100);
            updateProgress(progress, message);
        };
    }
    
    /**
     * 상태 콜백 생성
     */
    public Consumer<String> createStatusCallback(StatusType type) {
        return message -> updateStatus(message, type);
    }
    
    // ===============================
    // 애니메이션 지원
    // ===============================
    
    /**
     * 진행률 애니메이션 (부드러운 전환)
     */
    public void animateProgress(double fromProgress, double toProgress, 
                               long durationMs, String message) {
        if (progressBar == null) return;
        
        runOnUIThread(() -> {
            // JavaFX Timeline을 사용한 애니메이션 구현
            // 현재는 간단히 최종 값으로 설정
            updateProgress(toProgress, message);
        });
    }
    
    // ===============================
    // 상태 타입 열거형
    // ===============================
    
    public enum StatusType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        PROCESSING
    }
    
    // ===============================
    // 리소스 정리
    // ===============================
    
    /**
     * UIUpdateManager 종료
     */
    public void shutdown() {
        isShutdown = true;
        
        // 마지막 UI 정리
        runOnUIThread(() -> {
            resetProgress();
            clearStatus();
        });
        
        logger.info("UIUpdateManager 종료됨");
    }
    
    /**
     * 종료 상태 확인
     */
    public boolean isShutdown() {
        return isShutdown;
    }
    
    // ===============================
    // 누락된 메서드들 추가 (FileOperationHandler, ThemeManager에서 사용)
    // ===============================
    
    /**
     * 모니터링 UI 업데이트
     */
    public void updateMonitoringUI(boolean isMonitoring) {
        logger.debug("모니터링 UI 업데이트: {}", isMonitoring ? "활성화" : "비활성화");
        // 실제 구현에서는 모니터링 관련 UI 컴포넌트들을 업데이트
        updateStatus(isMonitoring ? "폴더 모니터링 활성화됨" : "폴더 모니터링 비활성화됨", StatusType.INFO);
    }
    
    /**
     * 모니터링 폴더 정보 업데이트
     */
    public void updateMonitoringFolder(String folderPath) {
        logger.debug("모니터링 폴더 업데이트: {}", folderPath);
        updateStatus("모니터링 폴더: " + folderPath, StatusType.INFO);
    }
    
    /**
     * 모니터링 정보 숨기기
     */
    public void hideMonitoringInfo() {
        logger.debug("모니터링 정보 숨김");
        updateStatus("모니터링 정보가 숨겨졌습니다", StatusType.INFO);
    }
    
    /**
     * 상태 레이블 업데이트 (직접 메시지 설정)
     */
    public void updateStatusLabel(String message) {
        updateStatus(message, StatusType.INFO);
    }
    
    /**
     * AI 분석 버튼 상태 업데이트
     */
    public void updateAIAnalysisButton(boolean enabled) {
        logger.debug("AI 분석 버튼 상태 업데이트: {}", enabled ? "활성화" : "비활성화");
        // 실제 구현에서는 AI 분석 버튼의 활성화/비활성화 상태를 변경
        updateStatus(enabled ? "AI 분석 버튼 활성화" : "AI 분석 버튼 비활성화", StatusType.INFO);
    }
    
    /**
     * 전체 UI 업데이트
     */
    public void updateUI() {
        runOnUIThread(() -> {
            refreshTable();
            logger.debug("전체 UI 업데이트 완료");
        });
    }
    
    /**
     * 임시 메시지 표시 (ThemeManager에서 사용)
     */
    public void showTemporaryMessage(String message) {
        updateStatus(message, StatusType.INFO);
        
        // 3초 후 메시지 자동 제거
        CompletableFuture.delayedExecutor(3, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> {
                    if (!isShutdown) {
                        clearStatus();
                    }
                });
    }
    
    // ===============================
    // 백업 MainController 호환성을 위한 메서드들
    // ===============================
    
    /**
     * 레거시 생성자 호환성을 위한 생성자
     */
    public UIUpdateManager(Object... components) {
        // 기본 생성자 호출
        this();
        logger.debug("레거시 호환 생성자 호출됨 - {} 컴포넌트", components.length);
    }
    
    /**
     * AI 상태 표시기 업데이트
     */
    public void updateAIStatusIndicator() {
        updateStatus("AI 분석 서비스 준비됨", StatusType.SUCCESS);
        logger.debug("AI 상태 표시기 업데이트됨");
    }
    
    /**
     * AI 상태 표시기 업데이트 (활성/비활성 상태 포함)
     */
    public void updateAIStatusIndicator(boolean isActive, String message) {
        runOnUIThread(() -> {
            // AI 상태 라벨이 있다면 업데이트 (실제 구현에서는 라벨 참조가 필요)
            logger.debug("AI 상태 업데이트: {} (활성: {})", message, isActive);
            
            // 상태에 따른 메시지 업데이트
            String statusText = isActive ? "AI 활성" : "AI 비활성";
            if (message != null && !message.trim().isEmpty()) {
                statusText = message;
            }
            
            updateStatus(statusText, isActive ? StatusType.SUCCESS : StatusType.INFO);
        });
    }
    
    /**
     * 모니터링 상태 라벨 업데이트 (CSS 클래스 포함)
     */
    public void updateMonitoringStatusWithStyle(boolean isActive, String message) {
        runOnUIThread(() -> {
            logger.debug("모니터링 상태 업데이트: {} (활성: {})", message, isActive);
            
            // 상태에 따른 메시지 업데이트
            String statusText = isActive ? "모니터링 활성" : "모니터링 대기";
            if (message != null && !message.trim().isEmpty()) {
                statusText = message;
            }
            
            updateStatus(statusText, isActive ? StatusType.SUCCESS : StatusType.INFO);
        });
    }
    
    /**
     * 새 파일 감지 처리
     */
    public void handleNewFileDetected(Object newFile) {
        updateStatus("새 파일이 감지되었습니다", StatusType.INFO);
        logger.debug("새 파일 감지 처리: {}", newFile);
    }
    
    /**
     * 모니터링 상태 업데이트
     */
    public void updateMonitoringStatus(String message, boolean isActive) {
        StatusType type = isActive ? StatusType.SUCCESS : StatusType.INFO;
        updateStatus(message, type);
        logger.debug("모니터링 상태 업데이트: {} (활성: {})", message, isActive);
    }
}