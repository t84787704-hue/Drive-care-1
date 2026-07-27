package com.drivecare.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DeletionStage
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.VehicleDeletionSummary
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedVehicleDeleteDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onDeletedSuccessfully: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Analysis, 2: Confirmation, 3: Progress
    var summary by remember { mutableStateOf<VehicleDeletionSummary?>(null) }
    var isLoadingSummary by remember { mutableStateOf(true) }
    var typedVehicleName by remember { mutableStateOf("") }
    val deletionProgress by viewModel.deletionProgress.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(vehicle) {
        isLoadingSummary = true
        summary = viewModel.computeVehicleDeletionSummary(vehicle)
        isLoadingSummary = false
    }

    Dialog(
        onDismissRequest = {
            if (!deletionProgress.isDeleting) {
                viewModel.resetDeletionProgress()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Column {
                            Text(
                                text = when (currentStep) {
                                    1 -> "Delete Analysis Summary"
                                    2 -> "Confirm Vehicle Deletion"
                                    else -> "Executing Cleanup"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step $currentStep of 3 • Safe Vehicle Removal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!deletionProgress.isDeleting) {
                        IconButton(onClick = {
                            viewModel.resetDeletionProgress()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content according to current step
                when (currentStep) {
                    1 -> {
                        // STEP 1: Pre-Deletion Analysis Summary
                        if (isLoadingSummary) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Analyzing linked records & storage...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            summary?.let { sum ->
                                DeleteAnalysisSummaryContent(
                                    vehicle = vehicle,
                                    summary = sum,
                                    onCancel = {
                                        viewModel.resetDeletionProgress()
                                        onDismiss()
                                    },
                                    onProceed = { currentStep = 2 }
                                )
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: Delete Multi-Confirmation & Type Vehicle Name
                        DeleteConfirmationNameContent(
                            vehicle = vehicle,
                            typedName = typedVehicleName,
                            onTypedNameChange = { typedVehicleName = it },
                            onBack = { currentStep = 1 },
                            onConfirmDelete = {
                                currentStep = 3
                                viewModel.deleteVehicleAdvanced(vehicle) { success ->
                                    if (success) {
                                        onDeletedSuccessfully()
                                    }
                                }
                            }
                        )
                    }
                    3 -> {
                        // STEP 3: Progress & Live Logs Execution Screen
                        DeleteProgressExecutionContent(
                            vehicle = vehicle,
                            progress = deletionProgress,
                            onDone = {
                                viewModel.resetDeletionProgress()
                                onDismiss()
                            },
                            onRetry = {
                                viewModel.deleteVehicleAdvanced(vehicle) { success ->
                                    if (success) {
                                        onDeletedSuccessfully()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteAnalysisSummaryContent(
    vehicle: Vehicle,
    summary: VehicleDeletionSummary,
    onCancel: () -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Vehicle Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (vehicle.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = vehicle.imageUri,
                        contentDescription = vehicle.vehicleName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.vehicleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vehicle.brand} ${vehicle.model} (${vehicle.manufacturingYear})".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vehicle.registrationNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Plate: ${vehicle.registrationNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Linked Records to be Permanently Deleted:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Records Grid
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.LocalGasStation,
                    label = "Fuel Records",
                    count = summary.fuelCount
                )
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Build,
                    label = "Maintenance",
                    count = summary.maintenanceCount
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Description,
                    label = "Documents",
                    count = summary.documentsCount
                )
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.AttachMoney,
                    label = "Expenses",
                    count = summary.expensesCount
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Security,
                    label = "Insurance",
                    count = summary.insuranceCount
                )
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Notifications,
                    label = "Reminders",
                    count = summary.remindersCount
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Place,
                    label = "Geofences",
                    count = summary.geofencesCount
                )
                RecordSummaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Gallery Photos",
                    count = summary.galleryCount
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Storage Usage Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = "Storage",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Estimated Storage Usage:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = summary.formattedStorageSize(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onProceed,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Vehicle")
            }
        }
    }
}

@Composable
private fun RecordSummaryChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteConfirmationNameContent(
    vehicle: Vehicle,
    typedName: String,
    onTypedNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val isMatch = remember(typedName, vehicle.vehicleName) {
        typedName.trim().equals(vehicle.vehicleName.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deleting this vehicle will remove all records from Room DB, Firestore cloud, Firebase Storage, and active notification alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Type vehicle name to confirm:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = vehicle.vehicleName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = typedName,
            onValueChange = onTypedNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type '${vehicle.vehicleName}'") },
            singleLine = true,
            isError = typedName.isNotBlank() && !isMatch,
            supportingText = {
                if (typedName.isNotBlank()) {
                    if (isMatch) {
                        Text("✓ Name matched!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    } else {
                        Text("Name does not match vehicle name", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onConfirmDelete,
                enabled = isMatch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            ) {
                Text("Permanently Delete")
            }
        }
    }
}

@Composable
private fun DeleteProgressExecutionContent(
    vehicle: Vehicle,
    progress: com.drivecare.app.ui.DeletionProgressState,
    onDone: () -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(progress.logs.size) {
        if (progress.logs.isNotEmpty()) {
            listState.animateScrollToItem(progress.logs.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (progress.currentStage) {
            DeletionStage.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vehicle Deleted Successfully",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            DeletionStage.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deletion Encountered an Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = progress.statusMessage.ifBlank { "Cleaning up vehicle data..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Stages Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StageIndicatorDot(label = "Room", isDone = progress.currentStage.ordinal > DeletionStage.ROOM_CLEANUP.ordinal, isActive = progress.currentStage == DeletionStage.ROOM_CLEANUP)
            StageIndicatorDot(label = "Firestore", isDone = progress.currentStage.ordinal > DeletionStage.FIRESTORE_CLEANUP.ordinal, isActive = progress.currentStage == DeletionStage.FIRESTORE_CLEANUP)
            StageIndicatorDot(label = "Storage", isDone = progress.currentStage.ordinal > DeletionStage.STORAGE_CLEANUP.ordinal, isActive = progress.currentStage == DeletionStage.STORAGE_CLEANUP)
            StageIndicatorDot(label = "Notifs", isDone = progress.currentStage.ordinal > DeletionStage.NOTIFICATION_CLEANUP.ordinal, isActive = progress.currentStage == DeletionStage.NOTIFICATION_CLEANUP)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Realtime Logs Terminal Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(progress.logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = when {
                            log.contains("[DELETE COMPLETE]") || log.contains("SUCCESS") -> Color(0xFF81C784)
                            log.contains("FAILED") || log.contains("ERROR") -> Color(0xFFE57373)
                            log.contains("[DELETE START]") -> Color(0xFF64B5F6)
                            else -> Color(0xFFE0E0E0)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (progress.currentStage == DeletionStage.SUCCESS) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        } else if (progress.currentStage == DeletionStage.FAILED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry Safely")
                }
            }
        }
    }
}

@Composable
private fun StageIndicatorDot(
    label: String,
    isDone: Boolean,
    isActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> Color(0xFF2E7D32)
                        isActive -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isActive || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
