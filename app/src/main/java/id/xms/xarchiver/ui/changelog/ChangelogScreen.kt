package id.xms.xarchiver.ui.changelog

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import id.xms.xarchiver.ui.theme.ThemePreferences

@Composable
fun ChangelogScreen(navController: NavController) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(initial = ThemePreferences.isMiuiOrHyperOsDevice)

    if (isMiuixUiEnabled) {
        MiuixChangelogScreen(navController = navController, themePreferences = themePreferences)
    } else {
        Material3ChangelogScreen(navController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3ChangelogScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val releases = ChangelogRepository.releases

    var selectedVersionFilter by remember { mutableStateOf<String?>(null) }

    val filteredReleases = remember(selectedVersionFilter) {
        if (selectedVersionFilter == null) releases
        else releases.filter { it.version == selectedVersionFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.changelog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_close)
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
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedVersionFilter == null,
                            onClick = { selectedVersionFilter = null },
                            label = { Text(stringResource(R.string.changelog_filter_all)) }
                        )

                        releases.forEach { release ->
                            FilterChip(
                                selected = selectedVersionFilter == release.version,
                                onClick = { selectedVersionFilter = release.version },
                                label = { Text("v${release.version}") }
                            )
                        }
                    }
                }
            }

            // Release Cards
            items(filteredReleases, key = { it.version }) { release ->
                M3ReleaseCard(
                    release = release,
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
private fun M3ReleaseCard(
    release: VersionRelease,
    onCopyRelease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Status Badge
                val badgeColor = if (release.isLatest) Color(0xFF10B981) else Color(0xFF3B82F6)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = stringResource(release.badgeRes),
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Copy Action
                IconButton(
                    onClick = onCopyRelease,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Release Date
            Text(
                text = stringResource(release.dateRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Version Summary Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(release.summaryRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Release Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                release.items.forEachIndexed { index, item ->
                    M3ChangelogItemRow(item = item)

                    if (index < release.items.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun M3ChangelogItemRow(item: ChangelogItem) {
    val categoryColor = Color(item.category.colorHex)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category Pill
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = categoryColor.copy(alpha = 0.12f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = stringResource(item.category.titleRes),
                    color = categoryColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title - Full Width, Clear Hierarchy
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Detailed Description - Full Width, Comfortable Line Height
        Text(
            text = stringResource(item.descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}
