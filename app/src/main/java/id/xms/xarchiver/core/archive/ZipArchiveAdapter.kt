package id.xms.xarchiver.core.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipArchiveAdapter : ArchiveAdapter {

    override suspend fun listEntries(input: InputStream): List<ArchiveEntry> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<ArchiveEntry>()
            ZipInputStream(input).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    result.add(
                        ArchiveEntry(
                            name = entry.name,
                            size = entry.size,
                            isDirectory = entry.isDirectory
                        )
                    )
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            result
        }

    override suspend fun extractEntry(
        input: InputStream,
        entryName: String,
        output: OutputStream
    ) = withContext(Dispatchers.IO) {
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == entryName && !entry.isDirectory) {
                    val buffer = ByteArray(8 * 1024)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        output.write(buffer, 0, len)
                    }
                    output.flush()
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    override suspend fun extractAll(input: InputStream, outputDir: java.io.File) = withContext(Dispatchers.IO) {
        ZipInputStream(input).use { zis ->
            var entry: java.util.zip.ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    java.io.File(outputDir, entry.name).mkdirs()
                } else {
                    val outFile = java.io.File(outputDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out ->
                        val buffer = ByteArray(8 * 1024)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            out.write(buffer, 0, len)
                        }
                        out.flush()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
