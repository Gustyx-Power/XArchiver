package id.xms.xarchiver.ui.archive

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.xms.xarchiver.core.archive.ExtractionProgress
import id.xms.xarchiver.core.archive.ExtractionState
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showExtractionDialog by remember { mutableStateOf(false) }
    var showCustomPathDialog by remember { mutableStateOf(false) }
    var customPath by remember { mutableStateOf("") }
    var extractionProgress by remember { mutableStateOf<ExtractionProgress?>(null) }

    val archiveEntries = viewModel.archiveEntries.value
    val currentPath = remember(archivePath, nestedPath) {
        if (nestedPath != null) "$archivePath:$nestedPath" else archivePath
    }

    // Default paths
    val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    val archiveFileName = File(archivePath).nameWithoutExtension

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

    Scaffold(
        topBar = {
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showExtractionDialog = true
                }
            ) {
                Icon(Icons.Default.Unarchive, contentDescription = "Extract")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    text = if (currentPrefix.isEmpty()) "No entries found in this archive" else "Empty folder",
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
                        ArchiveEntryItem(
                            entry = entry,
                            onClick = {
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
                                        // Extract and open the file
                                        scope.launch {
                                            try {
                                                Toast.makeText(context, "Extracting ${entry.name}...", Toast.LENGTH_SHORT).show()
                                                
                                                // Extract to Downloads folder instead of app cache
                                                val fileName = entry.name.substringAfterLast('/')
                                                val outputDir = "$downloadsPath/.XArchiver_temp"
                                                val outputFile = File(outputDir, fileName)
                                                
                                                // Create output directory
                                                File(outputDir).mkdirs()
                                                
                                                // Use viewArchiveEntry to get the cached file, then copy to accessible location
                                                val extractedPath = viewModel.viewArchiveEntry(archivePath, entry.name)
                                                
                                                if (extractedPath != null) {
                                                    // Copy from cache to Downloads
                                                    val cacheFile = File(extractedPath)
                                                    if (cacheFile.exists()) {
                                                        cacheFile.copyTo(outputFile, overwrite = true)
                                                        
                                                        Toast.makeText(context, "Extracted to: ${outputFile.absolutePath}", Toast.LENGTH_SHORT).show()
                                                        
                                                        // Open with appropriate viewer
                                                        val ext = extension
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
                                                        snackbarHostState.showSnackbar("Failed to extract ${entry.name}")
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to extract ${entry.name}")
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Error: ${e.message}")
                                            }
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
                    Text(
                        text = "Extract Archive",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Choose where to extract the archive:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Extract to Downloads option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExtractionDialog = false
                                    val outputDir = "$downloadsPath/$archiveFileName"
                                    scope.launch {
                                        extractArchive(
                                            viewModel = viewModel,
                                            archivePath = archivePath,
                                            outputDir = outputDir,
                                            snackbarHostState = snackbarHostState,
                                            onProgress = { extractionProgress = it }
                                        )
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Extract Here",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Downloads/$archiveFileName/",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Extract to custom path option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExtractionDialog = false
                                    customPath = downloadsPath
                                    showCustomPathDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Extract to Custom Path",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Choose a different location",
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
                    TextButton(onClick = { showExtractionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom Path Dialog
        if (showCustomPathDialog) {
            AlertDialog(
                onDismissRequest = { showCustomPathDialog = false },
                title = { Text("Custom Extract Path") },
                text = {
                    Column {
                        Text(
                            text = "Enter the path where you want to extract the archive:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = customPath,
                            onValueChange = { customPath = it },
                            label = { Text("Path") },
                            placeholder = { Text("/storage/emulated/0/Download") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCustomPathDialog = false
                            val outputDir = "$customPath/$archiveFileName"
                            scope.launch {
                                extractArchive(
                                    viewModel = viewModel,
                                    archivePath = archivePath,
                                    outputDir = outputDir,
                                    snackbarHostState = snackbarHostState,
                                    onProgress = { extractionProgress = it }
                                )
                            }
                        }
                    ) {
                        Text("Extract")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomPathDialog = false }) {
                        Text("Cancel")
                    }
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
                                text = "Extracting...",
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
    snackbarHostState: SnackbarHostState,
    onProgress: (ExtractionProgress?) -> Unit
) {
    try {
        // Create directory if it doesn't exist
        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        // Start extraction
        viewModel.extractArchive(archivePath, outputDir).collect { progress ->
            onProgress(progress)
            when (progress.state) {
                ExtractionState.COMPLETED -> {
                    onProgress(null) // Clear progress
                    snackbarHostState.showSnackbar("Extraction completed! Files saved to: $outputDir")
                }
                ExtractionState.ERROR -> {
                    onProgress(null) // Clear progress
                    snackbarHostState.showSnackbar("Extraction failed: ${progress.currentFile}")
                }
                else -> {
                    // Still extracting, progress continues
                }
            }
        }
    } catch (e: Exception) {
        onProgress(null) // Clear progress
        snackbarHostState.showSnackbar("Extraction failed: ${e.message}")
    }
}

@Composable
fun ArchiveEntryItem(
    entry: ArchiveEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatFileSize(entry.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
