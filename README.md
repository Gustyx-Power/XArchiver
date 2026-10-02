# XArchiver

XArchiver is a modern, high-performance open-source archive and file manager engineered for Android 10 and above (API level 29+). Designed with a clean modular architecture and Jetpack Compose, XArchiver provides comprehensive archive extraction and compression, specialized Android OTA payload.bin parsing via native Rust, multi-storage volume diagnostics, elevated file operations via Shizuku and Superuser, and a dual visual interface supporting both Material Design 3 and Xiaomi MIUIX HyperOS aesthetics.

---

## Key Features

### 1. Dual Visual Design System
- **MIUIX HyperOS Aesthetic**: Native squircle cards, real-time texture blur, floating pill navigation bars, and HyperOS system-inspired settings and about screens.
- **Expressive Material 3**: Dynamic color harmonies, custom typography, AMOLED deep dark mode, and pill notifications.
- **Intelligent Auto-Detection**: Automatically detects Xiaomi, Redmi, and Poco devices running MIUI or HyperOS to enable the MIUIX UI by default, with complete manual override support in Settings.

### 2. Comprehensive Archive Engine
- **Broad Format Support**: Create and extract standard archives including ZIP, TAR, GZ, TGZ, 7Z, BZIP2, and XZ.
- **Non-Destructive Archive Inspection**: Browse directory trees, view file metadata, and inspect internal contents without full extraction.
- **Streaming Extraction Engine**: Byte-accurate, single-pass extraction pipeline reporting real-time progress via Kotlin Coroutines and StateFlow.
- **Conflict Handling**: Intelligent collision detection and resolution dialogs for duplicate file entries.

### 3. Native Android OTA Payload.bin Parser
- **Rust Native JNI Core**: High-throughput OTA partition unpacking powered by native Rust (`libpayload_parser.so`).
- **Partition Browser**: Inspect partition manifests (boot, init_boot, vendor_boot, system, vendor, product, odm, etc.) directly on device.
- **Selective Extraction**: Extract individual partitions or unpack entire firmware images into flashable raw `.img` files.
- **Integrity Verification**: Automatic post-extraction SHA256 checksum verification against manifest records.

### 4. Storage & File System Diagnostics
- **Multi-Storage Volume Manager**: Seamless navigation across Internal Storage, Physical SD Cards, USB OTG drives, and Root system partitions.
- **Filesystem Detection**: Real-time identification of partition filesystem types (F2FS, ext4, FAT32, exFAT).
- **Recent Files Hub**: Dedicated tracking for recently opened, modified, and extracted archives with category sorting and metadata chips.
- **Enhanced Explorer**: Multi-selection operations, bulk copy/move/delete, file renaming, and Coil-powered thumbnail generation for images, videos, and APK packages.

### 5. Privileged Access Integration
- **Shizuku API Support**: Execute elevated operations on restricted Android directories (`/Android/data`, `/Android/obb`) without requiring device rooting.
- **Root Superuser Shell**: Deep root filesystem integration using `libsu` for system modifications and debugging.
- **Privileged Text Editor**: Built-in editor for inspecting and editing protected configuration and text files.

### 6. Built-In Release Notes & Documentation
- **Native In-App Changelog**: Interactive release notes screen accessible directly from the top bar next to Settings.
- **Technical Specifications Screen**: Detailed view of application architecture, runtime dependencies, SDK levels, and platform capabilities.
- **Supported Formats Catalog**: Comprehensive reference matrix of supported packaging formats, streaming algorithms, and compression levels.
- **Bilingual Localization**: Full Indonesian and English language translations across all interfaces.

---

## Supported Archive Formats

| Format | Extension | View / Browse | Extraction | Compression | Engine / Implementation |
|---|---|---|---|---|---|
| ZIP | `.zip`, `.jar`, `.apk` | Yes | Yes | Yes | Apache Commons Compress / Java Zip |
| TAR | `.tar` | Yes | Yes | Yes | Apache Commons Compress |
| GZIP | `.gz`, `.tgz` | Yes | Yes | Yes | Apache Commons Compress |
| BZIP2 | `.bz2`, `.tbz2` | Yes | Yes | Yes | Apache Commons Compress |
| XZ / LZMA | `.xz`, `.txz` | Yes | Yes | Yes | Tukaani XZ for Java |
| 7-Zip | `.7z` | Yes | Yes | Partial | Apache Commons Compress |
| OTA Payload | `payload.bin` | Yes | Yes | N/A | Native Rust JNI (`libpayload_parser`) |

---

## Project Architecture

The project is structured into clean, decoupled Android library modules:

