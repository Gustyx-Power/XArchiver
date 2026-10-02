package id.xms.xarchiver.ui.home

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import id.xms.xarchiver.ui.theme.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun MiuixAppSpecsScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val themePreferences = remember { ThemePreferences(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDark = isAppInDarkTheme(context, currentThemeMode)

    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)

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

    Scaffold(
        containerColor = pageBgColor,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.specs_title),
                largeTitle = stringResource(R.string.specs_title),
                color = pageBgColor,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_back),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Hero Highlights (2x2 Grid in signature HyperOS "All specs" style)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiuixHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.specs_tag_version),
                        value = "v$versionName",
                        sub = stringResource(R.string.specs_debug_build),
                        cardBg = cardBgColor,
                        cardBorder = cardBorderColor,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    )
                    MiuixHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.specs_tag_core_engine),
                        value = "Commons Compress",
                        sub = "Apache v1.26.1 • Rust",
                        cardBg = cardBgColor,
                        cardBorder = cardBorderColor,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        accentColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiuixHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.specs_tag_target_os),
                        value = "Android 15",
                        sub = "API Level 35",
                        cardBg = cardBgColor,
                        cardBorder = cardBorderColor,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        accentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                    MiuixHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.specs_tag_ui_arch),
                        value = "Jetpack Compose",
                        sub = "MIUIX KMP 0.9.3",
                        cardBg = cardBgColor,
                        cardBorder = cardBorderColor,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        accentColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    )
                }
            }

            // Section 1: General Info
            item {
                MiuixSectionTitle(text = stringResource(R.string.specs_section_general), color = secondaryTextColor)
                MiuixSpecsCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_app_name),
                        value = "XArchiver",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_package_id),
                        value = packageName,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(packageName))
                            Toast.makeText(context, context.getString(R.string.about_copied_format, packageName), Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_version),
                        value = "v$versionName",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString("v$versionName"))
                            Toast.makeText(context, context.getString(R.string.about_copied_format, "v$versionName"), Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_build_env),
                        value = "Android Gradle Plugin 8.3+ • Kotlin 1.9.22",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }
            }

            // Section 2: Archive & Compression Engine
            item {
                MiuixSectionTitle(text = stringResource(R.string.specs_section_engine), color = secondaryTextColor)
                MiuixSpecsCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_primary_compressor),
                        value = "Apache Commons Compress 1.26.1",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_payload_engine),
                        value = "Rust Native Engine (libpayload_parser.so • JNI)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString("Rust Native Engine (libpayload_parser.so)"))
                            Toast.makeText(context, context.getString(R.string.about_copied_format, "Rust Native Engine (libpayload_parser.so)"), Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_lzma_engine),
                        value = "Tukaani XZ for Java 1.9",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_io_buffering),
                        value = "Apache Commons IO 2.15.1",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_concurrency),
                        value = "Kotlin Coroutines & Flow (Dispatchers.IO)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }
            }

            // Section 3: UI Toolkit & Framework
            item {
                MiuixSectionTitle(text = stringResource(R.string.specs_section_ui), color = secondaryTextColor)
                MiuixSpecsCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_ui_toolkit),
                        value = "Jetpack Compose (BOM 2024.05)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_design_framework),
                        value = "MIUIX KMP 0.9.3 & Material Design 3",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_aesthetic_theme),
                        value = "Xiaomi HyperOS Squircle & AMOLED Dark",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }
            }

            // Section 4: System Integration & Storage
            item {
                MiuixSectionTitle(text = stringResource(R.string.specs_section_system), color = secondaryTextColor)
                MiuixSpecsCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_storage_engine),
                        value = "Storage Access Framework (SAF) & Java NIO",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_privileged_mode),
                        value = "Shizuku API v13+ & Root Shell (su)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_min_sdk),
                        value = "Android 10 Quince Tart (API 29)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_target_sdk),
                        value = "Android 15 Vanilla Ice Cream (API 35)",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixSpecItem(
                        title = stringResource(R.string.specs_host_os),
                        value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            val osString = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                            clipboardManager.setText(AnnotatedString(osString))
                            Toast.makeText(context, context.getString(R.string.about_copied_format, osString), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MiuixSectionTitle(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun MiuixHighlightCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    accentColor: Color
) {
    Surface(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiuixSpecsCard(
    cardBg: Color,
    cardBorder: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun MiuixSpecItem(
    title: String,
    value: String,
    primaryText: Color,
    secondaryText: Color,
    trailingIcon: ImageVector? = null,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = secondaryText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.1.sp
            )
            Text(
                text = value,
                color = primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = secondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
