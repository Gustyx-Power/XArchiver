package id.xms.xarchiver.ui.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Category
import id.xms.xarchiver.ui.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.take(3).forEach { category ->
                CategoryItem(
                    category = category,
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.drop(3).take(3).forEach { category ->
                CategoryItem(
                    category = category,
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, gradientColors) = getCategoryIconAndColor(category)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .size(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) { onClick(category) },
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(gradientColors),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = category.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Text(
            "${category.count} files",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun getCategoryIconAndColor(category: Category): Pair<ImageVector, List<Color>> {
    return when (category.name.lowercase()) {
        "images" -> Icons.Default.Image to listOf(ImageColor, ImageColor.copy(alpha = 0.8f))
        "videos" -> Icons.Default.VideoLibrary to listOf(VideoColor, VideoColor.copy(alpha = 0.8f))
        "audio" -> Icons.Default.AudioFile to listOf(AudioColor, AudioColor.copy(alpha = 0.8f))
        "documents" -> Icons.Default.Description to listOf(DocumentColor, DocumentColor.copy(alpha = 0.8f))
        "archives" -> Icons.Default.FolderZip to listOf(ZipColor, ZipColor.copy(alpha = 0.8f))
        "apps" -> Icons.Default.Apps to listOf(AccentOrange, AccentOrange.copy(alpha = 0.8f))
        else -> Icons.Default.Folder to listOf(GradientStart, GradientEnd)
    }
}
