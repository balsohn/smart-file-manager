package com.smartfilemanager.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Windows 시작프로그램 관리 클래스
 *
 * 이 클래스는 Smart File Manager를 Windows 시작 시 자동으로 실행되도록
 * 등록하거나 해제하는 기능을 제공합니다.
 *
 * 구현 방식:
 * 1. Windows Startup 폴더 방식 (권장) - 관리자 권한 불필요
 * 2. 레지스트리 방식 (백업) - 더 안정적이지만 복잡함
 */
public class StartupManager {

    private static final String APP_NAME = "Smart File Manager";
    private static final String EXECUTABLE_NAME = "SmartFileManager.exe";
    private static final String SHORTCUT_NAME = "Smart File Manager.lnk";

    // Windows Startup 폴더 경로들
    private static final String STARTUP_FOLDER_CURRENT_USER =
            System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";

    private static final String STARTUP_FOLDER_ALL_USERS =
            "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";

    /**
     * 현재 시작프로그램 등록 상태 확인
     *
     * @return 시작프로그램에 등록되어 있으면 true, 아니면 false
     */
    public static boolean isRegistered() {
        try {
            // 방법 1: Startup 폴더에 바로가기 확인
            Path currentUserStartup = Paths.get(STARTUP_FOLDER_CURRENT_USER, SHORTCUT_NAME);
            Path allUsersStartup = Paths.get(STARTUP_FOLDER_ALL_USERS, SHORTCUT_NAME);

            if (Files.exists(currentUserStartup) || Files.exists(allUsersStartup)) {
                System.out.println("[STARTUP] 시작프로그램 등록됨 (Startup 폴더)");
                return true;
            }

            // 방법 2: 레지스트리 확인 (Windows 전용)
            if (isRegisteredInRegistry()) {
                System.out.println("[STARTUP] 시작프로그램 등록됨 (레지스트리)");
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[ERROR] 시작프로그램 상태 확인 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 시작프로그램에 등록
     *
     * @param executablePath 실행파일 경로 (JAR 또는 EXE)
     * @return 등록 성공 여부
     */
    public static boolean register(String executablePath) {
        try {
            System.out.println("[STARTUP] 시작프로그램 등록 시작: " + executablePath);

            // 실행파일 존재 확인
            if (!Files.exists(Paths.get(executablePath))) {
                System.err.println("[ERROR] 실행파일을 찾을 수 없습니다: " + executablePath);
                return false;
            }

            // 방법 1: Startup 폴더에 바로가기 생성 (우선 시도)
            if (registerWithStartupFolder(executablePath)) {
                System.out.println("[SUCCESS] Startup 폴더 방식으로 등록 완료");
                return true;
            }

            // 방법 2: 레지스트리 등록 (백업)
            if (registerWithRegistry(executablePath)) {
                System.out.println("[SUCCESS] 레지스트리 방식으로 등록 완료");
                return true;
            }

            System.err.println("[ERROR] 모든 등록 방법 실패");
            return false;

        } catch (Exception e) {
            System.err.println("[ERROR] 시작프로그램 등록 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 시작프로그램에서 해제
     *
     * @return 해제 성공 여부
     */
    public static boolean unregister() {
        try {
            System.out.println("[STARTUP] 시작프로그램 해제 시작");

            boolean success = false;

            // 방법 1: Startup 폴더에서 제거
            if (unregisterFromStartupFolder()) {
                System.out.println("[SUCCESS] Startup 폴더에서 제거 완료");
                success = true;
            }

            // 방법 2: 레지스트리에서 제거
            if (unregisterFromRegistry()) {
                System.out.println("[SUCCESS] 레지스트리에서 제거 완료");
                success = true;
            }

            return success;

        } catch (Exception e) {
            System.err.println("[ERROR] 시작프로그램 해제 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 현재 실행 중인 JAR 파일 경로 가져오기
     *
     * @return JAR 파일 경로 또는 null
     */
    public static String getCurrentExecutablePath() {
        try {
            // 개발 환경에서는 클래스 경로를 사용
            String classPath = System.getProperty("java.class.path");

            // JAR로 실행 중인 경우
            if (classPath.endsWith(".jar")) {
                File jarFile = new File(classPath);
                if (jarFile.exists()) {
                    return jarFile.getAbsolutePath();
                }
            }

            // 개발 환경인 경우 - Java 실행 명령어 생성
            String javaHome = System.getProperty("java.home");
            String javaExe = javaHome + File.separator + "bin" + File.separator + "java.exe";
            String workingDir = System.getProperty("user.dir");

            // Gradle 빌드 결과물 경로 확인
            File buildJar = new File(workingDir, "build/libs/smart-file-manager-1.0.jar");
            if (buildJar.exists()) {
                return buildJar.getAbsolutePath();
            }

            // 임시로 현재 작업 디렉토리 반환 (개발용)
            System.out.println("[WARNING] 개발 환경에서 실행 중 - 배포 후 정확한 경로가 설정됩니다");
            return workingDir + File.separator + "SmartFileManager.jar";

        } catch (Exception e) {
            System.err.println("[ERROR] 실행파일 경로 확인 실패: " + e.getMessage());
            return null;
        }
    }

    // =================
    // Startup 폴더 방식
    // =================

    /**
     * Windows Startup 폴더에 바로가기 생성
     */
    private static boolean registerWithStartupFolder(String executablePath) {
        try {
            Path startupDir = Paths.get(STARTUP_FOLDER_CURRENT_USER);

            // Startup 폴더 생성 (없으면)
            if (!Files.exists(startupDir)) {
                Files.createDirectories(startupDir);
            }

            // 바로가기 파일 경로
            Path shortcutPath = startupDir.resolve(SHORTCUT_NAME);

            // VBScript를 사용해 바로가기 생성
            String vbsScript = createShortcutVBScript(executablePath, shortcutPath.toString());

            // 임시 VBS 파일 생성 및 실행
            Path tempVbs = Files.createTempFile("create_shortcut", ".vbs");
            Files.write(tempVbs, vbsScript.getBytes("UTF-8"));

            // VBScript 실행
            ProcessBuilder pb = new ProcessBuilder("cscript", "//NoLogo", tempVbs.toString());
            Process process = pb.start();
            int exitCode = process.waitFor();

            // 임시 파일 정리
            Files.deleteIfExists(tempVbs);

            if (exitCode == 0 && Files.exists(shortcutPath)) {
                System.out.println("[SUCCESS] 바로가기 생성 완료: " + shortcutPath);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[ERROR] Startup 폴더 등록 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * Startup 폴더에서 바로가기 제거
     */
    private static boolean unregisterFromStartupFolder() {
        try {
            boolean removed = false;

            // 현재 사용자 Startup 폴더
            Path currentUserShortcut = Paths.get(STARTUP_FOLDER_CURRENT_USER, SHORTCUT_NAME);
            if (Files.exists(currentUserShortcut)) {
                Files.delete(currentUserShortcut);
                removed = true;
                System.out.println("[SUCCESS] 현재 사용자 바로가기 제거: " + currentUserShortcut);
            }

            // 모든 사용자 Startup 폴더 (권한 있으면)
            Path allUsersShortcut = Paths.get(STARTUP_FOLDER_ALL_USERS, SHORTCUT_NAME);
            if (Files.exists(allUsersShortcut)) {
                try {
                    Files.delete(allUsersShortcut);
                    removed = true;
                    System.out.println("[SUCCESS] 모든 사용자 바로가기 제거: " + allUsersShortcut);
                } catch (Exception e) {
                    System.out.println("[WARNING] 모든 사용자 바로가기 제거 실패 (권한 부족): " + e.getMessage());
                }
            }

            return removed;

        } catch (Exception e) {
            System.err.println("[ERROR] Startup 폴더 해제 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * Windows 바로가기 생성용 VBScript 생성
     */
    private static String createShortcutVBScript(String targetPath, String shortcutPath) {
        return String.format("""
            Set oWS = WScript.CreateObject("WScript.Shell")
            sLinkFile = "%s"
            Set oLink = oWS.CreateShortcut(sLinkFile)
            oLink.TargetPath = "%s"
            oLink.WorkingDirectory = "%s"
            oLink.Description = "%s"
            oLink.Save
            """,
                shortcutPath.replace("\\", "\\\\"),
                targetPath.replace("\\", "\\\\"),
                Paths.get(targetPath).getParent().toString().replace("\\", "\\\\"),
                APP_NAME
        );
    }

    // =================
    // 레지스트리 방식
    // =================

    /**
     * 레지스트리에서 시작프로그램 등록 확인
     */
    private static boolean isRegisteredInRegistry() {
        try {
            // reg query 명령어로 레지스트리 확인
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", APP_NAME
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0; // 0이면 키가 존재함

        } catch (Exception e) {
            // 레지스트리 접근 실패는 정상 (등록되지 않은 상태)
            return false;
        }
    }

    /**
     * 레지스트리에 시작프로그램 등록
     */
    private static boolean registerWithRegistry(String executablePath) {
        try {
            // Java로 실행하는 명령어 생성
            String javaHome = System.getProperty("java.home");
            String javaExe = javaHome + File.separator + "bin" + File.separator + "java.exe";

            String command;
            if (executablePath.endsWith(".jar")) {
                command = String.format("\"%s\" -jar \"%s\"", javaExe, executablePath);
            } else {
                command = "\"" + executablePath + "\"";
            }

            // reg add 명령어로 레지스트리에 등록
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "add",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", APP_NAME,
                    "/t", "REG_SZ",
                    "/d", command,
                    "/f"
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("[SUCCESS] 레지스트리 등록 완료: " + command);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[ERROR] 레지스트리 등록 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 레지스트리에서 시작프로그램 해제
     */
    private static boolean unregisterFromRegistry() {
        try {
            // reg delete 명령어로 레지스트리에서 제거
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "delete",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", APP_NAME,
                    "/f"
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("[SUCCESS] 레지스트리 해제 완료");
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[ERROR] 레지스트리 해제 실패: " + e.getMessage());
            return false;
        }
    }

    // =================
    // 유틸리티 메서드들
    // =================

    /**
     * 운영체제가 Windows인지 확인
     *
     * @return Windows면 true, 아니면 false
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }

    /**
     * 시작프로그램 기능 지원 여부 확인
     *
     * @return 지원하면 true, 아니면 false
     */
    public static boolean isSupported() {
        return isWindows();
    }

    /**
     * 현재 상태 요약 정보
     *
     * @return 상태 정보 문자열
     */
    public static String getStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Windows 시작프로그램 상태:\n");
        summary.append("  지원 여부: ").append(isSupported() ? "지원됨" : "지원되지 않음").append("\n");
        summary.append("  등록 상태: ").append(isRegistered() ? "등록됨" : "등록되지 않음").append("\n");

        String execPath = getCurrentExecutablePath();
        summary.append("  실행파일: ").append(execPath != null ? execPath : "확인 불가").append("\n");

        return summary.toString();
    }

    /**
     * 테스트용 메서드 - 시작프로그램 등록/해제 토글
     *
     * @return 새로운 등록 상태
     */
    public static boolean toggle() {
        if (isRegistered()) {
            boolean success = unregister();
            System.out.println("[TOGGLE] 시작프로그램 해제 " + (success ? "성공" : "실패"));
            return false;
        } else {
            String execPath = getCurrentExecutablePath();
            if (execPath != null) {
                boolean success = register(execPath);
                System.out.println("[TOGGLE] 시작프로그램 등록 " + (success ? "성공" : "실패"));
                return success;
            }
            return false;
        }
    }
}