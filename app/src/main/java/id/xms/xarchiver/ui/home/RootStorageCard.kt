package id.xms.xarchiver.ui.home

import android.net.Uri
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import id.xms.xarchiver.core.root.*
import kotlinx.coroutines.launch

@Composable
fun RootStorageCard(nav: NavController) {
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(RootService.isGranted()) }
    var busy by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = LocalIndication.current
            ) {
                if (granted) {
                    nav.navigate("root_explorer/${Uri.encode("/")}")
                } else if (!busy) {
                    busy = true
                    scope.launch {
                        granted = RootService.ensureRoot()
                        busy = false
                        if (granted) nav.navigate("root_explorer/${Uri.encode("/")}")
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Root Storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            val sub = when {
                busy     -> "Requesting root..."
                granted  -> "Tap to open /"
                else     -> "Requires root access"
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {
                    if (granted) nav.navigate("root_explorer/${Uri.encode("/")}")
                    else if (!busy) {
                        busy = true
                        scope.launch {
                            granted = RootService.ensureRoot()
                            busy = false
                            if (granted) nav.navigate("root_explorer/${Uri.encode("/")}")
                        }
                    }
                },
                label = { Text(if (granted) "Open" else "Grant root") }
            )
        }
    }
}
