package id.xms.xarchiver.ui.archive

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import id.xms.xarchiver.core.SelectionManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import id.xms.xarchiver.ui.components.LocalNotificationHost
import id.xms.xarchiver.ui.components.NotificationType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import id.xms.xarchiver.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.xms.xarchiver.core.archive.ExtractionProgress
import id.xms.xarchiver.core.archive.ExtractionState
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveExplorerScreen(
    archivePath: String,
    nestedPath: String? = null,
    navController: NavController,
    viewModel: ArchiveViewModel = viewModel(
        factory = ArchiveViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationHostState = LocalNotificationHost.current
    var showExtractionDialog by remember { mutableStateOf(false) }
    var showCustomPathDialog by remember { mutableStateOf(false) }
    var customPath by remember { mutableStateOf("") }
    var extractionProgress by remember { mutableStateOf<ExtractionProgress?>(null) }
    var pendingPasswordExtract by remember { mutableStateOf<Pair<String, List<String>?>?>(null) }
    var passwordExtractError by remember { mutableStateOf<String?>(null) }
    var sessionPassword by remember { mutableStateOf<String?>(null) }
    var pendingPasswordViewEntry by remember { mutableStateOf<ArchiveEntry?>(null) }
    var passwordViewError by remember { mutableStateOf<String?>(null) }

    val archiveEntries = viewModel.archiveEntries.value
    val currentPath = remember(archivePath, nestedPath) {
        if (nestedPath != null) "$archivePath:$nestedPath" else archivePath
    }

    // Default paths
    val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    val archiveFileName = File(archivePath).nameWithoutExtension

    val requestArchiveExtract: (String, List<String>?) -> Unit = { outputDir, targetEntries ->
        scope.launch {
            val isEncrypted = viewModel.isArchiveEncrypted(archivePath)
            if (isEncrypted && sessionPassword == null) {
                passwordExtractError = null
                pendingPasswordExtract = Pair(outputDir, targetEntries)
            } else {
                extractArchive(
                    viewModel = viewModel,
                    archivePath = archivePath,
                    outputDir = outputDir,
                    targetEntries = targetEntries,
                    password = sessionPassword,
                    notificationHostState = notificationHostState,
                    onProgress = { extractionProgress = it }
                )
            }
        }
    }

    LaunchedEffect(archivePath, nestedPath) {
        if (nestedPath != null) {
            viewModel.loadNestedArchiveContents(archivePath, nestedPath)
        } else {
            viewModel.loadArchiveContents(archivePath)
        }
    }

    // Current folder prefix for navigation within archive
    var currentPrefix by remember { mutableStateOf("") }
    
    // Filter entries to show only items in current folder
    val selectionManager = remember { SelectionManager() }
    val isSelecting = selectionManager.isSelecting

    // Effect to handle back button behavior during selection
    BackHandler(enabled = isSelecting) {
        selectionManager.clearSelection()
    }

    val filteredEntries = remember(archiveEntries, currentPrefix) {
        val immediateItems = mutableListOf<ArchiveEntry>()
        val processedNames = mutableSetOf<String>()
        
        for (entry in archiveEntries) {
            val name = entry.name
            
            // Skip if not in current prefix
            if (currentPrefix.isNotEmpty() && !name.startsWith(currentPrefix)) continue
            
            // Get relative path from current prefix
            val relativePath = if (currentPrefix.isNotEmpty()) {
                name.removePrefix(currentPrefix)
            } else {
                name
            }
            
            // Skip empty paths
            if (relativePath.isEmpty() || relativePath == "/") continue
            
            // Check if this is a direct child or nested deeper
            val parts = relativePath.trimEnd('/').split("/")
            
            if (parts.size == 1 && parts[0].isNotEmpty()) {
                // Direct child file or folder - check for duplicates
                val normalizedName = name.trimEnd('/')
                if (normalizedName !in processedNames) {
                    processedNames.add(normalizedName)
                    immediateItems.add(entry)
                }
            } else if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                // This is inside a subfolder - add virtual folder entry if not already added
                val folderName = parts[0]
                val folderPath = currentPrefix + folderName
                
                if (folderPath !in processedNames) {
                    processedNames.add(folderPath)
                    // Find the actual folder entry or create a virtual one
                    val existingFolder = archiveEntries.find { 
                        it.name.trimEnd('/') == folderPath
                    }
                    if (existingFolder != null) {
                        immediateItems.add(existingFolder)
                    } else {
                        // Create virtual folder entry
                        immediateItems.add(VirtualFolderEntry(folderPath + "/"))
                    }
                }
            }
        }
        
        // Sort: folders first, then files, alphabetically
        immediateItems.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    val openEntryFile: (ArchiveEntry, String?) -> Unit = { targetEntry, pass ->
        scope.launch {
            try {
                Toast.makeText(context, context.getString(R.string.archive_toast_extracting_format, targetEntry.name), Toast.LENGTH_SHORT).show()

                // Extract to Downloads folder instead of app cache
                val fileName = targetEntry.name.substringAfterLast('/')
                val outputDir = "$downloadsPath/.XArchiver_temp"
                val outputFile = File(outputDir, fileName)

                // Create output directory
                File(outputDir).mkdirs()

                val effectivePass = pass ?: sessionPassword
                val extractedPath = viewModel.viewArchiveEntry(archivePath, targetEntry.name, effectivePass)

                if (extractedPath != null) {
                    val cacheFile = File(extractedPath)
                    if (cacheFile.exists()) {
                        cacheFile.copyTo(outputFile, overwrite = true)

                        Toast.makeText(context, context.getString(R.string.archive_toast_extracted_format, outputFile.absolutePath), Toast.LENGTH_SHORT).show()

                        // Open with appropriate viewer
                        val ext = targetEntry.name.substringAfterLast('.', "").lowercase()
                        when {
                            ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> {
                                navController.navigate("image_viewer/${Uri.encode(outputFile.absolutePath)}")
                            }
                            ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a") -> {
                                navController.navigate("audio_player/${Uri.encode(outputFile.absolutePath)}")
                            }
                            ext in listOf("mp4", "avi", "mkv", "mov", "wmv", "webm") -> {
                                navController.navigate("video_player/${Uri.encode(outputFile.absolutePath)}")
                            }
                            ext in listOf("txt", "md", "log", "json", "xml", "html", "css", "js") -> {
                                navController.navigate("text_editor/${Uri.encode(outputFile.absolutePath)}")
                            }
                            ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> {
                                // Open with external app
                                id.xms.xarchiver.core.ShareUtils.openFile(context, outputFile.absolutePath)
                            }
                            else -> {
                                // Try to open as text or with external app
                                navController.navigate("text_editor/${Uri.encode(outputFile.absolutePath)}")
                            }
                        }
                    } else {
                        notificationHostState.showNotification(context.getString(R.string.archive_notification_extract_failed_format, targetEntry.name), NotificationType.ERROR)
                    }
                } else {
                    val isEncrypted = (targetEntry as? id.xms.xarchiver.core.archive.XArchiveEntry)?.isEncrypted == true ||
                            viewModel.isEntryEncrypted(archivePath, targetEntry.name) ||
                            viewModel.isArchiveEncrypted(archivePath)
                    if (isEncrypted) {
                        passwordViewError = if (effectivePass != null) context.getString(R.string.archive_password_incorrect) else null
                        pendingPasswordViewEntry = targetEntry
                    } else {
                        notificationHostState.showNotification(context.getString(R.string.archive_notification_extract_failed_format, targetEntry.name), NotificationType.ERROR)
                    }
                }
            } catch (e: Exception) {
                notificationHostState.showNotification(context.getString(R.string.archive_notification_error_format, e.message), NotificationType.ERROR)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSelecting) {
                TopAppBar(
                    title = { Text(stringResource(R.string.archive_selected_format, selectionManager.selectedPaths.size)) },
                    navigationIcon = {
                        IconButton(onClick = { selectionManager.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectionManager.selectedPaths.size == filteredEntries.size) {
                                selectionManager.clearSelection()
                            } else {
                                selectionManager.selectAll(filteredEntries.map { it.name })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showExtractionDialog = true }) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Extract Selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = File(archivePath).name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentPrefix.isNotEmpty()) {
                                Text(
                                    text = "/" + currentPrefix.trimEnd('/'),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (currentPrefix.isNotEmpty()) {
                                // Go up one directory level
                                val parts = currentPrefix.trimEnd('/').split("/")
                                currentPrefix = if (parts.size > 1) {
                                    parts.dropLast(1).joinToString("/") + "/"
                                } else {
                                    ""
                                }
                            } else {
                                navController.popBackStack() 
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExtractionDialog = true }) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Extract All")
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredEntries.isEmpty()) {
                Text(
                    text = if (currentPrefix.isEmpty()) stringResource(R.string.archive_empty_entries) else stringResource(R.string.archive_empty_folder),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = filteredEntries,
                        key = { index, entry -> "${index}_${entry.name.trimEnd('/')}" }
                    ) { _, entry ->
                        val isSelected = selectionManager.isSelected(entry.name)
                        ArchiveEntryItem(
                            entry = entry,
                            isSelected = isSelected,
                            onLongClick = {
                                if (!isSelecting) {
                                    selectionManager.toggleSelection(entry.name)
                                }
                            },
                            onClick = {
                                if (isSelecting) {
                                    selectionManager.toggleSelection(entry.name)
                                    return@ArchiveEntryItem
                                }
                                if (entry.isDirectory) {
                                    // Navigate into folder
                                    currentPrefix = entry.name.let { 
                                        if (it.endsWith("/")) it else "$it/" 
                                    }
                                } else {
                                    val entryName = entry.name
                                    val extension = entryName.substringAfterLast('.', "").lowercase()

                                    // If the entry looks like it might be an archive file itself
                                    if (extension in listOf("zip", "rar", "7z", "tar", "gz", "jar", "apk")) {
                                        // Navigate to view the nested archive
                                        val encodedArchivePath = Uri.encode(archivePath)
                                        val encodedEntryPath = Uri.encode(entry.name)
                                        navController.navigate("archive_explorer/$encodedArchivePath/$encodedEntryPath")
                                    } else {
                                        val isEncrypted = (entry as? id.xms.xarchiver.core.archive.XArchiveEntry)?.isEncrypted == true ||
                                                viewModel.isEntryEncrypted(archivePath, entry.name) ||
                                                viewModel.isArchiveEncrypted(archivePath)
                                        if (isEncrypted && sessionPassword == null) {
                                            passwordViewError = null
                                            pendingPasswordViewEntry = entry
                                        } else {
                                            openEntryFile(entry, sessionPassword)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Extraction Dialog
        if (showExtractionDialog) {
            AlertDialog(
                onDismissRequest = { showExtractionDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.archive_extract_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.archive_extract_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Extract to Downloads option
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showExtractionDialog = false
                                    val outputDir = "$downloadsPath/$archiveFileName"
                                    val targetEntries = if (isSelecting) selectionManager.selectedPaths.toList() else null
                                    requestArchiveExtract(outputDir, targetEntries)
                                    selectionManager.clearSelection()
                                },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.archive_extract_here),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Downloads/$archiveFileName/",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Extract to custom path option
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showExtractionDialog = false
                                    customPath = downloadsPath
                                    showCustomPathDialog = true
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.archive_extract_custom),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.archive_extract_custom_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = { showExtractionDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                dismissButton = null,
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Custom Path Dialog
        if (showCustomPathDialog) {
            AlertDialog(
                onDismissRequest = { showCustomPathDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.archive_extract_custom_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.archive_extract_custom_prompt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = customPath,
                            onValueChange = { customPath = it },
                            label = { Text(stringResource(R.string.archive_extract_path_label)) },
                            placeholder = { Text("/storage/emulated/0/Download") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomPathDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = {
                                showCustomPathDialog = false
                                val outputDir = "$customPath/$archiveFileName"
                                val targetEntries = if (isSelecting) selectionManager.selectedPaths.toList() else null
                                requestArchiveExtract(outputDir, targetEntries)
                                selectionManager.clearSelection()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.action_extract), fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = null,
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Password Prompt Dialog for encrypted archives
        pendingPasswordExtract?.let { (outputDir, targetEntries) ->
            ArchivePasswordDialog(
                archiveName = File(archivePath).name,
                errorMessage = passwordExtractError,
                onDismiss = {
                    pendingPasswordExtract = null
                    passwordExtractError = null
                },
                onConfirm = { enteredPassword ->
                    pendingPasswordExtract = null
                    passwordExtractError = null
                    sessionPassword = enteredPassword
                    scope.launch {
                        extractArchive(
                            viewModel = viewModel,
                            archivePath = archivePath,
                            outputDir = outputDir,
                            targetEntries = targetEntries,
                            password = enteredPassword,
                            notificationHostState = notificationHostState,
                            onProgress = { extractionProgress = it }
                        )
                    }
                }
            )
        }

        // Password Prompt Dialog for opening encrypted files
        pendingPasswordViewEntry?.let { entryToView ->
            ArchivePasswordDialog(
                archiveName = entryToView.name.substringAfterLast('/'),
                errorMessage = passwordViewError,
                confirmText = stringResource(R.string.action_open),
                onDismiss = {
                    pendingPasswordViewEntry = null
                    passwordViewError = null
                },
                onConfirm = { enteredPass ->
                    pendingPasswordViewEntry = null
                    passwordViewError = null
                    sessionPassword = enteredPass
                    openEntryFile(entryToView, enteredPass)
                }
            )
        }

        // Show extraction progress
        extractionProgress?.let { progress ->
            if (progress.state == ExtractionState.EXTRACTING ||
                progress.state == ExtractionState.STARTED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.archive_extracting),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progress.currentFile,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress.percentage / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${progress.percentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function for extraction
private suspend fun extractArchive(
    viewModel: ArchiveViewModel,
    archivePath: String,
    outputDir: String,
    targetEntries: List<String>? = null,
    password: String? = null,
    notificationHostState: id.xms.xarchiver.ui.components.NotificationHostState,
    onProgress: (ExtractionProgress?) -> Unit
) {
    try {
        // Create directory if it doesn't exist
        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        // Start extraction
        viewModel.extractArchive(archivePath, outputDir, targetEntries, password).collect { progress ->
            onProgress(progress)
            when (progress.state) {
                ExtractionState.COMPLETED -> {
                    onProgress(null) // Clear progress
                    notificationHostState.showNotification("Extraction completed! Files saved to: $outputDir", NotificationType.SUCCESS)
                }
                ExtractionState.ERROR -> {
                    onProgress(null) // Clear progress
                    val err = progress.error ?: progress.currentFile
                    notificationHostState.showNotification("Extraction failed: $err", NotificationType.ERROR)
                }
                else -> {
                    // Still extracting, progress continues
                }
            }
        }
    } catch (e: Exception) {
        onProgress(null) // Clear progress
        notificationHostState.showNotification("Extraction failed: ${e.message}", NotificationType.ERROR)
    }
}

@Composable
fun ArchiveEntryItem(
    entry: ArchiveEntry,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            // Fix: Handle folder names with trailing slash
            val displayName = entry.name.trimEnd('/').substringAfterLast('/')
            Text(
                text = if (displayName.isNotEmpty()) displayName else entry.name.trimEnd('/'),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!entry.isDirectory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(entry.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if ((entry as? id.xms.xarchiver.core.archive.XArchiveEntry)?.isEncrypted == true) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats file size to a human-readable string
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

/**
 * Virtual folder entry for folders that don't have explicit entries in the archive
 */
private class VirtualFolderEntry(private val folderPath: String) : ArchiveEntry {
    override fun getName(): String = folderPath
    override fun getSize(): Long = 0
    override fun isDirectory(): Boolean = true
    override fun getLastModifiedDate(): java.util.Date? = null
}
