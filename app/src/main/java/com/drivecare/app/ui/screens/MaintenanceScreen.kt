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
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val advisorSuggestions = remember(vehicles, maintenanceLogs) {
        if (vehicles.isNotEmpty()) {
            viewModel.getMaintenanceAdvisorSuggestions(vehicles.first(), maintenanceLogs)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Smart Maintenance Advisor Section
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

            // Service History List
            Text(
                text = AppStrings.get("tab_service", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (maintenanceLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppStrings.get("no_service_logs", lang), style = MaterialTheme.typography.titleMedium)
                        Text(AppStrings.get("no_service_desc", lang), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(maintenanceLogs, key = { it.id }) { log ->
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
                                    Text(log.serviceTitle, fontWeight = FontWeight.Bold)
                                    Text("${log.vehicleName} • ${log.serviceDate}")
                                    if (log.workshopName.isNotBlank()) {
                                        Text("Workshop: ${log.workshopName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${log.serviceCost}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { viewModel.deleteMaintenance(log) }) {
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
        AddServiceDialog(
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = { log ->
                viewModel.addMaintenance(log)
                showAddDialog = false
            },
            lang = lang
        )
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

    val serviceTypes = listOf("Scheduled Service", "Oil Change", "Brake Inspection", "Tire Service", "Battery Check", "Engine Repair", "AC Service", "General Repair")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_service_log", lang)) },
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
                        workshopName = workshop.trim()
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
