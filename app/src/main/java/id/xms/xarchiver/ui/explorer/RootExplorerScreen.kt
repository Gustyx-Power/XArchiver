package id.xms.xarchiver.ui.explorer

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.core.root.RootFileService
import id.xms.xarchiver.ui.components.PathNavigationBar

@Composable
fun RootExplorerScreen(path: String, navController: NavController) {
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    LaunchedEffect(path) {
        files = RootFileService.listDirectory(path)
    }

    Scaffold(
        topBar = {
            PathNavigationBar(
                currentPath = path,
                onNavigate = { crumb -> 
                    navController.navigate("root_explorer/${Uri.encode(crumb)}") { 
                        launchSingleTop = true 
                    } 
                },
                onBack = { navController.navigateUp() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.path }) { file ->
                FileItemCard(
                    file = file,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = {
                        if (file.isDirectory) {
                            navController.navigate("root_explorer/${Uri.encode(file.path)}")
                        }
                    },
                    onLongClick = {}
                )
            }
        }
    }
}

