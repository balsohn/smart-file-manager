package com.smartfilemanager.ui;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * 시스템 트레이 관리 클래스
 * 애플리케이션을 시스템 트레이에 최소화하고 관리합니다
 */
public class SystemTrayManager {

    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    private static Stage primaryStage;
    private static boolean isSetup = false;

    /**
     * 시스템 트레이 설정
     */
    public static boolean setupSystemTray(Stage stage) {
        if (!SystemTray.isSupported()) {
            System.out.println("[WARNING] 시스템 트레이가 지원되지 않습니다.");
            return false;
        }

        if (isSetup) {
            return true;
        }

        try {
            primaryStage = stage;
            systemTray = SystemTray.getSystemTray();

            // 트레이 아이콘 이미지 생성 (간단한 파일 아이콘)
            Image trayIconImage = createTrayIcon();

            // 팝업 메뉴 생성
            PopupMenu popupMenu = createTrayPopupMenu();

            // 트레이 아이콘 생성
            trayIcon = new TrayIcon(trayIconImage, "Smart File Manager", popupMenu);
            trayIcon.setImageAutoSize(true);

            // 더블클릭으로 창 복원
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Platform.runLater(() -> showApplication());
                }
            });

            // 시스템 트레이에 추가
            systemTray.add(trayIcon);
            isSetup = true;

            System.out.println("[SUCCESS] 시스템 트레이 설정 완료");
            return true;

        } catch (AWTException e) {
            System.err.println("[ERROR] 시스템 트레이 설정 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 애플리케이션을 트레이로 최소화
     */
    public static void minimizeToTray() {
        if (primaryStage != null && isSetup) {
            primaryStage.hide();

            // 첫 번째 최소화 시 안내 메시지
            if (trayIcon != null) {
                trayIcon.displayMessage(
                        "Smart File Manager",
                        "애플리케이션이 시스템 트레이로 최소화되었습니다.\n" +
                                "트레이 아이콘을 더블클릭하면 다시 열 수 있습니다.",
                        TrayIcon.MessageType.INFO
                );
            }

            System.out.println("[INFO] 애플리케이션이 트레이로 최소화됨");
        }
    }

    /**
     * 애플리케이션 창 복원
     */
    public static void showApplication() {
        if (primaryStage != null) {
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
            System.out.println("[INFO] 애플리케이션 창 복원됨");
        }
    }

    /**
     * 시스템 트레이에서 제거
     */
    public static void removeFromTray() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
            isSetup = false;
            System.out.println("[INFO] 시스템 트레이에서 제거됨");
        }
    }

    /**
     * 트레이 팝업 메뉴 생성
     */
    private static PopupMenu createTrayPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();

        // 열기 메뉴
        MenuItem openItem = new MenuItem("📂 열기");
        openItem.addActionListener(e -> Platform.runLater(() -> showApplication()));

        // 새 스캔 메뉴 (추가 기능)
        MenuItem scanItem = new MenuItem("🔍 새 스캔");
        scanItem.addActionListener(e -> {
            Platform.runLater(() -> {
                showApplication();
                // 메인 컨트롤러의 스캔 메서드 호출 (나중에 연결)
                System.out.println("[TRAY] 새 스캔 요청됨");
            });
        });

        // 설정 메뉴
        MenuItem settingsItem = new MenuItem("⚙️ 설정");
        settingsItem.addActionListener(e -> {
            Platform.runLater(() -> {
                showApplication();
                // 설정 창 열기 (나중에 연결)
                System.out.println("[TRAY] 설정 요청됨");
            });
        });

        // 구분선
        popupMenu.addSeparator();

        // 정보 메뉴
        MenuItem aboutItem = new MenuItem("ℹ️ 정보");
        aboutItem.addActionListener(e -> {
            Platform.runLater(() -> {
                showApplication();
                AboutDialog.show(primaryStage);
            });
        });

        // 종료 메뉴
        MenuItem exitItem = new MenuItem("🚪 종료");
        exitItem.addActionListener(e -> {
            Platform.runLater(() -> {
                removeFromTray();
                Platform.exit();
            });
        });

        // 메뉴 아이템 추가
        popupMenu.add(openItem);
        popupMenu.add(scanItem);
        popupMenu.add(settingsItem);
        popupMenu.addSeparator();
        popupMenu.add(aboutItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        return popupMenu;
    }

    /**
     * 트레이 아이콘 이미지 생성
     */
    private static Image createTrayIcon() {
        // 16x16 픽셀의 간단한 파일 아이콘 생성
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경 투명
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, 16, 16);

        // 파일 아이콘 그리기 (간단한 사각형)
        g2d.setColor(new Color(52, 152, 219)); // 파란색
        g2d.fillRoundRect(2, 1, 10, 12, 2, 2);

        // 파일 접힌 부분
        g2d.setColor(new Color(41, 128, 185)); // 진한 파란색
        g2d.fillPolygon(new int[]{9, 12, 12}, new int[]{1, 1, 4}, 3);

        // 흰색 선들 (텍스트 라인 표현)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(4, 5, 6, 1);
        g2d.fillRect(4, 7, 5, 1);
        g2d.fillRect(4, 9, 4, 1);

        g2d.dispose();

        return image;
    }

    /**
     * 트레이 알림 표시
     */
    public static void showTrayNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }

    /**
     * 파일 정리 완료 알림
     */
    public static void notifyOrganizationComplete(int successCount, int totalCount) {
        if (trayIcon != null) {
            String message = String.format(
                    "파일 정리가 완료되었습니다!\n성공: %d개, 전체: %d개",
                    successCount, totalCount
            );
            trayIcon.displayMessage(
                    "🎉 정리 완료",
                    message,
                    TrayIcon.MessageType.INFO
            );
        }
    }

    /**
     * 중복 파일 발견 알림
     */
    public static void notifyDuplicatesFound(int duplicateCount, String savedSpace) {
        if (trayIcon != null) {
            String message = String.format(
                    "중복 파일 %d개를 발견했습니다!\n절약 가능 공간: %s",
                    duplicateCount, savedSpace
            );
            trayIcon.displayMessage(
                    "🔄 중복 파일 발견",
                    message,
                    TrayIcon.MessageType.WARNING
            );
        }
    }

    /**
     * 시스템 트레이 지원 여부 확인
     */
    public static boolean isSystemTraySupported() {
        return SystemTray.isSupported();
    }

    /**
     * 트레이 설정 상태 확인
     */
    public static boolean isSetup() {
        return isSetup;
    }
}