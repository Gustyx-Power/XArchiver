package id.xms.xarchiver.core

data class Category(val name: String, val icon: String, val count: Int)
data class Shortcut(val name: String, val icon: String)
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
    val path: String
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
