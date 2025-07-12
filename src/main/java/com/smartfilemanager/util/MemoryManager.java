package com.smartfilemanager.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 메모리 사용량 모니터링 및 관리 유틸리티 클래스
 * 메모리 부족 상황을 방지하고 시스템 안정성을 보장합니다
 */
public class MemoryManager {

    // 최대 메모리 사용률 (70%)
    private static final double MAX_MEMORY_USAGE_RATIO = 0.7;
    
    // 경고 메모리 사용률 (60%)
    private static final double WARNING_MEMORY_USAGE_RATIO = 0.6;
    
    // 메모리 정리 임계값 (80%)
    private static final double CLEANUP_MEMORY_USAGE_RATIO = 0.8;
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * 현재 메모리 사용률 확인
     */
    public static double getCurrentMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        
        if (max == -1) {
            // 최대 힙 크기가 무제한인 경우 committed 사용
            max = heapUsage.getCommitted();
        }
        
        return (double) used / max;
    }

    /**
     * 요청된 메모리 크기가 사용 가능한지 확인
     */
    public static boolean isMemoryAvailable(long requiredBytes) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        
        if (max == -1) {
            max = heapUsage.getCommitted();
        }
        
        long availableMemory = (long) (max * MAX_MEMORY_USAGE_RATIO) - used;
        return requiredBytes <= availableMemory;
    }

    /**
     * 메모리 상태 확인 및 경고
     */
    public static MemoryStatus checkMemoryStatus() {
        double currentUsage = getCurrentMemoryUsage();
        
        if (currentUsage >= CLEANUP_MEMORY_USAGE_RATIO) {
            return MemoryStatus.CRITICAL;
        } else if (currentUsage >= WARNING_MEMORY_USAGE_RATIO) {
            return MemoryStatus.WARNING;
        } else {
            return MemoryStatus.NORMAL;
        }
    }

    /**
     * 가비지 컬렉션 강제 실행
     */
    public static void forceGarbageCollection() {
        System.gc();
        System.runFinalization();
        
        // 잠시 대기하여 GC가 완료될 시간 제공
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[MEMORY] 가비지 컬렉션 실행 완료");
    }

    /**
     * 메모리 정리 및 모니터링
     */
    public static void checkMemoryAndCleanup() {
        MemoryStatus status = checkMemoryStatus();
        
        switch (status) {
            case CRITICAL:
                System.err.println("[MEMORY] 메모리 사용량이 위험 수준입니다! 정리를 시작합니다.");
                forceGarbageCollection();
                
                // 정리 후 재확인
                if (getCurrentMemoryUsage() >= CLEANUP_MEMORY_USAGE_RATIO) {
                    System.err.println("[MEMORY] 메모리 부족! 대용량 작업을 중단하세요.");
                }
                break;
                
            case WARNING:
                System.out.println("[MEMORY] 메모리 사용량이 높습니다. 주의가 필요합니다.");
                break;
                
            case NORMAL:
                // 정상 상태 - 별도 처리 불필요
                break;
        }
    }

    /**
     * 메모리 정보 출력
     */
    public static void printMemoryInfo() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("=== 메모리 사용 현황 ===");
        System.out.println("힙 메모리:");
        System.out.println("  사용중: " + formatBytes(heapUsage.getUsed()));
        System.out.println("  할당됨: " + formatBytes(heapUsage.getCommitted()));
        System.out.println("  최대: " + formatBytes(heapUsage.getMax()));
        System.out.println("  사용률: " + String.format("%.1f%%", getCurrentMemoryUsage() * 100));
        
        System.out.println("비힙 메모리:");
        System.out.println("  사용중: " + formatBytes(nonHeapUsage.getUsed()));
        System.out.println("  할당됨: " + formatBytes(nonHeapUsage.getCommitted()));
        
        System.out.println("상태: " + checkMemoryStatus());
        System.out.println("========================");
    }

    /**
     * 바이트 단위를 읽기 쉬운 형태로 변환
     */
    private static String formatBytes(long bytes) {
        if (bytes < 0) return "알 수 없음";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * 대용량 작업 전 메모리 확인
     */
    public static boolean canHandleLargeOperation(long estimatedMemoryNeeded) {
        if (!isMemoryAvailable(estimatedMemoryNeeded)) {
            System.err.println("[MEMORY] 메모리 부족으로 대용량 작업을 수행할 수 없습니다.");
            System.err.println("[MEMORY] 필요한 메모리: " + formatBytes(estimatedMemoryNeeded));
            printMemoryInfo();
            return false;
        }
        
        return true;
    }

    /**
     * 메모리 사용량 감시 스레드 시작
     */
    public static Thread startMemoryMonitor(long intervalSeconds) {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    checkMemoryAndCleanup();
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        monitorThread.setDaemon(true);
        monitorThread.setName("MemoryMonitor");
        monitorThread.start();
        
        System.out.println("[MEMORY] 메모리 모니터링 시작됨 (간격: " + intervalSeconds + "초)");
        return monitorThread;
    }

    /**
     * 메모리 상태 열거형
     */
    public enum MemoryStatus {
        NORMAL("정상"),
        WARNING("경고"),
        CRITICAL("위험");
        
        private final String description;
        
        MemoryStatus(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}