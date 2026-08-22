package id.xms.xarchiver.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class NotificationType {
    INFO, SUCCESS, ERROR
}

data class NotificationData(
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val durationMs: Long = 3000L
)

class NotificationHostState {
    var currentNotification by mutableStateOf<NotificationData?>(null)
        private set

    private val mutex = Mutex()

    suspend fun showNotification(
        message: String,
        type: NotificationType = NotificationType.INFO,
        durationMs: Long = 3000L
    ) {
        mutex.withLock {
            try {
                currentNotification = NotificationData(message, type, durationMs)
                delay(durationMs)
            } finally {
                if (currentNotification?.message == message) {
                    currentNotification = null
                }
            }
        }
    }

    // Backward compatibility with SnackbarHostState
    suspend fun showSnackbar(message: String) {
        showNotification(message, NotificationType.INFO)
    }
}

val LocalNotificationHost = staticCompositionLocalOf<NotificationHostState> {
    error("No NotificationHostState provided")
}

@Composable
fun DynamicIslandNotificationHost(
    hostState: NotificationHostState,
    modifier: Modifier = Modifier
) {
    val notification = hostState.currentNotification

    Box(
        modifier = modifier
            .padding(top = 48.dp) // Provide spacing from the top edge/status bar
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(
                initialOffsetY = { -it - 100 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it - 100 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
            notification?.let {
                DynamicIslandPill(it)
            }
        }
    }
}

@Composable
private fun DynamicIslandPill(notification: NotificationData) {
    val icon: ImageVector = when (notification.type) {
        NotificationType.INFO -> Icons.Default.Info
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
        NotificationType.ERROR -> Icons.Default.Error
    }

    val iconTint = when (notification.type) {
        NotificationType.INFO -> Color(0xFF64B5F6) // Blue
        NotificationType.SUCCESS -> Color(0xFF81C784) // Green
        NotificationType.ERROR -> Color(0xFFE57373) // Red
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(Color(0xFF1C1C1E)) // Sleek dark gray
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = notification.message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
