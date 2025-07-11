package com.smartfilemanager.manager;

import com.smartfilemanager.constants.MessageConstants;
import com.smartfilemanager.constants.UIConstants;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.FileAnalysisService;
import com.smartfilemanager.util.FileFormatUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * UI 업데이트 관련 로직을 관리하는 클래스
 */
public class UIUpdateManager {
    
    private final Label statusLabel;
    private final Label progressLabel;
    private final Label statsLabel;
    private final Label statisticsLabel;
    private final Label aiStatusIndicator;
    private final Label currentFileLabel;
    private final Label monitoringStatusLabel;
    private final Label monitoringFolderLabel;
    private final Button organizeButton;
    private final Button monitoringToggleButton;
    private final Button aiAnalysisButton;
    private final ProgressBar progressBar;
    private final HBox monitoringInfoBox;
    private final MenuItem realTimeMonitoringMenuItem;
    
    private final FileAnalysisService fileAnalysisService;
    private final ObservableList<FileInfo> fileList;
    
    public UIUpdateManager(Label statusLabel, Label progressLabel, Label statsLabel, 
                          Label statisticsLabel, Label aiStatusIndicator, Label currentFileLabel,
                          Label monitoringStatusLabel, Label monitoringFolderLabel,
                          Button organizeButton, Button monitoringToggleButton, Button aiAnalysisButton,
                          ProgressBar progressBar, HBox monitoringInfoBox, MenuItem realTimeMonitoringMenuItem,
                          FileAnalysisService fileAnalysisService, ObservableList<FileInfo> fileList) {
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.statsLabel = statsLabel;
        this.statisticsLabel = statisticsLabel;
        this.aiStatusIndicator = aiStatusIndicator;
        this.currentFileLabel = currentFileLabel;
        this.monitoringStatusLabel = monitoringStatusLabel;
        this.monitoringFolderLabel = monitoringFolderLabel;
        this.organizeButton = organizeButton;
        this.monitoringToggleButton = monitoringToggleButton;
        this.aiAnalysisButton = aiAnalysisButton;
        this.progressBar = progressBar;
        this.monitoringInfoBox = monitoringInfoBox;
        this.realTimeMonitoringMenuItem = realTimeMonitoringMenuItem;
        this.fileAnalysisService = fileAnalysisService;
        this.fileList = fileList;
    }
    
    /**
     * 전체 UI 업데이트
     */
    public void updateUI() {
        updateStatistics();
        updateOrganizeButtonState();
        updateAIStatusIndicator();
    }
    
    /**
     * 통계 정보 업데이트
     */
    public void updateStatistics() {
        if (fileList.isEmpty()) {
            setEmptyStatistics();
            return;
        }
        
        long totalFiles = fileList.size();
        long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
        long analyzedCount = getAnalyzedCount();
        long organizedCount = getOrganizedCount();
        long aiAnalyzedCount = getAIAnalyzedCount();
        
        updateStatisticsLabels(totalFiles, totalSize, analyzedCount, organizedCount, aiAnalyzedCount);
    }
    
    /**
     * 정리 버튼 상태 업데이트
     */
    public void updateOrganizeButtonState() {
        if (organizeButton != null) {
            boolean hasProcessableFiles = fileList.stream()
                    .anyMatch(file -> file.getStatus().isProcessable());
            organizeButton.setDisable(!hasProcessableFiles);
        }
    }
    
    /**
     * AI 상태 표시기 업데이트
     */
    public void updateAIStatusIndicator() {
        if (aiStatusIndicator == null) return;
        
        boolean aiAvailable = fileAnalysisService.isAIAnalysisAvailable();
        Platform.runLater(() -> {
            if (aiAvailable) {
                aiStatusIndicator.setText(MessageConstants.UILabels.AI_ACTIVE);
                aiStatusIndicator.getStyleClass().removeAll(UIConstants.AIStatusStyles.INACTIVE, UIConstants.AIStatusStyles.ERROR);
                aiStatusIndicator.getStyleClass().add(UIConstants.AIStatusStyles.ACTIVE);
            } else {
                aiStatusIndicator.setText(MessageConstants.UILabels.AI_INACTIVE);
                aiStatusIndicator.getStyleClass().removeAll(UIConstants.AIStatusStyles.ACTIVE, UIConstants.AIStatusStyles.ERROR);
                aiStatusIndicator.getStyleClass().add(UIConstants.AIStatusStyles.INACTIVE);
            }
        });
    }
    
    /**
     * 모니터링 UI 업데이트
     */
    public void updateMonitoringUI(boolean isMonitoringActive) {
        Platform.runLater(() -> {
            if (monitoringToggleButton != null) {
                monitoringToggleButton.setText(isMonitoringActive ? 
                    MessageConstants.UILabels.MONITORING_STOP : 
                    MessageConstants.UILabels.MONITORING_START);
                if (isMonitoringActive) {
                    monitoringToggleButton.getStyleClass().add("active");
                } else {
                    monitoringToggleButton.getStyleClass().remove("active");
                }
            }
            if (realTimeMonitoringMenuItem != null && realTimeMonitoringMenuItem instanceof CheckMenuItem) {
                ((CheckMenuItem) realTimeMonitoringMenuItem).setSelected(isMonitoringActive);
            }
        });
    }
    
