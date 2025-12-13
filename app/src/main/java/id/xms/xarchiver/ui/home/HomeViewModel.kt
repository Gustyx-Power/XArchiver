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

    var storages: List<StorageInfo> = StorageUtils.getAllStorage(app)
    
    // Use mutableStateOf for categories so UI updates when counts are loaded
    var categories = mutableStateOf(getInitialCategories())
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
