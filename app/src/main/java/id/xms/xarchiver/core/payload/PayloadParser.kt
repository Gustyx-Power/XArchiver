package id.xms.xarchiver.core.payload

import id.xms.xarchiver.core.payload.proto.UpdateMetadataProtos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for Android OTA payload.bin files
 * 
 * Format:
 * - Magic: "CrAU" (4 bytes)
 * - Version: 2 (8 bytes, little-endian)
 * - Manifest size (8 bytes, little-endian)
 * - Manifest signature size (4 bytes, little-endian)
 * - Manifest (protobuf, variable size)
 * - Manifest signature (variable size)
 * - Partition data (variable size)
 */
class PayloadParser {
    
    companion object {
        private const val MAGIC = "CrAU"
        private const val MAGIC_SIZE = 4
        private const val VERSION_SIZE = 8
        private const val MANIFEST_SIZE_SIZE = 8
        private const val SIGNATURE_SIZE_SIZE = 4
        private const val HEADER_SIZE = MAGIC_SIZE + VERSION_SIZE + MANIFEST_SIZE_SIZE + SIGNATURE_SIZE_SIZE
    }
    
    /**
     * Check if a file is a valid payload.bin
     */
    suspend fun isPayloadFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) return@withContext false
            if (file.length() < HEADER_SIZE) return@withContext false
            
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(MAGIC_SIZE)
                raf.read(magic)
                String(magic) == MAGIC
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Parse payload.bin header
     */
    suspend fun parseHeader(file: File): PayloadHeader? = withContext(Dispatchers.IO) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                // Read magic
                val magic = ByteArray(MAGIC_SIZE)
                raf.read(magic)
                if (String(magic) != MAGIC) {
                    return@withContext null
                }
                
                // Read version (8 bytes, little-endian)
                val versionBytes = ByteArray(VERSION_SIZE)
                raf.read(versionBytes)
                val version = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN).long
                
                // Read manifest size (8 bytes, little-endian)
                val manifestSizeBytes = ByteArray(MANIFEST_SIZE_SIZE)
                raf.read(manifestSizeBytes)
                val manifestSize = ByteBuffer.wrap(manifestSizeBytes).order(ByteOrder.LITTLE_ENDIAN).long
                
                // Read manifest signature size (4 bytes, little-endian)
                val signatureSizeBytes = ByteArray(SIGNATURE_SIZE_SIZE)
                raf.read(signatureSizeBytes)
                val signatureSize = ByteBuffer.wrap(signatureSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
                
                PayloadHeader(
                    version = version,
                    manifestSize = manifestSize,
                    manifestSignatureSize = signatureSize
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse full payload info with protobuf manifest parsing
     */
    suspend fun parsePayloadInfo(file: File): PayloadInfo? = withContext(Dispatchers.IO) {
        try {
            val header = parseHeader(file) ?: return@withContext null
            val manifestBytes = getManifestBytes(file) ?: return@withContext null
            
            // Parse protobuf manifest
            val manifest = UpdateMetadataProtos.DeltaArchiveManifest.parseFrom(manifestBytes)
            
            // Calculate data offset (after header + manifest + signature)
            val dataOffset = HEADER_SIZE + header.manifestSize + header.manifestSignatureSize
            
            // Parse partitions
            val partitions = manifest.partitionsList.map { partition ->
                parsePartition(partition, dataOffset, header.blockSize)
            }
            
            PayloadInfo(
                header = header.copy(blockSize = manifest.blockSize.toLong()),
                partitions = partitions,
                totalSize = file.length(),
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Parse a single partition from protobuf
     */
    private fun parsePartition(
        partition: UpdateMetadataProtos.PartitionUpdate,
        dataOffset: Long,
        blockSize: Long
    ): PayloadPartition {
        val operations = partition.operationsList.map { op ->
            InstallOperation(
                type = mapOperationType(op.type),
                dataOffset = dataOffset + (op.dataOffset ?: 0),
                dataLength = op.dataLength ?: 0,
                dstExtents = op.dstExtentsList.map { extent ->
                    Extent(
                        startBlock = extent.startBlock ?: 0,
                        numBlocks = extent.numBlocks ?: 0
                    )
                }
            )
        }
        
        // Calculate compressed and uncompressed sizes
        val compressedSize = operations.sumOf { it.dataLength }
        val uncompressedSize = partition.newPartitionSize ?: 0
        
        // Determine compression type from operations
        val compressionType = detectCompressionType(operations)
        
        // Get hash (convert bytes to hex string)
        val hash = partition.newPartitionHash?.toByteArray()?.joinToString("") { 
            "%02x".format(it) 
        } ?: ""
        
        // Calculate offset (first operation's data offset)
        val offset = operations.firstOrNull()?.dataOffset ?: 0
        
        return PayloadPartition(
            name = partition.partitionName,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            hash = hash,
            compressionType = compressionType,
            offset = offset,
            operations = operations
        )
    }
    
    /**
     * Map protobuf operation type to our enum
     */
    private fun mapOperationType(type: UpdateMetadataProtos.InstallOperation.Type): OperationType {
        return when (type) {
            UpdateMetadataProtos.InstallOperation.Type.REPLACE -> OperationType.REPLACE
            UpdateMetadataProtos.InstallOperation.Type.REPLACE_BZ -> OperationType.REPLACE_BZ
            UpdateMetadataProtos.InstallOperation.Type.REPLACE_XZ -> OperationType.REPLACE_XZ
            UpdateMetadataProtos.InstallOperation.Type.ZERO -> OperationType.ZERO
            UpdateMetadataProtos.InstallOperation.Type.DISCARD -> OperationType.DISCARD
            UpdateMetadataProtos.InstallOperation.Type.SOURCE_COPY -> OperationType.SOURCE_COPY
            UpdateMetadataProtos.InstallOperation.Type.SOURCE_BSDIFF -> OperationType.SOURCE_BSDIFF
            UpdateMetadataProtos.InstallOperation.Type.PUFFDIFF -> OperationType.PUFFDIFF
            else -> OperationType.UNKNOWN
        }
    }
    
    /**
     * Detect compression type from operations
     */
    private fun detectCompressionType(operations: List<InstallOperation>): CompressionType {
        // Check the most common operation type
        val types = operations.map { it.type }
        return when {
            types.any { it == OperationType.REPLACE_XZ } -> CompressionType.XZ
            types.any { it == OperationType.REPLACE_BZ } -> CompressionType.BZIP2
            types.any { it == OperationType.REPLACE } -> CompressionType.NONE
            else -> CompressionType.UNKNOWN
        }
    }
    
    /**
     * Get manifest bytes for protobuf parsing
     */
    private suspend fun getManifestBytes(file: File): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val header = parseHeader(file) ?: return@withContext null
            
            RandomAccessFile(file, "r").use { raf ->
                // Skip to manifest
                raf.seek(HEADER_SIZE.toLong())
                
                // Read manifest
                val manifestBytes = ByteArray(header.manifestSize.toInt())
                raf.read(manifestBytes)
                
                manifestBytes
            }
        } catch (e: Exception) {
            null
        }
    }
}
