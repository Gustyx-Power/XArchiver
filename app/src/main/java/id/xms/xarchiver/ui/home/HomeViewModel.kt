package id.xms.xarchiver.ui.home

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.xms.xarchiver.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    var storages = mutableStateOf(StorageUtils.getAllStorage(app))
        private set
    var isRefreshing = mutableStateOf(false)
        private set
    var rootStorageInfo: StorageInfo = StorageUtils.getRootStorageInfo()
    
    // Use mutableStateOf for categories so UI updates when counts are loaded
    var categories = mutableStateOf(getInitialCategories())
        private set
    
    var recentFilesGrouped = mutableStateOf<Map<String, List<FileItem>>>(emptyMap())
        private set
        
    var isLoadingRecent = mutableStateOf(false)
        private set
    
    var shortcuts = listOf(
        Shortcut(
            name = "Bluetooth",
            icon = "bluetooth",
            path = "${Environment.getExternalStorageDirectory().absolutePath}/Bluetooth"
        ),
        Shortcut(
            name = "Downloads",
            icon = "download",
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        ),
        Shortcut(
            name = "WhatsApp",
            icon = "chat",
            path = getWhatsAppPath()
        ),
        Shortcut(
            name = "Telegram",
            icon = "send",
            path = "${Environment.getExternalStorageDirectory().absolutePath}/Telegram"
        )
    )
    
    init {
        // Load actual file counts in background
        viewModelScope.launch {
            loadCategoryCounts()
        }
    }
    
    fun loadRecentFiles() {
        viewModelScope.launch {
            isLoadingRecent.value = true
            val recents = withContext(Dispatchers.IO) { getRecentFiles(getApplication()) }
            recentFilesGrouped.value = recents
            isLoadingRecent.value = false
        }
    }

    private suspend fun getRecentFiles(context: android.content.Context): Map<String, List<FileItem>> = withContext(Dispatchers.IO) {
        val recentFiles = mutableListOf<Pair<String, FileItem>>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"
        
        // Simpler selection: anything with size > 0 and a valid name
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0"
        
        try {
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) else null
                    val date = cursor.getLong(dateCol) * 1000L
                    val data = cursor.getString(dataCol)
                    
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    
                    var bucketName: String? = null
                    if (data != null) {
                        val file = File(data)
                        bucketName = file.parentFile?.name
                    }
                    if (bucketName == null) bucketName = "Recent"
                    
                    val fileItem = FileItem(
                        name = name,
                        path = data ?: "",
                        isDirectory = false,
                        size = size,
                        lastModified = date
                    )
                    // we'll actually set a fake "parentPath" as the bucketName for UI convenience if needed, but FileItem doesn't have parentPath
                    // We'll store pair
                    recentFiles.add(bucketName to fileItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext recentFiles.groupBy({ it.first }, { it.second })
    }

    fun refreshStorage() {
        viewModelScope.launch {
            val newStorages = withContext(Dispatchers.IO) { StorageUtils.getAllStorage(getApplication()) }
            
            val currentPaths = storages.value.map { it.path }
            val newPaths = newStorages.map { it.path }
            
            if (currentPaths != newPaths) {
                isRefreshing.value = true
                kotlinx.coroutines.delay(600) // Brief visual loading
                storages.value = newStorages
                isRefreshing.value = false
            } else {
                storages.value = newStorages
            }
        }
    }
    
    private fun getInitialCategories(): List<Category> {
        return listOf(
            Category(
                name = "Images",
                icon = "image",
                count = 0,
                mimeTypes = listOf("image/*")
            ),
            Category(
                name = "Videos",
                icon = "videocam",
                count = 0,
                mimeTypes = listOf("video/*")
            ),
            Category(
                name = "Audio",
                icon = "music_note",
                count = 0,
                mimeTypes = listOf("audio/*")
            ),
            Category(
                name = "Documents",
                icon = "description",
                count = 0,
                mimeTypes = listOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.ms-excel",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.*",
                    "text/*"
                )
            ),
            Category(
                name = "APK",
                icon = "android",
                count = 0,
                mimeTypes = listOf("application/vnd.android.package-archive")
            ),
            Category(
                name = "Archives",
                icon = "folder_zip",
                count = 0,
                mimeTypes = listOf(
                    "application/zip",
                    "application/x-rar-compressed",
                    "application/x-7z-compressed",
                    "application/x-tar",
                    "application/gzip",
                    "application/java-archive"
                )
            )
        )
    }
    
    private suspend fun loadCategoryCounts() = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val updatedCategories = categories.value.map { category ->
            val count = when (category.name) {
                "Images" -> queryMediaCount(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                "Videos" -> queryMediaCount(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                "Audio" -> queryMediaCount(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                "Documents" -> queryDocumentCount()
                "APK" -> queryApkCount()
                "Archives" -> queryArchiveCount()
                else -> 0
            }
            category.copy(count = count)
        }
        
        withContext(Dispatchers.Main) {
            categories.value = updatedCategories
        }
    }
    
    private fun queryMediaCount(uri: android.net.Uri): Int {
        val context = getApplication<Application>()
        var count = 0
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
    
    private fun queryDocumentCount(): Int {
        val context = getApplication<Application>()
        var count = 0
        try {
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf(
                "application/pdf",
                "application/msword%",
                "application/vnd.ms-%",
                "application/vnd.openxmlformats%",
                "text/%"
            )
            
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
    
    private fun queryApkCount(): Int {
        val context = getApplication<Application>()
        var count = 0
        try {
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/vnd.android.package-archive")
            
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
    
    private fun queryArchiveCount(): Int {
        val context = getApplication<Application>()
        var count = 0
        try {
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?)"
            val selectionArgs = arrayOf(
                "application/zip",
                "application/x-rar-compressed",
                "application/x-7z-compressed",
                "application/x-tar",
                "application/gzip",
                "application/java-archive"
            )
            
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
    
    companion object {
        fun getWhatsAppPath(): String {
            // Android 11+ uses scoped storage path
            val scopedPath = "${Environment.getExternalStorageDirectory().absolutePath}/Android/media/com.whatsapp/WhatsApp"
            val legacyPath = "${Environment.getExternalStorageDirectory().absolutePath}/WhatsApp"
            
            return when {
                File(scopedPath).exists() -> scopedPath
                File(legacyPath).exists() -> legacyPath
                else -> scopedPath // Default to scoped path
            }
        }
    }
}
