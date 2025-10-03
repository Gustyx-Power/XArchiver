package id.xms.xarchiver.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import id.xms.xarchiver.core.root.RootService
import kotlinx.coroutines.launch

@Composable
fun RootStorageCard(
    onOpenRoot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(RootService.isGranted()) }
    var busy by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) {
                if (granted) onOpenRoot("/")
                else if (!busy) {
                    busy = true
                    scope.launch {
                        granted = RootService.ensureRoot()
                        busy = false
                        if (granted) onOpenRoot("/")
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Root storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    busy    -> "Requesting root..."
                    granted -> "Tap to open /"
                    else    -> "Requires root access"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {
                    if (granted) onOpenRoot("/") else if (!busy) {
                        busy = true
                        scope.launch {
                            granted = RootService.ensureRoot()
                            busy = false
                            if (granted) onOpenRoot("/")
                        }
                    }
                },
                label = { Text(if (granted) "Open" else "Grant root") }
            )
        }
    }
}
