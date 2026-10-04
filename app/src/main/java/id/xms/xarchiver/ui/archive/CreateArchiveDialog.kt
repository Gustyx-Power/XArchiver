package id.xms.xarchiver.ui.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.xms.xarchiver.R
import id.xms.xarchiver.core.archive.ArchiveFormat
import id.xms.xarchiver.core.archive.CompressionLevel
import id.xms.xarchiver.core.archive.EncryptionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArchiveDialog(
    selectedFilesCount: Int,
    onDismiss: () -> Unit,
    onCreate: (String, ArchiveFormat, CompressionLevel, String?, EncryptionType) -> Unit
) {
    val context = LocalContext.current
    var archiveName by remember { mutableStateOf("archive") }
    var selectedFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var selectedCompression by remember { mutableStateOf(CompressionLevel.NORMAL) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedEncryption by remember { mutableStateOf(EncryptionType.AES_256) }
    var error by remember { mutableStateOf<String?>(null) }

    val extension = remember(selectedFormat) { selectedFormat.extension }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.archive_create_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.archive_items_selected_format, selectedFilesCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Archive Name Input
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = {
                        archiveName = it.replace(Regex("[\\\\/:*?\"<>|]"), "")
                        if (error != null) error = null
                    },
                    label = { Text(stringResource(R.string.archive_name_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    suffix = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = ".$extension",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    },
                    isError = error != null,
                    supportingText = error?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Format & Compression Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        // Format Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.archive_format_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box {
                                var expanded by remember { mutableStateOf(false) }

                                val availableFormats = if (selectedFilesCount > 1) {
                                    ArchiveFormat.entries.filter { it != ArchiveFormat.GZ }
                                } else {
                                    ArchiveFormat.entries
                                }

                                LaunchedEffect(selectedFilesCount) {
                                    if (selectedFilesCount > 1 && selectedFormat == ArchiveFormat.GZ) {
                                        selectedFormat = ArchiveFormat.ZIP
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { expanded = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(selectedFormat.displayName, style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    availableFormats.forEach { format ->
                                        DropdownMenuItem(
                                            text = { Text(format.displayName) },
                                            onClick = {
                                                selectedFormat = format
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Compression Row (for ZIP format)
                        if (selectedFormat == ArchiveFormat.ZIP) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Compress,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.archive_compression_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Box {
                                    var expanded by remember { mutableStateOf(false) }
                                    FilledTonalButton(
                                        onClick = { expanded = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(selectedCompression.displayName, style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        CompressionLevel.entries.forEach { level ->
                                            DropdownMenuItem(
                                                text = { Text(level.displayName) },
                                                onClick = {
                                                    selectedCompression = level
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Password & Encryption Card (for ZIP format)
                if (selectedFormat == ArchiveFormat.ZIP) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.archive_password_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text(stringResource(R.string.archive_password_hint)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = password.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.archive_encryption_method_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box {
                                            var expanded by remember { mutableStateOf(false) }
                                            FilledTonalButton(
                                                onClick = { expanded = true },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = when (selectedEncryption) {
                                                        EncryptionType.AES_256 -> "AES-256"
                                                        EncryptionType.ZIP_CRYPTO -> "ZipCrypto"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                EncryptionType.entries.forEach { enc ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                when (enc) {
                                                                    EncryptionType.AES_256 -> stringResource(R.string.archive_encryption_aes256)
                                                                    EncryptionType.ZIP_CRYPTO -> stringResource(R.string.archive_encryption_zipcrypto)
                                                                }
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedEncryption = enc
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = when (selectedEncryption) {
                                            EncryptionType.AES_256 -> stringResource(R.string.archive_encryption_aes256)
                                            EncryptionType.ZIP_CRYPTO -> stringResource(R.string.archive_encryption_zipcrypto)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedEncryption == EncryptionType.AES_256)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        if (archiveName.isBlank()) {
                            error = context.getString(R.string.archive_error_empty_name)
                            return@Button
                        }
                        val effectivePassword = password.trim().ifEmpty { null }
                        onCreate(archiveName, selectedFormat, selectedCompression, effectivePassword, selectedEncryption)
                    },
                    enabled = archiveName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.action_create), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(28.dp)
    )
}
