package id.xms.xarchiver.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class PathSegment(
    val displayName: String,
    val fullPath: String
)

@Composable
fun PathNavigationBar(
    currentPath: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isEditMode by remember { mutableStateOf(false) }
    var editPath by remember(currentPath) { mutableStateOf(currentPath) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Parse path into segments
    val segments = remember(currentPath) { parsePathToSegments(currentPath) }
    
    // Auto-scroll to end when path changes
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isEditMode) {
                // Manual path input mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = editPath,
                        onValueChange = { editPath = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                isEditMode = false
                                focusManager.clearFocus()
                                if (editPath.isNotBlank()) {
                                    onNavigate(editPath)
                                }
                            }
                        )
                    )
                }
                
                IconButton(onClick = {
                    isEditMode = false
                    focusManager.clearFocus()
                    if (editPath.isNotBlank()) {
                        onNavigate(editPath)
                    }
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = {
                    isEditMode = false
                    editPath = currentPath
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel"
                    )
                }
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                // Breadcrumb navigation mode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    segments.forEachIndexed { index, segment ->
                        PathChip(
                            text = segment.displayName,
                            isLast = index == segments.lastIndex,
                            onClick = { onNavigate(segment.fullPath) }
                        )
                        
                        if (index < segments.lastIndex) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                
                // Edit path button
                IconButton(onClick = {
                    editPath = currentPath
                    isEditMode = true
                }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit path",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PathChip(
    text: String,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isLast) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text.ifEmpty { "Root" },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
            color = if (isLast) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun parsePathToSegments(path: String): List<PathSegment> {
    if (path.isBlank() || path == "/") {
        return listOf(PathSegment("Root", "/"))
    }
    
    val segments = mutableListOf<PathSegment>()
    val parts = path.trimEnd('/').split("/").filter { it.isNotEmpty() }
    var currentPath = ""
    
    parts.forEachIndexed { index, part ->
        currentPath = if (currentPath.isEmpty()) "/$part" else "$currentPath/$part"
        
        val displayName = when {
            currentPath == "/storage/emulated/0" -> "Internal Storage"
            currentPath == "/storage" -> "Storage"
            currentPath == "/sdcard" -> "Internal Storage"
            part == "emulated" && currentPath.contains("/storage/emulated") -> null
            part == "0" && currentPath.contains("/storage/emulated/0") -> null
            else -> part
        }
        
        displayName?.let {
            segments.add(PathSegment(it, currentPath))
        }
    }
    
    return segments.ifEmpty { listOf(PathSegment("Root", "/")) }
}
