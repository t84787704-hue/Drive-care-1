package com.drivecare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrackerScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val allFuelEntries by viewModel.fuelEntries.collectAsState()
    val selectedVehicle by viewModel.selectedFuelVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }

    val filteredEntries = remember(allFuelEntries, selectedVehicle) {
        if (selectedVehicle == null) {
            allFuelEntries
        } else {
            allFuelEntries.filter { it.vehicleId == selectedVehicle?.id }
        }
    }

    val totalCost = remember(filteredEntries) {
        filteredEntries.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
    }
    val totalQuantity = remember(filteredEntries) {
        filteredEntries.sumOf { it.fuelQuantity.toDoubleOrNull() ?: 0.0 }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Log Fuel") },
                text = { Text("Log Fuel") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Vehicle Selector Dropdown Bar
            Text(
                text = "Select Vehicle",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = vehicleDropdownExpanded,
                onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedVehicle?.vehicleName ?: "All Vehicles",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = vehicleDropdownExpanded,
                    onDismissRequest = { vehicleDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Vehicles") },
                        onClick = {
                            viewModel.selectFuelVehicle(null)
                            vehicleDropdownExpanded = false
                        }
                    )
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.vehicleName) },
                            onClick = {
                                viewModel.selectFuelVehicle(vehicle)
                                vehicleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Stats Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Spent", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalCost)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Quantity", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalQuantity)} L",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fuel entries found for selected vehicle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        FuelCard(
                            entry = entry,
                            onDelete = { viewModel.deleteFuelEntry(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFuelDialog(
            vehicles = vehicles,
            preselectedVehicle = selectedVehicle ?: vehicles.firstOrNull(),
            onDismiss = { showAddDialog = false },
            onSave = { entry ->
                viewModel.addFuelEntry(entry)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FuelCard(
    entry: FuelEntry,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = entry.vehicleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${entry.fuelQuantity} L • $${entry.amountPaid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Date: ${entry.fuelDate} | Odometer: ${entry.currentOdometer} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelDialog(
    vehicles: List<Vehicle>,
    preselectedVehicle: Vehicle?,
    onDismiss: () -> Unit,
    onSave: (FuelEntry) -> Unit
) {
    var selectedVehicle by remember { mutableStateOf(preselectedVehicle ?: vehicles.firstOrNull()) }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    val currentDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var date by remember { mutableStateOf(currentDate) }
    var quantity by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf(selectedVehicle?.odometerReading ?: "") }
    var station by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fuel Log") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vehicle selection dropdown
                Text("Select Vehicle *", style = MaterialTheme.typography.labelSmall)
                ExposedDropdownMenuBox(
                    expanded = vehicleMenuExpanded,
                    onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = vehicleMenuExpanded,
                        onDismissRequest = { vehicleMenuExpanded = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.vehicleName) },
                                onClick = {
                                    selectedVehicle = v
                                    if (odometer.isBlank()) odometer = v.odometerReading
                                    vehicleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Litres/Gals") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost ($)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odometer,
                        onValueChange = { odometer = it },
                        label = { Text("Odometer") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = station,
                    onValueChange = { station = it },
                    label = { Text("Gas Station (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVehicle = selectedVehicle
                    if (targetVehicle != null) {
                        onSave(
                            FuelEntry(
                                vehicleId = targetVehicle.id,
                                vehicleName = targetVehicle.vehicleName,
                                fuelDate = date,
                                fuelType = targetVehicle.fuelType,
                                fuelQuantity = quantity,
                                amountPaid = cost,
                                currentOdometer = odometer,
                                fuelStationName = station
                            )
                        )
                    }
                },
                enabled = selectedVehicle != null && quantity.isNotBlank() && cost.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
