package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.drivecare.app.data.model.GeofenceZone
import com.drivecare.app.data.model.TripLog
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.model.VehicleTelemetry
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.FeatureFlags
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackingScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val tripLogs by viewModel.tripLogs.collectAsState()
    val geofences by viewModel.geofenceZones.collectAsState()
    val telemetryList by viewModel.recentTelemetry.collectAsState()

    val isGpsEnabled by FeatureFlags.gpsTrackingEnabled.collectAsState()
    val isGeofenceEnabled by FeatureFlags.geofencingEnabled.collectAsState()
    val isTelemetryEnabled by FeatureFlags.telemetryEnabled.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Live GPS & Telemetry, 1: Trip History, 2: Geofencing
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull()) }
    var showAddTripDialog by remember { mutableStateOf(false) }
    var showAddGeofenceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vehicles) {
        if (selectedVehicle == null && vehicles.isNotEmpty()) {
            selectedVehicle = vehicles.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Status Banner & Settings Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("GPS & Telemetry Module", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = isGpsEnabled,
                        onCheckedChange = { FeatureFlags.setGpsTrackingEnabled(context, it) }
                    )
                }
                if (!isGpsEnabled) {
                    Text(
                        "GPS Tracking Module is currently disabled in Feature Flags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (!isGpsEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Enable GPS Tracking in settings or the toggle above to view vehicle positions.")
            }
        } else {
            // Vehicle Picker Bar
            if (vehicles.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = vehicles.indexOf(selectedVehicle).coerceAtLeast(0),
                    edgePadding = 0.dp
                ) {
                    vehicles.forEach { v ->
                        Tab(
                            selected = selectedVehicle?.id == v.id,
                            onClick = { selectedVehicle = v },
                            text = { Text(v.vehicleName, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // Sub Navigation Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Status") },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Trip History") },
                    icon = { Icon(Icons.Default.Route, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Geofencing") },
                    icon = { Icon(Icons.Default.Fence, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Live GPS Map Visualizer & Telemetry Diagnostics
                    val activeVehicle = selectedVehicle
                    val latestTelem = telemetryList.find { it.vehicleId == activeVehicle?.id }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Simulated Satellite Map Tracker
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = activeVehicle?.vehicleName ?: "Selected Vehicle",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Live GPS: 37.7749° N, 122.4194° W (Signal Strong)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (activeVehicle != null) {
                                                    viewModel.addVehicleTelemetry(
                                                        VehicleTelemetry(
                                                            vehicleId = activeVehicle.id,
                                                            latitude = 37.7749 + (Math.random() * 0.01),
                                                            longitude = -122.4194 + (Math.random() * 0.01),
                                                            speedKmh = (40..90).random().toDouble(),
                                                            fuelLevelPct = (50..95).random().toDouble(),
                                                            batteryVoltage = 12.8,
                                                            engineTempC = 88.0
                                                        )
                                                    )
                                                    Toast.makeText(context, "GPS ping sent!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Simulate GPS Ping")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Real-Time Telemetry & Health", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = "Current Speed",
                                    value = "${latestTelem?.speedKmh?.toInt() ?: 0} km/h",
                                    icon = Icons.Default.Speed,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Fuel Level",
                                    value = "${latestTelem?.fuelLevelPct?.toInt() ?: 85}%",
                                    icon = Icons.Default.LocalGasStation,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = "Battery Voltage",
                                    value = "${latestTelem?.batteryVoltage ?: 12.6} V",
                                    icon = Icons.Default.ElectricCar,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Engine Temp",
                                    value = "${latestTelem?.engineTempC?.toInt() ?: 90} °C",
                                    icon = Icons.Default.Thermostat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Trip History Tab
                    val filteredTrips = tripLogs.filter { selectedVehicle == null || it.vehicleId == selectedVehicle?.id }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trips Logged (${filteredTrips.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddTripDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Trip")
                            }
                        }

                        if (filteredTrips.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Trips Recorded", fontWeight = FontWeight.SemiBold)
                                    Text("Log trips to track route history, distance & driver activity.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredTrips) { trip ->
                                    TripCard(trip = trip, onDelete = { viewModel.deleteTripLog(trip) })
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Geofencing Tab
                    val filteredGeofences = geofences.filter { selectedVehicle == null || it.vehicleId == selectedVehicle?.id }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Safe Perimeter Zones (${filteredGeofences.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddGeofenceDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Geofence")
                            }
                        }

                        if (filteredGeofences.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Fence, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Geofence Zones Configured", fontWeight = FontWeight.SemiBold)
                                    Text("Create boundary alerts for Home, Garage, Work, or Valet zones.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredGeofences) { zone ->
                                    GeofenceCard(zone = zone, onDelete = { viewModel.deleteGeofenceZone(zone) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTripDialog) {
        AddTripDialog(
            viewModel = viewModel,
            selectedVehicle = selectedVehicle,
            onDismiss = { showAddTripDialog = false }
        )
    }

    if (showAddGeofenceDialog) {
        AddGeofenceDialog(
            viewModel = viewModel,
            selectedVehicle = selectedVehicle,
            onDismiss = { showAddGeofenceDialog = false }
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun TripCard(trip: TripLog, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${trip.startLocation} ➔ ${trip.endLocation}", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${trip.vehicleName} • Driver: ${trip.driverName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${trip.distanceKm} km (${trip.durationMinutes} min)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun GeofenceCard(zone: GeofenceZone, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                    Icon(Icons.Default.Fence, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
                Column {
                    Text(zone.zoneName, fontWeight = FontWeight.Bold)
                    Text("Radius: ${zone.radiusMeters.toInt()}m • Entry/Exit Alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddTripDialog(
    viewModel: DriveCareViewModel,
    selectedVehicle: Vehicle?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var startLoc by remember { mutableStateOf("Home Base") }
    var endLoc by remember { mutableStateOf("Downtown Office") }
    var distanceStr by remember { mutableStateOf("24.5") }
    var durationStr by remember { mutableStateOf("35") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Trip History", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startLoc, onValueChange = { startLoc = it }, label = { Text("Start Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endLoc, onValueChange = { endLoc = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = distanceStr, onValueChange = { distanceStr = it }, label = { Text("Distance (km)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = durationStr, onValueChange = { durationStr = it }, label = { Text("Duration (mins)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle != null && startLoc.isNotBlank() && endLoc.isNotBlank()) {
                        viewModel.addTripLog(
                            TripLog(
                                vehicleId = selectedVehicle.id,
                                vehicleName = selectedVehicle.vehicleName,
                                startLocation = startLoc,
                                endLocation = endLoc,
                                distanceKm = distanceStr.toDoubleOrNull() ?: 10.0,
                                durationMinutes = durationStr.toIntOrNull() ?: 20,
                                tripDate = "2026-07-23"
                            )
                        )
                        Toast.makeText(context, "Trip saved!", Toast.LENGTH_SHORT).show()
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

@Composable
fun AddGeofenceDialog(
    viewModel: DriveCareViewModel,
    selectedVehicle: Vehicle?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoneName by remember { mutableStateOf("Home Perimeter") }
    var radiusStr by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Geofence Zone", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = zoneName, onValueChange = { zoneName = it }, label = { Text("Zone Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = radiusStr, onValueChange = { radiusStr = it }, label = { Text("Radius (Meters)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle != null && zoneName.isNotBlank()) {
                        viewModel.addGeofenceZone(
                            GeofenceZone(
                                vehicleId = selectedVehicle.id,
                                zoneName = zoneName,
                                centerLatitude = 37.7749,
                                centerLongitude = -122.4194,
                                radiusMeters = radiusStr.toDoubleOrNull() ?: 500.0
                            )
                        )
                        Toast.makeText(context, "Geofence zone added!", Toast.LENGTH_SHORT).show()
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
