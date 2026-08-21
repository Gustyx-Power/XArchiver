package id.xms.xarchiver.core.archive

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ExtractionState {
    IDLE,
    STARTED,
    EXTRACTING,
    COMPLETED,
    ERROR
}

data class ExtractionProgress(
    val percentage: Int,
    val currentFile: String,
    val state: ExtractionState,
    val error: String? = null,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0
)

class ArchiveManager(private val context: Context) {

    /**
     * Lists the contents of an archive file
     */
    fun listArchiveContents(archiveFilePath: String): Flow<ArchiveEntry> = flow {
        val file = File(archiveFilePath)
        val reader = createArchiveReader(file)
        
        reader?.use { archiveReader ->
            var entry = archiveReader.nextEntry()
            while (entry != null) {
                emit(entry)
                entry = archiveReader.nextEntry()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Lists the contents of a nested archive (archive within archive)
     */
    fun listNestedArchiveContents(
        archiveFilePath: String,
        nestedArchivePath: String
    ): Flow<ArchiveEntry> = flow {
        val file = File(archiveFilePath)
        val reader = createArchiveReader(file)
        
        reader?.use { archiveReader ->
            var entry = archiveReader.nextEntry()
            while (entry != null) {
                if (entry.name == nestedArchivePath) {
                    // Found the nested archive, read it
                    val nestedStream = ArchiveReaderInputStream(archiveReader)
                    val nestedReader = createArchiveReader(nestedStream, nestedArchivePath)
                    nestedReader?.use { nr ->
                        var nestedEntry = nr.nextEntry()
                        while (nestedEntry != null) {
                            emit(nestedEntry)
                            nestedEntry = nr.nextEntry()
                        }
                    }
                    break
                }
                entry = archiveReader.nextEntry()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts an archive file to the specified directory
     */
    fun extractArchive(
        archiveFilePath: String,
        outputDir: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Flow<ExtractionProgress> = flow {
        try {
            val file = File(archiveFilePath)
            val archiveSize = file.length()
            emit(ExtractionProgress(0, "Starting extraction...", ExtractionState.STARTED, null, 0L, archiveSize))
            
            val outputDirectory = File(outputDir)
            
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            var processedBytes = 0L
            var extractedCount = 0
            
            // Single pass: extract and track progress by bytes processed
            createArchiveReader(file)?.use { archiveReader ->
                var entry = archiveReader.nextEntry()
                
                while (entry != null) {
                    val outputFile = File(outputDirectory, entry.name)
                    
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        
                        extractedCount++
                        
                        // Emit progress before extraction
                        val percentage = if (archiveSize > 0) {
                            ((processedBytes * 100) / archiveSize).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        
                        emit(ExtractionProgress(
                            percentage,
                            entry.name,
                            ExtractionState.EXTRACTING,
                            null,
                            processedBytes,
                            archiveSize
                        ))
                        
                        // Extract file and track bytes
                        var bytesWritten = 0L
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (archiveReader.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesWritten += bytesRead
                                processedBytes += bytesRead
                                
                                // Emit progress periodically (every 1MB)
                                if (bytesWritten % (1024 * 1024) < 8192) {
                                    val currentPercentage = if (archiveSize > 0) {
                                        ((processedBytes * 100) / archiveSize).toInt().coerceIn(0, 99)
                                    } else {
                                        0
                                    }
                                    emit(ExtractionProgress(
                                        currentPercentage,
                                        entry.name,
                                        ExtractionState.EXTRACTING,
                                        null,
                                        processedBytes,
                                        archiveSize
                                    ))
                                    onProgress(currentPercentage, entry.name)
                                }
                            }
                        }
                    }
                    
                    entry = archiveReader.nextEntry()
                }
                
                emit(ExtractionProgress(100, "Extraction completed", ExtractionState.COMPLETED, null, archiveSize, archiveSize))
            }
        } catch (e: Exception) {
            emit(ExtractionProgress(
                0,
                "Error",
                ExtractionState.ERROR,
                e.message ?: "Unknown error"
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Views a single entry in an archive file
     */
    suspend fun viewArchiveEntry(
        archiveFilePath: String,
        entryPath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(archiveFilePath)
            val reader = createArchiveReader(file)
            
            reader?.use { archiveReader ->
                var entry = archiveReader.nextEntry()
                while (entry != null) {
                    if (entry.name == entryPath) {
                        // Check entry size before reading
                        val maxSize = 10 * 1024 * 1024 // 10MB limit
                        if (entry.size > maxSize) {
                            throw IllegalStateException("Entry too large to view as text")
                        }
                        
                        // Read with size limit
                        val stringBuilder = java.lang.StringBuilder()
                        var totalRead = 0
                        val buffer = ByteArray(8192)
                        
                        while (true) {
                            val read = archiveReader.read(buffer)
                            if (read == -1) break
                            
                            totalRead += read
                            if (totalRead > maxSize) {
                                throw IllegalStateException("Entry too large to view as text")
                            }
                            
                            stringBuilder.append(String(buffer, 0, read))
                        }
                        
                        return@withContext stringBuilder.toString()
                    }
                    entry = archiveReader.nextEntry()
                }
            }
            null
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a file is an archive
     */
    suspend fun isArchiveFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val extension = file.extension.lowercase()
        extension in listOf("zip", "tar", "gz", "tgz", "7z", "rar")
    }

    private interface ArchiveReader : java.io.Closeable {
        fun nextEntry(): ArchiveEntry?
        fun read(buffer: ByteArray): Int
    }
    
    private class ArchiveReaderInputStream(private val reader: ArchiveReader) : java.io.InputStream() {
        override fun read(): Int {
            val b = ByteArray(1)
            val read = reader.read(b)
            return if (read == -1) -1 else b[0].toInt() and 0xFF
        }
        
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (off == 0 && len == b.size) {
                return reader.read(b)
            }
            val temp = ByteArray(len)
            val read = reader.read(temp)
            if (read != -1) {
                System.arraycopy(temp, 0, b, off, read)
            }
            return read
        }
    }
    
    private fun createArchiveReader(file: File): ArchiveReader? {
        val extension = file.extension.lowercase()
        return when {
            extension == "7z" -> {
                object : ArchiveReader {
                    val sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(file)
                    override fun nextEntry(): ArchiveEntry? = sevenZFile.nextEntry
                    override fun read(buffer: ByteArray): Int = sevenZFile.read(buffer)
                    override fun close() = sevenZFile.close()
                }
            }
            extension == "gz" && !file.name.lowercase().endsWith(".tar.gz") -> {
                object : ArchiveReader {
                    val stream = GzipCompressorInputStream(BufferedInputStream(FileInputStream(file)))
                    var readEntry = false
                    override fun nextEntry(): ArchiveEntry? {
                        if (!readEntry) {
                            readEntry = true
                            return object : ArchiveEntry {
                                override fun getName() = file.nameWithoutExtension
                                override fun getSize() = -1L
                                override fun isDirectory() = false
                                override fun getLastModifiedDate() = java.util.Date(file.lastModified())
                            }
                        }
                        return null
                    }
                    override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
                    override fun close() = stream.close()
                }
            }
            else -> {
                val stream = createArchiveInputStream(file) ?: return null
                object : ArchiveReader {
                    override fun nextEntry(): ArchiveEntry? = stream.nextEntry
                    override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
                    override fun close() = stream.close()
                }
            }
        }
    }
    
    private fun createArchiveReader(inputStream: java.io.InputStream, name: String): ArchiveReader? {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when {
            extension == "7z" -> {
                null // Nested 7z not supported via sequential stream
            }
            extension == "gz" && !name.lowercase().endsWith(".tar.gz") -> {
                object : ArchiveReader {
                    val stream = GzipCompressorInputStream(inputStream)
                    var readEntry = false
                    override fun nextEntry(): ArchiveEntry? {
                        if (!readEntry) {
                            readEntry = true
                            return object : ArchiveEntry {
                                override fun getName() = name.removeSuffix(".gz")
                                override fun getSize() = -1L
                                override fun isDirectory() = false
                                override fun getLastModifiedDate() = java.util.Date()
                            }
                        }
                        return null
                    }
                    override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
                    override fun close() = stream.close()
                }
            }
            else -> {
                val stream = createArchiveInputStream(inputStream) ?: return null
                object : ArchiveReader {
                    override fun nextEntry(): ArchiveEntry? = stream.nextEntry
                    override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
                    override fun close() = stream.close()
                }
            }
        }
    }

    private fun createArchiveInputStream(file: File): ArchiveInputStream<*>? {
        val bufferedInputStream = BufferedInputStream(FileInputStream(file))
        val extension = file.extension.lowercase()
        
        return when (extension) {
            "zip" -> ZipArchiveInputStream(bufferedInputStream)
            "tar" -> TarArchiveInputStream(bufferedInputStream)
            "tgz" -> {
                val gzipStream = GzipCompressorInputStream(bufferedInputStream)
                TarArchiveInputStream(gzipStream)
            }
            "gz" -> {
                if (file.name.lowercase().endsWith(".tar.gz")) {
                    val gzipStream = GzipCompressorInputStream(bufferedInputStream)
                    TarArchiveInputStream(gzipStream)
                } else {
                    null // Handled by ArchiveReader
                }
            }
            else -> null
        }
    }

    private fun createArchiveInputStream(inputStream: java.io.InputStream): ArchiveInputStream<*>? {
        return try {
            ZipArchiveInputStream(inputStream)
        } catch (e: Exception) {
            try {
                TarArchiveInputStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }
    }
}
