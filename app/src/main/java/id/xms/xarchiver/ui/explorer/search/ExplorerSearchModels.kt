package id.xms.xarchiver.ui.explorer.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.ui.explorer.utils.FileTypeDetector
import java.io.File
import java.util.Locale

import id.xms.xarchiver.R

/**
 * Filter categories for explorer search matching ColorOS File Manager.
 */
enum class ExplorerSearchCategory(
    val labelRes: Int,
    val icon: ImageVector,
    val iconColor: Color
) {
    ALL(R.string.search_category_all, Icons.Default.Apps, Color(0xFF9E9E9E)),
    IMAGE(R.string.search_category_image, Icons.Default.Image, Color(0xFF388AF6)),
    VIDEO(R.string.search_category_video, Icons.Default.Videocam, Color(0xFF6366F1)),
    AUDIO(R.string.search_category_audio, Icons.Default.MusicNote, Color(0xFFF59E0B)),
    DOCUMENT(R.string.search_category_document, Icons.Default.Description, Color(0xFFEF4444)),
    ARCHIVE(R.string.search_category_archive, Icons.Default.FolderZip, Color(0xFFD97706)),
    APK(R.string.search_category_apk, Icons.Default.Android, Color(0xFF10B981));

    fun matches(file: FileItem): Boolean {
        if (this == ALL) return true
        if (file.isDirectory) return false // Category filters specifically match file types
        val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (this) {
            ALL -> true
            IMAGE -> FileTypeDetector.isImageExtension(ext)
            VIDEO -> FileTypeDetector.isVideoExtension(ext)
            AUDIO -> FileTypeDetector.isAudioExtension(ext)
            DOCUMENT -> FileTypeDetector.isDocumentExtension(ext) || FileTypeDetector.isTextExtension(ext)
            ARCHIVE -> FileTypeDetector.isArchiveExtension(file.name)
            APK -> ext in listOf("apk", "xapk", "apks")
        }
    }
}

/**
 * Get human-readable scope title matching ColorOS File Manager:
 * - "/storage/emulated/0" or "/" -> [defaultName]
 * - Any subfolder (e.g. "/storage/emulated/0/Documents") -> "Documents"
 */
fun getSearchScopeDisplayName(path: String, defaultName: String): String {
    val clean = path.trimEnd('/')
    val baseStorage = "/storage/emulated/0"
    if (clean.isEmpty() || clean == "/" || clean == "/sdcard" || clean == baseStorage) {
        return defaultName
    }
    return File(clean).name.ifEmpty { defaultName }
}

/**
 * Highlight matching query substrings within text using Compose AnnotatedString.
 */
fun buildHighlightedText(
    text: String,
    query: String,
    normalColor: Color,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text, spanStyles = listOf(AnnotatedString.Range(SpanStyle(color = normalColor), 0, text.length)))
    }

    val builder = AnnotatedString.Builder()
    var currentIndex = 0
    val lowerText = text.lowercase(Locale.ROOT)
    val lowerQuery = query.lowercase(Locale.ROOT)

    while (currentIndex < text.length) {
        val index = lowerText.indexOf(lowerQuery, currentIndex)
        if (index == -1) {
            builder.append(
                AnnotatedString(
                    text.substring(currentIndex),
                    spanStyle = SpanStyle(color = normalColor)
                )
            )
            break
        }
        if (index > currentIndex) {
            builder.append(
                AnnotatedString(
                    text.substring(currentIndex, index),
                    spanStyle = SpanStyle(color = normalColor)
                )
            )
        }
        val matchEnd = index + query.length
        builder.append(
            AnnotatedString(
                text.substring(index, matchEnd),
                spanStyle = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)
            )
        )
        currentIndex = matchEnd
    }
    return builder.toAnnotatedString()
}
