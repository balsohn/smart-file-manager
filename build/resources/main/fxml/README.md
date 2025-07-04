# FXML 레이아웃 파일

이 폴더에는 JavaFX FXML 레이아웃 파일들이 포함됩니다.

## 파일 목록

- `main.fxml` - 메인 화면 레이아웃 (예정)
- `settings.fxml` - 설정 화면 레이아웃 (예정)
- `about.fxml` - 정보 화면 레이아웃 (예정)

## 사용법

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
Parent root = loader.load();
```