# CSS 스타일시트

이 폴더에는 애플리케이션의 스타일시트 파일들이 포함됩니다.

## 파일 목록

- `styles.css` - 메인 애플리케이션 스타일시트
- `dark-theme.css` - 다크 테마 스타일 (예정)
- `components.css` - 컴포넌트별 세부 스타일 (예정)

## 사용법

```java
scene.getStylesheets().add(
    getClass().getResource("/css/styles.css").toExternalForm()
);
```