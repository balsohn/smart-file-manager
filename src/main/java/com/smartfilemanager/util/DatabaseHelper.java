package com.smartfilemanager.util;

import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite 데이터베이스 관리 헬퍼 클래스
 * 파일 처리 히스토리, 통계, 설정을 로컬 데이터베이스에 저장합니다
 */
public class DatabaseHelper {

    private static final String DB_DIR = System.getProperty("user.home") +
            File.separator + ".smartfilemanager";
    private static final String DB_PATH = DB_DIR + File.separator + "smartfilemanager.db";

    private Connection connection;

    public DatabaseHelper() {
        initializeDatabase();
    }

    /**
     * 데이터베이스 초기화
     */
    private void initializeDatabase() {
        try {
            // 데이터베이스 디렉토리 생성
            File dbDir = new File(DB_DIR);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
                System.out.println("[INFO] 데이터베이스 디렉토리 생성: " + DB_DIR);
            }

            // SQLite 연결
            String url = "jdbc:sqlite:" + DB_PATH;
            connection = DriverManager.getConnection(url);

            // 외래 키 지원 활성화
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            // 테이블 생성
            createTables();

            System.out.println("[SUCCESS] 데이터베이스 초기화 완료: " + DB_PATH);

        } catch (SQLException e) {
            System.err.println("[ERROR] 데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 데이터베이스 테이블 생성
     */
    private void createTables() throws SQLException {
        // 파일 처리 히스토리 테이블
        String createFileHistoryTable = """
            CREATE TABLE IF NOT EXISTS file_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_name TEXT NOT NULL,
                file_path TEXT NOT NULL,
                original_location TEXT,
                target_location TEXT,
                file_size INTEGER DEFAULT 0,
                file_extension TEXT,
                detected_category TEXT,
                detected_subcategory TEXT,
                operation_type TEXT NOT NULL,
                status TEXT NOT NULL,
                confidence_score REAL DEFAULT 0.0,
                error_message TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                processed_at TIMESTAMP
            )
        """;

        // 애플리케이션 통계 테이블
        String createStatisticsTable = """
            CREATE TABLE IF NOT EXISTS app_statistics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stat_date DATE NOT NULL,
                files_scanned INTEGER DEFAULT 0,
                files_organized INTEGER DEFAULT 0,
                files_failed INTEGER DEFAULT 0,
                duplicates_found INTEGER DEFAULT 0,
                space_saved INTEGER DEFAULT 0,
                categories_detected TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(stat_date)
            )
        """;

        // 사용자 정의 분류 규칙 테이블
        String createRulesTable = """
            CREATE TABLE IF NOT EXISTS classification_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rule_name TEXT NOT NULL UNIQUE,
                rule_type TEXT NOT NULL,
                condition_pattern TEXT NOT NULL,
                target_category TEXT NOT NULL,
                target_subcategory TEXT,
                target_folder TEXT,
                priority INTEGER DEFAULT 0,
                is_active BOOLEAN DEFAULT 1,
                is_user_defined BOOLEAN DEFAULT 1,
                applied_count INTEGER DEFAULT 0,
                success_count INTEGER DEFAULT 0,
                accuracy_score REAL DEFAULT 0.0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_used TIMESTAMP
            )
        """;

        // 정리 작업 세션 테이블
        String createSessionsTable = """
            CREATE TABLE IF NOT EXISTS organize_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL UNIQUE,
                source_folder TEXT NOT NULL,
                target_folder TEXT NOT NULL,
                total_files INTEGER DEFAULT 0,
                successful_files INTEGER DEFAULT 0,
                failed_files INTEGER DEFAULT 0,
                total_size INTEGER DEFAULT 0,
                session_status TEXT DEFAULT 'IN_PROGRESS',
                started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                completed_at TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createFileHistoryTable);
            stmt.execute(createStatisticsTable);
            stmt.execute(createRulesTable);
            stmt.execute(createSessionsTable);

            System.out.println("[SUCCESS] 데이터베이스 테이블 생성 완료");
        }
    }

    /**
     * 파일 처리 히스토리 저장
     */
    public void saveFileOperation(FileInfo fileInfo, String operationType) {
        String sql = """
            INSERT INTO file_history 
            (file_name, file_path, original_location, target_location, file_size, 
             file_extension, detected_category, detected_subcategory, operation_type, 
             status, confidence_score, error_message, processed_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fileInfo.getFileName());
            pstmt.setString(2, fileInfo.getFilePath());
            pstmt.setString(3, fileInfo.getOriginalLocation());
            pstmt.setString(4, fileInfo.getSuggestedPath());
            pstmt.setLong(5, fileInfo.getFileSize());
            pstmt.setString(6, fileInfo.getFileExtension());
            pstmt.setString(7, fileInfo.getDetectedCategory());
            pstmt.setString(8, fileInfo.getDetectedSubCategory());
            pstmt.setString(9, operationType);
            pstmt.setString(10, fileInfo.getStatus().name());
            pstmt.setDouble(11, fileInfo.getConfidenceScore());
            pstmt.setString(12, fileInfo.getErrorMessage());

            if (fileInfo.getProcessedAt() != null) {
                pstmt.setTimestamp(13, Timestamp.valueOf(fileInfo.getProcessedAt()));
            } else {
                pstmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            }

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[ERROR] 파일 작업 기록 실패: " + e.getMessage());
        }
    }

    /**
     * 파일 처리 히스토리 조회
     */
    public List<FileOperationHistory> getFileHistory(int limit) {
        String sql = """
            SELECT file_name, file_path, original_location, target_location, 
                   operation_type, status, created_at, processed_at
            FROM file_history 
            ORDER BY created_at DESC 
            LIMIT ?
        """;

        List<FileOperationHistory> history = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FileOperationHistory record = new FileOperationHistory();
                    record.setFileName(rs.getString("file_name"));
                    record.setFilePath(rs.getString("file_path"));
                    record.setOriginalLocation(rs.getString("original_location"));
                    record.setTargetLocation(rs.getString("target_location"));
                    record.setOperationType(rs.getString("operation_type"));
                    record.setStatus(rs.getString("status"));
                    record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                    Timestamp processedAt = rs.getTimestamp("processed_at");
                    if (processedAt != null) {
                        record.setProcessedAt(processedAt.toLocalDateTime());
                    }

                    history.add(record);
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 히스토리 조회 실패: " + e.getMessage());
        }

        return history;
    }

