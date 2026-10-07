package id.xms.xarchiver.ui.explorer.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xarchiver.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * Top bar displayed during multi-selection mode in MIUIX Explorer.
 */
@Composable
fun MiuixSelectionTopBar(
    selectedCount: Int,
    primaryTextColor: Color,
    pageBgColor: Color,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onReverseSelection: () -> Unit,
    onSelectSameType: (() -> Unit)? = null
) {
    Surface(
        color = pageBgColor,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = primaryTextColor
                )
            }
            Text(
                text = stringResource(R.string.explorer_selected_count, selectedCount),
                color = primaryTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(R.string.action_select_all),
                    tint = primaryTextColor
                )
            }
            IconButton(onClick = onReverseSelection) {
                Icon(
                    imageVector = Icons.Default.FlipToBack,
                    contentDescription = stringResource(R.string.action_reverse_selection),
                    tint = primaryTextColor
                )
            }
            if (onSelectSameType != null) {
                IconButton(onClick = onSelectSameType) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.action_select_same_type),
                        tint = primaryTextColor
                    )
                }
            }
        }
    }
}

/**
 * Top bar displayed during search mode in MIUIX Explorer.
 */
@Composable
fun MiuixSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    pageBgColor: Color,
    inputBgColor: Color,
    cardBorderColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    miuixBlue: Color,
    onCloseSearch: () -> Unit
) {
    Surface(
        color = pageBgColor,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(inputBgColor)
                .border(0.5.dp, cardBorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = primaryTextColor, fontSize = 16.sp),
                singleLine = true,
                cursorBrush = SolidColor(miuixBlue),
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
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = secondaryTextColor)
                }
            }
            IconButton(onClick = onCloseSearch) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = primaryTextColor
                )
            }
        }
    }
}

/**
 * Standard top bar displayed in MIUIX Explorer containing TopAppBar and breadcrumbs.
 */
@Composable
fun MiuixNormalTopBar(
    currentFolderDisplayName: String,
    path: String,
    isDark: Boolean,
    pageBgColor: Color,
    cardBgColor: Color,
    cardBorderColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    miuixBlue: Color,
    onBack: () -> Unit,
    onStartSearch: () -> Unit,
    onToggleSelectMode: () -> Unit,
    onRefresh: () -> Unit,
    onNavigatePath: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(pageBgColor)
    ) {
        TopAppBar(
            title = currentFolderDisplayName,
            largeTitle = currentFolderDisplayName,
            color = pageBgColor,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.action_back),
                        tint = primaryTextColor
                    )
                }
            },
            actions = {
                IconButton(onClick = onStartSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.explorer_search_files),
                        tint = primaryTextColor
                    )
                }
                IconButton(onClick = onToggleSelectMode) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = stringResource(R.string.action_select_all),
                        tint = primaryTextColor
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = primaryTextColor
                    )
                }
            }
        )

        // MIUI Breadcrumb Path Bar (modular component)
        MiuixBreadcrumbBar(
            currentPath = path,
            isDark = isDark,
            primaryColor = miuixBlue,
            cardBgColor = cardBgColor,
            borderColor = cardBorderColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            onNavigate = onNavigatePath
        )
    }
}
