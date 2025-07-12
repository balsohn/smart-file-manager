@echo off
title Smart File Manager 빠른 안전성 테스트
color 0A

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║                🔒 빠른 안전성 테스트                          ║
echo  ║                Smart File Manager                            ║
echo  ╚══════════════════════════════════════════════════════════════╝
echo.

REM 테스트 루트 설정
set TEST_ROOT=%USERPROFILE%\Desktop\QuickSafetyTest
set LOG_FILE=%TEST_ROOT%\test_log.txt

echo 📂 테스트 디렉토리: %TEST_ROOT%
echo 📝 로그 파일: %LOG_FILE%
echo.

REM 로그 파일 초기화
echo ===== Quick Safety Test Log ===== > "%LOG_FILE%"
echo 테스트 시작 시간: %date% %time% >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

REM 1. 테스트 환경 구성
echo [1/5] 🔧 테스트 환경 구성 중...
mkdir "%TEST_ROOT%" 2>nul
mkdir "%TEST_ROOT%\SystemFiles" 2>nul
mkdir "%TEST_ROOT%\SafeFiles" 2>nul
mkdir "%TEST_ROOT%\TestResults" 2>nul

echo 테스트 환경 구성 완료 >> "%LOG_FILE%"

REM 2. 보호되어야 할 파일들 생성
echo [2/5] 🛡️ 보호 대상 파일 생성 중...
echo 테스트용 실행파일 > "%TEST_ROOT%\SystemFiles\test_app.exe"
echo 테스트용 라이브러리 > "%TEST_ROOT%\SystemFiles\test_lib.dll"
echo 테스트용 시스템 파일 > "%TEST_ROOT%\SystemFiles\system_config.sys"
echo 테스트용 배치 파일 > "%TEST_ROOT%\SystemFiles\autorun.bat"

echo 보호 대상 파일 생성: >> "%LOG_FILE%"
echo   - test_app.exe >> "%LOG_FILE%"
echo   - test_lib.dll >> "%LOG_FILE%"
echo   - system_config.sys >> "%LOG_FILE%"
echo   - autorun.bat >> "%LOG_FILE%"

REM 3. 안전한 파일들 생성
echo [3/5] ✅ 안전한 파일 생성 중...
echo 일반 텍스트 문서입니다 > "%TEST_ROOT%\SafeFiles\document.txt"
echo 일반 이미지 파일입니다 > "%TEST_ROOT%\SafeFiles\photo.jpg"
echo 일반 비디오 파일입니다 > "%TEST_ROOT%\SafeFiles\video.mp4"
echo 일반 문서 파일입니다 > "%TEST_ROOT%\SafeFiles\report.docx"

echo 안전한 파일 생성: >> "%LOG_FILE%"
echo   - document.txt >> "%LOG_FILE%"
echo   - photo.jpg >> "%LOG_FILE%"
echo   - video.mp4 >> "%LOG_FILE%"
echo   - report.docx >> "%LOG_FILE%"

REM 4. 읽기 전용 파일 테스트
echo [4/5] 🔒 파일 잠금 테스트 파일 생성 중...
echo 읽기 전용 테스트 파일 > "%TEST_ROOT%\SafeFiles\locked_file.txt"
attrib +R "%TEST_ROOT%\SafeFiles\locked_file.txt"

echo 읽기 전용 파일 생성: locked_file.txt >> "%LOG_FILE%"

REM 5. 테스트 완료 안내
echo [5/5] ✨ 테스트 환경 준비 완료!
echo.

echo 테스트 환경 준비 완료 >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

REM 현재 상태 출력
echo ┌─────────────────────────────────────────────────────────────┐
echo │                     📋 테스트 시나리오                      │
echo ├─────────────────────────────────────────────────────────────┤
echo │                                                             │
echo │  🎯 목표: Smart File Manager의 안전성 기능 검증            │
echo │                                                             │
echo │  📁 테스트 대상 폴더들:                                    │
echo │     • %TEST_ROOT%\SystemFiles                      │
echo │       (보호되어야 함: .exe, .dll, .sys, .bat)             │
echo │                                                             │
echo │     • %TEST_ROOT%\SafeFiles                        │
echo │       (정리되어야 함: .txt, .jpg, .mp4, .docx)            │
echo │                                                             │
echo │  🔍 확인해야 할 사항:                                      │
echo │     1. 시스템 파일들이 자동으로 제외되는가?                │
echo │     2. 읽기 전용 파일이 보호되는가?                        │
echo │     3. 안전한 파일들만 정리되는가?                         │
echo │     4. 오류 발생 시 안전하게 중단되는가?                   │
echo │                                                             │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 다음 단계:
echo.
echo   1️⃣  Smart File Manager 실행
echo   2️⃣  스캔 폴더로 다음 경로들을 개별 테스트:
echo        📂 %TEST_ROOT%\SystemFiles
echo        📂 %TEST_ROOT%\SafeFiles  
echo   3️⃣  각 폴더에서 파일 정리 실행
echo   4️⃣  결과 확인 및 로그 분석
echo.

echo ⚠️  예상되는 안전한 동작:
echo     • SystemFiles 폴더: 모든 파일이 보호되어 정리되지 않음
echo     • SafeFiles 폴더: 읽기 전용 파일 제외하고 안전하게 정리됨
echo     • 콘솔에 [SAFETY] 메시지들이 출력됨
echo.

echo 🏁 테스트를 시작하려면 아무 키나 누르세요...
pause >nul

REM 테스트 완료 후 결과 확인용 스크립트 생성
echo @echo off > "%TEST_ROOT%\check_results.bat"
echo echo ============================================ >> "%TEST_ROOT%\check_results.bat"
echo echo 테스트 결과 확인 >> "%TEST_ROOT%\check_results.bat"
echo echo ============================================ >> "%TEST_ROOT%\check_results.bat"
echo echo. >> "%TEST_ROOT%\check_results.bat"
echo echo [SystemFiles 폴더 확인] >> "%TEST_ROOT%\check_results.bat"
echo echo 다음 파일들이 여전히 존재해야 합니다: >> "%TEST_ROOT%\check_results.bat"
echo dir "%TEST_ROOT%\SystemFiles" /b >> "%TEST_ROOT%\check_results.bat"
echo echo. >> "%TEST_ROOT%\check_results.bat"
echo echo [SafeFiles 폴더 확인] >> "%TEST_ROOT%\check_results.bat"
echo echo 읽기 전용 파일만 남아있어야 합니다: >> "%TEST_ROOT%\check_results.bat"
echo dir "%TEST_ROOT%\SafeFiles" /b >> "%TEST_ROOT%\check_results.bat"
echo echo. >> "%TEST_ROOT%\check_results.bat"
echo echo [백업 폴더 확인] >> "%TEST_ROOT%\check_results.bat"
echo dir "%%USERPROFILE%%\.smartfilemanager\backups" /b 2^>nul ^|^| echo 백업 없음 >> "%TEST_ROOT%\check_results.bat"
echo pause >> "%TEST_ROOT%\check_results.bat"

echo.
echo 🚀 Smart File Manager를 실행하여 테스트를 시작하세요!
echo.
echo 📊 테스트 완료 후 다음 파일을 실행하여 결과를 확인하세요:
echo    👉 %TEST_ROOT%\check_results.bat
echo.

REM 탐색기로 테스트 폴더 열기
explorer "%TEST_ROOT%"

echo 테스트 폴더가 탐색기에서 열렸습니다.
echo.
pause