    /**
     * 모니터링 상태 업데이트
     */
    public void updateMonitoringStatus(String message, boolean isMonitoringActive) {
        Platform.runLater(() -> {
            if (monitoringStatusLabel != null) {
                monitoringStatusLabel.setText(message);
                if (isMonitoringActive) {
                    monitoringStatusLabel.getStyleClass().removeAll(UIConstants.AIStatusStyles.INACTIVE, UIConstants.AIStatusStyles.ERROR);
                    monitoringStatusLabel.getStyleClass().add(UIConstants.AIStatusStyles.ACTIVE);
                } else {
                    monitoringStatusLabel.getStyleClass().removeAll(UIConstants.AIStatusStyles.ACTIVE, UIConstants.AIStatusStyles.ERROR);
                    monitoringStatusLabel.getStyleClass().add(UIConstants.AIStatusStyles.INACTIVE);
                }
            }
            updateStatusLabel(message);
        });
    }
    
    /**
     * 모니터링 폴더 정보 업데이트
     */
    public void updateMonitoringFolder(String path) {
        if (monitoringFolderLabel != null) {
            monitoringFolderLabel.setText(path);
        }
        if (monitoringInfoBox != null) {
            monitoringInfoBox.setVisible(true);
            monitoringInfoBox.setManaged(true);
        }
    }
    
    /**
     * 모니터링 정보 숨기기
     */
    public void hideMonitoringInfo() {
        if (monitoringInfoBox != null) {
            monitoringInfoBox.setVisible(false);
            monitoringInfoBox.setManaged(false);
        }
    }
    
    /**
     * 상태 라벨 업데이트
     */
    public void updateStatusLabel(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    /**
     * 진행률 UI 업데이트
     */
    public void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
                progressBar.setVisible(true);
            }
            if (progressLabel != null) {
                progressLabel.setText(message);
                progressLabel.setVisible(true);
            }
        });
    }
    
    /**
     * 임시 메시지 표시
     */
    public void showTemporaryMessage(String message) {
        if (currentFileLabel != null) {
            currentFileLabel.setText(message);
            Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(com.smartfilemanager.constants.FileConstants.ProcessingDelays.UI_MESSAGE_DISPLAY), 
                e -> {
                    if (currentFileLabel != null) {
                        currentFileLabel.setText("");
                    }
                }));
            timeline.play();
        }
    }
    
    /**
     * 새 파일 감지 시 UI 업데이트
     */
    public void handleNewFileDetected(FileInfo newFile) {
        Platform.runLater(() -> {
            updateStatistics();
            updateStatusLabel(String.format(MessageConstants.UILabels.NEW_FILE_DETECTED, newFile.getFileName()));
            
            if (currentFileLabel != null) {
                currentFileLabel.setText(String.format(MessageConstants.UILabels.FILE_DETECTED, newFile.getFileName()));
                Timeline timeline = new Timeline(new KeyFrame(
                    Duration.millis(com.smartfilemanager.constants.FileConstants.ProcessingDelays.UI_MESSAGE_DISPLAY), 
                    e -> {
                        if (currentFileLabel != null) {
                            currentFileLabel.setText("");
                        }
                    }));
                timeline.play();
            }
            
            if (newFile.getStatus() == ProcessingStatus.ORGANIZED) {
                showTemporaryMessage(String.format(MessageConstants.UILabels.FILE_AUTO_ORGANIZED, 
                    newFile.getFileName(), newFile.getDetectedCategory()));
            }
        });
    }
    
    /**
     * AI 분석 버튼 상태 업데이트
     */
    public void updateAIAnalysisButton(boolean isAnalyzing) {
        Platform.runLater(() -> {
            if (aiAnalysisButton != null) {
                aiAnalysisButton.setDisable(isAnalyzing);
                aiAnalysisButton.setText(isAnalyzing ? 
                    MessageConstants.UILabels.AI_ANALYZING : 
                    MessageConstants.UILabels.AI_ANALYZE);
            }
        });
    }
    
    // Private helper methods
    
    private void setEmptyStatistics() {
        if (statsLabel != null) {
            statsLabel.setText(MessageConstants.StatisticsLabels.EMPTY_STATS);
        }
        if (statisticsLabel != null) {
            statisticsLabel.setText(MessageConstants.StatisticsLabels.STATISTICS_FORMAT
                .formatted(0, 0, "0 B"));
        }
    }
    
    private void updateStatisticsLabels(long totalFiles, long totalSize, long analyzedCount, long organizedCount, long aiAnalyzedCount) {
        String formattedSize = FileFormatUtils.formatFileSize(totalSize);
        
        if (statsLabel != null) {
            String statsText = aiAnalyzedCount > 0 ? 
                FileFormatUtils.formatFileStatsWithAI(totalFiles, totalSize, aiAnalyzedCount) :
                FileFormatUtils.formatFileStats(totalFiles, totalSize);
            statsLabel.setText(statsText);
        }
        
        if (statisticsLabel != null) {
            statisticsLabel.setText(String.format(MessageConstants.StatisticsLabels.STATISTICS_FORMAT,
                analyzedCount, organizedCount, formattedSize));
        }
    }
    
    private long getAnalyzedCount() {
        return fileList.stream().filter(f -> 
            f.getStatus() == ProcessingStatus.ANALYZED || 
            f.getStatus() == ProcessingStatus.ORGANIZED).count();
    }
    
    private long getOrganizedCount() {
        return fileList.stream().filter(f -> 
            f.getStatus() == ProcessingStatus.ORGANIZED).count();
    }
    
    private long getAIAnalyzedCount() {
        return fileList.stream().mapToLong(file -> 
            com.smartfilemanager.util.FileIconUtils.isAIAnalyzed(file) ? 1 : 0).sum();
    }
}