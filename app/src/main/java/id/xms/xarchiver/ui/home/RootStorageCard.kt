package id.xms.xarchiver.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.res.stringResource
import id.xms.xarchiver.R

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
            title = stringResource(R.string.root_privileged_explorer)
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
            // Row 1: Badges on left, action on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Text(
                            text = "ROOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.root_required),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.action_request_permission),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Row 2: Icon + Title side-by-side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.root_privileged_explorer),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Row 3: Status / prompt text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (busy) stringResource(R.string.root_requesting_access) else stringResource(R.string.root_tap_to_grant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            // Row 4: Track indicator
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {}
        }
    }
}
