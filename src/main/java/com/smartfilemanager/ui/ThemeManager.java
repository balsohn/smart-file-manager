package com.smartfilemanager.ui;

import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * 애플리케이션 테마를 관리하는 클래스
 * 8가지 테마 지원: Light, Dark, Blue, Green, Purple, Orange, Teal, High Contrast
 */
public class ThemeManager {

    public enum Theme {
        LIGHT("light", "Light Theme", "/css/styles.css"),
        DARK("dark", "Dark Theme", "/css/dark-theme.css"),
        BLUE("blue", "Blue Theme", "/css/blue-theme.css"),
        GREEN("green", "Green Theme", "/css/green-theme.css"),
        PURPLE("purple", "Purple Theme", "/css/purple-theme.css"),
        ORANGE("orange", "Orange Theme", "/css/orange-theme.css"),
        TEAL("teal", "Teal Theme", "/css/teal-theme.css"),
        HIGH_CONTRAST("high-contrast", "High Contrast Theme", "/css/high-contrast-theme.css");

        private final String id;
        private final String displayName;
        private final String cssPath;

        Theme(String id, String displayName, String cssPath) {
            this.id = id;
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getCssPath() { return cssPath; }
    }

    private static Theme currentTheme = Theme.LIGHT;
    private static Scene mainScene = null;  // 메인 창 Scene
    private static Scene currentScene = null;  // 현재 작업 중인 Scene
    private static java.util.List<Scene> allScenes = new java.util.ArrayList<>();  // 모든 Scene 추적

