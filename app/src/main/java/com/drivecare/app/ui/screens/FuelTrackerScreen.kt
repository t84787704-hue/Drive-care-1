package com.drivecare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrackerScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val allFuelEntries by viewModel.fuelEntries.collectAsState()
    val selectedVehicle by viewModel.selectedFuelVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val filteredFuel = if (selectedVehicle == null) {
        allFuelEntries
    } else {
        allFuelEntries.filter { it.vehicleId == selectedVehicle?.id }
    }

    val totalSpent = filteredFuel.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
    val totalLitres = filteredFuel.sumOf { it.fuelQuantity.toDoubleOrNull() ?: 0.0 }
    val avgPricePerLitre = if (totalLitres > 0) totalSpent / totalLitres else 0.0

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = AppStrings.get("log_fuel", lang)) },
                    text = { Text(AppStrings.get("log_fuel", lang)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fuel Analytics Header Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(AppStrings.get("total_spent", lang), style = MaterialTheme.typography.labelSmall)
                        Text("$${String.format(Locale.US, "%.2f", totalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(AppStrings.get("total_quantity", lang), style = MaterialTheme.typography.labelSmall)
                        Text("${String.format(Locale.US, "%.1f", totalLitres)} L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Fuel Log List
            Text(
                text = AppStrings.get("tab_fuel", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (filteredFuel.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppStrings.get("no_fuel_entries", lang), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredFuel, key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(entry.vehicleName, fontWeight = FontWeight.Bold)
                                    Text("${entry.fuelDate} • ${entry.fuelType} • ${entry.currentOdometer} km")
                                    if (entry.fuelStationName.isNotBlank()) {
                                        Text("Station: ${entry.fuelStationName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${entry.amountPaid}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("${entry.fuelQuantity} Litres", style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { viewModel.deleteFuelEntry(entry) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && vehicles.isNotEmpty()) {
        AddFuelDialog(
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = { entry ->
                viewModel.addFuelEntry(entry)
                showAddDialog = false
            },
            lang = lang
        )
    }
}

@Composable
fun AddFuelDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (FuelEntry) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var quantity by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(selectedVehicle.odometerReading) }
    var station by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-07-22") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("log_fuel", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(AppStrings.get("select_vehicle", lang), fontWeight = FontWeight.Bold)
                OutlinedTextField(value = selectedVehicle.vehicleName, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text(AppStrings.get("litres", lang)) }, singleLine = true)
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(AppStrings.get("cost", lang)) }, singleLine = true)
                OutlinedTextField(value = odo, onValueChange = { odo = it }, label = { Text(AppStrings.get("odometer", lang)) }, singleLine = true)
                OutlinedTextField(value = station, onValueChange = { station = it }, label = { Text(AppStrings.get("gas_station", lang)) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quantity.isNotBlank() && cost.isNotBlank()) {
                        val entry = FuelEntry(
                            vehicleId = selectedVehicle.id,
                            vehicleName = selectedVehicle.vehicleName,
                            fuelDate = date,
                            fuelType = selectedVehicle.fuelType,
                            fuelQuantity = quantity,
                            amountPaid = cost,
                            currentOdometer = odo,
                            fuelStationName = station
                        )
                        onSave(entry)
                    }
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
