package id.xms.xarchiver.ui.explorer.utils

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.ShareUtils
import id.xms.xarchiver.core.archive.ArchiveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

object FileActionHandler {
    
    fun handleFileClick(
        context: Context,
        file: FileItem,
        navController: NavController,
        archiveManager: ArchiveManager,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        onApkClick: (File) -> Unit
    ) {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        val actualFile = File(file.path)
        
        when {
            file.isDirectory -> {
                navController.navigate("explorer/${Uri.encode(file.path)}")
            }
            file.name.endsWith(".apk", ignoreCase = true) -> {
                onApkClick(File(file.path))
            }
            FileTypeDetector.isArchiveExtension(file.name) -> {
                navController.navigate("archive_explorer/${Uri.encode(file.path)}")
            }
            FileTypeDetector.isImageExtension(ext) -> {
                navController.navigate("image_viewer/${Uri.encode(file.path)}")
            }
            FileTypeDetector.isAudioExtension(ext) -> {
                navController.navigate("audio_player/${Uri.encode(file.path)}")
            }
            FileTypeDetector.isVideoExtension(ext) -> {
                navController.navigate("video_player/${Uri.encode(file.path)}")
            }
            FileTypeDetector.isTextExtension(ext) -> {
                handleTextFile(actualFile, file.path, navController, scope, snackbarHostState)
            }
            FileTypeDetector.isDocumentExtension(ext) -> {
                ShareUtils.openFile(context, file.path)
            }
            else -> {
                handleUnknownFile(
                    actualFile, file.path, ext, 
                    archiveManager, navController, 
                    scope, snackbarHostState
                )
            }
        }
    }
    
    private fun handleTextFile(
        file: File,
        filePath: String,
        navController: NavController,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState
    ) {
        val maxSize = 10 * 1024 * 1024 // 10MB
        if (file.length() > maxSize) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "File too large to open as text (${file.length() / (1024 * 1024)}MB). Maximum: 10MB"
                )
            }
        } else {
            navController.navigate("text_editor/${Uri.encode(filePath)}")
        }
    }
    
    private fun handleUnknownFile(
        file: File,
        filePath: String,
        ext: String,
        archiveManager: ArchiveManager,
        navController: NavController,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState
    ) {
        scope.launch {
            // Check for payload.bin first
            if (FileTypeDetector.isPayloadBin(file.name)) {
                navController.navigate("payload_viewer/${Uri.encode(filePath)}")
                return@launch
            }
            
            if (archiveManager.isArchiveFile(filePath)) {
                navController.navigate("archive_explorer/${Uri.encode(filePath)}")
            } else {
                when {
                    FileTypeDetector.isBinaryExtension(ext) -> {
                        snackbarHostState.showSnackbar("Cannot open binary file (.${ext}) as text")
                    }
                    file.length() > 10 * 1024 * 1024 -> {
                        snackbarHostState.showSnackbar(
                            "File too large to open as text (${file.length() / (1024 * 1024)}MB). Maximum: 10MB"
                        )
                    }
                    else -> {
                        navController.navigate("text_editor/${Uri.encode(filePath)}")
                    }
                }
            }
        }
    }
}
