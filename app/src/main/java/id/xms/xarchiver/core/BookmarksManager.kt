package id.xms.xarchiver.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance for bookmarks
private val Context.bookmarksDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

/**
 * Data class representing a bookmark
 */
data class Bookmark(
    val path: String,
    val name: String
)

/**
 * Manager class for handling file/folder bookmarks using DataStore
 */
class BookmarksManager(private val context: Context) {
    
    private val bookmarksKey = stringSetPreferencesKey("bookmarks_set")
    
    /**
     * Get all bookmarks as a Flow
     */
    val bookmarks: Flow<List<Bookmark>> = context.bookmarksDataStore.data.map { preferences ->
        preferences[bookmarksKey]?.map { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) {
                Bookmark(path = parts[0], name = parts[1])
            } else {
                Bookmark(path = entry, name = entry.substringAfterLast('/'))
            }
        }?.sortedBy { it.name } ?: emptyList()
    }
    
    /**
     * Add a bookmark
     */
    suspend fun addBookmark(path: String, name: String? = null) {
        val displayName = name ?: path.substringAfterLast('/')
        val entry = "$path|$displayName"
        
        context.bookmarksDataStore.edit { preferences ->
            val currentSet = preferences[bookmarksKey] ?: emptySet()
            // Remove any existing bookmark for this path
            val filtered = currentSet.filter { !it.startsWith("$path|") }.toMutableSet()
            filtered.add(entry)
            preferences[bookmarksKey] = filtered
        }
    }
    
    /**
     * Remove a bookmark
     */
    suspend fun removeBookmark(path: String) {
        context.bookmarksDataStore.edit { preferences ->
            val currentSet = preferences[bookmarksKey] ?: emptySet()
            preferences[bookmarksKey] = currentSet.filter { !it.startsWith("$path|") }.toSet()
        }
    }
    
    /**
     * Check if a path is bookmarked
     */
    suspend fun isBookmarked(path: String): Boolean {
        var result = false
        context.bookmarksDataStore.edit { preferences ->
            val currentSet = preferences[bookmarksKey] ?: emptySet()
            result = currentSet.any { it.startsWith("$path|") }
        }
        return result
    }
    
    /**
     * Toggle bookmark status
     */
    suspend fun toggleBookmark(path: String, name: String? = null): Boolean {
        var isNowBookmarked = false
        context.bookmarksDataStore.edit { preferences ->
            val currentSet = preferences[bookmarksKey] ?: emptySet()
            val existingEntry = currentSet.find { it.startsWith("$path|") }
            
            if (existingEntry != null) {
                // Remove bookmark
                preferences[bookmarksKey] = currentSet - existingEntry
                isNowBookmarked = false
            } else {
                // Add bookmark
                val displayName = name ?: path.substringAfterLast('/')
                val entry = "$path|$displayName"
                preferences[bookmarksKey] = currentSet + entry
                isNowBookmarked = true
            }
        }
        return isNowBookmarked
    }
}
