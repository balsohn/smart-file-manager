package com.smartfilemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 파일 정보를 저장하는 모델 클래스
 * 스캔된 파일의 기본 정보와 분석 결과를 포함
 */
@Data                    // getter, setter, equals, hashCode, toString 자동 생성
@NoArgsConstructor       // 매개변수 없는 생성자 생성
@AllArgsConstructor      // 모든 필드를 매개변수로 받는 생성자 생성
@Builder                 // 빌더 패턴 지원
@ToString(exclude = {"errorMessage"}) // toString에서 errorMessage 제외
public class FileInfo {

    // 기본 파일 정보
    private String fileName;           // 파일명 (확장자 포함)
    private String filePath;           // 전체 경로
    private String originalLocation;   // 원본 위치 (정리 전 경로)
    private long fileSize;             // 파일 크기 (바이트)
    private String fileExtension;      // 파일 확장자 (소문자)
    private String mimeType;           // MIME 타입
    private LocalDateTime createdDate; // 생성 날짜
    private LocalDateTime modifiedDate;// 수정 날짜

    // 분석 결과
    private String detectedCategory;    // "Documents", "Images", "Videos", "Audio", "Archives" 등
    private String detectedSubCategory; // "Work", "Screenshots", "Movies" 등 세부 분류
    private String suggestedPath;       // 제안된 정리 경로
    private double confidenceScore;     // 분류 신뢰도 (0.0 ~ 1.0)

    // 메타데이터 (선택사항)
    private String extractedTitle;      // 문서에서 추출한 제목
    private String extractedAuthor;     // 문서에서 추출한 작성자
    private String description;         // 파일 설명
    private List<String> keywords;      // 키워드 목록 (AI 분석, 파일명 분석 등에서 추출)

    // 처리 상태
    private ProcessingStatus status;    // 처리 상태 열거형
    private String errorMessage;        // 오류 메시지 (처리 실패 시)
    private LocalDateTime processedAt;  // 처리 완료 시간
    
    // UI 상태
    private boolean selected;           // 선택 상태 (체크박스용)

    // 파일명과 경로로 생성하는 편의 생성자
    public FileInfo(String fileName, String filePath) {
        this();
        this.fileName = fileName;
        this.filePath = filePath;
        this.originalLocation = filePath;
        this.fileExtension = extractExtension(fileName);
        this.status = ProcessingStatus.PENDING;
        this.confidenceScore = 0.0;
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.keywords = new ArrayList<>(); // 키워드 리스트 초기화
        this.selected = true; // 기본적으로 선택된 상태로 설정
    }

