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
import androidx.compose.foundation.Image
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.*
import id.xms.xarchiver.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isRootAccessEnabled by themePreferences.isRootAccessEnabled.collectAsState(initial = false)
    
    val listState = rememberLazyListState()
    
    // Staggered entrance animation
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Parallax effect for header based on scroll
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
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
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header removed as requested

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
                title = "Storage",
                icon = Icons.Outlined.Storage,
                trailing = {
                    if (pageCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                "${pagerState.currentPage + 1} of $pageCount",
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
            if (page < storages.size) {
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
            title = "Categories",
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
            title = "Quick Access",
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
