package id.xms.xarchiver.ui.explorer.search

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xarchiver.R

/**
 * Top bar displayed during ColorOS 17 scoped search mode with search input and "Batalkan" button.
 */
@Composable
fun ExplorerSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCancelSearch: () -> Unit,
    placeholderText: String = stringResource(R.string.explorer_search_files),
    primaryTextColor: Color,
    secondaryTextColor: Color,
    inputBgColor: Color,
    cancelButtonBgColor: Color,
    accentColor: Color
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded search input pill matching ColorOS
            Row(
                modifier = Modifier
                    .weight(1f)
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
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* done */ }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = placeholderText,
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
            }

            Spacer(Modifier.width(10.dp))

            // Cancel button matching ColorOS
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cancelButtonBgColor,
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onCancelSearch)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_cancel),
                        color = primaryTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Horizontally scrolling category chips row matching ColorOS 17 search.
 */
@Composable
fun ExplorerSearchCategoryChips(
    selectedCategory: ExplorerSearchCategory,
    onCategorySelected: (ExplorerSearchCategory) -> Unit,
    activeBgColor: Color,
    activeTextColor: Color,
    inactiveBgColor: Color,
    inactiveTextColor: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExplorerSearchCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val chipBg = if (isSelected) activeBgColor else inactiveBgColor
            val chipTextColor = if (isSelected) activeTextColor else inactiveTextColor
            val chipIconTint = if (isSelected) activeTextColor else category.iconColor

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(chipBg)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = chipIconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(category.labelRes),
                    color = chipTextColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Summary header displayed above search results:
 * - Row 1: "[X] item seluruhnya" on left, "Filter ∇" on right
 * - Row 2: Scope name: "Perangkat ini" or current folder name (e.g. "Documents")
 */
@Composable
fun ExplorerSearchHeader(
    totalItems: Int,
    scopeDisplayName: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_total_items_format, totalItems),
                color = secondaryTextColor,
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onFilterClick != null) { onFilterClick?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_filter),
                    color = secondaryTextColor,
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.search_filter),
                    tint = secondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = scopeDisplayName,
            color = primaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Expand search footer:
 * When in subfolder: offers to search entire device.
 * When at root: offers to search all folders / hidden files.
 */
@Composable
fun ExplorerSearchExpandFooter(
    isRootScope: Boolean = false,
    secondaryTextColor: Color,
    accentColor: Color,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.search_cant_find_prompt),
                color = secondaryTextColor,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(
                    if (isRootScope) R.string.search_action_deep
                    else R.string.search_action_entire_device
                ),
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onActionClick)
            )
        }
    }
}

/**
 * Empty state view when search query yielded no results, or landing instructions.
 */
@Composable
fun ExplorerSearchEmptyView(
    isQueryEmpty: Boolean,
    searchQuery: String,
    scopeDisplayName: String,
    isRootScope: Boolean = false,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onExpandSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(secondaryTextColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (isQueryEmpty) {
            Text(
                text = stringResource(R.string.search_in_folder_title, scopeDisplayName),
                color = primaryTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.search_in_folder_hint),
                color = secondaryTextColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(R.string.search_no_results_title),
                color = primaryTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.search_no_results_desc, searchQuery, scopeDisplayName),
                color = secondaryTextColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            ExplorerSearchExpandFooter(
                isRootScope = isRootScope,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                onActionClick = onExpandSearchClick
            )
        }
    }
}
