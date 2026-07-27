package com.drivecare.app.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.drivecare.app.NavTab
import com.drivecare.app.data.model.*
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.FuelEfficiencyStats
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DocumentFileHelper
import com.drivecare.app.utils.PdfReportGenerator
import com.drivecare.app.utils.VehicleTypeHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToSection: ((NavTab, MoreSubSection?, Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val currencySymbol by viewModel.currentCurrencySymbol.collectAsState()

    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val insurancePolicies by viewModel.insurancePolicies.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val geofences by viewModel.geofenceZones.collectAsState()

    // Filter strictly for this selected vehicle
    val vFuel = remember(fuelEntries, vehicle.id) { fuelEntries.filter { it.vehicleId == vehicle.id } }
    val vService = remember(maintenanceLogs, vehicle.id) { maintenanceLogs.filter { it.vehicleId == vehicle.id } }
    val vInsurance = remember(insurancePolicies, vehicle.id) { insurancePolicies.filter { it.vehicleId == vehicle.id } }
    val vDocs = remember(documents, vehicle.id) { documents.filter { it.vehicleId == vehicle.id } }
    val vReminders = remember(reminders, vehicle.id) { reminders.filter { it.vehicleId == vehicle.id } }
    val vExpenses = remember(expenses, vehicle.id) { expenses.filter { it.vehicleId == vehicle.id } }
    val vGeofences = remember(geofences, vehicle.id) { geofences.filter { it.vehicleId == vehicle.id } }

    val health = viewModel.calculateHealthScore(vehicle, vReminders, vFuel, vService, vDocs)
    val fuelStats = viewModel.calculateVehicleFuelEfficiency(vehicle, vFuel)

    val totalFuelSpent = remember(vFuel) { vFuel.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 } }
    val totalServiceSpent = remember(vService) { vService.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 } }
    val totalInsuranceSpent = remember(vInsurance) { vInsurance.sumOf { it.premiumAmount } }
    val totalCustomExpenses = remember(vExpenses) { vExpenses.sumOf { it.amount } }
    val grandTotalExpenses = totalFuelSpent + totalServiceSpent + totalInsuranceSpent + totalCustomExpenses

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        AppStrings.get("tab_overview", lang).ifBlank { "Overview" },
        AppStrings.get("tab_fuel", lang).ifBlank { "Fuel" },
        AppStrings.get("tab_services", lang).ifBlank { "Maintenance" },
        AppStrings.get("tab_documents", lang).ifBlank { "Documents" },
        AppStrings.get("expense_manager_title", lang).ifBlank { "Expenses" },
        AppStrings.get("insurance_policies_title", lang).ifBlank { "Insurance" },
        AppStrings.get("tab_reminders", lang).ifBlank { "Reminders" },
        "Geofencing",
        "Gallery"
    )

    // Dialog state handlers
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddFuelDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddInsuranceDialog by remember { mutableStateOf(false) }
    var editingInsurancePolicy by remember { mutableStateOf<InsurancePolicy?>(null) }
    var renewingInsurancePolicy by remember { mutableStateOf<InsurancePolicy?>(null) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showAddGeofenceDialog by remember { mutableStateOf(false) }
    var showAddGalleryPhotoDialog by remember { mutableStateOf(false) }

    val formattedLastUpdated = remember(vehicle.lastUpdated) {
        if (vehicle.lastUpdated > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(vehicle.lastUpdated))
        } else {
            "N/A"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = vehicle.vehicleName.ifBlank { "Vehicle Details" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}".trim().ifBlank { VehicleTypeHelper.getDisplayName(vehicle.vehicleType, lang) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = AppStrings.get("back", lang).ifBlank { "Back" })
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val file = PdfReportGenerator.generateAndShareReport(context, vehicle, health, vFuel, vService)
                        if (file != null) {
                            Toast.makeText(context, AppStrings.get("pdf_generated", lang).ifBlank { "PDF Report Generated" }, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit_vehicle", lang).ifBlank { "Edit Vehicle" })
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = AppStrings.get("delete_vehicle", lang).ifBlank { "Delete Vehicle" }, tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            val (fabLabel, fabIcon) = when (selectedTabIndex) {
                0 -> "Edit Vehicle" to Icons.Default.Edit
                1 -> "Add Fuel" to Icons.Default.LocalGasStation
                2 -> "Add Service" to Icons.Default.Build
                3 -> "Add Document" to Icons.Default.FolderOpen
                4 -> "Add Expense" to Icons.Default.AttachMoney
                5 -> "Add Insurance" to Icons.Default.Security
                6 -> "Add Reminder" to Icons.Default.NotificationsActive
                7 -> "Add Geofence" to Icons.Default.LocationOn
                8 -> "Add Photo" to Icons.Default.AddAPhoto
                else -> "Add Record" to Icons.Default.Add
            }

            ExtendedFloatingActionButton(
                text = { Text(fabLabel) },
                icon = { Icon(fabIcon, contentDescription = fabLabel) },
                onClick = {
                    when (selectedTabIndex) {
                        0 -> showEditDialog = true
                        1 -> showAddFuelDialog = true
                        2 -> showAddServiceDialog = true
                        3 -> showAddDocDialog = true
                        4 -> showAddExpenseDialog = true
                        5 -> showAddInsuranceDialog = true
                        6 -> showAddReminderDialog = true
                        7 -> showAddGeofenceDialog = true
                        8 -> showAddGalleryPhotoDialog = true
                        else -> showEditDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Banner Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        if (vehicle.imageUri.isNotBlank()) {
                            AsyncImage(
                                model = vehicle.imageUri,
                                contentDescription = vehicle.vehicleName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = VehicleTypeHelper.getVehicleIcon(vehicle.vehicleType),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = vehicle.vehicleName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${VehicleTypeHelper.getDisplayName(vehicle.vehicleType, lang)} • ${vehicle.brand} ${vehicle.model} (${vehicle.manufacturingYear})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "Plate: ${vehicle.registrationNumber.ifBlank { "N/A" }}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (health >= 80) MaterialTheme.colorScheme.primaryContainer else if (health >= 50) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Health: $health%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> OverviewTabContent(
                        vehicle = vehicle,
                        formattedLastUpdated = formattedLastUpdated,
                        vFuel = vFuel,
                        vService = vService,
                        vDocs = vDocs,
                        vExpenses = vExpenses,
                        vInsurance = vInsurance,
                        vReminders = vReminders,
                        vGeofences = vGeofences,
                        fuelStats = fuelStats,
                        totalFuelSpent = totalFuelSpent,
                        totalServiceSpent = totalServiceSpent,
                        totalInsuranceSpent = totalInsuranceSpent,
                        totalCustomExpenses = totalCustomExpenses,
                        grandTotalExpenses = grandTotalExpenses,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onSwitchTab = { selectedTabIndex = it }
                    )
                    1 -> FuelTabContent(
                        vFuel = vFuel,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onDeleteFuel = { viewModel.deleteFuelEntry(it) }
                    )
                    2 -> MaintenanceTabContent(
                        vService = vService,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onDeleteService = { viewModel.deleteMaintenance(it) }
                    )
                    3 -> DocumentsTabContent(
                        vDocs = vDocs,
                        lang = lang,
                        onDeleteDoc = { viewModel.deleteDocument(it) }
                    )
                    4 -> ExpensesTabContent(
                        vExpenses = vExpenses,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                    5 -> InsuranceTabContent(
                        vInsurance = vInsurance,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onRenewPolicy = { renewingInsurancePolicy = it },
                        onEditPolicy = { editingInsurancePolicy = it },
                        onDeletePolicy = { viewModel.deleteInsurancePolicy(it) }
                    )
                    6 -> RemindersTabContent(
                        vReminders = vReminders,
                        lang = lang,
                        onToggleReminder = { viewModel.toggleReminder(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) }
                    )
                    7 -> GeofencingTabContent(
                        vGeofences = vGeofences,
                        lang = lang,
                        onToggleActive = { zone ->
                            viewModel.updateGeofenceZone(zone.copy(isActive = !zone.isActive))
                        },
                        onDeleteGeofence = { viewModel.deleteGeofenceZone(it) }
                    )
                    8 -> GalleryTabContent(
                        vehicle = vehicle,
                        vDocs = vDocs,
                        viewModel = viewModel,
                        lang = lang,
                        onAddPhotoClick = { showAddGalleryPhotoDialog = true }
                    )
                }
            }
        }
    }

    // Modal Dialogs for Adding/Editing
    if (showEditDialog) {
        VehicleFormDialog(
            title = AppStrings.get("edit_vehicle", lang).ifBlank { "Edit Vehicle" },
            vehicle = vehicle,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateVehicle(updated)
                showEditDialog = false
            },
            lang = lang
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(AppStrings.get("delete_vehicle", lang).ifBlank { "Delete Vehicle" }) },
            text = {
                Text("Are you sure you want to delete ${vehicle.vehicleName}? This will permanently remove the vehicle and ALL associated fuel logs, maintenance records, documents, expenses, insurance policies, reminders, and geofences from both local storage and cloud sync.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteVehicle(vehicle)
                        showDeleteConfirmDialog = false
                        onBackClick()
                    }
                ) {
                    Text(AppStrings.get("delete_vehicle", lang).ifBlank { "Delete" })
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", lang).ifBlank { "Cancel" })
                }
            }
        )
    }

    if (showAddFuelDialog) {
        AddVehicleFuelDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddFuelDialog = false }
        )
    }

    if (showAddServiceDialog) {
        AddVehicleServiceDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddServiceDialog = false }
        )
    }

    if (showAddDocDialog) {
        AddVehicleDocumentDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddDocDialog = false }
        )
    }

    if (showAddExpenseDialog) {
        AddVehicleExpenseDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddExpenseDialog = false }
        )
    }

    val vehiclesList by viewModel.vehicles.collectAsState()
    if (showAddInsuranceDialog || editingInsurancePolicy != null) {
        AddEditInsuranceDialog(
            vehicles = vehiclesList,
            editingPolicy = editingInsurancePolicy,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddInsuranceDialog = false
                editingInsurancePolicy = null
            },
            onSave = { policy ->
                if (policy.id == 0L) {
                    viewModel.addInsurancePolicy(policy)
                } else {
                    viewModel.updateInsurancePolicy(policy)
                }
                showAddInsuranceDialog = false
                editingInsurancePolicy = null
            }
        )
    }

    renewingInsurancePolicy?.let { policyToRenew ->
        RenewPolicyDialog(
            policy = policyToRenew,
            currencySymbol = currencySymbol,
            onDismiss = { renewingInsurancePolicy = null },
            onConfirmRenew = { newStart, newExpiry, newPremium ->
                viewModel.renewInsurancePolicy(policyToRenew, newStart, newExpiry, newPremium)
                renewingInsurancePolicy = null
            }
        )
    }

    if (showAddReminderDialog) {
        AddVehicleReminderDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddReminderDialog = false }
        )
    }

    if (showAddGeofenceDialog) {
        AddVehicleGeofenceDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddGeofenceDialog = false }
        )
    }

    if (showAddGalleryPhotoDialog) {
        AddVehicleGalleryPhotoDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddGalleryPhotoDialog = false }
        )
    }
}

