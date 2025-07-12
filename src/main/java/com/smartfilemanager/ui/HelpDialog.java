package com.smartfilemanager.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 애플리케이션 도움말을 표시하는 다이얼로그
 */
public class HelpDialog {

    public static void show(Stage ownerStage) {
        Stage helpStage = new Stage();
        helpStage.initModality(Modality.APPLICATION_MODAL);
        helpStage.initOwner(ownerStage);
        helpStage.setTitle("도움말 - Smart File Manager");
        helpStage.setResizable(true);
        helpStage.setMinWidth(600);
        helpStage.setMinHeight(500);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // 제목
        Label titleLabel = new Label("📖 Smart File Manager 도움말");
        titleLabel.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("help-title");

        // 탭 생성
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("help-tab-pane");

        // 1. 시작하기 탭
        Tab gettingStartedTab = createGettingStartedTab();

        // 2. 기능 설명 탭
        Tab featuresTab = createFeaturesTab();

        // 3. 문제 해결 탭
        Tab troubleshootingTab = createTroubleshootingTab();

        // 4. 키보드 단축키 탭
        Tab shortcutsTab = createShortcutsTab();

        tabPane.getTabs().addAll(gettingStartedTab, featuresTab, troubleshootingTab, shortcutsTab);

        // 닫기 버튼
        Button closeButton = new Button("닫기");
        closeButton.setPrefWidth(100);
        closeButton.getStyleClass().add("help-close-button");
        closeButton.setOnAction(e -> helpStage.close());

        content.getChildren().addAll(titleLabel, tabPane, closeButton);

        Scene scene = new Scene(content, 700, 600);
        helpStage.setScene(scene);
        
        // 도움말 창 Scene을 ThemeManager에 등록 (자동으로 현재 테마 적용됨)
        ThemeManager.registerScene(scene);
        
        // 도움말 창이 닫힐 때 Scene 등록 해제
        helpStage.setOnHidden(event -> ThemeManager.unregisterScene(scene));
        
        helpStage.show();
    }

