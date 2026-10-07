package id.xms.xarchiver.ui.explorer.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Phone

data class BreadcrumbSegment(val name: String, val path: String)

fun parseBreadcrumbs(path: String): List<BreadcrumbSegment> {
    if (path.isBlank() || path == "/") return listOf(BreadcrumbSegment("Root", "/"))

    val segments = mutableListOf<BreadcrumbSegment>()
    val clean = path.trimEnd('/')

    if (clean.startsWith("/storage/emulated/0")) {
        segments.add(BreadcrumbSegment("Internal Storage", "/storage/emulated/0"))
        val sub = clean.removePrefix("/storage/emulated/0").trimStart('/')
        if (sub.isNotEmpty()) {
            val parts = sub.split('/')
            var current = "/storage/emulated/0"
            for (p in parts) {
                current += "/$p"
                segments.add(BreadcrumbSegment(p, current))
            }
        }
    } else {
        val parts = clean.split('/').filter { it.isNotEmpty() }
        var current = ""
        for (p in parts) {
            current += "/$p"
            val display = if (current == "/sdcard") "Internal Storage" else p
            segments.add(BreadcrumbSegment(display, current))
        }
    }
    return segments.ifEmpty { listOf(BreadcrumbSegment("Root", "/")) }
}

@Composable
fun MiuixBreadcrumbBar(
    currentPath: String,
    isDark: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val segments = remember(currentPath) { parseBreadcrumbs(currentPath) }

    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            val isCurrent = index == segments.lastIndex

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isCurrent) { onNavigate(segment.path) },
                color = if (isCurrent) primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f) else if (isDark) Color(0xFF1A1A1D) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index == 0) {
                        Icon(
                            imageVector = if (segment.path == "/") Icons.Default.Terminal else MiuixIcons.Phone,
                            contentDescription = null,
                            tint = if (isCurrent) primaryColor else secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = segment.name,
                        color = if (isCurrent) primaryColor else primaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            if (index < segments.lastIndex) {
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    tint = secondaryTextColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp).size(12.dp)
                )
            }
        }
    }
}
