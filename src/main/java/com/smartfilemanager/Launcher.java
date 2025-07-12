package com.smartfilemanager;

/**
 * JavaFX 애플리케이션 런처
 * 
 * 이 클래스는 JavaFX 모듈 시스템과의 호환성 문제를 해결하기 위해 생성되었습니다.
 * Application 클래스를 직접 실행하는 대신 이 런처를 통해 애플리케이션을 시작합니다.
 */
public class Launcher {
    
    /**
     * 애플리케이션 진입점
     * 
     * @param args 명령줄 인수
     */
    public static void main(String[] args) {
        // JavaFX 모듈 시스템 우회를 위해 런처를 통해 실행
        try {
            // 시스템 속성 설정 (GUI 전용)
            System.setProperty("javafx.preloader", "");
            System.setProperty("file.encoding", "UTF-8");
            
            // 콘솔 창 숨기기 (Windows 전용)
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // 콘솔 출력 최소화 (GUI 모드)
                System.setProperty("java.awt.headless", "false");
            }
            
            // SmartFileManagerApp 실행
            SmartFileManagerApp.main(args);
            
        } catch (Exception e) {
            // GUI 오류 다이얼로그만 표시 (콘솔 출력 제거)
            try {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Smart File Manager 실행 중 오류가 발생했습니다:\n" + e.getMessage() + 
                    "\n\nJava 17 이상이 설치되어 있는지 확인해주세요.",
                    "Smart File Manager - 오류",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            } catch (Exception swingEx) {
                // Swing도 실패한 경우에만 콘솔 출력
                System.err.println("Smart File Manager 실행 실패: " + e.getMessage());
            }
            
            System.exit(1);
        }
    }
}