@echo off
echo ===================================
echo Smart File Manager 안전성 테스트 환경 구성
echo ===================================
echo.

REM 테스트 루트 디렉토리 생성
set TEST_ROOT="%USERPROFILE%\Desktop\SafetyTest_SmartFileManager"
echo 테스트 디렉토리 생성: %TEST_ROOT%
mkdir %TEST_ROOT% 2>nul

REM 1. 시스템 파일 보호 테스트용 파일 생성
echo.
echo [1/6] 시스템 파일 보호 테스트 환경 구성...
mkdir "%TEST_ROOT%\SystemProtectionTest" 2>nul

REM 가짜 시스템 파일들 생성 (실제 시스템 파일이 아닌 테스트용)
echo 테스트용 가짜 시스템 파일입니다 > "%TEST_ROOT%\SystemProtectionTest\fake_notepad.exe"
echo 테스트용 가짜 DLL 파일입니다 > "%TEST_ROOT%\SystemProtectionTest\fake_kernel32.dll"
echo 테스트용 배치 파일입니다 > "%TEST_ROOT%\SystemProtectionTest\test_script.bat"
echo 테스트용 시스템 파일입니다 > "%TEST_ROOT%\SystemProtectionTest\desktop.ini"

REM 2. 일반 파일들 (정리되어야 할 파일들)
mkdir "%TEST_ROOT%\NormalFiles" 2>nul
echo 테스트 텍스트 파일입니다 > "%TEST_ROOT%\NormalFiles\document1.txt"
echo 테스트 문서 파일입니다 > "%TEST_ROOT%\NormalFiles\report.docx"
echo 테스트 이미지 설명입니다 > "%TEST_ROOT%\NormalFiles\photo.jpg"
echo 테스트 비디오 설명입니다 > "%TEST_ROOT%\NormalFiles\video.mp4"

REM 3. 백업 테스트용 대용량 파일 생성
echo.
echo [2/6] 백업 테스트용 대용량 파일 생성...
mkdir "%TEST_ROOT%\BackupTest" 2>nul

REM 100MB 파일 생성 (fsutil 사용)
fsutil file createnew "%TEST_ROOT%\BackupTest\large_file_100MB.txt" 104857600 2>nul
if %errorlevel% equ 0 (
    echo ✓ 100MB 테스트 파일 생성 완료
) else (
    echo ✗ 100MB 파일 생성 실패 - 관리자 권한이 필요할 수 있습니다
    echo   대신 작은 파일로 테스트를 진행하세요
    echo 이것은 백업 테스트용 파일입니다 > "%TEST_ROOT%\BackupTest\backup_test_file.txt"
)

REM 4. 메모리 테스트용 다량의 파일 생성
echo.
echo [3/6] 메모리 테스트용 다량 파일 생성...
mkdir "%TEST_ROOT%\MemoryTest" 2>nul

echo 1000개의 테스트 파일 생성 중...
for /l %%i in (1,1,1000) do (
    echo 테스트 파일 %%i > "%TEST_ROOT%\MemoryTest\test_file_%%i.txt"
    if %%i equ 100 echo ✓ 100개 완료...
    if %%i equ 500 echo ✓ 500개 완료...
)
echo ✓ 1000개 테스트 파일 생성 완료

REM 5. 파일 잠금 테스트용 - 읽기 전용 파일
echo.
echo [4/6] 파일 잠금 테스트 환경 구성...
mkdir "%TEST_ROOT%\FileLockTest" 2>nul
echo 읽기 전용 테스트 파일입니다 > "%TEST_ROOT%\FileLockTest\readonly_file.txt"
attrib +R "%TEST_ROOT%\FileLockTest\readonly_file.txt"
echo ✓ 읽기 전용 파일 생성 완료

REM 6. 혼합 테스트 환경 - 실제 사용 시나리오
echo.
echo [5/6] 실제 사용 시나리오 테스트 환경 구성...
mkdir "%TEST_ROOT%\RealScenarioTest" 2>nul
mkdir "%TEST_ROOT%\RealScenarioTest\Downloads" 2>nul
mkdir "%TEST_ROOT%\RealScenarioTest\Documents" 2>nul
mkdir "%TEST_ROOT%\RealScenarioTest\Pictures" 2>nul

REM Downloads 폴더에 다양한 파일들
echo 다운로드된 문서입니다 > "%TEST_ROOT%\RealScenarioTest\Downloads\downloaded_doc.pdf"
echo 다운로드된 이미지입니다 > "%TEST_ROOT%\RealScenarioTest\Downloads\image1.png"
echo 다운로드된 압축파일입니다 > "%TEST_ROOT%\RealScenarioTest\Downloads\archive.zip"
echo 임시 파일입니다 > "%TEST_ROOT%\RealScenarioTest\Downloads\temp_file.tmp"

REM 사용자 생성 파일들
echo 개인 문서입니다 > "%TEST_ROOT%\RealScenarioTest\my_document.docx"
echo 개인 사진입니다 > "%TEST_ROOT%\RealScenarioTest\vacation_photo.jpg"
echo 개인 동영상입니다 > "%TEST_ROOT%\RealScenarioTest\home_video.mp4"

REM 7. 테스트 결과 확인용 스크립트 생성
echo.
echo [6/6] 테스트 도구 생성...

echo @echo off > "%TEST_ROOT%\check_backup_folder.bat"
echo echo 백업 폴더 확인: >> "%TEST_ROOT%\check_backup_folder.bat"
echo dir "%%USERPROFILE%%\.smartfilemanager\backups" /b 2^>nul ^|^| echo 백업 폴더가 없습니다 >> "%TEST_ROOT%\check_backup_folder.bat"
echo pause >> "%TEST_ROOT%\check_backup_folder.bat"

echo @echo off > "%TEST_ROOT%\check_database.bat"
echo echo 데이터베이스 파일 확인: >> "%TEST_ROOT%\check_database.bat"
echo dir "%%USERPROFILE%%\.smartfilemanager\*.db" 2^>nul ^|^| echo 데이터베이스 파일이 없습니다 >> "%TEST_ROOT%\check_database.bat"
echo pause >> "%TEST_ROOT%\check_database.bat"

REM 테스트 가이드 파일 복사
copy "SAFETY_TEST_GUIDE.md" "%TEST_ROOT%\테스트_가이드.md" 2>nul

echo.
echo ===================================
echo 🎉 테스트 환경 구성 완료!
echo ===================================
echo.
echo 📁 테스트 폴더: %TEST_ROOT%
echo.
echo 📋 구성된 테스트 환경:
echo   └─ SystemProtectionTest    : 시스템 파일 보호 테스트
echo   └─ NormalFiles            : 일반 파일 정리 테스트  
echo   └─ BackupTest             : 백업 기능 테스트
echo   └─ MemoryTest             : 메모리 관리 테스트 (1000개 파일)
echo   └─ FileLockTest           : 파일 잠금 테스트
echo   └─ RealScenarioTest       : 실제 사용 시나리오 테스트
echo   └─ 테스트_가이드.md        : 상세한 테스트 방법
echo.
echo 🚀 이제 Smart File Manager를 실행하고 테스트를 시작하세요!
echo.
echo ⚠️  주의사항:
echo    - 항상 테스트 파일들로만 진행하세요
echo    - 중요한 데이터는 별도 백업하세요
echo    - 테스트 후 결과를 확인하세요
echo.
pause

REM 탐색기에서 테스트 폴더 열기
explorer %TEST_ROOT%