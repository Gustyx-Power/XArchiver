package id.xms.xarchiver.core.extract

import id.xms.xarchiver.core.archive.ArchiveAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File
import java.io.InputStream

class ExtractionHelper(private val adapter: ArchiveAdapter) {

    suspend fun extractAll(input: InputStream, outputDir: File) = withContext(Dispatchers.IO) {
        adapter.extractAll(input, outputDir)
    }
}
