package id.xms.xarchiver.ui.payload

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.xms.xarchiver.core.payload.ExtractionState
import id.xms.xarchiver.core.payload.PayloadExtractionProgress
import id.xms.xarchiver.core.payload.PayloadExtractor
import id.xms.xarchiver.core.payload.PayloadInfo
import id.xms.xarchiver.core.payload.PayloadParser
import id.xms.xarchiver.core.payload.PayloadPartition
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class PayloadViewModel(private val context: Context) : ViewModel() {
    
    private val parser = PayloadParser()
    private val extractor = PayloadExtractor()
    
    var payloadInfo by mutableStateOf<PayloadInfo?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set
    
    var extractionProgress by mutableStateOf<PayloadExtractionProgress?>(null)
        private set
    
    var isExtracting by mutableStateOf(false)
        private set
    
    private var currentPayloadPath: String? = null
    private var extractionJob: Job? = null
    
    fun loadPayload(payloadPath: String) {
        currentPayloadPath = payloadPath
        viewModelScope.launch {
            isLoading = true
            error = null
            
            try {
                val file = File(payloadPath)
                
                if (!file.exists()) {
                    error = "File not found"
                    return@launch
                }
                
                if (!parser.isPayloadFile(file)) {
                    error = "Not a valid payload.bin file"
                    return@launch
                }
                
                val info = parser.parsePayloadInfo(file)
                if (info == null) {
                    error = "Failed to parse payload file. Check logcat for details."
                    return@launch
                }
                
                // Debug: Log partition count
                android.util.Log.d("PayloadViewModel", "Parsed ${info.partitions.size} partitions")
                info.partitions.forEach { partition ->
                    android.util.Log.d("PayloadViewModel", "Partition: ${partition.name}, size: ${partition.uncompressedSize}")
                }
                
                payloadInfo = info
            } catch (e: Exception) {
                android.util.Log.e("PayloadViewModel", "Error loading payload", e)
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun refreshPayload() {
        currentPayloadPath?.let { loadPayload(it) }
    }
    
    fun extractPartition(partition: PayloadPartition, outputPath: String? = null) {
        val info = payloadInfo ?: return
        
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            isExtracting = true
            extractionProgress = null
            
            try {
                val payloadFile = File(info.filePath)
                
                // Determine output path
                val outputFile = if (outputPath != null) {
                    File(outputPath)
                } else {
                    // Default to Downloads/XArchiver/extracted/
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val extractDir = File(downloadsDir, "XArchiver/extracted")
                    extractDir.mkdirs()
                    File(extractDir, "${partition.name}.img")
                }
                
                // Extract partition
                extractor.extractPartition(
                    payloadFile = payloadFile,
                    partition = partition,
                    outputFile = outputFile,
                    blockSize = info.header.blockSize
                ).collect { progress ->
                    extractionProgress = progress
                }
                
            } catch (e: Exception) {
                error = "Extraction failed: ${e.message}"
                extractionProgress = PayloadExtractionProgress(
                    partitionName = partition.name,
                    bytesExtracted = 0,
                    totalBytes = partition.uncompressedSize,
                    percentage = 0,
                    state = ExtractionState.ERROR
                )
            } finally {
                isExtracting = false
            }
        }
    }
    
    fun extractAllPartitions(outputPath: String? = null) {
        val info = payloadInfo ?: return
        
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            isExtracting = true
            extractionProgress = null
            
            try {
                val payloadFile = File(info.filePath)
                
                // Determine output directory
                val outputDir = if (outputPath != null) {
                    File(outputPath)
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val extractDir = File(downloadsDir, "XArchiver/extracted")
                    extractDir.mkdirs()
                    extractDir
                }
                
                // Extract all partitions
                extractor.extractPartitions(
                    payloadFile = payloadFile,
                    partitions = info.partitions,
                    outputDirectory = outputDir,
                    blockSize = info.header.blockSize
                ).collect { progress ->
                    extractionProgress = progress
                }
                
            } catch (e: Exception) {
                error = "Extraction failed: ${e.message}"
            } finally {
                isExtracting = false
            }
        }
    }
    
    fun cancelExtraction() {
        extractionJob?.cancel()
        extractionJob = null
        isExtracting = false
        extractionProgress = null
    }
}

class PayloadViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PayloadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PayloadViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
