package id.xms.xarchiver.ui.explorer

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.*
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.archive.ExtractionProgress
import id.xms.xarchiver.core.archive.ExtractionState
import id.xms.xarchiver.core.archive.ArchiveCreator
import id.xms.xarchiver.core.archive.ArchiveCreationProgress
import id.xms.xarchiver.core.archive.ArchiveFormat
import id.xms.xarchiver.core.archive.CompressionLevel
import id.xms.xarchiver.core.install.ApkInstaller
import id.xms.xarchiver.ui.archive.CreateArchiveDialog
import id.xms.xarchiver.ui.components.PathNavigationBar
import id.xms.xarchiver.ui.components.PropertiesDialog
import androidx.compose.ui.res.stringResource
import id.xms.xarchiver.R
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

internal val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archiveManager = remember { ArchiveManager(context) }
    val selectionManager = remember { SelectionManager() }
    val bookmarksManager = remember { BookmarksManager(context) }
    val snackbarHostState = id.xms.xarchiver.ui.components.LocalNotificationHost.current
    
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
    var showSelectionBottomSheet by remember { mutableStateOf(false) }
    var showCreateArchiveDialog by remember { mutableStateOf(false) }
    var showQuickExtractDialog by remember { mutableStateOf<FileItem?>(null) }
    var showCustomPathDialogFor by remember { mutableStateOf<FileItem?>(null) }
    var pendingFileOperation by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    val downloadsPath = remember { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath }
    var customPath by remember { mutableStateOf(downloadsPath) }
    var extractionProgress by remember { mutableStateOf<ExtractionProgress?>(null) }
    var archiveCreationProgress by remember { mutableStateOf<ArchiveCreationProgress?>(null) }
    var fileOperationProgress by remember { mutableStateOf<FileOperationProgress?>(null) }
    var multiArchiveExtractList by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentExtractingIndex by remember { mutableStateOf(0) }
    
    // Clipboard info
    val hasClipboard = FileOperationsManager.hasClipboardContent()
    val clipboardCount = FileOperationsManager.getClipboardCount()
    val clipboardOp = FileOperationsManager.getClipboardOperation()
    
    // Selection mode
    val isSelecting = selectionManager.isSelecting
    val selectedCount = selectionManager.selectedCount
    
    // Search functionality
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filtered files based on search
    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isEmpty()) {
            files
        } else {
            files.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    // Scroll state - remember per-path to restore position on back navigation
    val listState = rememberLazyListState()
    
    // Refresh function
    fun refreshFiles() {
        scope.launch {
            isLoading = true
            files = FileService.listDirectory(path)
            isLoading = false
        }
    }
    
    LaunchedEffect(path) {
        refreshFiles()
        selectionManager.clearSelection()
    }
    
    // Handle batch extraction of multiple archives
    LaunchedEffect(multiArchiveExtractList, currentExtractingIndex) {
        if (multiArchiveExtractList.isNotEmpty() && currentExtractingIndex < multiArchiveExtractList.size) {
            val archivePath = multiArchiveExtractList[currentExtractingIndex]
            val archiveFile = java.io.File(archivePath)
            val outputDir = path + "/" + archiveFile.nameWithoutExtension
            
            extractionProgress = ExtractionProgress(
                0, "Extracting ${archiveFile.name} (${currentExtractingIndex + 1}/${multiArchiveExtractList.size})...", ExtractionState.STARTED
            )
            
            try {
                archiveManager.extractArchive(archivePath, outputDir).collect { progress ->
                    extractionProgress = ExtractionProgress(
                        progress.percentage,
                        "${archiveFile.name}: ${progress.currentFile}",
                        progress.state
                    )
                    
                    if (progress.state == ExtractionState.COMPLETED) {
                        if (currentExtractingIndex + 1 < multiArchiveExtractList.size) {
                            currentExtractingIndex++
                        } else {
                            // All done
                            extractionProgress = null
                            multiArchiveExtractList = emptyList()
                            refreshFiles()
                            snackbarHostState.showSnackbar("Extracted ${multiArchiveExtractList.size} archives")
                        }
                    } else if (progress.state == ExtractionState.ERROR) {
                        snackbarHostState.showSnackbar("Error extracting ${archiveFile.name}")
                        if (currentExtractingIndex + 1 < multiArchiveExtractList.size) {
                            currentExtractingIndex++
                        } else {
                            extractionProgress = null
                            multiArchiveExtractList = emptyList()
                            refreshFiles()
                        }
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
                extractionProgress = null
                multiArchiveExtractList = emptyList()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1500f
                )
            )
    ) {
        Scaffold(
            topBar = {
            if (isSelecting) {
                // Selection mode top bar
                Surface(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectionManager.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel_selection))
                        }
                        Text(
                            stringResource(R.string.explorer_selected_count, selectedCount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { selectionManager.selectAll(files.map { it.path }) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.action_select_all))
                        }
                        IconButton(onClick = { selectionManager.reverseSelection(files.map { it.path }) }) {
                            Icon(Icons.Default.FlipToBack, contentDescription = stringResource(R.string.action_reverse_selection))
                        }
                        if (selectedCount == 1) {
                            IconButton(onClick = {
                                selectionManager.selectSameType(
                                    files.map { it.path },
                                    selectionManager.selectedPaths.first()
                                )
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.action_select_same_type))
                            }
                        }
                    }
                }
            } else if (isSearching) {
                // Search bar mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close_search))
                        }
                        
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.explorer_search_files),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                }
            } else {
                // Normal navigation bar with search button
                Column {
                    val isPrivileged = id.xms.xarchiver.core.root.RootService.isGranted() || id.xms.xarchiver.core.root.ShizukuService.isGranted()
                    PathNavigationBar(
                        currentPath = path,
                        minPath = if (isPrivileged) "/" else "/storage/emulated/0",
                        onNavigate = { newPath ->
                            navController.navigate("explorer/${Uri.encode(newPath)}") {
                                launchSingleTop = true
                            }
                        },
                        onBack = { navController.navigateUp() }
                    )
                    
                    // Search bar button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(CircleShape)
                            .combinedClickable(onClick = { isSearching = true }, onLongClick = {}),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Search files...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Check if selected files contain archives
                        val selectedArchives = selectionManager.selectedPaths.filter { 
                            isArchiveExtension(java.io.File(it).name) 
                        }
                        val hasArchives = selectedArchives.isNotEmpty()
                        
                        BottomActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            onClick = {
                                pendingFileOperation = Pair("COPY", selectionManager.selectedPaths.toList())
                                selectionManager.clearSelection()
                            }
                        )
                        BottomActionButton(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            onClick = {
                                pendingFileOperation = Pair("CUT", selectionManager.selectedPaths.toList())
                                selectionManager.clearSelection()
                            }
                        )
                        BottomActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = {
                                showDeleteDialog = selectionManager.selectedPaths.toList()
                            }
                        )
                        
                        // Show Extract button if archives are selected
                        if (hasArchives) {
                            BottomActionButton(
                                icon = Icons.Default.Unarchive,
                                label = "Extract",
                                onClick = {
                                    if (selectedArchives.size == 1) {
                                        // Single archive - show quick extract dialog
                                        val archiveFile = files.find { it.path == selectedArchives.first() }
                                        if (archiveFile != null) {
                                            selectionManager.clearSelection()
                                            showQuickExtractDialog = archiveFile
                                        }
                                    } else {
                                        // Multiple archives - batch extract all to current directory
                                        val archivesToExtract = selectedArchives.toList()
                                        selectionManager.clearSelection()
                                        multiArchiveExtractList = archivesToExtract
                                        currentExtractingIndex = 0
                                    }
                                }
                            )
                        } else {
                            BottomActionButton(
                                icon = Icons.Default.Share,
                                label = "Share",
                                onClick = {
                                    ShareUtils.shareMultipleFiles(context, selectionManager.selectedPaths)
                                    selectionManager.clearSelection()
                                }
                            )
                        }
                        
                        BottomActionButton(
                            icon = Icons.Default.FolderZip,
                            label = "Compress",
                            onClick = {
                                showCreateArchiveDialog = true
                            }
                        )
                        
                        BottomActionButton(
                            icon = Icons.Default.MoreVert,
                            label = "More",
                            onClick = {
                                showSelectionBottomSheet = true
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
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showNewFolderDialog = true
                                },
                                icon = { Icon(Icons.Default.CreateNewFolder, null) },
                                text = { Text(stringResource(R.string.dialog_new_folder)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    showNewFileDialog = true
                                },
                                icon = { Icon(Icons.Default.NoteAdd, null) },
                                text = { Text(stringResource(R.string.dialog_new_file)) },
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
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                items(6) { FileItemSkeleton() }
            } else if (filteredFiles.isEmpty()) {
                item {
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
                                        if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) stringResource(R.string.explorer_search_no_results, searchQuery) else stringResource(R.string.explorer_empty_folder),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "There's nothing here yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredFiles,
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
                                    context = context,
                                    file = file,
                                    navController = navController,
                                    archiveManager = archiveManager,
                                    scope = scope,
                                    snackbarHostState = snackbarHostState,
                                    onApkClick = { pendingApk = it }
                                )
                            }
                        },
                        onLongClick = {
                            if (!isSelecting) {
                                selectionManager.toggleSelection(file.path)
                            }
                        }
                    )
                }
            }
        }
        
        if (showSelectionBottomSheet) {
            val selectedPaths = selectionManager.selectedPaths.toList()
            val singleFile = if (selectedPaths.size == 1) files.find { it.path == selectedPaths.first() } else null
            
            SelectionActionBottomSheet(
                selectedPaths = selectedPaths,
                onDismiss = { showSelectionBottomSheet = false },
                onRename = {
                    singleFile?.let { showRenameDialog = it }
                    showSelectionBottomSheet = false
                },
                onShare = {
                    if (selectedPaths.size == 1) {
                        ShareUtils.shareFile(context, selectedPaths.first())
                    } else {
                        ShareUtils.shareMultipleFiles(context, selectedPaths)
                    }
                    showSelectionBottomSheet = false
                },
                onProperties = {
                    singleFile?.let { showPropertiesDialog = it.path }
                    showSelectionBottomSheet = false
                },
                onBookmark = {
                    singleFile?.let {
                        scope.launch {
                            val isNowBookmarked = bookmarksManager.toggleBookmark(it.path)
                            snackbarHostState.showSnackbar(
                                if (isNowBookmarked) "Added to bookmarks" else "Removed from bookmarks"
                            )
                        }
                    }
                    showSelectionBottomSheet = false
                },
                onExtract = if (singleFile != null && !singleFile.isDirectory && isArchiveExtension(singleFile.name)) {
                    {
                        showSelectionBottomSheet = false
                        showQuickExtractDialog = singleFile
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
        
        // Create Archive Dialog
        if (showCreateArchiveDialog) {
            CreateArchiveDialog(
                selectedFilesCount = selectionManager.selectedCount,
                onDismiss = { 
                    showCreateArchiveDialog = false
                },
                onCreate = { archiveName, format, compressionLevel ->
                    showCreateArchiveDialog = false
                    
                    val selectedFiles = selectionManager.selectedPaths.toList()
                    val extension = format.extension
                    val fullPath = "$path/$archiveName.$extension"
                    
                    scope.launch {
                        try {
                            archiveCreationProgress = ArchiveCreationProgress("Preparing...", 0, 0, selectedFiles.size, 0L, 0L)
                            ArchiveCreator.createArchive(
                                outputPath = fullPath,
                                files = selectedFiles,
                                basePath = if (selectedFiles.size == 1) {
                                    java.io.File(selectedFiles.first()).parentFile?.absolutePath ?: ""
                                } else {
                                    findCommonParent(selectedFiles)
                                },
                                compressionLevel = compressionLevel
                            ).collect { prog ->
                                archiveCreationProgress = prog
                            }
                            archiveCreationProgress = null
                            selectionManager.clearSelection()
                            refreshFiles()
                            snackbarHostState.showSnackbar("Archive created successfully")
                        } catch (e: Exception) {
                            archiveCreationProgress = null
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                }
            )
        }
        
        // Quick Extract Dialog
        showQuickExtractDialog?.let { file ->
            AlertDialog(
                onDismissRequest = { showQuickExtractDialog = null },
                title = { 
                    Text("Extract Archive", fontWeight = FontWeight.Bold) 
                },
                text = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        
                        // Extract Here
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(onClick = {
                                    showQuickExtractDialog = null
                                    scope.launch {
                                        extractionProgress = ExtractionProgress(
                                            0, "Starting...", ExtractionState.STARTED
                                        )
                                        archiveManager.extractArchive(
                                            file.path, 
                                            path + "/" + java.io.File(file.path).nameWithoutExtension
                                        ).collect { progress ->
                                            extractionProgress = progress
                                            if (progress.state == ExtractionState.COMPLETED) {
                                                extractionProgress = null
                                                refreshFiles()
                                                snackbarHostState.showSnackbar("Extracted to ${java.io.File(file.path).nameWithoutExtension}")
                                            } else if (progress.state == ExtractionState.ERROR) {
                                                extractionProgress = null
                                                snackbarHostState.showSnackbar("Extraction failed: ${progress.currentFile}")
                                            }
                                        }
                                    }
                                }, onLongClick = {}),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Unarchive, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Extract Here", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Extract to current folder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Open Archive
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(onClick = {
                                    showQuickExtractDialog = null
                                    navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                }, onLongClick = {}),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Open Archive", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Browse contents first",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Extract to Custom Path
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(onClick = {
                                    showQuickExtractDialog = null
                                    customPath = downloadsPath
                                    showCustomPathDialogFor = file
                                }, onLongClick = {}),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreateNewFolder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Extract to Custom Path", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Choose a different location",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showQuickExtractDialog = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
        
        // Custom Path Dialog
        showCustomPathDialogFor?.let { file ->
            id.xms.xarchiver.ui.components.FolderPickerDialog(
                title = "Extract to...",
                onDismissRequest = { showCustomPathDialogFor = null },
                onFolderSelected = { selectedPath ->
                    showCustomPathDialogFor = null
                    scope.launch {
                        extractionProgress = ExtractionProgress(
                            0, "Starting...", ExtractionState.STARTED
                        )
                        val targetFolder = selectedPath + "/" + java.io.File(file.path).nameWithoutExtension
                        archiveManager.extractArchive(
                            file.path, 
                            targetFolder
                        ).collect { progress ->
                            extractionProgress = progress
                            if (progress.state == ExtractionState.COMPLETED) {
                                extractionProgress = null
                                refreshFiles()
                                snackbarHostState.showSnackbar("Extracted to $targetFolder")
                            } else if (progress.state == ExtractionState.ERROR) {
                                extractionProgress = null
                                snackbarHostState.showSnackbar("Extraction failed: ${progress.currentFile}")
                            }
                        }
                    }
                }
            )
        }

        // Copy/Cut Dialog
        pendingFileOperation?.let { operation ->
            id.xms.xarchiver.ui.components.FolderPickerDialog(
                title = if (operation.first == "COPY") "Copy to..." else "Move to...",
                onDismissRequest = { pendingFileOperation = null },
                onFolderSelected = { selectedPath ->
                    val isCut = operation.first == "CUT"
                    val paths = operation.second
                    val itemCount = paths.size
                    pendingFileOperation = null
                    scope.launch {
                        if (isCut) {
                            FileOperationsManager.cutToClipboard(paths)
                        } else {
                            FileOperationsManager.copyToClipboard(paths)
                        }
                        
                        fileOperationProgress = FileOperationProgress("Preparing...", 0, 0, 0, 0, itemCount)
                        FileOperationsManager.pasteFiles(selectedPath).collect { progress ->
                            fileOperationProgress = progress
                        }
                        fileOperationProgress = null
                        
                        refreshFiles()
                        val action = if (isCut) "moved" else "copied"
                        snackbarHostState.showSnackbar("$itemCount items $action successfully")
                        FileOperationsManager.clearClipboard()
                    }
                }
            )
        }
        
        // Extraction Progress Overlay
        extractionProgress?.let { progress ->
            AlertDialog(
                onDismissRequest = { /* Can't dismiss during extraction */ },
                title = { Text("Extracting...") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.percentage / 100f }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("${progress.percentage}%", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            progress.currentFile,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatFileSize(progress.bytesProcessed)} / ${formatFileSize(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${progress.percentage}% complete",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }

        // File Operation Progress Overlay
        fileOperationProgress?.let { progress ->
            AlertDialog(
                onDismissRequest = { /* Can't dismiss during operation */ },
                title = { Text(if (fileOperationProgress?.totalFiles == 1) "Processing..." else "Moving/Copying...") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.percentage / 100f }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("${progress.percentage}%", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            progress.currentFile,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatFileSize(progress.bytesProcessed)} / ${formatFileSize(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${progress.filesProcessed} / ${progress.totalFiles} files",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }

        // Archive Creation Progress Overlay
        archiveCreationProgress?.let { progress ->
            AlertDialog(
                onDismissRequest = { /* Can't dismiss during compression */ },
                title = { Text("Compressing...") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.percentage / 100f }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("${progress.percentage}%", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            progress.currentFile,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatFileSize(progress.bytesProcessed)} / ${formatFileSize(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${progress.filesProcessed} / ${progress.totalFiles} files",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }
    }
}
}

@Composable
internal fun FileItemCard(
    file: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fileIcon = remember(file.name, file.isDirectory) { getFileIcon(file) }
    val fileColor = getFileColor(file)
    val formattedDate = remember(file.lastModified) {
        dateFormatter.format(Date(file.lastModified))
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
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

                var apkIconDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
                val context = LocalContext.current

                if (isApk) {
                    LaunchedEffect(file.path) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val pm = context.packageManager
                                val pi = pm.getPackageArchiveInfo(file.path, 0)
                                pi?.applicationInfo?.let { appInfo ->
                                    appInfo.sourceDir = file.path
                                    appInfo.publicSourceDir = file.path
                                    apkIconDrawable = appInfo.loadIcon(pm)
                                }
                            } catch (e: Exception) {
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
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(java.io.File(file.path))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isApk && apkIconDrawable != null) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(apkIconDrawable)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
                    text = if (file.isDirectory) "Folder" else "${file.size.humanReadable()} • $formattedDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (file.isDirectory) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            if (!isSelectionMode) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
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

// Helper functions
private fun handleFileClick(
    context: android.content.Context,
    file: FileItem,
    navController: NavController,
    archiveManager: ArchiveManager,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: id.xms.xarchiver.ui.components.NotificationHostState,
    onApkClick: (File) -> Unit
) {
    // Use FileActionHandler for consistent file handling
    id.xms.xarchiver.ui.explorer.utils.FileActionHandler.handleFileClick(
        context = context,
        file = file,
        navController = navController,
        archiveManager = archiveManager,
        scope = scope,
        notificationHostState = snackbarHostState,
        onApkClick = onApkClick
    )
}

private fun isDocumentExtension(ext: String): Boolean {
    return ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf")
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
    val lowerName = fileName.lowercase()
    // Check for compound extensions first
    if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar.bz2") ||
        lowerName.endsWith(".tar.xz") || lowerName.endsWith(".tar.lz")) {
        return true
    }
    val ext = lowerName.substringAfterLast('.', "")
    return ext in listOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "lz", "jar", "aar", "xapk")
}

internal fun getFileIcon(file: FileItem): ImageVector {
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
internal fun getFileColor(file: FileItem): Color {
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

// Dialog Composables
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionActionBottomSheet(
    selectedPaths: List<String>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onProperties: () -> Unit,
    onBookmark: () -> Unit,
    onExtract: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "${selectedPaths.size} item${if(selectedPaths.size > 1) "s" else ""} selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            
            if (selectedPaths.size == 1) {
                BottomSheetAction(Icons.Default.Edit, "Rename", onRename)
                BottomSheetAction(Icons.Default.Info, "Properties", onProperties)
                BottomSheetAction(Icons.Default.Bookmark, "Bookmark", onBookmark)
            }
            
            BottomSheetAction(Icons.Default.Share, "Share", onShare)
            
            onExtract?.let {
                BottomSheetAction(Icons.Default.FolderZip, "Open Archive", it)
            }
        }
    }
}

@Composable
private fun BottomSheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
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
        title = { Text(stringResource(R.string.dialog_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.dialog_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) { Text(stringResource(R.string.dialog_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
        title = { Text(stringResource(R.string.dialog_delete_title, count, if (count > 1) "s" else "")) },
        text = { Text(stringResource(R.string.dialog_delete_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
                label = { Text(stringResource(R.string.dialog_name)) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
        title = { Text(stringResource(R.string.dialog_install_apk_title)) },
        text = {
            Text(stringResource(R.string.dialog_install_apk_desc, apkFile.name))
        },
        confirmButton = {
            TextButton(onClick = onInstall) { Text(stringResource(R.string.action_install)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun findCommonParent(paths: List<String>): String {
    if (paths.isEmpty()) return ""
    if (paths.size == 1) return java.io.File(paths.first()).parentFile?.absolutePath ?: ""
    
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

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
