package id.xms.xarchiver.core.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod

/**
 * Encryption types for ZIP archives
 */
enum class EncryptionType(val displayName: String) {
    AES_256("AES-256 (High Security)"),
    ZIP_CRYPTO("ZipCrypto (Legacy Compatibility)")
}

/**
 * Compression levels for archive creation
 */
enum class CompressionLevel(val value: Int, val displayName: String) {
    STORE(Deflater.NO_COMPRESSION, "Store (No compression)"),
    FASTEST(Deflater.BEST_SPEED, "Fastest"),
    FAST(3, "Fast"),
    NORMAL(Deflater.DEFAULT_COMPRESSION, "Normal"),
    GOOD(7, "Good"),
    BEST(Deflater.BEST_COMPRESSION, "Best")
}

/**
 * Archive format types for creation
 */
enum class ArchiveFormat(val extension: String, val displayName: String) {
    ZIP("zip", "ZIP Archive"),
    TAR("tar", "TAR Archive"),
    TAR_GZ("tar.gz", "TAR.GZ (Gzip)"),
    TAR_BZ2("tar.bz2", "TAR.BZ2 (Bzip2)"),
    SEVEN_Z("7z", "7Z Archive"),
    GZ("gz", "Gzip")
}

/**
 * Progress information for archive creation
 */
data class ArchiveCreationProgress(
    val currentFile: String,
    val percentage: Int,
    val filesProcessed: Int,
    val totalFiles: Int,
    val bytesProcessed: Long,
    val totalBytes: Long
)

/**
 * Result of archive creation
 */
sealed class ArchiveResult {
    data class Success(val archivePath: String, val fileCount: Int, val size: Long) : ArchiveResult()
    data class Error(val message: String, val exception: Exception? = null) : ArchiveResult()
}

/**
 * Archive creator with support for multiple formats, compression levels, and password protection
 */
object ArchiveCreator {
    
