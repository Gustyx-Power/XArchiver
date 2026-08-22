package id.xms.xarchiver.core.payload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manual protobuf parser for payload.bin (no protobuf library dependency)
 * This is a pure Kotlin implementation that manually parses the protobuf format
 */
class PayloadParserManual {
    
    companion object {
        private const val MAGIC = "CrAU"
        private const val MAGIC_SIZE = 4
        private const val VERSION_SIZE = 8
        private const val MANIFEST_SIZE_SIZE = 8
        private const val SIGNATURE_SIZE_SIZE = 4
        private const val HEADER_SIZE = MAGIC_SIZE + VERSION_SIZE + MANIFEST_SIZE_SIZE + SIGNATURE_SIZE_SIZE
    }
    
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
    
    suspend fun parseHeader(file: File): PayloadHeader? = withContext(Dispatchers.IO) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                // Read magic
                val magic = ByteArray(MAGIC_SIZE)
                raf.read(magic)
                if (String(magic) != MAGIC) return@withContext null
                
                // Read version (8 bytes, BIG-ENDIAN)
                val versionBytes = ByteArray(VERSION_SIZE)
                raf.read(versionBytes)
                val version = ByteBuffer.wrap(versionBytes).order(ByteOrder.BIG_ENDIAN).long
                
                // Read manifest size (8 bytes, BIG-ENDIAN)
                val manifestSizeBytes = ByteArray(MANIFEST_SIZE_SIZE)
                raf.read(manifestSizeBytes)
                val manifestSize = ByteBuffer.wrap(manifestSizeBytes).order(ByteOrder.BIG_ENDIAN).long
                
                // Read manifest signature size (4 bytes, BIG-ENDIAN)
                val signatureSizeBytes = ByteArray(SIGNATURE_SIZE_SIZE)
                raf.read(signatureSizeBytes)
                val signatureSize = ByteBuffer.wrap(signatureSizeBytes).order(ByteOrder.BIG_ENDIAN).int.toLong()
                
