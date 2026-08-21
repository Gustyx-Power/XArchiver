package id.xms.xarchiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.xms.xarchiver.core.StorageInfo
import id.xms.xarchiver.core.StorageUtils
import id.xms.xarchiver.ui.theme.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    onDismissRequest: () -> Unit,
    onFolderSelected: (String) -> Unit,
    title: String = "Select Folder"
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isRootAccessEnabled by themePreferences.isRootAccessEnabled.collectAsState(initial = false)
    var currentPath by remember { mutableStateOf<String?>(null) }
    var partitions by remember { mutableStateOf<List<StorageInfo>>(emptyList()) }
    var folders by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load partitions initially
    LaunchedEffect(isRootAccessEnabled) {
        withContext(Dispatchers.IO) {
            val storages = StorageUtils.getAllStorage(context).toMutableList()
            if (isRootAccessEnabled) {
                storages.add(StorageUtils.getRootStorageInfo())
            }
            partitions = storages
        }
    }

    // Load folders when currentPath changes
    LaunchedEffect(currentPath) {
        if (currentPath != null) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val dir = File(currentPath!!)
                    val children = dir.listFiles()
                    if (children != null) {
                        folders = children.filter { it.isDirectory && !it.isHidden }.sortedBy { it.name.lowercase() }
                    } else {
                        folders = emptyList()
                    }
                } catch (e: Exception) {
                    folders = emptyList()
                }
            }
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentPath != null) {
                                Text(
                                    text = currentPath!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (currentPath != null) {
                            IconButton(
                                onClick = {
                                    val parent = File(currentPath!!).parent
                                    if (parent == null || partitions.any { it.path == currentPath }) {
                                        currentPath = null // Go back to partitions
                                    } else {
                                        currentPath = parent
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (currentPath == null) {
                        // Show partitions
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(partitions) { partition ->
                                StorageItem(
                                    info = partition,
                                    onClick = { currentPath = partition.path }
                                )
                            }
                        }
                    } else {
                        // Show folders
                        if (folders.isEmpty()) {
                            Text(
                                "Empty folder",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(folders) { folder ->
                                    FolderItem(
                                        folder = folder,
                                        onClick = { currentPath = folder.absolutePath }
                                    )
                                }
                            }
                        }
                    }
                }

                // BottomBar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { currentPath?.let { onFolderSelected(it) } },
                            enabled = currentPath != null
                        ) {
                            Text("Select This Folder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageItem(info: StorageInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(info.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(info.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderItem(folder: File, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(folder.name, style = MaterialTheme.typography.bodyLarge)
    }
}
