package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.util.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 향상된 통계 화면 컨트롤러
 * 다양한 차트와 분석 정보를 제공합니다
 */
@Slf4j
public class StatisticsController implements Initializable {

    // FXML 컴포넌트
    @FXML private TabPane statisticsTabPane;

    // 개요 탭
    @FXML private Label totalFilesLabel;
    @FXML private Label organizedFilesLabel;
    @FXML private Label spaceSavedLabel;
    @FXML private Label duplicatesFoundLabel;
    @FXML private ProgressBar organizationProgress;

    // 카테고리 분석 탭
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private CategoryAxis categoryAxis;
    @FXML private NumberAxis categoryNumberAxis;

    // 시간대별 분석 탭
    @FXML private LineChart<String, Number> timelineChart;
    @FXML private CategoryAxis timelineAxis;
    @FXML private NumberAxis timelineNumberAxis;
    @FXML private ComboBox<String> timePeriodComboBox;

    // 작업 히스토리 탭
    @FXML private TableView<HistoryEntry> historyTable;
    @FXML private TableColumn<HistoryEntry, String> dateColumn;
    @FXML private TableColumn<HistoryEntry, String> actionColumn;
    @FXML private TableColumn<HistoryEntry, String> fileCountColumn;
    @FXML private TableColumn<HistoryEntry, String> sizeColumn;

    // 상세 분석 탭
    @FXML private VBox detailAnalysisContainer;
    @FXML private TreeView<String> folderStructureTree;

    // 서비스
    private DatabaseHelper databaseHelper;
    private ConfigService configService;

