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
import id.xms.xarchiver.ui.explorer.ExplorerScreen
import id.xms.xarchiver.ui.home.HomeScreen

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
            AppContent()
        }
    }
}

@Composable
private fun AppContent() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController)
            }
            // explorer route: pass encoded path as single segment to allow slashes
            composable("explorer/{encodedPath}") { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("encodedPath") ?: Uri.encode("/sdcard")
                val path = Uri.decode(encoded)
                ExplorerScreen(path = path, navController = navController)
            }
        }
    }
}
