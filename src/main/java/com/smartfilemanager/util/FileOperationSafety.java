package com.smartfilemanager.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 파일 작업 안전성을 보장하는 유틸리티 클래스
 * 시스템 파일 보호, 백업 생성, 안전한 파일 이동/삭제 기능 제공
 */
public class FileOperationSafety {

    // 보호해야 할 파일 확장자
    private static final Set<String> PROTECTED_EXTENSIONS = Set.of(
        ".exe", ".dll", ".sys", ".bat", ".cmd", ".com", ".scr", ".msi", ".jar"
    );

    // 보호해야 할 디렉토리 (대소문자 무시)
    private static final Set<String> PROTECTED_DIRECTORIES = Set.of(
        "system32", "windows", "program files", "program files (x86)", 
        "programdata", "boot", "recovery", "$recycle.bin", "system volume information"
    );

    // 보호해야 할 특수 파일명
    private static final Set<String> PROTECTED_FILENAMES = Set.of(
        "desktop.ini", "thumbs.db", "autorun.inf", "bootmgr", "hiberfil.sys", "pagefile.sys"
    );

    // 백업 디렉토리
    private final Path backupDirectory;

    public FileOperationSafety() {
        this.backupDirectory = Paths.get(System.getProperty("user.home"), 
            ".smartfilemanager", "backups");
        try {
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            System.err.println("[ERROR] 백업 디렉토리 생성 실패: " + e.getMessage());
            throw new RuntimeException("백업 시스템 초기화 실패", e);
        }
    }

