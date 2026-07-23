package com.drivecare.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.Expense
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.screens.VehicleFormDialog
import com.drivecare.app.utils.LocalAppLanguage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.drivecare.app.utils.DocumentFileHelper
import com.drivecare.app.utils.SavedFileInfo

@Composable
fun QuickActionRow(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeActionDialog by remember { mutableStateOf<String?>(null) } // "VEHICLE", "FUEL", "SERVICE", "DOCUMENT", "EXPENSE"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.DirectionsCar,
                    label = "+ Vehicle",
                    onClick = { activeActionDialog = "VEHICLE" }
                )
                QuickActionButton(
                    icon = Icons.Default.LocalGasStation,
                    label = "+ Fuel",
                    onClick = { activeActionDialog = "FUEL" }
                )
                QuickActionButton(
                    icon = Icons.Default.Build,
                    label = "+ Service",
                    onClick = { activeActionDialog = "SERVICE" }
                )
                QuickActionButton(
                    icon = Icons.Default.Description,
                    label = "+ Document",
                    onClick = { activeActionDialog = "DOCUMENT" }
                )
                QuickActionButton(
                    icon = Icons.Default.AttachMoney,
                    label = "+ Expense",
                    onClick = { activeActionDialog = "EXPENSE" }
                )
            }
        }
    }

    // Dialogs
    when (activeActionDialog) {
        "VEHICLE" -> {
            VehicleFormDialog(
                title = "Add New Vehicle",
                onDismiss = { activeActionDialog = null },
                onSave = { v ->
                    viewModel.addVehicle(v)
                    Toast.makeText(context, "Vehicle added!", Toast.LENGTH_SHORT).show()
                    activeActionDialog = null
                },
                lang = LocalAppLanguage.current
            )
        }
        "FUEL" -> {
            QuickAddFuelDialog(
                viewModel = viewModel,
                onDismiss = { activeActionDialog = null }
            )
        }
        "SERVICE" -> {
            QuickAddServiceDialog(
                viewModel = viewModel,
                onDismiss = { activeActionDialog = null }
            )
        }
        "DOCUMENT" -> {
            QuickAddDocumentDialog(
                viewModel = viewModel,
                onDismiss = { activeActionDialog = null }
            )
        }
        "EXPENSE" -> {
            QuickAddExpenseDialog(
                viewModel = viewModel,
                onDismiss = { activeActionDialog = null }
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddFuelDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()

    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var fuelDate by remember { mutableStateOf("2026-07-23") }
    var fuelQuantity by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf(selectedVehicle?.odometerReading ?: "") }
    var stationName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Fuel Refill", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicles.isEmpty()) {
                    Text("Please add a vehicle first.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleDropdown,
                        onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleDropdown,
                            onDismissRequest = { expandedVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.vehicleName) },
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
                            value = fuelQuantity,
                            onValueChange = { fuelQuantity = it },
                            label = { Text("Liters / Gal") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = amountPaid,
                            onValueChange = { amountPaid = it },
                            label = { Text("Amount ($)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = odo,
                            onValueChange = { odo = it },
                            label = { Text("Odometer (km)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fuelDate,
                            onValueChange = { fuelDate = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = stationName,
                        onValueChange = { stationName = it },
                        label = { Text("Station Name (e.g. Shell / BP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (vehicles.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedVehicle != null && fuelQuantity.isNotBlank() && amountPaid.isNotBlank()) {
                            viewModel.addFuelEntry(
                                FuelEntry(
                                    vehicleId = selectedVehicle!!.id,
                                    vehicleName = selectedVehicle!!.vehicleName,
                                    fuelDate = fuelDate,
                                    fuelType = selectedVehicle!!.fuelType,
                                    fuelQuantity = fuelQuantity,
                                    amountPaid = amountPaid,
                                    currentOdometer = odo,
                                    fuelStationName = stationName
                                )
                            )
                            if (odo.isNotBlank() && odo != selectedVehicle!!.odometerReading) {
                                viewModel.updateVehicle(selectedVehicle!!.copy(odometerReading = odo))
                            }
                            Toast.makeText(context, "Fuel entry added!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter quantity & amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddServiceDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()

    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Routine Service") }
    var serviceCost by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("2026-07-23") }
    var workshopName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Service Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicles.isEmpty()) {
                    Text("Please add a vehicle first.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleDropdown,
                        onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleDropdown,
                            onDismissRequest = { expandedVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.vehicleName) },
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
                        label = { Text("Service Title (e.g. Synthetic Oil Change)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = serviceCost,
                            onValueChange = { serviceCost = it },
                            label = { Text("Cost ($)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = serviceDate,
                            onValueChange = { serviceDate = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = workshopName,
                        onValueChange = { workshopName = it },
                        label = { Text("Workshop / Garage Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (vehicles.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedVehicle != null && title.isNotBlank()) {
                            viewModel.addMaintenance(
                                Maintenance(
                                    vehicleId = selectedVehicle!!.id,
                                    vehicleName = selectedVehicle!!.vehicleName,
                                    serviceTitle = title,
                                    serviceType = serviceType,
                                    serviceDate = serviceDate,
                                    currentOdometer = selectedVehicle!!.odometerReading,
                                    serviceCost = serviceCost.ifBlank { "0" },
                                    workshopName = workshopName
                                )
                            )
                            Toast.makeText(context, "Service record logged!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter service title", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDocumentDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()

    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var docTitle by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Insurance") }
    var expiryDate by remember { mutableStateOf("2027-07-23") }
    var attachedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = DocumentFileHelper.saveFileToInternalStorage(context, uri)
            if (saved != null) {
                attachedFileInfo = saved
                Toast.makeText(context, "File attached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vehicle Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicles.isEmpty()) {
                    Text("Please add a vehicle first.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleDropdown,
                        onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleDropdown,
                            onDismissRequest = { expandedVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.vehicleName) },
                                    onClick = {
                                        selectedVehicle = v
                                        expandedVehicleDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title (e.g. Annual Insurance Policy)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (attachedFileInfo != null) {
                        Text(
                            "Attached: ${attachedFileInfo!!.fileName} (${DocumentFileHelper.formatFileSize(attachedFileInfo!!.fileSize)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach Image or PDF Document")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (vehicles.isNotEmpty()) {
                Button(
                    onClick = {
                        if (selectedVehicle != null && docTitle.isNotBlank()) {
                            viewModel.addDocument(
                                Document(
                                    vehicleId = selectedVehicle!!.id,
                                    vehicleName = selectedVehicle!!.vehicleName,
                                    docTitle = docTitle,
                                    docType = docType,
                                    issueDate = "2026-07-23",
                                    expiryDate = expiryDate,
                                    notes = "",
                                    fileUri = attachedFileInfo?.fileUriString ?: "",
                                    mimeType = attachedFileInfo?.mimeType ?: "",
                                    fileSize = attachedFileInfo?.fileSize ?: 0L
                                )
                            )
                            Toast.makeText(context, "Document added!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter document title", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()

    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Toll") }
    var amountStr by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-07-23") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Expense Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicles.isEmpty()) {
                    Text("Please add a vehicle first.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleDropdown,
                        onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleDropdown,
                            onDismissRequest = { expandedVehicleDropdown = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.vehicleName) },
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
                        label = { Text("Expense Title (e.g. Toll Plaza Fee)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount ($)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (vehicles.isNotEmpty()) {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull()
                        if (selectedVehicle != null && title.isNotBlank() && amt != null) {
                            viewModel.addExpense(
                                Expense(
                                    vehicleId = selectedVehicle!!.id,
                                    vehicleName = selectedVehicle!!.vehicleName,
                                    title = title,
                                    category = category,
                                    amount = amt,
                                    date = date
                                )
                            )
                            Toast.makeText(context, "Expense saved!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter title and valid amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
