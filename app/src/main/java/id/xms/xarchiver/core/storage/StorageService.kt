package id.xms.xarchiver.core.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageService(private val context: Context) {

    suspend fun listFiles(rootUri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val docFile = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        docFile.listFiles().map {
            FileItem(
                uri = it.uri,
                name = it.name ?: "unknown",
                isDirectory = it.isDirectory,
                size = it.length(),
                mimeType = it.type ?: MimeTypeHelper.getMimeType(it.name ?: ""),
                lastModified = it.lastModified()
            )
        }
    }

    suspend fun createFile(
        parentUri: Uri,
        mimeType: String,
        displayName: String
    ): FileItem? = withContext(Dispatchers.IO) {
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return@withContext null
        val file = parent.createFile(mimeType, displayName) ?: return@withContext null
        return@withContext FileItem(
            uri = file.uri,
            name = file.name ?: displayName,
            isDirectory = false,
            size = file.length(),
            mimeType = file.type,
            lastModified = file.lastModified()
        )
    }
}
