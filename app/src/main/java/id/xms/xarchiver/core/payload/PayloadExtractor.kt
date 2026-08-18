package id.xms.xarchiver.core.payload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * Extractor for Android OTA payload.bin partitions
 */
class PayloadExtractor {
    
    companion object {
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL = 1024 * 1024
    }
    
    /**
     * Extract a single partition from payload.bin
     */
    fun extractPartition(
        payloadFile: File,
        partition: PayloadPartition,
        outputFile: File,
        blockSize: Long = 4096
    ): Flow<PayloadExtractionProgress> = flow {
        try {
            emit(PayloadExtractionProgress(
                partitionName = partition.name,
                bytesExtracted = 0,
                totalBytes = partition.uncompressedSize,
                percentage = 0,
                state = ExtractionState.PREPARING
            ))
            
            // Check if partition has operations
            if (partition.operations.isEmpty()) {
                emit(PayloadExtractionProgress(
                    partitionName = partition.name,
                    bytesExtracted = 0,
                    totalBytes = partition.uncompressedSize,
                    percentage = 0,
                    state = ExtractionState.ERROR,
                    errorMessage = "Partition '${partition.name}' has no extraction operations. This payload.bin may be metadata-only or corrupted."
                ))
                throw Exception("No operations found for partition ${partition.name}")
            }
            
            // Check if partition has actual data
            if (partition.compressedSize == 0L) {
                emit(PayloadExtractionProgress(
                    partitionName = partition.name,
                    bytesExtracted = 0,
                    totalBytes = partition.uncompressedSize,
                    percentage = 0,
                    state = ExtractionState.ERROR,
                    errorMessage = "Partition '${partition.name}' has no compressed data (0 bytes). This payload.bin appears to be metadata-only."
                ))
                throw Exception("No data found for partition ${partition.name}")
            }
            
            // Create output file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()
            
            emit(PayloadExtractionProgress(
                partitionName = partition.name,
                bytesExtracted = 0,
                totalBytes = partition.uncompressedSize,
                percentage = 0,
                state = ExtractionState.EXTRACTING
            ))
            
            RandomAccessFile(payloadFile, "r").use { raf ->
                FileOutputStream(outputFile).use { fos ->
                    var totalBytesWritten = 0L
                    var lastProgressUpdate = 0L
                    
                    // Process each operation
                    for (operation in partition.operations) {
                        when (operation.type) {
                            OperationType.REPLACE,
                            OperationType.REPLACE_BZ,
                            OperationType.REPLACE_XZ -> {
                                // Seek to data offset
                                raf.seek(operation.dataOffset)
                                
                                // Read compressed data
                                val compressedData = ByteArray(operation.dataLength.toInt())
                                raf.readFully(compressedData)
                                
                                // Decompress and write
                                val decompressedData = when (operation.type) {
                                    OperationType.REPLACE_XZ -> decompressXZ(compressedData)
                                    OperationType.REPLACE_BZ -> decompressBZ2(compressedData)
                                    else -> compressedData // REPLACE - no compression
                                }
                                
                                // Write to output
                                fos.write(decompressedData)
                                totalBytesWritten += decompressedData.size
                                
                                // Emit progress
                                if (totalBytesWritten - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL) {
                                    val percentage = if (partition.uncompressedSize > 0) {
                                        ((totalBytesWritten * 100) / partition.uncompressedSize).toInt()
                                    } else 0
                                    
                                    emit(PayloadExtractionProgress(
                                        partitionName = partition.name,
                                        bytesExtracted = totalBytesWritten,
                                        totalBytes = partition.uncompressedSize,
                                        percentage = percentage,
                                        state = ExtractionState.EXTRACTING
                                    ))
                                    lastProgressUpdate = totalBytesWritten
                                }
                            }
                            
                            OperationType.ZERO -> {
                                // Write zeros
                                val totalBlocks = operation.dstExtents.sumOf { it.numBlocks }
                                val zeroBytes = ByteArray((totalBlocks * blockSize).toInt())
                                fos.write(zeroBytes)
                                totalBytesWritten += zeroBytes.size
                            }
                            
                            else -> {
                                // Unsupported operation types (SOURCE_COPY, BSDIFF, etc.)
                                // These require the old partition image which we don't have
                                // Skip for now
                            }
                        }
                    }
                    
                    fos.flush()
                }
            }
            
            // Verify hash if available
            if (partition.hash.isNotEmpty()) {
                emit(PayloadExtractionProgress(
                    partitionName = partition.name,
                    bytesExtracted = partition.uncompressedSize,
                    totalBytes = partition.uncompressedSize,
                    percentage = 100,
                    state = ExtractionState.VERIFYING
                ))
                
                val calculatedHash = calculateSHA256(outputFile)
                if (calculatedHash.lowercase() != partition.hash.lowercase()) {
                    emit(PayloadExtractionProgress(
                        partitionName = partition.name,
                        bytesExtracted = partition.uncompressedSize,
                        totalBytes = partition.uncompressedSize,
                        percentage = 100,
                        state = ExtractionState.ERROR,
                        errorMessage = "Hash verification failed for partition '${partition.name}'"
                    ))
                    throw Exception("Hash verification failed")
                }
            }
            
            // Completed
            emit(PayloadExtractionProgress(
                partitionName = partition.name,
                bytesExtracted = partition.uncompressedSize,
                totalBytes = partition.uncompressedSize,
                percentage = 100,
                state = ExtractionState.COMPLETED
            ))
            
        } catch (e: Exception) {
            emit(PayloadExtractionProgress(
                partitionName = partition.name,
                bytesExtracted = 0,
                totalBytes = partition.uncompressedSize,
                percentage = 0,
                state = ExtractionState.ERROR,
                errorMessage = e.message ?: "Unknown error"
            ))
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Extract multiple partitions
     */
    fun extractPartitions(
        payloadFile: File,
        partitions: List<PayloadPartition>,
        outputDirectory: File,
        blockSize: Long = 4096
    ): Flow<PayloadExtractionProgress> = flow {
        outputDirectory.mkdirs()
        
        for (partition in partitions) {
            val outputFile = File(outputDirectory, "${partition.name}.img")
            
            extractPartition(payloadFile, partition, outputFile, blockSize).collect { progress ->
                emit(progress)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Decompress XZ compressed data
     */
    private fun decompressXZ(compressedData: ByteArray): ByteArray {
        return XZInputStream(compressedData.inputStream()).use { xzIn ->
            xzIn.readBytes()
        }
    }
    
    /**
     * Decompress BZIP2 compressed data
     */
    private fun decompressBZ2(compressedData: ByteArray): ByteArray {
        return BZip2CompressorInputStream(compressedData.inputStream()).use { bz2In ->
            bz2In.readBytes()
        }
    }
    
    /**
     * Calculate SHA256 hash of a file
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
