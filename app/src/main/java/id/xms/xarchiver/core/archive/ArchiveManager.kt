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
import org.apache.commons.compress.PasswordRequiredException
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
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

open class XArchiveEntry(
    private val name: String,
    private val size: Long,
    private val isDir: Boolean,
    private val lastModified: java.util.Date,
    val isEncrypted: Boolean = false
) : ArchiveEntry {
    override fun getName(): String = name
    override fun getSize(): Long = size
    override fun isDirectory(): Boolean = isDir
    override fun getLastModifiedDate(): java.util.Date = lastModified
}

class ArchiveManager(private val context: Context) {

    /**
     * Lists the contents of an archive file
     */
    fun listArchiveContents(
        archiveFilePath: String,
        password: String? = null
    ): Flow<ArchiveEntry> = flow {
        val file = File(archiveFilePath)
        if (!file.exists()) return@flow

        val extension = file.extension.lowercase()
        if (extension == "zip") {
            try {
                val zipFile = if (!password.isNullOrEmpty()) {
                    ZipFile(file, password.toCharArray())
                } else {
                    ZipFile(file)
                }
                val headers = zipFile.fileHeaders
                for (header in headers) {
                    val date = try {
                        java.util.Date(header.lastModifiedTimeEpoch)
                    } catch (e: Exception) {
                        java.util.Date(file.lastModified())
                    }
                    emit(XArchiveEntry(
                        name = header.fileName,
                        size = header.uncompressedSize,
                        isDir = header.isDirectory,
                        lastModified = date,
                        isEncrypted = header.isEncrypted
                    ))
                }
                return@flow
            } catch (e: Exception) {
                e.printStackTrace()
                return@flow
            }
        }

        try {
            val is7zEncrypted = if (extension == "7z") isArchiveEncrypted(archiveFilePath) else false
            val reader = createArchiveReader(file, password)
            reader?.use { archiveReader ->
                var entry = archiveReader.nextEntry()
                while (entry != null) {
                    val entryName = entry.name
                    val isDir = entry.isDirectory
                    val size = entry.size
                    val date = entry.lastModifiedDate ?: java.util.Date(file.lastModified())
                    emit(XArchiveEntry(
                        name = entryName,
                        size = size,
                        isDir = isDir,
                        lastModified = date,
                        isEncrypted = is7zEncrypted
                    ))
                    entry = archiveReader.nextEntry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Lists the contents of a nested archive (archive within archive)
     */
    fun listNestedArchiveContents(
        archiveFilePath: String,
        nestedArchivePath: String
    ): Flow<ArchiveEntry> = flow {
        try {
            val file = File(archiveFilePath)
            if (!file.exists()) return@flow
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts an archive file to the specified directory
     */
    /**
     * Checks if an archive is encrypted / protected with a password
     */
    fun isArchiveEncrypted(archiveFilePath: String): Boolean {
        val file = File(archiveFilePath)
        if (!file.exists()) return false
        val extension = file.extension.lowercase()
        return when {
            extension == "zip" -> {
                try {
                    val zipFile = ZipFile(file)
                    if (zipFile.isEncrypted) return true
                    zipFile.fileHeaders.any { it.isEncrypted }
                } catch (e: Exception) {
                    false
                }
            }
            extension == "7z" -> {
                try {
                    org.apache.commons.compress.archivers.sevenz.SevenZFile.builder().setFile(file).get().use { szf ->
                        var entry = szf.nextEntry
                        while (entry != null) {
                            if (entry.hasStream()) {
                                val testBuf = ByteArray(1)
                                szf.read(testBuf)
                            }
                            entry = szf.nextEntry
                        }
                    }
                    false
                } catch (e: PasswordRequiredException) {
                    true
                } catch (e: Exception) {
                    val msg = e.message?.lowercase() ?: ""
                    msg.contains("password") || msg.contains("encrypt")
                }
            }
            else -> false
        }
    }

    /**
     * Checks if a specific entry in an archive is encrypted
     */
    fun isEntryEncrypted(archiveFilePath: String, entryPath: String): Boolean {
        val file = File(archiveFilePath)
        if (!file.exists()) return false
        val extension = file.extension.lowercase()
        return when {
            extension == "zip" -> {
                try {
                    val zipFile = ZipFile(file)
                    val header = zipFile.getFileHeader(entryPath)
                    header?.isEncrypted ?: isArchiveEncrypted(archiveFilePath)
                } catch (e: Exception) {
                    false
                }
            }
            extension == "7z" -> isArchiveEncrypted(archiveFilePath)
            else -> false
        }
    }

    /**
     * Extracts an archive file to the specified directory with optional password
     */
    fun extractArchive(
        archiveFilePath: String,
        outputDir: String,
        targetEntries: List<String>? = null,
        password: String? = null,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Flow<ExtractionProgress> = flow {
        val file = File(archiveFilePath)
        val archiveSize = file.length()
        emit(ExtractionProgress(0, "Starting extraction...", ExtractionState.STARTED, null, 0L, archiveSize))
        
        val outputDirectory = File(outputDir)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val extension = file.extension.lowercase()
        if (extension == "zip") {
            try {
                val zipFile = ZipFile(file)
                val isEncrypted = zipFile.isEncrypted || zipFile.fileHeaders.any { it.isEncrypted }
                if (isEncrypted && password.isNullOrEmpty()) {
                    emit(ExtractionProgress(0, "Password required", ExtractionState.ERROR, "Password required"))
                    return@flow
                }

                val fis = FileInputStream(file)
                val zis = if (!password.isNullOrEmpty()) {
                    net.lingala.zip4j.io.inputstream.ZipInputStream(fis, password.toCharArray())
                } else {
                    net.lingala.zip4j.io.inputstream.ZipInputStream(fis)
                }

                var processedBytes = 0L
                val fileHeaders = zipFile.fileHeaders
                val entriesToExtract = if (targetEntries != null) {
                    fileHeaders.filter { header ->
                        targetEntries.any { target ->
                            header.fileName == target || header.fileName.startsWith("$target/")
                        }
                    }
                } else {
                    fileHeaders
                }
                val totalUncompressedSize = entriesToExtract.sumOf { it.uncompressedSize }.coerceAtLeast(1L)

                zis.use { stream ->
                    var header = stream.nextEntry
                    while (header != null) {
                        val isTarget = if (targetEntries != null) {
                            targetEntries.any { target ->
                                header!!.fileName == target || header!!.fileName.startsWith("$target/")
                            }
                        } else {
                            true
                        }

                        if (!isTarget) {
                            header = stream.nextEntry
                            continue
                        }

                        val outputFile = File(outputDirectory, header.fileName)
                        if (header.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            val initialPercentage = if (archiveSize > 0) {
                                ((processedBytes * 100) / totalUncompressedSize).toInt().coerceIn(0, 99)
                            } else 0

                            emit(ExtractionProgress(
                                initialPercentage,
                                header.fileName,
                                ExtractionState.EXTRACTING,
                                null,
                                processedBytes,
                                totalUncompressedSize
                            ))

                            FileOutputStream(outputFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var lastUpdate = System.currentTimeMillis()
                                while (stream.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    processedBytes += bytesRead

                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdate > 100) {
                                        val currentPercentage = ((processedBytes * 100) / totalUncompressedSize).toInt().coerceIn(0, 99)
                                        emit(ExtractionProgress(
                                            currentPercentage,
                                            header.fileName,
                                            ExtractionState.EXTRACTING,
                                            null,
                                            processedBytes,
                                            totalUncompressedSize
                                        ))
                                        onProgress(currentPercentage, header.fileName)
                                        lastUpdate = now
                                    }
                                }
                            }
                        }
                        header = stream.nextEntry
                    }
                }
                emit(ExtractionProgress(100, "Extraction completed", ExtractionState.COMPLETED, null, totalUncompressedSize, totalUncompressedSize))
                return@flow
            } catch (e: ZipException) {
                val isWrongPassword = e.type == ZipException.Type.WRONG_PASSWORD ||
                    (e.message?.contains("password", ignoreCase = true) == true)
                val errorMsg = if (isWrongPassword) "Incorrect password" else (e.message ?: "Extraction failed")
                emit(ExtractionProgress(0, errorMsg, ExtractionState.ERROR, errorMsg))
                return@flow
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("password", ignoreCase = true) == true) "Incorrect password" else (e.message ?: "Extraction failed")
                emit(ExtractionProgress(0, errorMsg, ExtractionState.ERROR, errorMsg))
                return@flow
            }
        }

        // Other formats (7z, tar, etc.)
        try {
            var processedBytes = 0L
            createArchiveReader(file, password)?.use { archiveReader ->
                var entry = archiveReader.nextEntry()
                while (entry != null) {
                    if (targetEntries != null) {
                        val isTarget = targetEntries.any { target ->
                            entry!!.name == target || entry!!.name.startsWith("$target/")
                        }
                        if (!isTarget) {
                            entry = archiveReader.nextEntry()
                            continue
                        }
                    }

                    val outputFile = File(outputDirectory, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        val percentage = if (archiveSize > 0) {
                            ((processedBytes * 100) / archiveSize).toInt().coerceIn(0, 99)
                        } else 0

                        emit(ExtractionProgress(
                            percentage,
                            entry.name,
                            ExtractionState.EXTRACTING,
                            null,
                            processedBytes,
                            archiveSize
                        ))

                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var lastUpdate = System.currentTimeMillis()
                            while (archiveReader.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                processedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 100) {
                                    val currentPercentage = if (archiveSize > 0) {
                                        ((processedBytes * 100) / archiveSize).toInt().coerceIn(0, 99)
                                    } else 0
                                    emit(ExtractionProgress(
                                        currentPercentage,
                                        entry.name,
                                        ExtractionState.EXTRACTING,
                                        null,
                                        processedBytes,
                                        archiveSize
                                    ))
                                    onProgress(currentPercentage, entry.name)
                                    lastUpdate = now
                                }
                            }
                        }
                    }
                    entry = archiveReader.nextEntry()
                }
                emit(ExtractionProgress(100, "Extraction completed", ExtractionState.COMPLETED, null, archiveSize, archiveSize))
            }
        } catch (e: PasswordRequiredException) {
            emit(ExtractionProgress(0, "Password required", ExtractionState.ERROR, "Password required"))
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            val errorMsg = if (msg.contains("password") || msg.contains("checksum")) {
                "Incorrect password"
            } else {
                e.message ?: "Unknown error"
            }
            emit(ExtractionProgress(0, errorMsg, ExtractionState.ERROR, errorMsg))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Views a single entry in an archive file with optional password
     */
    suspend fun viewArchiveEntry(
        archiveFilePath: String,
        entryPath: String,
        password: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(archiveFilePath)
            if (!file.exists()) return@withContext null
            val extension = file.extension.lowercase()

            val cacheDir = File(context.cacheDir, "XArchiver_Temp")
            cacheDir.mkdirs()
            // Use original extension for viewers
            val ext = entryPath.substringAfterLast('.', "")
            val fileName = if (ext.isNotEmpty()) {
                "temp_view_${System.currentTimeMillis()}.$ext"
            } else {
                "temp_view_${System.currentTimeMillis()}"
            }
            val outputFile = File(cacheDir, fileName)

            if (extension == "zip") {
                val zipFile = if (!password.isNullOrEmpty()) {
                    ZipFile(file, password.toCharArray())
                } else {
                    ZipFile(file)
                }
                val header = zipFile.getFileHeader(entryPath)
                if (header != null) {
                    zipFile.extractFile(header, cacheDir.absolutePath, fileName)
                    return@withContext outputFile.absolutePath
                }
            }

            val reader = createArchiveReader(file, password)
            reader?.use { archiveReader ->
                var entry = archiveReader.nextEntry()
                while (entry != null) {
                    if (entry.name == entryPath) {
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = archiveReader.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                            }
                        }
                        return@withContext outputFile.absolutePath
                    }
                    entry = archiveReader.nextEntry()
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
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
    
    private fun createArchiveReader(file: File, password: String? = null): ArchiveReader? {
        val extension = file.extension.lowercase()
        return when {
            extension == "zip" -> {
                try {
                    val zipFile = if (!password.isNullOrEmpty()) {
                        ZipFile(file, password.toCharArray())
                    } else {
                        ZipFile(file)
                    }
                    val headers = zipFile.fileHeaders
                    var currentIndex = 0
                    object : ArchiveReader {
                        private var currentStream: java.io.InputStream? = null

                        override fun nextEntry(): ArchiveEntry? {
                            try {
                                currentStream?.close()
                            } catch (e: Exception) {
                                // ignore
                            }
                            currentStream = null
                            if (currentIndex >= headers.size) return null
                            val header = headers[currentIndex++]
                            val date = try {
                                java.util.Date(header.lastModifiedTimeEpoch)
                            } catch (e: Exception) {
                                java.util.Date(file.lastModified())
                            }
                            return XArchiveEntry(
                                name = header.fileName,
                                size = header.uncompressedSize,
                                isDir = header.isDirectory,
                                lastModified = date,
                                isEncrypted = header.isEncrypted
                            )
                        }

                        override fun read(buffer: ByteArray): Int {
                            val activeIndex = currentIndex - 1
                            if (activeIndex < 0 || activeIndex >= headers.size) return -1
                            val header = headers[activeIndex]
                            if (header.isDirectory) return -1
                            if (currentStream == null) {
                                currentStream = zipFile.getInputStream(header)
                            }
                            return currentStream?.read(buffer) ?: -1
                        }

                        override fun close() {
                            try {
                                currentStream?.close()
                            } catch (e: Exception) {
                                // ignore
                            }
                            currentStream = null
                            try {
                                zipFile.close()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                } catch (e: Exception) {
                    val stream = createArchiveInputStream(file) ?: return null
                    object : ArchiveReader {
                        override fun nextEntry(): ArchiveEntry? = stream.nextEntry
                        override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
                        override fun close() = stream.close()
                    }
                }
            }
            extension == "7z" -> {
                val builder = org.apache.commons.compress.archivers.sevenz.SevenZFile.builder().setFile(file)
                if (!password.isNullOrEmpty()) {
                    builder.setPassword(password.toCharArray())
                }
                val sevenZFile = builder.get()
                object : ArchiveReader {
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
