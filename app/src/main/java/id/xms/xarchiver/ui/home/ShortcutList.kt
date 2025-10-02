// ShortcutList.kt (modern + clickable)
package id.xms.xarchiver.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Shortcut

@Composable
fun ShortcutList(
    shortcuts: List<Shortcut>,
    onShortcutClick: (Shortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Text("Shortcuts", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        shortcuts.forEachIndexed { index, shortcut ->
            ListItem(
                headlineContent = { Text(shortcut.name) },
                supportingContent = {
                    // Tampilkan informasi tambahan bila ada (mis. path)
                    ///shortcut.path?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                },
                leadingContent = {
                    Icon(Icons.Default.Folder, contentDescription = shortcut.name)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier.clickable { onShortcutClick(shortcut) }
            )
            if (index != shortcuts.lastIndex) {
                Divider()
            }
        }
    }
}
