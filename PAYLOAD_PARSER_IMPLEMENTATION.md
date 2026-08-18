# Payload.bin Parser Implementation

## Problem
The original Kotlin implementation using Google's Protobuf library was failing with "invalid tag (zero)" error when parsing payload.bin manifest.

## Solution
Implemented a pure Kotlin manual protobuf parser that doesn't rely on any protobuf library.

## Implementation Details

### Files Created/Modified

1. **PayloadParserManual.kt** (NEW)
   - Pure Kotlin implementation
   - Manual protobuf parsing (no library dependency)
   - Parses payload.bin header and manifest
   - Extracts partition information

2. **PayloadParser.kt** (MODIFIED)
   - Now uses PayloadParserManual internally
   - Removed all protobuf library dependencies
   - Simplified to delegate to manual parser

3. **app/build.gradle.kts** (MODIFIED)
   - Removed `com.google.protobuf` plugin
   - Removed `protobuf-javalite` dependency
   - Removed protobuf configuration block

4. **Rust Implementation** (OPTIONAL - for future use)
   - `native/payload-parser/src/lib.rs` - JNI interface
   - `native/payload-parser/src/parser.rs` - Manual protobuf parser in Rust
   - `native/payload-parser/Cargo.toml` - Rust project config
   - Requires Android NDK setup to compile

## How It Works

### Manual Protobuf Parsing

The parser manually reads the protobuf wire format:

1. **Read Varint**: Reads variable-length integers (tag, field numbers, lengths)
2. **Decode Tag**: Extracts field number and wire type from tag
3. **Parse Fields**: Based on wire type:
   - Wire type 0: Varint (integers)
   - Wire type 2: Length-delimited (strings, bytes, nested messages)

### Payload.bin Structure

```
[Header - 24 bytes]
├── Magic: "CrAU" (4 bytes)
├── Version: 2 (8 bytes, big-endian)
├── Manifest Size (8 bytes, big-endian)
└── Signature Size (4 bytes, big-endian)

[Manifest - variable size]
├── Block size (field 3)
├── Signatures offset/size (fields 4-5)
├── Minor version (field 6)
├── Old/New partition infos (fields 8-9)
├── Partial update flag (field 10)
├── Dynamic partition groups (field 12)
└── Partitions (field 13) ← We parse this
    ├── Partition name (field 1)
    ├── Operations (field 3)
    │   ├── Type (field 1): REPLACE, REPLACE_XZ, REPLACE_BZ, etc.
    │   ├── Data offset (field 2)
    │   ├── Data length (field 3)
    │   └── Dst extents (field 6)
    └── New partition info (field 5)
        ├── Size (field 1)
        └── Hash (field 2)

[Signature - variable size]

[Partition Data - variable size]
```

### Key Functions

- `readVarint()`: Reads protobuf varint encoding
- `parseManifest()`: Parses the manifest to extract partitions
- `parsePartition()`: Parses individual partition data
- `parseOperation()`: Parses install operations
- `parsePartitionInfo()`: Extracts size and hash

## Testing

Build the app:
```bash
./gradlew clean :app:assembleDebug
```

The payload viewer should now work correctly and display partitions from payload.bin files.

## Future Enhancements

1. **Rust Native Implementation**: The Rust code is ready but requires NDK setup
2. **Extraction**: Implement actual partition extraction (currently only viewing)
3. **Compression Support**: Add decompression for XZ, BZIP2 formats
4. **Progress Tracking**: Add progress callbacks during parsing

## Why Manual Parsing?

1. **No Library Dependencies**: Avoids protobuf library compatibility issues
2. **Full Control**: We control exactly how the data is parsed
3. **Debugging**: Easier to debug and understand what's happening
4. **Size**: Smaller APK size without protobuf library
5. **Reliability**: No "invalid tag" errors from library mismatches