    /**
     * 씬에 테마 적용
     */
    public static void applyTheme(Scene scene, Theme theme) {
        if (scene == null) {
            System.err.println("[ERROR] Scene이 null입니다.");
            return;
        }

        currentScene = scene;
        currentTheme = theme;

        // 기존 스타일시트 제거
        scene.getStylesheets().clear();

        try {
            if (theme == Theme.LIGHT) {
                // 라이트 테마: 기본 스타일시트만 사용
                String baseStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(baseStylesheet);
            } else if (theme == Theme.DARK) {
                // 다크 테마: 기본 스타일시트 + 다크 테마 오버라이드
                String baseStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(baseStylesheet);
                
                String darkStylesheet = ThemeManager.class.getResource("/css/dark-theme.css").toExternalForm();
                scene.getStylesheets().add(darkStylesheet);
            } else {
                // 기타 컬러 테마: 기본 스타일시트 + 컬러 테마
                String baseStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(baseStylesheet);
                
                String themeStylesheet = ThemeManager.class.getResource(theme.getCssPath()).toExternalForm();
                scene.getStylesheets().add(themeStylesheet);
            }

            System.out.println("[SUCCESS] 테마 적용 완료: " + theme.getDisplayName());
            System.out.println("[DEBUG] 적용된 스타일시트: " + scene.getStylesheets());

        } catch (Exception e) {
            System.err.println("[ERROR] 테마 적용 실패: " + e.getMessage());
            e.printStackTrace();

            // 폴백: 기본 스타일시트만 적용
            try {
                scene.getStylesheets().clear();
                String fallbackStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(fallbackStylesheet);
                System.out.println("[INFO] 폴백 스타일시트 적용됨");
            } catch (Exception fallbackError) {
                System.err.println("[ERROR] 폴백 스타일시트도 실패: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * 새로운 씬에 현재 테마 적용 (서브 창용)
     */
    public static void applyCurrentTheme(Scene scene) {
        applyTheme(scene, currentTheme);
    }

    /**
     * 현재 씬에 테마 적용
     */
    public static void applyTheme(Theme theme) {
        if (currentScene != null) {
            applyTheme(currentScene, theme);
        } else {
            System.err.println("[ERROR] 현재 Scene이 설정되지 않았습니다.");
        }
    }

    /**
     * 테마 전환 (라이트 ↔ 다크)
     */
    public static void toggleTheme() {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(newTheme);
    }

    /**
     * 테마 토글 (UIUpdateManager와 연동) - 모든 창에 적용
     */
    public static void toggleTheme(javafx.scene.Scene scene, com.smartfilemanager.manager.UIUpdateManager uiUpdateManager) {
        try {
            Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
            currentTheme = newTheme;
            
            // 설정에 테마 변경 사항 저장
            saveThemeToConfig(newTheme);
            
            // 모든 Scene에 테마 적용
            applyThemeToAllScenes(newTheme);
            
            if (uiUpdateManager != null) {
                String message = (newTheme == Theme.DARK) ? 
                    com.smartfilemanager.constants.MessageConstants.ThemeMessages.DARK_THEME_APPLIED :
                    com.smartfilemanager.constants.MessageConstants.ThemeMessages.LIGHT_THEME_APPLIED;
                uiUpdateManager.showTemporaryMessage(message);
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR] 테마 토글 실패: " + e.getMessage());
            if (uiUpdateManager != null) {
                uiUpdateManager.showTemporaryMessage("테마 변경 중 오류가 발생했습니다");
            }
        }
    }

    /**
     * 테마 ID로 테마 적용 (모든 창에 적용)
     */
    public static void applyThemeById(String themeId) {
        for (Theme theme : Theme.values()) {
            if (theme.getId().equals(themeId)) {
                currentTheme = theme;
                saveThemeToConfig(theme);
                applyThemeToAllScenes(theme);
                return;
            }
        }
        System.err.println("[ERROR] 알 수 없는 테마 ID: " + themeId);
    }

    /**
     * 모든 등록된 Scene에 테마 적용
     */
    private static void applyThemeToAllScenes(Theme theme) {
        System.out.println("[INFO] 모든 창에 테마 적용 시작: " + theme.getDisplayName() + " (총 " + allScenes.size() + "개 창)");
        
        for (Scene scene : allScenes) {
            if (scene != null) {
                try {
                    applyTheme(scene, theme);
                    System.out.println("[SUCCESS] Scene에 테마 적용 완료");
                } catch (Exception e) {
                    System.err.println("[ERROR] Scene 테마 적용 실패: " + e.getMessage());
                }
            }
        }
        
        System.out.println("[INFO] 모든 창에 테마 적용 완료: " + theme.getDisplayName());
    }

    /**
     * 현재 테마 반환
     */
    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * 현재 테마 ID 반환
     */
    public static String getCurrentThemeId() {
        return currentTheme.getId();
    }

    /**
     * 사용 가능한 모든 테마 반환
     */
    public static Theme[] getAvailableThemes() {
        return Theme.values();
    }

    /**
     * 테마 이름 배열 반환 (ComboBox용)
     */
    public static String[] getThemeNames() {
        Theme[] themes = Theme.values();
        String[] names = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            names[i] = themes[i].getDisplayName();
        }
        return names;
    }

    /**
     * 테마 ID 배열 반환
     */
    public static String[] getThemeIds() {
        Theme[] themes = Theme.values();
        String[] ids = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            ids[i] = themes[i].getId();
        }
        return ids;
    }

    /**
     * 메인 씬 설정 (테마 관리자에 등록)
     */
    public static void setMainScene(Scene scene) {
        mainScene = scene;
        currentScene = scene;
        // 메인 Scene을 allScenes 리스트에 추가
        if (!allScenes.contains(scene)) {
            allScenes.add(scene);
        }
        System.out.println("[DEBUG] 메인 Scene 등록됨. 총 Scene 수: " + allScenes.size());
    }

    /**
     * 씬 등록 (모든 새 창은 여기서 등록)
     */
    public static void registerScene(Scene scene) {
        if (!allScenes.contains(scene)) {
            allScenes.add(scene);
            // 새로 등록된 Scene에 현재 테마 적용
            applyTheme(scene, currentTheme);
            System.out.println("[DEBUG] 새 Scene 등록됨. 총 Scene 수: " + allScenes.size());
        }
        currentScene = scene;
    }

    /**
     * 씬 설정 (테마 관리자에 등록) - 호환성 유지
     */
    public static void setScene(Scene scene) {
        registerScene(scene);
    }

    /**
     * Scene 제거 (창이 닫힐 때 호출)
     */
    public static void unregisterScene(Scene scene) {
        allScenes.remove(scene);
        if (scene == currentScene) {
            currentScene = mainScene;
        }
        System.out.println("[DEBUG] Scene 제거됨. 총 Scene 수: " + allScenes.size());
    }

    /**
     * 테마 변경 사항을 설정에 저장
     */
    private static void saveThemeToConfig(Theme theme) {
        try {
            com.smartfilemanager.service.ConfigService configService = new com.smartfilemanager.service.ConfigService();
            com.smartfilemanager.model.AppConfig currentConfig = configService.getCurrentConfig();
            
            // 테마 설정 업데이트
            currentConfig.setTheme(theme.getId());
            
            // 설정 저장
            configService.saveConfig(currentConfig);
            
            System.out.println("[INFO] 테마 설정 저장 완료: " + theme.getDisplayName());
            
        } catch (Exception e) {
            System.err.println("[ERROR] 테마 설정 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 설정에서 테마 로드 및 적용
     */
    public static void loadThemeFromConfig() {
        try {
            com.smartfilemanager.service.ConfigService configService = new com.smartfilemanager.service.ConfigService();
            com.smartfilemanager.model.AppConfig config = configService.getCurrentConfig();
            
            String themeId = config.getTheme();
            if (themeId != null && !themeId.isEmpty()) {
                for (Theme theme : Theme.values()) {
                    if (theme.getId().equals(themeId)) {
                        currentTheme = theme;
                        System.out.println("[INFO] 설정에서 테마 로드 완료: " + theme.getDisplayName());
                        return;
                    }
                }
            }
            
            // 기본값: 라이트 테마
            currentTheme = Theme.LIGHT;
            System.out.println("[INFO] 기본 테마 적용: " + currentTheme.getDisplayName());
            
        } catch (Exception e) {
            System.err.println("[ERROR] 테마 설정 로드 실패: " + e.getMessage());
            currentTheme = Theme.LIGHT;
        }
    }
    
    /**
     * 테마 변경 (MenuController에서 사용)
     */
    public static void setTheme(Theme theme) {
        if (theme == null) {
            System.err.println("[ERROR] 테마가 null입니다.");
            return;
        }
        
        currentTheme = theme;
        
        // 모든 등록된 Scene에 새 테마 적용
        for (Scene scene : allScenes) {
            applyTheme(scene, theme);
        }
        
        System.out.println("[INFO] 테마 변경됨: " + theme.getDisplayName());
    }
}