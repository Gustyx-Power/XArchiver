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
                modifier = Modifier.fillMaxWidth(),
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