    /**
     * Create a ZIP archive from files with optional AES-256 / ZipCrypto encryption
     */
    fun createZipArchive(
        outputPath: String,
        files: List<String>,
        basePath: String = "",  // Common parent path to strip from entry names
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        password: String? = null,
        encryptionType: EncryptionType = EncryptionType.AES_256
    ): Flow<ArchiveCreationProgress> = flow {
        val totalFiles = countFiles(files)
        val totalBytes = calculateTotalSize(files)
        var filesProcessed = 0
        var bytesProcessed = 0L
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val effectivePassword = if (password.isNullOrBlank()) null else password
        val fos = FileOutputStream(outputFile)
        val zos = if (effectivePassword != null) {
            ZipOutputStream(fos, effectivePassword.toCharArray())
        } else {
            ZipOutputStream(fos)
        }
        
        zos.use { zipOut ->
            for (filePath in files) {
                val file = File(filePath)
                if (file.exists()) {
                    addToZip(zipOut, file, basePath, compressionLevel, effectivePassword, encryptionType) { name, sizeChunk, isFinished ->
                        bytesProcessed += sizeChunk
                        if (isFinished) {
                            filesProcessed++
                        }
                        emit(ArchiveCreationProgress(
                            currentFile = name,
                            percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                            filesProcessed = filesProcessed,
                            totalFiles = totalFiles,
                            bytesProcessed = bytesProcessed,
                            totalBytes = totalBytes
                        ))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create a TAR archive from files
     */
    fun createTarArchive(
        outputPath: String,
        files: List<String>,
        basePath: String = "",
        compression: ArchiveFormat = ArchiveFormat.TAR
    ): Flow<ArchiveCreationProgress> = flow {
        val totalFiles = countFiles(files)
        val totalBytes = calculateTotalSize(files)
        var filesProcessed = 0
        var bytesProcessed = 0L
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        
        val outputStream = when (compression) {
            ArchiveFormat.TAR_GZ -> GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
            ArchiveFormat.TAR_BZ2 -> BZip2CompressorOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
            else -> BufferedOutputStream(FileOutputStream(outputFile))
        }
        
        TarArchiveOutputStream(outputStream).use { tos ->
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            
            for (filePath in files) {
                val file = File(filePath)
                if (file.exists()) {
                    addToTar(tos, file, basePath) { name, sizeChunk, isFinished ->
                        bytesProcessed += sizeChunk
                        if (isFinished) {
                            filesProcessed++
                        }
                        emit(ArchiveCreationProgress(
                            currentFile = name,
                            percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                            filesProcessed = filesProcessed,
                            totalFiles = totalFiles,
                            bytesProcessed = bytesProcessed,
                            totalBytes = totalBytes
                        ))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create an archive with auto-detected format based on output path extension
     */
    fun createArchive(
        outputPath: String,
        files: List<String>,
        basePath: String = "",
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        password: String? = null,
        encryptionType: EncryptionType = EncryptionType.AES_256
    ): Flow<ArchiveCreationProgress> {
        val extension = outputPath.substringAfterLast('.', "").lowercase()
        
        return when {
            outputPath.endsWith(".tar.gz", ignoreCase = true) || 
            outputPath.endsWith(".tgz", ignoreCase = true) -> {
                createTarArchive(outputPath, files, basePath, ArchiveFormat.TAR_GZ)
            }
            outputPath.endsWith(".tar.bz2", ignoreCase = true) ||
            outputPath.endsWith(".tbz2", ignoreCase = true) -> {
                createTarArchive(outputPath, files, basePath, ArchiveFormat.TAR_BZ2)
            }
            extension == "tar" -> {
                createTarArchive(outputPath, files, basePath, ArchiveFormat.TAR)
            }
            extension == "7z" -> {
                createSevenZArchive(outputPath, files, basePath)
            }
            extension == "gz" -> {
                createGzArchive(outputPath, files, basePath)
            }
            else -> {
                // Default to ZIP
                createZipArchive(outputPath, files, basePath, compressionLevel, password, encryptionType)
            }
        }
    }
    
    // Helper: Add file/directory to ZIP with Zip4j
    private suspend fun addToZip(
        zos: ZipOutputStream,
        file: File,
        basePath: String,
        compressionLevel: CompressionLevel,
        password: String?,
        encryptionType: EncryptionType,
        onProgress: suspend (String, Long, Boolean) -> Unit
    ) {
        val entryName = if (basePath.isNotEmpty()) {
            file.absolutePath.removePrefix(basePath).removePrefix("/").removePrefix("\\")
        } else {
            file.name
        }
        
        if (file.isDirectory) {
            val dirEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
            val params = ZipParameters().apply {
                fileNameInZip = dirEntry
                lastModifiedFileTime = file.lastModified()
                isEncryptFiles = false
            }
            zos.putNextEntry(params)
            zos.closeEntry()
            
            file.listFiles()?.forEach { child ->
                addToZip(zos, child, basePath, compressionLevel, password, encryptionType, onProgress)
            }
        } else {
            val params = ZipParameters().apply {
                fileNameInZip = entryName
                lastModifiedFileTime = file.lastModified()
                compressionMethod = if (compressionLevel == CompressionLevel.STORE) {
                    CompressionMethod.STORE
                } else {
                    CompressionMethod.DEFLATE
                }
                this.compressionLevel = when (compressionLevel) {
                    CompressionLevel.STORE -> net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
                    CompressionLevel.FASTEST -> net.lingala.zip4j.model.enums.CompressionLevel.FASTEST
                    CompressionLevel.FAST -> net.lingala.zip4j.model.enums.CompressionLevel.FAST
                    CompressionLevel.NORMAL -> net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
                    CompressionLevel.GOOD -> net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
                    CompressionLevel.BEST -> net.lingala.zip4j.model.enums.CompressionLevel.MAXIMUM
                }
                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    if (encryptionType == EncryptionType.ZIP_CRYPTO) {
                        this.encryptionMethod = EncryptionMethod.ZIP_STANDARD
                    } else {
                        this.encryptionMethod = EncryptionMethod.AES
                        this.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    }
                }
            }
            
            zos.putNextEntry(params)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var len: Int
                var lastUpdate = System.currentTimeMillis()
                var bytesWritten = 0L
                while (fis.read(buffer).also { len = it } > 0) {
                    zos.write(buffer, 0, len)
                    bytesWritten += len
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100) {
                        onProgress(entryName, bytesWritten, false)
                        bytesWritten = 0L
                        lastUpdate = now
                    }
                }
                if (bytesWritten > 0L) {
                    onProgress(entryName, bytesWritten, false)
                }
            }
            zos.closeEntry()
            onProgress(entryName, 0L, true)
        }
    }
    
    // Helper: Add file/directory to TAR
    private suspend fun addToTar(
        tos: TarArchiveOutputStream,
        file: File,
        basePath: String,
        onProgress: suspend (String, Long, Boolean) -> Unit
    ) {
        val entryName = if (basePath.isNotEmpty()) {
            file.absolutePath.removePrefix(basePath).removePrefix("/").removePrefix("\\")
        } else {
            file.name
        }
        
        if (file.isDirectory) {
            val dirEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
            val tarEntry = TarArchiveEntry(file, dirEntry)
            tos.putArchiveEntry(tarEntry)
            tos.closeArchiveEntry()
            
            file.listFiles()?.forEach { child ->
                addToTar(tos, child, basePath, onProgress)
            }
        } else {
            val tarEntry = TarArchiveEntry(file, entryName)
            tos.putArchiveEntry(tarEntry)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var len: Int
                var lastUpdate = System.currentTimeMillis()
                var bytesWritten = 0L
                while (fis.read(buffer).also { len = it } > 0) {
                    tos.write(buffer, 0, len)
                    bytesWritten += len
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100) {
                        onProgress(entryName, bytesWritten, false)
                        bytesWritten = 0L
                        lastUpdate = now
                    }
                }
                if (bytesWritten > 0L) {
                    onProgress(entryName, bytesWritten, false)
                }
            }
            tos.closeArchiveEntry()
            onProgress(entryName, 0L, true)
        }
    }
    
    fun createGzArchive(
        outputPath: String,
        files: List<String>,
        basePath: String = ""
    ): Flow<ArchiveCreationProgress> = flow {
        if (files.isEmpty()) return@flow
        
        val sourceFile = File(files.first())
        if (sourceFile.isDirectory || files.size > 1) {
            throw IllegalArgumentException("GZ format can only compress a single file, not a directory.")
        }
        
        val totalFiles = 1
        val totalBytes = sourceFile.length()
        var filesProcessed = 0
        var bytesProcessed = 0L
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        
        FileOutputStream(outputFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                GzipCompressorOutputStream(bos).use { gzos ->
                    val entryName = sourceFile.name
                    FileInputStream(sourceFile).use { fis ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        var lastUpdate = System.currentTimeMillis()
                        var bytesWritten = 0L
                        while (fis.read(buffer).also { len = it } > 0) {
                            gzos.write(buffer, 0, len)
                            bytesWritten += len
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 100) {
                                bytesProcessed += bytesWritten
                                emit(ArchiveCreationProgress(
                                    currentFile = entryName,
                                    percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                                    filesProcessed = filesProcessed,
                                    totalFiles = totalFiles,
                                    bytesProcessed = bytesProcessed,
                                    totalBytes = totalBytes
                                ))
                                bytesWritten = 0L
                                lastUpdate = now
                            }
                        }
                        if (bytesWritten > 0L) {
                            bytesProcessed += bytesWritten
                        }
                    }
                    filesProcessed++
                    emit(ArchiveCreationProgress(
                        currentFile = entryName,
                        percentage = 100,
                        filesProcessed = filesProcessed,
                        totalFiles = totalFiles,
                        bytesProcessed = bytesProcessed,
                        totalBytes = totalBytes
                    ))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    fun createSevenZArchive(
        outputPath: String,
        files: List<String>,
        basePath: String = ""
    ): Flow<ArchiveCreationProgress> = flow {
        val totalFiles = countFiles(files)
        val totalBytes = calculateTotalSize(files)
        var filesProcessed = 0
        var bytesProcessed = 0L
        
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        
        SevenZOutputFile(outputFile).use { szos ->
            for (filePath in files) {
                val file = File(filePath)
                if (file.exists()) {
                    addToSevenZ(szos, file, basePath) { name, sizeChunk, isFinished ->
                        bytesProcessed += sizeChunk
                        if (isFinished) {
                            filesProcessed++
                        }
                        emit(ArchiveCreationProgress(
                            currentFile = name,
                            percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                            filesProcessed = filesProcessed,
                            totalFiles = totalFiles,
                            bytesProcessed = bytesProcessed,
                            totalBytes = totalBytes
                        ))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun addToSevenZ(
        szos: SevenZOutputFile,
        file: File,
        basePath: String,
        onProgress: suspend (String, Long, Boolean) -> Unit
    ) {
        val entryName = if (basePath.isNotEmpty()) {
            file.absolutePath.removePrefix(basePath).removePrefix("/").removePrefix("\\")
        } else {
            file.name
        }
        
        if (file.isDirectory) {
            val dirEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
            val entry = szos.createArchiveEntry(file, dirEntry)
            szos.putArchiveEntry(entry)
            szos.closeArchiveEntry()
            
            file.listFiles()?.forEach { child ->
                addToSevenZ(szos, child, basePath, onProgress)
            }
        } else {
            val entry = szos.createArchiveEntry(file, entryName)
            szos.putArchiveEntry(entry)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var len: Int
                var lastUpdate = System.currentTimeMillis()
                var bytesWritten = 0L
                while (fis.read(buffer).also { len = it } > 0) {
                    szos.write(buffer, 0, len)
                    bytesWritten += len
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100) {
                        onProgress(entryName, bytesWritten, false)
                        bytesWritten = 0L
                        lastUpdate = now
                    }
                }
                if (bytesWritten > 0L) {
                    onProgress(entryName, bytesWritten, false)
                }
            }
            szos.closeArchiveEntry()
            onProgress(entryName, 0L, true)
        }
    }
    
    // Helper: Count total files recursively
    private fun countFiles(paths: List<String>): Int {
        var count = 0
        for (path in paths) {
            val file = File(path)
            if (file.isDirectory) {
                count += countFilesInDirectory(file)
            } else {
                count++
            }
        }
        return count
    }
    
    private fun countFilesInDirectory(dir: File): Int {
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFilesInDirectory(file)
            } else {
                count++
            }
        }
        return count
    }
    
    // Helper: Calculate total size
    private fun calculateTotalSize(paths: List<String>): Long {
        var size = 0L
        for (path in paths) {
            val file = File(path)
            if (file.isDirectory) {
                size += calculateDirectorySize(file)
            } else {
                size += file.length()
            }
        }
        return size
    }
    
    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}
