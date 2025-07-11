package com.smartfilemanager.util;

import com.smartfilemanager.constants.FileConstants;
import javafx.concurrent.Task;

/**
 * JavaFX Task 관련 유틸리티 클래스
 */
public final class TaskUtils {
    
    private TaskUtils() {
        // 인스턴스 생성 방지
    }
    
    /**
     * Task를 백그라운드 스레드에서 실행
     */
    public static void runTaskAsync(Task<?> task) {
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    /**
     * Task 실행 중 지연 처리 (중단 가능)
     */
    public static void sleepInterruptibly(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
    
    /**
     * 파일 스캔용 지연
     */
    public static void fileScanDelay() {
        try {
            sleepInterruptibly(FileConstants.ProcessingDelays.FILE_SCAN_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 파일 정리용 지연
     */
    public static void fileOrganizeDelay() {
        try {
            sleepInterruptibly(FileConstants.ProcessingDelays.FILE_ORGANIZE_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Task 성공 핸들러를 위한 결과 메시지 생성
     */
    public static String createSuccessMessage(String operation, int successCount) {
        return String.format("🎉 %s가 완료되었습니다!\n✅ 성공: %d개 파일", operation, successCount);
    }
    
    /**
     * Task 실패 핸들러를 위한 에러 메시지 생성
     */
    public static String createErrorMessage(String operation, Throwable error) {
        return String.format("%s 중 오류가 발생했습니다: %s", operation, error.getMessage());
    }
    
    /**
     * 진행률 계산
     */
    public static double calculateProgress(int current, int total) {
        if (total == 0) return 0.0;
        return (double) current / total;
    }
    
    /**
     * Task 완료 후 UI 업데이트를 위한 통계 메시지 생성
     */
    public static String createCompletionStats(int successCount, int totalCount) {
        int failedCount = totalCount - successCount;
        return String.format("정리 완료: %d개 성공, %d개 실패", successCount, failedCount);
    }
    
    /**
     * Task 중단 확인
     */
    public static void checkCancelled(Task<?> task) {
        if (task.isCancelled()) {
            throw new RuntimeException("Task was cancelled");
        }
    }
}