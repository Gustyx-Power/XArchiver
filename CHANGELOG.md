# Changelog

All notable changes to XArchiver will be documented in this file.

## [1.1-Release] - 2026-02-26

### Added
- New `ArchiveManager` class for centralized archive operations
- Real-time extraction progress tracking with byte-level accuracy
- Support for large archive files (3GB+) with smooth progress updates
- Enhanced archive preview functionality
- File size validation before opening in text editor (10MB limit)
- Binary file type detection to prevent opening non-text files

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

### Changed
- Improved extraction performance for large archives
- Progress updates now emit every 1MB for optimal UI responsiveness
- Refactored archive extraction to use Flow-based progress reporting
- Text editor now validates file size and type before loading
- Archive entry viewer now has size limits to prevent OOM errors

### Technical Improvements
- Added `flowOn` operator for proper coroutine context switching
- Implemented byte-based progress calculation for accurate tracking
- Enhanced error handling in archive operations
- Added file size validation throughout the application
- Improved memory management for large file operations
- Cleaned up build artifacts from version control

### Planned Features (Coming Soon)
- **Payload.bin Viewer**: Browse and extract Android OTA system images
  - View partition images (system, vendor, boot, etc.)
  - Extract individual partitions from payload.bin
  - Support for Android OTA update packages
  - Read-only access to prevent accidental modifications

---

## [1.0] - Initial Release

### Added
- Basic archive viewing and extraction
- Support for ZIP, TAR, GZ, TGZ formats
- File explorer with archive integration
- Material Design 3 UI
