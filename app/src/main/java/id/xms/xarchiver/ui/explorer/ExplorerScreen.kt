package id.xms.xarchiver.ui.explorer

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.*
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.install.ApkInstaller
import id.xms.xarchiver.ui.components.PathNavigationBar
import id.xms.xarchiver.ui.components.PropertiesDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archiveManager = remember { ArchiveManager(context) }
    val selectionManager = remember { SelectionManager() }
    val bookmarksManager = remember { BookmarksManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFabMenu by remember { mutableStateOf(false) }
    
    // Dialogs
    var pendingApk by remember { mutableStateOf<File?>(null) }
    var showPropertiesDialog by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf<FileItem?>(null) }
    
    // Clipboard info
    val hasClipboard = FileOperationsManager.hasClipboardContent()
    val clipboardCount = FileOperationsManager.getClipboardCount()
    val clipboardOp = FileOperationsManager.getClipboardOperation()
    
    // Selection mode
    val isSelecting = selectionManager.isSelecting
    val selectedCount = selectionManager.selectedCount
    
    // Refresh function
    fun refreshFiles() {
        files = FileService.listDirectory(path)
    }
    
    LaunchedEffect(path) {
        isLoading = true
        refreshFiles()
        isLoading = false
        selectionManager.clearSelection()
    }
    
    Scaffold(
        topBar = {
            if (isSelecting) {
                // Selection mode top bar
                TopAppBar(
                    title = { Text("$selectedCount selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectionManager.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            selectionManager.selectAll(files.map { it.path })
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            selectionManager.reverseSelection(files.map { it.path })
                        }) {
                            Icon(Icons.Default.FlipToBack, contentDescription = "Reverse Selection")
                        }
                        if (selectedCount == 1) {
                            IconButton(onClick = {
                                selectionManager.selectSameType(
                                    files.map { it.path },
                                    selectionManager.selectedPaths.first()
                                )
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Select Same Type")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                // Normal navigation bar
                PathNavigationBar(
                    currentPath = path,
                    onNavigate = { newPath ->
                        navController.navigate("explorer/${Uri.encode(newPath)}") {
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.navigateUp() }
                )
            }
        },
        bottomBar = {
            // Bottom action bar when selecting
            AnimatedVisibility(
                visible = isSelecting,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BottomActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            onClick = {
                                FileOperationsManager.copyToClipboard(selectionManager.selectedPaths)
                                selectionManager.clearSelection()
                                scope.launch {
                                    snackbarHostState.showSnackbar("${selectionManager.selectedCount} items copied")
                                }
                            }
                        )
                        BottomActionButton(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            onClick = {
                                FileOperationsManager.cutToClipboard(selectionManager.selectedPaths)
                                selectionManager.clearSelection()
                                scope.launch {
                                    snackbarHostState.showSnackbar("${selectionManager.selectedCount} items cut")
                                }
                            }
                        )
                        BottomActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = {
                                showDeleteDialog = selectionManager.selectedPaths.toList()
                            }
                        )
                        BottomActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                ShareUtils.shareMultipleFiles(context, selectionManager.selectedPaths)
                                selectionManager.clearSelection()
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelecting) {
                Column(horizontalAlignment = Alignment.End) {
                    // FAB Menu Items
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (hasClipboard) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        showFabMenu = false
                                        scope.launch {
                                            FileOperationsManager.pasteFiles(path).collect { progress ->
                                                // Show progress (could add a dialog)
                                            }
                                            refreshFiles()
                                            snackbarHostState.showSnackbar("Paste complete")
                                        }
                                    },
                                    icon = { Icon(Icons.Default.ContentPaste, null) },
                                    text = { Text("Paste ($clipboardCount)") },
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showNewFolderDialog = true
                                },
                                icon = { Icon(Icons.Default.CreateNewFolder, null) },
                                text = { Text("New Folder") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showNewFileDialog = true
                                },
                                icon = { Icon(Icons.Default.NoteAdd, null) },
                                text = { Text("New File") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                    }
                    
                    // Main FAB
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu }
                    ) {
                        Icon(
                            if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Menu"
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                items(6) { FileItemSkeleton() }
            } else if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Empty folder",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = files,
                    key = { _, file -> file.path }
                ) { _, file ->
                    FileItemCard(
                        file = file,
                        isSelected = selectionManager.isSelected(file.path),
                        isSelectionMode = isSelecting,
                        onClick = {
                            if (isSelecting) {
                                selectionManager.toggleSelection(file.path)
                            } else {
                                handleFileClick(
                                    file = file,
                                    navController = navController,
                                    archiveManager = archiveManager,
                                    scope = scope,
                                    onApkClick = { pendingApk = it }
                                )
                            }
                        },
                        onLongClick = {
                            if (!isSelecting) {
                                selectionManager.toggleSelection(file.path)
                            } else {
                                showActionDialog = file
                            }
                        }
                    )
                }
            }
        }
        
        // Dialogs
        showActionDialog?.let { file ->
            FileActionDialog(
                file = file,
                onDismiss = { showActionDialog = null },
                onCopy = {
                    FileOperationsManager.copyToClipboard(listOf(file.path))
                    showActionDialog = null
                    scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                },
                onCut = {
                    FileOperationsManager.cutToClipboard(listOf(file.path))
                    showActionDialog = null
                    scope.launch { snackbarHostState.showSnackbar("Cut to clipboard") }
                },
                onRename = {
                    showRenameDialog = file
                    showActionDialog = null
                },
                onDelete = {
                    showDeleteDialog = listOf(file.path)
                    showActionDialog = null
                },
                onShare = {
                    ShareUtils.shareFile(context, file.path)
                    showActionDialog = null
                },
                onProperties = {
                    showPropertiesDialog = file.path
                    showActionDialog = null
                },
                onBookmark = {
                    scope.launch {
                        val isNowBookmarked = bookmarksManager.toggleBookmark(file.path)
                        snackbarHostState.showSnackbar(
                            if (isNowBookmarked) "Added to bookmarks" else "Removed from bookmarks"
                        )
                    }
                    showActionDialog = null
                },
                onExtract = if (!file.isDirectory && isArchiveExtension(file.name)) {
                    {
                        showActionDialog = null
                        navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                    }
                } else null
            )
        }
        
        showPropertiesDialog?.let { filePath ->
            PropertiesDialog(
                filePath = filePath,
                onDismiss = { showPropertiesDialog = null }
            )
        }
        
        showRenameDialog?.let { file ->
            RenameDialog(
                currentName = file.name,
                onConfirm = { newName ->
                    scope.launch {
                        val result = FileOperationsManager.renameFile(file.path, newName)
                        when (result) {
                            is FileOperationResult.Success -> {
                                refreshFiles()
                                snackbarHostState.showSnackbar("Renamed successfully")
                            }
                            is FileOperationResult.Error -> {
                                snackbarHostState.showSnackbar(result.message)
                            }
                        }
                    }
                    showRenameDialog = null
                },
                onDismiss = { showRenameDialog = null }
            )
        }
        
        showDeleteDialog?.let { paths ->
            DeleteConfirmDialog(
                count = paths.size,
                onConfirm = {
                    scope.launch {
                        val result = FileOperationsManager.deleteFiles(paths)
                        selectionManager.clearSelection()
                        refreshFiles()
                        when (result) {
                            is FileOperationResult.Success -> {
                                snackbarHostState.showSnackbar(result.message)
                            }
                            is FileOperationResult.Error -> {
                                snackbarHostState.showSnackbar(result.message)
                            }
                        }
                    }
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }
        
        if (showNewFolderDialog) {
            NewItemDialog(
                title = "New Folder",
                placeholder = "Folder name",
                onConfirm = { name ->
                    scope.launch {
                        val result = FileOperationsManager.createFolder(path, name)
                        refreshFiles()
                        when (result) {
                            is FileOperationResult.Success -> snackbarHostState.showSnackbar("Folder created")
                            is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                        }
                    }
                    showNewFolderDialog = false
                },
                onDismiss = { showNewFolderDialog = false }
            )
        }
        
        if (showNewFileDialog) {
            NewItemDialog(
                title = "New File",
                placeholder = "filename.txt",
                onConfirm = { name ->
                    scope.launch {
                        val result = FileOperationsManager.createFile(path, name)
                        refreshFiles()
                        when (result) {
                            is FileOperationResult.Success -> snackbarHostState.showSnackbar("File created")
                            is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                        }
                    }
                    showNewFileDialog = false
                },
                onDismiss = { showNewFileDialog = false }
            )
        }
        
        pendingApk?.let { apkFile ->
            ApkInstallDialog(
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
private fun FileItemCard(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileIcon = remember(file.name, file.isDirectory) { getFileIcon(file) }
    val fileColor = remember(file.name, file.isDirectory) { getFileColor(file) }
    val formattedDate = remember(file.lastModified) {
        dateFormatter.format(Date(file.lastModified))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
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
            // Selection checkbox or icon
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(fileColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = fileColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.isDirectory) "Folder" else "${file.size.humanReadable()} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (file.isDirectory) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isSelectionMode) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FileItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    CircleShape
                )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(0.6f).height(16.dp).background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth(0.4f).height(12.dp).background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(4.dp)
                    )
                )
            }
        }
    }
}

