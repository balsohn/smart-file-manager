package com.smartfilemanager.manager;

import com.smartfilemanager.constants.UIConstants;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.util.FileFormatUtils;
import com.smartfilemanager.util.FileIconUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * 테이블 설정 및 셀 렌더링을 관리하는 클래스
 */
public class TableConfigManager {
    
    private final TableView<FileInfo> fileTable;
    private final TableColumn<FileInfo, String> nameColumn;
    private final TableColumn<FileInfo, String> categoryColumn;
    private final TableColumn<FileInfo, String> sizeColumn;
    private final TableColumn<FileInfo, String> statusColumn;
    private final TableColumn<FileInfo, String> dateColumn;
    private final TableColumn<FileInfo, Double> confidenceColumn;
    private final TableColumn<FileInfo, Boolean> selectColumn;
    
    public TableConfigManager(TableView<FileInfo> fileTable,
                             TableColumn<FileInfo, String> nameColumn,
                             TableColumn<FileInfo, String> categoryColumn,
                             TableColumn<FileInfo, String> sizeColumn,
                             TableColumn<FileInfo, String> statusColumn,
                             TableColumn<FileInfo, String> dateColumn,
                             TableColumn<FileInfo, Double> confidenceColumn,
                             TableColumn<FileInfo, Boolean> selectColumn) {
        this.fileTable = fileTable;
        this.nameColumn = nameColumn;
        this.categoryColumn = categoryColumn;
        this.sizeColumn = sizeColumn;
        this.statusColumn = statusColumn;
        this.dateColumn = dateColumn;
        this.confidenceColumn = confidenceColumn;
        this.selectColumn = selectColumn;
    }
    
    /**
     * 테이블 전체 설정
     */
    public void setupTable(ObservableList<FileInfo> fileList) {
        configureTableColumns();
        setupCellValueFactories();
        setupCellRendering();
        
        fileTable.setItems(fileList);
        fileTable.getSortOrder().add(nameColumn);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    /**
     * 테이블 컬럼 크기 설정
     */
    private void configureTableColumns() {
        nameColumn.setPrefWidth(UIConstants.TableColumnWidths.NAME_COLUMN);
        nameColumn.setMinWidth(UIConstants.TableColumnWidths.NAME_COLUMN_MIN);
        categoryColumn.setPrefWidth(UIConstants.TableColumnWidths.CATEGORY_COLUMN);
        sizeColumn.setPrefWidth(UIConstants.TableColumnWidths.SIZE_COLUMN);
        statusColumn.setPrefWidth(UIConstants.TableColumnWidths.STATUS_COLUMN);
        confidenceColumn.setPrefWidth(UIConstants.TableColumnWidths.CONFIDENCE_COLUMN);
        dateColumn.setPrefWidth(UIConstants.TableColumnWidths.DATE_COLUMN);
    }
    
    /**
     * 셀 값 팩토리 설정
     */
    private void setupCellValueFactories() {
        // 체크박스 컬럼 설정
        selectColumn.setCellValueFactory(cellData -> {
            FileInfo fileInfo = cellData.getValue();
            return new SimpleObjectProperty<>(fileInfo.isSelected());
        });
        
        nameColumn.setCellValueFactory(cellData -> {
            FileInfo fileInfo = cellData.getValue();
            String fileName = FileFormatUtils.getFileNameSafe(fileInfo);
            String icon = FileIconUtils.getFileIcon(fileInfo);
            return new SimpleStringProperty(icon + " " + fileName);
        });
        
        categoryColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(FileFormatUtils.formatCategory(cellData.getValue())));
        
        sizeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(FileFormatUtils.formatFileSize(cellData.getValue().getFileSize())));
        
        statusColumn.setCellValueFactory(cellData -> {
            FileInfo fileInfo = cellData.getValue();
            String statusName = FileFormatUtils.getStatusDisplayName(fileInfo);
            String icon = FileIconUtils.getStatusIcon(fileInfo.getStatus());
            return new SimpleStringProperty(icon + " " + statusName);
        });
        
        dateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(FileFormatUtils.formatTableDate(cellData.getValue().getModifiedDate())));
        
        confidenceColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getConfidenceScore()));
    }
    
    /**
     * 셀 렌더링 설정 (색상 및 스타일)
     */
    private void setupCellRendering() {
        setupCheckBoxColumnRendering();
        setupStatusColumnRendering();
        setupConfidenceColumnRendering();
    }
    
    /**
     * 체크박스 컬럼 렌더링
     */
    private void setupCheckBoxColumnRendering() {
        selectColumn.setCellFactory(column -> new TableCell<FileInfo, Boolean>() {
            private CheckBox checkBox;
            
            {
                // 셀이 생성될 때 체크박스 초기화
                checkBox = new CheckBox();
                checkBox.setOnAction(e -> {
                    FileInfo fileInfo = getTableRow().getItem();
                    if (fileInfo != null) {
                        fileInfo.setSelected(checkBox.isSelected());
                        System.out.println("체크박스 클릭: " + fileInfo.getFileName() + " -> " + checkBox.isSelected());
                        fireSelectionChangeEvent();
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    FileInfo fileInfo = getTableRow().getItem();
                    if (fileInfo != null) {
                        // 이벤트 핸들러를 임시로 제거하여 무한 루프 방지
                        checkBox.setOnAction(null);
                        checkBox.setSelected(fileInfo.isSelected());
                        // 이벤트 핸들러 다시 설정
                        checkBox.setOnAction(e -> {
                            fileInfo.setSelected(checkBox.isSelected());
                            System.out.println("체크박스 클릭: " + fileInfo.getFileName() + " -> " + checkBox.isSelected());
                            fireSelectionChangeEvent();
                        });
                        setGraphic(checkBox);
                        setText(null);
                    }
                }
            }
        });
    }
    
    /**
     * 상태 컬럼 색상 렌더링
     */
    private void setupStatusColumnRendering() {
        statusColumn.setCellFactory(column -> new TableCell<FileInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // 테이블 행에서 FileInfo 가져오기
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        FileInfo fileInfo = getTableView().getItems().get(index);
                        if (fileInfo != null && fileInfo.getStatus() != null) {
                            String color = FileIconUtils.getStatusColor(fileInfo.getStatus());
                            setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });
    }
    
    /**
     * 신뢰도 컬럼 색상 렌더링
     */
    private void setupConfidenceColumnRendering() {
        confidenceColumn.setCellFactory(column -> new TableCell<FileInfo, Double>() {
            @Override
            protected void updateItem(Double confidence, boolean empty) {
                super.updateItem(confidence, empty);
                if (empty || confidence == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 테이블 행에서 FileInfo 가져오기
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        FileInfo fileInfo = getTableView().getItems().get(index);
                        if (fileInfo != null) {
                            boolean isAIAnalyzed = FileIconUtils.isAIAnalyzed(fileInfo);
                            String confidenceText = FileFormatUtils.formatConfidenceWithAI(confidence, isAIAnalyzed);
                            setText(confidenceText);
                            
                            String color = FileIconUtils.getConfidenceColor(confidence);
                            setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        } else {
                            setText(FileFormatUtils.formatConfidence(confidence));
                            String color = FileIconUtils.getConfidenceColor(confidence);
                            setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        }
                    } else {
                        setText(FileFormatUtils.formatConfidence(confidence));
                        String color = FileIconUtils.getConfidenceColor(confidence);
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }
    
    /**
     * 테이블 새로고침
     */
    public void refreshTable() {
        fileTable.refresh();
    }
    
    /**
     * 선택 변경 이벤트 발생 (체크박스 상태 변경 시)
     */
    private void fireSelectionChangeEvent() {
        // 테이블 새로고침으로 UI 업데이트
        fileTable.refresh();
        
        // 선택된 파일 수 업데이트를 위한 이벤트 발생
        if (selectionChangeCallback != null) {
            selectionChangeCallback.run();
        }
    }
    
    // 선택 변경 콜백
    private Runnable selectionChangeCallback;
    
    /**
     * 선택 변경 콜백 설정
     */
    public void setSelectionChangeCallback(Runnable callback) {
        this.selectionChangeCallback = callback;
    }
    
    /**
     * 테이블 선택 해제
     */
    public void clearSelection() {
        fileTable.getSelectionModel().clearSelection();
    }
    
    /**
     * 선택된 항목 가져오기
     */
    public FileInfo getSelectedItem() {
        return fileTable.getSelectionModel().getSelectedItem();
    }
    
    /**
     * 선택 리스너 설정
     */
    public void setSelectionListener(javafx.beans.value.ChangeListener<FileInfo> listener) {
        fileTable.getSelectionModel().selectedItemProperty().addListener(listener);
    }
}