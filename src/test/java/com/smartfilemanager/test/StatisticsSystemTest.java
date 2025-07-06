package com.smartfilemanager.test;

import com.smartfilemanager.controller.StatisticsController;
import com.smartfilemanager.model.FileInfo;
import com.smartfilemanager.model.ProcessingStatus;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 통계 시스템 단독 테스트 애플리케이션
 * 메인 애플리케이션 없이 통계 기능만 테스트할 수 있습니다
 */
public class StatisticsSystemTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("🚀 통계 시스템 테스트 시작!");

        // FXML 로드
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics.fxml"));
        javafx.scene.Parent root = loader.load();

        // 컨트롤러 가져오기
        StatisticsController controller = loader.getController();

        // 테스트 데이터 생성
        ObservableList<FileInfo> testData = generateTestData();
        System.out.println("📊 테스트 데이터 생성 완료: " + testData.size() + "개 파일");

        // 컨트롤러에 데이터 전달
        controller.updateFileList(testData);

        // 씬 설정
        Scene scene = new Scene(root, 1200, 800);

        // CSS 로드
        scene.getStylesheets().addAll(
                getClass().getResource("/css/styles.css").toExternalForm(),
                getClass().getResource("/css/statistics-styles.css").toExternalForm()
        );

        // 스테이지 설정
        primaryStage.setTitle("📊 통계 시스템 테스트");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("🏁 통계 시스템 테스트 종료");
            System.exit(0);
        });

        primaryStage.show();

        // 테스트 결과 출력
        printTestSummary(testData);
    }

    /**
     * 테스트용 파일 데이터 생성
     */
    private ObservableList<FileInfo> generateTestData() {
        List<FileInfo> testFiles = new ArrayList<>();
        Random random = new Random();

        // 1. 문서 파일들 생성
        testFiles.addAll(createDocumentFiles(random, 45));

        // 2. 이미지 파일들 생성
        testFiles.addAll(createImageFiles(random, 68));

        // 3. 비디오 파일들 생성
        testFiles.addAll(createVideoFiles(random, 23));

        // 4. 오디오 파일들 생성
        testFiles.addAll(createAudioFiles(random, 31));

        // 5. 기타 파일들 생성
        testFiles.addAll(createOtherFiles(random, 19));

        // 6. 일부 파일을 정리 완료 상태로 변경
        markSomeAsOrganized(testFiles, random);

        return FXCollections.observableArrayList(testFiles);
    }

    /**
     * 문서 파일 생성
     */
    private List<FileInfo> createDocumentFiles(Random random, int count) {
        List<FileInfo> files = new ArrayList<>();
        String[] docTypes = {"pdf", "docx", "xlsx", "pptx", "txt"};
        String[] docCategories = {"Reports", "Work", "Personal", "Manual"};

        for (int i = 0; i < count; i++) {
            FileInfo file = new FileInfo();
            String ext = docTypes[random.nextInt(docTypes.length)];

            file.setFileName("document_" + (i + 1) + "." + ext);
            file.setFilePath("C:/TestFolder/Documents/" + file.getFileName());
            file.setFileSize(random.nextLong(50 * 1024 * 1024)); // 0-50MB
            file.setFileExtension(ext);
            file.setDetectedCategory("Documents");
            file.setDetectedSubCategory(docCategories[random.nextInt(docCategories.length)]);
            file.setConfidenceScore(0.8 + random.nextDouble() * 0.2);
            file.setStatus(ProcessingStatus.ANALYZED);
            file.setCreatedDate(generateRandomDate(random));
            file.setModifiedDate(generateRandomDate(random));

            files.add(file);
        }

        return files;
    }

    /**
     * 이미지 파일 생성
     */
    private List<FileInfo> createImageFiles(Random random, int count) {
        List<FileInfo> files = new ArrayList<>();
        String[] imgTypes = {"jpg", "png", "gif", "bmp", "tiff"};
        String[] imgCategories = {"Screenshots", "Photos", "Wallpapers", "Icons"};

        for (int i = 0; i < count; i++) {
            FileInfo file = new FileInfo();
            String ext = imgTypes[random.nextInt(imgTypes.length)];

            file.setFileName("image_" + (i + 1) + "." + ext);
            file.setFilePath("C:/TestFolder/Images/" + file.getFileName());
            file.setFileSize(random.nextLong(10 * 1024 * 1024)); // 0-10MB
            file.setFileExtension(ext);
            file.setDetectedCategory("Images");
            file.setDetectedSubCategory(imgCategories[random.nextInt(imgCategories.length)]);
            file.setConfidenceScore(0.9 + random.nextDouble() * 0.1);
            file.setStatus(ProcessingStatus.ANALYZED);
            file.setCreatedDate(generateRandomDate(random));
            file.setModifiedDate(generateRandomDate(random));

            files.add(file);
        }

        return files;
    }

    /**
     * 비디오 파일 생성
     */
    private List<FileInfo> createVideoFiles(Random random, int count) {
        List<FileInfo> files = new ArrayList<>();
        String[] videoTypes = {"mp4", "avi", "mkv", "mov", "wmv"};
        String[] videoCategories = {"Movies", "TV Shows", "Educational", "Personal"};

        for (int i = 0; i < count; i++) {
            FileInfo file = new FileInfo();
            String ext = videoTypes[random.nextInt(videoTypes.length)];

            file.setFileName("video_" + (i + 1) + "." + ext);
            file.setFilePath("C:/TestFolder/Videos/" + file.getFileName());
            file.setFileSize(random.nextLong(2L * 1024 * 1024 * 1024)); // 0-2GB
            file.setFileExtension(ext);
            file.setDetectedCategory("Videos");
            file.setDetectedSubCategory(videoCategories[random.nextInt(videoCategories.length)]);
            file.setConfidenceScore(0.85 + random.nextDouble() * 0.15);
            file.setStatus(ProcessingStatus.ANALYZED);
            file.setCreatedDate(generateRandomDate(random));
            file.setModifiedDate(generateRandomDate(random));

            files.add(file);
        }

        return files;
    }

    /**
     * 오디오 파일 생성
     */
    private List<FileInfo> createAudioFiles(Random random, int count) {
        List<FileInfo> files = new ArrayList<>();
        String[] audioTypes = {"mp3", "wav", "flac", "m4a", "ogg"};
        String[] audioCategories = {"Music", "Podcasts", "Audiobooks", "SFX"};

        for (int i = 0; i < count; i++) {
            FileInfo file = new FileInfo();
            String ext = audioTypes[random.nextInt(audioTypes.length)];

            file.setFileName("audio_" + (i + 1) + "." + ext);
            file.setFilePath("C:/TestFolder/Audio/" + file.getFileName());
            file.setFileSize(random.nextLong(100 * 1024 * 1024)); // 0-100MB
            file.setFileExtension(ext);
            file.setDetectedCategory("Audio");
            file.setDetectedSubCategory(audioCategories[random.nextInt(audioCategories.length)]);
            file.setConfidenceScore(0.75 + random.nextDouble() * 0.25);
            file.setStatus(ProcessingStatus.ANALYZED);
            file.setCreatedDate(generateRandomDate(random));
            file.setModifiedDate(generateRandomDate(random));

            files.add(file);
        }

        return files;
    }

    /**
     * 기타 파일 생성
     */
    private List<FileInfo> createOtherFiles(Random random, int count) {
        List<FileInfo> files = new ArrayList<>();
        String[] otherTypes = {"zip", "exe", "dll", "log", "tmp"};

        for (int i = 0; i < count; i++) {
            FileInfo file = new FileInfo();
            String ext = otherTypes[random.nextInt(otherTypes.length)];

            file.setFileName("file_" + (i + 1) + "." + ext);
            file.setFilePath("C:/TestFolder/Others/" + file.getFileName());
            file.setFileSize(random.nextLong(500 * 1024 * 1024)); // 0-500MB
            file.setFileExtension(ext);
            file.setDetectedCategory("Others");
            file.setDetectedSubCategory("Misc");
            file.setConfidenceScore(0.6 + random.nextDouble() * 0.4);
            file.setStatus(ProcessingStatus.ANALYZED);
            file.setCreatedDate(generateRandomDate(random));
            file.setModifiedDate(generateRandomDate(random));

            files.add(file);
        }

        return files;
    }

    /**
     * 일부 파일을 정리 완료 상태로 변경
     */
    private void markSomeAsOrganized(List<FileInfo> files, Random random) {
        int organizedCount = files.size() * 60 / 100; // 60% 정리 완료

        for (int i = 0; i < organizedCount; i++) {
            FileInfo file = files.get(i);
            file.setStatus(ProcessingStatus.ORGANIZED);
            file.setProcessedAt(generateRandomDate(random));

            // 정리된 파일의 새 경로 설정
            String newPath = "C:/OrganizedFiles/" + file.getDetectedCategory() + "/" +
                    file.getDetectedSubCategory() + "/" + file.getFileName();
            file.setSuggestedPath(newPath);
        }

        // 일부는 실패 상태로
        int failedCount = files.size() * 5 / 100; // 5% 실패
        for (int i = organizedCount; i < organizedCount + failedCount; i++) {
            if (i < files.size()) {
                FileInfo file = files.get(i);
                file.setStatus(ProcessingStatus.FAILED);
                file.setErrorMessage("파일 이동 권한 없음");
            }
        }
    }

    /**
     * 랜덤 날짜 생성
     */
    private LocalDateTime generateRandomDate(Random random) {
        LocalDateTime now = LocalDateTime.now();
        int daysBack = random.nextInt(365); // 최근 1년 내
        return now.minusDays(daysBack);
    }

    /**
     * 테스트 요약 출력
     */
    private void printTestSummary(ObservableList<FileInfo> testData) {
        System.out.println("\n📊 테스트 데이터 요약:");
        System.out.println("=".repeat(50));

        // 카테고리별 통계
        var categoryStats = testData.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        f -> f.getDetectedCategory() != null ? f.getDetectedCategory() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("📂 카테고리별 분포:");
        categoryStats.forEach((category, count) ->
                System.out.println("  • " + category + ": " + count + "개"));

        // 상태별 통계
        var statusStats = testData.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FileInfo::getStatus,
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("\n📈 상태별 분포:");
        statusStats.forEach((status, count) ->
                System.out.println("  • " + status + ": " + count + "개"));

        // 총 크기
        long totalSize = testData.stream()
                .mapToLong(FileInfo::getFileSize)
                .sum();

        System.out.println("\n💾 총 크기: " + formatFileSize(totalSize));

        System.out.println("\n🎯 테스트 포인트:");
        System.out.println("  1. 상단 통계 카드들이 올바르게 표시되는지 확인");
        System.out.println("  2. 카테고리 분석 탭의 파이차트/바차트 확인");
        System.out.println("  3. 시간대별 분석 탭의 라인차트 확인");
        System.out.println("  4. 작업 히스토리 탭의 테이블 확인");
        System.out.println("  5. 상세 분석 탭의 트리뷰와 통계 확인");
        System.out.println("  6. 다크테마/라이트테마 전환 확인");
        System.out.println("  7. 반응형 레이아웃 확인 (창 크기 조정)");
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("🧪 Smart File Manager - 통계 시스템 테스트");
        System.out.println("=".repeat(50));
        System.out.println("이 테스트는 통계 시스템을 독립적으로 테스트합니다.");
        System.out.println("실제 파일 없이 가상 데이터로 모든 기능을 확인할 수 있습니다.");
        System.out.println();

        launch(args);
    }
}