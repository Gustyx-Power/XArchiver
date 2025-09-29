package id.xms.xarchiver.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.StorageInfo
import id.xms.xarchiver.core.humanReadable

@Composable
fun StorageCard(info: StorageInfo, onClick: () -> Unit) {
    val percent = if (info.total > 0) (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(info.label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${info.used.humanReadable()} / ${info.total.humanReadable()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
