package id.xms.xarchiver.ui.payload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.core.payload.PayloadPartition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadViewerScreen(
    payloadPath: String,
    navController: NavController,
    viewModel: PayloadViewModel = viewModel(
        factory = PayloadViewModelFactory(LocalContext.current)
    )
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showExtractDialog by remember { mutableStateOf(false) }
    var selectedPartition by remember { mutableStateOf<PayloadPartition?>(null) }
    var showExtractAllDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(payloadPath) {
        viewModel.loadPayload(payloadPath)
    }
    
    // Show snackbar when extraction completes
    LaunchedEffect(viewModel.extractionProgress) {
        viewModel.extractionProgress?.let { progress ->
            if (progress.state == id.xms.xarchiver.core.payload.ExtractionState.COMPLETED) {
                snackbarHostState.showSnackbar(
                    message = "Extraction completed: ${progress.partitionName}",
                    duration = SnackbarDuration.Short
                )
            } else if (progress.state == id.xms.xarchiver.core.payload.ExtractionState.ERROR) {
                val errorMsg = progress.errorMessage ?: "Unknown error"
                snackbarHostState.showSnackbar(
                    message = "Extraction failed: ${progress.partitionName}\n$errorMsg",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payload Viewer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (viewModel.payloadInfo != null && viewModel.payloadInfo!!.partitions.isNotEmpty()) {
                        IconButton(
                            onClick = { showExtractAllDialog = true },
                            enabled = !viewModel.isExtracting
                        ) {
                            Icon(Icons.Default.Download, "Extract All")
                        }
                    }
                    IconButton(onClick = { viewModel.refreshPayload() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                viewModel.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                viewModel.payloadInfo != null -> {
                    PayloadContent(
                        payloadInfo = viewModel.payloadInfo!!,
                        extractionProgress = viewModel.extractionProgress,
                        isExtracting = viewModel.isExtracting,
                        onExtractPartition = { partition ->
                            selectedPartition = partition
                            showExtractDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Extract single partition dialog
    if (showExtractDialog && selectedPartition != null) {
        val parentFolderName = viewModel.payloadInfo?.filePath?.let { java.io.File(it).parentFile?.name } ?: "UnknownROM"
        AlertDialog(
            onDismissRequest = { 
                if (!viewModel.isExtracting) {
                    showExtractDialog = false
                }
            },
            title = { Text("Extract Partition") },
            text = {
                Column {
                    Text("Extract ${selectedPartition!!.name}.img?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Size: ${selectedPartition!!.uncompressedSize.humanReadable()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Output: XArchiver-payload/extracted-payload/$parentFolderName/",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.extractPartition(selectedPartition!!)
                        showExtractDialog = false
                    },
                    enabled = !viewModel.isExtracting
                ) {
                    Text("Extract")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExtractDialog = false },
                    enabled = !viewModel.isExtracting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Extract all partitions dialog
    if (showExtractAllDialog) {
        val parentFolderName = viewModel.payloadInfo?.filePath?.let { java.io.File(it).parentFile?.name } ?: "UnknownROM"
        AlertDialog(
            onDismissRequest = { 
                if (!viewModel.isExtracting) {
                    showExtractAllDialog = false
                }
            },
            title = { Text("Extract All Partitions") },
            text = {
                Column {
                    Text("Extract all ${viewModel.payloadInfo?.partitions?.size ?: 0} partitions?")
                    Spacer(modifier = Modifier.height(8.dp))
                    val totalSize = viewModel.payloadInfo?.partitions?.sumOf { it.uncompressedSize } ?: 0
                    Text(
                        text = "Total size: ${totalSize.humanReadable()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Output: XArchiver-payload/extracted-payload/$parentFolderName/",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.extractAllPartitions()
                        showExtractAllDialog = false
                    },
                    enabled = !viewModel.isExtracting
                ) {
                    Text("Extract All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExtractAllDialog = false },
                    enabled = !viewModel.isExtracting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PayloadContent(
    payloadInfo: id.xms.xarchiver.core.payload.PayloadInfo,
    extractionProgress: id.xms.xarchiver.core.payload.PayloadExtractionProgress?,
    isExtracting: Boolean,
    onExtractPartition: (PayloadPartition) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header info
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payload Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Version", payloadInfo.header.version.toString())
                    InfoRow("Total Size", payloadInfo.totalSize.humanReadable())
                    InfoRow("Partitions", payloadInfo.partitions.size.toString())
                    InfoRow("Block Size", "${payloadInfo.header.blockSize} bytes")
                }
            }
        }
        
        // Extraction progress
        if (extractionProgress != null && isExtracting) {
            item {
                ExtractionProgressCard(extractionProgress)
            }
        }
        
        // Partitions list
        if (payloadInfo.partitions.isNotEmpty()) {
            item {
                Text(
                    text = "Partitions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(payloadInfo.partitions) { partition ->
                PartitionCard(
                    partition = partition,
                    isExtracting = isExtracting && extractionProgress?.partitionName == partition.name,
                    onExtract = { onExtractPartition(partition) }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No partitions found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExtractionProgressCard(progress: id.xms.xarchiver.core.payload.PayloadExtractionProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracting: ${progress.partitionName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${progress.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress.percentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progress.state.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${progress.bytesExtracted.humanReadable()} / ${progress.totalBytes.humanReadable()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PartitionCard(
    partition: PayloadPartition,
    isExtracting: Boolean = false,
    onExtract: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partition.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Compressed: ${partition.compressedSize.humanReadable()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Uncompressed: ${partition.uncompressedSize.humanReadable()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Type: ${partition.compressionType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onExtract) {
                    Icon(Icons.Default.Download, "Extract")
                }
            }
        }
    }
}
