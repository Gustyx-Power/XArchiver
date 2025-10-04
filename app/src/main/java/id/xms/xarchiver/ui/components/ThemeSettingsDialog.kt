package id.xms.xarchiver.ui.components

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

@Composable
fun ThemeSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val scope = rememberCoroutineScope()

    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDynamicColorEnabled by themePreferences.isDynamicColorEnabled.collectAsState(initial = true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Theme Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Theme Mode Section
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Column(Modifier.selectableGroup()) {
                    ThemeOption(
                        title = "Light",
                        description = "Always use light theme",
                        icon = Icons.Default.LightMode,
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = {
                            scope.launch {
                                themePreferences.setThemeMode(ThemeMode.LIGHT)
                            }
                        }
                    )

                    ThemeOption(
                        title = "Dark",
                        description = "Always use dark theme",
                        icon = Icons.Default.DarkMode,
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = {
                            scope.launch {
                                themePreferences.setThemeMode(ThemeMode.DARK)
                            }
                        }
                    )

                    ThemeOption(
                        title = "System",
                        description = "Follow system setting",
                        icon = Icons.Default.SettingsBrightness,
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = {
                            scope.launch {
                                themePreferences.setThemeMode(ThemeMode.SYSTEM)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Material You Section
                Text(
                    "Color Scheme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Material You" else "Dynamic Colors",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        "Use colors from wallpaper"
                                    else
                                        "Not available on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        themePreferences.setDynamicColorEnabled(enabled)
                                    }
                                },
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            )
                        }
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Material You requires Android 12+",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            RadioButton(
                selected = selected,
                onClick = null, // handled by card click
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
