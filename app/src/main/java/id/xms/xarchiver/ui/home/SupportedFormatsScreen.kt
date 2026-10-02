package id.xms.xarchiver.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportedFormatsScreen(navController: NavController) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(initial = ThemePreferences.isMiuiOrHyperOsDevice)

    if (isMiuixUiEnabled) {
        MiuixSupportedFormatsScreen(navController = navController)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1500f
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.formats_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Standard Archives
                item {
                    MaterialFormatCategoryCard(
                        icon = Icons.Outlined.FolderZip,
                        title = stringResource(R.string.formats_group_standard)
                    ) {
                        MaterialFormatRow(
                            extension = "ZIP (.zip)",
                            desc = stringResource(R.string.formats_desc_zip),
                            capabilities = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_encrypt))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "7Z (.7z)",
                            desc = stringResource(R.string.formats_desc_7z),
                            capabilities = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_high_ratio))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "TAR (.tar)",
                            desc = stringResource(R.string.formats_desc_tar),
                            capabilities = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract))
                        )
                    }
                }

                // Section 2: Compressed Tarballs
                item {
                    MaterialFormatCategoryCard(
                        icon = Icons.Outlined.Inventory2,
                        title = stringResource(R.string.formats_group_tarballs)
                    ) {
                        MaterialFormatRow(
                            extension = "TAR.GZ (.tar.gz, .tgz)",
                            desc = stringResource(R.string.formats_desc_targz),
                            capabilities = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "TAR.BZ2 (.tar.bz2, .tbz2)",
                            desc = stringResource(R.string.formats_desc_tarbz2),
                            capabilities = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "TAR.XZ (.tar.xz, .txz)",
                            desc = stringResource(R.string.formats_desc_tarxz),
                            capabilities = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract))
                        )
                    }
                }

                // Section 3: Single-File Streams
                item {
                    MaterialFormatCategoryCard(
                        icon = Icons.Outlined.Compress,
                        title = stringResource(R.string.formats_group_streams)
                    ) {
                        MaterialFormatRow(
                            extension = "GZIP (.gz)",
                            desc = stringResource(R.string.formats_desc_gz),
                            capabilities = listOf(stringResource(R.string.formats_cap_decompress))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "BZIP2 (.bz2)",
                            desc = stringResource(R.string.formats_desc_bz2),
                            capabilities = listOf(stringResource(R.string.formats_cap_decompress))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "XZ (.xz)",
                            desc = stringResource(R.string.formats_desc_xz),
                            capabilities = listOf(stringResource(R.string.formats_cap_decompress))
                        )
                    }
                }

                // Section 4: Read-Only & Specialized
                item {
                    MaterialFormatCategoryCard(
                        icon = Icons.Outlined.Terminal,
                        title = stringResource(R.string.formats_group_specialized_alt)
                    ) {
                        MaterialFormatRow(
                            extension = "RAR (.rar)",
                            desc = stringResource(R.string.formats_desc_rar),
                            capabilities = listOf(stringResource(R.string.formats_cap_extract))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "APK / APKS / XAPK",
                            desc = stringResource(R.string.formats_desc_apk),
                            capabilities = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_inspect))
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialFormatRow(
                            extension = "PAYLOAD.BIN",
                            desc = stringResource(R.string.formats_desc_payload),
                            capabilities = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_rust_native))
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MaterialFormatCategoryCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun MaterialFormatRow(
    extension: String,
    desc: String,
    capabilities: List<String>
) {
    val createLabel = stringResource(R.string.formats_cap_create)
    val extractLabel = stringResource(R.string.formats_cap_extract)
    val decompressLabel = stringResource(R.string.formats_cap_decompress)
    val encryptLabel = stringResource(R.string.formats_cap_encrypt)
    val rustNativeLabel = stringResource(R.string.formats_cap_rust_native)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = extension,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                capabilities.forEach { cap ->
                    val (bg, textColor) = when (cap) {
                        "Create", createLabel -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        "Extract", "Decompress", extractLabel, decompressLabel -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                        "Encrypt", encryptLabel -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        "Rust Native", rustNativeLabel -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cap,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
