package com.smartfilemanager.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일 메타데이터 추출 유틸리티
 * 이미지 EXIF, 오디오 ID3, 비디오 메타데이터, 문서 속성 등을 추출합니다
 */
public class MetadataExtractor {

    /**
     * 메타데이터 정보를 저장하는 클래스
     */
    public static class MetadataInfo {
        private Map<String, Object> properties = new HashMap<>();
        private String cameraModel;
        private String artist;
        private String album;
        private String title;
        private String author;
        private String subject;
        private String keywords;
        private LocalDateTime dateCreated;
        private LocalDateTime dateModified;
        private String gpsLocation;
        private Integer imageWidth;
        private Integer imageHeight;
        private Integer duration; // 초 단위
        private Integer bitrate;
        private String codec;
        private String software;

        // Getters and Setters
        public Map<String, Object> getProperties() { return properties; }
        public void setProperty(String key, Object value) { properties.put(key, value); }
        public Object getProperty(String key) { return properties.get(key); }

        public String getCameraModel() { return cameraModel; }
        public void setCameraModel(String cameraModel) { this.cameraModel = cameraModel; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }

        public LocalDateTime getDateCreated() { return dateCreated; }
        public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

        public LocalDateTime getDateModified() { return dateModified; }
        public void setDateModified(LocalDateTime dateModified) { this.dateModified = dateModified; }

        public String getGpsLocation() { return gpsLocation; }
        public void setGpsLocation(String gpsLocation) { this.gpsLocation = gpsLocation; }

        public Integer getImageWidth() { return imageWidth; }
        public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }

