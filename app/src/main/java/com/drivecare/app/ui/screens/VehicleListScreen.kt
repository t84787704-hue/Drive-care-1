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
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.PdfReportGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedVehicleForProfile by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var selectedFilterType by remember { mutableStateOf("All") }

    val vehicleTypes = listOf("All", "Car", "SUV", "Motorcycle", "Van", "Truck", "Fleet")

    val filteredVehicles = if (selectedFilterType == "All") {
        vehicles
    } else {
        vehicles.filter { it.vehicleType.equals(selectedFilterType, ignoreCase = true) }
    }

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
                    FilterChip(
                        selected = selectedFilterType == type,
                        onClick = { selectedFilterType = type },
                        label = { Text(if (type == "All") AppStrings.get("all_vehicles", lang) else type) }
                    )
                }
            }

            if (filteredVehicles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppStrings.get("no_vehicles_title", lang), style = MaterialTheme.typography.titleMedium)
                        Text(AppStrings.get("no_vehicles_desc", lang), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVehicles, key = { it.id }) { v ->
                        val health = viewModel.calculateHealthScore(v, reminders, fuelEntries, maintenanceLogs)
                        VehicleCard(
                            vehicle = v,
                            healthScore = health,
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

    // Vehicle Detail / Profile Modal
    selectedVehicleForProfile?.let { v ->
        val health = viewModel.calculateHealthScore(v, reminders, fuelEntries, maintenanceLogs)
        val vFuel = fuelEntries.filter { it.vehicleId == v.id }
        val vService = maintenanceLogs.filter { it.vehicleId == v.id }

        AlertDialog(
            onDismissRequest = { selectedVehicleForProfile = null },
            title = { Text(AppStrings.get("vehicle_profile", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(v.vehicleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Type: ${v.vehicleType} • Brand: ${v.brand} • Model: ${v.model}")
                    Text("Year: ${v.manufacturingYear} • Fuel: ${v.fuelType}")
                    Text("Registration Plate: ${v.registrationNumber}")
                    Text("Odometer Reading: ${v.odometerReading} km")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Health Score: $health / 100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Fuel Logs Count: ${vFuel.size}")
                    Text("Service Logs Count: ${vService.size}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = PdfReportGenerator.generateAndShareReport(context, v, health, vFuel, vService)
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
                TextButton(onClick = { selectedVehicleForProfile = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
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
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text(AppStrings.get("confirm_delete_title", lang)) },
            text = { Text(AppStrings.get("confirm_delete_msg", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(v)
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("delete", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    healthScore: Int,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val vehicleIcon = when (vehicle.vehicleType.lowercase()) {
        "suv", "van" -> Icons.Default.AirportShuttle
        "motorcycle", "bike" -> Icons.Default.TwoWheeler
        "truck" -> Icons.Default.LocalShipping
        "electric", "ev" -> Icons.Default.EvStation
        else -> Icons.Default.DirectionsCar
    }

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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                    Column {
                        Text(vehicle.vehicleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${vehicle.vehicleType} • ${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.bodyMedium)
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
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var year by remember { mutableStateOf(vehicle?.manufacturingYear ?: "2024") }
    var plate by remember { mutableStateOf(vehicle?.registrationNumber ?: "") }
    var fuelType by remember { mutableStateOf(vehicle?.fuelType ?: "Petrol") }
    var odometer by remember { mutableStateOf(vehicle?.odometerReading ?: "0") }

    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var expandedFuelDropdown by remember { mutableStateOf(false) }

    val vehicleTypes = listOf("Car", "SUV", "Motorcycle", "Van", "Truck", "Electric", "Fleet")
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
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        vehicleTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
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

                    val newV = (vehicle ?: Vehicle(vehicleName = cleanName)).copy(
                        vehicleName = cleanName,
                        vehicleType = type,
                        brand = cleanBrand,
                        model = cleanModel,
                        manufacturingYear = year.ifBlank { "2024" },
                        registrationNumber = cleanPlate,
                        fuelType = fuelType,
                        odometerReading = odometer
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
