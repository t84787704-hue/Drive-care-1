package com.drivecare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.GpsTrackerDevice
import com.drivecare.app.data.model.TrackerLocationPoint
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    viewModel: DriveCareViewModel,
    initialVehicleId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val vehiclesList: List<Vehicle> by viewModel.vehicles.collectAsState()
    val trackersList: List<GpsTrackerDevice> by viewModel.gpsTrackers.collectAsState()
    val allLocations: List<TrackerLocationPoint> by viewModel.trackerLocations.collectAsState()

    var selectedVehicleId by remember(initialVehicleId) { mutableStateOf(initialVehicleId) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredLocations: List<TrackerLocationPoint> = remember(allLocations, selectedVehicleId) {
        val targetVehicleId = selectedVehicleId
        if (targetVehicleId == null) {
            allLocations
        } else {
            allLocations.filter { it.vehicleId == targetVehicleId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Location History", fontWeight = FontWeight.Bold) },
                actions = {
                    if (filteredLocations.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Bar
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Filter:", style = MaterialTheme.typography.labelLarge)

                    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        val currentVehicle = vehiclesList.firstOrNull { it.id == selectedVehicleId }
                        OutlinedButton(
                            onClick = { vehicleDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentVehicle?.vehicleName ?: "All Vehicles & Trackers")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Vehicles & Trackers") },
                                onClick = {
                                    selectedVehicleId = null
                                    vehicleDropdownExpanded = false
                                }
                            )
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

            if (filteredLocations.isEmpty()) {
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
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No Location History Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Location history points will automatically populate as physical GPS hardware pings coordinates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "${filteredLocations.size} location point(s) recorded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredLocations, key = { locPoint -> locPoint.id }) { loc ->
                        val vehicle = vehiclesList.firstOrNull { it.id == loc.vehicleId }
                        val tracker = trackersList.firstOrNull { it.trackerId == loc.trackerId }
                        LocationHistoryCard(
                            point = loc,
                            vehicle = vehicle,
                            tracker = tracker
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Location History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear location history logs?") },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTracker = trackersList.firstOrNull { it.vehicleId == selectedVehicleId }
                        if (selectedTracker != null) {
                            viewModel.deleteLocationHistoryForTracker(selectedTracker.trackerId)
                        }
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Location history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LocationHistoryCard(
    point: TrackerLocationPoint,
    vehicle: Vehicle?,
    tracker: GpsTrackerDevice?
) {
    val context = LocalContext.current
    val formattedTime = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US).format(Date(point.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = vehicle?.vehicleName ?: "Tracker: ${tracker?.trackerName ?: point.trackerId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Lat: ${String.format(Locale.US, "%.5f", point.latitude)}, Lng: ${String.format(Locale.US, "%.5f", point.longitude)} • ${point.speedKmh.toInt()} km/h",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = {
                    val geoUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, geoUri)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://maps.google.com/?q=${point.latitude},${point.longitude}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                }
            ) {
                Icon(Icons.Default.Map, contentDescription = "View on Map", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
