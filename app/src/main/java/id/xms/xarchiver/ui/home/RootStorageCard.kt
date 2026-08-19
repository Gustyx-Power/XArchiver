package id.xms.xarchiver.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.root.RootService
import id.xms.xarchiver.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RootStorageCard(
    onOpenRoot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(RootService.isGranted()) }
    var busy by remember { mutableStateOf(!RootService.isGranted()) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(Unit) {
        if (!granted) {
            busy = true
            granted = RootService.ensureRoot()
            busy = false
        } else {
            busy = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Gradient colors based on state
    val containerColor = if (granted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (granted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (granted) onOpenRoot("/")
                else if (!busy) {
                    busy = true
                    scope.launch {
                        granted = RootService.ensureRoot()
                        busy = false
                        if (granted) onOpenRoot("/")
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        Icon(
                            if (granted) Icons.Filled.Security else Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Root Explorer",
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    when {
                        busy -> "Requesting access..."
                        granted -> "System files access ready"
                        else -> "Tap to grant root access"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            // Status badge
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Surface(
                        color = contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(6.dp)
                    ) {}
                    
                    Spacer(Modifier.width(6.dp))
                    
                    Text(
                        if (granted) "Ready" else "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
