@echo off
setlocal enabledelayedexpansion

echo ========================================
echo XArchiver Debug Build Script (Windows)
echo ========================================
echo.

echo [INFO] Starting complete debug build pipeline...

REM Check prerequisites
echo [INFO] Checking prerequisites...

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java or Android Studio
    exit /b 1
)
echo [SUCCESS] Java found

where cargo >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Rust/Cargo not found. Please install Rust from https://rustup.rs/
    exit /b 1
)
echo [SUCCESS] Rust/Cargo found

where adb >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] ADB not found in PATH. Will try to use Android SDK version
    set "ADB_FOUND=false"
) else (
    echo [SUCCESS] ADB found
    set "ADB_FOUND=true"
)

REM Auto-detect Android SDK
if "%ANDROID_HOME%"=="" (
    set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    echo [INFO] Auto-detected ANDROID_HOME: !ANDROID_HOME!
)

if not exist "!ANDROID_HOME!" (
    echo [ERROR] Android SDK not found at !ANDROID_HOME!
    echo [ERROR] Please install Android Studio and SDK
    exit /b 1
)
echo [SUCCESS] Android SDK found: !ANDROID_HOME!

REM Find NDK version
set "ANDROID_NDK_ROOT="
for /d %%i in ("!ANDROID_HOME!\ndk\*") do (
    set "ANDROID_NDK_ROOT=%%i"
    goto :found_ndk
)

:found_ndk
if "%ANDROID_NDK_ROOT%"=="" (
    echo [ERROR] NDK not found in !ANDROID_HOME!\ndk\
    echo [ERROR] Please install NDK via Android Studio SDK Manager
    exit /b 1
)
echo [SUCCESS] NDK found: !ANDROID_NDK_ROOT!

REM Set up ADB path if not in system PATH
if "%ADB_FOUND%"=="false" (
    if exist "!ANDROID_HOME!\platform-tools\adb.exe" (
        set "ADB_PATH=!ANDROID_HOME!\platform-tools\adb.exe"
        echo [INFO] Using ADB from Android SDK
    ) else (
        echo [WARNING] ADB not found. Device detection and installation will be skipped
        set "ADB_PATH="
    )
) else (
    set "ADB_PATH=adb"
)

echo.
echo [INFO] === PHASE 1: Building Rust Library (Release) ===

REM Check if Rust Android target is installed
rustup target list --installed | findstr "aarch64-linux-android" >nul
if %errorlevel% neq 0 (
    echo [INFO] Adding Android target for Rust...
    rustup target add aarch64-linux-android
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to add Android target
        exit /b 1
    )
)
echo [SUCCESS] Android target ready

REM Set up cross-compilation environment
set "TOOLCHAIN_PATH=!ANDROID_NDK_ROOT!\toolchains\llvm\prebuilt\windows-x86_64"
echo [INFO] Using toolchain: !TOOLCHAIN_PATH!

if not exist "!TOOLCHAIN_PATH!\bin\aarch64-linux-android29-clang.cmd" (
    echo [ERROR] NDK toolchain not found at expected location
    echo [ERROR] Expected: !TOOLCHAIN_PATH!\bin\aarch64-linux-android29-clang.cmd
    exit /b 1
)

REM Set environment variables for cross-compilation
set "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=!TOOLCHAIN_PATH!\bin\aarch64-linux-android29-clang.cmd"
set "CC_aarch64_linux_android=!TOOLCHAIN_PATH!\bin\aarch64-linux-android29-clang.cmd"
set "AR_aarch64_linux_android=!TOOLCHAIN_PATH!\bin\llvm-ar.exe"
set "PATH=!TOOLCHAIN_PATH!\bin;%PATH%"

echo [INFO] Cross-compilation environment configured

REM Build Rust library
echo [INFO] Building Rust library (release mode)...
cd native/payload-parser

cargo build --release --target aarch64-linux-android
if %errorlevel% neq 0 (
    echo [ERROR] Rust build failed!
    exit /b 1
)
echo [SUCCESS] Rust library built successfully

