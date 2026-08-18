# Changelog

All notable changes to XArchiver will be documented in this file.

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
