package com.smartfilemanager;

import com.smartfilemanager.controller.MainController;
import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.ui.SystemTrayManager;
import com.smartfilemanager.ui.ThemeManager;
import com.smartfilemanager.util.FileTypeDetector;
import com.smartfilemanager.util.Messages;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Smart File Manager 메인 애플리케이션 클래스
 *
 * 이 클래스는 JavaFX 애플리케이션의 진입점이며,
 * FXML 기반 UI를 로드하고 초기화를 담당합니다.
 *
 * 주요 변경사항:
 * - UIFactory 기반에서 FXML 기반으로 전환
 * - MainController와 연동하여 UI 로직 분리
 * - 설정 로드 및 초기화 간소화
 *
 * @version 1.0
 * @author Smart File Manager Team
 */
public class SmartFileManagerApp extends Application {

    /**
     * JavaFX 애플리케이션 시작 메서드
     *
     * @param primaryStage 기본 스테이지
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("[INFO] 애플리케이션 시작 중...");

            // 1. FXML 파일 로드
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();

            System.out.println("[SUCCESS] FXML 로드 완료");

            // 2. 씬 생성
            Scene scene = new Scene(root, 900, 700);

            // 3. 설정 로드 및 테마 적용
            ConfigService configService = new ConfigService();
            AppConfig config = configService.getCurrentConfig();
            
            // 4. 커스텀 규칙 초기화
            if (config.isUseCustomRules()) {
                FileTypeDetector.initializeCustomRules(config.getCustomRulesFilePath());
                System.out.println("[SUCCESS] 커스텀 규칙 초기화 완료");
            }

            // 설정에서 테마 로드
            ThemeManager.loadThemeFromConfig();
            
            // 테마 매니저에 메인 씬 등록 및 현재 테마 적용
            ThemeManager.setMainScene(scene);
            ThemeManager.applyCurrentTheme(scene);
            
            System.out.println("[SUCCESS] 설정에서 테마 적용 완료: " + ThemeManager.getCurrentTheme().getDisplayName());

            // 5. 스테이지 설정
            setupStage(primaryStage, scene);

            // 6. 시스템 트레이 설정 (설정에 따라)
            if (config.isMinimizeToTray()) {
                boolean traySetup = SystemTrayManager.setupSystemTray(primaryStage);
                if (traySetup) {
                    System.out.println("[SUCCESS] 시스템 트레이 설정 완료");

                    // 창 닫기 이벤트를 트레이로 최소화로 변경
                    primaryStage.setOnCloseRequest(event -> {
                        event.consume(); // 기본 닫기 동작 취소
                        SystemTrayManager.minimizeToTray();
                    });
                } else {
                    System.out.println("[WARNING] 시스템 트레이 설정 실패 - 일반 모드로 동작");
                }
            }

            // 7. 애플리케이션 표시
            primaryStage.show();

            System.out.println("[START] " + Messages.get("app.title") + " 시작 완료!");

        } catch (Exception e) {
            System.err.println("[ERROR] 애플리케이션 시작 실패: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("애플리케이션을 시작할 수 없습니다", e.getMessage());
        }
    }

    /**
     * CSS 스타일시트 적용 (테마 미사용 시 폴백)
     */
    private void applyCSSStyles(Scene scene) {
        try {
            String cssPath = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            System.out.println("[SUCCESS] 기본 CSS 스타일시트 로드 완료");
        } catch (Exception e) {
            System.out.println("[WARNING] CSS 파일을 찾을 수 없습니다. 기본 스타일을 사용합니다.");
            System.out.println("[WARNING] 오류 내용: " + e.getMessage());
        }
    }

    /**
     * 스테이지 기본 설정
     *
     * @param primaryStage 설정할 스테이지
     * @param scene 스테이지에 설정할 씬
     */
    private void setupStage(Stage primaryStage, Scene scene) {
        // 기본 속성 설정
        primaryStage.setTitle(Messages.get("app.window.title"));
        primaryStage.setScene(scene);

        // 창 크기 제한
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);

        // 창을 화면 중앙에 배치
        primaryStage.centerOnScreen();

        // 애플리케이션 아이콘 설정 (있다면)
        setApplicationIcon(primaryStage);

