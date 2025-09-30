package id.xms.xarchiver.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.xms.xarchiver.R
import androidx.compose.ui.platform.LocalContext

@Composable
fun SplashScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    // Define permissions and labels based on Android version
    val isAndroid11OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val permissions = if (isAndroid11OrAbove) {
        listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    val permissionLabels = if (isAndroid11OrAbove) {
        listOf("Manage Storage (All files access)")
    } else {
        listOf("Read Storage Access", "Write Storage Access")
    }

    var permissionStates by remember { mutableStateOf(permissions.map { perm ->
        if (perm == Manifest.permission.MANAGE_EXTERNAL_STORAGE && isAndroid11OrAbove) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }) }

    var currentPermissionToRequest by remember { mutableStateOf<String?>(null) }

    val singlePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentPermissionToRequest?.let { perm ->
            val idx = permissions.indexOf(perm)
            if (idx != -1) {
                permissionStates = permissionStates.toMutableList().also { it[idx] = granted }
            }
            android.util.Log.d("SplashScreen", "Permission $perm granted: $granted")
        }
        currentPermissionToRequest = null
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
        permissionStates = permissions.map { perm -> result[perm] == true }
    }

    fun requestPermissions() {
        val missing = permissions.filterIndexed { i, _ -> !permissionStates[i] }
        if (missing.isNotEmpty()) {
            launcher.launch(missing.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = android.net.Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Enable install permission from this source in settings.", Toast.LENGTH_LONG).show()
        } else {
            onAllPermissionsGranted()
        }
    }

    LaunchedEffect(permissionStates) {
        if (permissionStates.all { it }) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                onAllPermissionsGranted()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Colorful logo icon
        Icon(
            imageVector = Icons.Filled.Storage,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = "XArchiver",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "The application requires the following permissions:",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        permissions.forEachIndexed { i, perm ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = permissionStates[i],
                    onCheckedChange = { checked ->
                        android.util.Log.d("SplashScreen", "Checkbox for $perm clicked, checked: $checked")
                        if (!permissionStates[i] && checked) {
                            if (perm == Manifest.permission.MANAGE_EXTERNAL_STORAGE && isAndroid11OrAbove) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:" + context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Please enable 'All files access' for XArchiver.", Toast.LENGTH_LONG).show()
                            } else {
                                currentPermissionToRequest = perm
                                singlePermissionLauncher.launch(perm)
                            }
                        }
                    },
                    enabled = true,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.secondary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                // Colorful icon for permission state
                Icon(
                    imageVector = if (permissionStates[i]) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (permissionStates[i]) MaterialTheme.colorScheme.primary else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = permissionLabels[i],
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { requestPermissions() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Allow & Continue", fontSize = 16.sp)
        }
    }
    LaunchedEffect(Unit) {
        if (isAndroid11OrAbove) {
            snapshotFlow { android.os.Environment.isExternalStorageManager() }
                .collect { granted ->
                    val idx = permissions.indexOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    if (idx != -1 && granted && !permissionStates[idx]) {
                        permissionStates = permissionStates.toMutableList().also { it[idx] = true }
                    }
                }
        }
    }
}
