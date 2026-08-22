package id.xms.xarchiver.core

import id.xms.xarchiver.core.root.RootFileService
import id.xms.xarchiver.core.root.RootService
import id.xms.xarchiver.core.root.ShizukuService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileService {
    
    private fun isPrivilegedEnabled() = RootService.isGranted() || ShizukuService.isGranted()
    
    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        
        // Use standard java.io.File if possible
        if (dir.exists() && dir.isDirectory && dir.canRead()) {
            return@withContext dir.listFiles()?.map {
                FileItem(
                    name = it.name,
                    path = it.absolutePath,
                    isDirectory = it.isDirectory,
                    size = it.length(),
                    lastModified = it.lastModified()
                )
            }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name }) ?: emptyList()
        }
        
        // Fallback to RootFileService if privileged access is available
        if (isPrivilegedEnabled()) {
            return@withContext RootFileService.listDirectory(path).sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })
        }
        
        return@withContext emptyList()
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(oldPath)
        val newFile = File(file.parent, newName)
        
        if (file.exists() && file.canWrite()) {
            return@withContext file.renameTo(newFile)
        }
        
        if (isPrivilegedEnabled()) {
            return@withContext RootFileService.rename(oldPath, newName)
        }
        
        return@withContext false
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        
        if (file.exists() && (file.parentFile?.canWrite() == true || file.canWrite())) {
            return@withContext if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
        
        if (isPrivilegedEnabled()) {
            return@withContext RootFileService.delete(listOf(path))
        }
        
        return@withContext false
    }
}
