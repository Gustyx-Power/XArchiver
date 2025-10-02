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
import id.xms.xarchiver.core.install.ApkInstaller
import java.util.*
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile // ktlint-disable no-wildcard-imports
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import java.text.SimpleDateFormat

@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    val context = LocalContext.current
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
                                        // TODO: open archive viewer / file viewer
                                    }
                                },
                                onLongClick = {
                                    if (!file.isDirectory) {
                                        selectedFile = file
                                    }
                                }
                            )
                    )
                    HorizontalDivider()
                }
            }
        }

        // Dialog for file actions
        selectedFile?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFile = null },
                title = { Text("File Options") },
                text = { Text("Choose an action for \"${file.name}\"") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            renamingFile = file
                            renameText = file.name
                            showRenameDialog = true
                            selectedFile = null
                        }) { Text("Rename") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            deletingFile = file
                            showDeleteDialog = true
                            selectedFile = null
                        }) { Text("Delete") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedFile = null }) { Text("Cancel") }
                }
            )
        }
        // Rename dialog
        if (showRenameDialog && renamingFile != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false; renamingFile = null },
                title = { Text("Rename File") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("New name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameText.isNotBlank()) {
                            FileService.renameFile(renamingFile!!.path, renameText)
                            files = FileService.listDirectory(path)
                        }
                        showRenameDialog = false
                        renamingFile = null
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false; renamingFile = null }) { Text("Cancel") }
                }
            )
        }
        // Delete dialog
        if (showDeleteDialog && deletingFile != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; deletingFile = null },
                title = { Text("Delete File") },
                text = { Text("Are you sure you want to delete this file?") },
                confirmButton = {
                    TextButton(onClick = {
                        FileService.deleteFile(deletingFile!!.path)
                        files = FileService.listDirectory(path)
                        showDeleteDialog = false
                        deletingFile = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; deletingFile = null }) { Text("Cancel") }
                }
            )
        }
        // APK install dialog (unchanged)
        pendingApk?.let { apkFile ->
            AlertDialog(
                onDismissRequest = { pendingApk = null },
                title = { Text("Install APK") },
                text = { Text("Do you want to install \"${apkFile.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        ApkInstaller.installApk(context, apkFile)
                        pendingApk = null
                    }) { Text("Install") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingApk = null }) { Text("Cancel") }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarWithBreadcrumb(
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
