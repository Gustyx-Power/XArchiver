package id.xms.xarchiver.ui.home

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSpecsScreen(navController: NavController) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(initial = ThemePreferences.isMiuiOrHyperOsDevice)

    if (isMiuixUiEnabled) {
        MiuixAppSpecsScreen(navController = navController)
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    val packageName = remember {
        context.packageName ?: "id.xms.xarchiver"
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
                                stringResource(R.string.specs_title),
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
                // Section 1: Application Identity
                item {
                    MaterialSpecsSectionCard(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.specs_section_identity)
                    ) {
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_app_name),
                            value = "XArchiver"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_package_id),
                            value = packageName,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(packageName))
                                Toast.makeText(context, context.getString(R.string.about_copied_format, packageName), Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_version),
                            value = "v$versionName",
                            onClick = {
                                clipboardManager.setText(AnnotatedString("v$versionName"))
                                Toast.makeText(context, context.getString(R.string.about_copied_format, "v$versionName"), Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_build_env),
                            value = "Android Gradle Plugin 8.3+"
                        )
                    }
                }

                // Section 2: Core Archiving & Compression Engines
                item {
                    MaterialSpecsSectionCard(
                        icon = Icons.Outlined.FolderZip,
                        title = stringResource(R.string.specs_section_core_engines)
                    ) {
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_primary_compressor),
                            value = "Apache Commons Compress 1.26.1"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_payload_engine),
                            value = "Rust Native Engine (libpayload_parser.so via JNI)",
                            onClick = {
                                clipboardManager.setText(AnnotatedString("Rust Native Engine (libpayload_parser.so via JNI)"))
                                Toast.makeText(context, context.getString(R.string.about_copied_format, "Rust Native Engine (libpayload_parser.so via JNI)"), Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_io_buffering),
                            value = "Apache Commons IO 2.15.1"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_lzma_engine),
                            value = "Tukaani XZ for Java 1.9"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_concurrency),
                            value = "Kotlin Coroutines & Flow (Dispatchers.IO)"
                        )
                    }
                }

                // Section 3: UI & Architecture
                item {
                    MaterialSpecsSectionCard(
                        icon = Icons.Outlined.DesignServices,
                        title = stringResource(R.string.specs_section_ui_arch)
                    ) {
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_ui_toolkit),
                            value = "Jetpack Compose (BOM 2024.05)"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_design_framework),
                            value = "Material Design 3 & Material You Dynamic Color"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = "Alternative UI",
                            value = "MIUIX KMP 0.9.3 (HyperOS / MIUI Theme)"
                        )
                    }
                }

                // Section 4: System Integration & Storage
                item {
                    MaterialSpecsSectionCard(
                        icon = Icons.Outlined.Storage,
                        title = stringResource(R.string.specs_section_storage_permissions)
                    ) {
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_storage_engine),
                            value = "Android Storage Access Framework (SAF) & NIO"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_privileged_mode),
                            value = "Shizuku Service API & Superuser (su)"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_min_sdk),
                            value = "Android 10 (API 29)"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_target_sdk),
                            value = "Android 15 (API 35)"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MaterialSpecItem(
                            label = stringResource(R.string.specs_host_os),
                            value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
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
private fun MaterialSpecsSectionCard(
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
private fun MaterialSpecItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.3f, fill = false)
        )
    }
}
