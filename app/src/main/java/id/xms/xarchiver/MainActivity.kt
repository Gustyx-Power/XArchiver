package id.xms.xarchiver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.xms.xarchiver.ui.archive.ArchiveExplorerScreen
import id.xms.xarchiver.ui.editor.TextEditorScreen
import id.xms.xarchiver.ui.explorer.CategoryExplorerScreen
import id.xms.xarchiver.ui.explorer.ExplorerScreen
import id.xms.xarchiver.ui.explorer.RootExplorerScreen
import id.xms.xarchiver.ui.home.HomeScreen
import id.xms.xarchiver.ui.theme.XArchiverTheme
import id.xms.xarchiver.ui.viewer.AudioPlayerScreen
import id.xms.xarchiver.ui.viewer.ImageViewerScreen
import id.xms.xarchiver.ui.viewer.VideoPlayerScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // "Nakal" permission flow (dev/testing). Pastikan mengerti risikonya.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // buka setting agar user bisa grant MANAGE_EXTERNAL_STORAGE
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0
            )
        }

        setContent {
            XArchiverTheme {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }

            // Category Explorer route
            composable("category_explorer/{categoryName}") { backStackEntry ->
                val categoryName = Uri.decode(
                    backStackEntry.arguments?.getString("categoryName") ?: "Images"
                )
                CategoryExplorerScreen(categoryName = categoryName, navController = navController)
            }

            composable("explorer/{encodedPath}") { backStackEntry ->
                val path = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: Uri.encode("/sdcard")
                )
                ExplorerScreen(path = path, navController = navController)
            }

            composable("root_explorer/{encodedPath}") { backStackEntry ->
                val path = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: Uri.encode("/")
                )
                RootExplorerScreen(path = path, navController = navController)
            }

            // Archive Explorer routes
            composable("archive_explorer/{encodedArchivePath}") { backStackEntry ->
                val archivePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedArchivePath") ?: ""
                )
                ArchiveExplorerScreen(archivePath = archivePath, navController = navController)
            }

            composable("archive_explorer/{encodedArchivePath}/{encodedNestedPath}") { backStackEntry ->
                val archivePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedArchivePath") ?: ""
                )
                val nestedPath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedNestedPath") ?: ""
                )
                ArchiveExplorerScreen(
                    archivePath = archivePath,
                    nestedPath = nestedPath,
                    navController = navController
                )
            }
            
            // Text Editor route
            composable("text_editor/{encodedPath}") { backStackEntry ->
                val filePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: ""
                )
                TextEditorScreen(filePath = filePath, navController = navController)
            }
            
            // Image Viewer route
            composable("image_viewer/{encodedPath}") { backStackEntry ->
                val filePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: ""
                )
                ImageViewerScreen(filePath = filePath, navController = navController)
            }
            
            // Audio Player route
            composable("audio_player/{encodedPath}") { backStackEntry ->
                val filePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: ""
                )
                AudioPlayerScreen(filePath = filePath, navController = navController)
            }
            
            // Video Player route
            composable("video_player/{encodedPath}") { backStackEntry ->
                val filePath = Uri.decode(
                    backStackEntry.arguments?.getString("encodedPath") ?: ""
                )
                VideoPlayerScreen(filePath = filePath, navController = navController)
            }
        }
    }
}
