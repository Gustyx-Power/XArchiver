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

import android.os.Build
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val ROOT_ACCESS_KEY = booleanPreferencesKey("root_access")
        private val MIUIX_UI_KEY = booleanPreferencesKey("miuix_ui")

        /**
         * Detects whether the current device is a Xiaomi, Redmi, or Poco device
         * running MIUI or HyperOS.
         */
        val isMiuiOrHyperOsDevice: Boolean by lazy {
            try {
                val brand = Build.BRAND.lowercase(Locale.ROOT)
                val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
                val isXiaomiFamily = listOf("xiaomi", "redmi", "poco", "blackshark").any {
                    brand.contains(it) || manufacturer.contains(it)
                }

                val miuiName = getSystemProperty("ro.miui.ui.version.name")
                val miuiCode = getSystemProperty("ro.miui.ui.version.code")
                val hyperOsName = getSystemProperty("ro.mi.os.version.name")
                val hyperOsCode = getSystemProperty("ro.mi.os.version.code")

                val hasMiuiOrHyperOs = miuiName.isNotBlank() ||
                        miuiCode.isNotBlank() ||
                        hyperOsName.isNotBlank() ||
                        hyperOsCode.isNotBlank() ||
                        isMiuiFrameworkPresent()

                isXiaomiFamily && hasMiuiOrHyperOs
            } catch (e: Throwable) {
                false
            }
        }

        private fun getSystemProperty(key: String): String {
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
                getMethod.invoke(null, key, "") as? String ?: ""
            } catch (e: Throwable) {
                try {
                    val clazz = Class.forName("android.os.SystemProperties")
                    val getMethod = clazz.getMethod("get", String::class.java)
                    getMethod.invoke(null, key) as? String ?: ""
                } catch (e2: Throwable) {
                    ""
                }
            }
        }

        private fun isMiuiFrameworkPresent(): Boolean {
            return try {
                Class.forName("miui.os.Build")
                true
            } catch (e: Throwable) {
                try {
                    Class.forName("android.provider.MiuiSettings")
                    true
                } catch (e2: Throwable) {
                    false
                }
            }
        }
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

    val isRootAccessEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ROOT_ACCESS_KEY] ?: false
    }

    suspend fun setRootAccessEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ROOT_ACCESS_KEY] = enabled
        }
    }

    val isMiuixUiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MIUIX_UI_KEY] ?: isMiuiOrHyperOsDevice
    }

    suspend fun setMiuixUiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MIUIX_UI_KEY] = enabled
        }
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
