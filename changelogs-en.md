# Changelog

All notable changes to XArchiver will be documented in this file.

## [2.1.6] - 2026-10-07

### Added & UI Polish - Dedicated MIUIX/HyperOS Explorer & Architecture Modularization
- **Full MIUIX/HyperOS Explorer Experience**: Implemented a dedicated File Explorer interface when MIUIX theme mode is active (`isMiuixUiEnabled`), featuring HyperOS cards, format color badges, shimmer loading skeleton, and empty states.
- **Dynamic Breadcrumbs Navigation (MIUIX Chips)**: Added a scrollable horizontal breadcrumbs bar with pill chips for fluid path navigation across folder hierarchies.
- **Quick Action Docks & Speed-Dial FAB**: Introduced floating selection action dock (`MiuixSelectionDock`), floating clipboard paste dock (`MiuixClipboardDock`), and expandable speed-dial FAB for file and folder creation.
- **Native HyperOS Dialogs & Modals**: Redesigned all explorer operation dialogs (new folder/file, rename, delete confirm, quick extract, APK installer, progress overlays) with native MIUIX aesthetic.
- **Full Explorer Architecture Modularization (Material & MIUIX)**: Decomposed both monolithic Material (`ExplorerScreen`) and MIUIX (`MiuixExplorerScreen`) explorer screens from >2.2k lines down to clean, modular architectures with dedicated subpackages (`material/` and `miuix/`: TopBars, Docks, FileItemCard, Dialogs, and DialogHost) for superior maintainability and developer ergonomics.

---

## [2.1.5] - 2026-10-06

### Fixed & Stability - Standalone OTA Update Engine
- **Semantic Version Comparison Fix**: Fixed version string splitting in `UpdateManager`. Previously, local builds with date suffixes (e.g. `2.1.3-20261004`) caused the patch digit to fail integer parsing, truncating the version to `2.1.0` and causing false-positive update loops for already installed releases.
- **Build Date & Suffix Awareness**: Implemented dedicated semantic version parsing separating base components (`major.minor.patch`) from build/date suffixes (`buildSuffix`), preventing false update triggers when base versions match.
- **Dynamic App Version Resolution**: Replaced hardcoded version fallback strings in `UpdateManager` with dynamic runtime resolution via `PackageManager` (`getAppVersionName`).
- **Automatic Notification & Cache Dismissal**: Reset `availableUpdate` to null, auto-cancelled system update notifications, and cleared update preferences whenever the app confirms it is running the latest version (`UpToDate`).
- **Auto-Invalidation on Update Screen**: Automatically clears outdated update prompts upon opening the update screen if the cached update is no longer newer than the installed version.
- **Comprehensive Unit Tests**: Added automated unit test suite (`UpdateManagerTest`) covering diverse tag naming conventions and comparison scenarios.

---

## [2.1.3] - 2026-10-04

### Added - ZIP Password Encryption (AES-256 & ZipCrypto)
- **Password Encryption on Compression**: Full support for creating password-protected ZIP archives with selectable encryption algorithms: industry-standard **AES-256** (maximum security) and **ZipCrypto** (legacy compatibility).
- **Automatic Locked Archive Detection**: Smart detection of encrypted ZIP archives upon previewing or extracting, seamlessly triggering the password entry dialog.
- **Zip4j Engine Integration**: Leveraged Zip4j library for robust and reliable ZIP password encryption and decryption handling.

### Redesigned & UI Enhancements
- **App-Wide Proportional Dialog Redesign**: Completely overhauled all popup dialogs across the application (compression, extraction, create folder/file, rename, overwrite confirmation, etc.) for a clean, proportional, and balanced Material 3 look.
- **Symmetric 50/50 Action Buttons**: Replaced uneven buttons with balanced 50/50 horizontal action rows designed for natural thumb reach.
- **Structured Header Icon Containers**: Added rounded squircle icon containers at the top of each dialog with thematic accent background tones.
- **Accurate Password Prompt Labels**: Cleaned up the unlock dialog label to "Password" (mandatory) instead of displaying an optional hint.

