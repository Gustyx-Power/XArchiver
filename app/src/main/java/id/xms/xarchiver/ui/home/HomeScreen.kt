package id.xms.xarchiver.ui.home

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.xms.xarchiver.ui.theme.*
import id.xms.xarchiver.ui.components.ThemeSettingsDialog

@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    var showThemeDialog by remember { mutableStateOf(false) }

    // Animation for the entire screen entrance
    val screenAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "screenAlpha"
    )

    // Staggered animation for content sections
    LaunchedEffect(Unit) {
        // Trigger entrance animations
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "XArchiver",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showThemeDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Theme Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .alpha(screenAlpha)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enhanced welcome header with animated entry
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 28.dp)
                ) {
                    Text(
                        "Welcome back!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Manage your files and archives with ease",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Enhanced storage section with modern card design
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Storage Overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Storage cards with enhanced spacing and layout
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val internal = viewModel.storages.firstOrNull()

                            // Internal storage card (main focus)
                            if (internal != null) {
                                StorageCard(
                                    info = internal,
                                    onClick = {
                                        navController.navigate("explorer/${Uri.encode(internal.path)}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                StoragePlaceholderCard(
                                    title = "Device Storage",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Root storage card
                            RootStorageCard(
                                onOpenRoot = { rootPath ->
                                    navController.navigate("root_explorer/${Uri.encode(rootPath)}")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Enhanced categories section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Quick Access",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Browse files by category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(Modifier.height(20.dp))

                        CategoryGrid(
                            categories = viewModel.categories,
                            onCategoryClick = { /* no-op or navigate */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Enhanced shortcuts section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        ShortcutList(
                            shortcuts = viewModel.shortcuts,
                            onShortcutClick = { shortcut ->
                                // Navigate to a default path since Shortcut doesn't have path property
                                val defaultPath = viewModel.storages.firstOrNull()?.path ?: "/sdcard"
                                navController.navigate("explorer/${Uri.encode(defaultPath)}")
                            },
                            modifier = Modifier.fillMaxWidth()
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
