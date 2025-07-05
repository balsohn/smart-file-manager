package com.smartfilemanager.controller;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.service.FileScanService;
import com.smartfilemanager.ui.FileDetailManager;
import com.smartfilemanager.ui.UIFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Event handlers for the Smart File Manager application
 * Separates event handling logic from the main application class
 */
public class EventHandlers {

    private final Stage primaryStage;
    private final ObservableList<FileInfo> fileList;

    // UI Components
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Button organizeButton;
    private TableView<FileInfo> fileTable;

    // Services
    private FileScanService fileScanService;
    private FileDetailManager fileDetailManager;

    public EventHandlers(Stage primaryStage, ObservableList<FileInfo> fileList) {
        this.primaryStage = primaryStage;
        this.fileList = fileList;
    }

    /**
     * Set UI components after initialization
     */
    public void setUIComponents(ProgressBar progressBar, Label statusLabel, Label progressLabel,
                                Button organizeButton, TableView<FileInfo> fileTable, VBox detailPanel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
        this.progressLabel = progressLabel;
        this.organizeButton = organizeButton;
        this.fileTable = fileTable;

        // Initialize services
        initializeServices(detailPanel);

        // Setup table selection listener
        setupTableSelectionListener();
    }

    /**
     * Initialize services
     */
    private void initializeServices(VBox detailPanel) {
        if (progressBar == null || statusLabel == null || progressLabel == null) {
            throw new IllegalStateException("UI components must be set before initializing services");
        }

        this.fileScanService = new FileScanService(progressBar, statusLabel, progressLabel, fileList);
        this.fileDetailManager = new FileDetailManager(detailPanel);
    }

