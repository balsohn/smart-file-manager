package com.smartfilemanager.service;

import com.smartfilemanager.model.AppConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 애플리케이션 설정을 관리하는 서비스
 * JSON 파일로 설정을 저장하고 로드합니다
 */
public class ConfigService {

    private static final String CONFIG_DIR = System.getProperty("user.home") +
            File.separator + ".smartfilemanager";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    private static final String BACKUP_DIR = CONFIG_DIR + File.separator + "backup";

    private final Gson gson;
    private AppConfig currentConfig;

    public ConfigService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        // 설정 디렉토리 생성
        ensureConfigDirectory();

        // 설정 로드
        this.currentConfig = loadConfig();
    }

    /**
     * 설정 파일 로드
     */
    public AppConfig loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);

            if (!configFile.exists()) {
                System.out.println("[INFO] 설정 파일이 없습니다. 기본 설정을 생성합니다.");
                AppConfig defaultConfig = AppConfig.createDefault();
                saveConfig(defaultConfig);
                return defaultConfig;
            }

            String json = Files.readString(configFile.toPath());
            AppConfig config = gson.fromJson(json, AppConfig.class);

            // 설정 유효성 검증
            if (!config.isValid()) {
                System.err.println("[ERROR] 설정 파일이 손상되었습니다. 기본 설정으로 복원합니다.");
                return AppConfig.createDefault();
            }

            System.out.println("[SUCCESS] 설정 파일을 성공적으로 로드했습니다: " + CONFIG_FILE);
            return config;

        } catch (Exception e) {
            System.err.println("[ERROR] 설정 로드 실패: " + e.getMessage());
            System.out.println("[INFO] 기본 설정을 사용합니다.");
            return AppConfig.createDefault();
        }
    }

    /**
     * 설정 파일 저장
     */
    public boolean saveConfig(AppConfig config) {
        try {
            // 설정 유효성 검증
            if (!config.isValid()) {
                System.err.println("[ERROR] 유효하지 않은 설정입니다. 저장을 중단합니다.");
                return false;
            }

            // 기존 설정 백업
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                createBackup();
            }

            // 설정 저장
            String json = gson.toJson(config);
            Files.writeString(Paths.get(CONFIG_FILE), json);

            this.currentConfig = config;

            System.out.println("[SUCCESS] 설정이 성공적으로 저장되었습니다: " + CONFIG_FILE);
            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] 설정 저장 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 현재 설정 반환
     */
    public AppConfig getCurrentConfig() {
        return currentConfig;
    }

    /**
     * 설정 업데이트
     */
    public boolean updateConfig(AppConfig newConfig) {
        return saveConfig(newConfig);
    }

    /**
     * 설정 초기화 (기본값으로 복원)
     */
    public AppConfig resetToDefault() {
        AppConfig defaultConfig = AppConfig.createDefault();
        if (saveConfig(defaultConfig)) {
            System.out.println("[INFO] 설정이 기본값으로 초기화되었습니다.");
            return defaultConfig;
        } else {
            System.err.println("[ERROR] 설정 초기화에 실패했습니다.");
            return currentConfig;
        }
    }

    /**
     * 설정 디렉토리 생성
     */
    private void ensureConfigDirectory() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            Path backupDir = Paths.get(BACKUP_DIR);

            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                System.out.println("[INFO] 설정 디렉토리 생성: " + CONFIG_DIR);
            }

            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                System.out.println("[INFO] 백업 디렉토리 생성: " + BACKUP_DIR);
            }

        } catch (IOException e) {
            System.err.println("[ERROR] 설정 디렉토리 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 기존 설정 백업
     */
    private void createBackup() {
        try {
            String timestamp = LocalDateTime.now()
                    .toString()
                    .replaceAll("[:\\-.]", "_")
                    .substring(0, 19); // YYYY_MM_DD_HH_MM_SS

            String backupFileName = "config_backup_" + timestamp + ".json";
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);

            Files.copy(Paths.get(CONFIG_FILE), backupPath);

            System.out.println("[INFO] 설정 백업 생성: " + backupPath);

            // 오래된 백업 파일 정리 (최근 5개만 보관)
            cleanupOldBackups();

        } catch (IOException e) {
            System.err.println("[ERROR] 설정 백업 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 오래된 백업 파일 정리
     */
    private void cleanupOldBackups() {
        try {
            File backupDir = new File(BACKUP_DIR);
            File[] backupFiles = backupDir.listFiles((dir, name) ->
                    name.startsWith("config_backup_") && name.endsWith(".json"));

            if (backupFiles != null && backupFiles.length > 5) {
                // 파일을 수정일 기준으로 정렬 (오래된 것부터)
                java.util.Arrays.sort(backupFiles,
                        (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

                // 오래된 파일들 삭제 (최근 5개만 보관)
                for (int i = 0; i < backupFiles.length - 5; i++) {
                    if (backupFiles[i].delete()) {
                        System.out.println("[INFO] 오래된 백업 파일 삭제: " + backupFiles[i].getName());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 백업 파일 정리 실패: " + e.getMessage());
        }
    }

    /**
     * 설정 파일이 존재하는지 확인
     */
    public boolean configExists() {
        return Files.exists(Paths.get(CONFIG_FILE));
    }

    /**
     * 설정 파일 경로 반환
     */
    public String getConfigFilePath() {
        return CONFIG_FILE;
    }

    /**
     * 백업 파일 목록 반환
     */
    public String[] getBackupFiles() {
        File backupDir = new File(BACKUP_DIR);
        String[] backupFiles = backupDir.list((dir, name) ->
                name.startsWith("config_backup_") && name.endsWith(".json"));

        if (backupFiles != null) {
            java.util.Arrays.sort(backupFiles, java.util.Collections.reverseOrder());
        }

        return backupFiles != null ? backupFiles : new String[0];
    }

    /**
     * 백업 파일로부터 설정 복원
     */
    public AppConfig restoreFromBackup(String backupFileName) {
        try {
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);

            if (!Files.exists(backupPath)) {
                System.err.println("[ERROR] 백업 파일이 존재하지 않습니다: " + backupFileName);
                return null;
            }

            String json = Files.readString(backupPath);
            AppConfig config = gson.fromJson(json, AppConfig.class);

            if (config.isValid() && saveConfig(config)) {
                System.out.println("[SUCCESS] 백업에서 설정을 복원했습니다: " + backupFileName);
                return config;
            } else {
                System.err.println("[ERROR] 백업 파일이 유효하지 않습니다: " + backupFileName);
                return null;
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 백업 복원 실패: " + e.getMessage());
            return null;
        }
    }
}