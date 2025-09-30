package id.xms.xarchiver.core

import java.io.File

object FileService {
    fun listDirectory(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.map {
            FileItem(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                size = it.length(),
                lastModified = it.lastModified()
            )
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name }) ?: emptyList()
    }

    fun renameFile(oldPath: String, newName: String): Boolean {
        val file = File(oldPath)
        val newFile = File(file.parent, newName)
        return file.exists() && file.renameTo(newFile)
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.delete()
    }
}
