@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1

echo ============================================
echo    XArchiver - Clean Debug Build Script
echo ============================================
echo.

:: Detect connected Android devices
echo Mendeteksi perangkat Android yang terhubung...
set DEVICE_COUNT=0
set SKIP_INSTALL=0

:: Get list of devices
for /f "skip=1 tokens=1,2" %%a in ('adb devices 2^>nul') do (
    if "%%b"=="device" (
        set /a DEVICE_COUNT+=1
        set "DEVICE_!DEVICE_COUNT!=%%a"
    )
)

if %DEVICE_COUNT% equ 0 (
    echo Tidak ada perangkat yang terhubung via USB.
    echo Build akan tetap dijalankan tanpa instalasi ke perangkat.
    set SKIP_INSTALL=1
    goto :build
)

:: Get device info for each connected device
echo.
echo Perangkat terdeteksi:
echo --------------------------------------------
set IDX=0
for /l %%i in (1,1,%DEVICE_COUNT%) do (
    set "SERIAL=!DEVICE_%%i!"
    
    :: Get model name
    for /f "tokens=*" %%m in ('adb -s !SERIAL! shell getprop ro.product.model 2^>nul') do set "MODEL=%%m"
    
    :: Get Android version
    for /f "tokens=*" %%v in ('adb -s !SERIAL! shell getprop ro.build.version.release 2^>nul') do set "VERSION=%%v"
    
    :: Remove carriage return from values
    set "MODEL=!MODEL:~0,-1!"
    set "VERSION=!VERSION:~0,-1!"
    
    echo %%i. !SERIAL! ^| Model: !MODEL! ^| Android: !VERSION!
    set "INFO_%%i=!SERIAL! | Model: !MODEL! | Android: !VERSION!"
)
echo --------------------------------------------

if %DEVICE_COUNT% equ 1 (
    set "SELECTED_DEVICE=!DEVICE_1!"
    echo.
    echo Menggunakan perangkat: !INFO_1!
) else (
    echo.
    set /p "CHOICE=Pilih perangkat (1-%DEVICE_COUNT%): "
    
    :: Validate choice
    if !CHOICE! geq 1 if !CHOICE! leq %DEVICE_COUNT% (
        set "SELECTED_DEVICE=!DEVICE_!CHOICE!!"
        echo Perangkat dipilih: !INFO_!CHOICE!!
    ) else (
        echo Pilihan tidak valid. Build akan tetap dijalankan tanpa instalasi.
        set SKIP_INSTALL=1
    )
)

:build
echo.
echo ============================================
echo    Memulai Build Process
echo ============================================
echo.



:: Get number of processors for parallel build
set WORKERS=%NUMBER_OF_PROCESSORS%
if not defined WORKERS set WORKERS=4

:: Build debug APK
echo [3/4] Melakukan build debug (menggunakan %WORKERS% workers)...
call gradlew.bat assembleDebug --parallel --max-workers=%WORKERS% --console=plain
if errorlevel 1 (
    echo       ERROR: Build gagal!
    goto :error
)
echo       Build selesai!
echo.

:: Find the APK file
set APK_PATH=
for /r "app\build\outputs\apk\debug" %%f in (*.apk) do (
    set "APK_PATH=%%f"
    goto :found_apk
)

:found_apk
if not defined APK_PATH (
    echo [ERROR] APK tidak ditemukan. Build mungkin gagal.
    goto :error
)

echo APK ditemukan: %APK_PATH%
echo.

:: Install to device if available
if %SKIP_INSTALL% equ 0 (
    echo [4/4] Menginstall APK ke perangkat %SELECTED_DEVICE%...
    adb -s %SELECTED_DEVICE% install -r "%APK_PATH%"
    if errorlevel 1 (
        echo       WARNING: Instalasi gagal!
    ) else (
        echo       APK berhasil diinstall!
    )
) else (
    echo [4/4] Tidak ada perangkat terhubung, melewati instalasi.
)

echo.
echo ============================================
echo    Build Debug Selesai!
echo ============================================
echo.

:: Show APK info
for %%f in ("%APK_PATH%") do (
    echo APK Path : %%~ff
    echo APK Size : %%~zf bytes
)

goto :end

:error
echo.
echo ============================================
echo    BUILD GAGAL!
echo ============================================
echo.
exit /b 1

:end
endlocal
pause
