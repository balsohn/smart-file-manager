package com.smartfilemanager.ui;

import javafx.scene.Scene;
import javafx.scene.Parent;

/**
 * 애플리케이션 테마를 관리하는 클래스
 * 라이트/다크 테마 전환을 지원합니다
 */
public class ThemeManager {

    public enum Theme {
        LIGHT("light", "Light Theme", "/css/light-theme.css"),
        DARK("dark", "Dark Theme", "/css/dark-theme.css");

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
    private static Scene currentScene = null;

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
            // 기본 스타일시트 추가
            String baseStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(baseStylesheet);

            // 테마 스타일시트 추가
            if (theme != Theme.LIGHT) { // LIGHT는 기본 스타일시트 사용
                String themeStylesheet = ThemeManager.class.getResource(theme.getCssPath()).toExternalForm();
                scene.getStylesheets().add(themeStylesheet);
            }

            System.out.println("[SUCCESS] 테마 적용 완료: " + theme.getDisplayName());

        } catch (Exception e) {
            System.err.println("[ERROR] 테마 적용 실패: " + e.getMessage());

            // 폴백: 기본 스타일시트만 적용
            try {
                String fallbackStylesheet = ThemeManager.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(fallbackStylesheet);
            } catch (Exception fallbackError) {
                System.err.println("[ERROR] 폴백 스타일시트도 실패: " + fallbackError.getMessage());
            }
        }
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
     * 테마 전환
     */
    public static void toggleTheme() {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(newTheme);
    }

    /**
     * 테마 ID로 테마 적용
     */
    public static void applyThemeById(String themeId) {
        for (Theme theme : Theme.values()) {
            if (theme.getId().equals(themeId)) {
                applyTheme(theme);
                return;
            }
        }
        System.err.println("[ERROR] 알 수 없는 테마 ID: " + themeId);
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
     * 씬 설정 (테마 관리자에 등록)
     */
    public static void setScene(Scene scene) {
        currentScene = scene;
    }
}