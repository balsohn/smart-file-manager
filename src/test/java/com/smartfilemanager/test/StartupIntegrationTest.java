package com.smartfilemanager.test;

import com.smartfilemanager.model.AppConfig;
import com.smartfilemanager.service.ConfigService;
import com.smartfilemanager.util.StartupManager;

/**
 * Windows 시작프로그램 기능 통합 테스트
 */
public class StartupIntegrationTest {

    public static void main(String[] args) {
        System.out.println("🚀 Smart File Manager - 시작프로그램 기능 테스트");
        System.out.println("=".repeat(60));

        // 1. 환경 확인
        testEnvironment();

        // 2. 기본 기능 테스트
        testBasicFunctions();

        // 3. 설정 연동 테스트
        testConfigIntegration();

        // 4. 실제 등록/해제 테스트 (선택사항)
        testRegistrationFlow();

        System.out.println("\n🎉 모든 테스트 완료!");
    }

    /**
     * 환경 확인 테스트
     */
    private static void testEnvironment() {
        System.out.println("\n📋 1. 환경 확인 테스트");

        System.out.println("   운영체제: " + System.getProperty("os.name"));
        System.out.println("   Java 버전: " + System.getProperty("java.version"));
        System.out.println("   사용자 홈: " + System.getProperty("user.home"));

        boolean isWindows = StartupManager.isWindows();
        boolean isSupported = StartupManager.isSupported();

        System.out.println("   Windows 여부: " + (isWindows ? "✅ 예" : "❌ 아니오"));
        System.out.println("   기능 지원: " + (isSupported ? "✅ 지원됨" : "❌ 지원되지 않음"));

        if (!isSupported) {
            System.out.println("   ⚠️  Windows가 아닌 시스템에서는 시작프로그램 기능이 비활성화됩니다.");
        }
    }

