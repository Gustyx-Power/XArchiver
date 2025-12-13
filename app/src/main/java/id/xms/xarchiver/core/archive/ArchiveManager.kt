package id.xms.xarchiver.core.archive

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.ArchiveEntry as ApacheArchiveEntry

/**
 * Enum representing the state of an extraction operation
 */
enum class ExtractionState {
    STARTED,
    EXTRACTING,
    COMPLETED,
    ERROR
}

/**
 * Data class holding extraction progress information
 */
data class ExtractionProgress(
    val state: ExtractionState,
    val currentFile: String,
    val percentage: Int,
    val extractedCount: Int = 0,
    val totalCount: Int = 0
)

/**
 * Manager class for handling archive operations like listing contents and extraction
 */
class ArchiveManager(private val context: Context) {
    
    private val zipAdapter = ZipArchiveAdapter()
    
    /**
     * Archive file extensions supported by this manager
     */
    private val supportedExtensions = listOf(
        "zip", "jar", "apk", "aar", "xapk"
        // TODO: Add more formats like "rar", "7z", "tar", "gz" with additional adapters
    )
    
    /**
     * Check if a file is a supported archive format
     */
    suspend fun isArchiveFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        if (supportedExtensions.contains(extension)) {
            return@withContext true
        }
        
        // Try to detect by magic bytes for unknown extensions
        try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) return@withContext false
            
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                if (fis.read(header) >= 4) {
                    // ZIP magic bytes: PK\x03\x04
                    if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        
        false
    }
    
    /**
     * List contents of an archive file
     */
    fun listArchiveContents(archiveFilePath: String): Flow<ApacheArchiveEntry> = flow {
        val file = File(archiveFilePath)
        if (!file.exists()) return@flow
        
        val extension = archiveFilePath.substringAfterLast('.', "").lowercase()
        
        when (extension) {
            "zip", "jar", "apk", "aar", "xapk" -> {
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        emit(ZipArchiveEntryWrapper(entry))
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            // TODO: Add support for other archive formats
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * List contents of a nested archive (archive within archive)
     */
    fun listNestedArchiveContents(
        archiveFilePath: String,
        nestedArchivePath: String
    ): Flow<ApacheArchiveEntry> = flow {
        // Extract the nested archive to a temp file first
        val tempFile = File(context.cacheDir, "temp_nested_${System.currentTimeMillis()}.zip")
        
        try {
            val file = File(archiveFilePath)
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == nestedArchivePath) {
                        // Found the nested archive, extract to temp
                        FileOutputStream(tempFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            // Now list the contents of the temp file
            if (tempFile.exists()) {
                ZipInputStream(FileInputStream(tempFile)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        emit(ZipArchiveEntryWrapper(entry))
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } finally {
            tempFile.delete()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Extract an archive to the specified output directory
     */
    fun extractArchive(
        archiveFilePath: String,
        outputDir: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(ExtractionState.STARTED, "Preparing...", 0))
        
        try {
            val file = File(archiveFilePath)
            val outputDirectory = File(outputDir)
            
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            
            // First pass: count total entries
            var totalEntries = 0
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    totalEntries++
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            // Second pass: extract with progress
            var extractedCount = 0
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outputFile = File(outputDirectory, entry.name)
                    
                    // Security check: prevent zip slip vulnerability
                    val canonicalOutput = outputDirectory.canonicalPath
                    val canonicalFile = outputFile.canonicalPath
                    if (!canonicalFile.startsWith(canonicalOutput)) {
                        throw SecurityException("Entry is outside of the target directory")
                    }
                    
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    
                    extractedCount++
                    val percentage = if (totalEntries > 0) (extractedCount * 100) / totalEntries else 0
                    
                    emit(ExtractionProgress(
                        state = ExtractionState.EXTRACTING,
                        currentFile = entry.name,
                        percentage = percentage,
                        extractedCount = extractedCount,
                        totalCount = totalEntries
                    ))
                    
                    onProgress(percentage, entry.name)
                    
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            emit(ExtractionProgress(
                state = ExtractionState.COMPLETED,
                currentFile = "Completed",
                percentage = 100,
                extractedCount = extractedCount,
                totalCount = totalEntries
            ))
            
        } catch (e: Exception) {
            emit(ExtractionProgress(
                state = ExtractionState.ERROR,
                currentFile = e.message ?: "Unknown error",
                percentage = 0
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * View/extract a single entry from an archive to a temporary file
     * Returns the path to the extracted file or null on failure
     */
    suspend fun viewArchiveEntry(
        archiveFilePath: String,
        entryPath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(archiveFilePath)
            val tempDir = File(context.cacheDir, "archive_view")
            tempDir.mkdirs()
            
            val outputFile = File(tempDir, File(entryPath).name)
            
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == entryPath && !entry.isDirectory) {
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        return@withContext outputFile.absolutePath
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Wrapper class to adapt java.util.zip.ZipEntry to Apache ArchiveEntry interface
 */
private class ZipArchiveEntryWrapper(
    private val zipEntry: java.util.zip.ZipEntry
) : ApacheArchiveEntry {
    override fun getName(): String = zipEntry.name
    override fun getSize(): Long = zipEntry.size
    override fun isDirectory(): Boolean = zipEntry.isDirectory
    override fun getLastModifiedDate(): java.util.Date? = 
        java.util.Date(zipEntry.time)
}
