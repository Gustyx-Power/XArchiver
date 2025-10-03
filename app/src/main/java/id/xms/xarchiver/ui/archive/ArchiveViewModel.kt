package id.xms.xarchiver.ui.archive

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.archive.ExtractionProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.ArchiveEntry

class ArchiveViewModel(context: Context) : ViewModel() {

    private val archiveManager = ArchiveManager(context)

    val archiveEntries = mutableStateOf<List<ArchiveEntry>>(emptyList())
    val isLoading = mutableStateOf(false)

    /**
     * Loads the contents of an archive file
     */
    fun loadArchiveContents(archiveFilePath: String) {
        isLoading.value = true
        viewModelScope.launch {
            val entries = mutableListOf<ArchiveEntry>()
            archiveManager.listArchiveContents(archiveFilePath).collect { entry ->
                entries.add(entry)
            }
            archiveEntries.value = entries
            isLoading.value = false
        }
    }

    /**
     * Loads the contents of a nested archive (archive within archive)
     */
    fun loadNestedArchiveContents(archiveFilePath: String, nestedArchivePath: String) {
        isLoading.value = true
        viewModelScope.launch {
            val entries = mutableListOf<ArchiveEntry>()
            archiveManager.listNestedArchiveContents(archiveFilePath, nestedArchivePath).collect { entry ->
                entries.add(entry)
            }
            archiveEntries.value = entries
            isLoading.value = false
        }
    }

    /**
     * Extracts an archive file to the specified directory
     */
    fun extractArchive(
        archiveFilePath: String,
        outputDir: String,
        onProgress: (ExtractionProgress) -> Unit = {}
    ): Flow<ExtractionProgress> {
        return archiveManager.extractArchive(archiveFilePath, outputDir) { progress, file ->
            // This callback is for the internal progress tracking
            // Don't call onProgress here as it conflicts with the Flow
        }
    }

    /**
     * Views a single entry in an archive file
     */
    suspend fun viewArchiveEntry(
        archiveFilePath: String,
        entryPath: String
    ): String? {
        return archiveManager.viewArchiveEntry(archiveFilePath, entryPath)
    }

    /**
     * Checks if a file is an archive
     */
    suspend fun isArchiveFile(filePath: String): Boolean {
        return archiveManager.isArchiveFile(filePath)
    }
}

/**
 * Factory for creating ArchiveViewModel instances with the required context parameter
 */
class ArchiveViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchiveViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