### Fixed & Stability
- **Encrypted Archive Preview Crash Fix**: Fixed a fatal crash when tapping once to view password-protected ZIP archives in the Explorer; the app now gracefully requests the archive password and opens contents safely.

---

## [2.1.0] - 2026-10-03

### Added - Recent Files & Activity Tracking
- **Recent Files Manager**: Added `RecentManager` to track recently accessed, modified, and extracted archives and files.
- **Dedicated Recent Files Tab**: Built-in tab on the Home Screen with category grouping, file metadata, and quick access actions.
- **Text Marquee Animation**: Implemented animated marquee for filenames and overflowing card titles.

### Added - Standalone OTA App Updates & Markdown Rendering
- **Dedicated Update Screen**: Standalone software update screen tailored for both MIUIX / HyperOS and Material Design 3 modes without dialog popups.
- **Automatic GitHub Releases Checker**: Detects new app releases directly from GitHub Releases API without Firebase dependencies.
- **Progress-Filling Pill**: Download action button with an interactive pill that fills smoothly horizontally as download progresses in real-time.
- **Native Markdown Renderer**: Renders rich release notes with headers, accented bullet lists, monospace inline code chips, and clickable links.
- **Notifications & Badge Indicators**: Displays an update badge dot on the Settings icon, About screen rows, and posts high-priority system notifications.

### Added - MIUIX / HyperOS Aesthetic Interface
- **MIUIX KMP Integration**: Integrated MIUIX Kotlin Multiplatform design components (`miuix-ui`, `miuix-blur`, `miuix-icons` v0.9.3).
- **HyperOS-styled Home Screen (`MiuixHomeScreen`)**: Features squircle cards, texture blur headers, and modern floating navigation pill.
- **HyperOS-styled Settings (`MiuixSettingsScreen`)**: Grouped settings cards, smooth switch tiles, and themed icons.
- **HyperOS "About Phone" Style About Screen (`MiuixAboutScreen`)**: Detailed app identity card, storage quick info, and specs navigation.
- **Dedicated Detailed App Specs Screen**: Standalone screens for both MIUIX (`MiuixAppSpecsScreen`) and Material 3 (`AppSpecsScreen`) displaying comprehensive app architecture, archiver engines, UI framework, and system parameters.
- **Dedicated Supported Formats Screen**: Standalone catalog screens (`MiuixSupportedFormatsScreen` and `SupportedFormatsScreen`) detailing archive types, single-file streams, packaging specs, and capability badges.

### Added - In-App Release Notes Screen
- **Native In-App Changelog Viewer**: Added dedicated `ChangelogScreen` and `MiuixChangelogScreen` accessible directly from the top bar next to Settings.
- **Proportional Full-Width Typography**: Clean full-width layouts for release items, micro-category pills, version filter pills, copy release notes, and summary callout cards.

### Added - Intelligent Device Auto-Detection
- **Xiaomi / Redmi / Poco Auto-Switch**: Automatic activation of MIUIX UI mode on Xiaomi family devices running MIUI or Xiaomi HyperOS.
- **Multi-tier Detection**: Checks hardware brand/manufacturer identity, MIUI/HyperOS system properties, and framework introspection.
- **User Preference Override**: Manual toggle in Settings remains fully respected and persisted in DataStore.

### Added - In-App HTML Web Runner & Preview
- **Interactive HTML Runner**: Added a Play button in the Text Editor when opening or editing `.html`/`.htm` files to render and test websites directly in-app without launching an external browser.
- **Full Relative Asset Support**: Automatically resolves and loads local CSS stylesheets, JavaScript scripts, and images in the same folder or subdirectories.
- **Mobile / Desktop Viewport Toggle**: Desktop mode switcher that overrides the viewport meta tag (1280px width), enables overview scaling, and simulates desktop Chrome to test responsive layouts.