    /**
     * 기본 기능 테스트
     */
    private static void testBasicFunctions() {
        System.out.println("\n🔧 2. 기본 기능 테스트");

        // 현재 등록 상태 확인
        boolean isRegistered = StartupManager.isRegistered();
        System.out.println("   현재 등록 상태: " + (isRegistered ? "✅ 등록됨" : "❌ 등록되지 않음"));

        // 실행 파일 경로 확인
        String execPath = StartupManager.getCurrentExecutablePath();
        System.out.println("   실행 파일 경로: " + (execPath != null ? execPath : "❌ 확인 불가"));

        // 상태 요약 확인
        System.out.println("\n   📊 상태 요약:");
        String summary = StartupManager.getStatusSummary();
        String[] lines = summary.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                System.out.println("   " + line);
            }
        }
    }

    /**
     * 설정 연동 테스트
     */
    private static void testConfigIntegration() {
        System.out.println("\n⚙️  3. 설정 연동 테스트");

        try {
            ConfigService configService = new ConfigService();
            AppConfig config = configService.getCurrentConfig();

            boolean configStartup = config.isStartWithWindows();
            boolean actualStartup = StartupManager.isRegistered();

            System.out.println("   설정 파일 값: " + (configStartup ? "✅ 활성화" : "❌ 비활성화"));
            System.out.println("   실제 등록 상태: " + (actualStartup ? "✅ 등록됨" : "❌ 등록되지 않음"));

            if (configStartup == actualStartup) {
                System.out.println("   ✅ 설정과 실제 상태가 일치합니다");
            } else {
                System.out.println("   ⚠️  설정과 실제 상태가 다릅니다 - 동기화 필요");

                // 자동 동기화 테스트
                config.setStartWithWindows(actualStartup);
                boolean saved = configService.saveConfig(config);
                System.out.println("   🔄 자동 동기화: " + (saved ? "✅ 성공" : "❌ 실패"));
            }

        } catch (Exception e) {
            System.out.println("   ❌ 설정 연동 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 실제 등록/해제 테스트 (사용자 확인 필요)
     */
    private static void testRegistrationFlow() {
        System.out.println("\n🧪 4. 등록/해제 테스트 (선택사항)");

        if (!StartupManager.isSupported()) {
            System.out.println("   ⚠️  Windows가 아닌 시스템에서는 테스트를 건너뜁니다.");
            return;
        }

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("   실제 등록/해제 테스트를 진행하시겠습니까? (y/n): ");

        String input = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(input) && !"yes".equals(input)) {
            System.out.println("   테스트를 건너뜁니다.");
            return;
        }

        try {
            boolean originalState = StartupManager.isRegistered();
            System.out.println("   원래 상태: " + (originalState ? "등록됨" : "등록되지 않음"));

            // 토글 테스트
            System.out.println("   🔄 상태 토글 테스트 중...");
            boolean newState = StartupManager.toggle();
            System.out.println("   새로운 상태: " + (newState ? "등록됨" : "등록되지 않음"));

            // 잠시 대기
            Thread.sleep(1000);

            // 상태 확인
            boolean verifyState = StartupManager.isRegistered();
            if (newState == verifyState) {
                System.out.println("   ✅ 토글 테스트 성공");
            } else {
                System.out.println("   ❌ 토글 테스트 실패 - 상태 불일치");
            }

            // 원래 상태로 복원할지 물어보기
            System.out.print("   원래 상태로 복원하시겠습니까? (y/n): ");
            String restoreInput = scanner.nextLine().trim().toLowerCase();

            if ("y".equals(restoreInput) || "yes".equals(restoreInput)) {
                System.out.println("   🔄 원래 상태로 복원 중...");

                if (originalState != StartupManager.isRegistered()) {
                    StartupManager.toggle();
                }

                boolean finalState = StartupManager.isRegistered();
                if (finalState == originalState) {
                    System.out.println("   ✅ 원래 상태로 복원 완료");
                } else {
                    System.out.println("   ⚠️  복원 실패 - 수동으로 설정을 확인해주세요");
                }
            }

        } catch (Exception e) {
            System.out.println("   ❌ 등록/해제 테스트 실패: " + e.getMessage());
        }

        scanner.close();
    }
}

/**
 * 빠른 상태 확인용 유틸리티
 */
class StartupQuickCheck {

    public static void main(String[] args) {
        System.out.println("🔍 Smart File Manager 시작프로그램 상태 확인");
        System.out.println("-".repeat(50));

        if (StartupManager.isSupported()) {
            boolean registered = StartupManager.isRegistered();
            String execPath = StartupManager.getCurrentExecutablePath();

            System.out.println("상태: " + (registered ? "✅ 등록됨" : "❌ 등록되지 않음"));
            System.out.println("경로: " + (execPath != null ? execPath : "확인 불가"));

            if (registered) {
                System.out.println("💡 Windows 시작 시 자동으로 실행됩니다.");
            } else {
                System.out.println("💡 설정에서 '시작프로그램 등록'을 활성화할 수 있습니다.");
            }
        } else {
            System.out.println("❌ Windows가 아닌 시스템에서는 지원되지 않습니다.");
        }
    }
}

/**
 * 명령행 도구
 */
class StartupCommandTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "status":
                System.out.println(StartupManager.getStatusSummary());
                break;

            case "register":
                if (StartupManager.isSupported()) {
                    String execPath = StartupManager.getCurrentExecutablePath();
                    if (execPath != null && StartupManager.register(execPath)) {
                        System.out.println("✅ 시작프로그램 등록 완료");
                    } else {
                        System.out.println("❌ 시작프로그램 등록 실패");
                        System.exit(1);
                    }
                } else {
                    System.out.println("❌ 지원되지 않는 시스템");
                    System.exit(1);
                }
                break;

            case "unregister":
                if (StartupManager.isSupported()) {
                    if (StartupManager.unregister()) {
                        System.out.println("✅ 시작프로그램 해제 완료");
                    } else {
                        System.out.println("❌ 시작프로그램 해제 실패");
                        System.exit(1);
                    }
                } else {
                    System.out.println("❌ 지원되지 않는 시스템");
                    System.exit(1);
                }
                break;

            case "toggle":
                if (StartupManager.isSupported()) {
                    boolean newState = StartupManager.toggle();
                    System.out.println("🔄 토글 완료: " + (newState ? "등록됨" : "해제됨"));
                } else {
                    System.out.println("❌ 지원되지 않는 시스템");
                    System.exit(1);
                }
                break;

            default:
                System.out.println("❌ 알 수 없는 명령어: " + command);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("사용법: java StartupCommandTool <command>");
        System.out.println("");
        System.out.println("명령어:");
        System.out.println("  status     - 현재 상태 확인");
        System.out.println("  register   - 시작프로그램 등록");
        System.out.println("  unregister - 시작프로그램 해제");
        System.out.println("  toggle     - 상태 토글 (등록 ↔ 해제)");
    }
}