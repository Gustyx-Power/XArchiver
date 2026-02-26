package id.xms.xarchiver.ui.explorer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.xms.xarchiver.core.BookmarksManager
import id.xms.xarchiver.core.ClipboardOperation
import id.xms.xarchiver.core.FileOperationsManager
import id.xms.xarchiver.core.FileOperationProgress
import id.xms.xarchiver.core.SelectionManager
import id.xms.xarchiver.core.StorageUtils
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.archive.ExtractionProgress
import id.xms.xarchiver.core.storage.FileItem
import id.xms.xarchiver.core.storage.StorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExplorerViewModel(context: Context) : ViewModel() {
    
    private val storageService = StorageService(context)
    val archiveManager = ArchiveManager(context)
    val selectionManager = SelectionManager()
    val bookmarksManager = BookmarksManager(context)
    
    var files by mutableStateOf<List<FileItem>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(true)
        private set
    
    var extractionProgress by mutableStateOf<ExtractionProgress?>(null)
        private set
    
    var multiArchiveExtractList by mutableStateOf<List<String>>(emptyList())
        private set
    
    var currentExtractingIndex by mutableStateOf(0)
        private set
    
    // Clipboard info
    val hasClipboard: Boolean
        get() = FileOperationsManager.hasClipboardContent()
    
    val clipboardCount: Int
        get() = FileOperationsManager.getClipboardCount()
    
    val clipboardOperation: ClipboardOperation?
        get() = FileOperationsManager.getClipboardOperation()
    
    fun loadFiles(path: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val loadedFiles = storageService.listFiles(android.net.Uri.parse("file://$path"))
                files = loadedFiles
            } catch (e: Exception) {
                files = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    fun refreshFiles(path: String) {
        loadFiles(path)
    }
    
    fun deleteFiles(filePaths: List<String>, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    filePaths.forEach { path ->
                        val file = java.io.File(path)
                        if (file.exists()) {
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        }
                    }
                }
                onComplete(true, "Deleted ${filePaths.size} item(s)")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    fun renameFile(oldPath: String, newName: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val oldFile = java.io.File(oldPath)
                    val newFile = java.io.File(oldFile.parent, newName)
                    
                    if (newFile.exists()) {
                        onComplete(false, "File already exists")
                        return@withContext
                    }
                    
                    val success = oldFile.renameTo(newFile)
                    if (success) {
                        onComplete(true, "Renamed successfully")
                    } else {
                        onComplete(false, "Failed to rename")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    fun createFolder(parentPath: String, folderName: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newFolder = java.io.File(parentPath, folderName)
                    if (newFolder.exists()) {
                        onComplete(false, "Folder already exists")
                        return@withContext
                    }
                    
                    val success = newFolder.mkdirs()
                    if (success) {
                        onComplete(true, "Folder created")
                    } else {
                        onComplete(false, "Failed to create folder")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    fun createFile(parentPath: String, fileName: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newFile = java.io.File(parentPath, fileName)
                    if (newFile.exists()) {
                        onComplete(false, "File already exists")
                        return@withContext
                    }
                    
                    val success = newFile.createNewFile()
                    if (success) {
                        onComplete(true, "File created")
                    } else {
                        onComplete(false, "Failed to create file")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    fun pasteFiles(destinationPath: String, onProgress: (FileOperationProgress) -> Unit, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                var lastProgress: FileOperationProgress? = null
                
                FileOperationsManager.pasteFiles(destinationPath).collect { progress ->
                    lastProgress = progress
                    onProgress(progress)
                }
                
                // Check if completed successfully
                val success = lastProgress?.percentage == 100
                val message = if (success) {
                    "Pasted ${lastProgress?.filesProcessed ?: 0} file(s)"
                } else {
                    "Paste failed"
                }
                
                onComplete(success, message)
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
    
    fun updateMultiArchiveExtractList(list: List<String>) {
        multiArchiveExtractList = list
        currentExtractingIndex = 0
    }
    
    fun clearMultiArchiveExtractList() {
        multiArchiveExtractList = emptyList()
        currentExtractingIndex = 0
    }
    
    fun incrementExtractingIndex() {
        currentExtractingIndex++
    }
    
    fun updateExtractionProgress(progress: ExtractionProgress?) {
        extractionProgress = progress
    }
}

class ExplorerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExplorerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
