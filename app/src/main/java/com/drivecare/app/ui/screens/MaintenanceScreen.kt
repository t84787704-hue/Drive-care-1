package com.drivecare.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DocumentFileHelper
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.SavedFileInfo
import com.drivecare.app.utils.VehicleTypeHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MaintenanceSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    COST_DESC("Cost: High to Low"),
    COST_ASC("Cost: Low to High")
}

enum class MaintenanceViewMode {
    LIST,
    TIMELINE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    highlightRecordId: Long? = null
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<Maintenance?>(null) }
    var logToDelete by remember { mutableStateOf<Maintenance?>(null) }
    var previewReceiptUri by remember { mutableStateOf<String?>(null) }

    var selectedFilterVehicleId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf(MaintenanceSortOption.DATE_DESC) }
    var viewMode by remember { mutableStateOf(MaintenanceViewMode.LIST) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = remember {
        listOf("All", "Scheduled Service", "Oil Change", "Brake Inspection", "Tire Service", "Battery Check", "Engine Repair", "AC Service", "General Repair")
    }

    val filteredLogs = remember(maintenanceLogs, selectedFilterVehicleId, searchQuery, selectedCategory, sortOption) {
        var list = maintenanceLogs.filter { log ->
            (selectedFilterVehicleId == null || log.vehicleId == selectedFilterVehicleId) &&
            (selectedCategory == "All" || log.serviceType.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
                log.serviceTitle.contains(searchQuery, ignoreCase = true) ||
                log.workshopName.contains(searchQuery, ignoreCase = true) ||
                log.notes.contains(searchQuery, ignoreCase = true) ||
                log.vehicleName.contains(searchQuery, ignoreCase = true))
        }

        when (sortOption) {
            MaintenanceSortOption.DATE_DESC -> list.sortedByDescending { it.serviceDate }
            MaintenanceSortOption.DATE_ASC -> list.sortedBy { it.serviceDate }
            MaintenanceSortOption.COST_DESC -> list.sortedByDescending { it.serviceCost.toDoubleOrNull() ?: 0.0 }
            MaintenanceSortOption.COST_ASC -> list.sortedBy { it.serviceCost.toDoubleOrNull() ?: 0.0 }
        }
    }

    val totalSpent = remember(filteredLogs) {
        filteredLogs.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
    }

    val avgCost = remember(filteredLogs) {
        if (filteredLogs.isNotEmpty()) totalSpent / filteredLogs.size else 0.0
    }

    val vehicleCostBreakdown = remember(maintenanceLogs, vehicles) {
        vehicles.map { v ->
            val vLogs = maintenanceLogs.filter { it.vehicleId == v.id }
            val sum = vLogs.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
            v to sum
        }
    }

    val advisorSuggestions = remember(vehicles, maintenanceLogs, selectedFilterVehicleId) {
        val targetVehicle = if (selectedFilterVehicleId != null) {
            vehicles.find { it.id == selectedFilterVehicleId }
        } else vehicles.firstOrNull()

        if (targetVehicle != null) {
            viewModel.getMaintenanceAdvisorSuggestions(targetVehicle, maintenanceLogs)
        } else emptyList()
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = AppStrings.get("add_service_log", lang)) },
                    text = { Text(AppStrings.get("add_service_log", lang)) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Filter Chips Bar
            if (vehicles.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilterVehicleId == null,
                                onClick = { selectedFilterVehicleId = null },
                                label = { Text(AppStrings.get("all_vehicles", lang)) }
                            )
                        }
                        items(vehicles, key = { it.id }) { v ->
                            FilterChip(
                                selected = selectedFilterVehicleId == v.id,
                                onClick = { selectedFilterVehicleId = v.id },
                                label = { Text(v.vehicleName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Metric Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "$%.2f", totalSpent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${filteredLogs.size} Services",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Avg. Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "$%.2f", avgCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Vehicle Maintenance Cost Breakdown
            if (vehicles.size > 1 && selectedFilterVehicleId == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Total Maintenance Cost per Vehicle",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(vehicleCostBreakdown) { (veh, costSum) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(veh.vehicleName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                String.format(Locale.US, "$%.2f", costSum),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Full-Width Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search title, workshop, notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // View Mode Toggle & Sort Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Mode Switcher
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { viewMode = MaintenanceViewMode.LIST },
                            selected = viewMode == MaintenanceViewMode.LIST,
                            icon = { Icon(Icons.Default.ViewList, contentDescription = "List View", modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("List", style = MaterialTheme.typography.labelSmall)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { viewMode = MaintenanceViewMode.TIMELINE },
                            selected = viewMode == MaintenanceViewMode.TIMELINE,
                            icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline View", modifier = Modifier.size(16.dp)) }
                        ) {
                            Text("Timeline", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort records")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            MaintenanceSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Smart Maintenance Advisor Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                text = AppStrings.get("advisor_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = AppStrings.get("advisor_subtitle", lang),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (advisorSuggestions.isEmpty()) {
                            Text("All maintenance items are up to date!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        } else {
                            advisorSuggestions.forEach { rec ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(AppStrings.get(rec.titleKey, lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(rec.reason, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Surface(
                                            color = if (rec.urgency == "HIGH") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = rec.urgency,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Service History List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (viewMode == MaintenanceViewMode.TIMELINE) "Service Timeline (${filteredLogs.size})" else AppStrings.get("tab_service", lang) + " (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sortOption.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(AppStrings.get("no_service_logs", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(AppStrings.get("no_service_desc", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (viewMode == MaintenanceViewMode.LIST) {
                itemsIndexed(items = filteredLogs, key = { index, log -> "${log.id}_${index}_${log.createdAt}_${log.serviceTitle}" }) { _, log ->
                    MaintenanceCard(
                        log = log,
                        onEdit = { logToEdit = log },
                        onDelete = { logToDelete = log },
                        onViewReceipt = { uri -> previewReceiptUri = uri }
                    )
                }
            } else {
                // TIMELINE VIEW
                itemsIndexed(items = filteredLogs, key = { index, log -> "${log.id}_${index}_${log.createdAt}_${log.serviceTitle}" }) { _, log ->
                    TimelineMaintenanceCard(
                        log = log,
                        onEdit = { logToEdit = log },
                        onDelete = { logToDelete = log },
                        onViewReceipt = { uri -> previewReceiptUri = uri }
                    )
                }
            }
        }
    }

    if (showAddDialog && vehicles.isNotEmpty()) {
        AddServiceDialog(
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = { log ->
                viewModel.addMaintenance(log)
                showAddDialog = false
                Toast.makeText(context, "Service log saved successfully!", Toast.LENGTH_SHORT).show()
            },
            lang = lang
        )
    }

    logToEdit?.let { log ->
        EditServiceDialog(
            vehicles = vehicles,
            log = log,
            onDismiss = { logToEdit = null },
            onSave = { updated ->
                viewModel.updateMaintenance(updated)
                logToEdit = null
                Toast.makeText(context, "Service log updated!", Toast.LENGTH_SHORT).show()
            },
            lang = lang
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Service Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${log.serviceTitle}' ($${log.serviceCost})? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMaintenance(log)
                        logToDelete = null
                        Toast.makeText(context, "Service record deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (previewReceiptUri != null) {
        ReceiptPreviewDialog(
            receiptUri = previewReceiptUri!!,
            onDismiss = { previewReceiptUri = null }
        )
    }
}

@Composable
fun MaintenanceCard(
    log: Maintenance,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val context = LocalContext.current
    val imageBitmap = remember(log.invoicePhotoUri) {
        if (log.invoicePhotoUri.isNotBlank()) {
            loadLocalBitmap(context, log.invoicePhotoUri)
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(log.serviceTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                log.serviceType,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text("${log.vehicleName} • ${log.serviceDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    "$${log.serviceCost}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Due and Reminder Badges
            if (log.nextDueServiceDate.isNotBlank() || log.reminderDate.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (log.nextDueServiceDate.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Next Due: ${log.nextDueServiceDate}", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    if (log.reminderDate.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Remind: ${log.reminderDate}", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (log.workshopName.isNotBlank()) {
                        Text("Workshop: ${log.workshopName}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (log.currentOdometer.isNotBlank() && log.currentOdometer != "0") {
                        Text("Odometer: ${log.currentOdometer} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Receipt",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                .clickable { onViewReceipt(log.invoicePhotoUri) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (log.invoicePhotoUri.isNotBlank()) {
                        IconButton(onClick = { onViewReceipt(log.invoicePhotoUri) }) {
                            Icon(Icons.Default.Receipt, contentDescription = "View Receipt", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Log", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${log.notes}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineMaintenanceCard(
    log: Maintenance,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline axis
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .heightIn(min = 40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        // Card Content
        Box(modifier = Modifier.weight(1f)) {
            MaintenanceCard(
                log = log,
                onEdit = onEdit,
                onDelete = onDelete,
                onViewReceipt = onViewReceipt
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (Maintenance) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Scheduled Service") }
    var expandedServiceTypeDropdown by remember { mutableStateOf(false) }

    var cost by remember { mutableStateOf("") }
    var workshop by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(todayStr) }
    var odometer by remember { mutableStateOf(selectedVehicle?.odometerReading ?: "0") }
    var nextDueServiceDate by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Invoice photo captured!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val cacheDir = File(context.cacheDir, "receipt_photos").apply { if (!exists()) mkdirs() }
                val tempFile = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required to capture invoice", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "Receipt image attached!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val serviceTypes = listOf("Scheduled Service", "Oil Change", "Brake Inspection", "Tire Service", "Battery Check", "Engine Repair", "AC Service", "General Repair")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_service_log", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.vehicleName} (${VehicleTypeHelper.getDisplayName(it.vehicleType, lang)})" } ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = selectedVehicle?.let { v ->
                            {
                                Icon(
                                    imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text("${v.vehicleName} (${VehicleTypeHelper.getDisplayName(v.vehicleType, lang)} • ${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    odometer = v.odometerReading
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(AppStrings.get("service_title", lang) + " *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Service Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedServiceTypeDropdown,
                    onExpandedChange = { expandedServiceTypeDropdown = !expandedServiceTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = serviceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServiceTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedServiceTypeDropdown,
                        onDismissRequest = { expandedServiceTypeDropdown = false }
                    ) {
                        serviceTypes.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    serviceType = st
                                    if (title.isBlank()) title = st
                                    expandedServiceTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d{0,2})?$"""))) {
                                cost = input
                            }
                        },
                        label = { Text(AppStrings.get("cost", lang) + " ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = odometer,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                odometer = input
                            }
                        },
                        label = { Text("Odometer (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workshop,
                        onValueChange = { workshop = it },
                        label = { Text(AppStrings.get("workshop", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nextDueServiceDate,
                        onValueChange = { nextDueServiceDate = it },
                        label = { Text("Next Due Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reminderDate,
                        onValueChange = { reminderDate = it },
                        label = { Text("Reminder Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Invoice / Receipt Attachment Section
                Text("Invoice / Receipt Photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (attachedFileInfo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Receipt Attached (${DocumentFileHelper.formatFileSize(attachedFileInfo!!.fileSize)})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { attachedFileInfo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove receipt")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        val cacheDir = File(context.cacheDir, "receipt_photos").apply { if (!exists()) mkdirs() }
                                        val tempFile = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                        tempCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle
                    if (v == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "Please enter service title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanCostStr = cost.trim().replace(",", ".")
                    val costDouble = cleanCostStr.toDoubleOrNull()
                    if (cleanCostStr.isBlank() || costDouble == null || costDouble < 0) {
                        Toast.makeText(context, "Please enter valid service cost", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val log = Maintenance(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        serviceTitle = cleanTitle,
                        serviceType = serviceType,
                        serviceDate = date.ifBlank { todayStr },
                        currentOdometer = odometer.ifBlank { v.odometerReading },
                        serviceCost = cleanCostStr,
                        workshopName = workshop.trim(),
                        notes = notes.trim(),
                        invoicePhotoUri = attachedFileInfo?.fileUriString ?: "",
                        nextDueServiceDate = nextDueServiceDate.trim(),
                        reminderDate = reminderDate.trim()
                    )
                    onSave(log)
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceDialog(
    vehicles: List<Vehicle>,
    log: Maintenance,
    onDismiss: () -> Unit,
    onSave: (Maintenance) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current

    var selectedVehicle by remember { mutableStateOf(vehicles.find { it.id == log.vehicleId } ?: vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(log.serviceTitle) }
    var serviceType by remember { mutableStateOf(log.serviceType) }
    var expandedServiceTypeDropdown by remember { mutableStateOf(false) }

    var cost by remember { mutableStateOf(log.serviceCost) }
    var workshop by remember { mutableStateOf(log.workshopName) }
    var date by remember { mutableStateOf(log.serviceDate) }
    var odometer by remember { mutableStateOf(log.currentOdometer) }
    var nextDueServiceDate by remember { mutableStateOf(log.nextDueServiceDate) }
    var reminderDate by remember { mutableStateOf(log.reminderDate) }
    var notes by remember { mutableStateOf(log.notes) }
    var invoicePhotoUri by remember { mutableStateOf(log.invoicePhotoUri) }

    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                invoicePhotoUri = saved.fileUriString
                Toast.makeText(context, "Invoice photo captured!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                invoicePhotoUri = saved.fileUriString
                Toast.makeText(context, "Receipt image attached!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val serviceTypes = listOf("Scheduled Service", "Oil Change", "Brake Inspection", "Tire Service", "Battery Check", "Engine Repair", "AC Service", "General Repair")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Service Log", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.vehicleName} (${VehicleTypeHelper.getDisplayName(it.vehicleType, lang)})" } ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = selectedVehicle?.let { v ->
                            {
                                Icon(
                                    imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(v.vehicleType),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text("${v.vehicleName} (${VehicleTypeHelper.getDisplayName(v.vehicleType, lang)} • ${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedServiceTypeDropdown,
                    onExpandedChange = { expandedServiceTypeDropdown = !expandedServiceTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = serviceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServiceTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedServiceTypeDropdown,
                        onDismissRequest = { expandedServiceTypeDropdown = false }
                    ) {
                        serviceTypes.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    serviceType = st
                                    expandedServiceTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d{0,2})?$"""))) {
                                cost = input
                            }
                        },
                        label = { Text("Cost ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = odometer,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                odometer = input
                            }
                        },
                        label = { Text("Odometer (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workshop,
                        onValueChange = { workshop = it },
                        label = { Text("Workshop") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nextDueServiceDate,
                        onValueChange = { nextDueServiceDate = it },
                        label = { Text("Next Due Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reminderDate,
                        onValueChange = { reminderDate = it },
                        label = { Text("Reminder Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Invoice photo
                Text("Invoice / Receipt Photo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (invoicePhotoUri.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Receipt Attached", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { invoicePhotoUri = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove receipt")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val cacheDir = File(context.cacheDir, "receipt_photos").apply { if (!exists()) mkdirs() }
                                    val tempFile = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle
                    if (v == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "Please enter service title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanCostStr = cost.trim().replace(",", ".")
                    val costDouble = cleanCostStr.toDoubleOrNull()
                    if (cleanCostStr.isBlank() || costDouble == null || costDouble < 0) {
                        Toast.makeText(context, "Please enter valid service cost", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updatedLog = log.copy(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        serviceTitle = cleanTitle,
                        serviceType = serviceType,
                        serviceDate = date.ifBlank { log.serviceDate },
                        currentOdometer = odometer,
                        serviceCost = cleanCostStr,
                        workshopName = workshop.trim(),
                        notes = notes.trim(),
                        invoicePhotoUri = invoicePhotoUri,
                        nextDueServiceDate = nextDueServiceDate.trim(),
                        reminderDate = reminderDate.trim()
                    )
                    onSave(updatedLog)
                }
            ) {
                Text("Update Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun ReceiptPreviewDialog(
    receiptUri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageBitmap = remember(receiptUri) {
        if (receiptUri.isNotBlank()) loadLocalBitmap(context, receiptUri) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Service Invoice / Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Invoice Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Unable to load receipt image", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun loadLocalBitmap(context: Context, uriString: String): ImageBitmap? {
    if (uriString.isBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists() && file.isFile) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } else {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap?.asImageBitmap()
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}
