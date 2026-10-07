package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.*
import id.xms.xarchiver.core.archive.*
import id.xms.xarchiver.core.install.ApkInstaller
import id.xms.xarchiver.ui.components.LocalNotificationHost
import id.xms.xarchiver.ui.explorer.material.*
import id.xms.xarchiver.ui.explorer.utils.FileActionHandler
import id.xms.xarchiver.ui.explorer.utils.FileTypeDetector
import id.xms.xarchiver.ui.theme.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Standard Material Design 3 File Explorer Screen.
 * Delegates to modular components in id.xms.xarchiver.ui.explorer.material.*
 * or routes to MiuixExplorerScreen when MIUIX / HyperOS mode is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(
        initial = ThemePreferences.isMiuiOrHyperOsDevice
    )

    // Route to dedicated MIUIX / HyperOS File Explorer if enabled
    if (isMiuixUiEnabled) {
        MiuixExplorerScreen(path = path, navController = navController)
        return
    }

    val scope = rememberCoroutineScope()
    val archiveManager = remember { ArchiveManager(context) }
    val selectionManager = remember { SelectionManager() }
    val bookmarksManager = remember { BookmarksManager(context) }
    val snackbarHostState = LocalNotificationHost.current

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Dialog & overlay states
    var pendingApk by remember { mutableStateOf<File?>(null) }
    var showPropertiesDialog by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showFileExistsDialog by remember { mutableStateOf<String?>(null) }
    var showSelectionBottomSheet by remember { mutableStateOf(false) }
    var showCreateArchiveDialog by remember { mutableStateOf(false) }
    var showQuickExtractDialog by remember { mutableStateOf<FileItem?>(null) }
    var showCustomPathDialogFor by remember { mutableStateOf<FileItem?>(null) }
    var pendingFileOperation by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var extractionProgress by remember { mutableStateOf<ExtractionProgress?>(null) }
    var archiveCreationProgress by remember { mutableStateOf<ArchiveCreationProgress?>(null) }
    var fileOperationProgress by remember { mutableStateOf<FileOperationProgress?>(null) }
    var multiArchiveExtractList by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentExtractingIndex by remember { mutableStateOf(0) }
    var pendingPasswordExtraction by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var passwordExtractionError by remember { mutableStateOf<String?>(null) }

    // Selection mode state
    val isSelecting = selectionManager.isSelecting
    val selectedCount = selectionManager.selectedCount

    // Search state
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isEmpty()) {
            files
        } else {
            files.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val listState = rememberLazyListState()

    fun refreshFiles() {
        scope.launch {
            isLoading = true
            files = FileService.listDirectory(path)
            isLoading = false
        }
    }

    val executeExtraction: (String, String, String, String?) -> Unit = { archivePath, archiveName, outputDir, pwd ->
        scope.launch {
            extractionProgress = ExtractionProgress(0, "Starting...", ExtractionState.STARTED)
            archiveManager.extractArchive(
                archiveFilePath = archivePath,
                outputDir = outputDir,
                password = pwd
            ).collect { progress ->
                extractionProgress = progress
                if (progress.state == ExtractionState.COMPLETED) {
                    extractionProgress = null
                    refreshFiles()
                    snackbarHostState.showSnackbar("Extracted to ${File(outputDir).name}")
                } else if (progress.state == ExtractionState.ERROR) {
                    extractionProgress = null
                    val err = progress.error ?: progress.currentFile
                    if (err.contains("password", ignoreCase = true) || err.contains("kata sandi", ignoreCase = true)) {
                        passwordExtractionError = if (pwd != null) "Incorrect password. Please try again." else null
                        pendingPasswordExtraction = Triple(archivePath, archiveName, outputDir)
                    } else {
                        snackbarHostState.showSnackbar("Extraction failed: $err")
                    }
                }
            }
        }
    }

    val requestExtraction: (String, String, String) -> Unit = { archivePath, archiveName, outputDir ->
        scope.launch {
            val isEncrypted = withContext(Dispatchers.IO) {
                archiveManager.isArchiveEncrypted(archivePath)
            }
            if (isEncrypted) {
                passwordExtractionError = null
                pendingPasswordExtraction = Triple(archivePath, archiveName, outputDir)
            } else {
                executeExtraction(archivePath, archiveName, outputDir, null)
            }
        }
    }

    LaunchedEffect(path) {
        refreshFiles()
        selectionManager.clearSelection()
        isSearching = false
        searchQuery = ""
    }

    // Handle batch extraction of multiple selected archives
    LaunchedEffect(multiArchiveExtractList, currentExtractingIndex) {
        if (multiArchiveExtractList.isNotEmpty() && currentExtractingIndex < multiArchiveExtractList.size) {
            val archivePath = multiArchiveExtractList[currentExtractingIndex]
            val archiveFile = File(archivePath)
            val outputDir = "$path/${archiveFile.nameWithoutExtension}"

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
                            extractionProgress = null
                            multiArchiveExtractList = emptyList()
                            refreshFiles()
                            snackbarHostState.showSnackbar("Extracted archives successfully")
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
                brush = Brush.radialGradient(
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
                    MaterialSelectionTopBar(
                        selectedCount = selectedCount,
                        onClearSelection = { selectionManager.clearSelection() },
                        onSelectAll = { selectionManager.selectAll(files.map { it.path }) },
                        onReverseSelection = { selectionManager.reverseSelection(files.map { it.path }) },
                        onSelectSameType = if (selectedCount == 1) {
                            { selectionManager.selectSameType(files.map { it.path }, selectionManager.selectedPaths.first()) }
                        } else null
                    )
                } else if (isSearching) {
                    MaterialSearchTopBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onCloseSearch = {
                            isSearching = false
                            searchQuery = ""
                        }
                    )
                } else {
                    MaterialNormalTopBar(
                        path = path,
                        navController = navController,
                        onStartSearch = { isSearching = true }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isSelecting,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    val selectedArchives = selectionManager.selectedPaths.filter {
                        FileTypeDetector.isArchiveExtension(File(it).name)
                    }
                    val hasArchives = selectedArchives.isNotEmpty()

                    MaterialSelectionDock(
                        hasArchives = hasArchives,
                        onCopy = {
                            pendingFileOperation = Pair("COPY", selectionManager.selectedPaths.toList())
                            selectionManager.clearSelection()
                        },
                        onCut = {
                            pendingFileOperation = Pair("CUT", selectionManager.selectedPaths.toList())
                            selectionManager.clearSelection()
                        },
                        onDelete = {
                            showDeleteDialog = selectionManager.selectedPaths.toList()
                        },
                        onExtractOrShare = {
                            if (hasArchives) {
                                if (selectedArchives.size == 1) {
                                    val archiveFile = files.find { it.path == selectedArchives.first() }
                                    if (archiveFile != null) {
                                        selectionManager.clearSelection()
                                        showQuickExtractDialog = archiveFile
                                    }
                                } else {
                                    val archivesToExtract = selectedArchives.toList()
                                    selectionManager.clearSelection()
                                    multiArchiveExtractList = archivesToExtract
                                    currentExtractingIndex = 0
                                }
                            } else {
                                ShareUtils.shareMultipleFiles(context, selectionManager.selectedPaths)
                                selectionManager.clearSelection()
                            }
                        },
                        onCompress = {
                            showCreateArchiveDialog = true
                        },
                        onMore = {
                            showSelectionBottomSheet = true
                        }
                    )
                }
            },
            floatingActionButton = {
                MaterialExplorerFab(
                    visible = !isSelecting,
                    showFabMenu = showFabMenu,
                    onToggleFabMenu = { showFabMenu = !showFabMenu },
                    onNewFolder = {
                        showFabMenu = false
                        showNewFolderDialog = true
                    },
                    onNewFile = {
                        showFabMenu = false
                        showNewFileDialog = true
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    items(6) { MaterialFileItemSkeleton() }
                } else if (filteredFiles.isEmpty()) {
                    item { MaterialEmptyFolderView(searchQuery = searchQuery) }
                } else {
                    itemsIndexed(
                        items = filteredFiles,
                        key = { _, file -> file.path },
                        contentType = { _, file -> if (file.isDirectory) "folder" else "file" }
                    ) { _, file ->
                        MaterialFileItemCard(
                            file = file,
                            isSelected = selectionManager.isSelected(file.path),
                            isSelectionMode = isSelecting,
                            onClick = {
                                if (isSelecting) {
                                    selectionManager.toggleSelection(file.path)
                                } else {
                                    FileActionHandler.handleFileClick(
                                        context = context,
                                        file = file,
                                        navController = navController,
                                        archiveManager = archiveManager,
                                        scope = scope,
                                        notificationHostState = snackbarHostState,
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
        }
    }

    // Material 3 Dialogs & Progress Overlays Host
    val selectedPathsList = selectionManager.selectedPaths.toList()
    val singleFile = if (selectedPathsList.size == 1) files.find { it.path == selectedPathsList.first() } else null

    MaterialExplorerDialogHost(
        showSelectionBottomSheet = showSelectionBottomSheet,
        selectedPaths = selectedPathsList,
        singleFile = singleFile,
        showPropertiesDialog = showPropertiesDialog,
        showRenameDialog = showRenameDialog,
        showDeleteDialog = showDeleteDialog,
        showNewFolderDialog = showNewFolderDialog,
        showNewFileDialog = showNewFileDialog,
        showFileExistsDialog = showFileExistsDialog,
        pendingApk = pendingApk,
        showCreateArchiveDialog = showCreateArchiveDialog,
        showQuickExtractDialog = showQuickExtractDialog,
        showCustomPathDialogFor = showCustomPathDialogFor,
        pendingPasswordExtraction = pendingPasswordExtraction,
        passwordExtractionError = passwordExtractionError,
        pendingFileOperation = pendingFileOperation,
        extractionProgress = extractionProgress,
        fileOperationProgress = fileOperationProgress,
        archiveCreationProgress = archiveCreationProgress,
        onDismissSelectionBottomSheet = { showSelectionBottomSheet = false },
        onRenameSingleFile = { showRenameDialog = it },
        onShareSelected = { paths ->
            if (paths.size == 1) {
                ShareUtils.shareFile(context, paths.first())
            } else {
                ShareUtils.shareMultipleFiles(context, paths)
            }
        },
        onPropertiesSingleFile = { showPropertiesDialog = it },
        onBookmarkSingleFile = { file ->
            scope.launch {
                val isNowBookmarked = bookmarksManager.toggleBookmark(file.path)
                snackbarHostState.showSnackbar(
                    if (isNowBookmarked) "Added to bookmarks" else "Removed from bookmarks"
                )
            }
        },
        onExtractSingleFile = { file -> showQuickExtractDialog = file },
        onDismissPropertiesDialog = { showPropertiesDialog = null },
        onConfirmRename = { file, newName ->
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
        onDismissRename = { showRenameDialog = null },
        onConfirmDelete = { paths ->
            scope.launch {
                val result = FileOperationsManager.deleteFiles(paths)
                selectionManager.clearSelection()
                refreshFiles()
                when (result) {
                    is FileOperationResult.Success -> snackbarHostState.showSnackbar(result.message)
                    is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                }
            }
            showDeleteDialog = null
        },
        onDismissDelete = { showDeleteDialog = null },
        onConfirmNewFolder = { name ->
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
        onDismissNewFolder = { showNewFolderDialog = false },
        onConfirmNewFile = { name ->
            if (files.any { it.name == name }) {
                showFileExistsDialog = name
                showNewFileDialog = false
            } else {
                scope.launch {
                    val result = FileOperationsManager.createFile(path, name)
                    refreshFiles()
                    when (result) {
                        is FileOperationResult.Success -> snackbarHostState.showSnackbar("File created")
                        is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                    }
                }
                showNewFileDialog = false
            }
        },
        onDismissNewFile = { showNewFileDialog = false },
        onOverwriteExists = { name ->
            scope.launch {
                FileOperationsManager.deleteFiles(listOf("$path/$name"))
                val result = FileOperationsManager.createFile(path, name)
                refreshFiles()
                when (result) {
                    is FileOperationResult.Success -> snackbarHostState.showSnackbar("File overwritten")
                    is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                }
            }
            showFileExistsDialog = null
        },
        onDuplicateExists = { name ->
            val nameWithoutExt = name.substringBeforeLast(".", name)
            val ext = if (name.contains(".")) ".${name.substringAfterLast(".")}" else ""
            var index = 1
            var newName = "$nameWithoutExt($index)$ext"
            while (files.any { it.name == newName }) {
                index++
                newName = "$nameWithoutExt($index)$ext"
            }
            scope.launch {
                val result = FileOperationsManager.createFile(path, newName)
                refreshFiles()
                when (result) {
                    is FileOperationResult.Success -> snackbarHostState.showSnackbar("File created as $newName")
                    is FileOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
                }
            }
            showFileExistsDialog = null
        },
        onDismissFileExists = { showFileExistsDialog = null },
        onInstallApk = { apkFile ->
            ApkInstaller.installApk(context, apkFile)
            pendingApk = null
        },
        onDismissApk = { pendingApk = null },
        onCreateArchive = { archiveName, format, compressionLevel, password, encryptionType ->
            showCreateArchiveDialog = false
            val selectedFiles = selectionManager.selectedPaths.toList()
            val fullPath = "$path/$archiveName.${format.extension}"

            scope.launch {
                try {
                    archiveCreationProgress = ArchiveCreationProgress("Preparing...", 0, 0, selectedFiles.size, 0L, 0L)
                    ArchiveCreator.createArchive(
                        outputPath = fullPath,
                        files = selectedFiles,
                        basePath = if (selectedFiles.size == 1) {
                            File(selectedFiles.first()).parentFile?.absolutePath ?: ""
                        } else {
                            findCommonParent(selectedFiles)
                        },
                        compressionLevel = compressionLevel,
                        password = password,
                        encryptionType = encryptionType
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
        },
        onDismissCreateArchive = { showCreateArchiveDialog = false },
        onExtractHere = { file ->
            showQuickExtractDialog = null
            val outputDir = "$path/${File(file.path).nameWithoutExtension}"
            requestExtraction(file.path, file.name, outputDir)
        },
        onOpenArchive = { file ->
            showQuickExtractDialog = null
            navController.navigate("archive_explorer/${Uri.encode(file.path)}")
        },
        onCustomPathExtractRequest = { file ->
            showQuickExtractDialog = null
            showCustomPathDialogFor = file
        },
        onDismissQuickExtract = { showQuickExtractDialog = null },
        onCustomPathFolderSelected = { file, selectedPath ->
            showCustomPathDialogFor = null
            val targetFolder = "$selectedPath/${File(file.path).nameWithoutExtension}"
            requestExtraction(file.path, file.name, targetFolder)
        },
        onDismissCustomPath = { showCustomPathDialogFor = null },
        onConfirmPassword = { archivePath, archiveName, outputDir, enteredPassword ->
            pendingPasswordExtraction = null
            passwordExtractionError = null
            executeExtraction(archivePath, archiveName, outputDir, enteredPassword)
        },
        onDismissPassword = {
            pendingPasswordExtraction = null
            passwordExtractionError = null
        },
        onFileOperationDestinationSelected = { operation, paths, selectedPath ->
            val isCut = operation == "CUT"
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
        },
        onDismissFileOperation = { pendingFileOperation = null }
    )
}
