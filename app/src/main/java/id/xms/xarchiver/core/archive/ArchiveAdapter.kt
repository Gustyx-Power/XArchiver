package id.xms.xarchiver.core.archive

import java.io.InputStream

interface ArchiveAdapter {
    suspend fun listEntries(input: InputStream): List<ArchiveEntry>
    suspend fun extractEntry(input: InputStream, entryName: String, output: java.io.OutputStream)
}
