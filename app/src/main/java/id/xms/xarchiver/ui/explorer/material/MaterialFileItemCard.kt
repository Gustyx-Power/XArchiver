package id.xms.xarchiver.ui.explorer.material

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.xms.xarchiver.R
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.ui.explorer.utils.FileTypeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

private val fileApkIconCache = LruCache<String, Drawable>(64)

/**
 * Format timestamp following clean localized date rules:
 * - If current year: "28 September"
 * - If different year: "31 Desember 2025"
 */
fun formatFileDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestamp
    val itemYear = cal.get(Calendar.YEAR)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthNames = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val month = monthNames.getOrElse(cal.get(Calendar.MONTH)) { "" }
    return if (itemYear == currentYear) {
        "$day $month"
    } else {
        "$day $month $itemYear"
    }
}

/**
 * Material Monet folder icon tinted with MaterialTheme.colorScheme.primary
 * and optional center badges for Documents, Download, Movies, Music, Pictures.
 */
@Composable
fun MaterialFolderIcon(
    folderName: String,
    modifier: Modifier = Modifier,
    iconSize: Int = 44,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val badgeIcon: ImageVector? = remember(folderName) {
        val lower = folderName.lowercase(Locale.ROOT)
        when {
            lower == "documents" || lower == "dokumen" -> Icons.Default.Description
            lower == "download" || lower == "downloads" -> Icons.Default.Download
            lower == "movies" || lower == "video" || lower == "videos" -> Icons.Default.Videocam
            lower == "music" || lower == "musik" || lower == "audio" -> Icons.Default.MusicNote
            lower == "pictures" || lower == "gambar" || lower == "photos" -> Icons.Default.Image
            else -> null
        }
    }

    Box(
        modifier = modifier.size(iconSize.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )

        if (badgeIcon != null) {
            val badgeSize = (iconSize * 0.44f).dp
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                modifier = Modifier
                    .size(badgeSize)
                    .offset(y = (iconSize * 0.08f).dp)
            )
        }
    }
}

/**
 * File icon or thumbnail for non-directory items.
 */
@Composable
fun MaterialFileIcon(
    file: FileItem,
    modifier: Modifier = Modifier,
    iconSize: Int = 44
) {
    val context = LocalContext.current
    val extension = remember(file.name) { file.name.substringAfterLast('.', "").lowercase() }
    val isImage = remember(extension) { FileTypeDetector.isImageExtension(extension) }
    val isVideo = remember(extension) { FileTypeDetector.isVideoExtension(extension) }
    val isApk = remember(extension) { extension == "apk" }

    var apkIconDrawable by remember(file.path) {
        mutableStateOf(if (isApk) fileApkIconCache.get(file.path) else null)
    }

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
                            fileApkIconCache.put(file.path, icon)
                            apkIconDrawable = icon
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier.size(iconSize.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            (isImage || isVideo) -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(file.path))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            isApk && apkIconDrawable != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(apkIconDrawable)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }
            else -> {
                val (vector, color) = when {
                    FileTypeDetector.isArchiveExtension(file.name) -> Icons.Default.FolderZip to MaterialTheme.colorScheme.tertiary
                    extension in listOf("mp3", "m4a", "flac", "wav", "ogg") -> Icons.Default.AudioFile to MaterialTheme.colorScheme.secondary
                    extension in listOf("txt", "log", "json", "xml", "kt", "java", "py", "c", "cpp") -> Icons.AutoMirrored.Filled.Article to MaterialTheme.colorScheme.primary
                    extension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> Icons.Default.Description to MaterialTheme.colorScheme.error
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size((iconSize * 0.75f).dp)
                )
            }
        }
    }
}

/**
 * Material Monet circular selection badge.
 */
@Composable
fun MaterialSelectionBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val outlineColor = MaterialTheme.colorScheme.outline
    if (isSelected) {
        Box(
            modifier = modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = onPrimaryColor,
                modifier = Modifier.size(14.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(BorderStroke(1.5.dp, outlineColor.copy(alpha = 0.6f)), CircleShape)
        )
    }
}

