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

        // 카테고리 정보 (서브카테고리 포함)
        details.append("[CATEGORY] ").append(fileInfo.getDetectedCategory());
        if (fileInfo.getDetectedSubCategory() != null && !fileInfo.getDetectedSubCategory().equals("General")) {
            details.append(" → ").append(fileInfo.getDetectedSubCategory());
        }
        details.append("\n");

        details.append("[SIZE] ").append(fileInfo.getFormattedFileSize()).append("\n");

        // MIME 타입 정보
        if (fileInfo.getMimeType() != null) {
            details.append("[TYPE] ").append(fileInfo.getMimeType()).append("\n");
        }

        // 경로 정보 (짧게 표시)
        String path = fileInfo.getFilePath();
        if (path.length() > 45) {
            path = "..." + path.substring(path.length() - 42);
        }
        details.append("[PATH] ").append(path).append("\n");

        // 신뢰도 점수
        if (fileInfo.getConfidenceScore() > 0) {
            details.append("[CONFIDENCE] ").append(String.format("%.0f%%", fileInfo.getConfidenceScore() * 100)).append("\n");
        }

        // 추출된 제목
        if (fileInfo.getExtractedTitle() != null && !fileInfo.getExtractedTitle().isEmpty()) {
            String title = fileInfo.getExtractedTitle();
            if (title.length() > 30) {
                title = title.substring(0, 27) + "...";
            }
            details.append("[TITLE] ").append(title).append("\n");
        }

        // 설명 (있다면)
        if (fileInfo.getDescription() != null && !fileInfo.getDescription().isEmpty()) {
            String desc = fileInfo.getDescription();
            if (desc.length() > 50) {
                desc = desc.substring(0, 47) + "...";
            }
            details.append("[DESC] ").append(desc);
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