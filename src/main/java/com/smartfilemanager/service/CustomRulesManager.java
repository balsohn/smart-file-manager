package com.smartfilemanager.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.smartfilemanager.model.FileRule;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 정의 파일 정리 규칙 관리 서비스
 * JSON 파일 기반으로 커스텀 규칙을 저장/로드하고 관리합니다
 */
public class CustomRulesManager {
    
    private static final String DEFAULT_RULES_FILE = "custom-rules.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private List<FileRule> rules;
    private String rulesFilePath;
    
    public CustomRulesManager() {
        this(getDefaultRulesPath());
    }
    
    public CustomRulesManager(String rulesFilePath) {
        this.rulesFilePath = rulesFilePath;
        this.rules = new ArrayList<>();
        loadRules();
    }
    
    /**
     * 기본 규칙 파일 경로 반환
     */
    private static String getDefaultRulesPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".smartfilemanager", DEFAULT_RULES_FILE).toString();
    }
    
    /**
     * 규칙 파일 로드
     */
    public void loadRules() {
        try {
            Path path = Paths.get(rulesFilePath);
            
            // 파일이 없으면 기본 규칙 생성
            if (!Files.exists(path)) {
                createDefaultRules();
                saveRules();
                return;
            }
            
            String json = Files.readString(path);
            Type listType = new TypeToken<List<FileRule>>(){}.getType();
            rules = gson.fromJson(json, listType);
            
            if (rules == null) {
                rules = new ArrayList<>();
                createDefaultRules();
            }
            
            System.out.println("[CustomRules] " + rules.size() + "개 규칙 로드됨: " + rulesFilePath);
            
        } catch (Exception e) {
            System.err.println("[ERROR] 규칙 파일 로드 실패: " + e.getMessage());
            rules = new ArrayList<>();
            createDefaultRules();
        }
    }
    
    /**
     * 규칙 파일 저장
     */
    public void saveRules() {
        try {
            Path path = Paths.get(rulesFilePath);
            Files.createDirectories(path.getParent());
            
            String json = gson.toJson(rules);
            Files.writeString(path, json);
            
            System.out.println("[CustomRules] " + rules.size() + "개 규칙 저장됨: " + rulesFilePath);
            
        } catch (Exception e) {
            System.err.println("[ERROR] 규칙 파일 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * 기본 규칙 생성
     */
    private void createDefaultRules() {
        rules.clear();
        
        // 문서 파일
        rules.add(FileRule.createDefault(
            "문서 파일",
            Arrays.asList("pdf", "doc", "docx", "txt", "rtf", "odt", "hwp"),
            "Documents",
            1
        ));
        
        // 이미지 파일
        rules.add(FileRule.createDefault(
            "이미지 파일",
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff"),
            "Images",
            2
        ));
        
        // 비디오 파일
        rules.add(FileRule.createDefault(
            "비디오 파일",
            Arrays.asList("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v"),
            "Videos",
            3
        ));
        
        // 오디오 파일
        rules.add(FileRule.createDefault(
            "오디오 파일",
            Arrays.asList("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma"),
            "Audio",
            4
        ));
        
        // 압축 파일
        rules.add(FileRule.createDefault(
            "압축 파일",
            Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2", "xz"),
            "Archives",
            5
        ));
        
        // 실행 파일
        rules.add(FileRule.createDefault(
            "실행 파일",
            Arrays.asList("exe", "msi", "dmg", "pkg", "deb", "rpm", "appimage"),
            "Applications",
            6
        ));
        
        // 코드 파일
        rules.add(FileRule.createDefault(
            "코드 파일",
            Arrays.asList("java", "js", "html", "css", "py", "cpp", "c", "h", "php", "rb", "go"),
            "Code",
            7
        ));
        
        System.out.println("[CustomRules] 기본 규칙 " + rules.size() + "개 생성됨");
    }
    
    /**
     * 모든 규칙 조회
     */
    public List<FileRule> getAllRules() {
        return new ArrayList<>(rules);
    }
    
    /**
     * 활성화된 규칙만 조회 (우선순위 순)
     */
    public List<FileRule> getEnabledRules() {
        return rules.stream()
                .filter(FileRule::isEnabled)
                .sorted(Comparator.comparingInt(FileRule::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 확장자에 맞는 규칙 찾기
     */
    public Optional<FileRule> findRuleForExtension(String extension) {
        if (extension == null) return Optional.empty();
        
        String normalizedExt = extension.toLowerCase().replace(".", "");
        
        return getEnabledRules().stream()
                .filter(rule -> rule.containsExtension(normalizedExt))
                .findFirst();
    }
    
    /**
     * 파일명으로 카테고리 결정
     */
    public String determineCategory(String fileName) {
        if (fileName == null) return "Others";
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "Others";
        
        String extension = fileName.substring(lastDot + 1);
        Optional<FileRule> rule = findRuleForExtension(extension);
        
        return rule.map(FileRule::getTargetFolder).orElse("Others");
    }
    
    /**
     * 규칙 추가
     */
    public void addRule(FileRule rule) {
        if (rule == null || !rule.isValid()) {
            throw new IllegalArgumentException("유효하지 않은 규칙입니다");
        }
        
        // ID가 없으면 생성
        if (rule.getId() == null) {
            rule.setId(UUID.randomUUID().toString());
        }
        
        // 생성/수정 시간 설정
        String now = LocalDateTime.now().toString();
        if (rule.getCreatedDate() == null) {
            rule.setCreatedDate(now);
        }
        rule.setModifiedDate(now);
        
        rules.add(rule);
        saveRules();
        
        System.out.println("[CustomRules] 규칙 추가됨: " + rule.getName());
    }
    
    /**
     * 규칙 수정
     */
    public void updateRule(FileRule updatedRule) {
        if (updatedRule == null || !updatedRule.isValid()) {
            throw new IllegalArgumentException("유효하지 않은 규칙입니다");
        }
        
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(updatedRule.getId())) {
                updatedRule.setModifiedDate(LocalDateTime.now().toString());
                rules.set(i, updatedRule);
                saveRules();
                System.out.println("[CustomRules] 규칙 수정됨: " + updatedRule.getName());
                return;
            }
        }
        
        throw new IllegalArgumentException("수정할 규칙을 찾을 수 없습니다: " + updatedRule.getId());
    }
    
    /**
     * 규칙 삭제
     */
    public void deleteRule(String ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException("규칙 ID가 필요합니다");
        }
        
        boolean removed = rules.removeIf(rule -> rule.getId().equals(ruleId));
        
        if (removed) {
            saveRules();
            System.out.println("[CustomRules] 규칙 삭제됨: " + ruleId);
        } else {
            throw new IllegalArgumentException("삭제할 규칙을 찾을 수 없습니다: " + ruleId);
        }
    }
    
    /**
     * 규칙 활성화/비활성화
     */
    public void toggleRule(String ruleId, boolean enabled) {
        rules.stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .ifPresentOrElse(
                    rule -> {
                        rule.setEnabled(enabled);
                        rule.setModifiedDate(LocalDateTime.now().toString());
                        saveRules();
                        System.out.println("[CustomRules] 규칙 " + (enabled ? "활성화" : "비활성화") + ": " + rule.getName());
                    },
                    () -> {
                        throw new IllegalArgumentException("규칙을 찾을 수 없습니다: " + ruleId);
                    }
                );
    }
    
    /**
     * 규칙 우선순위 변경
     */
    public void updatePriority(String ruleId, int newPriority) {
        rules.stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .ifPresentOrElse(
                    rule -> {
                        rule.setPriority(newPriority);
                        rule.setModifiedDate(LocalDateTime.now().toString());
                        saveRules();
                        System.out.println("[CustomRules] 우선순위 변경: " + rule.getName() + " -> " + newPriority);
                    },
                    () -> {
                        throw new IllegalArgumentException("규칙을 찾을 수 없습니다: " + ruleId);
                    }
                );
    }
    
    /**
     * 규칙 파일 내보내기
     */
    public void exportRules(String exportPath) throws IOException {
        String json = gson.toJson(rules);
        Files.writeString(Paths.get(exportPath), json);
        System.out.println("[CustomRules] 규칙 내보내기 완료: " + exportPath);
    }
    
    /**
     * 규칙 파일 가져오기
     */
    public void importRules(String importPath) throws IOException {
        String json = Files.readString(Paths.get(importPath));
        Type listType = new TypeToken<List<FileRule>>(){}.getType();
        List<FileRule> importedRules = gson.fromJson(json, listType);
        
        if (importedRules != null) {
            rules.clear();
            rules.addAll(importedRules);
            saveRules();
            System.out.println("[CustomRules] 규칙 가져오기 완료: " + importedRules.size() + "개");
        }
    }
    
    /**
     * 충돌하는 확장자 찾기
     */
    public Map<String, List<FileRule>> findConflictingExtensions() {
        Map<String, List<FileRule>> conflicts = new HashMap<>();
        
        for (FileRule rule : getEnabledRules()) {
            for (String ext : rule.getExtensions()) {
                conflicts.computeIfAbsent(ext, k -> new ArrayList<>()).add(rule);
            }
        }
        
        // 2개 이상의 규칙이 같은 확장자를 가진 경우만 반환
        return conflicts.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }
    
    /**
     * 규칙 통계 조회
     */
    public Map<String, Object> getRulesStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRules", rules.size());
        stats.put("enabledRules", getEnabledRules().size());
        stats.put("totalExtensions", rules.stream()
                .flatMap(rule -> rule.getExtensions().stream())
                .distinct()
                .count());
        stats.put("conflictingExtensions", findConflictingExtensions().size());
        
        return stats;
    }
    
    /**
     * 규칙 파일 경로 변경
     */
    public void setRulesFilePath(String newPath) {
        this.rulesFilePath = newPath;
        loadRules();
    }
    
    /**
     * 현재 규칙 파일 경로 조회
     */
    public String getRulesFilePath() {
        return rulesFilePath;
    }
}