REM Create jniLibs directory and copy library
if not exist "..\jniLibs\arm64-v8a" mkdir "..\jniLibs\arm64-v8a"
copy "target\aarch64-linux-android\release\libpayload_parser.so" "..\jniLibs\arm64-v8a\" >nul
if %errorlevel% neq 0 (
    echo [ERROR] Failed to copy Rust library
    exit /b 1
)
echo [SUCCESS] Rust library copied to jniLibs

cd ..\..

echo.
echo [INFO] === PHASE 2: Building Android Project (Debug) ===

REM Clean previous build
echo [INFO] Cleaning previous build...
call gradlew.bat clean
echo [INFO] Clean step completed, continuing with build...

REM Build debug APK
echo [INFO] Building debug APK (includes C++ compilation)...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] Android build failed!
    exit /b 1
)
echo [SUCCESS] Debug APK built successfully

REM Verify APK exists
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [ERROR] APK not found at expected location
    exit /b 1
)

for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do set "APK_SIZE=%%~zA"
echo [SUCCESS] APK created: app\build\outputs\apk\debug\app-debug.apk (!APK_SIZE! bytes)

echo.
echo [INFO] === PHASE 3: Device Detection and Installation ===

if "%ADB_PATH%"=="" (
    echo [WARNING] ADB not available. Skipping device detection and installation
    goto :build_complete
)

REM Check for connected devices
echo [INFO] Checking for connected Android devices...
"!ADB_PATH!" devices > temp_devices.txt 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] Failed to run ADB. Skipping device detection
    goto :build_complete
)

REM Count devices (excluding header line)
set "DEVICE_COUNT=0"
for /f "skip=1 tokens=2" %%i in (temp_devices.txt) do (
    if "%%i"=="device" (
        set /a DEVICE_COUNT+=1
    )
)
del temp_devices.txt >nul 2>nul

if %DEVICE_COUNT% equ 0 (
    echo [WARNING] No Android devices connected via USB
    echo.
    echo To install manually:
    echo 1. Enable USB debugging on your Android device
    echo 2. Connect device via USB
    echo 3. Run: adb install -r app\build\outputs\apk\debug\app-debug.apk
    goto :build_complete
)

echo [SUCCESS] Found %DEVICE_COUNT% connected Android device(s) with USB debugging enabled

REM Check if device is authorized
"!ADB_PATH!" devices | findstr "unauthorized" >nul
if %errorlevel% equ 0 (
    echo [WARNING] Device found but not authorized. Please check your device for USB debugging authorization dialog
    goto :build_complete
)

REM Install APK automatically
echo [INFO] Installing APK on connected device(s)...
"!ADB_PATH!" install -r "app\build\outputs\apk\debug\app-debug.apk"
if %errorlevel% equ 0 (
    echo [SUCCESS] APK installed successfully!
    echo.
    echo [INFO] You can now launch XArchiver on your device
    
    REM Try to launch the app
    echo [INFO] Attempting to launch XArchiver...
    "!ADB_PATH!" shell am start -n id.xms.xarchiver/.MainActivity >nul 2>nul
    if %errorlevel% equ 0 (
        echo [SUCCESS] XArchiver launched successfully!
    ) else (
        echo [WARNING] Could not auto-launch app. Please launch manually from device
    )
) else (
    echo [ERROR] Failed to install APK. Please check device connection and try manual installation
)

:build_complete
echo.
echo ========================================
echo [SUCCESS] BUILD COMPLETE!
echo ========================================
echo.
echo Build Summary:
echo   Rust Engine: BUILT (Release mode, optimized)
echo   C++ Bridge:  BUILT (Debug mode, with symbols)
echo   Android App: BUILT (Debug mode, debuggable)
echo   APK Size:    !APK_SIZE! bytes
echo   Location:    app\build\outputs\apk\debug\app-debug.apk
echo.
if %DEVICE_COUNT% gtr 0 (
    echo Device Status: INSTALLED and READY
) else (
    echo Device Status: Manual installation required
)
echo.
pause