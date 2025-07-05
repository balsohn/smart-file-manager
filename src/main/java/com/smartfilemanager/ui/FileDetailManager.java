package com.smartfilemanager.ui;

import com.smartfilemanager.model.FileInfo;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 파일 상세 정보 표시를 관리하는 클래스
 */
public class FileDetailManager {

    private VBox detailPanel;
    private Label detailContent;

    public FileDetailManager(VBox detailPanel) {
        this.detailPanel = detailPanel;
        this.detailContent = (Label) detailPanel.lookup("#detailContent");
    }

    /**
     * 선택된 파일의 상세 정보를 표시합니다.
     */
    public void updateFileDetails(FileInfo fileInfo) {
        if (fileInfo == null) {
            hideDetails();
            return;
        }

        StringBuilder details = new StringBuilder();

        // 기본 정보
        details.append("[FILE] ").append(fileInfo.getFileName()).append("\n");
        details.append("[CATEGORY] ").append(fileInfo.getDetectedCategory());

        if (fileInfo.getDetectedSubCategory() != null) {
            details.append(" → ").append(fileInfo.getDetectedSubCategory());
        }
        details.append("\n");

        details.append("[SIZE] ").append(fileInfo.getFormattedFileSize()).append("\n");

        // 경로 정보 (짧게 표시)
        String path = fileInfo.getFilePath();
        if (path.length() > 50) {
            path = "..." + path.substring(path.length() - 47);
        }
        details.append("[PATH] ").append(path);

        // 추가 정보가 있다면
        if (fileInfo.getConfidenceScore() > 0) {
            details.append("\n[CONFIDENCE] ").append(String.format("%.0f%%", fileInfo.getConfidenceScore() * 100));
        }

        detailContent.setText(details.toString());
        showDetails();
    }

    /**
     * 상세 정보 패널을 표시합니다.
     */
    public void showDetails() {
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    /**
     * 상세 정보 패널을 숨깁니다.
     */
    public void hideDetails() {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    /**
     * 상세 정보 패널이 표시 중인지 확인합니다.
     */
    public boolean isDetailsVisible() {
        return detailPanel.isVisible();
    }
}