// --- OVERVIEW TAB CONTENT ---
@Composable
private fun OverviewTabContent(
    vehicle: Vehicle,
    formattedLastUpdated: String,
    vFuel: List<FuelEntry>,
    vService: List<Maintenance>,
    vDocs: List<Document>,
    vExpenses: List<Expense>,
    vInsurance: List<InsurancePolicy>,
    vReminders: List<Reminder>,
    vGeofences: List<GeofenceZone>,
    fuelStats: FuelEfficiencyStats,
    totalFuelSpent: Double,
    totalServiceSpent: Double,
    totalInsuranceSpent: Double,
    totalCustomExpenses: Double,
    grandTotalExpenses: Double,
    currencySymbol: String,
    lang: AppLanguage,
    onSwitchTab: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vehicle Full Specs Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Full Vehicle Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                SpecItemRow(label = "Vehicle Name", value = vehicle.vehicleName)
                SpecItemRow(label = "Make / Brand", value = vehicle.brand.ifBlank { "N/A" })
                SpecItemRow(label = "Model", value = vehicle.model.ifBlank { "N/A" })
                SpecItemRow(label = "Manufacturing Year", value = vehicle.manufacturingYear.ifBlank { "N/A" })
                SpecItemRow(label = "License Plate", value = vehicle.registrationNumber.ifBlank { "N/A" })
                SpecItemRow(label = "VIN / Chassis No.", value = vehicle.vin.ifBlank { "N/A" })
                SpecItemRow(label = "Fuel Type", value = vehicle.fuelType)
                SpecItemRow(label = "Current Odometer", value = "${vehicle.odometerReading} km")
                SpecItemRow(label = "Purchase Date", value = vehicle.purchaseDate.ifBlank { "N/A" })
                SpecItemRow(label = "Notes", value = vehicle.notes.ifBlank { "None" })
                SpecItemRow(label = "Last Updated", value = formattedLastUpdated)
            }
        }

        // Financial Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Total Cost of Ownership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.2f", grandTotalExpenses)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Fuel: $currencySymbol${String.format(Locale.US, "%.0f", totalFuelSpent)} • Maintenance: $currencySymbol${String.format(Locale.US, "%.0f", totalServiceSpent)} • Insurance: $currencySymbol${String.format(Locale.US, "%.0f", totalInsuranceSpent)} • Expenses: $currencySymbol${String.format(Locale.US, "%.0f", totalCustomExpenses)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Summary Grid Cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryModuleCard(
                title = "Fuel Analytics",
                value = "${vFuel.size} Refills",
                subValue = "Avg ${String.format(Locale.US, "%.1f", fuelStats.kmPerLitre)} km/L",
                icon = Icons.Default.LocalGasStation,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(1) }
            )
            SummaryModuleCard(
                title = "Maintenance",
                value = "${vService.size} Logs",
                subValue = "$currencySymbol${String.format(Locale.US, "%.0f", totalServiceSpent)} total",
                icon = Icons.Default.Build,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(2) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryModuleCard(
                title = "Documents",
                value = "${vDocs.size} Attached",
                subValue = "View All Files",
                icon = Icons.Default.FolderOpen,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(3) }
            )
            SummaryModuleCard(
                title = "Insurance",
                value = if (vInsurance.isNotEmpty()) vInsurance.first().providerName else "Not Active",
                subValue = if (vInsurance.isNotEmpty()) "Expires: ${vInsurance.first().expiryDate}" else "Add policy",
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(5) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryModuleCard(
                title = "Geofences",
                value = "${vGeofences.count { it.isActive }} Active",
                subValue = "${vGeofences.size} Total Zones",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(7) }
            )
            SummaryModuleCard(
                title = "Photo Gallery",
                value = "${vDocs.count { it.docType == "Gallery" || it.docType == "Photo" } + if (vehicle.imageUri.isNotBlank()) 1 else 0} Photos",
                subValue = "View Gallery",
                icon = Icons.Default.Collections,
                modifier = Modifier.weight(1f),
                onClick = { onSwitchTab(8) }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SpecItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryModuleCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- TAB CONTENT COMPONENTS ---

@Composable
private fun FuelTabContent(
    vFuel: List<FuelEntry>,
    currencySymbol: String,
    lang: AppLanguage,
    onDeleteFuel: (FuelEntry) -> Unit
) {
    if (vFuel.isEmpty()) {
        EmptyTabPlaceholder(title = "No Fuel Records", subtitle = "Tap the + FAB button to add fuel refills for this vehicle.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vFuel, key = { it.id }) { fuel ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${fuel.fuelQuantity} L • ${fuel.fuelType}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Date: ${fuel.fuelDate} • Station: ${fuel.fuelStationName.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                            Text("Odometer: ${fuel.currentOdometer} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$currencySymbol${fuel.amountPaid}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onDeleteFuel(fuel) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTabContent(
    vService: List<Maintenance>,
    currencySymbol: String,
    lang: AppLanguage,
    onDeleteService: (Maintenance) -> Unit
) {
    if (vService.isEmpty()) {
        EmptyTabPlaceholder(title = "No Maintenance Logs", subtitle = "Tap the + FAB button to record maintenance or service history.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vService, key = { it.id }) { service ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.serviceTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${service.serviceType} • Date: ${service.serviceDate}", style = MaterialTheme.typography.bodyMedium)
                            Text("Workshop: ${service.workshopName.ifBlank { "N/A" }} • Odo: ${service.currentOdometer} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$currencySymbol${service.serviceCost}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onDeleteService(service) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsTabContent(
    vDocs: List<Document>,
    lang: AppLanguage,
    onDeleteDoc: (Document) -> Unit
) {
    if (vDocs.isEmpty()) {
        EmptyTabPlaceholder(title = "No Attached Documents", subtitle = "Tap the + FAB button to upload registration or license documents.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vDocs, key = { it.id }) { doc ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.docTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Category: ${doc.docType} • Expiry: ${doc.expiryDate.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onDeleteDoc(doc) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTabContent(
    vExpenses: List<Expense>,
    currencySymbol: String,
    lang: AppLanguage,
    onDeleteExpense: (Expense) -> Unit
) {
    if (vExpenses.isEmpty()) {
        EmptyTabPlaceholder(title = "No Custom Expenses", subtitle = "Tap the + FAB button to track parking, tolls, taxes, or other fees.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vExpenses, key = { it.id }) { exp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Category: ${exp.category} • Date: ${exp.date}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$currencySymbol${exp.amount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onDeleteExpense(exp) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsuranceTabContent(
    vInsurance: List<InsurancePolicy>,
    currencySymbol: String,
    lang: AppLanguage,
    onRenewPolicy: (InsurancePolicy) -> Unit,
    onEditPolicy: (InsurancePolicy) -> Unit,
    onDeletePolicy: (InsurancePolicy) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val warn30DaysStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    val activeCount = remember(vInsurance, todayStr, warn30DaysStr) {
        vInsurance.count { it.getPolicyStatus() == "ACTIVE" }
    }
    val expiringCount = remember(vInsurance, todayStr, warn30DaysStr) {
        vInsurance.count { it.getPolicyStatus() == "EXPIRING_SOON" }
    }
    val expiredCount = remember(vInsurance, todayStr) {
        vInsurance.count { it.getPolicyStatus() == "EXPIRED" }
    }
    val totalPremium = remember(vInsurance) {
        vInsurance.sumOf { it.premiumAmount }
    }

    if (vInsurance.isEmpty()) {
        EmptyTabPlaceholder(title = "No Insurance Policies", subtitle = "Tap the + FAB button to record policy details and expiration alerts.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Summary Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$activeCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expiring", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57F17))
                            Text("$expiringCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expired", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                            Text("$expiredCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }
                    Card(modifier = Modifier.weight(1.2f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Premium", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("$currencySymbol$totalPremium", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                        }
                    }
                }
            }

            items(vInsurance, key = { it.id }) { pol ->
                InsurancePolicyCard(
                    policy = pol,
                    currencySymbol = currencySymbol,
                    todayStr = todayStr,
                    warn30DaysStr = warn30DaysStr,
                    onRenew = { onRenewPolicy(pol) },
                    onEdit = { onEditPolicy(pol) },
                    onDelete = { onDeletePolicy(pol) },
                    onCallAgent = { }
                )
            }
        }
    }
}

@Composable
private fun RemindersTabContent(
    vReminders: List<Reminder>,
    lang: AppLanguage,
    onToggleReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit
) {
    if (vReminders.isEmpty()) {
        EmptyTabPlaceholder(title = "No Active Reminders", subtitle = "Tap the + FAB button to set service or inspection alerts.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vReminders, key = { it.id }) { rem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = rem.isCompleted,
                                onCheckedChange = { onToggleReminder(rem) }
                            )
                            Column {
                                Text(rem.reminderTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Due Date: ${rem.dueDate} • Type: ${rem.reminderType}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        IconButton(onClick = { onDeleteReminder(rem) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeofencingTabContent(
    vGeofences: List<GeofenceZone>,
    lang: AppLanguage,
    onToggleActive: (GeofenceZone) -> Unit,
    onDeleteGeofence: (GeofenceZone) -> Unit
) {
    if (vGeofences.isEmpty()) {
        EmptyTabPlaceholder(title = "No Geofence Zones", subtitle = "Tap the + FAB button to create virtual perimeter alerts for this vehicle.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vGeofences, key = { it.id }) { geo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(geo.zoneName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (geo.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (geo.isActive) "ACTIVE" else "DISABLED",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Center: ${String.format(Locale.US, "%.4f", geo.centerLatitude)}, ${String.format(Locale.US, "%.4f", geo.centerLongitude)} • Radius: ${geo.radiusMeters.toInt()}m", style = MaterialTheme.typography.bodyMedium)
                            Text("Alerts: ${if (geo.notifyOnEnter) "Enter" else ""} ${if (geo.notifyOnExit) "Exit" else ""}".trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = geo.isActive,
                                onCheckedChange = { onToggleActive(geo) }
                            )
                            IconButton(onClick = { onDeleteGeofence(geo) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTabContent(
    vehicle: Vehicle,
    vDocs: List<Document>,
    viewModel: DriveCareViewModel,
    lang: AppLanguage,
    onAddPhotoClick: () -> Unit
) {
    val context = LocalContext.current
    val photoDocs = remember(vDocs) { vDocs.filter { it.docType == "Gallery" || it.docType == "Photo" } }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    val mainPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedInfo = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (savedInfo != null) {
                viewModel.updateVehicle(vehicle.copy(imageUri = savedInfo.fileUriString))
                Toast.makeText(context, "Main vehicle photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Vehicle Photo Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Main Vehicle Cover Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { mainPhotoLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (vehicle.imageUri.isBlank()) "Set Photo" else "Change")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (vehicle.imageUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { fullScreenImageUri = vehicle.imageUri }
                    ) {
                        AsyncImage(
                            model = vehicle.imageUri,
                            contentDescription = "Main Vehicle Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("No main photo set. Tap 'Set Photo' to select one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Text("Photo Gallery (${photoDocs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (photoDocs.isEmpty()) {
            EmptyTabPlaceholder(title = "No Gallery Photos", subtitle = "Tap the + FAB or 'Add Photo' to upload photos of your vehicle.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photoDocs, key = { it.id }) { photo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable { fullScreenImageUri = photo.fileUri }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (photo.fileUri.isNotBlank()) {
                                AsyncImage(
                                    model = photo.fileUri,
                                    contentDescription = photo.docTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = photo.docTitle.ifBlank { "Photo" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteDocument(photo) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Image Dialog
    if (fullScreenImageUri != null) {
        AlertDialog(
            onDismissRequest = { fullScreenImageUri = null },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = fullScreenImageUri,
                        contentDescription = "Full Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { fullScreenImageUri = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmptyTabPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- QUICK ADD DIALOGS PRE-FILLED WITH VEHICLE ID ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleFuelDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(vehicle.odometerReading) }
    var station by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Fuel for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Fuel Litres *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount Paid *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = odo, onValueChange = { odo = it }, label = { Text("Odometer Reading") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = station, onValueChange = { station = it }, label = { Text("Fuel Station") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quantity.isNotBlank() && amount.isNotBlank()) {
                        viewModel.addFuelEntry(
                            FuelEntry(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                fuelDate = date,
                                fuelType = vehicle.fuelType,
                                fuelQuantity = quantity,
                                amountPaid = amount,
                                currentOdometer = odo,
                                fuelStationName = station
                            )
                        )
                        // Also update vehicle current odometer
                        if (odo.isNotBlank() && (odo.toLongOrNull() ?: 0L) > (vehicle.odometerReading.toLongOrNull() ?: 0L)) {
                            viewModel.updateVehicle(vehicle.copy(odometerReading = odo))
                        }
                        onDismiss()
                    }
                }
            ) { Text("Save Fuel Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleServiceDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Routine Service") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(vehicle.odometerReading) }
    var workshop by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Maintenance for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Service Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Service Cost *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = odo, onValueChange = { odo = it }, label = { Text("Current Odometer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = workshop, onValueChange = { workshop = it }, label = { Text("Workshop Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && cost.isNotBlank()) {
                        viewModel.addMaintenance(
                            Maintenance(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                serviceTitle = title,
                                serviceType = serviceType,
                                serviceCost = cost,
                                serviceDate = date,
                                currentOdometer = odo,
                                workshopName = workshop
                            )
                        )
                        if (odo.isNotBlank() && (odo.toLongOrNull() ?: 0L) > (vehicle.odometerReading.toLongOrNull() ?: 0L)) {
                            viewModel.updateVehicle(vehicle.copy(odometerReading = odo))
                        }
                        onDismiss()
                    }
                }
            ) { Text("Save Service") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleDocumentDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Registration") }
    var expiry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Document for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Document Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = expiry, onValueChange = { expiry = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addDocument(
                            Document(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                docTitle = title,
                                docType = docType,
                                expiryDate = expiry
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Document") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleExpenseDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Parking") }
    var amountStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Expense for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        viewModel.addExpense(
                            Expense(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                title = title,
                                category = category,
                                amount = amt,
                                date = date
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleInsuranceDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf("") }
    var policyNum by remember { mutableStateOf("") }
    var coverageType by remember { mutableStateOf("Comprehensive") }
    var premiumStr by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var expiry by remember { mutableStateOf("") }
    var agentContact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Insurance for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Insurance Provider *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = policyNum, onValueChange = { policyNum = it }, label = { Text("Policy Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = coverageType, onValueChange = { coverageType = it }, label = { Text("Coverage Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = premiumStr, onValueChange = { premiumStr = it }, label = { Text("Premium Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = expiry, onValueChange = { expiry = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = agentContact, onValueChange = { agentContact = it }, label = { Text("Agent Contact") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (provider.isNotBlank()) {
                        viewModel.addInsurancePolicy(
                            InsurancePolicy(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                providerName = provider,
                                policyNumber = policyNum,
                                coverageType = coverageType,
                                premiumAmount = premiumStr.toDoubleOrNull() ?: 0.0,
                                startDate = startDate,
                                expiryDate = expiry,
                                agentContact = agentContact
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Policy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleReminderDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Service") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Category / Type") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addReminder(
                            Reminder(
                                vehicleId = vehicle.id,
                                vehicleName = vehicle.vehicleName,
                                reminderTitle = title,
                                reminderType = type,
                                dueDate = dueDate
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Reminder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleGeofenceDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    var zoneName by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("28.6139") }
    var lngStr by remember { mutableStateOf("77.2090") }
    var radiusStr by remember { mutableStateOf("500") }
    var notifyOnEnter by remember { mutableStateOf(true) }
    var notifyOnExit by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Geofence for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = zoneName, onValueChange = { zoneName = it }, label = { Text("Zone Name *") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = radiusStr, onValueChange = { radiusStr = it }, label = { Text("Radius (Meters)") }, modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Entry")
                    Checkbox(checked = notifyOnEnter, onCheckedChange = { notifyOnEnter = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Exit")
                    Checkbox(checked = notifyOnExit, onCheckedChange = { notifyOnExit = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (zoneName.isNotBlank()) {
                        val lat = latStr.toDoubleOrNull() ?: 0.0
                        val lng = lngStr.toDoubleOrNull() ?: 0.0
                        val rad = radiusStr.toDoubleOrNull() ?: 500.0
                        viewModel.addGeofenceZone(
                            GeofenceZone(
                                vehicleId = vehicle.id,
                                zoneName = zoneName,
                                centerLatitude = lat,
                                centerLongitude = lng,
                                radiusMeters = rad,
                                notifyOnEnter = notifyOnEnter,
                                notifyOnExit = notifyOnExit,
                                isActive = true
                            )
                        )
                        onDismiss()
                    }
                }
            ) { Text("Save Geofence") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddVehicleGalleryPhotoDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo to Gallery") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Photo Title / Caption") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Button(
                    onClick = { pickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Select Photo from Device" else "Change Photo")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedUri != null,
                onClick = {
                    val uri = selectedUri
                    if (uri != null) {
                        val savedInfo = DocumentFileHelper.saveFileToInternalStorage(context, uri)
                        if (savedInfo != null) {
                            viewModel.addDocument(
                                Document(
                                    vehicleId = vehicle.id,
                                    vehicleName = vehicle.vehicleName,
                                    docTitle = caption.ifBlank { savedInfo.fileName },
                                    docType = "Gallery",
                                    fileUri = savedInfo.fileUriString,
                                    mimeType = savedInfo.mimeType,
                                    fileSize = savedInfo.fileSize
                                )
                            )
                            Toast.makeText(context, "Photo added to gallery!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                }
            ) { Text("Save Photo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
