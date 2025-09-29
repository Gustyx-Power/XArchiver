package id.xms.xarchiver.core

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

object StorageUtils {

    fun getAllStorage(context: Context): List<StorageInfo> {
        val storages = mutableListOf<StorageInfo>()

        // Internal
        storages.add(getStorageInfo(Environment.getExternalStorageDirectory(), "Internal Storage"))

        // Eksternal (SD card) → via Context.externalMediaDirs
        context.externalMediaDirs?.forEach { dir ->
            dir?.let {
                val base = it.absoluteFile
                // pastikan path ada dan berbeda dari internal
                if (base.exists() && !isSameAsInternal(base)) {
                    storages.add(getStorageInfo(base, "SD Card"))
                }
            }
        }

        return storages
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
