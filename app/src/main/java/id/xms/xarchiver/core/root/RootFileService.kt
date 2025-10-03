package id.xms.xarchiver.core.root

import com.topjohnwu.superuser.io.SuFile
import id.xms.xarchiver.core.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootFileService {
    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = SuFile.open(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        dir.listFiles()?.map { f ->
            FileItem(
                name = f.name ?: "",
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                size = f.length(),
                lastModified = f.lastModified()
            )
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })
            ?: emptyList()
    }
}
