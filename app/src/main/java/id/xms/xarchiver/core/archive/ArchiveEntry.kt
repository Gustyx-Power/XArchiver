package id.xms.xarchiver.core.archive

data class ArchiveEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean
)
