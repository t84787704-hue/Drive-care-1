package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivecare.app.NavTab
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.PdfReportGenerator
import com.drivecare.app.utils.VehicleTypeHelper
import com.drivecare.app.ui.components.AdvancedVehicleDeleteDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSection: ((NavTab, MoreSubSection?, Long?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val insurancePolicies by viewModel.insurancePolicies.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedVehicleForProfile by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var selectedFilterType by remember { mutableStateOf("All") }

    val vehicleTypes = remember { listOf("All") + VehicleTypeHelper.ALL_TYPES.map { it.code } }

    val filteredVehicles = if (selectedFilterType == "All") {
        vehicles
    } else {
        vehicles.filter { it.vehicleType.equals(selectedFilterType, ignoreCase = true) }
    }

    if (selectedVehicleForProfile != null) {
        val currentV = vehicles.find { it.id == selectedVehicleForProfile!!.id } ?: selectedVehicleForProfile!!
        VehicleDetailScreen(
            vehicle = currentV,
            viewModel = viewModel,
            modifier = modifier,
            onBackClick = { selectedVehicleForProfile = null },
            onNavigateToSection = { tab, subSection, vId ->
                selectedVehicleForProfile = null
                onNavigateToSection?.invoke(tab, subSection, vId)
            }
        )
    } else {
        Scaffold(
            modifier = modifier,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = AppStrings.get("add_vehicle", lang)) },
                    text = { Text(AppStrings.get("add_vehicle", lang)) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Filter Bar
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vehicleTypes) { type ->
                        val labelText = if (type == "All") AppStrings.get("all_vehicles", lang) else VehicleTypeHelper.getDisplayName(type, lang)
                        FilterChip(
                            selected = selectedFilterType == type,
                            onClick = { selectedFilterType = type },
                            label = { Text(labelText) },
                            leadingIcon = if (type != "All") {
                                {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                if (filteredVehicles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = AppStrings.get("no_vehicles_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = AppStrings.get("no_vehicles_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showAddDialog = true }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.get("add_vehicle", lang))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredVehicles, key = { it.id }) { v ->
                            val health = viewModel.calculateHealthScore(v, reminders, fuelEntries, maintenanceLogs, documents)
                            val vehicleDocsCount = documents.count { it.vehicleId == v.id }
                            VehicleCard(
                                vehicle = v,
                                healthScore = health,
                                docCount = vehicleDocsCount,
                                onProfileClick = { selectedVehicleForProfile = v },
                                onEditClick = { vehicleToEdit = v },
                                onDeleteClick = { vehicleToDelete = v },
                                lang = lang
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddDialog) {
        VehicleFormDialog(
            title = AppStrings.get("add_vehicle", lang),
            onDismiss = { showAddDialog = false },
            onSave = { newVehicle ->
                viewModel.addVehicle(newVehicle)
                showAddDialog = false
            },
            lang = lang
        )
    }

    // Edit Vehicle Dialog
    vehicleToEdit?.let { v ->
        VehicleFormDialog(
            title = AppStrings.get("edit_vehicle", lang),
            vehicle = v,
            onDismiss = { vehicleToEdit = null },
            onSave = { updated ->
                viewModel.updateVehicle(updated)
                vehicleToEdit = null
            },
            lang = lang
        )
    }

    // Delete Confirmation Dialog
    vehicleToDelete?.let { v ->
        AdvancedVehicleDeleteDialog(
            vehicle = v,
            viewModel = viewModel,
            lang = lang,
            onDismiss = { vehicleToDelete = null },
            onDeletedSuccessfully = {
                vehicleToDelete = null
                Toast.makeText(context, "Vehicle ${v.vehicleName} removed completely", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    healthScore: Int,
    docCount: Int = 0,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val vehicleIcon = VehicleTypeHelper.getVehicleIcon(vehicle.vehicleType)
    val localizedTypeName = VehicleTypeHelper.getDisplayName(vehicle.vehicleType, lang)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = vehicleIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(vehicle.vehicleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$localizedTypeName • ${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$docCount Docs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (healthScore >= 80) MaterialTheme.colorScheme.primaryContainer else if (healthScore >= 50) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$healthScore%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${AppStrings.get("plate", lang)}: ${vehicle.registrationNumber.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                Text("${vehicle.fuelType} • ${vehicle.odometerReading} km", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit_vehicle", lang))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = AppStrings.get("delete_vehicle", lang), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormDialog(
    title: String,
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(vehicle?.vehicleName ?: "") }
    var type by remember { mutableStateOf(vehicle?.vehicleType ?: "Car") }
    var customTypeInput by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var year by remember { mutableStateOf(vehicle?.manufacturingYear ?: "2024") }
    var plate by remember { mutableStateOf(vehicle?.registrationNumber ?: "") }
    var fuelType by remember { mutableStateOf(vehicle?.fuelType ?: "Petrol") }
    var odometer by remember { mutableStateOf(vehicle?.odometerReading ?: "0") }
    var vin by remember { mutableStateOf(vehicle?.vin ?: "") }
    var purchaseDate by remember { mutableStateOf(vehicle?.purchaseDate ?: "") }
    var notes by remember { mutableStateOf(vehicle?.notes ?: "") }

    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var expandedFuelDropdown by remember { mutableStateOf(false) }

    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "CNG", "LPG")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("vehicle_name", lang) + " *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Vehicle Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = VehicleTypeHelper.getDisplayName(type, lang),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("type", lang) + " *") },
                        leadingIcon = {
                            Icon(
                                imageVector = VehicleTypeHelper.getVehicleIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        VehicleTypeHelper.ALL_TYPES.forEach { t ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = VehicleTypeHelper.getVehicleIcon(t.code),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text(AppStrings.get(t.stringKey, lang)) },
                                onClick = {
                                    type = t.code
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                if (type == "Other") {
                    OutlinedTextField(
                        value = customTypeInput,
                        onValueChange = { customTypeInput = it },
                        label = { Text("Custom Vehicle Type Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(AppStrings.get("brand", lang) + " *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(AppStrings.get("model", lang) + " *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                year = input.take(4)
                            }
                        },
                        label = { Text(AppStrings.get("year", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        label = { Text(AppStrings.get("plate_no", lang) + " *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fuel Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedFuelDropdown,
                    onExpandedChange = { expandedFuelDropdown = !expandedFuelDropdown }
                ) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fuel Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFuelDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFuelDropdown,
                        onDismissRequest = { expandedFuelDropdown = false }
                    ) {
                        fuelTypes.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    fuelType = f
                                    expandedFuelDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = odometer,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            odometer = input
                        }
                    },
                    label = { Text(AppStrings.get("odometer", lang) + " (km) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.uppercase() },
                    label = { Text("VIN / Chassis No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("Purchase Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Please enter vehicle name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val cleanBrand = brand.trim()
                    if (cleanBrand.isBlank()) {
                        Toast.makeText(context, "Please enter brand", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val cleanModel = model.trim()
                    if (cleanModel.isBlank()) {
                        Toast.makeText(context, "Please enter model", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val cleanPlate = plate.trim()
                    if (cleanPlate.isBlank()) {
                        Toast.makeText(context, "Please enter registration plate", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val odoDouble = odometer.toDoubleOrNull()
                    if (odometer.isBlank() || odoDouble == null || odoDouble < 0) {
                        Toast.makeText(context, "Please enter valid odometer reading", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalType = if (type == "Other" && customTypeInput.isNotBlank()) customTypeInput.trim() else type
                    val newV = (vehicle ?: Vehicle(vehicleName = cleanName)).copy(
                        vehicleName = cleanName,
                        vehicleType = finalType,
                        brand = cleanBrand,
                        model = cleanModel,
                        manufacturingYear = year.ifBlank { "2024" },
                        registrationNumber = cleanPlate,
                        fuelType = fuelType,
                        odometerReading = odometer,
                        vin = vin,
                        purchaseDate = purchaseDate,
                        notes = notes,
                        lastUpdated = System.currentTimeMillis()
                    )
                    onSave(newV)
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
fun VehicleDetailDialog(
    vehicle: Vehicle,
    viewModel: DriveCareViewModel,
    fuelEntries: List<com.drivecare.app.data.model.FuelEntry>,
    maintenanceLogs: List<com.drivecare.app.data.model.Maintenance>,
    reminders: List<com.drivecare.app.data.model.Reminder>,
    documents: List<com.drivecare.app.data.model.Document>,
    insurancePolicies: List<com.drivecare.app.data.model.InsurancePolicy>,
    expenses: List<com.drivecare.app.data.model.Expense>,
    lang: com.drivecare.app.utils.AppLanguage,
    onDismiss: () -> Unit,
    onNavigateToSection: (NavTab, MoreSubSection?, Long) -> Unit
) {
    val context = LocalContext.current
    val currencySymbol by viewModel.currentCurrencySymbol.collectAsState()
    val health = viewModel.calculateHealthScore(vehicle, reminders, fuelEntries, maintenanceLogs, documents)

    val vFuel = fuelEntries.filter { it.vehicleId == vehicle.id }
    val vService = maintenanceLogs.filter { it.vehicleId == vehicle.id }
    val vInsurance = insurancePolicies.filter { it.vehicleId == vehicle.id }
    val vDocs = documents.filter { it.vehicleId == vehicle.id }
    val vReminders = reminders.filter { it.vehicleId == vehicle.id && !it.isCompleted }
    val vExpenses = expenses.filter { it.vehicleId == vehicle.id }

    val geofences by viewModel.geofenceZones.collectAsState()
    val vGeofences = geofences.filter { it.vehicleId == vehicle.id }

    val fuelStats = viewModel.calculateVehicleFuelEfficiency(vehicle, fuelEntries)
    val totalFuelSpent = vFuel.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
    val totalServiceSpent = vService.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
    val totalInsuranceSpent = vInsurance.sumOf { it.premiumAmount }
    val totalCustomExpenses = vExpenses.sumOf { it.amount }
    val grandTotalExpenses = totalFuelSpent + totalServiceSpent + totalInsuranceSpent + totalCustomExpenses

    val latestService = vService.maxByOrNull { it.serviceDate }
    val activePolicy = vInsurance.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(vehicle.vehicleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${vehicle.brand} ${vehicle.model} (${vehicle.manufacturingYear})", style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    color = if (health >= 80) MaterialTheme.colorScheme.primaryContainer else if (health >= 50) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Health: $health%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Vehicle Specs Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Vehicle Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text("Registration Plate: ${vehicle.registrationNumber.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                        Text("Vehicle Type: ${VehicleTypeHelper.getDisplayName(vehicle.vehicleType, lang)} • Fuel: ${vehicle.fuelType}", style = MaterialTheme.typography.bodySmall)
                        Text("Odometer Reading: ${vehicle.odometerReading} km", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        if (vehicle.notes.isNotBlank()) {
                            Text("Notes: ${vehicle.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 2. Fuel Tracker Summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Fuel Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { onNavigateToSection(NavTab.FUEL, null, vehicle.id) }) {
                                Text("Log Fuel", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("Total Fuel Spent: ${String.format(Locale.US, "%.2f", totalFuelSpent)} (${vFuel.size} Refills)", style = MaterialTheme.typography.bodySmall)
                        Text("Avg Fuel Efficiency: ${String.format(Locale.US, "%.1f", fuelStats.kmPerLitre)} km/L (${String.format(Locale.US, "%.1f", fuelStats.litresPer100Km)} L/100km)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }

                // 3. Maintenance History Summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Maintenance History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { onNavigateToSection(NavTab.SERVICE, null, vehicle.id) }) {
                                Text("Add Service", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("Total Service Cost: ${String.format(Locale.US, "%.2f", totalServiceSpent)} (${vService.size} Logs)", style = MaterialTheme.typography.bodySmall)
                        if (latestService != null) {
                            Text("Latest Service: ${latestService.serviceTitle} on ${latestService.serviceDate}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("No service logs recorded yet.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 4. Insurance Status Summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Insurance Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { onNavigateToSection(NavTab.MORE, MoreSubSection.INSURANCE, vehicle.id) }) {
                                Text("Manage", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (activePolicy != null) {
                            Text("Provider: ${activePolicy.providerName} (#${activePolicy.policyNumber})", style = MaterialTheme.typography.bodySmall)
                            Text("Coverage: ${activePolicy.coverageType} • Expires: ${activePolicy.expiryDate}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("No active insurance policy configured.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 4b. Geofence Safe Zones Summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Fence, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Geofence Safe Zones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(onClick = { onNavigateToSection(NavTab.MORE, MoreSubSection.GPS_TRACKING, vehicle.id) }) {
                                Text(if (vGeofences.isEmpty()) "Add Zone" else "Manage", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (vGeofences.isNotEmpty()) {
                            Text("${vGeofences.size} Active Safe Zone(s) Monitored", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("Zones: ${vGeofences.joinToString { "${it.zoneName} (${it.radiusMeters.toInt()}m)" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("No safe perimeter zones active for this vehicle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 5. Documents & Reminders
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f).clickable { onNavigateToSection(NavTab.MORE, MoreSubSection.DOCUMENTS, vehicle.id) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Documents", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("${vDocs.size} Attached Files", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).clickable { onNavigateToSection(NavTab.SERVICE, null, vehicle.id) }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("${vReminders.size} Due / Pending", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 6. Total Expenses Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Vehicle Expenses", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "$currencySymbol${String.format(Locale.US, "%.2f", grandTotalExpenses)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Fuel: $currencySymbol${String.format(Locale.US, "%.0f", totalFuelSpent)} • Service: $currencySymbol${String.format(Locale.US, "%.0f", totalServiceSpent)} • Insurance: $currencySymbol${String.format(Locale.US, "%.0f", totalInsuranceSpent)} • Other: $currencySymbol${String.format(Locale.US, "%.0f", totalCustomExpenses)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = PdfReportGenerator.generateAndShareReport(context, vehicle, health, vFuel, vService)
                    if (file != null) {
                        Toast.makeText(context, AppStrings.get("pdf_generated", lang), Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("export_pdf", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
