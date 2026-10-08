package id.xms.xarchiver.ui.explorer.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xarchiver.R
import id.xms.xarchiver.ui.explorer.ExplorerSortBy
import id.xms.xarchiver.ui.explorer.ExplorerSortOrder
import id.xms.xarchiver.ui.explorer.ExplorerViewMode
import java.io.File

/**
 * Redesigned Material Explorer TopBar with Monet color scheme and circular action buttons.
 */
@Composable
fun MaterialNormalTopBar(
    path: String,
    totalItemCount: Int,
    currentViewMode: ExplorerViewMode,
    currentSortOrder: ExplorerSortOrder,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    buttonBgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onBack: () -> Unit,
    onStartSearch: () -> Unit,
    onSelectViewMode: (ExplorerViewMode) -> Unit,
    onStartEdit: () -> Unit,
    onOpenSort: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val isRoot = remember(path) {
        path == "/" || path == "/sdcard" || path == "/storage/emulated/0" || path.trimEnd('/') == "/storage/emulated/0"
    }

    val titleText = remember(path, isRoot) {
        if (isRoot) "Semua file" else File(path).name.ifEmpty { "Semua file" }
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = primaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            // Title and Subtitle (Item count)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    color = primaryTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$totalItemCount item seluruhnya",
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Action Buttons: [+] [Search] [:]
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Add (+)
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                            .clickable { showAddMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = primaryTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Folder baru",
                                    color = primaryTextColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            onClick = {
                                showAddMenu = false
                                onNewFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Tambahkan file",
                                    color = primaryTextColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            onClick = {
                                showAddMenu = false
                                onNewFile()
                            }
                        )
                    }
                }

                // Button 2: Search
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(buttonBgColor)
                        .clickable(onClick = onStartSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.explorer_search_files),
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Button 3: More (3 dots)
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                            .clickable { showMoreMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Lainnya",
                            tint = primaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        modifier = Modifier
                            .widthIn(min = 180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        // Tampilan kisi
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tampilan kisi", color = primaryTextColor, fontSize = 15.sp)
                                    if (currentViewMode == ExplorerViewMode.GRID) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onSelectViewMode(ExplorerViewMode.GRID)
                            }
                        )

                        // Air terjun
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Air terjun", color = primaryTextColor, fontSize = 15.sp)
                                    if (currentViewMode == ExplorerViewMode.WATERFALL) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onSelectViewMode(ExplorerViewMode.WATERFALL)
                            }
                        )

                        // Tampilan daftar
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tampilan daftar", color = primaryTextColor, fontSize = 15.sp)
                                    if (currentViewMode == ExplorerViewMode.LIST) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onSelectViewMode(ExplorerViewMode.LIST)
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Edit
                        DropdownMenuItem(
                            text = {
                                Text("Edit", color = primaryTextColor, fontSize = 15.sp)
                            },
                            onClick = {
                                showMoreMenu = false
                                onStartEdit()
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Urutkan
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Urutkan", color = primaryTextColor, fontSize = 15.sp)
                                        Text(
                                            if (currentSortOrder == ExplorerSortOrder.DESCENDING) "Turun" else "Naik",
                                            color = secondaryTextColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onOpenSort()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material Monet breadcrumb path navigation bar.
 */
@Composable
fun MaterialBreadcrumbBar(
    currentPath: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val accentColor = MaterialTheme.colorScheme.primary
    val separatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val segments = remember(currentPath) {
        parseMaterialSegments(currentPath)
    }

    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, seg ->
            val isLast = index == segments.lastIndex

            Text(
                text = seg.name,
                color = if (isLast) accentColor else accentColor.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = !isLast) { onNavigate(seg.path) }
                    .padding(vertical = 2.dp)
            )

            if (!isLast) {
                Text(
                    text = " > ",
                    color = separatorColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

data class MaterialSegment(val name: String, val path: String)

private fun parseMaterialSegments(path: String): List<MaterialSegment> {
    val clean = path.trimEnd('/')
    val baseStorage = "/storage/emulated/0"

    if (clean == "/" || clean == "/sdcard" || clean == baseStorage) {
        return listOf(MaterialSegment("Semua file", baseStorage))
    }

    if (clean.startsWith(baseStorage)) {
        val list = mutableListOf(MaterialSegment("Semua file", baseStorage))
        val sub = clean.removePrefix(baseStorage).trimStart('/')
        if (sub.isNotEmpty()) {
            val parts = sub.split('/')
            var cur = baseStorage
            for (part in parts) {
                cur += "/$part"
                list.add(MaterialSegment(part, cur))
            }
        }
        return list
    }

    val parts = clean.split('/').filter { it.isNotEmpty() }
    val list = mutableListOf<MaterialSegment>()
    var cur = ""
    for (part in parts) {
        cur += "/$part"
        list.add(MaterialSegment(part, cur))
    }
    return list.ifEmpty { listOf(MaterialSegment("Semua file", "/")) }
}

/**
 * Top bar displayed during multi-selection mode (Edit mode).
 */
@Composable
fun MaterialSelectionTopBar(
    selectedCount: Int,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    buttonBgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onReverseSelection: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .clickable(onClick = onClearSelection),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = primaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = "$selectedCount dipilih",
                color = primaryTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(buttonBgColor)
                        .clickable(onClick = onSelectAll),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.action_select_all),
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(buttonBgColor)
                        .clickable(onClick = onReverseSelection),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipToBack,
                        contentDescription = stringResource(R.string.action_reverse_selection),
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top bar displayed during search mode.
 */
@Composable
fun MaterialSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    inputBgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(inputBgColor)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = primaryTextColor,
                    fontSize = 15.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* done */ }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.explorer_search_files),
                                color = secondaryTextColor,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = primaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Bottom sheet for sorting options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialSortBottomSheet(
    currentSortBy: ExplorerSortBy,
    currentSortOrder: ExplorerSortOrder,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    onSortByChange: (ExplorerSortBy) -> Unit,
    onSortOrderChange: (ExplorerSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val modalBgColor = MaterialTheme.colorScheme.surfaceContainerLow
    val accentColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = modalBgColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Urutkan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Berdasarkan",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = secondaryTextColor,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            SortItemRow(
                title = "Nama",
                isSelected = currentSortBy == ExplorerSortBy.NAME,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortByChange(ExplorerSortBy.NAME) }
            )
            SortItemRow(
                title = "Waktu / Tanggal",
                isSelected = currentSortBy == ExplorerSortBy.DATE,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortByChange(ExplorerSortBy.DATE) }
            )
            SortItemRow(
                title = "Ukuran",
                isSelected = currentSortBy == ExplorerSortBy.SIZE,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortByChange(ExplorerSortBy.SIZE) }
            )
            SortItemRow(
                title = "Jenis / Tipe",
                isSelected = currentSortBy == ExplorerSortBy.TYPE,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortByChange(ExplorerSortBy.TYPE) }
            )

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "Arah Urutan",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = secondaryTextColor,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            SortItemRow(
                title = "Turun (Z-A / Baru ke Lama)",
                isSelected = currentSortOrder == ExplorerSortOrder.DESCENDING,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortOrderChange(ExplorerSortOrder.DESCENDING) }
            )
            SortItemRow(
                title = "Naik (A-Z / Lama ke Baru)",
                isSelected = currentSortOrder == ExplorerSortOrder.ASCENDING,
                primaryTextColor = primaryTextColor,
                accentColor = accentColor,
                onClick = { onSortOrderChange(ExplorerSortOrder.ASCENDING) }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortItemRow(
    title: String,
    isSelected: Boolean,
    primaryTextColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (isSelected) accentColor else primaryTextColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
