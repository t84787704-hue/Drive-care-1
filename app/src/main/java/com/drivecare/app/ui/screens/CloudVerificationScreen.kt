package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecare.app.data.cloud.SyncState
import com.drivecare.app.ui.DriveCareViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudVerificationScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val auditLogs by viewModel.syncManager.auditLogs.collectAsState()
    val lastDownloadCounts by viewModel.syncManager.lastDownloadCounts.collectAsState()
    val lastUploadCounts by viewModel.syncManager.lastUploadCounts.collectAsState()
    val lastDownloadTime by viewModel.syncManager.lastDownloadTime.collectAsState()
    val lastUploadTime by viewModel.syncManager.lastUploadTime.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var isPerformingAction by remember { mutableStateOf(false) }

    fun formatDate(timestamp: Long): String {
        return if (timestamp <= 0L) "Never" else {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.triggerManualSync()
                            Toast.makeText(context, "Sync triggered", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Account Status & Current UID
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Firebase Cloud Identity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(
                                containerColor = when (syncState) {
                                    SyncState.SUCCESS -> Color(0xFF2E7D32)
                                    SyncState.SYNCING -> Color(0xFF1565C0)
                                    SyncState.ERROR -> MaterialTheme.colorScheme.error
                                    SyncState.OFFLINE -> Color(0xFFE65100)
                                    SyncState.IDLE -> MaterialTheme.colorScheme.secondary
                                }
                            ) {
                                Text(
                                    text = syncState.name,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current UID:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currentUser?.uid ?: "NOT SIGNED IN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (currentUser != null) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(currentUser?.uid ?: ""))
                                    Toast.makeText(context, "UID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Email:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currentUser?.email ?: "N/A",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Display Name:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = userProfile?.fullName?.ifBlank { null } ?: currentUser?.displayName ?: "N/A",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Sync Metrics & Last Counts
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Firestore Sync Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Last Overall Sync:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatDate(lastSyncTime), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download / Restore", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("Time: ${formatDate(lastDownloadTime)}", style = MaterialTheme.typography.bodySmall)
                                if (lastDownloadCounts.isNotEmpty()) {
                                    lastDownloadCounts.forEach { (key, count) ->
                                        Text("• $key: $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    Text("No downloads logged yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload / Backup", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("Time: ${formatDate(lastUploadTime)}", style = MaterialTheme.typography.bodySmall)
                                if (lastUploadCounts.isNotEmpty()) {
                                    lastUploadCounts.forEach { (key, count) ->
                                        Text("• $key: $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    Text("No uploads logged yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Card 3: Interactive Developer Test Actions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Developer Test Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isPerformingAction = true
                                    scope.launch {
                                        val res = viewModel.syncManager.downloadAndRestoreData(context, viewModel.db)
                                        isPerformingAction = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Cloud Restore Complete: ${res.getOrNull()} items restored", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Restore Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isPerformingAction && currentUser != null
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Data")
                            }

                            Button(
                                onClick = {
                                    isPerformingAction = true
                                    scope.launch {
                                        viewModel.triggerManualSync()
                                        isPerformingAction = false
                                        Toast.makeText(context, "Upload Triggered", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isPerformingAction && currentUser != null
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup Data")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isPerformingAction = true
                                scope.launch {
                                    viewModel.syncManager.performFullBidirectionalSync(context, viewModel.db)
                                    isPerformingAction = false
                                    Toast.makeText(context, "Full 2-Way Sync Finished", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPerformingAction && currentUser != null
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Full 2-Way Bidirectional Sync")
                        }
                    }
                }
            }

            // Card 4: Audit Log Terminal Output
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Developer Audit Log Terminal (${auditLogs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            viewModel.syncManager.addAuditLog("[CLEAR] Logs reset by developer")
                        }) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        if (auditLogs.isEmpty()) {
                            Text(
                                text = "No audit log entries available yet.",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(auditLogs) { log ->
                                    val logColor = when {
                                        log.contains("SUCCESS") || log.contains("RESTORE SUCCESS") -> Color(0xFF81C784)
                                        log.contains("ERROR") || log.contains("FAIL") -> Color(0xFFE57373)
                                        log.contains("WARN") -> Color(0xFFFFB74D)
                                        log.contains("REALTIME") -> Color(0xFF64B5F6)
                                        else -> Color(0xFFE0E0E0)
                                    }
                                    Text(
                                        text = log,
                                        color = logColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
