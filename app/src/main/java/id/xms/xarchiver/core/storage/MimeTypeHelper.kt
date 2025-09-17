package id.xms.xarchiver.core.storage

import android.webkit.MimeTypeMap

object MimeTypeHelper {
    fun getMimeType(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "")
        return if (ext.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        } else null
    }
}
