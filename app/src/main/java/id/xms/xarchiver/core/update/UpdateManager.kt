package id.xms.xarchiver.core.update

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.xms.xarchiver.core.install.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String
) {
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}

data class UpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkAsset: ReleaseAsset,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateManager {

    private const val GITHUB_REPO_API =
        "https://api.github.com/repos/Xtra-Manager-Software/XArchiver/releases"

    /**
     * Observable reactive update state for UI (badge dots, dialogs, etc.)
     */
    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
    var showDialog by mutableStateOf(false)
    var isChecking by mutableStateOf(false)

    /**
     * Check GitHub Releases for updates compared to [currentVersion].
     */
    suspend fun checkForUpdate(
        currentVersion: String = "2.1.0"
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        android.util.Log.d("OTA", "checkForUpdate started with currentVersion=$currentVersion")
        try {
            // First try /releases/latest, fallback to /releases list
            var releaseJson: JSONObject? = null
            try {
                val latestUrl = URL("$GITHUB_REPO_API/latest")
                val response = fetchHttpString(latestUrl)
                releaseJson = JSONObject(response)
                android.util.Log.d("OTA", "Fetched /latest successfully")
            } catch (e: Exception) {
                android.util.Log.w("OTA", "Failed /latest: ${e.message}, falling back to /releases list")
                // If /latest fails (e.g. pre-releases only), fetch first item from /releases array
                val listUrl = URL(GITHUB_REPO_API)
                val response = fetchHttpString(listUrl)
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    releaseJson = jsonArray.getJSONObject(0)
                }
            }

            if (releaseJson == null) {
                return@withContext UpdateCheckResult.Error("No release data found on GitHub.")
            }

            val tagName = releaseJson.optString("tag_name", "")
            val title = releaseJson.optString("name", "XArchiver Update")
            val body = releaseJson.optString("body", "")
            val publishedAt = releaseJson.optString("published_at", "")

            val remoteVersion = extractVersionString(tagName)
            if (remoteVersion.isBlank()) {
                return@withContext UpdateCheckResult.Error("Invalid release tag format: $tagName")
            }

            // Find APK asset in release
            val assetsArray = releaseJson.optJSONArray("assets") ?: JSONArray()
            var apkAsset: ReleaseAsset? = null
            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.getJSONObject(i)
                val assetName = assetObj.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    val downloadUrl = assetObj.optString("browser_download_url", "")
                    val size = assetObj.optLong("size", 0L)
                    if (downloadUrl.isNotBlank()) {
                        apkAsset = ReleaseAsset(
                            name = assetName,
                            size = size,
                            downloadUrl = downloadUrl
                        )
                        break
                    }
                }
            }

            if (apkAsset == null) {
                return@withContext UpdateCheckResult.UpToDate(currentVersion)
            }

            if (isNewerVersion(remoteVersion, currentVersion)) {
                val updateInfo = UpdateInfo(
                    versionName = remoteVersion,
                    tagName = tagName,
                    releaseTitle = title,
                    changelog = body,
                    apkAsset = apkAsset,
                    publishedAt = publishedAt
                )
                availableUpdate = updateInfo
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateCheckResult.Error(e.message ?: "Failed to connect to update server.")
        }
    }

    /**
     * Silent check performed on app launch or in background.
     * Only triggers a system status bar notification if a new version is detected
     * that has not been notified yet.
     */
    suspend fun checkSilently(
        context: Context,
        currentVersion: String = "2.1.0"
    ) {
        android.util.Log.d("OTA", "checkSilently started")
        when (val result = checkForUpdate(currentVersion)) {
            is UpdateCheckResult.UpdateAvailable -> {
                android.util.Log.d("OTA", "checkSilently update available: ${result.updateInfo.tagName}")
                availableUpdate = result.updateInfo
                val prefs = context.getSharedPreferences("xarchiver_ota_prefs", Context.MODE_PRIVATE)
                val lastNotified = prefs.getString("last_notified_tag", null)
                if (lastNotified != result.updateInfo.tagName) {
                    android.util.Log.d("OTA", "Posting update notification for ${result.updateInfo.tagName}")
                    UpdateNotificationHelper.showUpdateNotification(context, result.updateInfo)
                    prefs.edit().putString("last_notified_tag", result.updateInfo.tagName).apply()
                } else {
                    android.util.Log.d("OTA", "Already notified for ${result.updateInfo.tagName}")
                }
            }
            is UpdateCheckResult.UpToDate -> {
                android.util.Log.d("OTA", "checkSilently UpToDate")
            }
            is UpdateCheckResult.Error -> {
                android.util.Log.e("OTA", "checkSilently Error: ${result.message}")
            }
        }
    }

    /**
     * Download the release APK file with real-time byte progress.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long, progressPercent: Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val safeName = fileName.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
        val apkFile = File(updatesDir, safeName)
        if (apkFile.exists()) apkFile.delete()

        var connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("User-Agent", "XArchiver-App")
            connect()
        }

        // GitHub redirects release downloads to AWS S3 CDN
        var redirects = 0
        while (connection.responseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308) && redirects < 6) {
            val redirectUrl = connection.getHeaderField("Location") ?: break
            connection.disconnect()
            connection = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("User-Agent", "XArchiver-App")
                connect()
            }
            redirects++
        }

        if (connection.responseCode !in 200..299) {
            throw IOException("Download failed with HTTP ${connection.responseCode}: ${connection.responseMessage}")
        }

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(apkFile).use { output ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val percent = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                    onProgress(downloadedBytes, totalBytes, percent)
                }
                output.flush()
            }
        }

        apkFile
    }

    /**
     * Trigger package installer for the downloaded APK file.
     */
    fun installApk(context: Context, apkFile: File) {
        UpdateNotificationHelper.cancelUpdateNotification(context)
        ApkInstaller.installApk(context, apkFile)
    }

    private fun fetchHttpString(url: URL): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "XArchiver-App")
            connect()
        }

        if (conn.responseCode !in 200..299) {
            throw IOException("HTTP error: ${conn.responseCode} ${conn.responseMessage}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Extracts numeric version like "2.1.0" from strings like "release-2.1.0", "v2.1.0", "2.1.0".
     */
    fun extractVersionString(input: String): String {
        val regex = Regex("""(\d+\.\d+(?:\.\d+)?)""")
        val match = regex.find(input)
        return match?.value ?: ""
    }

    /**
     * Compare semantic versions. Returns true if [remoteVersion] is higher than [currentVersion].
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteDigits = remoteVersion.split('.').mapNotNull { it.toIntOrNull() }
        val currentDigits = currentVersion.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteDigits.size, currentDigits.size)
        for (i in 0 until length) {
            val r = remoteDigits.getOrElse(i) { 0 }
            val c = currentDigits.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
