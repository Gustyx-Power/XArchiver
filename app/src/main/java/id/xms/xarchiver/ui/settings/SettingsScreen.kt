package id.xms.xarchiver.ui.settings

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import androidx.compose.ui.res.stringResource
import id.xms.xarchiver.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val scope = rememberCoroutineScope()

    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDynamicColorEnabled by themePreferences.isDynamicColorEnabled.collectAsState(initial = true)
    val isRootAccessEnabled by themePreferences.isRootAccessEnabled.collectAsState(initial = false)
    
    var showShizukuGuide by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

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
                                stringResource(R.string.settings_title), 
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                
                SettingsSectionHeader(stringResource(R.string.settings_appearance))
                
                SettingsCard {
                    SettingsOption(
                        title = stringResource(R.string.settings_light_theme),
                        description = stringResource(R.string.settings_light_theme_desc),
                        icon = Icons.Default.LightMode,
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { scope.launch { themePreferences.setThemeMode(ThemeMode.LIGHT) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsOption(
                        title = stringResource(R.string.settings_dark_theme),
                        description = stringResource(R.string.settings_dark_theme_desc),
                        icon = Icons.Default.DarkMode,
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = { scope.launch { themePreferences.setThemeMode(ThemeMode.DARK) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsOption(
                        title = stringResource(R.string.settings_system_default),
                        description = stringResource(R.string.settings_system_default_desc),
                        icon = Icons.Default.SettingsBrightness,
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { scope.launch { themePreferences.setThemeMode(ThemeMode.SYSTEM) } }
                    )
                }
                
                SettingsSectionHeader(stringResource(R.string.settings_color_scheme))
                
                SettingsCard {
                    val isSOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isSOrAbove) {
                                scope.launch { themePreferences.setDynamicColorEnabled(!isDynamicColorEnabled) }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isSOrAbove) stringResource(R.string.settings_material_you) else stringResource(R.string.settings_dynamic_colors),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isSOrAbove) stringResource(R.string.settings_material_you_desc) else stringResource(R.string.settings_dynamic_colors_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = isDynamicColorEnabled && isSOrAbove,
                            onCheckedChange = { enabled ->
                                scope.launch { themePreferences.setDynamicColorEnabled(enabled) }
                            },
                            enabled = isSOrAbove
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(initial = false)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { themePreferences.setMiuixUiEnabled(!isMiuixUiEnabled) }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MIUIX UI Mode (Beta)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Use Xiaomi HyperOS style components (Restart app)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = isMiuixUiEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { themePreferences.setMiuixUiEnabled(enabled) }
                            }
                        )
                    }
                }
                
                SettingsSectionHeader(stringResource(R.string.settings_advanced))
                
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { themePreferences.setRootAccessEnabled(!isRootAccessEnabled) }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_privileged_access),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_privileged_access_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = isRootAccessEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { themePreferences.setRootAccessEnabled(enabled) }
                            }
                        )
                    }
                }
                
                SettingsSectionHeader(stringResource(R.string.settings_shizuku_integration))
                
                SettingsCard {
                    val shizukuAvailable by id.xms.xarchiver.core.root.ShizukuService.isAvailableFlow.collectAsState()
                    val shizukuGranted by id.xms.xarchiver.core.root.ShizukuService.isGrantedFlow.collectAsState()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShizukuGuide = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (shizukuGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Terminal, // Using Terminal as Adb is sometimes not available without androidx.compose.material:material-icons-extended
                                    contentDescription = null,
                                    tint = if (shizukuGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_shizuku_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (shizukuGranted) stringResource(R.string.settings_shizuku_connected)
                                else if (shizukuAvailable) stringResource(R.string.settings_shizuku_permission_denied)
                                else stringResource(R.string.settings_shizuku_not_running),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                SettingsSectionHeader(stringResource(R.string.settings_information))
                
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("about") }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_about),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.settings_about_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
        
        if (showShizukuGuide) {
            ShizukuGuideDialog(
                onDismiss = { showShizukuGuide = false },
                onRequestPermission = {
                    scope.launch {
                        id.xms.xarchiver.core.root.ShizukuService.ensureShizuku()
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ShizukuGuideDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val isAvailable by id.xms.xarchiver.core.root.ShizukuService.isAvailableFlow.collectAsState()
    val isGranted by id.xms.xarchiver.core.root.ShizukuService.isGrantedFlow.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.shizuku_guide_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.shizuku_guide_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider()
                
                if (isGranted) {
                    Text(
                        stringResource(R.string.shizuku_guide_connected),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isAvailable) {
                    Text(
                        stringResource(R.string.shizuku_guide_no_permission),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(R.string.shizuku_guide_tap_request))
                } else {
                    Text(stringResource(R.string.shizuku_guide_how_to_start), fontWeight = FontWeight.Bold)
                    
                    Text(stringResource(R.string.shizuku_guide_step1))
                    Text(stringResource(R.string.shizuku_guide_step2))
                    Text(stringResource(R.string.shizuku_guide_step3))
                    Text(stringResource(R.string.shizuku_guide_step4))
                }
            }
        },
        confirmButton = {
            if (!isGranted) {
                Button(onClick = {
                    onRequestPermission()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.action_request_permission))
                }
            } else {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
        dismissButton = {
            if (!isGranted) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
