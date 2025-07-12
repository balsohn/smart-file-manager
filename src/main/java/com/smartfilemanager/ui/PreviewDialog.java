package com.smartfilemanager.ui;

import com.smartfilemanager.model.FileInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 파일 미리보기 다이얼로그
 * 더블클릭 시 파일 내용을 미리보기로 표시합니다.
 */
public class PreviewDialog {
    
    // 지원하는 파일 확장자
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp"
    );
    
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList(
        "txt", "md", "json", "xml", "csv", "log", "properties", "yml", "yaml",
        "java", "js", "html", "css", "py", "cpp", "c", "h", "php", "rb", "go"
    );
    
    public static void showPreview(Stage ownerStage, FileInfo fileInfo) {
        if (fileInfo == null || !new File(fileInfo.getFilePath()).exists()) {
            showErrorDialog("파일을 찾을 수 없습니다.");
            return;
        }
        
        String fileName = fileInfo.getFileName();
        String extension = getFileExtension(fileName).toLowerCase();
        
        // 지원하지 않는 파일 형식 체크
        if (!isPreviewSupported(extension)) {
            showUnsupportedDialog(ownerStage, fileInfo);
            return;
        }
        
        try {
            Stage previewStage = new Stage();
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.initOwner(ownerStage);
            previewStage.setTitle("📄 미리보기 - " + fileName);
            
            BorderPane root = new BorderPane();
            
            // 헤더 정보
            VBox header = createHeader(fileInfo);
            root.setTop(header);
            
            // 미리보기 내용
            ScrollPane contentPane = createPreviewContent(fileInfo, extension);
            root.setCenter(contentPane);
            
            // 하단 버튼
            HBox buttonBar = createButtonBar(previewStage, fileInfo);
            root.setBottom(buttonBar);
            
            Scene scene = new Scene(root, 800, 600);
            // 스타일시트는 ThemeManager가 자동으로 적용하므로 제거
            
            previewStage.setScene(scene);
            previewStage.setResizable(true);
            
            // Preview 창 Scene을 ThemeManager에 등록 (자동으로 현재 테마 적용됨)
            ThemeManager.registerScene(scene);
            
            // Preview 창이 닫힐 때 Scene 등록 해제
            previewStage.setOnHidden(event -> ThemeManager.unregisterScene(scene));
            
            previewStage.show();
            
        } catch (Exception e) {
            showErrorDialog("미리보기를 표시할 수 없습니다: " + e.getMessage());
        }
    }
    
    private static VBox createHeader(FileInfo fileInfo) {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("📄 " + fileInfo.getFileName());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        File file = new File(fileInfo.getFilePath());
        Label infoLabel = new Label(String.format("크기: %s | 수정일: %s | 경로: %s", 
            formatFileSize(fileInfo.getFileSize()),
            fileInfo.getModifiedDate().toString(),
            file.getParent()));
        infoLabel.setStyle("-fx-text-fill: #6c757d;");
        
        header.getChildren().addAll(titleLabel, infoLabel);
        return header;
    }
    
    private static ScrollPane createPreviewContent(FileInfo fileInfo, String extension) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        if (IMAGE_EXTENSIONS.contains(extension)) {
            scrollPane.setContent(createImagePreview(new File(fileInfo.getFilePath())));
        } else if (TEXT_EXTENSIONS.contains(extension)) {
            scrollPane.setContent(createTextPreview(Paths.get(fileInfo.getFilePath())));
        }
        
        return scrollPane;
    }
    
    private static ImageView createImagePreview(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(750);
            imageView.setFitHeight(500);
            return imageView;
        } catch (Exception e) {
            return new ImageView(); // 빈 이미지뷰 반환
        }
    }
    
    private static TextArea createTextPreview(Path filePath) {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setFont(Font.font("Consolas", 12));
        
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String content = new String(bytes);
            
            // 텍스트가 너무 길면 처음 10000자만 표시
            if (content.length() > 10000) {
                content = content.substring(0, 10000) + "\n\n... (파일이 너무 큽니다. 처음 10000자만 표시)";
            }
            
            textArea.setText(content);
        } catch (IOException e) {
            textArea.setText("파일을 읽을 수 없습니다: " + e.getMessage());
        }
        
        return textArea;
    }
    
    private static HBox createButtonBar(Stage previewStage, FileInfo fileInfo) {
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(15));
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Button openButton = new Button("📂 파일 열기");
        openButton.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(new File(fileInfo.getFilePath()));
            } catch (Exception ex) {
                showErrorDialog("파일을 열 수 없습니다: " + ex.getMessage());
            }
        });
        
        Button closeButton = new Button("닫기");
        closeButton.setOnAction(e -> previewStage.close());
        
        buttonBar.getChildren().addAll(openButton, closeButton);
        return buttonBar;
    }
    
    private static void showUnsupportedDialog(Stage ownerStage, FileInfo fileInfo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(ownerStage);
        alert.setTitle("미리보기 지원 안함");
        alert.setHeaderText("미리보기를 지원하지 않는 파일 형식입니다.");
        alert.setContentText("파일: " + fileInfo.getFileName() + "\n\n지원 형식:\n" +
                           "• 이미지: JPG, PNG, GIF, BMP, TIFF\n" +
                           "• 텍스트: TXT, MD, JSON, XML, CSV, LOG\n" +
                           "• 코드: JAVA, JS, HTML, CSS, PY 등\n\n" +
                           "파일을 기본 프로그램으로 여시겠습니까?");
        
        ButtonType openButton = new ButtonType("파일 열기");
        ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openButton, cancelButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == openButton) {
                try {
                    java.awt.Desktop.getDesktop().open(new File(fileInfo.getFilePath()));
                } catch (Exception e) {
                    showErrorDialog("파일을 열 수 없습니다: " + e.getMessage());
                }
            }
        });
    }
    
    private static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("오류");
        alert.setHeaderText("미리보기 오류");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static boolean isPreviewSupported(String extension) {
        return IMAGE_EXTENSIONS.contains(extension) || TEXT_EXTENSIONS.contains(extension);
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
    
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}