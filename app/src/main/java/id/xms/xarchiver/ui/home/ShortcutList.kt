package id.xms.xarchiver.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Shortcut
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun ShortcutList(
    shortcuts: List<Shortcut>,
    onShortcutClick: (Shortcut) -> Unit,
    modifier: Modifier.Companion = Modifier
) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Text("Shortcuts", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        shortcuts.forEach { sc ->
            ListItem(
                headlineContent = { Text(sc.name) },
                leadingContent  = { Icon(Icons.Default.Folder, contentDescription = sc.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current
                    ) { onShortcutClick(sc) }
            )
            Divider()
        }
    }
}
