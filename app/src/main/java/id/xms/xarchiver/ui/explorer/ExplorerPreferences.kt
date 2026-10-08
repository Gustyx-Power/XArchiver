package id.xms.xarchiver.ui.explorer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import id.xms.xarchiver.ui.theme.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ExplorerViewMode {
    LIST,
    WATERFALL,
    GRID
}

enum class ExplorerSortBy {
    NAME,
    DATE,
    SIZE,
    TYPE
}

enum class ExplorerSortOrder {
    DESCENDING, // Turun
    ASCENDING   // Naik
}

class ExplorerPreferences(private val context: Context) {
    companion object {
        private val VIEW_MODE_KEY = intPreferencesKey("explorer_view_mode")
        private val SORT_BY_KEY = intPreferencesKey("explorer_sort_by")
        private val SORT_ORDER_KEY = intPreferencesKey("explorer_sort_order")
    }

    val viewMode: Flow<ExplorerViewMode> = context.dataStore.data.map { preferences ->
        when (preferences[VIEW_MODE_KEY]) {
            1 -> ExplorerViewMode.WATERFALL
            2 -> ExplorerViewMode.GRID
            else -> ExplorerViewMode.LIST
        }
    }

    suspend fun setViewMode(mode: ExplorerViewMode) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = when (mode) {
                ExplorerViewMode.LIST -> 0
                ExplorerViewMode.WATERFALL -> 1
                ExplorerViewMode.GRID -> 2
            }
        }
    }

    val sortBy: Flow<ExplorerSortBy> = context.dataStore.data.map { preferences ->
        when (preferences[SORT_BY_KEY]) {
            1 -> ExplorerSortBy.DATE
            2 -> ExplorerSortBy.SIZE
            3 -> ExplorerSortBy.TYPE
            else -> ExplorerSortBy.NAME
        }
    }

    suspend fun setSortBy(sortBy: ExplorerSortBy) {
        context.dataStore.edit { preferences ->
            preferences[SORT_BY_KEY] = when (sortBy) {
                ExplorerSortBy.NAME -> 0
                ExplorerSortBy.DATE -> 1
                ExplorerSortBy.SIZE -> 2
                ExplorerSortBy.TYPE -> 3
            }
        }
    }

    val sortOrder: Flow<ExplorerSortOrder> = context.dataStore.data.map { preferences ->
        when (preferences[SORT_ORDER_KEY]) {
            1 -> ExplorerSortOrder.ASCENDING
            else -> ExplorerSortOrder.DESCENDING
        }
    }

    suspend fun setSortOrder(order: ExplorerSortOrder) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = when (order) {
                ExplorerSortOrder.DESCENDING -> 0
                ExplorerSortOrder.ASCENDING -> 1
            }
        }
    }
}
