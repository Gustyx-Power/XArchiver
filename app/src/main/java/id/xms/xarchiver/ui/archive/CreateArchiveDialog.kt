package id.xms.xarchiver.ui.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.archive.ArchiveCreator
import id.xms.xarchiver.core.archive.ArchiveCreationProgress
import id.xms.xarchiver.core.archive.ArchiveFormat
import id.xms.xarchiver.core.archive.CompressionLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArchiveDialog(
    selectedFiles: List<String>,
    outputDirectory: String,
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var archiveName by remember { mutableStateOf("archive") }
    var selectedFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var selectedCompression by remember { mutableStateOf(CompressionLevel.NORMAL) }
    var isCreating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<ArchiveCreationProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val extension = remember(selectedFormat) { selectedFormat.extension }
    val fullPath = remember(archiveName, extension, outputDirectory) {
        "$outputDirectory/$archiveName.$extension"
    }
    
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Create Archive")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isCreating) {
                    // Progress UI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            progress = { (progress?.percentage ?: 0) / 100f },
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${progress?.percentage ?: 0}%",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            progress?.currentFile ?: "Preparing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${progress?.filesProcessed ?: 0} / ${progress?.totalFiles ?: selectedFiles.size} files",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Archive name input
                    OutlinedTextField(
                        value = archiveName,
                        onValueChange = { archiveName = it.replace(Regex("[\\\\/:*?\"<>|]"), "") },
                        label = { Text("Archive name") },
                        suffix = { Text(".$extension") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Format selection
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    Column(Modifier.selectableGroup()) {
                        ArchiveFormat.entries.forEach { format ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedFormat == format,
                                        onClick = { selectedFormat = format },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFormat == format,
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(format.displayName)
                                    Text(
                                        ".${format.extension}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Compression level (only for ZIP)
                    if (selectedFormat == ArchiveFormat.ZIP) {
                        HorizontalDivider()
                        Text("Compression Level", style = MaterialTheme.typography.labelLarge)
                        
                        Column(Modifier.selectableGroup()) {
                            CompressionLevel.entries.forEach { level ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedCompression == level,
                                            onClick = { selectedCompression = level },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedCompression == level,
                                        onClick = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(level.displayName)
                                }
                            }
                        }
                    }
                    
                    // File count info
                    HorizontalDivider()
                    Text(
                        "${selectedFiles.size} item(s) selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Error message
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isCreating) {
                Button(
                    onClick = {
                        if (archiveName.isBlank()) {
                            error = "Please enter an archive name"
                            return@Button
                        }
                        
                        isCreating = true
                        error = null
                        
                        scope.launch {
                            try {
                                ArchiveCreator.createArchive(
                                    outputPath = fullPath,
                                    files = selectedFiles,
                                    basePath = if (selectedFiles.size == 1) {
                                        java.io.File(selectedFiles.first()).parentFile?.absolutePath ?: ""
                                    } else {
                                        // Find common parent
                                        findCommonParent(selectedFiles)
                                    },
                                    compressionLevel = selectedCompression
                                ).collect { prog ->
                                    progress = prog
                                }
                                
                                onComplete(fullPath)
                            } catch (e: Exception) {
                                error = e.message
                                isCreating = false
                            }
                        }
                    },
                    enabled = archiveName.isNotBlank()
                ) {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            if (!isCreating) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
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
