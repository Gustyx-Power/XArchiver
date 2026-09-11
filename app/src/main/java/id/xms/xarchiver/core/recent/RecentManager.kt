package id.xms.xarchiver.core.recent

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import id.xms.xarchiver.XArchiverApp
import id.xms.xarchiver.core.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object RecentManager {
    private const val PREFS_NAME = "xarchiver_recent_prefs"
    private const val KEY_RECENTS = "recent_items_json"
    private const val MAX_TRACKED_ITEMS = 100

    /**
     * Record a file or folder that was created, renamed, extracted, or touched.
     */
    fun recordFile(path: String) {
        try {
            val context = try { XArchiverApp.instance } catch (e: Exception) { null } ?: return
            val file = File(path)
            if (!file.exists()) return

            // Scan with MediaScanner if it's a file
            if (file.isFile) {
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            val newArray = JSONArray()
            val entry = JSONObject().apply {
                put("path", file.absolutePath)
                put("time", System.currentTimeMillis())
                put("isDir", file.isDirectory)
            }
            newArray.put(entry)

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val existingPath = obj.optString("path")
                if (existingPath.isNotEmpty() && existingPath != file.absolutePath && File(existingPath).exists()) {
                    newArray.put(obj)
                    if (newArray.length() >= MAX_TRACKED_ITEMS) break
                }
            }

            prefs.edit().putString(KEY_RECENTS, newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get recent files and folders grouped by directory name.
     */
    suspend fun getRecentFilesGrouped(context: Context): Map<String, List<FileItem>> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<String, FileItem>() // canonicalPath -> FileItem
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        // 1. Load tracked items from SharedPreferences
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val path = obj.optString("path")
                val isDir = obj.optBoolean("isDir", false)
                if (path.isNotEmpty()) {
                    val file = File(path)
                    if (file.exists()) {
                        val item = FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = isDir || file.isDirectory,
                            size = if (file.isFile) file.length() else 0L,
                            lastModified = file.lastModified()
                        )
                        resultMap[file.canonicalPath] = item
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scan external storage for recently created / modified files and folders
        try {
            val storageRoot = Environment.getExternalStorageDirectory()
            if (storageRoot != null && storageRoot.exists() && storageRoot.canRead()) {
                scanStorageDirectory(storageRoot, sevenDaysAgo, resultMap, depth = 0, maxDepth = 2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Query MediaStore for media files (Images, Videos, Audios, Downloads)
        try {
            queryMediaStore(context, resultMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Filter and sort
        val allItems = resultMap.values
            .filter { File(it.path).exists() }
            .sortedByDescending { it.lastModified }
            .take(80)

        // 5. Group by folder
        val grouped = mutableMapOf<String, MutableList<FileItem>>()
        for (item in allItems) {
            val folderName = getDisplayFolderName(item.path)
            grouped.getOrPut(folderName) { mutableListOf() }.add(item)
        }

        // Sort folders by the newest item in each folder
        val sortedGrouped = grouped.toList()
            .sortedByDescending { (_, list) -> list.maxOfOrNull { it.lastModified } ?: 0L }
            .toMap()

        return@withContext sortedGrouped
    }

    private fun scanStorageDirectory(
        dir: File,
        recentThreshold: Long,
        resultMap: MutableMap<String, FileItem>,
        depth: Int,
        maxDepth: Int
    ) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val name = file.name
            // Skip system/hidden/Android data caches
            if (name.startsWith(".")) continue
            if (depth == 0 && name.equals("Android", ignoreCase = true)) continue

            val lastMod = file.lastModified()
            if (file.isDirectory) {
                // If folder was modified recently (e.g. newly created folder)
                if (lastMod >= recentThreshold && depth <= 1) {
                    resultMap[file.canonicalPath] = FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = true,
                        size = 0L,
                        lastModified = lastMod
                    )
                }
                if (depth < maxDepth) {
                    scanStorageDirectory(file, recentThreshold, resultMap, depth + 1, maxDepth)
                }
            } else {
                // File: include if modified recently or if root
                if (lastMod >= recentThreshold || depth == 0) {
                    resultMap[file.canonicalPath] = FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        size = file.length(),
                        lastModified = lastMod
                    )
                }
            }
        }
    }

    private fun queryMediaStore(context: Context, resultMap: MutableMap<String, FileItem>) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT 40"

        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )

        for (uri in uris) {
            try {
                context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val path = if (dataCol != -1) cursor.getString(dataCol) else null
                        if (path.isNullOrEmpty()) continue

                        val file = File(path)
                        if (!file.exists()) continue

                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: file.name else file.name
                        val size = if (sizeCol != -1) cursor.getLong(sizeCol) else file.length()
                        val date = if (dateCol != -1) cursor.getLong(dateCol) * 1000L else file.lastModified()

                        resultMap[file.canonicalPath] = FileItem(
                            name = name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = size,
                            lastModified = if (date > 0) date else file.lastModified()
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore individual query failures
            }
        }
    }

    private fun getDisplayFolderName(filePath: String): String {
        val file = File(filePath)
        val parent = file.parentFile ?: return "Storage"
        val parentName = parent.name

        // Friendly mapping for well-known folders
        val lowerPath = filePath.lowercase()
        return when {
            lowerPath.contains("/screenshots") || lowerPath.contains("/tangkapan layar") -> "Tangkapan layar"
            lowerPath.contains("/dcim/camera") || lowerPath.contains("/camera") -> "Kamera"
            lowerPath.contains("whatsapp") -> "WhatsApp"
            parentName.equals("0", ignoreCase = true) || parentName.equals("sdcard", ignoreCase = true) -> "Internal Storage"
            else -> parentName
        }
    }
}
