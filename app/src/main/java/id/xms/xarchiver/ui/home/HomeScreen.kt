package id.xms.xarchiver.ui.home

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.*

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("XArchiver", color = MaterialTheme.colorScheme.onBackground) }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            items(viewModel.storages.size) { index ->
                val storage = viewModel.storages[index]
                StorageCard(storage) {
                    navController.navigate("explorer/${Uri.encode("/sdcard")}")
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { CategoryGrid(viewModel.categories) }
            item { Spacer(Modifier.height(8.dp)) }
            item { ShortcutList(viewModel.shortcuts) }
        }
    }
}
