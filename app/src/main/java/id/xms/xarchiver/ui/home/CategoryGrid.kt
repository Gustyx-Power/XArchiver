package id.xms.xarchiver.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.core.Category

@Composable
fun CategoryGrid(categories: List<Category>) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            categories.take(3).forEach { CategoryItem(it) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            categories.drop(3).take(3).forEach { CategoryItem(it) }
        }
    }
}

@Composable
fun CategoryItem(category: Category) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.FolderZip, contentDescription = category.name,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(category.name, style = MaterialTheme.typography.bodySmall)
        Text("${category.count}", style = MaterialTheme.typography.labelSmall)
    }
}
