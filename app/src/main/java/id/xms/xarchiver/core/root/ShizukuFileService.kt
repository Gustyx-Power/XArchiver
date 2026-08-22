package id.xms.xarchiver.core.root

import id.xms.xarchiver.core.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

object ShizukuFileService {

    // Parses `ls -lA` output which looks like:
    // -rw-rw---- 1 u0_a123 ext_data_rw 1234 2023-10-01 12:00 my_file.txt
    // drwxrwx--- 2 u0_a123 ext_data_rw 4096 2023-10-01 12:00 my_folder
    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        if (!ShizukuService.isGranted()) return@withContext emptyList()

        val items = mutableListOf<FileItem>()
        val cmd = "ls -lA \"$path\" 2>/dev/null"
        
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                if (l.startsWith("total ")) continue
                
                // Usually 7 or more parts: perms links owner group size date time name
                val parts = l.trim().split("\\s+".toRegex(), 8)
                if (parts.size < 8) continue
                
                val perms = parts[0]
                val size = parts[4].toLongOrNull() ?: 0L
                val dateStr = "${parts[5]} ${parts[6]}"
                val name = parts[7]
                
                val lastModified = try {
                    dateFormat.parse(dateStr)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
                
                val isDirectory = perms.startsWith("d")
                
                items.add(
                    FileItem(
                        name = name,
                        path = if (path.endsWith("/")) "$path$name" else "$path/$name",
                        isDirectory = isDirectory,
                        size = size,
                        lastModified = lastModified
                    )
                )
            }
            process.waitFor()
            
            return@withContext items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private suspend fun runCommand(cmd: String): Boolean = withContext(Dispatchers.IO) {
        if (!ShizukuService.isGranted()) return@withContext false
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            process.waitFor()
            return@withContext process.exitValue() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun delete(paths: List<String>): Boolean {
        val pathsStr = paths.joinToString(" ") { "\"$it\"" }
        return runCommand("rm -rf $pathsStr")
    }

    suspend fun rename(oldPath: String, newName: String): Boolean {
        val parent = java.io.File(oldPath).parent ?: "/"
        val newPath = if (parent.endsWith("/")) "$parent$newName" else "$parent/$newName"
        return runCommand("mv \"$oldPath\" \"$newPath\"")
    }

    suspend fun createFolder(parentPath: String, name: String): Boolean {
        val path = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        return runCommand("mkdir -p \"$path\"")
    }

    suspend fun createFile(parentPath: String, name: String): Boolean {
        val path = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
        return runCommand("touch \"$path\"")
    }

    suspend fun copy(sourcePaths: List<String>, destPath: String): Boolean {
        val pathsStr = sourcePaths.joinToString(" ") { "\"$it\"" }
        return runCommand("cp -rf $pathsStr \"$destPath\"")
    }

    suspend fun move(sourcePaths: List<String>, destPath: String): Boolean {
        val pathsStr = sourcePaths.joinToString(" ") { "\"$it\"" }
        return runCommand("mv $pathsStr \"$destPath\"")
    }
}
