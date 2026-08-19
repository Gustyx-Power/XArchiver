package id.xms.xarchiver.core

data class Category(
    val name: String,
    val icon: String,
    val count: Int,
    val mimeTypes: List<String> = emptyList()
)

data class Shortcut(
    val name: String,
    val icon: String,
    val path: String
)
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

data class StorageInfo(
    val used: Long,
    val total: Long,
    val label: String,
    val path: String,
    val fsType: String = "" // e.g. ext4, exFAT, f2fs
)


fun Long.humanReadable(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$this B"
    }
}

fun Long.commercialStorageSize(): String {
    val gib = this / (1024.0 * 1024.0 * 1024.0)
    var size = 8
    while (size <= 2048) {
        if (gib <= size * 1.05) {
            return if (size >= 1024) "${size / 1024} TB" else "$size GB"
        }
        size *= 2
    }
    return this.humanReadable()
}
