package id.xms.xarchiver.ui.payload

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.xms.xarchiver.core.payload.PayloadInfo
import id.xms.xarchiver.core.payload.PayloadParser
import id.xms.xarchiver.core.payload.PayloadPartition
import kotlinx.coroutines.launch
import java.io.File

class PayloadViewModel(private val context: Context) : ViewModel() {
    
    private val parser = PayloadParser()
    
    var payloadInfo by mutableStateOf<PayloadInfo?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set
    
    private var currentPayloadPath: String? = null
    
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
                
                val info = parser.parseBasicInfo(file)
                if (info == null) {
                    error = "Failed to parse payload file"
                    return@launch
                }
                
                payloadInfo = info
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun refreshPayload() {
        currentPayloadPath?.let { loadPayload(it) }
    }
    
    fun extractPartition(partition: PayloadPartition) {
        // TODO: Implement extraction
        // This will be implemented in Phase 3
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