    /**
     * 일일 통계 업데이트
     */
    public void updateDailyStatistics(int filesScanned, int filesOrganized,
                                      int filesFailed, int duplicatesFound,
                                      long spaceSaved, String categoriesDetected) {
        String sql = """
            INSERT OR REPLACE INTO app_statistics 
            (stat_date, files_scanned, files_organized, files_failed, 
             duplicates_found, space_saved, categories_detected) 
            VALUES (DATE('now'), ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, filesScanned);
            pstmt.setInt(2, filesOrganized);
            pstmt.setInt(3, filesFailed);
            pstmt.setInt(4, duplicatesFound);
            pstmt.setLong(5, spaceSaved);
            pstmt.setString(6, categoriesDetected);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[ERROR] 통계 업데이트 실패: " + e.getMessage());
        }
    }

    /**
     * 통계 정보 조회
     */
    public Map<String, Object> getStatistics(int days) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                SUM(files_scanned) as total_scanned,
                SUM(files_organized) as total_organized,
                SUM(files_failed) as total_failed,
                SUM(duplicates_found) as total_duplicates,
                SUM(space_saved) as total_space_saved,
                COUNT(*) as active_days
            FROM app_statistics 
            WHERE stat_date >= DATE('now', '-' || ? || ' days')
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalScanned", rs.getInt("total_scanned"));
                    stats.put("totalOrganized", rs.getInt("total_organized"));
                    stats.put("totalFailed", rs.getInt("total_failed"));
                    stats.put("totalDuplicates", rs.getInt("total_duplicates"));
                    stats.put("totalSpaceSaved", rs.getLong("total_space_saved"));
                    stats.put("activeDays", rs.getInt("active_days"));
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 통계 조회 실패: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 새 정리 세션 시작
     */
    public String startOrganizeSession(String sessionId, String sourceFolder, String targetFolder) {
        String sql = """
            INSERT INTO organize_sessions 
            (session_id, source_folder, target_folder, session_status) 
            VALUES (?, ?, ?, 'IN_PROGRESS')
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, sourceFolder);
            pstmt.setString(3, targetFolder);

