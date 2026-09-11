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
        if (!destination.exists() && destination.parentFile?.canWrite() == true) {
            destination.mkdirs()
        }
        
        val isPrivileged = id.xms.xarchiver.core.root.RootService.isGranted() || id.xms.xarchiver.core.root.ShizukuService.isGranted()
        val isCut = clipboardOperation == ClipboardOperation.CUT
        
        // Use RootFileService if privileged access is enabled
        if (isPrivileged) {
            val totalFiles = clipboardFiles.size
            emit(FileOperationProgress(
                currentFile = "Processing...",
                percentage = 50,
                bytesProcessed = 0,
                totalBytes = 0,
                filesProcessed = 0,
                totalFiles = totalFiles
            ))
            
            val success = if (isCut) {
                id.xms.xarchiver.core.root.RootFileService.move(clipboardFiles, destinationDir)
            } else {
                id.xms.xarchiver.core.root.RootFileService.copy(clipboardFiles, destinationDir)
            }
            
            if (success) {
                emit(FileOperationProgress(
                    currentFile = "Done",
                    percentage = 100,
                    bytesProcessed = 0,
                    totalBytes = 0,
                    filesProcessed = totalFiles,
                    totalFiles = totalFiles
                ))
                if (isCut) clearClipboard()
            }
            return@flow
        }
        
        // Standard java.io.File implementation
        val totalFiles = countTotalFiles(clipboardFiles)
        var filesProcessed = 0
        val totalBytes = calculateTotalSize(clipboardFiles)
        var bytesProcessed = 0L
        
        for (sourcePath in clipboardFiles) {
            val sourceFile = File(sourcePath)
            val destFile = File(destination, sourceFile.name)
            
            if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, destFile) { file, bytesChunk, isFinished ->
                    bytesProcessed += bytesChunk
                    if (isFinished) {
                        filesProcessed++
                    }
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
                val destF = getUniqueFile(destFile)
                copyFile(sourceFile, destF) { bytesChunk ->
                    bytesProcessed += bytesChunk
                    emit(FileOperationProgress(
                        currentFile = sourceFile.name,
                        percentage = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0,
                        bytesProcessed = bytesProcessed,
                        totalBytes = totalBytes,
                        filesProcessed = filesProcessed,
                        totalFiles = totalFiles
                    ))
                }
                filesProcessed++
            }
        }
        
        // If this was a CUT operation, delete the source files
        if (isCut) {
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
        val success = FileService.renameFile(path, newName)
        if (success) {
            FileOperationResult.Success("Renamed successfully", 1)
        } else {
            FileOperationResult.Error("Failed to rename file")
        }
    }
    
    /**
     * Delete files or directories
     */
    suspend fun deleteFiles(
        paths: List<String>
    ): FileOperationResult = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var failedCount = 0
        
        for (path in paths) {
            if (FileService.deleteFile(path)) {
                deletedCount++
            } else {
                failedCount++
            }
        }
        
        if (failedCount > 0) {
            FileOperationResult.Error("Deleted $deletedCount files, $failedCount failed")
        } else {
            FileOperationResult.Success("Deleted $deletedCount items", deletedCount)
        }
    }
    
    /**
     * Create a new folder
     */
    suspend fun createFolder(
        parentPath: String,
        folderName: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        val newFolder = File(parentPath, folderName)
        
        val isPrivileged = id.xms.xarchiver.core.root.RootService.isGranted() || id.xms.xarchiver.core.root.ShizukuService.isGranted()
        
        if (newFolder.exists()) {
            return@withContext FileOperationResult.Error("Folder already exists")
        }
        
        var success = false
        if (File(parentPath).canWrite()) {
            success = newFolder.mkdirs()
        } else if (isPrivileged) {
            success = id.xms.xarchiver.core.root.RootFileService.createFolder(parentPath, folderName)
        }
        
        if (success) {
            id.xms.xarchiver.core.recent.RecentManager.recordFile(newFolder.absolutePath)
            FileOperationResult.Success("Folder created", 1)
        } else {
            FileOperationResult.Error("Failed to create folder")
        }
    }
    
    /**
     * Create a new file
     */
    suspend fun createFile(
        parentPath: String,
        fileName: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        val newFile = File(parentPath, fileName)
        
        val isPrivileged = id.xms.xarchiver.core.root.RootService.isGranted() || id.xms.xarchiver.core.root.ShizukuService.isGranted()
        
        if (newFile.exists()) {
            return@withContext FileOperationResult.Error("File already exists")
        }
        
        var success = false
        if (File(parentPath).canWrite()) {
            success = newFile.createNewFile()
        } else if (isPrivileged) {
            success = id.xms.xarchiver.core.root.RootFileService.createFile(parentPath, fileName)
        }
        
        if (success) {
            id.xms.xarchiver.core.recent.RecentManager.recordFile(newFile.absolutePath)
            FileOperationResult.Success("File created", 1)
        } else {
            FileOperationResult.Error("Failed to create file")
        }
    }
    
    // Helper functions
    
    private suspend fun copyFile(source: File, dest: File, onProgress: suspend (Long) -> Unit = {}) {
        java.io.FileInputStream(source).use { input ->
            java.io.FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var length: Int
                var lastUpdate = System.currentTimeMillis()
                var bytesWritten = 0L
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                    bytesWritten += length
                    
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100) {
                        onProgress(bytesWritten)
                        bytesWritten = 0L
                        lastUpdate = now
                    }
                }
                if (bytesWritten > 0L) {
                    onProgress(bytesWritten)
                }
            }
        }
    }
    
    private suspend fun copyDirectory(
        source: File,
        dest: File,
        onProgress: suspend (File, Long, Boolean) -> Unit
    ) {
        dest.mkdirs()
        
        source.listFiles()?.forEach { file ->
            val destFile = File(dest, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile, onProgress)
            } else {
                copyFile(file, destFile) { bytesChunk ->
                    onProgress(file, bytesChunk, false)
                }
                onProgress(file, 0L, true)
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
