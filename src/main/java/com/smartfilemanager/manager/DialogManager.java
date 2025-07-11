package com.smartfilemanager.manager;

import com.smartfilemanager.constants.MessageConstants;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.util.FileFormatUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.List;
import java.util.Optional;

/**
 * 다이얼로그 표시 및 사용자 확인을 관리하는 클래스
 */
public class DialogManager {
    
    /**
     * 정보 다이얼로그 표시
     */
    public void showInfoDialog(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }
    
    /**
     * 경고 다이얼로그 표시
     */
    public void showWarningDialog(String title, String message) {
        showAlert(title, message, Alert.AlertType.WARNING);
    }
    
    /**
     * 오류 다이얼로그 표시
     */
    public void showErrorDialog(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }
    
    /**
     * 확인 다이얼로그 표시
     */
    public boolean showConfirmDialog(String title, String message) {
        return showConfirmAlert(title, message);
    }
    
    /**
     * 파일 정리 확인 다이얼로그
     */
    public boolean showOrganizeConfirmDialog(List<FileInfo> files) {
        if (files.isEmpty()) return false;
        
        long totalSize = files.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = FileFormatUtils.formatFileSize(totalSize);
        
        String message = String.format(MessageConstants.DialogMessages.ORGANIZE_CONFIRM, 
                                     files.size(), formattedSize);
        
        return showConfirmDialog(MessageConstants.DialogTitles.ORGANIZE_FILES, message);
    }
    
    /**
     * 파일 되돌리기 확인 다이얼로그
     */
    public boolean showUndoConfirmDialog(List<FileInfo> files) {
        if (files.isEmpty()) return false;
        
        long totalSize = files.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = FileFormatUtils.formatFileSize(totalSize);
        
        String message = String.format(MessageConstants.DialogMessages.UNDO_CONFIRM, 
                                     files.size(), formattedSize);
        
        return showConfirmDialog(MessageConstants.DialogTitles.UNDO_FILES, message);
    }
    
    /**
     * AI 분석 확인 다이얼로그
     */
    public boolean showAIAnalysisConfirmDialog(int fileCount) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(MessageConstants.DialogTitles.AI_BATCH_ANALYSIS);
        confirmAlert.setHeaderText(fileCount + "개 파일을 AI로 재분석하시겠습니까?");
        confirmAlert.setContentText(MessageConstants.DialogMessages.AI_ANALYSIS_CONFIRM);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * AI 분석 결과 요약 다이얼로그
     */
    public void showAISummaryDialog(long aiAnalyzedFiles, long totalFiles) {
        if (aiAnalyzedFiles == 0) {
            showInfoDialog("AI 분석 결과 없음", MessageConstants.DialogMessages.AI_SUMMARY_EMPTY);
            return;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(MessageConstants.AISummaryMessages.TITLE).append("\n\n");
        summary.append(String.format(MessageConstants.AISummaryMessages.ANALYZED_FILES, aiAnalyzedFiles)).append("\n");
        summary.append(String.format(MessageConstants.AISummaryMessages.TOTAL_FILES, totalFiles)).append("\n");
        
        showInfoDialog("AI 분석 요약", summary.toString());
    }
    
    /**
     * 성공 메시지 다이얼로그
     */
    public void showSuccessDialog(String operation, int successCount) {
        String message = String.format(MessageConstants.DialogMessages.ORGANIZE_SUCCESS, 
                                     operation, successCount);
        showInfoDialog(MessageConstants.DialogTitles.COMPLETE, message);
    }
    
    /**
     * 공통 Alert 다이얼로그 표시
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setPrefWidth(400);
            alert.showAndWait();
        });
    }
    
    /**
     * 확인 Alert 다이얼로그 표시
     */
    private boolean showConfirmAlert(String title, String message) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(title);
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(message);
        confirmAlert.getDialogPane().setPrefWidth(400);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}