package id.xms.xarchiver.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_MODE_KEY]) {
            0 -> ThemeMode.LIGHT
            1 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val isDynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = when (themeMode) {
                ThemeMode.LIGHT -> 0
                ThemeMode.DARK -> 1
                ThemeMode.SYSTEM -> 2
            }
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
