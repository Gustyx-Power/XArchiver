package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.FileService
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import androidx.compose.ui.platform.LocalContext
import id.xms.xarchiver.core.archive.ArchiveManager
import id.xms.xarchiver.core.install.ApkInstaller
import java.util.*
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archiveManager = remember { ArchiveManager(context) }

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var pendingApk by remember { mutableStateOf<File?>(null) } // state dialog
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var renamingFile by remember { mutableStateOf<FileItem?>(null) }
    var deletingFile by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        files = FileService.listDirectory(path)
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            topBar = {
                TopAppBarWithBreadcrumb(
                    path = path,
                    onCrumbClick = { crumbPath ->
                        navController.navigate("explorer/${Uri.encode(crumbPath)}") {
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.navigateUp() }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(files) { file ->
                    ListItem(
                        headlineContent = { Text(file.name, color = MaterialTheme.colorScheme.onBackground) },
                        supportingContent = {
                            if (!file.isDirectory) {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text("${file.size.humanReadable()} • ${dateFormat.format(Date(file.lastModified))}", color = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                onClick = {
                                    if (file.isDirectory) {
                                        navController.navigate("explorer/${Uri.encode(file.path)}")
                                    } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                        pendingApk = File(file.path)
                                    } else {
                                        // Check if this is an archive file we can open
                                        val fileExtension = file.name.substringAfterLast('.', "").lowercase()
                                        val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "tgz", "jar", "apk", "aar", "xapk")

                                        if (archiveExtensions.contains(fileExtension)) {
                                            // Navigate to archive explorer
                                            navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                        } else {
                                            // For files that don't match known extensions, check if they might be archives
                                            scope.launch {
                                                if (archiveManager.isArchiveFile(file.path)) {
                                                    navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                                } else {
                                                    // Handle regular files (future file viewer implementation)
                                                }
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    // Allow long click on both files AND directories
                                    selectedFile = file
                                }
                            )
                    )
                    HorizontalDivider()
                }
            }
        }

        // Dialog for file/directory actions
        selectedFile?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFile = null },
                title = {
                    Text(
                        text = if (file.isDirectory) "Folder Options" else "File Options",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Choose an action for ${if (file.isDirectory) "folder" else "file"} \"${file.name}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Rename option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    renamingFile = file
                                    renameText = file.name
                                    showRenameDialog = true
                                    selectedFile = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Rename",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Delete option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    deletingFile = file
                                    showDeleteDialog = true
                                    selectedFile = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Extract option for archive files (only for files, not directories)
                        if (!file.isDirectory) {
                            val fileExtension = file.name.substringAfterLast('.', "").lowercase()
                            val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "tgz", "jar", "apk", "aar",
                                "xapk", "xz", "bz2","iso","payload","bin","img","tar.gz","tar.xz","tar.bz2","tar.zst","zst"
                            , "cpio","ar","deb","rpm","dmg","payload.bin")

                            if (archiveExtensions.contains(fileExtension)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFile = null
                                            navController.navigate("archive_explorer/${Uri.encode(file.path)}")
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Unarchive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Extract Archive",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedFile = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Rename dialog
        if (showRenameDialog && renamingFile != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false; renamingFile = null },
                title = {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Rename ${if (renamingFile!!.isDirectory) "Folder" else "File"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a new name for the ${if (renamingFile!!.isDirectory) "folder" else "file"}:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("New name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameText.isNotBlank()) {
                                FileService.renameFile(renamingFile!!.path, renameText)
                                files = FileService.listDirectory(path)
                            }
                            showRenameDialog = false
                            renamingFile = null
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRenameDialog = false; renamingFile = null },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Delete dialog
        if (showDeleteDialog && deletingFile != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; deletingFile = null },
                title = {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Delete ${if (deletingFile!!.isDirectory) "Folder" else "File"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = if (deletingFile!!.isDirectory) {
                                "Are you sure you want to delete this folder and all its contents?"
                            } else {
                                "Are you sure you want to delete this file?"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (deletingFile!!.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "\"${deletingFile!!.name}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (deletingFile!!.isDirectory) {
                            Text(
                                text = "⚠️ This will permanently delete the folder and all files inside it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "This action cannot be undone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            FileService.deleteFile(deletingFile!!.path)
                            files = FileService.listDirectory(path)
                            showDeleteDialog = false
                            deletingFile = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false; deletingFile = null },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        // APK install dialog
        pendingApk?.let { apkFile ->
            AlertDialog(
                onDismissRequest = { pendingApk = null },
                title = {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Install APK",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Do you want to install this application?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = apkFile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "APK Package",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚠️ Only install applications from trusted sources.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ApkInstaller.installApk(context, apkFile)
                            pendingApk = null
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Install")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingApk = null },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithBreadcrumb(
    path: String,
    onCrumbClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    TopAppBar(
        title = {
            Row(Modifier.horizontalScroll(scrollState)) {
                // back icon (go up one level)
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                // Build crumbs
                val normalized = path.trimEnd('/')
                val parts = if (normalized.isEmpty() || normalized == "/") listOf("/") else normalized.split("/").filter { it.isNotEmpty() }
                var acc = ""
                if (parts.isEmpty()) {
                    // root
                    BreadcrumbChip("/", "/") { onCrumbClick(it) }
                } else {
                    // first crumb maybe "/sdcard" etc.
                    parts.forEachIndexed { index, part ->
                        acc = "$acc/$part"
                        BreadcrumbChip(part, acc) { onCrumbClick(it) }
                        if (index < parts.size - 1) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.padding(horizontal = 6.dp))
                        }
                    }
                }
            }
        },
        actions = {}
    )
}



@Composable
private fun BreadcrumbChip(label: String, pathForClick: String, onClick: (String) -> Unit) {
    TextButton(onClick = { onClick(pathForClick) }, modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