                PayloadHeader(
                    version = version,
                    manifestSize = manifestSize,
                    manifestSignatureSize = signatureSize
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing header", e)
            null
        }
    }
    
    suspend fun parsePayloadInfo(file: File): PayloadInfo? = withContext(Dispatchers.IO) {
        try {
            val header = parseHeader(file) ?: return@withContext null
            
            // Try to read payload_properties.txt for accurate sizes
            val propertiesFile = File(file.parentFile, "payload_properties.txt")
            val propertiesSizes = if (propertiesFile.exists()) {
                parsePayloadProperties(propertiesFile)
            } else {
                emptyMap()
            }
            
            // Read manifest bytes
            val manifestBytes = RandomAccessFile(file, "r").use { raf ->
                raf.seek(HEADER_SIZE.toLong())
                val bytes = ByteArray(header.manifestSize.toInt())
                raf.read(bytes)
                bytes
            }
            
            // Parse manifest manually
            val dataOffset = HEADER_SIZE + header.manifestSize + header.manifestSignatureSize
            
            // First, try to get block_size from manifest
            var blockSize = 4096L
            var pos = 0
            while (pos < manifestBytes.size && pos < 1000) { // Only check first 1000 bytes for block_size
                try {
                    val (tag, tagSize) = readVarint(manifestBytes, pos)
                    pos += tagSize
                    
                    val fieldNumber = (tag shr 3).toInt()
                    val wireType = (tag and 0x07).toInt()
                    
                    if (fieldNumber == 3 && wireType == 0) {
                        // block_size field
                        val (value, size) = readVarint(manifestBytes, pos)
                        blockSize = value
                        android.util.Log.d("PayloadParserManual", "Found block_size in manifest: $blockSize")
                        break
                    }
                    
                    if (wireType == 0) {
                        val (_, size) = readVarint(manifestBytes, pos)
                        pos += size
                    } else if (wireType == 2) {
                        val (length, lengthSize) = readVarint(manifestBytes, pos)
                        pos += lengthSize
                        pos += length.toInt()
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
            
            val partitions = parseManifest(manifestBytes, dataOffset, blockSize, propertiesSizes)
            
            PayloadInfo(
                header = header.copy(blockSize = blockSize),
                partitions = partitions,
                totalSize = file.length(),
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing payload", e)
            null
        }
    }
    
    private fun parsePayloadProperties(file: File): Map<String, Long> {
        val sizes = mutableMapOf<String, Long>()
        try {
            file.readLines().forEach { line ->
                // Format: FILE_HASH=hash or FILE_SIZE=size or METADATA_HASH=hash or METADATA_SIZE=size
                if (line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    // Extract partition name and size
                    // Example: system_SIZE=3221225472
                    if (key.endsWith("_SIZE")) {
                        val partitionName = key.removeSuffix("_SIZE").lowercase()
                        val size = value.toLongOrNull()
                        if (size != null && size > 0) {
                            sizes[partitionName] = size
                            android.util.Log.d("PayloadParserManual", "Found size from properties: $partitionName = $size")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing payload_properties.txt", e)
        }
        return sizes
    }
    
    private fun parseManifest(data: ByteArray, dataOffset: Long, blockSize: Long, propertiesSizes: Map<String, Long> = emptyMap()): List<PayloadPartition> {
        val partitions = mutableListOf<PayloadPartition>()
        var pos = 0
        
        android.util.Log.d("PayloadParserManual", "parseManifest: data size = ${data.size}, dataOffset=$dataOffset, blockSize=$blockSize")
        
        while (pos < data.size) {
            try {
                // Read tag (field number + wire type)
                val (tag, tagSize) = readVarint(data, pos)
                pos += tagSize
                
                val fieldNumber = (tag shr 3).toInt()
                val wireType = (tag and 0x07).toInt()
                
                if (fieldNumber == 0) break
                
                android.util.Log.d("PayloadParserManual", "Manifest field=$fieldNumber, wire=$wireType, pos=$pos")
                
                when (wireType) {
                    0 -> {
                        // Varint
                        val (value, size) = readVarint(data, pos)
                        pos += size
                        android.util.Log.d("PayloadParserManual", "  Varint value: $value")
                    }
                    2 -> {
                        // Length-delimited
                        val (length, lengthSize) = readVarint(data, pos)
                        pos += lengthSize
                        
                        android.util.Log.d("PayloadParserManual", "  Length-delimited: length=$length")
                        
                        // Field 13 is partitions
                        if (fieldNumber == 13) {
                            android.util.Log.d("PayloadParserManual", "  Found partition field! Parsing...")
                            val partitionData = data.copyOfRange(pos, pos + length.toInt())
                            val partition = parsePartition(partitionData, dataOffset, blockSize, propertiesSizes)
                            if (partition != null) {
                                partitions.add(partition)
                                android.util.Log.d("PayloadParserManual", "  Added partition: ${partition.name}, ops=${partition.operations.size}")
                            }
                        }
                        
                        pos += length.toInt()
                    }
                    else -> {
                        android.util.Log.d("PayloadParserManual", "  Unknown wire type, breaking")
                        break
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PayloadParserManual", "Error parsing manifest at pos $pos", e)
                break
            }
        }
        
        android.util.Log.d("PayloadParserManual", "parseManifest complete: found ${partitions.size} partitions")
        return partitions
    }
    
    private fun parsePartition(data: ByteArray, dataOffset: Long, blockSize: Long = 4096, propertiesSizes: Map<String, Long> = emptyMap()): PayloadPartition? {
        try {
            var name = ""
            var uncompressedSize = 0L
            var hash = ""
            val operations = mutableListOf<InstallOperation>()
            var pos = 0
            
            android.util.Log.d("PayloadParserManual", "parsePartition: data size = ${data.size}, blockSize=$blockSize")
            
            while (pos < data.size) {
                val (tag, tagSize) = readVarint(data, pos)
                pos += tagSize
                
                val fieldNumber = (tag shr 3).toInt()
                val wireType = (tag and 0x07).toInt()
                
                if (fieldNumber == 0) break
                
                when (wireType) {
                    0 -> {
                        val (value, size) = readVarint(data, pos)
                        pos += size
                        android.util.Log.d("PayloadParserManual", "  Field $fieldNumber (varint): $value")
                    }
                    2 -> {
                        val (length, lengthSize) = readVarint(data, pos)
                        pos += lengthSize
                        
                        when (fieldNumber) {
                            1 -> {
                                // partition_name (proto field 1)
                                name = String(data.copyOfRange(pos, pos + length.toInt()))
                                android.util.Log.d("PayloadParserManual", "  Found partition name: $name")
                            }
                            5 -> {
                                // new_partition_signature (proto field 5) — skip
                            }
                            6 -> {
                                // old_partition_info (proto field 6)
                                android.util.Log.d("PayloadParserManual", "  Found old_partition_info field, length=$length")
                                if (uncompressedSize == 0L) {
                                    val infoData = data.copyOfRange(pos, pos + length.toInt())
                                    val (size, h) = parsePartitionInfo(infoData)
                                    if (size > 0) {
                                        uncompressedSize = size
                                        if (hash.isEmpty()) hash = h
                                        android.util.Log.d("PayloadParserManual", "  Using old_partition_info: size=$size")
                                    }
                                }
                            }
                            7 -> {
                                // new_partition_info (proto field 7)
                                android.util.Log.d("PayloadParserManual", "  Found new_partition_info field, length=$length")
                                val infoData = data.copyOfRange(pos, pos + length.toInt())
                                val (size, h) = parsePartitionInfo(infoData)
                                uncompressedSize = size
                                hash = h
                                android.util.Log.d("PayloadParserManual", "  Parsed: size=$size, hash=${h.take(16)}")
                            }
                            8 -> {
                                // operations (proto field 8 — repeated InstallOperation)
                                val opData = data.copyOfRange(pos, pos + length.toInt())
                                val op = parseOperation(opData)
                                if (op != null) {
                                    operations.add(op)
                                }
                            }
                            else -> {
                                // Fields 2-4, 9-17 etc. — skip silently
                            }
                        }
                        
                        pos += length.toInt()
                    }
                    else -> {
                        android.util.Log.d("PayloadParserManual", "  Unknown wire type $wireType for field $fieldNumber")
                        break
                    }
                }
            }
            
            android.util.Log.d("PayloadParserManual", "Partition $name: operations=${operations.size}, uncompressedSize=$uncompressedSize")
            
            // If uncompressedSize is still 0, calculate from dst_extents
            if (uncompressedSize == 0L && operations.isNotEmpty()) {
                uncompressedSize = operations.sumOf { op ->
                    op.dstExtents.sumOf { extent ->
                        extent.numBlocks * blockSize
                    }
                }
                android.util.Log.d("PayloadParserManual", "  Calculated size from extents: $uncompressedSize")
            }
            
            val updatedOperations = operations.map { op ->
                op.copy(dataOffset = dataOffset + op.dataOffset)
            }
            
            // Calculate compressed size and detect compression
            var compressedSize = 0L
            var compressionType = CompressionType.NONE
            var firstOffset = 0L
            
            updatedOperations.forEachIndexed { index, op ->
                compressedSize += op.dataLength
                if (index == 0) {
                    firstOffset = op.dataOffset
                }
                
                when (op.type) {
                    OperationType.REPLACE_XZ -> compressionType = CompressionType.XZ
                    OperationType.REPLACE_BZ -> compressionType = CompressionType.BZIP2
                    else -> {}
                }
            }
            
            // If uncompressed size is still 0, try to estimate from compressed data
            if (uncompressedSize == 0L && compressedSize > 0) {
                // For full OTA without partition info, use compressed size as estimate
                // Typically uncompressed is 2-3x compressed for system partitions
                uncompressedSize = compressedSize * 2
                android.util.Log.d("PayloadParserManual", "  Estimated size from compressed: $uncompressedSize")
            }
            
            // Try to get size from payload_properties.txt first
            if (uncompressedSize == 0L && propertiesSizes.containsKey(name.lowercase())) {
                uncompressedSize = propertiesSizes[name.lowercase()] ?: 0L
                android.util.Log.d("PayloadParserManual", "  Using size from properties for $name: $uncompressedSize")
            }
            
            // If still 0 and this is a known large partition, use reasonable defaults
            if (uncompressedSize == 0L) {
                uncompressedSize = when (name) {
                    "system" -> 3L * 1024 * 1024 * 1024 // 3GB default
                    "vendor" -> 1L * 1024 * 1024 * 1024 // 1GB default
                    "product" -> 1L * 1024 * 1024 * 1024 // 1GB default
                    "system_ext" -> 512L * 1024 * 1024 // 512MB default
                    "odm" -> 256L * 1024 * 1024 // 256MB default
                    "boot", "recovery" -> 128L * 1024 * 1024 // 128MB default
                    "vendor_boot" -> 64L * 1024 * 1024 // 64MB default
                    "dtbo", "vbmeta", "vbmeta_system" -> 8L * 1024 * 1024 // 8MB default
                    else -> 32L * 1024 * 1024 // 32MB default for others
                }
                android.util.Log.d("PayloadParserManual", "  Using default size for $name: $uncompressedSize")
            }
            
            android.util.Log.d("PayloadParserManual", "Final: Partition $name: uncompressed=$uncompressedSize, compressed=$compressedSize, ops=${operations.size}")
            
            return PayloadPartition(
                name = name,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                hash = hash,
                compressionType = compressionType,
                offset = firstOffset,
                operations = updatedOperations
            )
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing partition", e)
            return null
        }
    }
    
    private fun parseOperation(data: ByteArray): InstallOperation? {
        try {
            var opType = 0
            var dataOffset = 0L
            var dataLength = 0L
            val dstExtents = mutableListOf<Extent>()
            var pos = 0
            
            while (pos < data.size) {
                val (tag, tagSize) = readVarint(data, pos)
                pos += tagSize
                
                val fieldNumber = (tag shr 3).toInt()
                val wireType = (tag and 0x07).toInt()
                
                if (fieldNumber == 0) break
                
                when (wireType) {
                    0 -> {
                        val (value, size) = readVarint(data, pos)
                        pos += size
                        
                        when (fieldNumber) {
                            1 -> opType = value.toInt()
                            2 -> dataOffset = value
                            3 -> dataLength = value
                        }
                    }
                    2 -> {
                        val (length, lengthSize) = readVarint(data, pos)
                        pos += lengthSize
                        
                        if (fieldNumber == 6) {
                            // dst_extents
                            val extentData = data.copyOfRange(pos, pos + length.toInt())
                            val extent = parseExtent(extentData)
                            if (extent != null) {
                                dstExtents.add(extent)
                                android.util.Log.d("PayloadParserManual", "      Added extent: start=${extent.startBlock}, num=${extent.numBlocks}")
                            }
                        }
                        
                        pos += length.toInt()
                    }
                    else -> break
                }
            }
            
            android.util.Log.d("PayloadParserManual", "    Operation: type=$opType, dataOffset=$dataOffset, dataLength=$dataLength, extents=${dstExtents.size}")
            
            return InstallOperation(
                type = mapOperationType(opType),
                dataOffset = dataOffset,
                dataLength = dataLength,
                dstExtents = dstExtents
            )
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing operation", e)
            return null
        }
    }
    
    private fun parseExtent(data: ByteArray): Extent? {
        try {
            var startBlock = 0L
            var numBlocks = 0L
            var pos = 0
            
            while (pos < data.size) {
                val (tag, tagSize) = readVarint(data, pos)
                pos += tagSize
                
                val fieldNumber = (tag shr 3).toInt()
                val wireType = (tag and 0x07).toInt()
                
                if (fieldNumber == 0) break
                
                if (wireType == 0) {
                    val (value, size) = readVarint(data, pos)
                    pos += size
                    
                    when (fieldNumber) {
                        1 -> startBlock = value
                        2 -> numBlocks = value
                    }
                }
            }
            
            return Extent(startBlock, numBlocks)
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parsePartitionInfo(data: ByteArray): Pair<Long, String> {
        var partitionSize = 0L
        var hash = ""
        var pos = 0
        
        try {
            android.util.Log.d("PayloadParserManual", "parsePartitionInfo: data size = ${data.size}")
            
            while (pos < data.size) {
                val (tag, tagSize) = readVarint(data, pos)
                pos += tagSize
                
                val fieldNumber = (tag shr 3).toInt()
                val wireType = (tag and 0x07).toInt()
                
                android.util.Log.d("PayloadParserManual", "  field=$fieldNumber, wire=$wireType, pos=$pos")
                
                if (fieldNumber == 0) break
                
                when (wireType) {
                    0 -> {
                        val (value, size) = readVarint(data, pos)
                        pos += size
                        
                        if (fieldNumber == 1) {
                            partitionSize = value
                            android.util.Log.d("PayloadParserManual", "  Found size: $partitionSize")
                        }
                    }
                    2 -> {
                        val (length, lengthSize) = readVarint(data, pos)
                        pos += lengthSize
                        
                        if (fieldNumber == 2) {
                            // hash bytes
                            hash = data.copyOfRange(pos, pos + length.toInt())
                                .joinToString("") { "%02x".format(it) }
                            android.util.Log.d("PayloadParserManual", "  Found hash: ${hash.take(16)}...")
                        }
                        
                        pos += length.toInt()
                    }
                    else -> break
                }
            }
            
            android.util.Log.d("PayloadParserManual", "parsePartitionInfo result: size=$partitionSize, hash=${hash.take(16)}")
        } catch (e: Exception) {
            android.util.Log.e("PayloadParserManual", "Error parsing partition info", e)
        }
        
        return Pair(partitionSize, hash)
    }
    
    private fun mapOperationType(type: Int): OperationType {
        return when (type) {
            0 -> OperationType.REPLACE
            1 -> OperationType.REPLACE_BZ
            8 -> OperationType.REPLACE_XZ
            6 -> OperationType.ZERO
            7 -> OperationType.DISCARD
            4 -> OperationType.SOURCE_COPY
            5 -> OperationType.SOURCE_BSDIFF
            9 -> OperationType.PUFFDIFF
            else -> OperationType.UNKNOWN
        }
    }
    
    private fun readVarint(data: ByteArray, startPos: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var bytesRead = 0
        
        for (i in startPos until minOf(startPos + 10, data.size)) {
            val byte = data[i].toInt() and 0xFF
            bytesRead++
            
            result = result or ((byte and 0x7F).toLong() shl shift)
            
            if (byte and 0x80 == 0) {
                return Pair(result, bytesRead)
            }
            
            shift += 7
        }
        
        throw IllegalStateException("Varint too long")
    }
}
