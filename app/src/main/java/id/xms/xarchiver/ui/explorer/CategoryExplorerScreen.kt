package id.xms.xarchiver.ui.explorer

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class MediaFileItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val mimeType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExplorerScreen(
    categoryName: String,
    navController: NavController
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<MediaFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    // Load files based on category
    LaunchedEffect(categoryName) {
        isLoading = true
        files = withContext(Dispatchers.IO) {
            loadFilesForCategory(context, categoryName)
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${files.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Loading files...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else if (files.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        getCategoryIcon(categoryName),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No $categoryName found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // File list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = files,
                        key = { it.id }
                    ) { file ->
                        MediaFileCard(
                            file = file,
                            categoryName = categoryName,
                            dateFormatter = dateFormatter,
                            onClick = {
                                // Navigate to file explorer at the file's parent directory
                                val parentPath = file.path.substringBeforeLast('/')
                                navController.navigate("explorer/${Uri.encode(parentPath)}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaFileCard(
    file: MediaFileItem,
    categoryName: String,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val iconColor = getCategoryColor(categoryName)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(categoryName),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        file.size.humanReadable(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        dateFormatter.format(Date(file.dateModified * 1000)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun loadFilesForCategory(
    context: android.content.Context,
    categoryName: String
): List<MediaFileItem> {
    val files = mutableListOf<MediaFileItem>()
    
    val (uri, projection, selection, selectionArgs) = when (categoryName.lowercase()) {
        "images" -> {
            Quad(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.MIME_TYPE
                ),
                null,
                null
            )
        }
        "videos" -> {
            Quad(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_MODIFIED,
                    MediaStore.Video.Media.MIME_TYPE
                ),
                null,
                null
            )
        }
        "audio" -> {
            Quad(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.MIME_TYPE
                ),
                null,
                null
            )
        }
        "documents" -> {
            Quad(
                MediaStore.Files.getContentUri("external"),
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE
                ),
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                        "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?",
                arrayOf("application/pdf", "application/msword%", "application/vnd.ms-%", 
                        "application/vnd.openxmlformats%", "text/%")
            )
        }
        "apk" -> {
            Quad(
                MediaStore.Files.getContentUri("external"),
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE
                ),
                "${MediaStore.Files.FileColumns.MIME_TYPE} = ?",
                arrayOf("application/vnd.android.package-archive")
            )
        }
        "archives" -> {
            Quad(
                MediaStore.Files.getContentUri("external"),
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE
                ),
                "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?)",
                arrayOf("application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
                        "application/x-tar", "application/gzip", "application/java-archive")
            )
        }
        else -> return emptyList()
    }
    
    try {
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(projection[0])
            val nameColumn = cursor.getColumnIndexOrThrow(projection[1])
            val pathColumn = cursor.getColumnIndexOrThrow(projection[2])
            val sizeColumn = cursor.getColumnIndexOrThrow(projection[3])
            val dateColumn = cursor.getColumnIndexOrThrow(projection[4])
            val mimeColumn = cursor.getColumnIndexOrThrow(projection[5])
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(pathColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn)
                val mime = cursor.getString(mimeColumn) ?: ""
                
                files.add(MediaFileItem(id, name, path, size, date, mime))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return files
}

private fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "images" -> Icons.Default.Image
        "videos" -> Icons.Default.VideoLibrary
        "audio" -> Icons.Default.MusicNote
        "documents" -> Icons.Default.Description
        "apk" -> Icons.Default.Android
        "archives" -> Icons.Default.FolderZip
        else -> Icons.Default.Folder
    }
}

private fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "images" -> ImageColor
        "videos" -> VideoColor
        "audio" -> AudioColor
        "documents" -> DocumentColor
        "apk" -> AccentGreen
        "archives" -> ZipColor
        else -> AccentOrange
    }
}

// Helper data class for query parameters
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
