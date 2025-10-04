package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.FileService
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import androidx.compose.ui.platform.LocalContext
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.install.ApkInstaller
import java.util.*
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import id.xms.xarchiver.ui.theme.*

// Create a global date formatter to avoid recreating it
private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archiveManager = remember { ArchiveManager(context) }

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var pendingApk by remember { mutableStateOf<File?>(null) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var renamingFile by remember { mutableStateOf<FileItem?>(null) }
    var deletingFile by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(path) {
        isLoading = true
        files = FileService.listDirectory(path)
        isLoading = false
    }

    // Optimize: Use simple background color instead of gradient
    Scaffold(
        topBar = {
            TopAppBarWithBreadcrumb(
                path = path,
                onCrumbClick = { crumbPath ->
                    navController.navigate("explorer/${Uri.encode(crumbPath)}") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                items(6) { index ->
                    OptimizedFileItemSkeleton()
                }
            } else {
                itemsIndexed(
                    items = files,
                    key = { _, file -> file.path }
                ) { index, file ->
                    OptimizedFileItem(
                        file = file,
                        dateFormatter = dateFormatter,
                        onClick = {
                            if (file.isDirectory) {
                                navController.navigate("explorer/${Uri.encode(file.path)}")
                            } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                pendingApk = File(file.path)
                            } else {
                                // Check if this is an archive file we can open
                                val fileExtension = file.name.substringAfterLast('.', "").lowercase()
                                val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "tgz", "jar", "apk", "aar", "xapk")

                                if (archiveExtensions.contains(fileExtension)) {
                                    navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                } else {
                                    scope.launch {
                                        if (archiveManager.isArchiveFile(file.path)) {
                                            navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                        } else {
                                            // Handle regular files (future file viewer implementation)
                                        }
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            selectedFile = file
                        }
                    )
                }
            }
        }

        // Enhanced Dialog for file/directory actions
        selectedFile?.let { file ->
            EnhancedActionDialog(
                file = file,
                onDismiss = { selectedFile = null },
                onRename = {
                    renamingFile = file
                    renameText = file.name
                    showRenameDialog = true
                    selectedFile = null
                },
                onDelete = {
                    deletingFile = file
                    showDeleteDialog = true
                    selectedFile = null
                },
                onExtract = {
                    selectedFile = null
                    navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                }
            )
        }

        // Enhanced Rename dialog
        if (showRenameDialog && renamingFile != null) {
            EnhancedRenameDialog(
                file = renamingFile!!,
                initialName = renameText,
                onNameChange = { renameText = it },
                onConfirm = {
                    if (renameText.isNotBlank()) {
                        FileService.renameFile(renamingFile!!.path, renameText)
                        files = FileService.listDirectory(path)
                    }
                    showRenameDialog = false
                    renamingFile = null
                },
                onDismiss = {
                    showRenameDialog = false
                    renamingFile = null
                }
            )
        }

        // Enhanced Delete dialog
        if (showDeleteDialog && deletingFile != null) {
            EnhancedDeleteDialog(
                file = deletingFile!!,
                onConfirm = {
                    FileService.deleteFile(deletingFile!!.path)
                    files = FileService.listDirectory(path)
                    showDeleteDialog = false
                    deletingFile = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    deletingFile = null
                }
            )
        }

        // Enhanced APK install dialog
        pendingApk?.let { apkFile ->
            EnhancedApkInstallDialog(
                apkFile = apkFile,
                onInstall = {
                    ApkInstaller.installApk(context, apkFile)
                    pendingApk = null
                },
                onDismiss = { pendingApk = null }
            )
        }
    }
}

@Composable
private fun OptimizedFileItem(
    file: FileItem,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Pre-calculate values to avoid recalculation during scroll
    val fileIcon = remember(file.name, file.isDirectory) { getFileIcon(file) }
    val fileColor = remember(file.name, file.isDirectory) { getFileColor(file) }
    val formattedDate = remember(file.lastModified) {
        dateFormatter.format(Date(file.lastModified))
    }

    // Simple Card without animations for better performance
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
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
            // Simplified icon without complex backgrounds
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = fileColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileIcon,
                    contentDescription = null,
                    tint = fileColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!file.isDirectory) {
                    Text(
                        text = "${file.size.humanReadable()} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Simple chevron without animation
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun OptimizedFileItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

private fun getFileIcon(file: FileItem): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder

    val extension = file.name.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk" -> Icons.Default.Archive
        "apk" -> Icons.Default.Android
        "mp3", "wav", "flac", "aac", "ogg" -> Icons.Default.AudioFile
        "mp4", "avi", "mkv", "mov", "wmv" -> Icons.Default.VideoFile
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Icons.Default.Image
        "pdf" -> Icons.Default.PictureAsPdf
        "txt", "md", "log" -> Icons.Default.Description
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun getFileColor(file: FileItem): Color {
    if (file.isDirectory) return Color(0xFF4CAF50) // Green for folders

    val extension = file.name.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk" -> Color(0xFFFF9800) // Orange for archives
        "apk" -> Color(0xFF4CAF50) // Green for APK
        "mp3", "wav", "flac", "aac", "ogg" -> Color(0xFF9C27B0) // Purple for audio
        "mp4", "avi", "mkv", "mov", "wmv" -> Color(0xFFE91E63) // Pink for video
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Color(0xFF2196F3) // Blue for images
        "pdf" -> Color(0xFFF44336) // Red for PDF
        "txt", "md", "log", "doc", "docx" -> Color(0xFF607D8B) // Blue-grey for documents
        "xls", "xlsx" -> Color(0xFF4CAF50) // Green for spreadsheets
        "ppt", "pptx" -> Color(0xFFFF5722) // Deep orange for presentations
        else -> Color(0xFF757575) // Grey for other files
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithBreadcrumb(
    path: String,
    onCrumbClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Enhanced TopAppBar with gradient background
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced back button
                Surface(
                    onClick = onBack,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Enhanced breadcrumb display
                val cleanPath = getCleanDisplayPath(path)

                // Show simplified path
                if (cleanPath.length > 30) {
                    // For very long paths, show just the last folder
                    val lastFolder = cleanPath.substringAfterLast('/')
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EnhancedBreadcrumbChip(
                            label = lastFolder.ifEmpty { "Root" },
                            isLast = true,
                            onClick = { onCrumbClick(path) }
                        )
                    }
                } else {
                    // Build enhanced crumbs for shorter paths
                    val normalized = path.trimEnd('/')
                    val parts = getDisplayParts(normalized)

                    parts.forEachIndexed { index, (displayName, fullPath) ->
                        EnhancedBreadcrumbChip(
                            label = displayName,
                            isLast = index == parts.size - 1,
                            onClick = { onCrumbClick(fullPath) }
                        )

                        if (index < parts.size - 1) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = Modifier.shadow(
            elevation = 4.dp,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun EnhancedBreadcrumbChip(
    label: String,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        color = if (isLast) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(20.dp),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLast) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper functions for cleaner path display
private fun getCleanDisplayPath(path: String): String {
    return path
        .replace("/storage/emulated/0", "Internal Storage")
        .replace("/sdcard", "Internal Storage")
        .replace("/storage", "")
        .replace("emulated/0", "Internal Storage")
}

private fun getDisplayParts(path: String): List<Pair<String, String>> {
    if (path.isEmpty() || path == "/") {
        return listOf("Root" to "/")
    }

    val parts = path.split("/").filter { it.isNotEmpty() }
    val result = mutableListOf<Pair<String, String>>()
    var currentPath = ""

    for ((index, part) in parts.withIndex()) {
        currentPath = "$currentPath/$part"

        val displayName = when {
            currentPath == "/storage/emulated/0" || currentPath == "/sdcard" -> "Internal Storage"
            currentPath == "/storage" -> "Storage"
            currentPath == "/data" -> "System Data"
            currentPath == "/system" -> "System"
            part == "emulated" -> null // Skip this part
            part == "0" && currentPath.contains("emulated") -> null // Skip this part
            else -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        displayName?.let {
            result.add(it to currentPath)
        }
    }

    return result.ifEmpty { listOf("Root" to "/") }
}

@Composable
private fun EnhancedActionDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExtract: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.scale(scale),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getFileIcon(file),
                    contentDescription = null,
                    tint = getFileColor(file),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (file.isDirectory) "Folder Options" else "File Options",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Choose an action for ${if (file.isDirectory) "folder" else "file"}:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // File name display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                ActionButton(
                    icon = Icons.Default.Edit,
                    text = "Rename",
                    description = "Change the name",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onRename
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActionButton(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    description = "Remove permanently",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onDelete
                )

                // Extract option for archive files
                if (!file.isDirectory) {
                    val fileExtension = file.name.substringAfterLast('.', "").lowercase()
                    val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "tgz", "jar", "apk", "aar", "xapk")

                    if (archiveExtensions.contains(fileExtension)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ActionButton(
                            icon = Icons.Default.Unarchive,
                            text = "Extract Archive",
                            description = "Browse contents",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onExtract
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedRenameDialog(
    file: FileItem,
    initialName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "renameDialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.scale(scale),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Rename ${if (file.isDirectory) "Folder" else "File"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a new name:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = initialName,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Rename", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EnhancedDeleteDialog(
    file: FileItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "deleteDialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.scale(scale),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Delete ${if (file.isDirectory) "Folder" else "File"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column {
                Text(
                    text = if (file.isDirectory) {
                        "Are you sure you want to delete this folder and all its contents?"
                    } else {
                        "Are you sure you want to delete this file?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getFileIcon(file),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (file.isDirectory) "⚠️ This action will permanently delete the folder and all files inside it." else "⚠️ This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EnhancedApkInstallDialog(
    apkFile: File,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "apkDialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.scale(scale),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Install APK",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Do you want to install this application?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = apkFile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Android Package",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Only install applications from trusted sources",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Install", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