/**
 * Redesigned Tampilan Daftar (List View) row item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialFileListItem(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayItemCount = remember(file) {
        file.itemCount ?: if (file.isDirectory) {
            try { File(file.path).list()?.size ?: 0 } catch (_: Exception) { 0 }
        } else null
    }

    val subtitleText = remember(file, displayItemCount) {
        val dateStr = formatFileDate(file.lastModified)
        if (file.isDirectory) {
            val count = displayItemCount ?: 0
            if (dateStr.isNotEmpty()) "$count item  |  $dateStr" else "$count item"
        } else {
            val sizeStr = file.size.humanReadable()
            if (dateStr.isNotEmpty()) "$sizeStr  |  $dateStr" else sizeStr
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.isDirectory) {
                MaterialFolderIcon(folderName = file.name, iconSize = 44)
            } else {
                MaterialFileIcon(file = file, iconSize = 44)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = primaryTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isSelectionMode) {
                MaterialSelectionBadge(isSelected = isSelected)
            } else if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = secondaryTextColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(
            color = dividerColor,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 76.dp, end = 16.dp)
        )
    }
}

/**
 * 3-column Grid Card with rounded surface container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialFileGridCard(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    cardBgColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayItemCount = remember(file) {
        file.itemCount ?: if (file.isDirectory) {
            try { File(file.path).list()?.size ?: 0 } catch (_: Exception) { 0 }
        } else null
    }

    val subtitleText = remember(file, displayItemCount) {
        if (file.isDirectory) {
            "${displayItemCount ?: 0} item"
        } else {
            file.size.humanReadable()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (file.isDirectory) {
                MaterialFolderIcon(folderName = file.name, iconSize = 56)
            } else {
                MaterialFileIcon(file = file, iconSize = 56)
            }

            if (isSelectionMode) {
                MaterialSelectionBadge(
                    isSelected = isSelected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = file.name,
            color = primaryTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = subtitleText,
            color = secondaryTextColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * 3-column Item without card background (icon directly on screen canvas).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialFileWaterfallItem(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayItemCount = remember(file) {
        file.itemCount ?: if (file.isDirectory) {
            try { File(file.path).list()?.size ?: 0 } catch (_: Exception) { 0 }
        } else null
    }

    val subtitleText = remember(file, displayItemCount) {
        if (file.isDirectory) {
            "${displayItemCount ?: 0} item"
        } else {
            file.size.humanReadable()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            contentAlignment = Alignment.Center
        ) {
            if (file.isDirectory) {
                MaterialFolderIcon(folderName = file.name, iconSize = 58)
            } else {
                MaterialFileIcon(file = file, iconSize = 58)
            }

            if (isSelectionMode) {
                MaterialSelectionBadge(
                    isSelected = isSelected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = file.name,
            color = primaryTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = subtitleText,
            color = secondaryTextColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha * 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {}
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.2f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha * 0.15f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * Empty folder placeholder view.
 */
@Composable
fun MaterialEmptyFolderView(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isNotEmpty())
                stringResource(R.string.explorer_search_no_results, searchQuery)
            else
                stringResource(R.string.explorer_empty_folder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

fun findCommonParent(paths: List<String>): String {
    if (paths.isEmpty()) return ""
    var common = File(paths.first()).parent ?: ""
    for (i in 1 until paths.size) {
        val parent = File(paths[i]).parent ?: ""
        while (!parent.startsWith(common) && common.isNotEmpty()) {
            common = File(common).parent ?: ""
        }
    }
    return common
}

fun formatFileSize(bytes: Long): String = bytes.humanReadable()

fun isArchiveExtension(extension: String): Boolean = FileTypeDetector.isArchiveExtension("dummy.$extension")
