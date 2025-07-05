package com.smartfilemanager;

import com.smartfilemanager.controller.EventHandlers;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.ui.UIFactory;
import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Smart File Manager 메인 애플리케이션 클래스 (리팩토링됨)
 *
 * 변경사항:
 * - UI 생성 로직을 UIFactory로 분리
 * - 이벤트 핸들러들을 EventHandlers로 분리
 * - 파일 스캔 로직을 FileScanService로 분리
 * - 메인 클래스는 애플리케이션 구조와 초기화만 담당
 */
public class SmartFileManagerApp extends Application {

    // 핵심 데이터
    private ObservableList<FileInfo> fileList;

    // 이벤트 처리
    private EventHandlers eventHandlers;

    // UI 컴포넌트들 (참조용)
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Button organizeButton;
    private TableView<FileInfo> fileTable;
    private VBox fileDetailPanel;

    @Override
    public void start(Stage primaryStage) {
        // 1. 데이터 초기화
        initializeData();

        // 2. 이벤트 핸들러 초기화
        eventHandlers = new EventHandlers(primaryStage, fileList);

        // 3. UI 생성
        BorderPane root = createMainLayout(primaryStage);

        // 4. UI 컴포넌트 참조 설정
        setupUIReferences(root);

        // 5. 이벤트 핸들러에 UI 컴포넌트 설정
        eventHandlers.setUIComponents(progressBar, statusLabel, progressLabel, organizeButton, fileTable, fileDetailPanel);

        // 6. 씬 및 스테이지 설정
        setupStage(primaryStage, root);

        System.out.println("[START] " + Messages.get("app.title") + " Started!");
    }

    /**
     * 데이터 초기화
     */
    private void initializeData() {
        fileList = FXCollections.observableArrayList();
    }

    /**
     * 메인 레이아웃 생성
     */
    private BorderPane createMainLayout(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // 메뉴바 생성
        MenuBar menuBar = UIFactory.createMenuBar(
                primaryStage,
                eventHandlers::handleOpenFolder,
                eventHandlers::handleSettings,
                eventHandlers::handleScanFiles,
                eventHandlers::handleOrganizeFiles,
                eventHandlers::handleUndoOrganization,
                eventHandlers::handleFindDuplicates,
                eventHandlers::handleCleanupFiles,
                eventHandlers::handleAbout,
                eventHandlers::handleHelpTopics
        );
        root.setTop(menuBar);

        // 중앙 컨텐츠 영역
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        return root;
    }

    /**
     * 중앙 컨텐츠 영역 생성
     */
    private VBox createCenterContent() {
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(30));
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setStyle("-fx-background-color: #f8f9fa;");

        // 제목 섹션
        VBox titleSection = UIFactory.createTitleSection();

        // 버튼 섹션
        HBox buttonSection = UIFactory.createButtonSection(
                eventHandlers::handleScanFiles,
                eventHandlers::handleOrganizeFiles,
                eventHandlers::handleSettings
        );

        // 파일 목록 테이블 섹션
        VBox tableSection = UIFactory.createTableSection(fileList);

        // 상태 섹션
        HBox statusSection = UIFactory.createStatusSection();

        centerContent.getChildren().addAll(titleSection, buttonSection, tableSection, statusSection);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        return centerContent;
    }

    /**
     * UI 컴포넌트 참조 설정
     */
    private void setupUIReferences(BorderPane root) {
        // ID를 통해 UI 컴포넌트들 찾기
        progressBar = (ProgressBar) root.lookup("#progressBar");
        statusLabel = (Label) root.lookup("#statusLabel");
        progressLabel = (Label) root.lookup("#progressLabel");
        organizeButton = (Button) root.lookup("#organizeButton");
        fileTable = (TableView<FileInfo>) root.lookup("#fileTable");
        fileDetailPanel = (VBox) root.lookup("#fileDetailPanel");

        // 컴포넌트가 정상적으로 찾아졌는지 확인
        if (progressBar == null || statusLabel == null || progressLabel == null ||
                organizeButton == null || fileTable == null || fileDetailPanel == null) {
            System.err.println("[WARNING] Some UI components could not be found by ID");
            System.err.println("ProgressBar: " + (progressBar != null ? "✓" : "✗"));
            System.err.println("StatusLabel: " + (statusLabel != null ? "✓" : "✗"));
            System.err.println("ProgressLabel: " + (progressLabel != null ? "✓" : "✗"));
            System.err.println("OrganizeButton: " + (organizeButton != null ? "✓" : "✗"));
            System.err.println("FileTable: " + (fileTable != null ? "✓" : "✗"));
            System.err.println("FileDetailPanel: " + (fileDetailPanel != null ? "✓" : "✗"));
        } else {
            System.out.println("[SUCCESS] All UI components found successfully");
        }
    }

    /**
     * 스테이지 설정
     */
    private void setupStage(Stage primaryStage, BorderPane root) {
        // 씬 생성
        Scene scene = new Scene(root, 900, 700);

        // CSS 스타일 적용 (만약 있다면)
        try {
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("[INFO] CSS file not found, using default styles");
        }

        // 스테이지 설정
        primaryStage.setTitle(Messages.get("app.window.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}