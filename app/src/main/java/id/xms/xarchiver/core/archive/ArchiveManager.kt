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
    val error: String? = null
)

class ArchiveManager(private val context: Context) {

    /**
     * Lists the contents of an archive file
     */
    fun listArchiveContents(archiveFilePath: String): Flow<ArchiveEntry> = flow {
        val file = File(archiveFilePath)
        val inputStream = createArchiveInputStream(file)
        
        inputStream?.use { archiveStream ->
            var entry = archiveStream.nextEntry
            while (entry != null) {
                emit(entry)
                entry = archiveStream.nextEntry
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
        val inputStream = createArchiveInputStream(file)
        
        inputStream?.use { archiveStream ->
            var entry = archiveStream.nextEntry
            while (entry != null) {
                if (entry.name == nestedArchivePath) {
                    // Found the nested archive, read it
                    val nestedInputStream = createArchiveInputStream(archiveStream)
                    nestedInputStream?.use { nestedStream ->
                        var nestedEntry = nestedStream.nextEntry
                        while (nestedEntry != null) {
                            emit(nestedEntry)
                            nestedEntry = nestedStream.nextEntry
                        }
                    }
                    break
                }
                entry = archiveStream.nextEntry
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
            emit(ExtractionProgress(0, "Starting extraction...", ExtractionState.STARTED))
            
            val file = File(archiveFilePath)
            val outputDirectory = File(outputDir)
            
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            
            val archiveSize = file.length()
            var processedBytes = 0L
            var extractedCount = 0
            
            // Single pass: extract and track progress by bytes processed
            createArchiveInputStream(file)?.use { archiveStream ->
                var entry = archiveStream.nextEntry
                
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
                            ExtractionState.EXTRACTING
                        ))
                        
                        // Extract file and track bytes
                        var bytesWritten = 0L
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (archiveStream.read(buffer).also { bytesRead = it } != -1) {
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
                                        ExtractionState.EXTRACTING
                                    ))
                                    onProgress(currentPercentage, entry.name)
                                }
                            }
                        }
                    }
                    
                    entry = archiveStream.nextEntry
                }
                
                emit(ExtractionProgress(100, "Extraction completed", ExtractionState.COMPLETED))
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
            val inputStream = createArchiveInputStream(file)
            
            inputStream?.use { archiveStream ->
                var entry = archiveStream.nextEntry
                while (entry != null) {
                    if (entry.name == entryPath) {
                        return@withContext archiveStream.bufferedReader().readText()
                    }
                    entry = archiveStream.nextEntry
                }
            }
            null
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

    private fun createArchiveInputStream(file: File): ArchiveInputStream<*>? {
        val bufferedInputStream = BufferedInputStream(FileInputStream(file))
        val extension = file.extension.lowercase()
        
        return when (extension) {
            "zip" -> ZipArchiveInputStream(bufferedInputStream)
            "tar" -> TarArchiveInputStream(bufferedInputStream)
            "gz", "tgz" -> {
                val gzipStream = GzipCompressorInputStream(bufferedInputStream)
                TarArchiveInputStream(gzipStream)
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