    /**
     * Setup table selection event listener
     */
    private void setupTableSelectionListener() {
        if (fileTable != null) {
            fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                fileDetailManager.updateFileDetails(newSelection);
            });
        }
    }

    /**
     * Handle open folder action
     */
    public void handleOpenFolder() {
        System.out.println("[INFO] Open Folder clicked");

        // Create and configure DirectoryChooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Organize");

        // Set default directory to user's home/Downloads
        String userHome = System.getProperty("user.home");
        File defaultDirectory = new File(userHome, "Downloads");
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        } else {
            directoryChooser.setInitialDirectory(new File(userHome));
        }

        // Show folder selection dialog
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory != null) {
            System.out.println("[INFO] Selected folder: " + selectedDirectory.getAbsolutePath());
            startFileScan(selectedDirectory);
        } else {
            System.out.println("[INFO] Folder selection cancelled");
        }
    }

    /**
     * Start file scanning process
     */
    private void startFileScan(File directory) {
        // Clear existing selection and hide details
        if (fileTable != null) {
            fileTable.getSelectionModel().clearSelection();
        }
        fileDetailManager.hideDetails();

        // Disable organize button
        if (organizeButton != null) {
            organizeButton.setDisable(true);
        }

        // Start scanning
        fileScanService.startFileScan(directory);

        // Add listener to enable organize button when files are ready
        fileList.addListener((javafx.collections.ListChangeListener<FileInfo>) change -> {
            if (organizeButton != null) {
                boolean hasProcessableFiles = fileList.stream().anyMatch(file -> file.getStatus().isProcessable());
                organizeButton.setDisable(!hasProcessableFiles);
            }
        });
    }

    /**
     * Handle settings action
     */
    public void handleSettings() {
        System.out.println("[INFO] Settings clicked");
        UIFactory.showInfoDialog("Settings", "Settings dialog will be implemented later.");
    }

    /**
     * Handle scan files action (scan button)
     */
    public void handleScanFiles() {
        System.out.println("[INFO] Scan Files clicked");
        handleOpenFolder();
    }

    /**
     * Handle organize files action (enhanced version)
     */
    public void handleOrganizeFiles() {
        System.out.println("[INFO] Organize Files clicked");

        // Check if there are files to organize
        long analyzedCount = fileList.stream()
                .filter(file -> file.getStatus().isProcessable())
                .count();

        if (analyzedCount == 0) {
            UIFactory.showInfoDialog("No Files to Organize", "Please scan a folder first to find files to organize.");
            return;
        }

        // Detailed statistics
        long totalFiles = fileList.size();
        long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
        String formattedSize = formatFileSize(totalSize);

        // Category analysis
        java.util.Map<String, Long> categoryCount = fileList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        StringBuilder message = new StringBuilder();
        message.append("Ready to organize ").append(analyzedCount).append(" files out of ").append(totalFiles).append(" total files.\n");
        message.append("Total size: ").append(formattedSize).append("\n\n");
        message.append("Files by category:\n");

        categoryCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> message.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" files\n"));

        message.append("\nThis action will move files to organized folders. Continue?");

        // Confirmation dialog
        boolean confirmed = UIFactory.showConfirmDialog("Organize Files", message.toString());

        if (confirmed) {
            UIFactory.showInfoDialog("Organize Files",
                    "File organization feature will be implemented in Phase 4.\n" +
                            "For now, this shows the organization preview.");
        }
    }

    /**
     * Handle find duplicates action (enhanced version)
     */
    public void handleFindDuplicates() {
        System.out.println("[INFO] Find Duplicates clicked");

        if (fileList.isEmpty()) {
            UIFactory.showInfoDialog("No Files", "Please scan a folder first to find duplicate files.");
            return;
        }

        // Temporary duplicate possibility analysis
        long duplicateCandidates = fileList.stream()
                .collect(java.util.stream.Collectors.groupingBy(FileInfo::getFileSize))
                .values().stream()
                .filter(group -> group.size() > 1)
                .mapToLong(java.util.List::size)
                .sum();

        if (duplicateCandidates == 0) {
            UIFactory.showInfoDialog("No Duplicates Found", "No potential duplicate files were found based on file size.");
        } else {
            UIFactory.showInfoDialog("Potential Duplicates",
                    "Found " + duplicateCandidates + " files with identical sizes.\n" +
                            "Advanced duplicate detection will be implemented in Phase 6.");
        }
    }

    /**
     * Handle about action
     */
    public void handleAbout() {
        StringBuilder about = new StringBuilder();
        about.append("Smart File Manager v1.0\n");
        about.append("AI-powered File Organization Tool\n\n");

        about.append("Current Statistics:\n");
        if (!fileList.isEmpty()) {
            long totalSize = fileList.stream().mapToLong(FileInfo::getFileSize).sum();
            about.append("• Files analyzed: ").append(fileList.size()).append("\n");
            about.append("• Total size: ").append(formatFileSize(totalSize)).append("\n");

            long organizedCount = fileList.stream().filter(f -> f.getStatus().isCompleted()).count();
            about.append("• Files organized: ").append(organizedCount).append("\n");
        } else {
            about.append("• No files analyzed yet\n");
        }

        about.append("\nBuilt with JavaFX\n");
        about.append("© 2024 Smart File Manager");

        UIFactory.showInfoDialog("About Smart File Manager", about.toString());
    }

    /**
     * Handle help topics action
     */
    public void handleHelpTopics() {
        StringBuilder help = new StringBuilder();
        help.append("Smart File Manager Help\n\n");

        help.append("Quick Start:\n");
        help.append("1. Click 'Scan Folder' to analyze files\n");
        help.append("2. Review the file list and categories\n");
        help.append("3. Click 'Organize Files' to sort them\n\n");

        help.append("Tips:\n");
        help.append("• Right-click files for context menu\n");
        help.append("• Click column headers to sort\n");
        help.append("• Select files to view details\n");
        help.append("• Use Ctrl+O for quick folder selection\n\n");

        help.append("Keyboard Shortcuts:\n");
        help.append("• Ctrl+O: Open Folder\n");
        help.append("• F5: Scan Files\n");
        help.append("• F6: Organize Files\n");
        help.append("• F7: Find Duplicates\n");
        help.append("• Ctrl+Q: Exit\n");

        UIFactory.showInfoDialog("Help Topics", help.toString());
    }

    /**
     * File size formatting utility
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Get file detail manager for testing
     */
    public FileDetailManager getFileDetailManager() {
        return fileDetailManager;
    }
}