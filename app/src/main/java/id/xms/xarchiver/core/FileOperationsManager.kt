package id.xms.xarchiver.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Operation types for file clipboard
 */
enum class ClipboardOperation {
    COPY,
    CUT
}

/**
 * Progress information for file operations
 */
data class FileOperationProgress(
    val currentFile: String,
    val percentage: Int,
    val bytesProcessed: Long,
    val totalBytes: Long,
    val filesProcessed: Int,
    val totalFiles: Int
)

/**
 * Result of a file operation
 */
sealed class FileOperationResult {
    data class Success(val message: String, val filesAffected: Int) : FileOperationResult()
    data class Error(val message: String, val exception: Exception? = null) : FileOperationResult()
}

/**
 * Manager class for file operations: copy, cut, paste, rename, delete
 */
object FileOperationsManager {
    
    // Clipboard state
    private var clipboardFiles: List<String> = emptyList()
    private var clipboardOperation: ClipboardOperation? = null
    
    /**
     * Check if clipboard has files
     */
    fun hasClipboardContent(): Boolean = clipboardFiles.isNotEmpty()
    
    /**
     * Get clipboard operation type
     */
    fun getClipboardOperation(): ClipboardOperation? = clipboardOperation
    
    /**
     * Get number of files in clipboard
     */
    fun getClipboardCount(): Int = clipboardFiles.size
    
    /**
     * Copy files to clipboard
     */
    fun copyToClipboard(paths: List<String>) {
        clipboardFiles = paths.toList()
        clipboardOperation = ClipboardOperation.COPY
    }
    
    /**
     * Cut files to clipboard
     */
    fun cutToClipboard(paths: List<String>) {
        clipboardFiles = paths.toList()
        clipboardOperation = ClipboardOperation.CUT
    }
    
    /**
     * Clear clipboard
     */
    fun clearClipboard() {
        clipboardFiles = emptyList()
        clipboardOperation = null
    }
    
    /**
     * Paste files from clipboard to destination directory
     */
    fun pasteFiles(
        destinationDir: String
    ): Flow<FileOperationProgress> = flow {
        if (clipboardFiles.isEmpty()) {
            return@flow
        }
        
        val destination = File(destinationDir)
        if (!destination.exists()) {
            destination.mkdirs()
        }
        
        val totalFiles = countTotalFiles(clipboardFiles)
        var filesProcessed = 0
        val totalBytes = calculateTotalSize(clipboardFiles)
        var bytesProcessed = 0L
        
        for (sourcePath in clipboardFiles) {
            val sourceFile = File(sourcePath)
            val destFile = File(destination, sourceFile.name)
            
            if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, destFile) { file, bytes ->
                    bytesProcessed += bytes
                    filesProcessed++
                    emit(FileOperationProgress(
                        currentFile = file.name,
                        percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                        bytesProcessed = bytesProcessed,
                        totalBytes = totalBytes,
                        filesProcessed = filesProcessed,
                        totalFiles = totalFiles
                    ))
                }
            } else {
                copyFile(sourceFile, getUniqueFile(destFile))
                bytesProcessed += sourceFile.length()
                filesProcessed++
                emit(FileOperationProgress(
                    currentFile = sourceFile.name,
                    percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                    bytesProcessed = bytesProcessed,
                    totalBytes = totalBytes,
                    filesProcessed = filesProcessed,
                    totalFiles = totalFiles
                ))
            }
        }
        
        // If this was a CUT operation, delete the source files
        if (clipboardOperation == ClipboardOperation.CUT) {
            for (sourcePath in clipboardFiles) {
                val sourceFile = File(sourcePath)
                deleteRecursively(sourceFile)
            }
            clearClipboard()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Rename a file or directory
     */
    suspend fun renameFile(
        path: String,
        newName: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext FileOperationResult.Error("File not found")
            }
            
            val newFile = File(file.parentFile, newName)
            
            if (newFile.exists()) {
                return@withContext FileOperationResult.Error("A file with that name already exists")
            }
            
            val success = file.renameTo(newFile)
            if (success) {
                FileOperationResult.Success("Renamed successfully", 1)
            } else {
                FileOperationResult.Error("Failed to rename file")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Error: ${e.message}", e)
        }
    }
    
    /**
     * Delete files or directories
     */
    suspend fun deleteFiles(
        paths: List<String>
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            var failedCount = 0
            
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    if (deleteRecursively(file)) {
                        deletedCount++
                    } else {
                        failedCount++
                    }
                }
            }
            
            if (failedCount > 0) {
                FileOperationResult.Error("Deleted $deletedCount files, $failedCount failed")
            } else {
                FileOperationResult.Success("Deleted $deletedCount items", deletedCount)
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Error: ${e.message}", e)
        }
    }
    
    /**
     * Create a new folder
     */
    suspend fun createFolder(
        parentPath: String,
        folderName: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val newFolder = File(parentPath, folderName)
            
            if (newFolder.exists()) {
                return@withContext FileOperationResult.Error("Folder already exists")
            }
            
            val success = newFolder.mkdirs()
            if (success) {
                FileOperationResult.Success("Folder created", 1)
            } else {
                FileOperationResult.Error("Failed to create folder")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Error: ${e.message}", e)
        }
    }
    
    /**
     * Create a new file
     */
    suspend fun createFile(
        parentPath: String,
        fileName: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentPath, fileName)
            
            if (newFile.exists()) {
                return@withContext FileOperationResult.Error("File already exists")
            }
            
            val success = newFile.createNewFile()
            if (success) {
                FileOperationResult.Success("File created", 1)
            } else {
                FileOperationResult.Error("Failed to create file")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Error: ${e.message}", e)
        }
    }
    
    // Helper functions
    
    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
    }
    
    private suspend fun copyDirectory(
        source: File,
        dest: File,
        onProgress: suspend (File, Long) -> Unit
    ) {
        dest.mkdirs()
        
        source.listFiles()?.forEach { file ->
            val destFile = File(dest, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile, onProgress)
            } else {
                copyFile(file, destFile)
                onProgress(file, file.length())
            }
        }
    }
    
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        return file.delete()
    }
    
    private fun countTotalFiles(paths: List<String>): Int {
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
            if (file.isDirectory) {
                size += calculateDirectorySize(file)
            } else {
                size += file.length()
            }
        }
        return size
    }
    
    private fun getUniqueFile(file: File): File {
        if (!file.exists()) return file
        
        val parent = file.parentFile
        val name = file.nameWithoutExtension
        val extension = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
        
        var counter = 1
        var newFile = File(parent, "${name}_$counter$extension")
        while (newFile.exists()) {
            counter++
            newFile = File(parent, "${name}_$counter$extension")
        }
        return newFile
    }
}
