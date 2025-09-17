package id.xms.xarchiver.core.storage

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val mimeType: String?,
    val lastModified: Long
)