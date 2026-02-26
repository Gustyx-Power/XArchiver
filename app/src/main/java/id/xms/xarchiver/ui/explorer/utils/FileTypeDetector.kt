package id.xms.xarchiver.ui.explorer.utils

object FileTypeDetector {
    
    fun isTextExtension(ext: String): Boolean {
        return ext in listOf(
            "txt", "md", "log", "json", "xml", "html", "htm", "css", "js", "ts",
            "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs", "go", "rs",
            "php", "rb", "swift", "sh", "bat", "ps1", "yaml", "yml", "toml", "ini",
            "cfg", "conf", "properties", "gradle", "pro", "gitignore", "env"
        )
    }

    fun isArchiveExtension(fileName: String): Boolean {
        val lowerName = fileName.lowercase()
        // Check for compound extensions first
        if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar.bz2") ||
            lowerName.endsWith(".tar.xz") || lowerName.endsWith(".tar.lz")) {
            return true
        }
        
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lz", "tgz", "tbz2", "txz"
        )
    }

    fun isImageExtension(ext: String): Boolean {
        return ext in listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "heic", "heif"
        )
    }

    fun isAudioExtension(ext: String): Boolean {
        return ext in listOf(
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "wma", "ape"
        )
    }

    fun isVideoExtension(ext: String): Boolean {
        return ext in listOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp"
        )
    }

    fun isDocumentExtension(ext: String): Boolean {
        return ext in listOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp"
        )
    }

    fun isBinaryExtension(ext: String): Boolean {
        return ext in listOf(
            "bin", "so", "apk", "dex", "img", "dat", "exe", "dll"
        )
    }

    fun isPayloadBin(fileName: String): Boolean {
        return fileName.equals("payload.bin", ignoreCase = true)
    }
}
