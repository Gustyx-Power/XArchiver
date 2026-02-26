package id.xms.xarchiver.core.payload


data class PayloadPartition(
    val name: String,                    // e.g., "system", "vendor", "boot"
    val compressedSize: Long,            // Size in payload.bin
    val uncompressedSize: Long,          // Size after extraction
    val hash: String,                    // SHA256 hash
    val compressionType: CompressionType,
    val offset: Long,                    // Offset in payload.bin
    val operations: List<InstallOperation> = emptyList()
)


enum class CompressionType {
    NONE,
    BZIP2,
    XZ,
    LZMA,
    UNKNOWN
}


data class InstallOperation(
    val type: OperationType,
    val dataOffset: Long,
    val dataLength: Long,
    val dstExtents: List<Extent>
)

enum class OperationType {
    REPLACE,
    REPLACE_BZ,
    REPLACE_XZ,
    ZERO,
    DISCARD,
    SOURCE_COPY,
    SOURCE_BSDIFF,
    PUFFDIFF,
    UNKNOWN
}

data class Extent(
    val startBlock: Long,
    val numBlocks: Long
)

/**
 * Payload file header information
 */
data class PayloadHeader(
    val version: Long,
    val manifestSize: Long,
    val manifestSignatureSize: Long,
    val blockSize: Long = 4096
)


data class PayloadInfo(
    val header: PayloadHeader,
    val partitions: List<PayloadPartition>,
    val totalSize: Long,
    val filePath: String
)

data class PayloadExtractionProgress(
    val partitionName: String,
    val bytesExtracted: Long,
    val totalBytes: Long,
    val percentage: Int,
    val state: ExtractionState
)

enum class ExtractionState {
    IDLE,
    PREPARING,
    EXTRACTING,
    VERIFYING,
    COMPLETED,
    ERROR
}
