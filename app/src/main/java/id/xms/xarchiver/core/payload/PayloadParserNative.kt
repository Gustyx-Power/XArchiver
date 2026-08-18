package id.xms.xarchiver.core.payload

import java.io.File

/**
 * Native JNI wrapper for Rust payload parser
 */
object PayloadParserNative {
    
    init {
        try {
            System.loadLibrary("payload_parser")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("PayloadParserNative", "Failed to load native library", e)
        }
    }
    
    /**
     * Check if a file is a valid payload.bin
     */
    external fun isPayloadFile(filePath: String): Long
    
    /**
     * Parse payload.bin header
     */
    external fun parseHeader(filePath: String): PayloadHeader?
    
    /**
     * Parse full payload info with partitions
     */
    external fun parsePayloadInfo(filePath: String): PayloadInfo?
}
