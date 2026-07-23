package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Date
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (FuelEntry) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var quantity by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(selectedVehicle?.odometerReading ?: "0") }
    var station by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(todayStr) }

    LaunchedEffect(selectedVehicle) {
        selectedVehicle?.let { v ->
            if (odo.isBlank() || odo == "0") {
                odo = v.odometerReading
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("log_fuel", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                
                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
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
                                text = { Text("${v.vehicleName} (${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    odo = v.odometerReading
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*([.,]\d{0,2})?$"""))) {
                                quantity = input
                            }
                        },
                        label = { Text(AppStrings.get("litres", lang) + " *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odo,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                odo = input
                            }
                        },
                        label = { Text(AppStrings.get("odometer", lang) + " (km) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                OutlinedTextField(
                    value = station,
                    onValueChange = { station = it },
                    label = { Text(AppStrings.get("gas_station", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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

                    val cleanQtyStr = quantity.trim().replace(",", ".")
                    val qtyDouble = cleanQtyStr.toDoubleOrNull()
                    if (cleanQtyStr.isBlank() || qtyDouble == null || qtyDouble <= 0) {
                        Toast.makeText(context, "Please enter valid fuel quantity (> 0)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanCostStr = cost.trim().replace(",", ".")
                    val costDouble = cleanCostStr.toDoubleOrNull()
                    if (cleanCostStr.isBlank() || costDouble == null || costDouble <= 0) {
                        Toast.makeText(context, "Please enter valid fuel cost (> 0)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val odoDouble = odo.trim().toDoubleOrNull()
                    if (odo.isBlank() || odoDouble == null || odoDouble < 0) {
                        Toast.makeText(context, "Please enter valid odometer reading", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val entry = FuelEntry(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        fuelDate = date.ifBlank { todayStr },
                        fuelType = v.fuelType,
                        fuelQuantity = cleanQtyStr,
                        amountPaid = cleanCostStr,
                        currentOdometer = odo,
                        fuelStationName = station.trim()
                    )
                    onSave(entry)
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
