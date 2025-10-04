package id.xms.xarchiver.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material You compatible color schemes
private val DynamicLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Teal40,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F7FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFE8F4FD),
    onPrimaryContainer = DeepBlue
)

private val DynamicDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Teal80,
    background = SurfaceDark,
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF333333),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Blue80
)

// Legacy static color schemes
private val StaticLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F7FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFE8F4FD),
    onPrimaryContainer = DeepBlue,
    tertiary = AccentOrange
)

private val StaticDarkColorScheme = darkColorScheme(
    primary = GradientStart,
    secondary = Teal80,
    background = SurfaceDark,
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF333333),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Blue80,
    tertiary = AccentOrange
)

@Composable
fun XArchiverTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = ThemePreferences(context)

    val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDynamicColorEnabled by themePreferences.isDynamicColorEnabled.collectAsState(initial = true)

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        // Use Material You colors if available and enabled
        isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to static custom colors
        darkTheme -> StaticDarkColorScheme
        else -> StaticLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}