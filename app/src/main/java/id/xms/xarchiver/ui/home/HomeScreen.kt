package id.xms.xarchiver.ui.home

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.*
import id.xms.xarchiver.ui.theme.*
import kotlinx.coroutines.delay

enum class HomeTab {
    Recent, Files
}

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isRootAccessEnabled by themePreferences.isRootAccessEnabled.collectAsState(initial = false)
    
    var currentTab by remember { mutableStateOf(HomeTab.Files) }
    
    LaunchedEffect(currentTab) {
        if (currentTab == HomeTab.Recent) {
            viewModel.loadRecentFiles()
        }
    }
    
    val listState = rememberLazyListState()
    val recentListState = rememberLazyListState()
    
    // Staggered entrance animation
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Parallax effect for header based on scroll
    val scrollOffset by remember {
        derivedStateOf {
            if (currentTab == HomeTab.Files && listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else if (currentTab == HomeTab.Recent && recentListState.firstVisibleItemIndex == 0) {
                recentListState.firstVisibleItemScrollOffset.toFloat()
            } else {
                300f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                ModernTopBar(
                    onSettingsClick = { navController.navigate("settings") },
                    scrollOffset = scrollOffset
                )
            },
            floatingActionButton = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val isRecent = currentTab == HomeTab.Recent
                        TextButton(
                            onClick = { currentTab = HomeTab.Recent },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isRecent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isRecent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(if (isRecent) Icons.Filled.History else Icons.Outlined.History, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.fab_recent), fontWeight = if (isRecent) FontWeight.Bold else FontWeight.Normal)
                        }
                        
                        val isFiles = currentTab == HomeTab.Files
                        TextButton(
                            onClick = { currentTab = HomeTab.Files },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isFiles) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isFiles) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(if (isFiles) Icons.Filled.Folder else Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.fab_files), fontWeight = if (isFiles) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            containerColor = Color.Transparent
        ) { padding ->
            if (currentTab == HomeTab.Files) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Storage Section
                    item(key = "storage") {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(600, delayMillis = 100)) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(600, 100, EaseOutCubic)
                            )
                        ) {
                            StorageSection(
                                viewModel = viewModel,
                                navController = navController,
                                isRootAccessEnabled = isRootAccessEnabled
                            )
                        }
                    }

                    // Quick Categories - Grid layout
                    item(key = "categories") {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(600, 200, EaseOutCubic)
                            )
                        ) {
                            CategoriesSection(
                                categories = viewModel.categories.value,
                                onCategoryClick = { category ->
                                    navController.navigate("category_explorer/${Uri.encode(category.name)}")
                                }
                            )
                        }
                    }

                    // Quick Access Folders
                    item(key = "shortcuts") {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(600, delayMillis = 300)) + slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(600, 300, EaseOutCubic)
                            )
                        ) {
                            QuickAccessSection(
                                shortcuts = viewModel.shortcuts,
                                onShortcutClick = { shortcut ->
                                    navController.navigate("explorer/${Uri.encode(shortcut.path)}")
                                }
                            )
                        }
                    }
                }
            } else {
                // Recent Files Screen
                val groupedFiles = viewModel.recentFilesGrouped.value
                val isLoading = viewModel.isLoadingRecent.value
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (groupedFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tidak ada file terbaru", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        state = recentListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedFiles.forEach { (bucketName, files) ->
                            item(key = bucketName) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary, // generic
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bucketName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        // Row of files
                                        androidx.compose.foundation.lazy.LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(files.size) { index ->
                                                val file = files[index]
                                                Box(
                                                    modifier = Modifier
                                                        .size(100.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    // Simple preview
                                                    val isImageOrVideo = file.name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".mp4") || it.endsWith(".gif") || it.endsWith(".webp") }
                                                    if (isImageOrVideo) {
                                                        androidx.compose.foundation.Image(
                                                            painter = coil.compose.rememberAsyncImagePainter(java.io.File(file.path)),
                                                            contentDescription = file.name,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                                            verticalArrangement = Arrangement.Center,
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Icon(Icons.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                                            Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Removed AnimatedBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTopBar(
    onSettingsClick: () -> Unit,
    scrollOffset: Float
) {
    val elevation = (scrollOffset / 50f).coerceIn(0f, 1f)
    
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f + (elevation * 0.15f)),
        shadowElevation = (elevation * 4).dp
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "XArchiver Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            "XArchiver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            actions = {
                // Settings button
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

// WelcomeHeader removed

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StorageSection(
    viewModel: HomeViewModel,
    navController: NavController,
    isRootAccessEnabled: Boolean
) {
    val storages = viewModel.storages.value
    val isRefreshing = viewModel.isRefreshing.value
    val rootEnabled = isRootAccessEnabled
    val pageCount = storages.size + (if (rootEnabled) 1 else 0)

    // Gunakan teknik polling karena beberapa custom ROM (HyperOS/MIUI) 
    // seringkali menggunakan intent broadcast non-standar untuk USB OTG.
    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refreshStorage()
            kotlinx.coroutines.delay(2000) // Cek setiap 2 detik
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })

        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionHeader(
                title = stringResource(R.string.home_storage),
                icon = Icons.Outlined.Storage,
                trailing = {
                    if (isRefreshing) {
                        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                        androidx.compose.material3.ContainedLoadingIndicator()
                    } else if (pageCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                stringResource(R.string.home_storage_page_format, pagerState.currentPage + 1, pageCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            )
        }
        
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (isRefreshing) {
                StoragePlaceholderCard(title = stringResource(R.string.home_detecting_storage))
            } else if (page < storages.size) {
                val storage = storages[page]
                val icon = when {
                    storage.label.contains("USB", ignoreCase = true) -> Icons.Outlined.Usb
                    storage.label.contains("SD", ignoreCase = true) -> Icons.Outlined.Save
                    else -> Icons.Outlined.Smartphone
                }
                
                StorageCard(
                    info = storage,
                    icon = icon,
                    onClick = {
                        navController.navigate("explorer/${Uri.encode(storage.path)}")
                    }
                )
            } else if (rootEnabled) {
                RootStorageCard(
                    info = viewModel.rootStorageInfo,
                    onOpenRoot = { rootPath ->
                        navController.navigate("root_explorer/${Uri.encode(rootPath)}")
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoriesSection(
    categories: List<id.xms.xarchiver.core.Category>,
    onCategoryClick: (id.xms.xarchiver.core.Category) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.home_categories),
            icon = Icons.Outlined.Category
        )
        
        CategoryGrid(
            categories = categories,
            onCategoryClick = onCategoryClick
        )
    }
}

@Composable
private fun QuickAccessSection(
    shortcuts: List<id.xms.xarchiver.core.Shortcut>,
    onShortcutClick: (id.xms.xarchiver.core.Shortcut) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.home_quick_access),
            icon = Icons.Outlined.FolderOpen
        )
        
        ShortcutList(
            shortcuts = shortcuts,
            onShortcutClick = onShortcutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        trailing()
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