            pstmt.executeUpdate();
            System.out.println("[INFO] 정리 세션 시작: " + sessionId);
            return sessionId;

        } catch (SQLException e) {
            System.err.println("[ERROR] 세션 생성 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 정리 세션 완료
     */
    public void completeOrganizeSession(String sessionId, int totalFiles,
                                        int successfulFiles, int failedFiles,
                                        long totalSize) {
        String sql = """
            UPDATE organize_sessions 
            SET total_files = ?, successful_files = ?, failed_files = ?, 
                total_size = ?, session_status = 'COMPLETED', 
                completed_at = CURRENT_TIMESTAMP 
            WHERE session_id = ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, totalFiles);
            pstmt.setInt(2, successfulFiles);
            pstmt.setInt(3, failedFiles);
            pstmt.setLong(4, totalSize);
            pstmt.setString(5, sessionId);

            pstmt.executeUpdate();
            System.out.println("[INFO] 정리 세션 완료: " + sessionId);

        } catch (SQLException e) {
            System.err.println("[ERROR] 세션 완료 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 최근 세션 목록 조회
     */
    public List<OrganizeSession> getRecentSessions(int limit) {
        String sql = """
            SELECT session_id, source_folder, target_folder, total_files, 
                   successful_files, failed_files, total_size, session_status,
                   started_at, completed_at
            FROM organize_sessions 
            ORDER BY started_at DESC 
            LIMIT ?
        """;

        List<OrganizeSession> sessions = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrganizeSession session = new OrganizeSession();
                    session.setSessionId(rs.getString("session_id"));
                    session.setSourceFolder(rs.getString("source_folder"));
                    session.setTargetFolder(rs.getString("target_folder"));
                    session.setTotalFiles(rs.getInt("total_files"));
                    session.setSuccessfulFiles(rs.getInt("successful_files"));
                    session.setFailedFiles(rs.getInt("failed_files"));
                    session.setTotalSize(rs.getLong("total_size"));
                    session.setSessionStatus(rs.getString("session_status"));
                    session.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());

                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    if (completedAt != null) {
                        session.setCompletedAt(completedAt.toLocalDateTime());
                    }

                    sessions.add(session);
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 세션 목록 조회 실패: " + e.getMessage());
        }

        return sessions;
    }

    /**
     * 데이터베이스 연결 종료
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[INFO] 데이터베이스 연결 종료");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 데이터베이스 연결 종료 실패: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스 정리 (오래된 레코드 삭제)
     */
    public void cleanupOldRecords() {
        try {
            // 90일 이상 된 히스토리 삭제
            String cleanupHistory = """
                DELETE FROM file_history 
                WHERE created_at < datetime('now', '-90 days')
            """;

            // 1년 이상 된 통계 삭제
            String cleanupStats = """
                DELETE FROM app_statistics 
                WHERE created_at < datetime('now', '-1 year')
            """;

            try (Statement stmt = connection.createStatement()) {
                int historyDeleted = stmt.executeUpdate(cleanupHistory);
                int statsDeleted = stmt.executeUpdate(cleanupStats);

                System.out.println("[INFO] 데이터베이스 정리 완료: 히스토리 " +
                        historyDeleted + "건, 통계 " + statsDeleted + "건 삭제");
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] 데이터베이스 정리 실패: " + e.getMessage());
        }
    }

    // =====================================
    // 내부 데이터 클래스들
    // =====================================

    /**
     * 파일 작업 히스토리 레코드
     */
    public static class FileOperationHistory {
        private String fileName;
        private String filePath;
        private String originalLocation;
        private String targetLocation;
        private String operationType;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getOriginalLocation() { return originalLocation; }
        public void setOriginalLocation(String originalLocation) { this.originalLocation = originalLocation; }

        public String getTargetLocation() { return targetLocation; }
        public void setTargetLocation(String targetLocation) { this.targetLocation = targetLocation; }

        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    }

    /**
     * 정리 세션 레코드
     */
    public static class OrganizeSession {
        private String sessionId;
        private String sourceFolder;
        private String targetFolder;
        private int totalFiles;
        private int successfulFiles;
        private int failedFiles;
        private long totalSize;
        private String sessionStatus;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getSourceFolder() { return sourceFolder; }
        public void setSourceFolder(String sourceFolder) { this.sourceFolder = sourceFolder; }

        public String getTargetFolder() { return targetFolder; }
        public void setTargetFolder(String targetFolder) { this.targetFolder = targetFolder; }

        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public int getSuccessfulFiles() { return successfulFiles; }
        public void setSuccessfulFiles(int successfulFiles) { this.successfulFiles = successfulFiles; }

        public int getFailedFiles() { return failedFiles; }
        public void setFailedFiles(int failedFiles) { this.failedFiles = failedFiles; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public String getSessionStatus() { return sessionStatus; }
        public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}