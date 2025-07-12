# 🔍 스캔 단계에서 걸러지는 파일 목록

## 현재 `isSafeToScan()` 함수에서 차단되는 파일들

### 🚫 **1. 보호된 시스템 디렉토리에 있는 모든 파일**
```
📁 system32 (어떤 경로든 포함하면 차단)
📁 windows 
📁 program files
📁 program files (x86)
📁 programdata
📁 boot
📁 recovery
📁 $recycle.bin
📁 system volume information
```

**예시:**
- `C:\Windows\System32\notepad.exe` ❌
- `C:\Program Files\Adobe\readme.txt` ❌
- `D:\MyData\windows\myfile.txt` ❌ (경로에 "windows" 포함)

---

### 🚫 **2. 핵심 시스템 파일 확장자**
```
.dll   (Dynamic Link Library)
.sys   (System files)  
.com   (Command files)
.scr   (Screen saver)
```

**예시:**
- `MyProgram.dll` ❌
- `driver.sys` ❌  
- `game.com` ❌
- `screensaver.scr` ❌

---

### 🚫 **3. 특수 시스템 파일명** (대소문자 무관)
```
desktop.ini
thumbs.db
autorun.inf
bootmgr
hiberfil.sys
pagefile.sys
```

**예시:**
- `Desktop.ini` ❌
- `THUMBS.DB` ❌
- `Autorun.inf` ❌

---

### 🚫 **4. 존재하지 않는 파일**
- 파일 경로가 유효하지 않거나 파일이 삭제된 경우

---

## ✅ **스캔은 허용되지만 실제 작업 시 차단되는 파일들**

### 🟡 **실행 파일들** (스캔 ✅, 이동/삭제 ❌)
```
.exe   (실행 파일)
.msi   (설치 파일)  
.jar   (Java 실행 파일)
.bat   (배치 파일)
.cmd   (명령 파일)
```

### 🟡 **잠금/권한 문제** (스캔 ✅, 이동/삭제 시 확인)
- 현재 사용 중인 파일
- 읽기 전용 파일
- 권한이 없는 파일

---

## 📊 **다운로드 폴더에서 흔히 걸러지는 파일 예시**

### ❌ **스캔 단계에서 완전 차단**
```
- Windows Defender 관련 파일 (.sys)
- 브라우저 확장파일 (.dll)  
- 바이러스 백신 파일 (.com)
- 시스템 화면보호기 (.scr)
- desktop.ini, thumbs.db
```

### ✅ **스캔은 되지만 정리 시 보호**
```
- setup.exe (설치 파일)
- installer.msi (설치 패키지)
- application.jar (자바 프로그램)
- script.bat (배치 스크립트)
- autostart.cmd (명령 스크립트)
```

---

## 🎯 **실제 사용자 파일들 (모두 스캔 허용)**
```
✅ 문서: .pdf, .docx, .txt, .xlsx
✅ 이미지: .jpg, .png, .gif, .bmp
✅ 비디오: .mp4, .avi, .mkv, .mov
✅ 음악: .mp3, .wav, .flac, .m4a
✅ 압축: .zip, .rar, .7z, .tar
✅ 데이터: .json, .xml, .csv, .log
✅ 웹: .html, .css, .js (비실행)
✅ 이미지: .iso, .img (디스크 이미지)
✅ 기타: .tmp, .bak, .cache 등
```

---

## 🔧 **현재 필터링이 과도한 경우들**

만약 다운로드 폴더에서 여전히 많이 걸러진다면:

### 1. **경로 문제**
```
❌ D:\Downloads\Program Files\myfile.txt  
   (경로에 "program files" 포함)
   
❌ C:\Users\이름\Downloads\windows_update\file.txt
   (경로에 "windows" 포함)
```

### 2. **시스템 생성 파일**
```
❌ desktop.ini (폴더마다 자동 생성)
❌ thumbs.db (이미지 폴더에 자동 생성)
```

### 3. **보안 소프트웨어 파일**
```
❌ antivirus.dll
❌ firewall.sys  
❌ scanner.com
```

이 목록에서 예상하지 못한 파일이 차단되고 있다면 말씀해주세요! 🔍