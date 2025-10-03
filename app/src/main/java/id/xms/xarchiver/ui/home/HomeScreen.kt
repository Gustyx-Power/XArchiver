package id.xms.xarchiver.ui.home

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Files") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // ROW: Device storage (kiri) + Root storage (kanan)
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    val internal = viewModel.storages.firstOrNull()
                    Column(Modifier.weight(1f)) {
                        if (internal != null) {
                            StorageCard(
                                info = internal,
                                onClick = { navController.navigate("explorer/${Uri.encode(internal.path)}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            StoragePlaceholderCard("Device storage", Modifier.fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        RootStorageCard(
                            onOpenRoot = { rootPath ->
                                navController.navigate("root_explorer/${Uri.encode(rootPath)}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Categories
            item {
                Text("Categories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                CategoryGrid(
                    categories = viewModel.categories,
                    onCategoryClick = { /* no-op or navigate */ },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .height(160.dp)
                )

            }

            item { Spacer(Modifier.height(12.dp)) }

            // Shortcuts
            item {
                val base = viewModel.storages.firstOrNull()?.path ?: "/sdcard"
                ShortcutList(
                    shortcuts = viewModel.shortcuts,
                    onShortcutClick = { sc ->
                        // Implementasi aman, tanpa TODO():
                        when (sc.name.lowercase()) {
                            "downloads" -> navController.navigate("explorer/${Uri.encode("$base/Download")}")
                            "bluetooth" -> navController.navigate("explorer/${Uri.encode("$base/Bluetooth")}")
                            "whatsapp" -> navController.navigate("explorer/${Uri.encode("$base/Android/media/com.whatsapp/WhatsApp/Media")}")
                            "telegram" -> navController.navigate("explorer/${Uri.encode("$base/Telegram")}")
                            else -> { /* no-op */ }
                        }
                    }
                )
            }
        }
    }
}
