package id.xms.xarchiver.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Shortcut

@Composable
fun ShortcutList(shortcuts: List<Shortcut>) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Text("Shortcuts", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        shortcuts.forEach { shortcut ->
            ListItem(
                headlineContent = { Text(shortcut.name) },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = shortcut.name) }
            )
            Divider()
        }
    }
}
