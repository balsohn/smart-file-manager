# CSS 스타일시트

이 폴더에는 Smart File Manager 애플리케이션의 스타일시트 파일들이 포함됩니다.

## 파일 목록

- `styles.css` - 메인 애플리케이션 스타일시트 (1000+ 줄의 종합 스타일)
- `dark-theme.css` - 다크 테마 전용 스타일시트 (완전 구현됨)
- `statistics-styles.css` - 통계 화면 전용 스타일시트 (차트 및 분석 UI)

## 주요 스타일 클래스

### 버튼 스타일
- `.scan-button`, `.organize-button`, `.settings-button` - 주요 액션 버튼
- `.ai-button`, `.monitoring-button` - 보조 기능 버튼
- `.primary-button`, `.secondary-button` - 일반 버튼
- `.button.active` - 활성 상태 버튼

### 상태 표시
- `.status-ready`, `.status-processing`, `.status-warning`, `.status-error` - 기본 상태
- `.status-active`, `.status-inactive`, `.status-success` - 확장 상태

### 폼 컨트롤
- `.text-field`, `.text-field-error` - 텍스트 입력 필드
- `.combo-box`, `.combo-box-error` - 콤보박스
- `.check-box`, `.spinner` - 체크박스 및 스피너

### 유틸리티 클래스
- `.fade-in-animation`, `.bounce-animation`, `.pulse-animation` - 애니메이션
- `.disabled-state`, `.loading-state`, `.hover-lift` - 상태 효과
- `.padding-small/medium/large`, `.margin-top-small/medium/large` - 간격 조정
- `.width-full/half/quarter`, `.height-small/medium/large` - 크기 조정

## 테마 지원

### 라이트 테마 (기본)
`styles.css`에 정의된 기본 테마입니다.

### 다크 테마
`dark-theme.css`를 추가로 로드하여 다크 테마를 활성화합니다.

## 사용법

```java
// 기본 스타일 로드
scene.getStylesheets().add(
    getClass().getResource("/css/styles.css").toExternalForm()
);

// 다크 테마 적용
scene.getStylesheets().add(
    getClass().getResource("/css/dark-theme.css").toExternalForm()
);

// 통계 화면 스타일 (통계 뷰에서만)
scene.getStylesheets().add(
    getClass().getResource("/css/statistics-styles.css").toExternalForm()
);
```

## 개발 가이드

### 새 스타일 추가 시 주의사항
1. 기존 스타일 클래스와의 일관성 유지
2. 다크 테마 지원을 위한 대응 스타일 추가
3. 반응형 디자인 고려 (작은 화면 대응)
4. 애니메이션 및 전환 효과 적절히 활용

### 색상 팔레트
- Primary: #007bff (파란색)
- Success: #28a745 (초록색)
- Warning: #ffc107 (노란색)
- Danger: #dc3545 (빨간색)
- Info: #17a2b8 (청록색)
- Light: #f8f9fa (연한 회색)
- Dark: #343a40 (진한 회색)