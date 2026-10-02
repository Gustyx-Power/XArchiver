package id.xms.xarchiver.ui.changelog

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import id.xms.xarchiver.ui.theme.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixChangelogScreen(
    navController: NavController,
    themePreferences: ThemePreferences? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = themePreferences ?: remember { ThemePreferences(context) }
    val currentThemeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDark = isAppInDarkTheme(context, currentThemeMode)

    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)
    val summaryBgColor = if (isDark) Color(0xFF1C1C1F) else Color(0xFFF3F4F6)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)

    var selectedVersionFilter by remember { mutableStateOf<String?>(null) }
    val releases = ChangelogRepository.releases

    val filteredReleases = remember(selectedVersionFilter) {
        if (selectedVersionFilter == null) releases
        else releases.filter { it.version == selectedVersionFilter }
    }

    Scaffold(
        containerColor = pageBgColor,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.changelog_title),
                largeTitle = stringResource(R.string.changelog_title),
                color = pageBgColor,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_close),
                            tint = primaryTextColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("XArchiver Release Notes")
                            appendLine("=======================")
                            releases.forEach { rel ->
                                appendLine("v${rel.version} (${context.getString(rel.dateRes)})")
                                appendLine(context.getString(rel.summaryRes))
                                appendLine()
                                rel.items.forEach { item ->
                                    appendLine("  • [${context.getString(item.category.titleRes)}] ${context.getString(item.titleRes)}")
                                    appendLine("    ${context.getString(item.descRes)}")
                                }
                                appendLine()
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = primaryTextColor,
                            modifier = Modifier.size(20.dp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview & Filter Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.changelog_subtitle),
                        color = secondaryTextColor,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )

                    // Filter Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAll = selectedVersionFilter == null
                        MiuixFilterPill(
                            label = stringResource(R.string.changelog_filter_all),
                            isSelected = isAll,
                            isDark = isDark,
                            onClick = { selectedVersionFilter = null }
                        )

                        releases.forEach { release ->
                            val isSelected = selectedVersionFilter == release.version
                            MiuixFilterPill(
                                label = "v${release.version}",
                                isSelected = isSelected,
                                isDark = isDark,
                                onClick = { selectedVersionFilter = release.version }
                            )
                        }
                    }
                }
            }

            // Release Cards
            items(filteredReleases, key = { it.version }) { release ->
                MiuixReleaseCard(
                    release = release,
                    isDark = isDark,
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    summaryBgColor = summaryBgColor,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    dividerColor = dividerColor,
                    onCopyRelease = {
                        val notes = buildString {
                            appendLine("XArchiver v${release.version} (${context.getString(release.dateRes)})")
                            appendLine(context.getString(release.summaryRes))
                            appendLine()
                            release.items.forEach { item ->
                                appendLine("• [${context.getString(item.category.titleRes)}] ${context.getString(item.titleRes)}")
                                appendLine("  ${context.getString(item.descRes)}")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(notes))
                        Toast.makeText(context, context.getString(R.string.about_copied_format, "v${release.version}"), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MiuixFilterPill(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val pillBg = when {
        isSelected -> MiuixTheme.colorScheme.primary
        isDark -> Color(0xFF222225)
        else -> Color(0xFFE5E7EB)
    }
    val pillTextColor = when {
        isSelected -> Color.White
        isDark -> Color(0xFFE2E4E9)
        else -> Color(0xFF374151)
    }

    Surface(
        shape = RoundedCornerShape(100),
        color = pillBg,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = pillTextColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MiuixReleaseCard(
    release: VersionRelease,
    isDark: Boolean,
    cardBgColor: Color,
    cardBorderColor: Color,
    summaryBgColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    dividerColor: Color,
    onCopyRelease: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(
                width = 0.5.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Version Header Row: Title + Status Badge on left, Copy on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v${release.version}",
                    color = primaryTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Status Badge
                val badgeColor = if (release.isLatest) Color(0xFF10B981) else Color(0xFF3B82F6)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                ) {
                    Text(
                        text = stringResource(release.badgeRes),
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Copy Action
                androidx.compose.material3.IconButton(
                    onClick = onCopyRelease,
                    modifier = Modifier.size(32.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy notes",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Release Date
            Text(
                text = stringResource(release.dateRes),
                color = secondaryTextColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Version Summary Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = summaryBgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(release.summaryRes),
                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Release Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                release.items.forEachIndexed { index, item ->
                    MiuixChangelogItemRow(
                        item = item,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        isDark = isDark
                    )

                    if (index < release.items.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = dividerColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixChangelogItemRow(
    item: ChangelogItem,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    isDark: Boolean
) {
    val categoryColor = Color(item.category.colorHex)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category Pill
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = categoryColor.copy(alpha = if (isDark) 0.16f else 0.10f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = stringResource(item.category.titleRes),
                    color = categoryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title - Full Width, Clear Hierarchy
        Text(
            text = stringResource(item.titleRes),
            color = primaryTextColor,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Detailed Description - Full Width, Comfortable Line Height
        Text(
            text = stringResource(item.descRes),
            color = secondaryTextColor,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )
    }
}