// Helper functions
private fun handleFileClick(
    file: FileItem,
    navController: NavController,
    archiveManager: ArchiveManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onApkClick: (File) -> Unit
) {
    val ext = file.name.substringAfterLast('.', "").lowercase()
    
    when {
        file.isDirectory -> {
            navController.navigate("explorer/${Uri.encode(file.path)}")
        }
        file.name.endsWith(".apk", ignoreCase = true) -> {
            onApkClick(File(file.path))
        }
        isArchiveExtension(file.name) -> {
            navController.navigate("archive_explorer/${Uri.encode(file.path)}")
        }
        isImageExtension(ext) -> {
            navController.navigate("image_viewer/${Uri.encode(file.path)}")
        }
        isAudioExtension(ext) -> {
            navController.navigate("audio_player/${Uri.encode(file.path)}")
        }
        isVideoExtension(ext) -> {
            navController.navigate("video_player/${Uri.encode(file.path)}")
        }
        isTextExtension(ext) -> {
            navController.navigate("text_editor/${Uri.encode(file.path)}")
        }
        else -> {
            // Try to detect as archive, otherwise open as text
            scope.launch {
                if (archiveManager.isArchiveFile(file.path)) {
                    navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                } else {
                    // Open as text by default for unknown file types
                    navController.navigate("text_editor/${Uri.encode(file.path)}")
                }
            }
        }
    }
}

