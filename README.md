# 🗂️ Smart File Manager / 스마트 파일 관리자

> **Languages**: [English](#english) | [한국어](#한국어)

---

## English

### 😅 My Downloads folder exploded, so I built this

You know that feeling when your Downloads folder becomes a digital wasteland with 2000+ random files? Yeah, me too. That's why I built **Smart File Manager** - an AI-powered desktop tool that automatically organizes your files with just a right-click.

**The frustration was real:**
- "Where did I put that assignment PDF?"
- Downloads folder: `random_file_name (1).jpg`, `Screenshot 2024-07-11 at 3.42.17 AM.png`, `untitled.zip`...
- Manually creating folders and moving files for the 999th time

**The solution is here:**
- Right-click any file → **"📝 Create Rule"** 
- Smart suggestions fill everything automatically
- Never manually organize files again!

### ✨ Key Features

#### 🎯 **User-Defined File Organization Rules**
**The main reason this exists**: Create custom rules and let the computer do the boring work.

**How it works:**
1. **Right-click any file** in the file list
2. Choose **"📝 Create Rule"** or **"✏️ Edit Rule"** (if rule exists)
3. **Smart suggestions** automatically fill in file type and target folder
4. Save and apply - done forever!

**Real examples:**
- Right-click `photo.jpg` → Creates rule: `jpg files → Images folder`
- Right-click `assignment.pdf` → Creates rule: `pdf files → Documents folder`
- Right-click `project.zip` → Creates rule: `zip files → Archives folder`

#### 🤖 **AI-Powered File Analysis**
Because sometimes file extensions lie about what they actually are.
- OpenAI integration for intelligent file content analysis
- Enhanced categorization accuracy beyond just file extensions
- Smart detection for screenshots, wallpapers, documents

#### 📊 **Real-time Statistics & Monitoring**
Know exactly what's cluttering your digital life.
- Live folder monitoring with automatic file organization
- Visual statistics with charts and graphs
- "You have 47 duplicate files wasting 2.3GB" - that kind of insight

#### 🎨 **Modern UI Experience**
Built for humans, not robots.
- Light/Dark theme support (because we all code at 3 AM)
- Intuitive right-click context menus
- Real-time preview and validation
- Actually looks good on your desktop

### 🚀 Getting Started

#### Prerequisites
- Java 17 or later (yeah, it's a JavaFX app)
- Windows/Linux/macOS (tested on Windows mostly)

#### Quick Start

```bash
# Clone this life-saver
git clone https://github.com/balsohn/smart-file-manager.git
cd smart-file-manager

# Build it
./gradlew build

# Run it and watch the magic happen
./gradlew run
```

#### Available Commands

```bash
# Main application
./gradlew run

# Demos to see what it can do
./gradlew statisticsTest      # See file distribution charts
./gradlew aiDemo              # AI analysis demo  
./gradlew monitoringTest      # Real-time monitoring demo
./gradlew runAllDemos         # Run everything

# Utility commands
./gradlew verifyStatisticsFiles
./gradlew createTestFile
./gradlew helpStatistics
```

### 📖 How to Use

#### 1. **Scan Files**
- Click **"📁 Scan Folder"** to select your chaotic folder
- Watch as it discovers all your forgotten files

#### 2. **Create Organization Rules** (The good stuff)
- **Method 1**: Right-click any file → **"📝 Create Rule"** (recommended)
- **Method 2**: Go to Settings → **"📝 Organization Rules"** tab (for power users)

#### 3. **Organize Files**  
- Select files you want to organize (or select all for maximum satisfaction)
- Click **"🗂️ Organize Files"**
- Preview where everything will go
- Confirm and watch your folder become beautiful

#### 4. **Advanced Features**
- **AI Analysis**: Enable in Settings → Performance for smarter categorization
- **Real-time Monitoring**: Auto-organize new files as they arrive
- **Statistics**: See exactly how much digital hoarding you've been doing

### 🎯 Use Cases

#### **For Fellow Developers**
```
Your Downloads folder probably looks like:
• random-project.zip → "Code/Archives" folder
• some-library-v2.1.jar → "Code/Libraries" folder  
• definitely-not-homework.java → "Code/Personal" folder
```

#### **For Students**
```
Stop losing your assignments:
• final-assignment-FINAL-v3.pdf → "School/Assignments" folder
• study-notes.docx → "School/Notes" folder
• presentation.pptx → "School/Presentations" folder
```

#### **For Content Creators**
```
Organize your creative chaos:
• thumbnail-idea.png → "Content/Thumbnails" folder
• video-project.mp4 → "Content/Videos" folder
• background-music.mp3 → "Content/Audio" folder
```

### 🏗️ Technical Architecture

#### **Recent Refactoring (2024.07.11)**
- **MainController**: 1,015 lines → 359 lines (**65% reduction**)
- **11 new specialized classes** because monolithic code is evil

#### **Core Components**

##### **Rule Management System**
- `FileRule`: Rule model with validation (so you can't break things)
- `CustomRulesManager`: JSON-based rule persistence
- `RuleDialogController`: The UI that makes rule creation actually pleasant

##### **Manager Classes** (Because separation of concerns matters)
- `UIUpdateManager`: Keeps the UI responsive
- `TableConfigManager`: Makes file tables not suck  
- `FileOperationHandler`: Handles the actual file moving
- `DialogManager`: All the popup dialogs you need

##### **Utility Classes**
- `FileFormatUtils`: Makes file sizes readable (2.3GB instead of 2,474,836,992 bytes)
- `FileIconUtils`: Pretty icons and colors
- `TaskUtils`: Background tasks that don't freeze your UI

### 🔧 Configuration

#### **Settings Overview**
- **📁 Basic Settings**: Where to scan, where to organize
- **🔍 Duplicate Detection**: Find those 47 copies of the same meme
- **⚡ Performance**: File size limits, AI analysis settings
- **🎨 UI Settings**: Light/dark themes, system tray
- **📝 Organization Rules**: The heart of the whole thing

#### **AI Integration** (Optional but recommended)
1. Get OpenAI API key from [OpenAI Platform](https://platform.openai.com)
2. Go to Settings → Performance → Enable AI Analysis
3. Paste your API key
4. Watch as it gets smarter at organizing your files

### 🛠️ Development

#### **Project Structure**
```
src/main/java/com/smartfilemanager/
├── controller/          # UI controllers
├── model/              # Data models  
├── service/            # Business logic
├── manager/            # Specialized managers (the refactoring heroes)
├── util/               # Utility classes
├── ui/                 # UI components
└── constants/          # Constants (no more magic numbers)
```

#### **Key Design Principles**
- **Single Responsibility**: Each class does one thing well
- **Don't Repeat Yourself**: Utilities are actually reusable
- **Fail Fast**: Validation everywhere to catch problems early
- **User-Friendly**: If it's confusing, it's a bug

### 🤝 Contributing

Found a bug? Have an idea? Want to make Downloads folders everywhere less chaotic?

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/even-better-organization`)
3. Make your changes
4. Test thoroughly (please)
5. Create a Pull Request

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### 🎉 Acknowledgments

- **JavaFX**: For making desktop apps that don't look like they're from 1995
- **OpenAI API**: For understanding what files actually contain
- **Every developer** who has ever lost a file in their Downloads folder
- **Coffee**: The real MVP behind this project

---

## 한국어

### 🤷‍♂️ Downloads 폴더 터져서 개발한 프로그램

Downloads 폴더에 파일 2000개 쌓여있는 거 보고 빡쳐서 만든 프로그램입니다. AI 기반으로 파일을 자동 정리해주는 **스마트 파일 관리자**입니다. 우클릭 한 번이면 끝!

**이런 경험 있으시죠?**
- "분명히 다운받았는데 이 파일 어디 있지?"
- Downloads 폴더: `무제 (1).jpg`, `스크린샷 2024-07-11 오전 3시 42분 17초.png`, `파일.zip`...
- 999번째 폴더 만들고 수동으로 파일 옮기기

**이제 그만하세요:**
- 파일 우클릭 → **"📝 새 규칙 만들기"**
- 알아서 다 채워줌
- 평생 자동 정리!

### ✨ 주요 기능

#### 🎯 **사용자 정의 파일 정리 규칙**
**이 프로그램을 만든 핵심 이유**: 규칙 한 번 만들면 컴퓨터가 알아서 정리해줍니다.

**사용법:**
1. **파일 목록에서 아무 파일이나 우클릭**
2. **"📝 새 규칙 만들기"** 또는 **"✏️ 규칙 수정하기"** 선택
3. **똑똑한 제안** 기능이 파일 형식과 타겟 폴더를 자동으로 채워줌
4. 저장하면 끝 - 평생 써먹기!

**실제 예시:**
- `사진.jpg` 우클릭 → `jpg 파일 → Images 폴더` 규칙 생성
- `과제.pdf` 우클릭 → `pdf 파일 → Documents 폴더` 규칙 생성  
- `프로젝트.zip` 우클릭 → `zip 파일 → Archives 폴더` 규칙 생성

#### 🤖 **AI 기반 파일 분석**
가끔 파일 확장자가 거짓말할 때가 있거든요.
- OpenAI 연동으로 파일 내용까지 똑똑하게 분석
- 확장자만으로는 알 수 없는 정확한 분류
- 스크린샷, 배경화면, 문서 자동 구분

#### 📊 **실시간 통계 및 모니터링**
당신의 디지털 쓰레기통 상태를 정확히 파악하세요.
- 실시간 폴더 감시로 새 파일 자동 정리
- 차트와 그래프로 보는 시각적 통계
- "중복 파일 47개가 2.3GB 낭비하고 있습니다" 같은 충격적 진실

#### 🎨 **현대적인 UI 경험**
사람이 쓰라고 만든 프로그램입니다.
- 라이트/다크 테마 지원 (새벽 3시 코딩러를 위해)
- 직관적인 우클릭 메뉴
- 실시간 미리보기 및 검증
- 데스크톱에 두기 부끄럽지 않은 디자인

### 🚀 시작하기

#### 필요한 것들
- Java 17 이상 (JavaFX 앱입니다)
- Windows/Linux/macOS (주로 Windows에서 테스트됨)

#### 빠른 시작

```bash
# 인생을 바꿀 프로그램 다운로드
git clone https://github.com/balsohn/smart-file-manager.git
cd smart-file-manager

# 빌드하기
./gradlew build

# 실행하고 마법 구경하기
./gradlew run
```

#### 사용 가능한 명령어들

```bash
# 메인 애플리케이션
./gradlew run

# 뭘 할 수 있는지 보는 데모들
./gradlew statisticsTest      # 파일 분포 차트 보기
./gradlew aiDemo              # AI 분석 데모  
./gradlew monitoringTest      # 실시간 모니터링 데모
./gradlew runAllDemos         # 다 돌려보기

# 유틸리티 명령어들
./gradlew verifyStatisticsFiles
./gradlew createTestFile
./gradlew helpStatistics
```

### 📖 사용법

#### 1. **파일 스캔**
- **"📁 폴더 스캔"** 클릭해서 정리하고 싶은 폴더 선택
- 숨어있던 파일들을 다 찾아줍니다

#### 2. **정리 규칙 만들기** (핵심 기능)
- **방법 1**: 파일 우클릭 → **"📝 새 규칙 만들기"** (추천)
- **방법 2**: 설정 → **"📝 정리 규칙"** 탭 (고급 사용자용)

#### 3. **파일 정리하기**  
- 정리하고 싶은 파일 선택 (또는 전체 선택해서 쾌감 극대화)
- **"🗂️ 파일 정리"** 클릭
- 어디로 갈지 미리보기
- 확인하면 폴더가 아름다워집니다

#### 4. **고급 기능들**
- **AI 분석**: 설정 → 성능에서 활성화하면 더 똑똑하게 분류
- **실시간 모니터링**: 새 파일이 들어오면 자동으로 정리
- **통계**: 당신이 얼마나 디지털 쓰레기를 모았는지 정확히 보여줌

### 🎯 사용 사례

#### **개발자분들을 위해**
```
Downloads 폴더 아마 이럴 겁니다:
• 랜덤프로젝트.zip → "Code/Archives" 폴더
• 어떤라이브러리-v2.1.jar → "Code/Libraries" 폴더  
• 절대과제아님.java → "Code/Personal" 폴더
```

#### **학생분들을 위해**
```
과제 잃어버리지 마세요:
• 최종과제-진짜최종-v3.pdf → "학교/과제" 폴더
• 공부노트.docx → "학교/노트" 폴더
• 발표자료.pptx → "학교/발표" 폴더
```

#### **콘텐츠 제작자분들을 위해**
```
창작의 혼돈을 정리하세요:
• 썸네일아이디어.png → "콘텐츠/썸네일" 폴더
• 영상프로젝트.mp4 → "콘텐츠/동영상" 폴더
• 배경음악.mp3 → "콘텐츠/오디오" 폴더
```

### 🏗️ 기술적 구조

#### **최근 리팩토링 (2024.07.11)**
- **MainController**: 1,015줄 → 359줄 (**65% 감소**)
- **11개의 새로운 전문 클래스** 생성 (모놀리식 코드는 악의 축)

#### **핵심 컴포넌트들**

##### **규칙 관리 시스템**
- `FileRule`: 검증 기능이 있는 규칙 모델 (망가뜨릴 수 없게)
- `CustomRulesManager`: JSON 기반 규칙 저장
- `RuleDialogController`: 규칙 만들기를 즐겁게 해주는 UI

##### **매니저 클래스들** (관심사 분리가 중요하니까)
- `UIUpdateManager`: UI가 반응성 있게 유지
- `TableConfigManager`: 파일 테이블이 별로 안 거슬리게  
- `FileOperationHandler`: 실제 파일 이동 처리
- `DialogManager`: 필요한 모든 팝업 다이얼로그

##### **유틸리티 클래스들**
- `FileFormatUtils`: 파일 크기를 읽기 쉽게 (2,474,836,992바이트 대신 2.3GB)
- `FileIconUtils`: 예쁜 아이콘과 색상
- `TaskUtils`: UI 얼지 않게 하는 백그라운드 작업

### 🔧 설정

#### **설정 개요**
- **📁 기본 설정**: 어디서 스캔하고 어디로 정리할지
- **🔍 중복 파일 탐지**: 똑같은 밈 47개 찾기
- **⚡ 성능**: 파일 크기 제한, AI 분석 설정
- **🎨 UI 설정**: 라이트/다크 테마, 시스템 트레이
- **📝 정리 규칙**: 모든 것의 핵심

#### **AI 연동** (선택사항이지만 추천)
1. [OpenAI 플랫폼](https://platform.openai.com)에서 API 키 받기
2. 설정 → 성능 → AI 분석 활성화
3. API 키 붙여넣기
4. 파일 정리가 더 똑똑해지는 걸 구경하기

### 🛠️ 개발

#### **프로젝트 구조**
```
src/main/java/com/smartfilemanager/
├── controller/          # UI 컨트롤러들
├── model/              # 데이터 모델들  
├── service/            # 비즈니스 로직
├── manager/            # 전문 매니저들 (리팩토링의 영웅들)
├── util/               # 유틸리티 클래스들
├── ui/                 # UI 컴포넌트들
└── constants/          # 상수들 (매직 넘버는 이제 그만)
```

#### **핵심 설계 원칙들**
- **단일 책임**: 각 클래스는 한 가지 일만 잘하기
- **중복 제거**: 유틸리티는 진짜로 재사용 가능하게
- **빨리 실패**: 문제를 일찍 잡기 위한 검증 everywhere
- **사용자 친화**: 헷갈리면 그건 버그

### 🤝 기여하기

버그 찾았나요? 아이디어 있나요? 전 세계 Downloads 폴더를 덜 혼란스럽게 만들고 싶나요?

1. 이 저장소 포크하기
2. 기능 브랜치 만들기 (`git checkout -b feature/더욱-완벽한-정리`)
3. 변경사항 만들기
4. 꼼꼼히 테스트하기 (제발)
5. Pull Request 생성하기

### 📄 라이센스

이 프로젝트는 MIT 라이센스 하에 있습니다 - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

### 🎉 감사의 말

- **JavaFX**: 1995년스럽지 않은 데스크톱 앱을 만들 수 있게 해줘서
- **OpenAI API**: 파일이 실제로 뭘 담고 있는지 이해해줘서
- **Downloads 폴더에서 파일 잃어본 모든 개발자들**: 여러분이 있어서 이 프로그램이 나왔습니다
- **커피**: 이 프로젝트의 진짜 MVP

---

**Made with ❤️ and too much caffeine for better file organization**
