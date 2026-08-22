package id.xms.xarchiver.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.StorageInfo
import id.xms.xarchiver.core.root.RootService
import kotlinx.coroutines.launch

@Composable
fun RootStorageCard(
    info: StorageInfo,
    onOpenRoot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val shizukuGranted by id.xms.xarchiver.core.root.ShizukuService.isGrantedFlow.collectAsState()
    
    var granted by remember(shizukuGranted) { mutableStateOf(RootService.isGranted() || shizukuGranted) }
    var busy by remember(shizukuGranted) { mutableStateOf(!(RootService.isGranted() || shizukuGranted)) }
    
    LaunchedEffect(Unit) {
        if (!granted) {
            busy = true
            // Try Root first, then Shizuku
            granted = RootService.ensureRoot()
            if (!granted && id.xms.xarchiver.core.root.ShizukuService.isAvailable()) {
                granted = id.xms.xarchiver.core.root.ShizukuService.ensureShizuku()
            }
            busy = false
        } else {
            busy = false
        }
    }

    if (granted) {
        val rootGranted = RootService.isGranted()
        val connectionType = if (rootGranted) "Root" else "Shizuku"
        val startPath = if (rootGranted) "/" else "/storage/emulated/0"
        
        StorageCard(
            info = info.copy(fsType = connectionType),
            onClick = { onOpenRoot(startPath) },
            modifier = modifier,
            icon = Icons.Filled.Security,
            title = "Privileged Explorer"
        )
    } else {
        RootRequiredCard(
            busy = busy,
            onClick = {
                if (!busy) {
                    busy = true
                    scope.launch {
                        granted = RootService.ensureRoot()
                        if (!granted && id.xms.xarchiver.core.root.ShizukuService.isAvailable()) {
                            granted = id.xms.xarchiver.core.root.ShizukuService.ensureShizuku()
                        }
                        busy = false
                        if (granted) onOpenRoot("/")
                    }
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun RootRequiredCard(
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Status badge
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Required",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Privileged Explorer",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                if (busy) "Requesting privileged access..." else "Tap to grant Root or Shizuku access",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(44.dp)) // To match StorageCard height roughly
        }
    }
}