### Changed & Modernized
- **Minimum SDK Raised**: Increased `minSdk` from Android 8.0 (API 26) to Android 10 (API 29) across all app and core modules (`core-storage`, `core-archive`, `core-extract`, `core-root`).
- **Manifest Merger Compatibility**: Added `tools:overrideLibrary` configuration for MIUIX KMP libraries to ensure smooth runtime execution on Android 10+.
- **Full Localization (English & Indonesian)**: Complete resource-based localization (`strings.xml` and `values-in/strings.xml`) for all About, Specs, and Supported Formats screens and badges.
- **App Version Bump**: Bumped to version `2.1.0` (versionCode `2`).

### Optimized - File Explorer & Scrolling Performance
- **LazyColumn Item Recycling (`contentType`)**: Implemented `contentType` distinguishing folders and files to allow optimal composition slot recycling during fast scrolling.
- **In-Memory APK Icon Cache (`LruCache`)**: Added memory caching for APK icons to prevent repeated disk I/O and zip manifest parsing during scrolling.
- **Eliminated Heavy Touch Animations**: Removed coroutine-based spring physics animations on card interaction to ensure instantaneous, stutter-free scroll response.
- **Overdraw Reduction & Memoization**: Reduced unnecessary card shadow overdraw and memoized file metadata formatting and category color lookups.

---

## [2.0.0] - 2026-08-19

### Added - Elevated Privileges & Root Integration
- **Shizuku Integration**: Added support for Shizuku API (v13+) via `dev.rikka.shizuku` for privileged file operations without root.
- **Root Shell Service**: Integrated `com.github.topjohnwu.libsu:core` v6.0.0 for full superuser (su) capabilities.
- **Privileged File Operations**: Implemented `ShizukuFileService` and `RootFileService` with automated fallback in `FileOperationsManager`.
- **Privileged Text Editing**: Enabled reading and writing protected system files via `TextEditorScreen`.
- **Privileged Access UI**: Added dedicated Shizuku status indicator, setup guide, and root toggle in Settings.

### Added - Storage Management & File Explorer Overhaul
- **Multi-Storage Horizontal Pager**: Redesigned home storage section supporting Internal Storage, SD Card, OTG USB, and Root partitions.
- **Robust Storage Detection**: Implemented Android `StorageManager` volume discovery with automatic polling and refresh for OTG drives.
- **Filesystem Diagnostics**: Real-time filesystem type detection (F2FS, ext4, FAT32) and storage health metrics.
- **Enhanced File Explorer**: Multi-file selection mode (`SelectionManager`), batch extraction, copy, move, delete, and rename.
- **Media Previews & APK Badges**: Integrated Coil image and video thumbnail decoding; extracted application icons for APK files.
- **Conflict Resolution**: Added file collision dialog for duplicate file handling during extraction/copying.

### Added - Native Payload Parser & Archive Capabilities
- **Rust Native Payload Engine**: Integrated native Rust `payload-parser` JNI module for rapid Android OTA system image extraction.
- **`PayloadViewerScreen`**: Interactive partition browser with individual/batch extraction and SHA256 integrity verification.
- **Unified `ArchiveReader`**: Standardized stream-based archive reader architecture.
- **Streaming Extraction Progress**: Byte-level real-time extraction progress updates.
- **Selective Extraction**: Temporary storage extraction and inspection without uncompressing full archives.

### Added - UI/UX & Dynamic Island Notifications
- **Expressive Material 3 Design**: Overhauled color palette, custom squircle shapes, and AMOLED dark mode.
- **Dynamic Island Notifications**: Replaced stock snackbars with animated pill notification banners (`DynamicIslandNotificationHost`).
- **Modernized Settings & About**: Full-screen Settings redesign and dynamic version metadata in About screen.
- **Permission Flow Setup**: Streamlined onboarding with permission-gated `SetupScreen`.
- **String Internationalization**: Standardized string resources for multi-language support.

---

## [1.1-Release] - 2026-02-26

