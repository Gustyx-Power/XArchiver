package id.xms.xarchiver.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.StorageInfo
import id.xms.xarchiver.core.humanReadable


@Composable
fun StorageCard(info: StorageInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val percent =
        if (info.total > 0) (info.used.toFloat() / info.total.toFloat()).coerceIn(0f, 1f) else 0f
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = remember { ripple() },
                onClick = onClick
            )
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(info.label, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${info.used.humanReadable()} / ${info.total.humanReadable()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}