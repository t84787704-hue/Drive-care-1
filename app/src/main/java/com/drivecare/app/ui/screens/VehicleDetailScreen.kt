package com.drivecare.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecare.app.NavTab
import com.drivecare.app.data.model.*
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.FuelEfficiencyStats
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
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
        AppStrings.get("tab_reminders", lang).ifBlank { "Reminders" }
    )

    // Dialog state handlers
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddFuelDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddInsuranceDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

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
                1 -> "Add Fuel" to Icons.Default.LocalGasStation
                2 -> "Add Service" to Icons.Default.Build
                3 -> "Add Document" to Icons.Default.FolderOpen
                4 -> "Add Expense" to Icons.Default.AttachMoney
                5 -> "Add Insurance" to Icons.Default.Security
                6 -> "Add Reminder" to Icons.Default.NotificationsActive
                else -> "Quick Add" to Icons.Default.Add
            }

            ExtendedFloatingActionButton(
                text = { Text(fabLabel) },
                icon = { Icon(fabIcon, contentDescription = fabLabel) },
                onClick = {
                    when (selectedTabIndex) {
                        1 -> showAddFuelDialog = true
                        2 -> showAddServiceDialog = true
                        3 -> showAddDocDialog = true
                        4 -> showAddExpenseDialog = true
                        5 -> showAddInsuranceDialog = true
                        6 -> showAddReminderDialog = true
                        else -> showAddFuelDialog = true
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
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = VehicleTypeHelper.getVehicleIcon(vehicle.vehicleType),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                        onDeletePolicy = { viewModel.deleteInsurancePolicy(it) }
                    )
                    6 -> RemindersTabContent(
                        vReminders = vReminders,
                        lang = lang,
                        onToggleReminder = { viewModel.toggleReminder(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) }
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
                Text("Are you sure you want to delete ${vehicle.vehicleName}? This will permanently remove the vehicle and ALL associated fuel logs, maintenance records, documents, expenses, insurance policies, and reminders from both local storage and cloud sync.")
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

    if (showAddInsuranceDialog) {
        AddVehicleInsuranceDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddInsuranceDialog = false }
        )
    }

    if (showAddReminderDialog) {
        AddVehicleReminderDialog(
            vehicle = vehicle,
            viewModel = viewModel,
            onDismiss = { showAddReminderDialog = false }
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
                Divider()

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
    onDeletePolicy: (InsurancePolicy) -> Unit
) {
    if (vInsurance.isEmpty()) {
        EmptyTabPlaceholder(title = "No Insurance Policies", subtitle = "Tap the + FAB button to record policy details and expiration alerts.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(vInsurance, key = { it.id }) { pol ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pol.providerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeletePolicy(pol) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text("Policy #: ${pol.policyNumber} • Coverage: ${pol.coverageType}", style = MaterialTheme.typography.bodyMedium)
                        Text("Premium: $currencySymbol${pol.premiumAmount} • Expires: ${pol.expiryDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
    var premiumStr by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Insurance for ${vehicle.vehicleName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Insurance Provider *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = policyNum, onValueChange = { policyNum = it }, label = { Text("Policy Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = premiumStr, onValueChange = { premiumStr = it }, label = { Text("Premium Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = expiry, onValueChange = { expiry = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
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
                                premiumAmount = premiumStr.toDoubleOrNull() ?: 0.0,
                                expiryDate = expiry
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
