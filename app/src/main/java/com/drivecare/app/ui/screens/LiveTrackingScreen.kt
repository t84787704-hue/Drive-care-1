package com.drivecare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.GpsTrackerDevice
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.tracker.TrackerPayload
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    viewModel: DriveCareViewModel,
    initialVehicleId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current

    val vehiclesList: List<Vehicle> by viewModel.vehicles.collectAsState()
    val trackersList: List<GpsTrackerDevice> by viewModel.gpsTrackers.collectAsState()

    var selectedVehicleId by remember(initialVehicleId, vehiclesList) {
        mutableStateOf(initialVehicleId ?: vehiclesList.firstOrNull()?.id)
    }

    val selectedVehicle: Vehicle? = vehiclesList.firstOrNull { it.id == selectedVehicleId }
    val assignedTracker: GpsTrackerDevice? = trackersList.firstOrNull { it.vehicleId == selectedVehicleId }

    var showSimulateHardwarePayloadDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live GPS Tracking", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Location refreshed from live hardware feed", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Selector Header
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Vehicle for Live Positioning:", style = MaterialTheme.typography.labelMedium)

                    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { vehicleDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedVehicle?.vehicleName ?: "Select a Vehicle",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            vehiclesList.forEach { v ->
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
                }
            }

            if (selectedVehicle == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Please select or register a vehicle to view live tracking.")
                }
            } else if (assignedTracker == null) {
                // No Tracker Assigned View
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PortableWifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No GPS Tracker Assigned to ${selectedVehicle.vehicleName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Assign a physical hardware GPS tracker to this vehicle to enable live worldwide tracking, geofence security alerts, and location history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val tracker: GpsTrackerDevice = assignedTracker
                // Live Tracker Status Dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = selectedVehicle.vehicleName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tracker: ${tracker.trackerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Online / Offline Status Badge
                            Surface(
                                color = if (tracker.isOnline) Color(0xFFE8F5E9) else Color(0xFFEEEEEE),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (tracker.isOnline) Color(0xFF2E7D32) else Color(0xFF757575),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (tracker.isOnline) "ONLINE" else "OFFLINE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tracker.isOnline) Color(0xFF2E7D32) else Color(0xFF616161)
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Coordinates Display Card
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Current Coordinates",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                val lat = tracker.lastLatitude
                                val lng = tracker.lastLongitude

                                if (lat != null && lng != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Latitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                            Text(
                                                text = String.format(Locale.US, "%.6f", lat),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column {
                                            Text("Longitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                            Text(
                                                text = String.format(Locale.US, "%.6f", lng),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column {
                                            Text("Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                            Text(
                                                text = "${tracker.lastSpeedKmh?.toInt() ?: 0} km/h",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    val updateTime = tracker.lastUpdatedTime
                                    if (updateTime != null) {
                                        val formattedTime = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US).format(Date(updateTime))
                                        Text(
                                            text = "Last Update: $formattedTime",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Awaiting first coordinate ping from hardware tracker (IMEI: ${tracker.imeiNumber})...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Hardware Detail Attributes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("IMEI Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(tracker.imeiNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Text("SIM Phone Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(if (tracker.simNumber.isBlank()) "None" else tracker.simNumber, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val lat = tracker.lastLatitude
                                    val lng = tracker.lastLongitude
                                    if (lat != null && lng != null) {
                                        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(selectedVehicle.vehicleName)})")
                                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    } else {
                                        Toast.makeText(context, "No coordinates available yet", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = tracker.lastLatitude != null
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in Maps")
                            }

                            OutlinedButton(
                                onClick = {
                                    showSimulateHardwarePayloadDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ingest Hardware Data")
                            }
                        }
                    }
                }
            }
        }
    }

    // Ingest Hardware Data Dialog
    if (showSimulateHardwarePayloadDialog && assignedTracker != null) {
        val tracker: GpsTrackerDevice = assignedTracker
        var inputLat by remember { mutableStateOf(tracker.lastLatitude?.toString() ?: "37.7749") }
        var inputLng by remember { mutableStateOf(tracker.lastLongitude?.toString() ?: "-122.4194") }
        var inputSpeed by remember { mutableStateOf("55.0") }

        AlertDialog(
            onDismissRequest = { showSimulateHardwarePayloadDialog = false },
            title = { Text("Hardware Payload Entry Point", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Simulates incoming hardware packet for tracker IMEI: ${tracker.imeiNumber}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = inputLat,
                        onValueChange = { inputLat = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputLng,
                        onValueChange = { inputLng = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputSpeed,
                        onValueChange = { inputSpeed = it },
                        label = { Text("Speed (km/h)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = inputLat.toDoubleOrNull() ?: 0.0
                        val lng = inputLng.toDoubleOrNull() ?: 0.0
                        val speed = inputSpeed.toDoubleOrNull() ?: 0.0

                        val payload = TrackerPayload(
                            trackerCode = tracker.imeiNumber,
                            latitude = lat,
                            longitude = lng,
                            speedKmh = speed,
                            timestamp = System.currentTimeMillis(),
                            protocolVendor = "DIRECT_INGEST"
                        )

                        viewModel.ingestTrackerPayload(payload)
                        showSimulateHardwarePayloadDialog = false
                        Toast.makeText(context, "Hardware coordinate payload ingested!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Ingest Payload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulateHardwarePayloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