private fun isImageExtension(ext: String): Boolean {
    return ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "heic", "heif")
}

private fun isAudioExtension(ext: String): Boolean {
    return ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
}

private fun isVideoExtension(ext: String): Boolean {
    return ext in listOf("mp4", "avi", "mkv", "mov", "wmv", "webm", "flv", "3gp", "ts", "m4v")
}

private fun isTextExtension(ext: String): Boolean {
    return ext in listOf(
        "txt", "md", "log", "json", "xml", "html", "htm", "css", "js", "ts",
        "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs", "go", "rs",
        "php", "rb", "swift", "sh", "bat", "ps1", "yaml", "yml", "toml", "ini",
        "cfg", "conf", "properties", "gradle", "pro", "gitignore", "env"
    )
}

private fun isArchiveExtension(fileName: String): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return ext in listOf("zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk")
}

private fun getFileIcon(file: FileItem): ImageVector {
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

private fun getFileColor(file: FileItem): Color {
    if (file.isDirectory) return Color(0xFF4CAF50)
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "zip", "rar", "7z", "tar", "gz", "tgz", "jar", "aar", "xapk" -> Color(0xFFFF9800)
        "apk" -> Color(0xFF4CAF50)
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

// Dialog Composables
@Composable
private fun FileActionDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onProperties: () -> Unit,
    onBookmark: () -> Unit,
    onExtract: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                DialogAction(Icons.Default.ContentCopy, "Copy", onCopy)
                DialogAction(Icons.Default.ContentCut, "Cut", onCut)
                DialogAction(Icons.Default.Edit, "Rename", onRename)
                DialogAction(Icons.Default.Delete, "Delete", onDelete)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DialogAction(Icons.Default.Share, "Share", onShare)
                DialogAction(Icons.Default.Info, "Properties", onProperties)
                DialogAction(Icons.Default.Bookmark, "Bookmark", onBookmark)
                onExtract?.let {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    DialogAction(Icons.Default.FolderZip, "Open Archive", it)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DialogAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete $count item${if (count > 1) "s" else ""}?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewItemDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ApkInstallDialog(
    apkFile: File,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Android, null, tint = Color(0xFF4CAF50)) },
        title = { Text("Install APK") },
        text = {
            Text("Do you want to install ${apkFile.name}?")
        },
        confirmButton = {
            TextButton(onClick = onInstall) { Text("Install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
