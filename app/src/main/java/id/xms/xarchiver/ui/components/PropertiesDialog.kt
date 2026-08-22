package id.xms.xarchiver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import id.xms.xarchiver.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.humanReadable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PropertiesDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    val file = remember { File(filePath) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
    
    // Calculate directory size if needed
    var directorySize by remember { mutableStateOf<Long?>(null) }
    var fileCount by remember { mutableStateOf(0) }
    var folderCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(filePath) {
        if (file.isDirectory) {
            var size = 0L
            var files = 0
            var folders = 0
            
            file.walkTopDown().forEach { f ->
                if (f.isFile) {
                    size += f.length()
                    files++
                } else if (f.isDirectory && f != file) {
                    folders++
                }
            }
            
            directorySize = size
            fileCount = files
            folderCount = folders
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.dialog_properties_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name
                PropertyRow(
                    icon = Icons.Default.Label,
                    label = stringResource(R.string.property_name),
                    value = file.name
                )
                
                HorizontalDivider()
                
                // Path
                PropertyRow(
                    icon = Icons.Default.FolderOpen,
                    label = stringResource(R.string.property_location),
                    value = file.parent ?: "/"
                )
                
                HorizontalDivider()
                
                // Size
                val displaySize = if (file.isDirectory) {
                    directorySize?.humanReadable() ?: stringResource(R.string.property_calculating)
                } else {
                    file.length().humanReadable()
                }
                PropertyRow(
                    icon = Icons.Default.Storage,
                    label = stringResource(R.string.property_size),
                    value = displaySize
                )
                
                // For directories, show file/folder count
                if (file.isDirectory) {
                    HorizontalDivider()
                    PropertyRow(
                        icon = Icons.Default.Folder,
                        label = stringResource(R.string.property_contents),
                        value = stringResource(R.string.property_contents_format, fileCount, folderCount)
                    )
                }
                
                HorizontalDivider()
                
                // Last Modified
                PropertyRow(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.property_modified),
                    value = dateFormatter.format(Date(file.lastModified()))
                )
                
                HorizontalDivider()
                
                // Permissions
                val readStr = stringResource(R.string.property_read)
                val writeStr = stringResource(R.string.property_write)
                val execStr = stringResource(R.string.property_execute)
                val noneStr = stringResource(R.string.property_none)
                val permissions = buildString {
                    if (file.canRead()) append(readStr)
                    if (file.canWrite()) append(writeStr)
                    if (file.canExecute()) append(execStr)
                }.trim().ifEmpty { noneStr }
                
                PropertyRow(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.property_permissions),
                    value = permissions
                )
                
                // File extension (for files only)
                if (file.isFile && file.extension.isNotEmpty()) {
                    HorizontalDivider()
                    PropertyRow(
                        icon = Icons.Default.Extension,
                        label = stringResource(R.string.property_type),
                        value = ".${file.extension.uppercase()}"
                    )
                }
                
                // Hidden file indicator
                if (file.name.startsWith(".")) {
                    HorizontalDivider()
                    PropertyRow(
                        icon = Icons.Default.VisibilityOff,
                        label = stringResource(R.string.property_hidden),
                        value = stringResource(R.string.property_yes)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun PropertyRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
