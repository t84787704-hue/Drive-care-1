package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.GpsTrackerDevice
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.tracker.TrackerIngestionService
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackerManagementScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onOpenLiveTracking: (Long?) -> Unit = {},
    onOpenLocationHistory: (Long?) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lang = LocalAppLanguage.current

    val trackersList: List<GpsTrackerDevice> by viewModel.gpsTrackers.collectAsState()
    val vehiclesList: List<Vehicle> by viewModel.vehicles.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTracker by remember { mutableStateOf<GpsTrackerDevice?>(null) }
    var deletingTracker by remember { mutableStateOf<GpsTrackerDevice?>(null) }
    var showProtocolInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Hardware Trackers", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tracker")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Tracker") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header summary banner
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardware GPS Trackers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val onlineCount = trackersList.filter { it.isOnline }.size
                        Text(
                            text = "${trackersList.size} registered tracker(s) • $onlineCount online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    OutlinedButton(
                        onClick = { showProtocolInfoDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("API Guide")
                    }
                }
            }

            if (trackersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No GPS Trackers Registered",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add physical hardware GPS trackers (IMEI/SIM) and assign them to your vehicles for live worldwide positioning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Register First Tracker")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(trackersList, key = { trackerItem -> trackerItem.id }) { tracker ->
                        val assignedVehicle = vehiclesList.firstOrNull { it.id == tracker.vehicleId }
                        GpsTrackerCard(
                            tracker = tracker,
                            assignedVehicle = assignedVehicle,
                            onEdit = { editingTracker = tracker },
                            onDelete = { deletingTracker = tracker },
                            onOpenLiveTracking = { onOpenLiveTracking(tracker.vehicleId) },
                            onOpenLocationHistory = { onOpenLocationHistory(tracker.vehicleId) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Tracker Dialog
    if (showAddDialog || editingTracker != null) {
        TrackerEditDialog(
            initialTracker = editingTracker,
            vehicles = vehiclesList,
            onDismiss = {
                showAddDialog = false
                editingTracker = null
            },
            onSave = { tracker ->
                viewModel.saveGpsTracker(tracker)
                showAddDialog = false
                editingTracker = null
                Toast.makeText(context, "GPS Tracker saved successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingTracker != null) {
        val trackerToDelete = deletingTracker!!
        AlertDialog(
            onDismissRequest = { deletingTracker = null },
            title = { Text("Delete GPS Tracker", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete tracker '${trackerToDelete.trackerName}' (IMEI: ${trackerToDelete.imeiNumber})? History and coordinates for this tracker will also be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGpsTracker(trackerToDelete)
                        deletingTracker = null
                        Toast.makeText(context, "GPS Tracker removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTracker = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // API & Payload Documentation Dialog
    if (showProtocolInfoDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Hardware Integration API Guide", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DriveCare Generic GPS Tracker Framework",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DriveCare supports any standard TCP/UDP hardware device (Teltonika, Concox, SinoTrack, Coban, GT06, JT808) via JSON or NMEA string ingestion.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text("Sample JSON Ingestion Payload:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val sampleJson = """
                                {
                                  "imei": "864123045678901",
                                  "latitude": 37.774929,
                                  "longitude": -122.419416,
                                  "speed_kmh": 65.4,
                                  "heading": 180.0,
                                  "altitude": 15.0,
                                  "ignition": true,
                                  "timestamp": ${System.currentTimeMillis()}
                                }
                            """.trimIndent()
                            Text(
                                text = sampleJson,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sampleJson))
                                    Toast.makeText(context, "Sample JSON copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy JSON")
                            }
                        }
                    }

                    Text("Sample NMEA GPRMC Ingestion Payload:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val sampleNmea = "\$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230326,003.1,W*6A"
                            Text(
                                text = sampleNmea,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showProtocolInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GpsTrackerCard(
    tracker: GpsTrackerDevice,
    assignedVehicle: Vehicle?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenLiveTracking: () -> Unit,
    onOpenLocationHistory: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (tracker.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = tracker.trackerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("IMEI Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = tracker.imeiNumber,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("SIM Phone Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = if (tracker.simNumber.isBlank()) "None" else tracker.simNumber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Assigned Vehicle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = assignedVehicle?.vehicleName ?: "Unassigned",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (assignedVehicle != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Contact", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    val lastUpdate = tracker.lastUpdatedTime
                    Text(
                        text = if (lastUpdate != null) dateFormat.format(Date(lastUpdate)) else "Never",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (tracker.lastLatitude != null && tracker.lastLongitude != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Lat: ${String.format(Locale.US, "%.5f", tracker.lastLatitude)}, Lng: ${String.format(Locale.US, "%.5f", tracker.lastLongitude)}",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        val speed = tracker.lastSpeedKmh
                        if (speed != null) {
                            Text(
                                text = "${speed.toInt()} km/h",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenLiveTracking,
                    modifier = Modifier.weight(1f),
                    enabled = tracker.vehicleId != null
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live Map", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onOpenLocationHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TrackerEditDialog(
    initialTracker: GpsTrackerDevice?,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (GpsTrackerDevice) -> Unit
) {
    var trackerName by remember { mutableStateOf(initialTracker?.trackerName ?: "") }
    var trackerCode by remember { mutableStateOf(initialTracker?.trackerId ?: "") }
    var imeiNumber by remember { mutableStateOf(initialTracker?.imeiNumber ?: "") }
    var simNumber by remember { mutableStateOf(initialTracker?.simNumber ?: "") }
    var selectedVehicleId by remember { mutableStateOf(initialTracker?.vehicleId) }
    var notes by remember { mutableStateOf(initialTracker?.notes ?: "") }

    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTracker == null) "Register New GPS Tracker" else "Edit GPS Tracker", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = trackerName,
                    onValueChange = { trackerName = it },
                    label = { Text("Tracker Name *") },
                    placeholder = { Text("e.g. SinoTrack ST-901, Concox GT06") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = imeiNumber,
                    onValueChange = { imeiNumber = it },
                    label = { Text("Hardware IMEI Number *") },
                    placeholder = { Text("15-digit IMEI number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = simNumber,
                    onValueChange = { simNumber = it },
                    label = { Text("SIM Card Phone Number") },
                    placeholder = { Text("+1 555-0192") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Assign to Vehicle:", style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
                    OutlinedButton(
                        onClick = { vehicleDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentVehicle?.vehicleName ?: "Unassigned")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unassigned") },
                            onClick = {
                                selectedVehicleId = null
                                vehicleDropdownExpanded = false
                            }
                        )
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.vehicleName) },
                                onClick = {
                                    selectedVehicleId = v.id
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Installation Info") },
                    placeholder = { Text("e.g. Installed under dash, fuse box wired") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trackerName.isBlank() || imeiNumber.isBlank()) return@Button
                    val code = if (trackerCode.isBlank()) imeiNumber else trackerCode
                    val baseTracker = initialTracker ?: GpsTrackerDevice(
                        trackerName = trackerName,
                        trackerId = code,
                        imeiNumber = imeiNumber,
                        createdDate = dateFormat.format(Date())
                    )
                    val tracker = baseTracker.copy(
                        trackerName = trackerName,
                        trackerId = code,
                        imeiNumber = imeiNumber,
                        simNumber = simNumber,
                        vehicleId = selectedVehicleId,
                        notes = notes
                    )
                    onSave(tracker)
                },
                enabled = trackerName.isNotBlank() && imeiNumber.isNotBlank()
            ) {
                Text("Save Tracker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
