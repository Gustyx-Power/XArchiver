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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Archive Explorer",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = File(currentPath.substringBefore(":")).name +
                                  (if (nestedPath != null) " > ${File(nestedPath).name}" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            } else if (archiveEntries.isEmpty()) {
                Text(
                    text = "No entries found in this archive",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = archiveEntries,
                        key = { entry -> entry.name }
                    ) { entry ->
                        ArchiveEntryItem(
                            entry = entry,
                            onClick = {
                                // If it's a directory, we do nothing for now
                                if (!entry.isDirectory) {
                                    val entryName = entry.name
                                    val extension = entryName.substringAfterLast('.', "")

                                    // If the entry looks like it might be an archive file itself
                                    if (listOf("zip", "rar", "7z", "tar", "gz", "jar", "apk").contains(extension)) {
                                        // Navigate to view the nested archive
                                        val encodedArchivePath = Uri.encode(archivePath)
                                        val encodedEntryPath = Uri.encode(entry.name)
                                        navController.navigate("archive_explorer/$encodedArchivePath/$encodedEntryPath")
                                    } else {
                                        // View the file content
                                        scope.launch {
                                            val extractedPath = viewModel.viewArchiveEntry(archivePath, entry.name)
                                            if (extractedPath != null) {
                                                // TODO: Open the file with a file viewer
                                                Toast.makeText(context, "Opening: ${entry.name}", Toast.LENGTH_SHORT).show()
                                                // You can integrate with your file viewing mechanism here
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to extract ${entry.name}")
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
            Text(
                text = entry.name.substringAfterLast('/'),
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