    private static Tab createGettingStartedTab() {
        Tab tab = new Tab("🚀 시작하기");

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        String gettingStartedText = """
        🗂️ Smart File Manager 사용법
        
        📋 1단계: 폴더 선택
        • "폴더 열기" 버튼을 클릭하거나 Ctrl+O를 누르세요
        • 정리하고 싶은 폴더를 선택합니다 (예: Downloads)
        
        🔍 2단계: 파일 스캔
        • 선택한 폴더의 모든 파일을 자동으로 분석합니다
        • 파일 타입, 크기, 생성일 등을 확인합니다
        • 진행률 바에서 스캔 진행 상황을 확인할 수 있습니다
        
        📊 3단계: 분석 결과 확인
        • 테이블에서 분석된 파일 목록을 확인합니다
        • 각 파일의 분류 결과와 상태를 확인합니다
        • 파일을 클릭하면 상세 정보가 표시됩니다
        
        📦 4단계: 파일 정리
        • "파일 정리" 버튼을 클릭합니다
        • 확인 다이얼로그에서 정리 계획을 검토합니다
        • "확인"을 클릭하면 파일이 자동으로 분류됩니다
        
        ✅ 5단계: 결과 확인
        • 정리가 완료되면 통계 정보를 확인합니다
        • 필요시 "되돌리기" 기능으로 원상복구할 수 있습니다
        
        💡 팁: 
        • 처음 사용할 때는 중요하지 않은 테스트 폴더로 연습해보세요
        • 설정에서 분류 규칙을 커스터마이징할 수 있습니다
        • 정리 전에 항상 백업을 생성하는 것을 권장합니다
        """;

        TextArea textArea = new TextArea(gettingStartedText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("help-text-area");

        content.getChildren().add(textArea);
        scrollPane.setContent(content);
        tab.setContent(scrollPane);

        return tab;
    }

    private static Tab createFeaturesTab() {
        Tab tab = new Tab("⚡ 기능 설명");

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        String featuresText = """
        🔧 주요 기능 상세 설명
        
        📁 스마트 파일 분류
        • 확장자 기반 자동 분류 (문서, 이미지, 비디오, 오디오 등)
        • 파일명 패턴 분석 (날짜, 키워드 추출)
        • MIME 타입 정확한 감지
        • 서브카테고리 자동 분류 (스크린샷, 설치파일 등)
        
        🔄 중복 파일 탐지
        • 해시 기반 정확한 중복 파일 찾기
        • 파일 크기 기반 1차 필터링
        • 유사한 파일명 탐지
        • 보관할 파일 자동 추천
        • 중복 제거로 저장공간 절약
        
        🧹 불필요한 파일 정리
        • 임시 파일 자동 감지 (.tmp, .cache, Thumbs.db 등)
        • 빈 파일 및 폴더 탐지
        • 오래된 로그 파일 정리
        • 캐시 파일 정리
        • 안전성 수준별 분류 (안전함/주의/확인필요)
        
        ↩️ 안전한 되돌리기
        • 모든 정리 작업을 되돌릴 수 있습니다
        • 원본 위치로 정확한 복원
        • 파일 손실 방지
        • 실수 방지 기능
        
        📊 상세한 통계
        • 처리된 파일 수 및 크기
        • 카테고리별 분류 현황
        • 절약된 저장공간 계산
        • 작업 히스토리 저장
        
        ⚙️ 사용자 정의 설정
        • 분류 규칙 커스터마이징
        • 자동 정리 옵션
        • 백업 설정
        • UI 테마 변경
        """;

        TextArea textArea = new TextArea(featuresText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("help-text-area");

        content.getChildren().add(textArea);
        scrollPane.setContent(content);
        tab.setContent(scrollPane);

        return tab;
    }

    private static Tab createTroubleshootingTab() {
        Tab tab = new Tab("🔧 문제 해결");

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        String troubleshootingText = """
        🔧 자주 묻는 질문 및 문제 해결
        
        ❓ Q: 파일이 잘못 분류되었어요
        ✅ A: "되돌리기" 기능을 사용해서 원상복구한 후,
           설정에서 분류 규칙을 수정하고 다시 시도하세요.
        
        ❓ Q: 스캔이 너무 오래 걸려요
        ✅ A: 설정에서 "최대 파일 크기" 제한을 줄이거나,
           "내용 분석" 옵션을 비활성화해보세요.
        
        ❓ Q: 일부 파일이 이동하지 않았어요
        ✅ A: 파일이 다른 프로그램에서 사용 중이거나,
           권한이 없을 수 있습니다. 해당 파일을 닫고 다시 시도하세요.
        
        ❓ Q: 중복 파일이 감지되지 않아요
        ✅ A: 설정에서 "중복 파일 탐지" 옵션이 활성화되어 있는지 확인하고,
           파일 크기가 0바이트가 아닌지 확인하세요.
        
        ❓ Q: 정리된 폴더 구조를 변경하고 싶어요
        ✅ A: 설정 > 기본 설정에서 "정리 폴더 경로"와 
           "날짜별 정리" 옵션을 조정할 수 있습니다.
        
        ❓ Q: 설정을 초기화하고 싶어요
        ✅ A: 설정 창에서 "기본값으로 복원" 버튼을 클릭하거나,
           사용자 홈 폴더의 .smartfilemanager 폴더를 삭제하세요.
        
        ❓ Q: 애플리케이션이 느려요
        ✅ A: 1) Java 힙 메모리 증가: -Xmx2g 옵션 추가
           2) 백그라운드 스캔 비활성화
           3) 대용량 파일 제외 설정
        
        ❓ Q: 파일이 삭제되었는데 복구하고 싶어요
        ✅ A: 1) "되돌리기" 기능 사용
           2) 설정에서 백업이 활성화되어 있다면 백업 폴더 확인
           3) 시스템 휴지통 확인
        
        🚨 응급 상황:
        • 모든 파일이 사라진 경우: 당황하지 말고 "되돌리기" 시도
        • 프로그램이 응답하지 않는 경우: 작업 관리자에서 종료 후 재시작
        • 설정이 손상된 경우: .smartfilemanager 폴더 삭제 후 재시작
        
        📞 추가 도움이 필요한 경우:
        • GitHub Issues에 문제를 등록해주세요
        • 오류 메시지와 로그 파일을 함께 첨부해주세요
        • 사용 환경 정보 (OS, Java 버전)를 알려주세요
        """;

        TextArea textArea = new TextArea(troubleshootingText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("help-text-area");

        content.getChildren().add(textArea);
        scrollPane.setContent(content);
        tab.setContent(scrollPane);

        return tab;
    }

    private static Tab createShortcutsTab() {
        Tab tab = new Tab("⌨️ 단축키");

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        String shortcutsText = """
        ⌨️ 키보드 단축키
        
        📁 파일 작업:
        Ctrl + O          폴더 열기
        F5               파일 스캔 (새로고침)
        F6               파일 정리
        Ctrl + Z          정리 되돌리기
        F7               중복 파일 찾기
        F8               불필요한 파일 정리
        
        🔧 설정 및 도움말:
        Ctrl + ,          설정 열기
        F1               이 도움말 열기
        Ctrl + I          정보 (About) 창
        Ctrl + Q          프로그램 종료
        
        📋 테이블 조작:
        ↑↓              파일 선택 이동
        Enter            파일 상세 정보 보기
        Space            파일 선택/해제
        Ctrl + A          모든 파일 선택
        Delete           선택한 파일 삭제 (주의!)
        
        🎨 UI 조작:
        Tab              다음 요소로 이동
        Shift + Tab       이전 요소로 이동
        Enter            버튼 클릭/메뉴 선택
        Esc              다이얼로그 닫기
        
        🔍 고급 검색:
        Ctrl + F          파일 검색 (구현 예정)
        Ctrl + H          히스토리 보기 (구현 예정)
        
        💡 팁:
        • 대부분의 작업은 마우스 없이도 키보드로 가능합니다
        • 메뉴에서 단축키를 확인할 수 있습니다
        • Alt 키를 누르면 메뉴 가속키가 표시됩니다
        """;

        TextArea textArea = new TextArea(shortcutsText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("help-text-area");

        content.getChildren().add(textArea);
        scrollPane.setContent(content);
        tab.setContent(scrollPane);

        return tab;
    }
}