### Added - Payload.bin Viewer (NEW!)
- **Complete Android OTA Payload.bin Support**
  - View and extract Android OTA system images
  - Browse partition list (system, vendor, boot, product, etc.)
  - Extract individual partitions to .img files
  - Extract all partitions at once
  - Real-time extraction progress tracking
  - SHA256 hash verification after extraction
  
- **Protobuf Manifest Parsing**
  - Parse Android OTA manifest using Protocol Buffers
  - Display partition information (name, size, compression type)
  - Support for multiple compression types (XZ, BZIP2, uncompressed)
  - Block-level operation parsing
  
- **Advanced Decompression**
  - XZ/LZMA decompression support
  - BZIP2 decompression support
  - On-the-fly decompression (no temporary files)
  - Memory-efficient streaming decompression

### Added - Archive Management
- New `ArchiveManager` class for centralized archive operations
- Real-time extraction progress tracking with byte-level accuracy
- Support for large archive files (3GB+) with smooth progress updates
- Enhanced archive preview functionality
- File size validation before opening in text editor (10MB limit)
- Binary file type detection to prevent opening non-text files

### Added - Code Refactoring
- Created `ExplorerViewModel` for better separation of concerns
- Created `FileTypeDetector` utility for file type detection
- Created `FileActionHandler` for centralized file click handling
- Modularized `ExplorerScreen` (reduced from 1304 lines)
- Improved code maintainability and testability

### Fixed
- **Critical**: Fixed application crash when previewing compressed files (ZIP, TAR, GZ)
  - Resolved Flow invariant violation by properly using `flowOn(Dispatchers.IO)`
  - Archive preview now works reliably without crashes
- **Critical**: Fixed OutOfMemoryError when opening large binary files (e.g., payload.bin)
  - Added 10MB file size limit for text editor
  - Prevented binary files from being opened as text
  - Added proper error messages for unsupported file types
- **Major**: Fixed extraction progress bar stuck at 0%
  - Implemented single-pass extraction with byte-level progress tracking
  - Progress bar now updates smoothly during extraction of large files
  - Eliminated unnecessary two-pass archive scanning
- Fixed archive adapter interface to support batch extraction operations
- Fixed memory issues when viewing large archive entries
- Fixed compilation errors in ExplorerViewModel (type mismatches)
- Fixed FileItem import conflicts between core and storage packages

### Changed
- Improved extraction performance for large archives
- Progress updates now emit every 1MB for optimal UI responsiveness
- Refactored archive extraction to use Flow-based progress reporting
- Text editor now validates file size and type before loading
- Archive entry viewer now has size limits to prevent OOM errors
- Default extraction output: `Downloads/XArchiver/extracted/`

### Technical Improvements
- Added Protocol Buffers support for Android OTA manifest parsing
- Integrated `org.tukaani:xz:1.9` for XZ decompression
- Integrated Apache Commons Compress for BZIP2 decompression
- Added `flowOn` operator for proper coroutine context switching
- Implemented byte-based progress calculation for accurate tracking
- Enhanced error handling in archive operations
- Added file size validation throughout the application
- Improved memory management for large file operations
- Cleaned up build artifacts from version control
- Added protobuf gradle plugin configuration

### Documentation
- Created comprehensive project structure documentation
- Added Phase 2 completion documentation (Protobuf parsing)
- Added Phase 3 completion documentation (Partition extraction)
- Updated refactoring summary

### Dependencies Added
- `com.google.protobuf:protobuf-javalite:3.25.1` - Protobuf parsing
- `org.tukaani:xz:1.9` - XZ/LZMA decompression
- Protobuf Gradle Plugin v0.9.4

### Known Limitations
- Delta OTA updates not supported (requires old partition image)
- No custom output directory selection
- Sequential partition extraction (no parallel processing)
- No extraction resume support

---

## [1.0] - Initial Release

### Added
- Basic archive viewing and extraction
- Support for ZIP, TAR, GZ, TGZ formats
- File explorer with archive integration
- Material Design 3 UI