    /**
     * 파일 작업이 안전한지 확인 (스캔용 - 더 관대한 기준)
     */
    public boolean isSafeToScan(Path filePath) {
        if (filePath == null) {
            return false;
        }

        try {
            // 파일 존재 확인
            if (!Files.exists(filePath)) {
                return false;
            }

            // 시스템 디렉토리 확인 (핵심 시스템 파일만)
            String pathStr = filePath.toString().toLowerCase();
            if (PROTECTED_DIRECTORIES.stream().anyMatch(pathStr::contains)) {
                System.out.println("[SAFETY] 보호된 시스템 디렉토리: " + filePath);
                return false;
            }

            // 핵심 시스템 파일만 확장자로 보호 (.exe, .msi, .jar는 스캔 허용)
            String extension = getFileExtension(filePath.toString()).toLowerCase();
            Set<String> criticalExtensions = Set.of(".dll", ".sys", ".com", ".scr");
            if (criticalExtensions.contains(extension)) {
                System.out.println("[SAFETY] 핵심 시스템 파일 확장자: " + extension);
                return false;
            }

            // 특수 시스템 파일명 확인
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (PROTECTED_FILENAMES.contains(fileName)) {
                System.out.println("[SAFETY] 보호된 시스템 파일: " + fileName);
                return false;
            }

            // 스캔용에서는 잠금/권한 검사 생략 (읽기만 하므로)
            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] 스캔 안전성 검사 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 파일 작업이 안전한지 확인 (실제 이동/삭제용 - 엄격한 기준)
     */
    public boolean isSafeToOperate(Path filePath) {
        if (filePath == null) {
            return false;
        }

        try {
            // 파일 존재 확인
            if (!Files.exists(filePath)) {
                return false;
            }

            // 시스템 디렉토리 확인
            String pathStr = filePath.toString().toLowerCase();
            if (PROTECTED_DIRECTORIES.stream().anyMatch(pathStr::contains)) {
                System.out.println("[SAFETY] 보호된 시스템 디렉토리: " + filePath);
                return false;
            }

            // 모든 보호된 확장자 확인 (실제 작업 시에는 엄격하게)
            String extension = getFileExtension(filePath.toString()).toLowerCase();
            if (PROTECTED_EXTENSIONS.contains(extension)) {
                System.out.println("[SAFETY] 보호된 파일 확장자: " + extension);
                return false;
            }

            // 특수 파일명 확인
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (PROTECTED_FILENAMES.contains(fileName)) {
                System.out.println("[SAFETY] 보호된 시스템 파일: " + fileName);
                return false;
            }

            // 파일 잠금 상태 확인 (실제 작업 시에만)
            if (isFileLocked(filePath)) {
                System.out.println("[SAFETY] 파일이 사용 중: " + filePath);
                return false;
            }

            // 쓰기 권한 확인 (실제 작업 시에만)
            if (!Files.isWritable(filePath)) {
                System.out.println("[SAFETY] 쓰기 권한 없음: " + filePath);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] 안전성 검사 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 파일이 잠겨있는지 확인
     */
    private boolean isFileLocked(Path filePath) {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            return false;
        } catch (IOException e) {
            return true; // 파일이 잠겨있거나 접근할 수 없음
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * 백업 생성
     */
    public Path createBackup(Path originalFile) throws IOException {
        if (!Files.exists(originalFile)) {
            throw new IOException("백업할 파일이 존재하지 않습니다: " + originalFile);
        }

        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = originalFile.getFileName() + "_backup_" + timestamp;
        Path backupPath = backupDirectory.resolve(backupName);

        try {
            Files.copy(originalFile, backupPath, 
                StandardCopyOption.COPY_ATTRIBUTES, 
                StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("[BACKUP] 백업 생성됨: " + backupPath);
            return backupPath;
        } catch (IOException e) {
            System.err.println("[ERROR] 백업 생성 실패: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 안전한 파일 이동
     */
    public boolean safeFileMove(Path source, Path target) {
        Path backupPath = null;
        
        try {
            // 안전성 검사
            if (!isSafeToOperate(source)) {
                System.err.println("[SAFETY] 파일 이동이 안전하지 않음: " + source);
                return false;
            }

            // 대상 디렉토리 생성
            Files.createDirectories(target.getParent());

            // 중요한 파일인 경우 백업 생성 (100MB 이상)
            if (Files.size(source) > 100 * 1024 * 1024) {
                backupPath = createBackup(source);
            }

            // 파일명 충돌 해결
            Path finalTarget = resolveNameConflict(target);

            // 파일 이동 (크로스 드라이브 지원)
            try {
                Files.move(source, finalTarget, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // ATOMIC_MOVE 실패 시 일반 이동 시도 (크로스 드라이브용)
                Files.move(source, finalTarget, StandardCopyOption.REPLACE_EXISTING);
            }

            // 백업 삭제 (성공 시)
            if (backupPath != null) {
                Files.deleteIfExists(backupPath);
            }

            System.out.println("[SUCCESS] 파일 이동 완료: " + source + " -> " + finalTarget);
            return true;

        } catch (IOException e) {
            System.err.println("[ERROR] 파일 이동 실패: " + e.getMessage());
            
            // 롤백 처리 (백업이 있는 경우)
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    Files.move(backupPath, source, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[ROLLBACK] 백업으로부터 파일 복원됨");
                } catch (IOException rollbackE) {
                    System.err.println("[ERROR] 롤백 실패: " + rollbackE.getMessage());
                }
            }
            
            return false;
        }
    }

    /**
     * 파일명 충돌 해결
     */
    private Path resolveNameConflict(Path target) {
        if (!Files.exists(target)) {
            return target;
        }

        Path parent = target.getParent();
        String fileName = target.getFileName().toString();
        String baseName;
        String extension;

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            baseName = fileName;
            extension = "";
        } else {
            baseName = fileName.substring(0, lastDotIndex);
            extension = fileName.substring(lastDotIndex);
        }

        int counter = 1;
        Path newTarget;
        do {
            String newFileName = baseName + "_" + counter + extension;
            newTarget = parent.resolve(newFileName);
            counter++;
        } while (Files.exists(newTarget) && counter < 1000); // 무한 루프 방지

        return newTarget;
    }

    /**
     * 안전한 파일 삭제
     */
    public boolean safeFileDelete(Path filePath) {
        try {
            // 안전성 검사
            if (!isSafeToOperate(filePath)) {
                System.err.println("[SAFETY] 파일 삭제가 안전하지 않음: " + filePath);
                return false;
            }

            // 중요한 파일인 경우 백업 생성
            Path backupPath = null;
            if (Files.size(filePath) > 10 * 1024 * 1024) { // 10MB 이상
                backupPath = createBackup(filePath);
                System.out.println("[INFO] 삭제 전 백업 생성됨: " + backupPath);
            }

            // 파일 삭제
            Files.delete(filePath);
            System.out.println("[SUCCESS] 파일 삭제 완료: " + filePath);
            
            return true;

        } catch (IOException e) {
            System.err.println("[ERROR] 파일 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 오래된 백업 파일 정리
     */
    public void cleanupOldBackups(int daysToKeep) {
        try {
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            
            Files.walk(backupDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("[CLEANUP] 오래된 백업 삭제: " + path);
                    } catch (IOException e) {
                        System.err.println("[ERROR] 백업 삭제 실패: " + e.getMessage());
                    }
                });

        } catch (IOException e) {
            System.err.println("[ERROR] 백업 정리 실패: " + e.getMessage());
        }
    }

    /**
     * 현재 위험한 작업이 진행 중인지 확인
     */
    public boolean hasActiveOperations() {
        // 실제 구현에서는 현재 진행 중인 파일 작업들을 추적
        // 여기서는 간단히 false 반환
        return false;
    }

    /**
     * 백업 디렉토리 경로 반환
     */
    public Path getBackupDirectory() {
        return backupDirectory;
    }
}