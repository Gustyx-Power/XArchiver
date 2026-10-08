package id.xms.xarchiver.ui.explorer.material

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom action dock during selection mode using Material Monet theme tokens.
 */
@Composable
fun MaterialSelectionDock(
    hasArchives: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onExtractOrShare: () -> Unit,
    onCompress: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockBgColor = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = dockBgColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialBottomActionButton(
                icon = Icons.Default.ContentCopy,
                label = "Salin",
                tint = contentColor,
                onClick = onCopy
            )
            MaterialBottomActionButton(
                icon = Icons.Default.ContentCut,
                label = "Potong",
                tint = contentColor,
                onClick = onCut
            )
            MaterialBottomActionButton(
                icon = Icons.Default.Delete,
                label = "Hapus",
                tint = errorColor,
                onClick = onDelete
            )
            if (hasArchives) {
                MaterialBottomActionButton(
                    icon = Icons.Default.Unarchive,
                    label = "Ekstrak",
                    tint = contentColor,
                    onClick = onExtractOrShare
                )
            } else {
                MaterialBottomActionButton(
                    icon = Icons.Default.Share,
                    label = "Bagikan",
                    tint = contentColor,
                    onClick = onExtractOrShare
                )
            }
            MaterialBottomActionButton(
                icon = Icons.Default.FolderZip,
                label = "Kompres",
                tint = contentColor,
                onClick = onCompress
            )
            MaterialBottomActionButton(
                icon = Icons.Default.MoreHoriz,
                label = "Lainnya",
                tint = contentColor,
                onClick = onMore
            )
        }
    }
}

@Composable
fun MaterialBottomActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp
        )
    }
}
