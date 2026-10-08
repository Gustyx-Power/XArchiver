package id.xms.xarchiver.ui.explorer.search

import com.topjohnwu.superuser.io.SuFile
import id.xms.xarchiver.core.FileItem
import id.xms.xarchiver.core.root.RootService
import id.xms.xarchiver.core.root.ShizukuService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

object ExplorerSearchEngine {

    /**
     * Search files and folders scoped strictly within [scopePath] and its subdirectories.
     * Guaranteed to only return items inside [scopePath].
     */
    suspend fun searchScopedDirectory(
        scopePath: String,
        query: String,
        category: ExplorerSearchCategory = ExplorerSearchCategory.ALL,
        isDeepSearch: Boolean = false,
        maxResults: Int = 400
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val scopeDir = File(scopePath)

        // If query is empty and category is ALL, return empty list (landing state)
        if (cleanQuery.isEmpty() && category == ExplorerSearchCategory.ALL) {
            return@withContext emptyList()
        }

        val results = mutableListOf<FileItem>()
        val depth = if (isDeepSearch) 30 else 12
        val limit = if (isDeepSearch) 1000 else maxResults

        if (scopeDir.exists() && scopeDir.isDirectory && scopeDir.canRead()) {
            try {
                scopeDir.walkTopDown()
                    .maxDepth(depth)
                    .onEnter { dir ->
                        coroutineContext.ensureActive()
                        // Don't recurse into unreadable dirs, skip hidden unless deep search
                        dir.canRead() && (isDeepSearch || !dir.name.startsWith("."))
                    }
                    .filter { file ->
                        coroutineContext.ensureActive()
                        if (file.absolutePath == scopeDir.absolutePath) return@filter false

                        val matchesQuery = cleanQuery.isEmpty() || file.name.contains(cleanQuery, ignoreCase = true)
                        if (!matchesQuery) return@filter false

                        // If file is directory, it matches if category is ALL
                        if (file.isDirectory) {
                            category == ExplorerSearchCategory.ALL
                        } else {
                            val ext = file.name.substringAfterLast('.', "").lowercase()
                            val tempItem = FileItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = false,
                                size = file.length(),
                                lastModified = file.lastModified()
                            )
                            category.matches(tempItem)
                        }
                    }
                    .take(limit)
                    .forEach { file ->
                        coroutineContext.ensureActive()
                        val count = if (file.isDirectory) {
                            try { file.list()?.size ?: 0 } catch (_: Exception) { 0 }
                        } else null

                        results.add(
                            FileItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                itemCount = count
                            )
                        )
                    }

                return@withContext results.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
            } catch (_: Exception) {
                // fall through to privileged search if permitted
            }
        }

        // Fallback for root / Shizuku if standard java.io.File couldn't access
        if (RootService.isGranted()) {
            try {
                val suDir = SuFile.open(scopePath)
                if (suDir.exists() && suDir.isDirectory) {
                    suDir.walkTopDown()
                        .maxDepth(depth)
                        .filter { file ->
                            coroutineContext.ensureActive()
                            if (file.absolutePath == suDir.absolutePath) return@filter false
                            val fileName = file.name ?: ""
                            val matchesQuery = cleanQuery.isEmpty() || fileName.contains(cleanQuery, ignoreCase = true)
                            if (!matchesQuery) return@filter false

                            if (file.isDirectory) {
                                category == ExplorerSearchCategory.ALL
                            } else {
                                val tempItem = FileItem(
                                    name = fileName,
                                    path = file.absolutePath,
                                    isDirectory = false,
                                    size = file.length(),
                                    lastModified = file.lastModified()
                                )
                                category.matches(tempItem)
                            }
                        }
                        .take(limit)
                        .forEach { file ->
                            coroutineContext.ensureActive()
                            val count = if (file.isDirectory) file.list()?.size ?: 0 else null
                            results.add(
                                FileItem(
                                    name = file.name ?: "",
                                    path = file.absolutePath,
                                    isDirectory = file.isDirectory,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    itemCount = count
                                )
                            )
                        }
                    return@withContext results.sortedWith(
                        compareByDescending<FileItem> { it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    )
                }
            } catch (_: Exception) {}
        }

        return@withContext results
    }
}
