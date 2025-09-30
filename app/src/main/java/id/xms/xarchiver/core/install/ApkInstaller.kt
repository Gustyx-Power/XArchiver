package id.xms.xarchiver.core.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File
import android.os.Build
import android.provider.Settings

object ApkInstaller {
    fun installApk(context: Context, file: File) {
        // Check permission for installing unknown apps (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                // Arahkan user ke pengaturan untuk mengaktifkan izin
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:" + context.packageName)
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                android.widget.Toast.makeText(context, "Aktifkan izin install dari sumber ini di pengaturan.", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Only add FLAG_ACTIVITY_NEW_TASK if context is not Activity
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Check if there is an activity to handle the intent
        val pm: PackageManager = context.packageManager
        val resolveInfo = intent.resolveActivity(pm)
        if (resolveInfo != null) {
            try {
                context.startActivity(Intent.createChooser(intent, "Install APK"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Optionally, show a message to the user
            android.widget.Toast.makeText(context, "No installer found.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
