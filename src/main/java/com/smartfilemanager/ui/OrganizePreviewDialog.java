package com.smartfilemanager.ui;

import com.smartfilemanager.model.FileInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 정리 미리보기 다이얼로그
 * 정리 전에 파일들이 어디로 이동할지 미리 확인할 수 있습니다.
 */
public class OrganizePreviewDialog {
    
    public static boolean showPreview(Stage ownerStage, List<FileInfo> filesToOrganize) {
        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.initOwner(ownerStage);
        previewStage.setTitle("📋 정리 확인");
        previewStage.setResizable(true);
        
        BorderPane root = new BorderPane();
        
        // 헤더
        VBox header = createHeader(filesToOrganize);
        root.setTop(header);
        
        // 미리보기 테이블
        TableView<PreviewItem> previewTable = createPreviewTable(filesToOrganize);
        root.setCenter(previewTable);
        
        // 결과 반환을 위한 변수
        final boolean[] confirmed = {false};
        
        // 하단 버튼
        HBox buttonBar = createButtonBar(previewStage, confirmed);
        root.setBottom(buttonBar);
        
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add("/css/styles.css");
        
        previewStage.setScene(scene);
        previewStage.showAndWait();
        
        return confirmed[0];
    }
    
    private static VBox createHeader(List<FileInfo> filesToOrganize) {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("📋 정리 확인");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Label infoLabel = new Label(String.format("총 %d개 파일을 다음과 같이 정리합니다. 계속하시겠습니까?", filesToOrganize.size()));
        infoLabel.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px;");
        
        // 카테고리별 통계
        Map<String, Long> categoryStats = filesToOrganize.stream()
            .collect(Collectors.groupingBy(
                file -> file.getDetectedCategory() != null ? file.getDetectedCategory() : "기타",
                Collectors.counting()
            ));
        
        StringBuilder statsText = new StringBuilder("카테고리별 분포: ");
        categoryStats.forEach((category, count) -> 
            statsText.append(String.format("%s(%d개) ", category, count)));
        
        Label statsLabel = new Label(statsText.toString());
        statsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        
        header.getChildren().addAll(titleLabel, infoLabel, statsLabel);
        return header;
    }
    
    private static TableView<PreviewItem> createPreviewTable(List<FileInfo> filesToOrganize) {
        TableView<PreviewItem> table = new TableView<>();
        
        // 컬럼 정의
        TableColumn<PreviewItem, String> fileNameColumn = new TableColumn<>("📄 파일명");
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileNameColumn.setPrefWidth(250);
        
        TableColumn<PreviewItem, String> currentPathColumn = new TableColumn<>("📂 현재 위치");
        currentPathColumn.setCellValueFactory(new PropertyValueFactory<>("currentPath"));
        currentPathColumn.setPrefWidth(200);
        
        TableColumn<PreviewItem, String> targetPathColumn = new TableColumn<>("🎯 정리 위치");
        targetPathColumn.setCellValueFactory(new PropertyValueFactory<>("targetPath"));
        targetPathColumn.setPrefWidth(200);
        
        TableColumn<PreviewItem, String> categoryColumn = new TableColumn<>("📂 카테고리");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setPrefWidth(120);
        
        TableColumn<PreviewItem, String> actionColumn = new TableColumn<>("⚡ 작업");
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setPrefWidth(100);
        
        table.getColumns().addAll(fileNameColumn, currentPathColumn, targetPathColumn, categoryColumn, actionColumn);
        
        // 데이터 생성
        List<PreviewItem> previewItems = filesToOrganize.stream()
            .map(OrganizePreviewDialog::createPreviewItem)
            .collect(Collectors.toList());
        
        table.getItems().addAll(previewItems);
        
        // 스타일링
        table.setRowFactory(tv -> {
            TableRow<PreviewItem> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    // 이동/복사에 따른 색상 구분
                    if ("이동".equals(newItem.getAction())) {
                        row.setStyle("-fx-background-color: #e3f2fd;");
                    } else if ("복사".equals(newItem.getAction())) {
                        row.setStyle("-fx-background-color: #f3e5f5;");
                    } else if ("건너뜀".equals(newItem.getAction())) {
                        row.setStyle("-fx-background-color: #fff3e0;");
                    }
                }
            });
            return row;
        });
        
        return table;
    }
    
    private static PreviewItem createPreviewItem(FileInfo fileInfo) {
        String fileName = fileInfo.getFileName();
        String currentPath = new File(fileInfo.getFilePath()).getParent();
        
        // 정리 위치 결정
        String targetPath;
        String action;
        
        if (fileInfo.getSuggestedPath() != null && !fileInfo.getSuggestedPath().isEmpty()) {
            targetPath = fileInfo.getSuggestedPath();
            
            // 현재 위치와 목적지가 같은지 확인
            if (currentPath.equals(new File(targetPath).getParent())) {
                action = "건너뜀";
            } else {
                action = "이동"; // 기본적으로 이동
            }
        } else {
            targetPath = "정리 위치 미정";
            action = "건너뜀";
        }
        
        String category = fileInfo.getDetectedCategory() != null ? fileInfo.getDetectedCategory() : "기타";
        
        return new PreviewItem(fileName, currentPath, targetPath, category, action);
    }
    
    private static HBox createButtonBar(Stage previewStage, boolean[] confirmed) {
        HBox buttonBar = new HBox(15);
        buttonBar.setPadding(new Insets(15));
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Button confirmButton = new Button("✅ 확인 및 정리 시작");
        confirmButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        confirmButton.setPrefWidth(150);
        confirmButton.setOnAction(e -> {
            confirmed[0] = true;
            previewStage.close();
        });
        
        Button cancelButton = new Button("❌ 취소");
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        cancelButton.setPrefWidth(100);
        cancelButton.setOnAction(e -> previewStage.close());
        
        // 범례 추가
        VBox legend = new VBox(5);
        legend.setAlignment(Pos.CENTER_LEFT);
        
        Label legendTitle = new Label("📝 작업 유형:");
        legendTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        Label moveLabel = new Label("🔵 이동: 파일을 새 위치로 이동");
        moveLabel.setStyle("-fx-text-fill: #0277bd; -fx-font-size: 11px;");
        
        Label skipLabel = new Label("🟡 건너뜀: 이미 올바른 위치에 있음");
        skipLabel.setStyle("-fx-text-fill: #f57c00; -fx-font-size: 11px;");
        
        legend.getChildren().addAll(legendTitle, moveLabel, skipLabel);
        
        HBox fullBar = new HBox();
        fullBar.setPadding(new Insets(15));
        fullBar.setAlignment(Pos.CENTER_RIGHT);
        fullBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Region spacer = new Region();
        fullBar.getChildren().addAll(legend, spacer, cancelButton, confirmButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        return fullBar;
    }
    
    /**
     * 미리보기 테이블에 사용할 데이터 모델
     */
    public static class PreviewItem {
        private final String fileName;
        private final String currentPath;
        private final String targetPath;
        private final String category;
        private final String action;
        
        public PreviewItem(String fileName, String currentPath, String targetPath, String category, String action) {
            this.fileName = fileName;
            this.currentPath = currentPath;
            this.targetPath = targetPath;
            this.category = category;
            this.action = action;
        }
        
        // Getters for PropertyValueFactory
        public String getFileName() { return fileName; }
        public String getCurrentPath() { return currentPath; }
        public String getTargetPath() { return targetPath; }
        public String getCategory() { return category; }
        public String getAction() { return action; }
    }
}