        public Integer getImageHeight() { return imageHeight; }
        public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }

        public Integer getDuration() { return duration; }
        public void setDuration(String duration) {
            try {
                this.duration = Integer.parseInt(duration);
            } catch (NumberFormatException e) {
                this.duration = null;
            }
        }

        public Integer getBitrate() { return bitrate; }
        public void setBitrate(Integer bitrate) { this.bitrate = bitrate; }

        public String getCodec() { return codec; }
        public void setCodec(String codec) { this.codec = codec; }

        public String getSoftware() { return software; }
        public void setSoftware(String software) { this.software = software; }
    }

    /**
     * 파일에서 메타데이터를 추출합니다
     */
    public static MetadataInfo extractMetadata(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String extension = getFileExtension(filePath).toLowerCase();

            MetadataInfo metadata = new MetadataInfo();

            // 기본 파일 속성 추출
            extractBasicFileAttributes(path, metadata);

            // 파일 타입별 메타데이터 추출
            switch (extension) {
                case "jpg":
                case "jpeg":
                case "tiff":
                case "tif":
                    extractImageMetadata(path, metadata);
                    break;

                case "png":
                    extractPngMetadata(path, metadata);
                    break;

                case "mp3":
                    extractMp3Metadata(path, metadata);
                    break;

                case "mp4":
                case "mov":
                case "avi":
                    extractVideoMetadata(path, metadata);
                    break;

                case "pdf":
                    extractPdfMetadata(path, metadata);
                    break;

                case "doc":
                case "docx":
                    extractWordMetadata(path, metadata);
                    break;

                default:
                    // 다른 파일 타입에 대한 일반적인 처리
                    extractGenericMetadata(path, metadata);
                    break;
            }

            return metadata;

        } catch (Exception e) {
            System.err.println("[ERROR] 메타데이터 추출 실패: " + filePath + " - " + e.getMessage());
            return new MetadataInfo();
        }
    }

    /**
     * 기본 파일 속성 추출
     */
    private static void extractBasicFileAttributes(Path path, MetadataInfo metadata) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        metadata.setDateCreated(LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault()));
        metadata.setDateModified(LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

        metadata.setProperty("fileSize", attrs.size());
        metadata.setProperty("isDirectory", attrs.isDirectory());
        metadata.setProperty("isRegularFile", attrs.isRegularFile());
    }

    /**
     * JPEG/TIFF 이미지 메타데이터 추출 (간단한 EXIF 구현)
     */
    private static void extractImageMetadata(Path path, MetadataInfo metadata) {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            // 간단한 EXIF 데이터 읽기 (실제로는 metadata-extractor 라이브러리 권장)
            System.out.println("[WARNING] 완전한 EXIF 데이터 추출을 위해서는 metadata-extractor 라이브러리 사용을 권장합니다.");

            // JPEG 파일 확인
            byte[] header = new byte[4];
            fis.read(header);

            if (header[0] == (byte)0xFF && header[1] == (byte)0xD8) {
                // JPEG 파일 - 기본 정보만 추출
                extractJpegBasicInfo(path, metadata);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 이미지 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * JPEG 기본 정보 추출
     */
    private static void extractJpegBasicInfo(Path path, MetadataInfo metadata) {
        try {
            // 파일 이름에서 카메라 정보 추출 시도
            String fileName = path.getFileName().toString();

            // 일반적인 카메라 파일명 패턴
            if (fileName.matches("IMG_\\d+.*") || fileName.matches("DSC\\d+.*")) {
                metadata.setProperty("cameraFile", true);
            }

            // 스크린샷 패턴 감지
            if (fileName.toLowerCase().contains("screenshot") ||
                    fileName.toLowerCase().contains("screen shot") ||
                    fileName.matches(".*\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}.*")) {
                metadata.setProperty("isScreenshot", true);
            }

            metadata.setProperty("imageFormat", "JPEG");

        } catch (Exception e) {
            System.err.println("[ERROR] JPEG 기본 정보 추출 실패: " + e.getMessage());
        }
    }

    /**
     * PNG 메타데이터 추출
     */
    private static void extractPngMetadata(Path path, MetadataInfo metadata) {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            byte[] header = new byte[24];
            fis.read(header);

            // PNG 시그니처 확인
            if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                // IHDR 청크에서 크기 정보 추출
                if (header[12] == 'I' && header[13] == 'H' && header[14] == 'D' && header[15] == 'R') {
                    int width = bytesToInt(Arrays.copyOfRange(header, 16, 20));
                    int height = bytesToInt(Arrays.copyOfRange(header, 20, 24));

                    metadata.setImageWidth(width);
                    metadata.setImageHeight(height);
                    metadata.setProperty("imageFormat", "PNG");
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] PNG 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * MP3 메타데이터 추출 (ID3 태그)
     */
    private static void extractMp3Metadata(Path path, MetadataInfo metadata) {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            // ID3v2 태그 확인
            byte[] header = new byte[10];
            fis.read(header);

            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                System.out.println("[WARNING] 완전한 ID3 태그 추출을 위해서는 JAudioTagger 라이브러리 사용을 권장합니다.");

                // 파일명에서 정보 추출 시도
                String fileName = path.getFileName().toString();
                extractAudioInfoFromFilename(fileName, metadata);

                metadata.setProperty("audioFormat", "MP3");
                metadata.setProperty("hasID3", true);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] MP3 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 파일명에서 오디오 정보 추출
     */
    private static void extractAudioInfoFromFilename(String fileName, MetadataInfo metadata) {
        // 일반적인 음악 파일명 패턴: "Artist - Title.mp3" 또는 "Title - Artist.mp3"
        Pattern artistTitlePattern = Pattern.compile("(.+?)\\s*-\\s*(.+?)\\.(mp3|wav|flac)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = artistTitlePattern.matcher(fileName);

        if (matcher.find()) {
            String part1 = matcher.group(1).trim();
            String part2 = matcher.group(2).trim();

            // 첫 번째 부분이 아티스트일 가능성이 높음
            metadata.setArtist(part1);
            metadata.setTitle(part2);
        }

        // 앨범 정보 (폴더명에서 추출)
        String parentFolder = new File(fileName).getParent();
        if (parentFolder != null) {
            metadata.setAlbum(new File(parentFolder).getName());
        }
    }

    /**
     * 비디오 메타데이터 추출
     */
    private static void extractVideoMetadata(Path path, MetadataInfo metadata) {
        try {
            String fileName = path.getFileName().toString();

            // 비디오 형식 감지
            String extension = getFileExtension(fileName).toLowerCase();
            metadata.setProperty("videoFormat", extension.toUpperCase());

            // 파일명에서 해상도 정보 추출 시도
            extractVideoInfoFromFilename(fileName, metadata);

            // 파일 크기로 대략적인 품질 추정
            long fileSize = Files.size(path);
            if (fileSize > 1024 * 1024 * 1024) { // 1GB 이상
                metadata.setProperty("quality", "고화질");
            } else if (fileSize > 100 * 1024 * 1024) { // 100MB 이상
                metadata.setProperty("quality", "중간화질");
            } else {
                metadata.setProperty("quality", "저화질");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 비디오 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 파일명에서 비디오 정보 추출
     */
    private static void extractVideoInfoFromFilename(String fileName, MetadataInfo metadata) {
        // 해상도 패턴 감지
        Pattern resolutionPattern = Pattern.compile("(\\d{3,4})[xX×](\\d{3,4})");
        Matcher matcher = resolutionPattern.matcher(fileName);

        if (matcher.find()) {
            try {
                int width = Integer.parseInt(matcher.group(1));
                int height = Integer.parseInt(matcher.group(2));
                metadata.setImageWidth(width);
                metadata.setImageHeight(height);
            } catch (NumberFormatException e) {
                // 무시
            }
        }

        // 일반적인 해상도 키워드
        if (fileName.toLowerCase().contains("1080p") || fileName.toLowerCase().contains("fhd")) {
            metadata.setImageWidth(1920);
            metadata.setImageHeight(1080);
        } else if (fileName.toLowerCase().contains("720p") || fileName.toLowerCase().contains("hd")) {
            metadata.setImageWidth(1280);
            metadata.setImageHeight(720);
        } else if (fileName.toLowerCase().contains("4k") || fileName.toLowerCase().contains("uhd")) {
            metadata.setImageWidth(3840);
            metadata.setImageHeight(2160);
        }

        // TV 시리즈 정보 추출
        Pattern tvPattern = Pattern.compile("S(\\d+)E(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher tvMatcher = tvPattern.matcher(fileName);
        if (tvMatcher.find()) {
            metadata.setProperty("season", tvMatcher.group(1));
            metadata.setProperty("episode", tvMatcher.group(2));
            metadata.setProperty("isTvShow", true);
        }
    }

    /**
     * PDF 메타데이터 추출
     */
    private static void extractPdfMetadata(Path path, MetadataInfo metadata) {
        try {
            System.out.println("[WARNING] 완전한 PDF 메타데이터 추출을 위해서는 Apache PDFBox 라이브러리 사용을 권장합니다.");

            // 간단한 PDF 정보만 추출
            metadata.setProperty("documentFormat", "PDF");

            // 파일명에서 정보 추출
            String fileName = path.getFileName().toString();
            extractDocumentInfoFromFilename(fileName, metadata);

        } catch (Exception e) {
            System.err.println("[ERROR] PDF 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * Word 문서 메타데이터 추출
     */
    private static void extractWordMetadata(Path path, MetadataInfo metadata) {
        try {
            System.out.println("[WARNING] 완전한 Word 메타데이터 추출을 위해서는 Apache POI 라이브러리 사용을 권장합니다.");

            String extension = getFileExtension(path.toString()).toLowerCase();
            metadata.setProperty("documentFormat", extension.equals("docx") ? "DOCX" : "DOC");

            // 파일명에서 정보 추출
            String fileName = path.getFileName().toString();
            extractDocumentInfoFromFilename(fileName, metadata);

        } catch (Exception e) {
            System.err.println("[ERROR] Word 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 파일명에서 문서 정보 추출
     */
    private static void extractDocumentInfoFromFilename(String fileName, MetadataInfo metadata) {
        // 날짜 패턴 추출
        Pattern datePattern = Pattern.compile("(\\d{4})[\\-_](\\d{2})[\\-_](\\d{2})");
        Matcher matcher = datePattern.matcher(fileName);

        if (matcher.find()) {
            metadata.setProperty("documentDate", matcher.group());
        }

        // 문서 타입 추측
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("report") || lowerName.contains("보고서")) {
            metadata.setProperty("documentType", "보고서");
        } else if (lowerName.contains("manual") || lowerName.contains("매뉴얼")) {
            metadata.setProperty("documentType", "매뉴얼");
        } else if (lowerName.contains("invoice") || lowerName.contains("영수증")) {
            metadata.setProperty("documentType", "영수증");
        } else if (lowerName.contains("contract") || lowerName.contains("계약")) {
            metadata.setProperty("documentType", "계약서");
        }
    }

    /**
     * 일반적인 메타데이터 추출
     */
    private static void extractGenericMetadata(Path path, MetadataInfo metadata) {
        try {
            String fileName = path.getFileName().toString();
            String extension = getFileExtension(fileName);

            metadata.setProperty("fileExtension", extension);
            metadata.setProperty("fileName", fileName);

            // 파일명에서 일반적인 패턴 추출
            if (fileName.toLowerCase().contains("backup") || fileName.toLowerCase().contains("백업")) {
                metadata.setProperty("isBackup", true);
            }

            if (fileName.toLowerCase().contains("temp") || fileName.toLowerCase().contains("임시")) {
                metadata.setProperty("isTemporary", true);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 일반 메타데이터 추출 실패: " + e.getMessage());
        }
    }

    // 헬퍼 메서드들

    /**
     * 파일 확장자 추출
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }

    /**
     * 바이트 배열을 정수로 변환 (빅 엔디안)
     */
    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

    /**
     * 메타데이터 요약 정보 생성
     */
    public static String generateSummary(MetadataInfo metadata) {
        StringBuilder summary = new StringBuilder();

        if (metadata.getTitle() != null) {
            summary.append("제목: ").append(metadata.getTitle()).append("\n");
        }

        if (metadata.getArtist() != null) {
            summary.append("아티스트: ").append(metadata.getArtist()).append("\n");
        }

        if (metadata.getAuthor() != null) {
            summary.append("작성자: ").append(metadata.getAuthor()).append("\n");
        }

        if (metadata.getImageWidth() != null && metadata.getImageHeight() != null) {
            summary.append("해상도: ").append(metadata.getImageWidth())
                    .append(" × ").append(metadata.getImageHeight()).append("\n");
        }

        if (metadata.getCameraModel() != null) {
            summary.append("카메라: ").append(metadata.getCameraModel()).append("\n");
        }

        if (metadata.getDuration() != null) {
            summary.append("재생시간: ").append(formatDuration(metadata.getDuration())).append("\n");
        }

        return summary.toString();
    }

    /**
     * 재생시간 포맷팅
     */
    private static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
}