```
XArchiver/
|-- app/                  # Main application, Jetpack Compose UI, Navigation, Themes
|   |-- ui/home/          # Home screens (Material 3 and MIUIX HyperOS)
|   |-- ui/explorer/      # File browser, selection manager, thumbnail loaders
|   |-- ui/payload/       # Android OTA payload.bin viewer and partition extractor
|   |-- ui/changelog/     # In-app native release notes screen and repository
|   |-- ui/settings/      # Application preferences, theme toggle, root controls
|   `-- ui/theme/         # Design tokens, MIUIX KMP bridge, dynamic colors
|
|-- core-archive/         # Archive reader interfaces, entry models, format detectors
|-- core-extract/         # Streaming extraction engine, Flow progress reporting
|-- core-storage/         # Volume discovery, RecentManager, storage diagnostics
|-- core-root/            # Shizuku API bindings and libsu root shell services
`-- native-engine/        # Rust JNI source for high-performance payload parsing
```

---

## Technical Specifications

- **Language**: Kotlin 2.0.21, Rust 2021 Edition
- **UI Framework**: Jetpack Compose (Material Design 3 + MIUIX KMP v0.9.3)
- **Minimum SDK**: Android 10 (API level 29)
- **Target SDK**: Android 15 (API level 35)
- **Architecture**: MVI / MVVM with unidirectional data flow and Kotlin Coroutines/Flow
- **Image Pipeline**: Coil 2.7.0
- **Privileged Access**: Shizuku v13+ (`dev.rikka.shizuku:api`), TopJohnWu libsu v6.0.0
- **Persistence**: Jetpack DataStore Preferences

---

## Prerequisites & Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (recommended: Eclipse Temurin or Azul Zulu)
- Android SDK Platform 35 and Build Tools 35.0.0
- Android NDK (r26b or newer) if building native Rust modules
- Rust toolchain (`cargo`, `rustc`) with Android targets (`aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`)

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Gustyx-Power/XArchiver.git
   cd XArchiver
   ```

2. Compile debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Install on connected test device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Compile release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## Localization

XArchiver features full resource-level internationalization:
- **English**: Default locale (`values/strings.xml` `changelogs-en.md`)
- **Indonesian**: Indonesian locale (`values-in/strings.xml`, `changelogs-id.md`)

---

## Credits and Acknowledgements

### Architectural and Design References
XArchiver draws valuable inspiration, design paradigms, and operational workflows from leading mobile file managers and archive tools:
- **MT Manager**: Inspiration for advanced archive inspection, payload.bin partition browsing, and privileged file editing capabilities.
- **OriginOS File**: Inspiration for sleek squircle card layouts, clean typographic balance, and modern UI aesthetic.
- **ZArchiver**: Inspiration for dependable, comprehensive archive management, on-the-fly streaming extraction, and broad compression format support.

### Open-Source Libraries and Dependencies
Special gratitude to the open-source projects and communities that make XArchiver possible:
- **MIUIX KMP** (`top.yukonga.miuix.kmp` by yukonga): Xiaomi HyperOS and MIUI design system components, squircle shapes, real-time texture blur, and extended icon sets.
- **Apache Commons Compress & Commons IO**: Battle-tested archive manipulation and streaming decompression engine (ZIP, TAR, GZ, BZIP2, 7Z).
- **Tukaani XZ for Java**: Native Java implementation of the XZ and LZMA data compression standards.
- **Google Protocol Buffers** (`protobuf-javalite`): Lightweight serialization framework for parsing Android OTA payload manifests.
- **Rikka Shizuku** (`dev.rikka.shizuku`): Standardized framework enabling elevated Android system file operations without requiring full root access.
- **topjohnwu libsu**: Robust, high-performance root shell implementation and superuser service bridge.
- **Coil** (`io.coil-kt:coil-compose`): Fast, memory-efficient asynchronous image and video thumbnail decoding pipeline for Jetpack Compose.
- **Google Jetpack Media3 (ExoPlayer)**: Audio and video playback foundation for in-app media previews.
- **Square Okio**: High-performance I/O and byte-stream library for efficient file and buffer handling.
- **Google Android Jetpack**: Compose, DataStore, Lifecycle, Navigation, and Material Design 3 foundation.

---

## Contributing

Contributions, bug reports, and feature requests are welcome. When submitting a pull request:
1. Ensure the code compiles cleanly without warnings: `./gradlew assembleDebug`
2. Follow Kotlin official coding conventions and Compose best practices.
3. Provide localized string resources for any newly introduced user-facing text.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for complete details.

