package id.xms.xarchiver.ui.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.core.Category
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.Shortcut
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.ui.theme.ThemePreferences
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.*
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
fun MiuixHomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isRootAccessEnabled by themePreferences.isRootAccessEnabled.collectAsState(initial = false)
    var currentTab by remember { mutableStateOf(HomeTab.Files) }
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(currentTab) {
        if (currentTab == HomeTab.Recent) {
            viewModel.loadRecentFiles()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.app_name),
                largeTitle = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(MiuixIcons.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Surface(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(100),
                        blurRadiusX = 30f,
                        blurRadiusY = 30f
                    ),
                color = MiuixTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(100)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRecent = currentTab == HomeTab.Recent
                    Button(
                        onClick = { currentTab = HomeTab.Recent },
                        modifier = Modifier.padding(end = 4.dp),
                        colors = if (isRecent) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(color = androidx.compose.ui.graphics.Color.Transparent, contentColor = MiuixTheme.colorScheme.onSurface)
                    ) {
                        androidx.compose.material3.Icon(
                            if (isRecent) MiuixIcons.Recent else MiuixIcons.Recent, 
                            contentDescription = null, 
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                            tint = if (isRecent) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                        )
                        Text(stringResource(R.string.fab_recent))
                    }

                    val isFiles = currentTab == HomeTab.Files
                    Button(
                        onClick = { currentTab = HomeTab.Files },
                        modifier = Modifier.padding(start = 4.dp),
                        colors = if (isFiles) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(color = androidx.compose.ui.graphics.Color.Transparent, contentColor = MiuixTheme.colorScheme.onSurface)
                    ) {
                        androidx.compose.material3.Icon(
                            if (isFiles) MiuixIcons.Folder else MiuixIcons.Folder, 
                            contentDescription = null, 
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                            tint = if (isFiles) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                        )
                        Text(stringResource(R.string.fab_files))
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).layerBackdrop(backdrop)
        ) {
            item {
                if (currentTab == HomeTab.Files) {
                    MiuixFilesContent(viewModel, navController, isRootAccessEnabled, backdrop)
                } else {
                    MiuixRecentContent(viewModel, navController, backdrop)
                }
                Spacer(modifier = Modifier.height(100.dp)) // padding for bottom FAB
            }
        }
    }
}

@Composable
fun MiuixFilesContent(viewModel: HomeViewModel, navController: NavController, isRootAccessEnabled: Boolean, backdrop: LayerBackdrop) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Section
        val storages = viewModel.storages.value
        val isRefreshing = viewModel.isRefreshing.value
        
        Column {
            SmallTitle(text = stringResource(R.string.home_storage))
            Card(modifier = Modifier.fillMaxWidth().textureBlur(backdrop = backdrop, shape = RoundedCornerShape(16.dp), blurRadiusX = 20f, blurRadiusY = 20f), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))) {
                if (isRefreshing) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Detecting storage...", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                } else {
                    val allStorages = storages.toMutableList<Any>()
                    if (isRootAccessEnabled) {
                        allStorages.add("ROOT")
                    }
                    
                    allStorages.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        
                        if (item is id.xms.xarchiver.core.StorageInfo) {
                            val icon = when {
                                item.label.contains("USB", ignoreCase = true) -> MiuixIcons.Folder
                                item.label.contains("SD", ignoreCase = true) -> MiuixIcons.Folder
                                else -> MiuixIcons.Phone
                            }
                            MiuixListItem(
                                title = item.label,
                                subtitle = "${(item.total - item.used).humanReadable()} free / ${item.total.humanReadable()}",
                                icon = icon,
                                onClick = { navController.navigate("explorer/${Uri.encode(item.path)}") }
                            )
                        } else {
                            val rootInfo = viewModel.rootStorageInfo
                            MiuixListItem(
                                title = "Root Storage",
                                subtitle = "${(rootInfo.total - rootInfo.used).humanReadable()} free / ${rootInfo.total.humanReadable()}",
                                icon = MiuixIcons.Folder,
                                onClick = { navController.navigate("root_explorer/${Uri.encode("/")}") }
                            )
                        }
                    }
                }
            }
        }

        // Categories Section
        val categories = viewModel.categories.value
        if (categories.isNotEmpty()) {
            Column {
                SmallTitle(text = stringResource(R.string.home_categories))
                Card(modifier = Modifier.fillMaxWidth().textureBlur(backdrop = backdrop, shape = RoundedCornerShape(16.dp), blurRadiusX = 20f, blurRadiusY = 20f), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            categories.take(3).forEach { category ->
                                MiuixCategoryItem(category) { navController.navigate("category_explorer/${Uri.encode(category.name)}") }
                            }
                        }
                        if (categories.size > 3) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                categories.drop(3).take(3).forEach { category ->
                                    MiuixCategoryItem(category) { navController.navigate("category_explorer/${Uri.encode(category.name)}") }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Access Section
        val shortcuts = viewModel.shortcuts
        if (shortcuts.isNotEmpty()) {
            Column {
                SmallTitle(text = stringResource(R.string.home_quick_access))
                Card(modifier = Modifier.fillMaxWidth().textureBlur(backdrop = backdrop, shape = RoundedCornerShape(16.dp), blurRadiusX = 20f, blurRadiusY = 20f), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))) {
                    shortcuts.forEachIndexed { index, shortcut ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        MiuixListItem(
                            title = shortcut.name,
                            subtitle = shortcut.path,
                            icon = MiuixIcons.Folder,
                            onClick = { navController.navigate("explorer/${Uri.encode(shortcut.path)}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiuixRecentContent(viewModel: HomeViewModel, navController: NavController, backdrop: LayerBackdrop) {
    val recentFiles = viewModel.recentFilesGrouped.value
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recentFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No recent files.", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        } else {
            recentFiles.forEach { (group, items) ->
                Column {
                    SmallTitle(text = group)
                    Card(modifier = Modifier.fillMaxWidth().textureBlur(backdrop = backdrop, shape = RoundedCornerShape(16.dp), blurRadiusX = 20f, blurRadiusY = 20f), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))) {
                        items.forEachIndexed { index, file ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            MiuixListItem(
                                title = file.name,
                                subtitle = file.path,
                                icon = MiuixIcons.File,
                                onClick = { 
                                    // Open container folder
                                    navController.navigate("explorer/${Uri.encode(java.io.File(file.path).parent)}") 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiuixListItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            icon, 
            contentDescription = null, 
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                color = MiuixTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle, 
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, 
                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        androidx.compose.material3.Icon(
            MiuixIcons.ChevronForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MiuixCategoryItem(category: Category, onClick: () -> Unit) {
    val icon = when (category.name.lowercase()) {
        "images" -> MiuixIcons.Image
        "videos" -> MiuixIcons.Play
        "audio" -> MiuixIcons.Music
        "documents" -> MiuixIcons.Notes
        "archives" -> MiuixIcons.Folder
        "apk", "apps" -> MiuixIcons.Settings
        else -> MiuixIcons.Folder
    }

    Column(
        modifier = Modifier.clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MiuixTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.Icon(
                    icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            category.name,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        Text(
            "${category.count}",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    }
}
