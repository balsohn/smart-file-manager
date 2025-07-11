package com.smartfilemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 사용자 정의 파일 정리 규칙 모델
 * 파일 확장자별로 어떤 폴더로 정리할지 정의합니다
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRule {
    
    /**
     * 규칙 고유 ID
     */
    private String id;
    
    /**
     * 규칙 이름 (예: "문서 파일", "이미지 파일")
     */
    private String name;
    
    /**
     * 적용할 파일 확장자 목록 (소문자, 점 제외)
     * 예: ["pdf", "doc", "docx", "txt"]
     */
    private List<String> extensions;
    
    /**
     * 타겟 폴더명 (정리 폴더 하위의 서브폴더)
     * 예: "Documents", "Images", "Videos"
     */
    private String targetFolder;
    
    /**
     * 규칙 우선순위 (1이 가장 높음)
     * 같은 확장자가 여러 규칙에 포함된 경우 우선순위가 높은 규칙 적용
     */
    private int priority;
    
    /**
     * 규칙 활성화 여부
     */
    private boolean enabled;
    
    /**
     * 규칙 설명 (선택사항)
     */
    private String description;
    
    /**
     * 규칙 생성일
     */
    private String createdDate;
    
    /**
     * 규칙 수정일
     */
    private String modifiedDate;
    
    /**
     * 특정 확장자가 이 규칙에 포함되는지 확인
     */
    public boolean containsExtension(String extension) {
        if (extensions == null || extension == null) {
            return false;
        }
        
        String normalizedExt = extension.toLowerCase().replace(".", "");
        return extensions.contains(normalizedExt);
    }
    
    /**
     * 규칙 유효성 검사
     */
    public boolean isValid() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (targetFolder == null || targetFolder.trim().isEmpty()) {
            return false;
        }
        
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }
        
        if (priority < 1 || priority > 100) {
            return false;
        }
        
        // 확장자가 모두 유효한지 확인
        for (String ext : extensions) {
            if (ext == null || ext.trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 규칙을 문자열로 표현 (디버깅용)
     */
    @Override
    public String toString() {
        return String.format("FileRule{name='%s', extensions=%s, targetFolder='%s', priority=%d, enabled=%s}", 
                           name, extensions, targetFolder, priority, enabled);
    }
    
    /**
     * 기본 규칙 생성 헬퍼 메소드
     */
    public static FileRule createDefault(String name, List<String> extensions, String targetFolder, int priority) {
        return FileRule.builder()
                .id(java.util.UUID.randomUUID().toString())
                .name(name)
                .extensions(extensions)
                .targetFolder(targetFolder)
                .priority(priority)
                .enabled(true)
                .description("기본 제공 규칙")
                .createdDate(java.time.LocalDateTime.now().toString())
                .modifiedDate(java.time.LocalDateTime.now().toString())
                .build();
    }
}