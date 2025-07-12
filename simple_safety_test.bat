@echo off
chcp 65001 > nul
title Smart File Manager Safety Test
echo.
echo ========================================
echo   Smart File Manager Safety Test
echo ========================================
echo.

REM Test directory setup
set TEST_ROOT=%USERPROFILE%\Desktop\SafetyTest
echo Test Directory: %TEST_ROOT%
echo.

echo [1/4] Creating test environment...
mkdir "%TEST_ROOT%" 2>nul
mkdir "%TEST_ROOT%\SystemFiles" 2>nul
mkdir "%TEST_ROOT%\SafeFiles" 2>nul

echo [2/4] Creating protected files (should be filtered)...
echo Test executable file > "%TEST_ROOT%\SystemFiles\test_app.exe"
echo Test library file > "%TEST_ROOT%\SystemFiles\test_lib.dll"
echo Test system file > "%TEST_ROOT%\SystemFiles\system_file.sys"

echo [3/4] Creating safe files (should be scanned)...
echo Test document > "%TEST_ROOT%\SafeFiles\document.txt"
echo Test image > "%TEST_ROOT%\SafeFiles\photo.jpg"
echo Test video > "%TEST_ROOT%\SafeFiles\video.mp4"

echo [4/4] Test environment ready!
echo.
echo ========================================
echo   Test Instructions
echo ========================================
echo.
echo 1. Open Smart File Manager
echo 2. Scan these folders:
echo    - %TEST_ROOT%\SystemFiles
echo    - %TEST_ROOT%\SafeFiles
echo.
echo Expected Results:
echo - SystemFiles: Should show protection messages
echo - SafeFiles: Should scan all files normally
echo.
echo Press any key to open test folder...
pause > nul
explorer "%TEST_ROOT%"

echo.
echo Test completed! Check the scan results.
echo.
pause