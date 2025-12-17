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
import id.xms.xarchiver.ui.theme.*
import id.xms.xarchiver.ui.components.ThemeSettingsDialog
import kotlinx.coroutines.delay

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    var showThemeDialog by remember { mutableStateOf(false) }
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
        // Animated background circles for premium feel
        AnimatedBackground()
        
        Scaffold(
            topBar = {
                ModernTopBar(
                    onThemeClick = { showThemeDialog = true },
                    onAboutClick = { navController.navigate("about") },
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
                // Welcome Header with parallax effect
                item(key = "header") {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(600)) + slideInVertically(
                            initialOffsetY = { -50 },
                            animationSpec = tween(600, easing = EaseOutCubic)
                        )
                    ) {
                        WelcomeHeader(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = scrollOffset * 0.3f
                                    alpha = 1f - (scrollOffset / 400f).coerceIn(0f, 0.5f)
                                }
                        )
                    }
                }

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
                            navController = navController
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

    // Theme dialog
    if (showThemeDialog) {
        ThemeSettingsDialog(
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Gradient circle 1
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * offset1, size.height * 0.2f),
                        radius = size.width * 0.6f
                    )
                )
                // Gradient circle 2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientEnd.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * offset2, size.height * 0.7f),
                        radius = size.width * 0.5f
                    )
                )
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTopBar(
    onThemeClick: () -> Unit,
    onAboutClick: () -> Unit,
    scrollOffset: Float
) {
    val elevation = (scrollOffset / 50f).coerceIn(0f, 1f)
    
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = elevation * 0.95f),
        shadowElevation = (elevation * 8).dp
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern app icon with gradient background
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientStart, GradientEnd)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            "XArchiver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "File Manager",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            actions = {
                // Search button
                IconButton(onClick = { /* TODO: Search */ }) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // About button
                IconButton(onClick = onAboutClick) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Settings/Theme button
                IconButton(onClick = onThemeClick) {
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

@Composable
private fun WelcomeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        // Greeting based on time
        val greeting = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                hour < 12 -> "Good Morning"
                hour < 17 -> "Good Afternoon"
                else -> "Good Evening"
            }
        }
        
        Text(
            greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            "Manage your files and archives",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StorageSection(
    viewModel: HomeViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Storage",
            icon = Icons.Outlined.Storage
        )
        
        val internal = viewModel.storages.firstOrNull()
        
        // Internal storage card
        if (internal != null) {
            StorageCard(
                info = internal,
                onClick = {
                    navController.navigate("explorer/${Uri.encode(internal.path)}")
                }
            )
        } else {
            StoragePlaceholderCard(title = "Device Storage")
        }
        
        // Root storage card
        RootStorageCard(
            onOpenRoot = { rootPath ->
                navController.navigate("root_explorer/${Uri.encode(rootPath)}")
            }
        )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
