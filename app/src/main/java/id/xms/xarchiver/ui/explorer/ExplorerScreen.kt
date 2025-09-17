package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.foundation.clickable
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
import java.io.File
import java.util.*
import androidx.compose.material.ripple.rememberRipple

@Composable
fun ExplorerScreen(path: String, navController: NavController) {
    // load file listing when path changes
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    LaunchedEffect(path) {
        files = FileService.listDirectory(path)
    }

    Scaffold(
        topBar = {
            TopAppBarWithBreadcrumb(path = path, onCrumbClick = { crumbPath ->
                // navigate to selected crumb (encode path)
                navController.navigate("explorer/${Uri.encode(crumbPath)}") {
                    launchSingleTop = true
                }
            }, onBack = {
                navController.navigateUp()
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.name) },
                    supportingContent = {
                        if (!file.isDirectory) {
                            Text("${file.size.humanReadable()} • ${Date(file.lastModified).toLocaleString()}")
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .clickable(
                            indication = rememberRipple(),
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            if (file.isDirectory) {
                                // navigate into directory (encode path)
                                navController.navigate("explorer/${Uri.encode(file.path)}")
                            } else {
                                // TODO: open file / archive viewer
                            }
                        }
                )
                Divider()
            }
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
    SmallTopAppBar(
        title = {
            Row(Modifier.horizontalScroll(scrollState)) {
                // back icon (go up one level)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        acc = acc + "/" + part
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
