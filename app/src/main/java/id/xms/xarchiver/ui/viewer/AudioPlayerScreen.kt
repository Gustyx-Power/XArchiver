package id.xms.xarchiver.ui.viewer

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.core.ShareUtils
import id.xms.xarchiver.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    filePath: String,
    navController: NavController
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }
    val fileName = file.name
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Initialize MediaPlayer
    DisposableEffect(filePath) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepareAsync()
                setOnPreparedListener {
                    duration = it.duration
                    isLoading = false
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, what, extra ->
                    error = "Playback error: $what, $extra"
                    isLoading = false
                    true
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
        mediaPlayer = player
        
        onDispose {
            player.release()
            mediaPlayer = null
        }
    }
    
    // Update position periodically
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                currentPosition = it.currentPosition
            }
            delay(100)
        }
    }
    
    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                player.pause()
            } else {
                player.start()
            }
            isPlaying = !isPlaying
        }
    }
    
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        currentPosition = position
    }
    
    fun skip(seconds: Int) {
        val newPosition = (currentPosition + seconds * 1000).coerceIn(0, duration)
        seekTo(newPosition)
    }
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorShift"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { ShareUtils.shareFile(context, filePath) }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AudioColor.copy(alpha = 0.3f + colorShift * 0.2f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Failed to play audio", color = MaterialTheme.colorScheme.error)
                    Text(error ?: "", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Album art placeholder
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AudioColor.copy(alpha = 0.6f),
                                        AudioColor.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Title
                    Text(
                        fileName.substringBeforeLast('.'),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Progress
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { seekTo(it.toInt()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AudioColor,
                                activeTrackColor = AudioColor
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatDuration(currentPosition),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                formatDuration(duration),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { skip(-10) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay10, "Rewind 10s")
                        }
                        
                        FloatingActionButton(
                            onClick = { togglePlayPause() },
                            containerColor = AudioColor,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { skip(10) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward10, "Forward 10s")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
    return String.format("%d:%02d", minutes, seconds)
}
