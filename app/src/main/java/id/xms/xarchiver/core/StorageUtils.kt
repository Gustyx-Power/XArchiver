package id.xms.xarchiver.core

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

object StorageUtils {

    fun getAllStorage(context: Context): List<StorageInfo> {
        val storages = mutableListOf<StorageInfo>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
        val externalDirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)

        // Internal Storage
        storages.add(getStorageInfo(Environment.getExternalStorageDirectory(), "Internal Storage"))

        // Removable storage (SD Card, USB OTG)
        storageManager.storageVolumes.forEach { volume ->
            if (!volume.isPrimary) {
                var rootPath: String? = null
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    rootPath = volume.directory?.absolutePath
                }

                if (rootPath == null) {
                    try {
                        rootPath = volume.javaClass.getMethod("getPath").invoke(volume) as String
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                if (rootPath != null) {
                    val rootFile = File(rootPath)
                    if (!isSameAsInternal(rootFile)) {
                        val label = volume.getDescription(context)
                        if (storages.none { it.path == rootPath }) {
                            storages.add(getStorageInfo(rootFile, label))
                        }
                    }
                }
            }
        }

        // AGGRESSIVE FALLBACK 1: ContextCompat.getExternalFilesDirs
        externalDirs.forEach { dir ->
            if (dir != null) {
                val rootStr = dir.absolutePath.substringBefore("/Android/data").substringBefore("/Android/media")
                val rootFile = File(rootStr)
                if (rootStr.startsWith("/storage/") && !isSameAsInternal(rootFile) && rootFile.name != "emulated") {
                    if (storages.none { it.path == rootStr }) {
                        val isUsb = rootStr.contains("-")
                        val label = if (isUsb) "USB Storage (${rootFile.name})" else "SD Card (${rootFile.name})"
                        storages.add(getStorageInfo(rootFile, label))
                    }
                }
            }
        }

        // AGGRESSIVE FALLBACK 2: Scan /storage/ manually
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory && dir.name != "emulated" && dir.name != "self") {
                        if (storages.none { it.path == dir.absolutePath }) {
                            val isUsb = dir.name.contains("-")
                            val label = if (isUsb) "Removable Storage (${dir.name})" else "External Storage (${dir.name})"
                            storages.add(getStorageInfo(dir, label))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        return storages
    }

    fun getRootStorageInfo(): StorageInfo {
        return getStorageInfo(File("/"), "Root System")
    }

    private fun getStorageInfo(path: File, label: String): StorageInfo {
        return try {
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val used = total - (availableBlocks * blockSize)

            StorageInfo(used, total, label, path.absolutePath)
        } catch (e: Exception) {
            StorageInfo(0, 0, label, path.absolutePath)
        }
    }

    private fun isSameAsInternal(file: File): Boolean {
        return file.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)
    }
}
