package id.xms.xarchiver.ui.explorer.material

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xarchiver.R

/**
 * Floating bottom action bar during selection mode in Material 3 Explorer.
 */
@Composable
fun MaterialSelectionDock(
    hasArchives: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onExtractOrShare: () -> Unit,
    onCompress: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialBottomActionButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy",
                onClick = onCopy
            )
            MaterialBottomActionButton(
                icon = Icons.Default.ContentCut,
                label = "Cut",
                onClick = onCut
            )
            MaterialBottomActionButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
            if (hasArchives) {
                MaterialBottomActionButton(
                    icon = Icons.Default.Unarchive,
                    label = "Extract",
                    onClick = onExtractOrShare
                )
            } else {
                MaterialBottomActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = onExtractOrShare
                )
            }
            MaterialBottomActionButton(
                icon = Icons.Default.FolderZip,
                label = "Compress",
                onClick = onCompress
            )
            MaterialBottomActionButton(
                icon = Icons.Default.MoreVert,
                label = "More",
                onClick = onMore
            )
        }
    }
}

/**
 * Single action item in Material selection bottom bar.
 */
@Composable
fun MaterialBottomActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint
        )
    }
}

/**
 * Floating Action Button speed dial for Material 3 Explorer with expandable menu.
 */
@Composable
fun MaterialExplorerFab(
    visible: Boolean,
    showFabMenu: Boolean,
    onToggleFabMenu: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit
) {
    if (!visible) return

    Column(horizontalAlignment = Alignment.End) {
        // Expandable Speed-Dial Menu
        AnimatedVisibility(
            visible = showFabMenu,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNewFolder,
                    icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                    text = { Text(stringResource(R.string.dialog_new_folder)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                ExtendedFloatingActionButton(
                    onClick = onNewFile,
                    icon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.dialog_new_file)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        // Main FAB
        FloatingActionButton(onClick = onToggleFabMenu) {
            Icon(
                imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Menu"
            )
        }
    }
}
