package id.xms.xarchiver.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
fun MiuixSupportedFormatsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDark = isAppInDarkTheme(context, currentThemeMode)

    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)

    Scaffold(
        containerColor = pageBgColor,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.formats_title),
                largeTitle = stringResource(R.string.formats_title),
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group 1: Standard Archives
            item {
                MiuixFormatSectionTitle(text = stringResource(R.string.formats_group_standard), color = secondaryTextColor)
                MiuixFormatCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixFormatItem(
                        extension = "ZIP (.zip)",
                        description = stringResource(R.string.formats_desc_zip),
                        tags = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_encrypt)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "7Z (.7z)",
                        description = stringResource(R.string.formats_desc_7z),
                        tags = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_high_ratio)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "TAR (.tar)",
                        description = stringResource(R.string.formats_desc_tar),
                        tags = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                }
            }

            // Group 2: Compressed Tarballs
            item {
                MiuixFormatSectionTitle(text = stringResource(R.string.formats_group_tarballs), color = secondaryTextColor)
                MiuixFormatCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixFormatItem(
                        extension = "TAR.GZ (.tar.gz, .tgz)",
                        description = stringResource(R.string.formats_desc_targz),
                        tags = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "TAR.BZ2 (.tar.bz2, .tbz2)",
                        description = stringResource(R.string.formats_desc_tarbz2),
                        tags = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "TAR.XZ (.tar.xz, .txz)",
                        description = stringResource(R.string.formats_desc_tarxz),
                        tags = listOf(stringResource(R.string.formats_cap_create), stringResource(R.string.formats_cap_extract)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                }
            }

            // Group 3: Single-File Compressed Streams
            item {
                MiuixFormatSectionTitle(text = stringResource(R.string.formats_group_streams), color = secondaryTextColor)
                MiuixFormatCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixFormatItem(
                        extension = "GZIP (.gz)",
                        description = stringResource(R.string.formats_desc_gz),
                        tags = listOf(stringResource(R.string.formats_cap_decompress)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "BZIP2 (.bz2)",
                        description = stringResource(R.string.formats_desc_bz2),
                        tags = listOf(stringResource(R.string.formats_cap_decompress)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "XZ (.xz)",
                        description = stringResource(R.string.formats_desc_xz),
                        tags = listOf(stringResource(R.string.formats_cap_decompress)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                }
            }

            // Group 4: Specialized & Package Formats
            item {
                MiuixFormatSectionTitle(text = stringResource(R.string.formats_group_specialized), color = secondaryTextColor)
                MiuixFormatCard(cardBg = cardBgColor, cardBorder = cardBorderColor) {
                    MiuixFormatItem(
                        extension = "RAR (.rar)",
                        description = stringResource(R.string.formats_desc_rar),
                        tags = listOf(stringResource(R.string.formats_cap_extract)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "APK / APKS / XAPK",
                        description = stringResource(R.string.formats_desc_apk),
                        tags = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_inspect)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = dividerColor)
                    MiuixFormatItem(
                        extension = "PAYLOAD.BIN",
                        description = stringResource(R.string.formats_desc_payload),
                        tags = listOf(stringResource(R.string.formats_cap_extract), stringResource(R.string.formats_cap_rust_native)),
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor,
                        isDark = isDark
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
private fun MiuixFormatSectionTitle(text: String, color: Color) {
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
private fun MiuixFormatCard(
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
private fun MiuixFormatItem(
    extension: String,
    description: String,
    tags: List<String>,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean
) {
    val createLabel = stringResource(R.string.formats_cap_create)
    val extractLabel = stringResource(R.string.formats_cap_extract)
    val decompressLabel = stringResource(R.string.formats_cap_decompress)
    val encryptLabel = stringResource(R.string.formats_cap_encrypt)
    val rustNativeLabel = stringResource(R.string.formats_cap_rust_native)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = extension,
                color = primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    val (tagBg, tagText) = when (tag) {
                        "Create", createLabel -> if (isDark) Color(0xFF1E3A8A) to Color(0xFF93C5FD) else Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
                        "Extract", "Decompress", extractLabel, decompressLabel -> if (isDark) Color(0xFF14532D) to Color(0xFF86EFAC) else Color(0xFFF0FDF4) to Color(0xFF15803D)
                        "Encrypt", encryptLabel -> if (isDark) Color(0xFF78350F) to Color(0xFFFDE68A) else Color(0xFFFFFBEB) to Color(0xFFB45309)
                        "Rust Native", rustNativeLabel -> if (isDark) Color(0xFF7C2D12) to Color(0xFFFDBA74) else Color(0xFFFFF7ED) to Color(0xFFC2410C)
                        else -> if (isDark) Color(0xFF374151) to Color(0xFFD1D5DB) else Color(0xFFF3F4F6) to Color(0xFF4B5563)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tagText
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = secondaryText,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