    // 확장자 추출 메서드
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "unknown";
    }

    // 키워드 관련 유틸리티 메서드들

    /**
     * 키워드 추가
     */
    public void addKeyword(String keyword) {
        if (keywords == null) {
            keywords = new ArrayList<>();
        }
        if (keyword != null && !keyword.trim().isEmpty() && !keywords.contains(keyword.trim())) {
            keywords.add(keyword.trim());
        }
    }

    /**
     * 여러 키워드 추가
     */
    public void addKeywords(List<String> newKeywords) {
        if (newKeywords != null) {
            for (String keyword : newKeywords) {
                addKeyword(keyword);
            }
        }
    }

    /**
     * 키워드 제거
     */
    public void removeKeyword(String keyword) {
        if (keywords != null) {
            keywords.remove(keyword);
        }
    }

    /**
     * 키워드 목록 초기화
     */
    public void clearKeywords() {
        if (keywords == null) {
            keywords = new ArrayList<>();
        } else {
            keywords.clear();
        }
    }

    /**
     * 특정 키워드 포함 여부 확인
     */
    public boolean hasKeyword(String keyword) {
        return keywords != null && keywords.contains(keyword);
    }

    /**
     * 키워드 개수 반환
     */
    public int getKeywordCount() {
        return keywords != null ? keywords.size() : 0;
    }

    // 기존 유틸리티 메서드들

    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 포맷
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    /**
     * 파일이 이미지인지 확인
     */
    public boolean isImage() {
        return fileExtension != null &&
                (fileExtension.equals("jpg") || fileExtension.equals("jpeg") ||
                        fileExtension.equals("png") || fileExtension.equals("gif") ||
                        fileExtension.equals("bmp") || fileExtension.equals("svg") ||
                        fileExtension.equals("webp"));
    }

    /**
     * 파일이 문서인지 확인
     */
    public boolean isDocument() {
        return fileExtension != null &&
                (fileExtension.equals("pdf") || fileExtension.equals("doc") ||
                        fileExtension.equals("docx") || fileExtension.equals("txt") ||
                        fileExtension.equals("rtf") || fileExtension.equals("odt"));
    }

    /**
     * 파일이 비디오인지 확인
     */
    public boolean isVideo() {
        return fileExtension != null &&
                (fileExtension.equals("mp4") || fileExtension.equals("avi") ||
                        fileExtension.equals("mkv") || fileExtension.equals("mov") ||
                        fileExtension.equals("wmv") || fileExtension.equals("flv") ||
                        fileExtension.equals("webm"));
    }

    /**
     * 파일이 오디오인지 확인
     */
    public boolean isAudio() {
        return fileExtension != null &&
                (fileExtension.equals("mp3") || fileExtension.equals("wav") ||
                        fileExtension.equals("flac") || fileExtension.equals("aac") ||
                        fileExtension.equals("m4a") || fileExtension.equals("ogg"));
    }

    /**
     * 파일이 압축 파일인지 확인
     */
    public boolean isArchive() {
        return fileExtension != null &&
                (fileExtension.equals("zip") || fileExtension.equals("rar") ||
                        fileExtension.equals("7z") || fileExtension.equals("tar") ||
                        fileExtension.equals("gz") || fileExtension.equals("bz2"));
    }

    /**
     * 파일의 기본 카테고리를 자동 감지
     */
    public String autoDetectCategory() {
        if (isImage()) return "Images";
        if (isDocument()) return "Documents";
        if (isVideo()) return "Videos";
        if (isAudio()) return "Audio";
        if (isArchive()) return "Archives";
        return "Others";
    }

    /**
     * FileInfo 빌더에 기본값 설정하는 정적 메서드
     */
    public static FileInfoBuilder defaultBuilder() {
        return FileInfo.builder()
                .status(ProcessingStatus.PENDING)
                .confidenceScore(0.0)
                .createdDate(LocalDateTime.now())
                .modifiedDate(LocalDateTime.now())
                .keywords(new ArrayList<>()); // 키워드 리스트 초기화
    }

    /**
     * 파일 정보 요약 반환
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("파일: ").append(fileName).append("\n");
        summary.append("카테고리: ").append(detectedCategory != null ? detectedCategory : "미분류").append("\n");
        summary.append("크기: ").append(getFormattedFileSize()).append("\n");
        summary.append("상태: ").append(status != null ? status.getDisplayName() : "알 수 없음").append("\n");

        if (keywords != null && !keywords.isEmpty()) {
            summary.append("키워드: ").append(String.join(", ", keywords)).append("\n");
        }

        if (confidenceScore > 0) {
            summary.append("신뢰도: ").append(String.format("%.0f%%", confidenceScore * 100)).append("\n");
        }

        return summary.toString();
    }

    /**
     * 키워드를 문자열로 반환
     */
    public String getKeywordsAsString() {
        return keywords != null ? String.join(", ", keywords) : "";
    }

    /**
     * 파일이 분석 완료되었는지 확인
     */
    public boolean isAnalyzed() {
        return status == ProcessingStatus.ANALYZED ||
                status == ProcessingStatus.ORGANIZED;
    }

    /**
     * 파일이 정리 가능한 상태인지 확인
     */
    public boolean isReadyForOrganization() {
        return status == ProcessingStatus.ANALYZED &&
                suggestedPath != null &&
                !suggestedPath.trim().isEmpty();
    }

    /**
     * 오류가 있는지 확인
     */
    public boolean hasError() {
        return status == ProcessingStatus.FAILED &&
                errorMessage != null &&
                !errorMessage.trim().isEmpty();
    }
}