        // 창 닫기 이벤트 처리
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("[INFO] 사용자가 애플리케이션을 종료했습니다.");
        });

        System.out.println("[SUCCESS] 스테이지 설정 완료");
    }

    /**
     * 애플리케이션 아이콘 설정
     *
     * @param stage 아이콘을 설정할 스테이지
     */
    private void setApplicationIcon(Stage stage) {
        try {
            // 향후 아이콘 파일이 있을 때 사용
            // Image icon = new Image(getClass().getResourceAsStream("/icons/app-icon.png"));
            // stage.getIcons().add(icon);
            // System.out.println("[SUCCESS] 애플리케이션 아이콘 설정 완료");
        } catch (Exception e) {
            System.out.println("[INFO] 애플리케이션 아이콘을 찾을 수 없습니다.");
        }
    }

    /**
     * 오류 발생 시 사용자에게 알림 후 종료
     *
     * @param title 오류 제목
     * @param message 오류 메시지
     */
    private void showErrorAndExit(String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("❌ " + title);
            alert.setHeaderText(null);
            alert.setContentText(message + "\n\n애플리케이션을 종료합니다.");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("[ERROR] 오류 다이얼로그 표시 실패: " + e.getMessage());
        }

        System.exit(1);
    }

    /**
     * 애플리케이션 종료 시 호출
     */
    @Override
    public void stop() throws Exception {
        System.out.println("[STOP] " + Messages.get("app.title") + " 종료 중...");

        // 시스템 트레이에서 제거
        SystemTrayManager.removeFromTray();

        super.stop();
        System.out.println("[STOP] 애플리케이션 종료 완료");
    }

    /**
     * 메인 메서드 - 애플리케이션 진입점
     *
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        // UTF-8 인코딩 강제 설정 (이모지 지원 향상)
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        
        // 애플리케이션 시작 로그
        System.out.println("=".repeat(60));
        System.out.println("🗂️  Smart File Manager v1.0");
        System.out.println("🤖 AI 기반 스마트 파일 정리 도구");
        System.out.println("🚀 JavaFX 기반 데스크톱 애플리케이션");
        System.out.println("=".repeat(60));

        // 시스템 정보 출력
        printSystemInfo();

        // JavaFX 애플리케이션 시작
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("[FATAL] 애플리케이션 시작 중 치명적 오류 발생:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 시스템 정보 출력 (디버깅용)
     */
    private static void printSystemInfo() {
        try {
            System.out.println("[SYSTEM] Java 버전: " + System.getProperty("java.version"));
            System.out.println("[SYSTEM] JavaFX 버전: " + System.getProperty("javafx.version", "Unknown"));
            System.out.println("[SYSTEM] 운영체제: " + System.getProperty("os.name") + " " +
                    System.getProperty("os.version"));
            System.out.println("[SYSTEM] 인코딩: " + System.getProperty("file.encoding"));
            System.out.println("[SYSTEM] 사용자 홈: " + System.getProperty("user.home"));
            System.out.println("[SYSTEM] 작업 디렉토리: " + System.getProperty("user.dir"));
            
            // 이모지 지원 폰트 확인
            checkEmojiSupport();
            
            System.out.println("-".repeat(60));
        } catch (Exception e) {
            System.out.println("[WARNING] 시스템 정보 출력 실패: " + e.getMessage());
        }
    }

    /**
     * 이모지 지원 폰트 확인
     */
    private static void checkEmojiSupport() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            System.out.print("[EMOJI] 이모지 지원: ");
            
            if (osName.contains("windows")) {
                System.out.println("Windows (Segoe UI Emoji 사용)");
            } else if (osName.contains("mac")) {
                System.out.println("macOS (Apple Color Emoji 사용)");
            } else if (osName.contains("linux")) {
                System.out.println("Linux (Noto Color Emoji 권장)");
            } else {
                System.out.println("알 수 없는 OS (기본 폰트 사용)");
            }
            
            // 이모지 테스트 출력
            System.out.println("[EMOJI] 테스트: 🗂️📁⚙️📊🔧💾 (정상 표시되면 이모지 지원됨)");
            
        } catch (Exception e) {
            System.out.println("[WARNING] 이모지 지원 확인 실패: " + e.getMessage());
        }
    }
}