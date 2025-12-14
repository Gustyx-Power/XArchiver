package id.xms.xarchiver.ui.viewer

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.ShareUtils
import id.xms.xarchiver.core.humanReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    filePath: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    val file = remember { File(filePath) }
    val fileName = file.name
    
    // Load image
    LaunchedEffect(filePath) {
        isLoading = true
        error = null
        try {
            val loadedBitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(filePath)
            }
            if (loadedBitmap != null) {
                bitmap = loadedBitmap
            } else {
                error = "Failed to decode image"
            }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }
    
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
                    IconButton(onClick = { rotation -= 90f }) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, "Rotate Left")
                    }
                    IconButton(onClick = { rotation += 90f }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate Right")
                    }
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(Icons.Default.Info, "Info")
                    }
                    IconButton(onClick = { ShareUtils.shareFile(context, filePath) }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Failed to load image",
                            color = Color.Gray
                        )
                        Text(
                            error ?: "",
                            color = Color.Gray.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                bitmap != null -> {
                    // Zoomable image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offset += pan
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                    rotationZ = rotation
                                }
                        )
                    }
                    
                    // Zoom controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { 
                            scale = (scale - 0.5f).coerceAtLeast(0.5f) 
                        }) {
                            Icon(Icons.Default.ZoomOut, "Zoom Out", tint = Color.White)
                        }
                        
                        Text(
                            "${(scale * 100).toInt()}%",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        
                        IconButton(onClick = { 
                            scale = (scale + 0.5f).coerceAtMost(5f) 
                        }) {
                            Icon(Icons.Default.ZoomIn, "Zoom In", tint = Color.White)
                        }
                        
                        IconButton(onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            rotation = 0f
                        }) {
                            Icon(Icons.Default.RestartAlt, "Reset", tint = Color.White)
                        }
                    }
                }
            }
            
            // Info overlay
            if (showInfo && bitmap != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("File: $fileName", color = Color.White)
                        Text("Size: ${file.length().humanReadable()}", color = Color.White)
                        Text("Dimensions: ${bitmap!!.width} × ${bitmap!!.height}", color = Color.White)
                        Text("Path: ${file.parent}", color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
