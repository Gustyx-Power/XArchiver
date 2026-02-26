package id.xms.xarchiver.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    filePath: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var content by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var wordWrap by remember { mutableStateOf(true) }
    var showLineNumbers by remember { mutableStateOf(true) }
    
    // Undo/Redo stack (simple implementation)
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    
    val file = remember { File(filePath) }
    val fileName = remember { file.name }
    
    // Calculate line count
    val lines = remember(content) { content.split("\n") }
    val lineCount = lines.size
    
    // Load file content
    LaunchedEffect(filePath) {
        isLoading = true
        try {
            val text = withContext(Dispatchers.IO) {
                val fileSize = file.length()
                val maxSize = 10 * 1024 * 1024 // 10MB limit
                
                if (fileSize > maxSize) {
                    throw IllegalStateException(
                        "File too large to open as text (${fileSize / (1024 * 1024)}MB). " +
                        "Maximum supported size is ${maxSize / (1024 * 1024)}MB."
                    )
                }
                
                // Check if file is likely binary
                val extension = file.extension.lowercase()
                val binaryExtensions = listOf("bin", "so", "apk", "dex", "img", "dat", "exe", "dll")
                
                // Special handling for known system files
                if (file.name.equals("payload.bin", ignoreCase = true)) {
                    throw IllegalStateException(
                        "Payload.bin viewer coming soon! This feature will allow you to browse and extract Android OTA system images."
                    )
                }
                
                if (extension in binaryExtensions) {
                    throw IllegalStateException(
                        "Cannot open binary file (.${extension}) as text. " +
                        "This file type is not supported in text editor."
                    )
                }
                
                file.readText()
            }
            content = text
            originalContent = text
        } catch (e: OutOfMemoryError) {
            scope.launch {
                snackbarHostState.showSnackbar("File too large to load into memory")
            }
            navController.popBackStack()
        } catch (e: IllegalStateException) {
            scope.launch {
                snackbarHostState.showSnackbar(e.message ?: "Cannot open file")
            }
            navController.popBackStack()
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Error loading file: ${e.message}")
            }
            navController.popBackStack()
        }
        isLoading = false
    }
    
    // Track changes
    LaunchedEffect(content) {
        hasChanges = content != originalContent
    }
    
    fun saveFile() {
        scope.launch {
            isSaving = true
            try {
                withContext(Dispatchers.IO) {
                    file.writeText(content)
                }
                originalContent = content
                hasChanges = false
                snackbarHostState.showSnackbar("File saved")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving: ${e.message}")
            }
            isSaving = false
        }
    }
    
    fun pushUndo() {
        if (undoStack.lastOrNull() != content) {
            undoStack.add(content)
            if (undoStack.size > 50) undoStack.removeAt(0)
        }
    }
    
    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(content)
            content = undoStack.removeLast()
        }
    }
    
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(content)
            content = redoStack.removeLast()
        }
    }
    
    // Handle back press
    fun handleBack() {
        if (hasChanges) {
            showExitDialog = true
        } else {
            navController.navigateUp()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            fileName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (hasChanges) {
                            Text(
                                "Modified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                    IconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                    }
                    IconButton(onClick = { showFindDialog = true }) {
                        Icon(Icons.Default.Search, "Find")
                    }
                    IconButton(
                        onClick = { saveFile() },
                        enabled = hasChanges && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lines: $lineCount | Chars: ${content.length}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row {
                        TextButton(onClick = { wordWrap = !wordWrap }) {
                            Text(if (wordWrap) "Wrap: ON" else "Wrap: OFF")
                        }
                        TextButton(onClick = { showLineNumbers = !showLineNumbers }) {
                            Text(if (showLineNumbers) "Lines: ON" else "Lines: OFF")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Line numbers
                if (showLineNumbers) {
                    val lineNumberScrollState = rememberScrollState()
                    
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(lineNumberScrollState)
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        lines.forEachIndexed { index, _ ->
                            Text(
                                text = "${index + 1}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                }
                
                // Editor
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (wordWrap) Modifier.verticalScroll(verticalScroll)
                            else Modifier
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                        )
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = { newValue ->
                            pushUndo()
                            redoStack.clear()
                            content = newValue
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Unsaved Changes") },
            text = { Text("Do you want to save changes before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    saveFile()
                    showExitDialog = false
                    navController.navigateUp()
                }) { Text("Save & Exit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        navController.navigateUp()
                    }) { Text("Discard") }
                    TextButton(onClick = { showExitDialog = false }) { 
                        Text("Cancel") 
                    }
                }
            }
        )
    }
    
    // Find dialog
    if (showFindDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var replaceQuery by remember { mutableStateOf("") }
        var showReplace by remember { mutableStateOf(false) }
        var currentMatchIndex by remember { mutableStateOf(-1) }
        var matchCount by remember { mutableStateOf(0) }
        var highlightMatches by remember { mutableStateOf(false) }
        
        // Calculate match count when search query changes
        LaunchedEffect(searchQuery, content) {
            if (searchQuery.isNotEmpty()) {
                var count = 0
                var startIndex = 0
                while (true) {
                    val index = content.indexOf(searchQuery, startIndex, ignoreCase = true)
                    if (index == -1) break
                    count++
                    startIndex = index + 1
                }
                matchCount = count
                currentMatchIndex = if (count > 0) 0 else -1
            } else {
                matchCount = 0
                currentMatchIndex = -1
            }
        }
        
        AlertDialog(
            onDismissRequest = { showFindDialog = false },
            title = { Text("Find & Replace") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Find") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    "$matchCount found",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (matchCount > 0) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    
                    // Find action buttons
                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (matchCount > 0) {
                                        currentMatchIndex = (currentMatchIndex + 1) % matchCount
                                        // Show current match position
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Match ${currentMatchIndex + 1} of $matchCount"
                                            )
                                        }
                                    }
                                },
                                enabled = matchCount > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Find Next", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            OutlinedButton(
                                onClick = { highlightMatches = !highlightMatches },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (highlightMatches) "Clear" else "Highlight All",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showReplace = !showReplace }) {
                        Text(if (showReplace) "Hide Replace" else "Show Replace")
                    }
                    
                    if (showReplace) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            label = { Text("Replace with") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty() && matchCount > 0) {
                                        pushUndo()
                                        // Replace first occurrence
                                        content = content.replaceFirst(searchQuery, replaceQuery, ignoreCase = true)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Replaced 1 occurrence")
                                        }
                                    }
                                },
                                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replace", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Button(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        pushUndo()
                                        val originalCount = matchCount
                                        content = content.replace(searchQuery, replaceQuery, ignoreCase = true)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Replaced $originalCount occurrences")
                                        }
                                    }
                                },
                                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replace All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFindDialog = false }) { Text("Close") }
            }
        )
    }
}
