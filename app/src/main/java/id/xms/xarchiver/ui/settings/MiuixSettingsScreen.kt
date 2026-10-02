package id.xms.xarchiver.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.core.root.ShizukuService
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import id.xms.xarchiver.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
fun MiuixSettingsScreen(
    navController: NavController,
    themePreferences: ThemePreferences? = null
) {
    val context = LocalContext.current
    val prefs = themePreferences ?: remember { ThemePreferences(context) }
    val scope = rememberCoroutineScope()

    val currentThemeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDynamicColorEnabled by prefs.isDynamicColorEnabled.collectAsState(initial = true)
    val isMiuixUiEnabled by prefs.isMiuixUiEnabled.collectAsState(initial = ThemePreferences.isMiuiOrHyperOsDevice)
    val isRootAccessEnabled by prefs.isRootAccessEnabled.collectAsState(initial = false)

    val isDark = isAppInDarkTheme(context, currentThemeMode)
    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)

    var showShizukuGuide by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = pageBgColor,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_title),
                largeTitle = stringResource(R.string.settings_title),
                color = pageBgColor,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_close),
                            tint = primaryTextColor
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tampilan / Appearance
            item {
                SmallTitle(text = stringResource(R.string.settings_appearance))
            }
            item {
                MiuixSettingsCard(isDark = isDark) {
                    MiuixSettingsItem(
                        icon = Icons.Default.LightMode,
                        iconBgColor = Color(0xFFF59E0B),
                        title = stringResource(R.string.settings_light_theme),
                        subtitle = stringResource(R.string.settings_light_theme_desc),
                        isDark = isDark,
                        onClick = { scope.launch { prefs.setThemeMode(ThemeMode.LIGHT) } },
                        trailing = {
                            RadioButton(
                                selected = currentThemeMode == ThemeMode.LIGHT,
                                onClick = { scope.launch { prefs.setThemeMode(ThemeMode.LIGHT) } }
                            )
                        }
                    )
                    MiuixSettingsDivider(isDark)
                    MiuixSettingsItem(
                        icon = Icons.Default.DarkMode,
                        iconBgColor = Color(0xFF6366F1),
                        title = stringResource(R.string.settings_dark_theme),
                        subtitle = stringResource(R.string.settings_dark_theme_desc),
                        isDark = isDark,
                        onClick = { scope.launch { prefs.setThemeMode(ThemeMode.DARK) } },
                        trailing = {
                            RadioButton(
                                selected = currentThemeMode == ThemeMode.DARK,
                                onClick = { scope.launch { prefs.setThemeMode(ThemeMode.DARK) } }
                            )
                        }
                    )
                    MiuixSettingsDivider(isDark)
                    MiuixSettingsItem(
                        icon = Icons.Default.SettingsBrightness,
                        iconBgColor = Color(0xFF64748B),
                        title = stringResource(R.string.settings_system_default),
                        subtitle = stringResource(R.string.settings_system_default_desc),
                        isDark = isDark,
                        onClick = { scope.launch { prefs.setThemeMode(ThemeMode.SYSTEM) } },
                        trailing = {
                            RadioButton(
                                selected = currentThemeMode == ThemeMode.SYSTEM,
                                onClick = { scope.launch { prefs.setThemeMode(ThemeMode.SYSTEM) } }
                            )
                        }
                    )
                }
            }

            // Skema Warna & Mode UI
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SmallTitle(text = stringResource(R.string.settings_color_scheme))
            }
            item {
                val isSOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                MiuixSettingsCard(isDark = isDark) {
                    MiuixSettingsItem(
                        icon = Icons.Default.Palette,
                        iconBgColor = Color(0xFF8B5CF6),
                        title = if (isSOrAbove) stringResource(R.string.settings_material_you) else stringResource(R.string.settings_dynamic_colors),
                        subtitle = if (isSOrAbove) stringResource(R.string.settings_material_you_desc) else stringResource(R.string.settings_dynamic_colors_desc),
                        isDark = isDark,
                        enabled = isSOrAbove,
                        onClick = if (isSOrAbove) {
                            { scope.launch { prefs.setDynamicColorEnabled(!isDynamicColorEnabled) } }
                        } else null,
                        trailing = {
                            Switch(
                                checked = isDynamicColorEnabled && isSOrAbove,
                                onCheckedChange = { enabled ->
                                    scope.launch { prefs.setDynamicColorEnabled(enabled) }
                                },
                                enabled = isSOrAbove
                            )
                        }
                    )
                    MiuixSettingsDivider(isDark)
                    MiuixSettingsItem(
                        icon = Icons.Default.PhoneAndroid,
                        iconBgColor = Color(0xFFFF6900),
                        title = "MIUIX UI Mode",
                        subtitle = "Gaya antarmuka HyperOS / MIUI (Otomatis aktif di Xiaomi/Redmi/Poco)",
                        isDark = isDark,
                        onClick = {
                            scope.launch { prefs.setMiuixUiEnabled(!isMiuixUiEnabled) }
                        },
                        trailing = {
                            Switch(
                                checked = isMiuixUiEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { prefs.setMiuixUiEnabled(enabled) }
                                }
                            )
                        }
                    )
                }
            }

            // Lanjutan
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SmallTitle(text = stringResource(R.string.settings_advanced))
            }
            item {
                MiuixSettingsCard(isDark = isDark) {
                    MiuixSettingsItem(
                        icon = Icons.Default.Security,
                        iconBgColor = Color(0xFFEF4444),
                        title = stringResource(R.string.settings_privileged_access),
                        subtitle = stringResource(R.string.settings_privileged_access_desc),
                        isDark = isDark,
                        onClick = {
                            scope.launch { prefs.setRootAccessEnabled(!isRootAccessEnabled) }
                        },
                        trailing = {
                            Switch(
                                checked = isRootAccessEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { prefs.setRootAccessEnabled(enabled) }
                                }
                            )
                        }
                    )
                }
            }

            // Integrasi Shizuku
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SmallTitle(text = stringResource(R.string.settings_shizuku_integration))
            }
            item {
                val shizukuAvailable by ShizukuService.isAvailableFlow.collectAsState()
                val shizukuGranted by ShizukuService.isGrantedFlow.collectAsState()

                val shizukuStatusText = when {
                    shizukuGranted -> stringResource(R.string.settings_shizuku_connected)
                    shizukuAvailable -> stringResource(R.string.settings_shizuku_permission_denied)
                    else -> stringResource(R.string.settings_shizuku_not_running)
                }
                val shizukuStatusColor = when {
                    shizukuGranted -> Color(0xFF10B981)
                    shizukuAvailable -> Color(0xFFF59E0B)
                    else -> if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)
                }

                MiuixSettingsCard(isDark = isDark) {
                    MiuixSettingsItem(
                        icon = Icons.Default.Terminal,
                        iconBgColor = Color(0xFF10B981),
                        title = stringResource(R.string.settings_shizuku_status),
                        subtitle = shizukuStatusText,
                        isDark = isDark,
                        onClick = { showShizukuGuide = true },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (shizukuGranted) stringResource(R.string.settings_shizuku_connected) else "",
                                    color = shizukuStatusColor,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = MiuixIcons.ChevronForward,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Informasi
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SmallTitle(text = stringResource(R.string.settings_information))
            }
            item {
                MiuixSettingsCard(isDark = isDark) {
                    MiuixSettingsItem(
                        icon = Icons.Outlined.Info,
                        iconBgColor = Color(0xFF3B82F6),
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_desc),
                        isDark = isDark,
                        onClick = { navController.navigate("about") },
                        trailing = {
                            Icon(
                                imageVector = MiuixIcons.ChevronForward,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showShizukuGuide) {
        ShizukuGuideDialog(
            onDismiss = { showShizukuGuide = false },
            onRequestPermission = {
                scope.launch {
                    ShizukuService.ensureShizuku()
                }
            }
        )
    }
}

@Composable
private fun MiuixSettingsCard(
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorder = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(
                width = 0.5.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun MiuixSettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String? = null,
    isDark: Boolean,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = if (enabled) primaryColor else secondaryColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (enabled) secondaryColor else secondaryColor.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        trailing()
    }
}

@Composable
private fun MiuixSettingsDivider(isDark: Boolean) {
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = dividerColor
    )
}
