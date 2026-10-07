package id.xms.xarchiver.ui.explorer.material

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.xms.xarchiver.R
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

internal val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
private val apkIconCache = LruCache<String, Drawable>(64)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialFileItemCard(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileIcon = remember(file.name, file.isDirectory) { getFileIcon(file) }
    val fileColor = getFileColor(file)
    val formattedSubtitle = remember(file.isDirectory, file.size, file.lastModified) {
        if (file.isDirectory) "Folder" else "${file.size.humanReadable()} • ${dateFormatter.format(Date(file.lastModified))}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox or icon
            if (isSelectionMode) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                }
            } else {
                val isImage = remember(file.name) {
                    isImageExtension(file.name.substringAfterLast('.', "").lowercase())
                }
                val isVideo = remember(file.name) {
                    isVideoExtension(file.name.substringAfterLast('.', "").lowercase())
                }
                val isApk = remember(file.name) {
                    file.name.substringAfterLast('.', "").lowercase() == "apk"
                }

                var apkIconDrawable by remember(file.path) {
                    mutableStateOf(if (isApk) apkIconCache.get(file.path) else null)
                }
                val context = LocalContext.current

                if (isApk && apkIconDrawable == null) {
                    LaunchedEffect(file.path) {
                        withContext(Dispatchers.IO) {
                            try {
                                val pm = context.packageManager
                                val pi = pm.getPackageArchiveInfo(file.path, 0)
                                pi?.applicationInfo?.let { appInfo ->
                                    appInfo.sourceDir = file.path
                                    appInfo.publicSourceDir = file.path
                                    val icon = appInfo.loadIcon(pm)
                                    if (icon != null) {
                                        apkIconCache.put(file.path, icon)
                                        apkIconDrawable = icon
                                    }
                                }
                            } catch (_: Exception) {
                                // Ignore if extracting icon fails
                            }
                        }
                    }
                }

                Surface(
                    color = fileColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isImage || isVideo) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(file.path))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isApk && apkIconDrawable != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(apkIconDrawable)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        } else {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = null,
                                tint = fileColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formattedSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (file.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Shimmer skeleton loading placeholder for file items.
 */
@Composable
fun MaterialFileItemSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.2f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(56.dp)
            ) {}
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(0.7f).height(18.dp).background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.3f),
                        RoundedCornerShape(6.dp)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth(0.4f).height(14.dp).background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.2f),
                        RoundedCornerShape(6.dp)
                    )
                )
            }
        }
    }
}

/**
 * Placeholder view displayed when the folder is empty or search returns no results.
 */
@Composable
fun MaterialEmptyFolderView(
    searchQuery: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (searchQuery.isNotEmpty())
                    stringResource(R.string.explorer_search_no_results, searchQuery)
                else
                    stringResource(R.string.explorer_empty_folder),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "There's nothing here yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun isDocumentExtension(ext: String): Boolean {
    return ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf")
}

fun isImageExtension(ext: String): Boolean {
    return ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "heic", "heif")
}

fun isAudioExtension(ext: String): Boolean {
    return ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
}

fun isVideoExtension(ext: String): Boolean {
    return ext in listOf("mp4", "avi", "mkv", "mov", "wmv", "webm", "flv", "3gp", "ts", "m4v")
}

fun isTextExtension(ext: String): Boolean {
    return ext in listOf(
        "txt", "md", "log", "json", "xml", "html", "htm", "css", "js", "ts",
        "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs", "go", "rs",
        "php", "rb", "swift", "sh", "bat", "ps1", "yaml", "yml", "toml", "ini",
        "cfg", "conf", "properties", "gradle", "pro", "gitignore", "env"
    )
}

fun isArchiveExtension(fileName: String): Boolean {
    val lowerName = fileName.lowercase()
    if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar.bz2") ||
        lowerName.endsWith(".tar.xz") || lowerName.endsWith(".tar.lz")) {
        return true
    }
    val ext = lowerName.substringAfterLast('.', "")
    return ext in listOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "lz", "jar", "aar", "xapk")
}

fun getFileIcon(file: FileItem): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk" -> Icons.Default.Archive
        "apk" -> Icons.Default.Android
        "mp3", "wav", "flac", "aac", "ogg" -> Icons.Default.AudioFile
        "mp4", "avi", "mkv", "mov", "wmv" -> Icons.Default.VideoFile
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Icons.Default.Image
        "pdf" -> Icons.Default.PictureAsPdf
        "txt", "md", "log", "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

@Composable
fun getFileColor(file: FileItem): Color {
    if (file.isDirectory) return MaterialTheme.colorScheme.primary
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk" -> Color(0xFFFF9800)
        "apk" -> MaterialTheme.colorScheme.tertiary
        "mp3", "wav", "flac", "aac", "ogg" -> Color(0xFF9C27B0)
        "mp4", "avi", "mkv", "mov", "wmv" -> Color(0xFFE91E63)
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Color(0xFF2196F3)
        "pdf" -> Color(0xFFF44336)
        "txt", "md", "log", "doc", "docx" -> Color(0xFF607D8B)
        "xls", "xlsx" -> Color(0xFF4CAF50)
        "ppt", "pptx" -> Color(0xFFFF5722)
        else -> Color(0xFF757575)
    }
}

fun findCommonParent(paths: List<String>): String {
    if (paths.isEmpty()) return ""
    if (paths.size == 1) return File(paths.first()).parentFile?.absolutePath ?: ""

    val splitPaths = paths.map { it.split("/", "\\") }
    val minLength = splitPaths.minOf { it.size }

    val commonParts = mutableListOf<String>()
    for (i in 0 until minLength) {
        val part = splitPaths[0][i]
        if (splitPaths.all { it[i] == part }) {
            commonParts.add(part)
        } else {
            break
        }
    }

    return commonParts.joinToString("/")
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
