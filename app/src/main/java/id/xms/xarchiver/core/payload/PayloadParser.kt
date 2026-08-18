package id.xms.xarchiver.core.payload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Parser for Android OTA payload.bin files
 * Uses manual protobuf parsing (no library dependency)
 */
class PayloadParser {
    
    private val manualParser = PayloadParserManual()
    
    /**
     * Check if a file is a valid payload.bin
     */
    suspend fun isPayloadFile(file: File): Boolean {
        return manualParser.isPayloadFile(file)
    }
    
    /**
     * Parse payload.bin header
     */
    suspend fun parseHeader(file: File): PayloadHeader? {
        return manualParser.parseHeader(file)
    }
    
    /**
     * Parse full payload info with manual protobuf parsing
     */
    suspend fun parsePayloadInfo(file: File): PayloadInfo? {
        return manualParser.parsePayloadInfo(file)
    }
}
