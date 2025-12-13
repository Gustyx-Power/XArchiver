package id.xms.xarchiver.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Shortcut
import id.xms.xarchiver.ui.theme.*

@Composable
fun ShortcutList(
    shortcuts: List<Shortcut>,
    onShortcutClick: (Shortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    // Horizontal scrollable row for shortcuts - more modern approach
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        shortcuts.forEachIndexed { index, shortcut ->
            ShortcutItem(
                shortcut = shortcut,
                onClick = { onShortcutClick(shortcut) },
                modifier = Modifier.weight(1f),
                delayMs = index * 50
            )
        }
    }
}

@Composable
private fun ShortcutItem(
    shortcut: Shortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    delayMs: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val (icon, iconColor) = remember(shortcut) { 
        getShortcutIconAndColor(shortcut) 
    }

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with colored background
                Surface(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            icon,
                            contentDescription = shortcut.name,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    shortcut.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

private fun getShortcutIconAndColor(shortcut: Shortcut): Pair<ImageVector, Color> {
    return when (shortcut.name.lowercase()) {
        "downloads" -> Icons.Outlined.Download to AccentGreen
        "bluetooth" -> Icons.Filled.Bluetooth to Blue40
        "whatsapp" -> Icons.AutoMirrored.Filled.Chat to Color(0xFF25D366)
        "telegram" -> Icons.AutoMirrored.Filled.Send to Color(0xFF0088CC)
        "dcim", "camera" -> Icons.Outlined.CameraAlt to ImageColor
        "pictures" -> Icons.Outlined.Image to ImageColor
        "music" -> Icons.Outlined.MusicNote to AudioColor
        "movies", "videos" -> Icons.Outlined.VideoLibrary to VideoColor
        "documents" -> Icons.Outlined.Description to DocumentColor
        else -> Icons.Outlined.Folder to AccentOrange
    }
}
