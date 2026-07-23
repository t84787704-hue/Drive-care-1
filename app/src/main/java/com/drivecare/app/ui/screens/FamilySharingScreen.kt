package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.DriverProfile
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.model.VehicleShare
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.FeatureFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySharingScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val profiles by viewModel.driverProfiles.collectAsState()
    val shares by viewModel.vehicleShares.collectAsState()
    val isFamilySharingEnabled by FeatureFlags.familySharingEnabled.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Driver Profiles, 1: Vehicle Sharing, 2: Ownership Transfer
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showAddShareDialog by remember { mutableStateOf(false) }
    var showTransferOwnershipDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Family Sharing Header & Feature Flag Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Family Sharing & Fleet Multi-User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Share vehicles, manage drivers & transfer ownership", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isFamilySharingEnabled,
                        onCheckedChange = { FeatureFlags.setFamilySharingEnabled(context, it) }
                    )
                }
            }
        }

        if (!isFamilySharingEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Family Sharing is turned off in settings.")
            }
        } else {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Driver Profiles") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Vehicle Access") },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Ownership") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Driver Profiles Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Registered Drivers (${profiles.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddProfileDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Driver")
                            }
                        }

                        if (profiles.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Driver Profiles Yet", fontWeight = FontWeight.SemiBold)
                                    Text("Add family members or authorized drivers for vehicle sharing.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(profiles) { profile ->
                                    DriverProfileCard(profile = profile, onDelete = { viewModel.deleteDriverProfile(profile) })
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Vehicle Sharing Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shared Access Grants (${shares.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddShareDialog = true }) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Vehicle")
                            }
                        }

                        if (shares.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Shared Vehicles", fontWeight = FontWeight.SemiBold)
                                    Text("Grant secondary access to family members or fleet drivers.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(shares) { share ->
                                    VehicleShareCard(share = share, onDelete = { viewModel.deleteVehicleShare(share) })
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Vehicle Ownership Transfer Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Vehicle Ownership Transfer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Transfer master title registration and vehicle history to another registered driver profile or email address.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showTransferOwnershipDialog = true },
                                    enabled = vehicles.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Initiate Ownership Transfer")
                                }
                            }
                        }

                        Text("Current Garage Fleet Ownership", fontWeight = FontWeight.Bold)

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(vehicles) { v ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(v.vehicleName, fontWeight = FontWeight.Bold)
                                            Text("Plate: ${v.registrationNumber.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                            Text("Owner (Primary)", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AddDriverProfileDialog(
            viewModel = viewModel,
            onDismiss = { showAddProfileDialog = false }
        )
    }

    if (showAddShareDialog) {
        AddVehicleShareDialog(
            viewModel = viewModel,
            vehicles = vehicles,
            onDismiss = { showAddShareDialog = false }
        )
    }

    if (showTransferOwnershipDialog) {
        TransferOwnershipDialog(
            viewModel = viewModel,
            vehicles = vehicles,
            profiles = profiles,
            onDismiss = { showTransferOwnershipDialog = false }
        )
    }
}

@Composable
fun DriverProfileCard(profile: DriverProfile, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(profile.name, fontWeight = FontWeight.Bold)
                    Text(profile.email.ifBlank { profile.phone.ifBlank { "Driver Profile" } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (profile.licenseNumber.isNotBlank()) {
                        Text("License: ${profile.licenseNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun VehicleShareCard(share: VehicleShare, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${share.vehicleName} shared with:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(share.sharedWithEmail, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                        Text(share.role, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
                        Text(share.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddDriverProfileDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Driver Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("Driver License #") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addDriverProfile(
                            DriverProfile(
                                name = name,
                                email = email,
                                phone = phone,
                                licenseNumber = license
                            )
                        )
                        Toast.makeText(context, "Driver Profile Created!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleShareDialog(
    viewModel: DriveCareViewModel,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("DRIVER") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Vehicle Access", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicles.isEmpty()) {
                    Text("No vehicles in garage.")
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
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Recipient Email") },
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
                        if (selectedVehicle != null && email.isNotBlank()) {
                            viewModel.addVehicleShare(
                                VehicleShare(
                                    vehicleId = selectedVehicle!!.id,
                                    vehicleName = selectedVehicle!!.vehicleName,
                                    sharedWithEmail = email,
                                    role = role,
                                    status = "ACTIVE"
                                )
                            )
                            Toast.makeText(context, "Access invitation sent!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Share")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferOwnershipDialog(
    viewModel: DriveCareViewModel,
    vehicles: List<Vehicle>,
    profiles: List<DriverProfile>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    var newOwnerName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Vehicle Ownership", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle to Transfer") },
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
                    value = newOwnerName,
                    onValueChange = { newOwnerName = it },
                    label = { Text("New Owner Name / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle != null && newOwnerName.isNotBlank()) {
                        viewModel.transferVehicleOwnership(selectedVehicle!!.id, newOwnerName)
                        Toast.makeText(context, "Ownership transferred to $newOwnerName!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
