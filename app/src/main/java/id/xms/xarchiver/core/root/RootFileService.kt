package id.xms.xarchiver.core.root

import com.topjohnwu.superuser.io.SuFile
import id.xms.xarchiver.core.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootFileService {
    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return@withContext ShizukuFileService.listDirectory(path)
        }
        
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

    private suspend fun runCommand(cmd: String): Boolean = withContext(Dispatchers.IO) {
        if (!RootService.isGranted()) {
            if (ShizukuService.isGranted()) {
                // If only Shizuku is granted but this is reached, route to ShizukuFileService?
                // Actually the methods will handle this routing.
                return@withContext false
            }
            return@withContext false
        }
        val result = com.topjohnwu.superuser.Shell.cmd(cmd).exec()
        return@withContext result.isSuccess
    }

    suspend fun delete(paths: List<String>): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.delete(paths)
        }
        val pathsStr = paths.joinToString(" ") { "\"$it\"" }
        return runCommand("rm -rf $pathsStr")
    }

    suspend fun rename(oldPath: String, newName: String): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.rename(oldPath, newName)
        }
        val parent = java.io.File(oldPath).parent ?: "/"
        val newPath = if (parent.endsWith("/")) "$parent$newName" else "$parent/$newName"
        return runCommand("mv \"$oldPath\" \"$newPath\"")
    }

    suspend fun createFolder(parentPath: String, name: String): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.createFolder(parentPath, name)
        }
        val path = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        return runCommand("mkdir -p \"$path\"")
    }

    suspend fun createFile(parentPath: String, name: String): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.createFile(parentPath, name)
        }
        val path = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        return runCommand("touch \"$path\"")
    }

    suspend fun copy(sourcePaths: List<String>, destPath: String): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.copy(sourcePaths, destPath)
        }
        val pathsStr = sourcePaths.joinToString(" ") { "\"$it\"" }
        return runCommand("cp -rf $pathsStr \"$destPath\"")
    }

    suspend fun move(sourcePaths: List<String>, destPath: String): Boolean {
        if (!RootService.isGranted() && ShizukuService.isGranted()) {
            return ShizukuFileService.move(sourcePaths, destPath)
        }
        val pathsStr = sourcePaths.joinToString(" ") { "\"$it\"" }
        return runCommand("mv $pathsStr \"$destPath\"")
    }

    suspend fun readText(filePath: String): String? = withContext(Dispatchers.IO) {
        if (!RootService.isGranted()) return@withContext null
        
        runCatching {
            com.topjohnwu.superuser.io.SuFileInputStream.open(filePath).bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    suspend fun writeText(filePath: String, text: String): Boolean = withContext(Dispatchers.IO) {
        if (!RootService.isGranted()) return@withContext false

        val isSystem = filePath.startsWith("/system") || filePath.startsWith("/vendor")
        if (isSystem) {
            runCommand("mount -o rw,remount /")
            runCommand("mount -o rw,remount /system")
        }

        val success = runCatching {
            com.topjohnwu.superuser.io.SuFileOutputStream.open(filePath).bufferedWriter().use { it.write(text) }
            true
        }.getOrDefault(false)

        if (isSystem && success) {
            runCommand("chmod 644 \"$filePath\"")
            runCommand("chown root:root \"$filePath\"")
            runCommand("mount -o ro,remount /")
            runCommand("mount -o ro,remount /system")
        }
        
        return@withContext success
    }
}