    // 데이터
    private ObservableList<FileInfo> allFiles = FXCollections.observableArrayList();
    private ObservableList<HistoryEntry> historyEntries = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeServices();
        setupCharts();
        setupHistoryTable();
        setupEventHandlers();
        loadInitialData();
    }

    /**
     * 서비스 초기화
     */
    private void initializeServices() {
        this.databaseHelper = new DatabaseHelper();
        this.configService = new ConfigService();
    }

    /**
     * 차트 초기 설정
     */
    private void setupCharts() {
        // 카테고리 파이 차트 설정
        categoryPieChart.setTitle("파일 카테고리별 분포");
        categoryPieChart.setLabelLineLength(10);
        categoryPieChart.setLegendSide(Side.RIGHT);

        // 카테고리 바 차트 설정
        categoryBarChart.setTitle("카테고리별 파일 수");
        categoryAxis.setLabel("파일 카테고리");
        categoryNumberAxis.setLabel("파일 수");

        // 시간대별 라인 차트 설정
        timelineChart.setTitle("시간대별 파일 정리 현황");
        timelineAxis.setLabel("날짜");
        timelineNumberAxis.setLabel("정리된 파일 수");
        timelineChart.setCreateSymbols(true);

        // 시간 범위 선택 콤보박스
        timePeriodComboBox.setItems(FXCollections.observableArrayList(
                "최근 7일", "최근 30일", "최근 3개월", "최근 1년", "전체"
        ));
        timePeriodComboBox.setValue("최근 30일");
    }

    /**
     * 히스토리 테이블 설정
     */
    private void setupHistoryTable() {
        dateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                ));

        actionColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAction()));

        fileCountColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getFileCount())
                ));

        sizeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        formatFileSize(cellData.getValue().getTotalSize())
                ));

        // 테이블 스타일링
        historyTable.setRowFactory(tv -> {
            TableRow<HistoryEntry> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    switch (newItem.getAction()) {
                        case "정리 완료":
                            row.setStyle("-fx-background-color: #e8f5e8;");
                            break;
                        case "되돌리기":
                            row.setStyle("-fx-background-color: #fff3e0;");
                            break;
                        case "중복 제거":
                            row.setStyle("-fx-background-color: #e3f2fd;");
                            break;
                        default:
                            row.setStyle("");
                    }
                }
            });
            return row;
        });

        historyTable.setItems(historyEntries);
    }

    /**
     * 이벤트 핸들러 설정
     */
    private void setupEventHandlers() {
        timePeriodComboBox.setOnAction(e -> updateTimelineChart());

        // 탭 변경 시 데이터 새로고침
        statisticsTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> {
                    if (newTab != null) {
                        refreshTabData(newTab.getText());
                    }
                }
        );
    }

    /**
     * 초기 데이터 로드
     */
    private void loadInitialData() {
        // 데이터베이스에서 통계 데이터 로드
        Map<String, Object> stats = databaseHelper.getStatistics(30);
        updateOverviewStats(stats);

        // 히스토리 데이터 로드
        loadHistoryData();

        // 차트 데이터 업데이트
        updateAllCharts();
    }

    /**
     * 파일 리스트 업데이트 (메인 컨트롤러에서 호출)
     */
    public void updateFileList(ObservableList<FileInfo> files) {
        this.allFiles.clear();
        this.allFiles.addAll(files);
        updateAllCharts();
        updateOverviewFromFiles();
    }

    /**
     * 개요 통계 업데이트
     */
    private void updateOverviewStats(Map<String, Object> stats) {
        totalFilesLabel.setText(String.valueOf(stats.getOrDefault("total_scanned", 0)));
        organizedFilesLabel.setText(String.valueOf(stats.getOrDefault("total_organized", 0)));
        spaceSavedLabel.setText(formatFileSize((Long) stats.getOrDefault("total_space_saved", 0L)));
        duplicatesFoundLabel.setText(String.valueOf(stats.getOrDefault("total_duplicates", 0)));

        // 진행률 계산
        int total = (Integer) stats.getOrDefault("total_scanned", 1);
        int organized = (Integer) stats.getOrDefault("total_organized", 0);
        double progress = total > 0 ? (double) organized / total : 0.0;
        organizationProgress.setProgress(progress);
    }

    /**
     * 파일 리스트로부터 개요 업데이트
     */
    private void updateOverviewFromFiles() {
        if (allFiles.isEmpty()) return;

        long totalFiles = allFiles.size();
        long organizedFiles = allFiles.stream()
                .filter(f -> f.getStatus() == ProcessingStatus.ORGANIZED)
                .count();

        long totalSize = allFiles.stream()
                .mapToLong(FileInfo::getFileSize)
                .sum();

        totalFilesLabel.setText(String.valueOf(totalFiles));
        organizedFilesLabel.setText(String.valueOf(organizedFiles));

        double progress = totalFiles > 0 ? (double) organizedFiles / totalFiles : 0.0;
        organizationProgress.setProgress(progress);
    }

    /**
     * 모든 차트 업데이트
     */
    private void updateAllCharts() {
        updateCategoryCharts();
        updateTimelineChart();
        updateFolderStructure();
    }

    /**
     * 카테고리 차트 업데이트
     */
    private void updateCategoryCharts() {
        if (allFiles.isEmpty()) return;

        // 카테고리별 분류
        Map<String, Long> categoryCount = allFiles.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "미분류",
                        Collectors.counting()
                ));

        // 파이 차트 데이터
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        categoryCount.forEach((category, count) ->
                pieData.add(new PieChart.Data(category + " (" + count + "개)", count))
        );
        categoryPieChart.setData(pieData);

        // 바 차트 데이터
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("파일 수");

        categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                        barSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()))
                );

        categoryBarChart.getData().clear();
        categoryBarChart.getData().add(barSeries);
    }

    /**
     * 시간대별 차트 업데이트
     */
    private void updateTimelineChart() {
        String period = timePeriodComboBox.getValue();
        int days = getDaysFromPeriod(period);

        // 데이터베이스에서 시간대별 데이터 조회
        Map<LocalDate, Integer> timelineData = getTimelineData(days);

        XYChart.Series<String, Number> timelineSeries = new XYChart.Series<>();
        timelineSeries.setName("정리된 파일 수");

        timelineData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        timelineSeries.getData().add(new XYChart.Data<>(
                                entry.getKey().format(DateTimeFormatter.ofPattern("MM-dd")),
                                entry.getValue()
                        ))
                );

        timelineChart.getData().clear();
        timelineChart.getData().add(timelineSeries);
    }

    /**
     * 폴더 구조 트리 업데이트
     */
    private void updateFolderStructure() {
        TreeItem<String> rootItem = new TreeItem<>("정리된 파일 구조");
        rootItem.setExpanded(true);

        Map<String, Map<String, Integer>> folderStructure = analyzeFolderStructure();

        folderStructure.forEach((category, subfolders) -> {
            TreeItem<String> categoryItem = new TreeItem<>(category);
            categoryItem.setExpanded(true);

            subfolders.forEach((subfolder, count) -> {
                TreeItem<String> subfolderItem = new TreeItem<>(
                        subfolder + " (" + count + "개 파일)"
                );
                categoryItem.getChildren().add(subfolderItem);
            });

            rootItem.getChildren().add(categoryItem);
        });

        folderStructureTree.setRoot(rootItem);
    }

    /**
     * 히스토리 데이터 로드
     */
    private void loadHistoryData() {
        // 실제 구현에서는 데이터베이스에서 로드
        historyEntries.clear();

        // 예시 데이터 (실제로는 DatabaseHelper에서 조회)
        historyEntries.addAll(
                List.of(
                        new HistoryEntry(LocalDate.now().atTime(14, 30), "정리 완료", 25, 245760000L),
                        new HistoryEntry(LocalDate.now().minusDays(1).atTime(16, 15), "중복 제거", 8, 156430000L),
                        new HistoryEntry(LocalDate.now().minusDays(2).atTime(10, 45), "정리 완료", 42, 589120000L),
                        new HistoryEntry(LocalDate.now().minusDays(3).atTime(9, 20), "되돌리기", 5, 23450000L)
                )
        );
    }

    /**
     * 탭별 데이터 새로고침
     */
    private void refreshTabData(String tabName) {
        switch (tabName) {
            case "개요":
                updateOverviewFromFiles();
                break;
            case "카테고리 분석":
                updateCategoryCharts();
                break;
            case "시간대별 분석":
                updateTimelineChart();
                break;
            case "작업 히스토리":
                loadHistoryData();
                break;
            case "상세 분석":
                updateFolderStructure();
                break;
        }
    }

    /**
     * 유틸리티 메서드들
     */
    private int getDaysFromPeriod(String period) {
        return switch (period) {
            case "최근 7일" -> 7;
            case "최근 30일" -> 30;
            case "최근 3개월" -> 90;
            case "최근 1년" -> 365;
            default -> Integer.MAX_VALUE;
        };
    }

    private Map<LocalDate, Integer> getTimelineData(int days) {
        // 실제 구현에서는 데이터베이스 쿼리
        Map<LocalDate, Integer> data = new HashMap<>();
        LocalDate now = LocalDate.now();

        for (int i = 0; i < Math.min(days, 30); i++) {
            LocalDate date = now.minusDays(i);
            data.put(date, (int) (Math.random() * 50)); // 예시 데이터
        }

        return data;
    }

    private Map<String, Map<String, Integer>> analyzeFolderStructure() {
        return allFiles.stream()
                .filter(f -> f.getDetectedCategory() != null)
                .collect(Collectors.groupingBy(
                        FileInfo::getDetectedCategory,
                        Collectors.groupingBy(
                                f -> f.getDetectedSubCategory() != null ? f.getDetectedSubCategory() : "기타",
                                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                        )
                ));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 히스토리 엔트리 클래스
     */
    public static class HistoryEntry {
        private final java.time.LocalDateTime date;
        private final String action;
        private final int fileCount;
        private final long totalSize;

        public HistoryEntry(java.time.LocalDateTime date, String action, int fileCount, long totalSize) {
            this.date = date;
            this.action = action;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
        }

        // Getters
        public java.time.LocalDateTime getDate() { return date; }
        public String getAction() { return action; }
        public int getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
    }
}