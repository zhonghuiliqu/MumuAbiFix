@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

REM ===========================================================================
REM  MuMu Princess Connect ABI fix - targeted edit (adb, no APK build)
REM  Only changes com.bilibili.priconne (and its .yofun.mumu variant) ABI to x86_64.
REM  Taiwan tw.sonet.princessconnect and all other entries are left untouched.
REM  Usage:  adb-fix.bat            backup and edit
REM          adb-fix.bat restore    restore from .bak
REM          adb-fix.bat status     show target dir + priconne entry
REM ===========================================================================

set "MUMU_HOST=127.0.0.1"
set "MUMU_PORT=16384"

set "ADB=adb"
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

set "TARGET_DIR=/data/system/etc/mumu-configs"
set "F1=abi-select-android12.config"
set "F2=abi-select-v2.config"
set "EDITOR=%~dp0fix_edit.ps1"

where adb >nul 2>nul
if errorlevel 1 if not exist "%ADB%" (
  echo [ERROR] adb.exe not found. Install Android platform-tools / set ADB full path.
  pause
  exit /b 1
)

echo ============================================================
echo   MuMu ABI fix (targeted)   adb: %MUMU_HOST%:%MUMU_PORT%
echo ============================================================
echo [1/3] connect MuMu...
%ADB% connect %MUMU_HOST%:%MUMU_PORT%
%ADB% devices

set "MODE=apply"
if /i "%~1"=="restore" set "MODE=restore"
if /i "%~1"=="status"   set "MODE=status"

echo [2/3] check root (su)...
%ADB% shell "su -c 'id'" | findstr /C:"uid=0" >nul 2>nul
if errorlevel 1 (
  echo   - no root (uid=0). Enable/authorize root in MuMu and retry.
  pause
  exit /b 1
)
echo   - root OK (uid=0)

if /i "%MODE%"=="status"  goto :status
if /i "%MODE%"=="restore" goto :restore

:apply
echo [3/3] backup + targeted edit (only Princess Connect -> x86_64)...
for %%F in (%F1% %F2%) do (
  set "TF=%TARGET_DIR%/%%F"
  set "TB=!TF!.bak"
  set "TMP=%TEMP%\mumu_%%F"
  %ADB% shell "su -c 'if [ ! -f !TB! ]; then cp !TF! !TB!; fi'" >nul
  %ADB% shell "su -c 'cat !TF!'" > "!TMP!"
  for /f %%C in ('powershell -NoProfile -ExecutionPolicy Bypass -File "%EDITOR%" "!TMP!"') do set "CH=%%C"
  echo   -- %%F : !CH!
  if not "!CH!"=="CHANGED=0" (
    %ADB% push "!TMP!" /data/local/tmp/%%F >nul
    %ADB% shell "su -c 'cp /data/local/tmp/%%F !TF! && chmod 644 !TF! && echo WRITE_OK'"
  ) else (
    echo       (no Princess Connect entry found, target unchanged)
  )
)
echo.
echo Done. Restart MuMu emulator to apply.
pause
exit /b 0

:restore
echo [3/3] restore from .bak...
for %%F in (%F1% %F2%) do (
  echo   -- restore %%F
  %ADB% shell "su -c 'if [ -f %TARGET_DIR%/%%F.bak ]; then cp %TARGET_DIR%/%%F.bak %TARGET_DIR%/%%F && echo RESTORED_%%F; else echo NOBAK_%%F; fi'"
)
echo Restore done.
pause
exit /b 0

:status
echo [3/3] target dir + Princess Connect entry:
%ADB% shell "su -c 'ls -l %TARGET_DIR%'"
echo.
%ADB% shell "su -c 'grep -r bilibili.priconne %TARGET_DIR%'"
pause
exit /b 0
