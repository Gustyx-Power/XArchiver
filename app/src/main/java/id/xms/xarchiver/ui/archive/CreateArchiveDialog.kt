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
import id.xms.xarchiver.core.archive.ArchiveFormat
import id.xms.xarchiver.core.archive.CompressionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArchiveDialog(
    selectedFilesCount: Int,
    onDismiss: () -> Unit,
    onCreate: (String, ArchiveFormat, CompressionLevel) -> Unit
) {
    var archiveName by remember { mutableStateOf("archive") }
    var selectedFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var selectedCompression by remember { mutableStateOf(CompressionLevel.NORMAL) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val extension = remember(selectedFormat) { selectedFormat.extension }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Create Archive")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        
                        val availableFormats = if (selectedFilesCount > 1) {
                            ArchiveFormat.entries.filter { it != ArchiveFormat.GZ }
                        } else {
                            ArchiveFormat.entries
                        }
                        
                        // Ensure selectedFormat is valid if it was GZ but now multiple files are selected
                        LaunchedEffect(selectedFilesCount) {
                            if (selectedFilesCount > 1 && selectedFormat == ArchiveFormat.GZ) {
                                selectedFormat = ArchiveFormat.ZIP
                            }
                        }
                        
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(selectedFormat.displayName)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableFormats.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.displayName) },
                                    onClick = { 
                                        selectedFormat = format
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Compression level (only for ZIP)
                if (selectedFormat == ArchiveFormat.ZIP) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compression", style = MaterialTheme.typography.labelLarge)
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(selectedCompression.displayName)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                CompressionLevel.entries.forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(level.displayName) },
                                        onClick = { 
                                            selectedCompression = level
                                            expanded = false 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // File count info
                HorizontalDivider()
                Text(
                    "$selectedFilesCount item(s) selected",
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (archiveName.isBlank()) {
                        error = "Please enter an archive name"
                        return@Button
                    }
                    onCreate(archiveName, selectedFormat, selectedCompression)
                },
                enabled = archiveName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
