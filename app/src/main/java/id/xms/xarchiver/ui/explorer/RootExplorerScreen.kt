package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.core.root.RootFileService
import java.util.*

@Composable
fun RootExplorerScreen(path: String, navController: NavController) {
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    LaunchedEffect(path) {
        files = RootFileService.listDirectory(path)
    }

    Scaffold(
        topBar = {
            TopAppBarWithBreadcrumb(
                path = path,
                onCrumbClick = { crumb -> navController.navigate("root_explorer/${Uri.encode(crumb)}") { launchSingleTop = true } },
                onBack = { navController.navigateUp() }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.name) },
                    supportingContent = {
                        if (!file.isDirectory) Text("${file.size.humanReadable()} • ${Date(file.lastModified).toLocaleString()}")
                    },
                    leadingContent = {
                        Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        if (file.isDirectory) {
                            navController.navigate("root_explorer/${Uri.encode(file.path)}")
                        } else {
                        }
                    }
                )
                Divider()
            }
        }
    }
}
