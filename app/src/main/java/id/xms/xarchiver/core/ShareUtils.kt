package id.xms.xarchiver.core

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for sharing files via Android's share intent
 */
object ShareUtils {
    
    /**
     * Share a single file
     */
    fun shareFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val mimeType = getMimeType(file.extension) ?: "*/*"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }
    
    /**
     * Share multiple files
     */
    fun shareMultipleFiles(context: Context, filePaths: List<String>) {
        if (filePaths.isEmpty()) return
        
        if (filePaths.size == 1) {
            shareFile(context, filePaths.first())
            return
        }
        
        val uris = filePaths.mapNotNull { path ->
            val file = File(path)
            if (file.exists() && file.isFile) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else null
        }
        
        if (uris.isEmpty()) return
        
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share ${filePaths.size} files"))
    }
    
    /**
     * Open a file with external app
     */
    fun openFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val mimeType = getMimeType(file.extension) ?: "*/*"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No app to handle this file type, show chooser
            context.startActivity(Intent.createChooser(intent, "Open with"))
        }
    }
    
    private fun getMimeType(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
}
