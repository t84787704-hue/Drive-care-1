package com.drivecare.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drivecare.app.data.model.GeofenceZone
import com.drivecare.app.data.model.TripLog
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.model.VehicleTelemetry
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.components.GeofenceStatusLevel
import com.drivecare.app.ui.components.PermissionOnboardingDialog
import com.drivecare.app.ui.components.checkGeofencePermissionStatus
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.FeatureFlags
import com.drivecare.app.utils.LocalAppLanguage
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiscoveredObdDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean = false
)

fun isLocationServiceEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return false
    return try {
        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackingScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    val vehicles by viewModel.vehicles.collectAsState()
    val tripLogs by viewModel.tripLogs.collectAsState()
    val geofences by viewModel.geofenceZones.collectAsState()
    val telemetryList by viewModel.recentTelemetry.collectAsState()

    val isGpsEnabled by FeatureFlags.gpsTrackingEnabled.collectAsState()

    var isGpsProviderOn by remember { mutableStateOf(isLocationServiceEnabled(context)) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live GPS & Telemetry, 1: Trip History, 2: Geofencing
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull()) }
    var showAddTripDialog by remember { mutableStateOf(false) }
    var showAddGeofenceDialog by remember { mutableStateOf(false) }
    var geofenceToEdit by remember { mutableStateOf<GeofenceZone?>(null) }
    var geofenceToDelete by remember { mutableStateOf<GeofenceZone?>(null) }
    var showPermissionOnboardingDialog by remember { mutableStateOf(false) }
    var showManualTelemetryDialog by remember { mutableStateOf(false) }
    var showObdScannerDialog by remember { mutableStateOf(false) }

    var geofencePermissionStatus by remember { mutableStateOf(checkGeofencePermissionStatus(context)) }

    // OBD2 Connection State (Optional Mode - Manual remains default)
    var isObdConnected by remember { mutableStateOf(false) }
    var obdDeviceName by remember { mutableStateOf<String?>(null) }
    var obdFuelLevel by remember { mutableDoubleStateOf(84.0) }
    var obdBatteryVoltage by remember { mutableDoubleStateOf(13.8) }
    var obdEngineTemp by remember { mutableDoubleStateOf(88.0) }
    var obdRpm by remember { mutableIntStateOf(1950) }

    // Speed unit toggle: KM/H vs MPH
    var useMiles by remember { mutableStateOf(false) }

    // Runtime Location Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                geofencePermissionStatus = checkGeofencePermissionStatus(context)
                isGpsProviderOn = isLocationServiceEnabled(context)
                hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasLocationPermission = granted
        if (!granted) {
            Toast.makeText(context, "Location permission is required for real GPS tracking.", Toast.LENGTH_LONG).show()
        }
    }

    // Runtime Bluetooth Permission state for OBD2
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasBluetoothPermission = granted
        if (granted) {
            showObdScannerDialog = true
        } else {
            Toast.makeText(context, "Bluetooth permissions are required to connect to an OBD2 scanner.", Toast.LENGTH_LONG).show()
        }
    }

    // Runtime Background Location Permission state for Geofence Monitoring (Android 10+)
    var hasBackgroundLocationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "Background location granted for geofence boundary alerts!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Background location denied. Geofence alerts will trigger when app is active.", Toast.LENGTH_LONG).show()
        }
    }

    // Real Live GPS state
    var currentLatitude by remember { mutableDoubleStateOf(0.0) }
    var currentLongitude by remember { mutableDoubleStateOf(0.0) }
    var currentSpeedKmh by remember { mutableDoubleStateOf(0.0) }
    var currentAddress by remember { mutableStateOf("Acquiring GPS fix...") }
    var isLiveTrackingActive by remember { mutableStateOf(true) }

    // Active Trip Tracking State
    var isTripActive by remember { mutableStateOf(false) }
    var tripStartLat by remember { mutableDoubleStateOf(0.0) }
    var tripStartLng by remember { mutableDoubleStateOf(0.0) }
    var tripStartAddress by remember { mutableStateOf("") }
    var tripStartTimeMillis by remember { mutableLongStateOf(0L) }
    var tripDistanceMeters by remember { mutableDoubleStateOf(0.0) }
    var tripMaxSpeedKmh by remember { mutableDoubleStateOf(0.0) }
    var tripLastLocation by remember { mutableStateOf<Location?>(null) }

    // Manual Telemetry values for selected vehicle
    var manualFuelLevel by remember { mutableDoubleStateOf(85.0) }
    var manualBatteryVoltage by remember { mutableDoubleStateOf(12.6) }
    var manualEngineTemp by remember { mutableDoubleStateOf(90.0) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(vehicles) {
        if (selectedVehicle == null && vehicles.isNotEmpty()) {
            selectedVehicle = vehicles.first()
        }
    }

    // Battery Optimization & Clean Location Lifecycle
    DisposableEffect(hasLocationPermission, isLiveTrackingActive, isGpsProviderOn) {
        if (hasLocationPermission && isLiveTrackingActive && isGpsProviderOn) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            ).setMinUpdateIntervalMillis(1000L).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).toDouble() else 0.0
                    currentSpeedKmh = speedKmh

                    // Geocode address asynchronously
                    coroutineScope.launch {
                        currentAddress = fetchAddressString(context, location.latitude, location.longitude)
                    }

                    // Active Trip Distance Accumulation
                    if (isTripActive) {
                        val lastLoc = tripLastLocation
                        if (lastLoc != null) {
                            val dist = lastLoc.distanceTo(location).toDouble()
                            if (dist > 0.5) { // Filter minor noise
                                tripDistanceMeters += dist
                            }
                        }
                        tripLastLocation = location
                        if (speedKmh > tripMaxSpeedKmh) {
                            tripMaxSpeedKmh = speedKmh
                        }

                        // Persist telemetry record periodically during trip
                        selectedVehicle?.let { v ->
                            val activeFuel = if (isObdConnected) obdFuelLevel else manualFuelLevel
                            val activeVoltage = if (isObdConnected) obdBatteryVoltage else manualBatteryVoltage
                            val activeTemp = if (isObdConnected) obdEngineTemp else manualEngineTemp

                            viewModel.addVehicleTelemetry(
                                VehicleTelemetry(
                                    vehicleId = v.id,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    speedKmh = speedKmh,
                                    fuelLevelPct = activeFuel,
                                    batteryVoltage = activeVoltage,
                                    engineTempC = activeTemp
                                )
                            )
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                hasLocationPermission = false
            }

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose { }
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
                        Icon(
                            imageVector = if (isGpsProviderOn) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = if (isGpsProviderOn) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text("GPS & Telemetry Module", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Surface(
                                    color = if (isGpsProviderOn) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = if (isGpsProviderOn) "GPS: ON" else "GPS: OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGpsProviderOn) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // GPS OFF Warning Banner (One-Tap Enable GPS)
        if (isGpsEnabled && !isGpsProviderOn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "⚠️ Location Services are OFF",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        "Turn on GPS to use Live Tracking, Trip History and Geofencing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening Location Settings...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enable GPS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (!isGpsEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Enable GPS Tracking in settings or the toggle above to view vehicle positions.")
            }
        } else if (vehicles.isEmpty()) {
            // Clean empty state when user has no vehicles registered yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("No Vehicles Registered", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Please add a vehicle first in the Vehicle Garage to use GPS tracking, trip logging, and telemetry features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Vehicle Picker Bar
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
                    // Live Real GPS Visualizer & Telemetry Diagnostics
                    val activeVehicle = selectedVehicle

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Permission Banner if not granted
                        if (!hasLocationPermission) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Text("Location Permission Required", fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            "Real GPS tracking and trip distance calculations require device location permission.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    locationPermissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                                        )
                                                    )
                                                }
                                            ) {
                                                Text("Grant Permission")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.fromParts("package", context.packageName, null)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            ) {
                                                Text("App Settings")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // OBD2 Connection Badge if Connected
                        if (isObdConnected) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                            Column {
                                                Text("OBD2 Scanner Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                                Text("Connected: ${obdDeviceName ?: "Bluetooth OBD2"}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        TextButton(onClick = {
                                            isObdConnected = false
                                            obdDeviceName = null
                                            Toast.makeText(context, "Disconnected OBD2 Scanner. Reverted to Manual Telemetry.", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("Disconnect")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Real GPS Satellite Location Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = activeVehicle?.vehicleName ?: "Selected Vehicle",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        FilterChip(
                                            selected = useMiles,
                                            onClick = { useMiles = !useMiles },
                                            label = { Text(if (useMiles) "Speed: MPH" else "Speed: KM/H") }
                                        )
                                    }

                                    if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                                        Text(
                                            text = "Coordinates: ${String.format(Locale.US, "%.5f", currentLatitude)}° N, ${String.format(Locale.US, "%.5f", currentLongitude)}° E",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = currentAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = if (!isGpsProviderOn)
                                                "⚠️ Device Location (GPS) is turned OFF"
                                            else if (hasLocationPermission)
                                                "Acquiring real satellite GPS signal..."
                                            else
                                                "Location permission required",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Real Trip Start / Stop Control Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isTripActive) {
                                            Button(
                                                modifier = Modifier.weight(1f),
                                                enabled = hasLocationPermission && isGpsProviderOn && activeVehicle != null,
                                                onClick = {
                                                    if (!isGpsProviderOn) {
                                                        Toast.makeText(context, "Please enable Location Services (GPS) first.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    isTripActive = true
                                                    tripStartLat = currentLatitude
                                                    tripStartLng = currentLongitude
                                                    tripStartAddress = currentAddress.ifBlank { "Current Position" }
                                                    tripStartTimeMillis = System.currentTimeMillis()
                                                    tripDistanceMeters = 0.0
                                                    tripMaxSpeedKmh = currentSpeedKmh
                                                    tripLastLocation = null
                                                    Toast.makeText(context, "Real Trip Tracking Started!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Start Trip")
                                            }
                                        } else {
                                            Button(
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                onClick = {
                                                    isTripActive = false
                                                    val endAddress = currentAddress.ifBlank { "Destination Position" }
                                                    val endTimeMillis = System.currentTimeMillis()
                                                    val durationMinutes = ((endTimeMillis - tripStartTimeMillis) / 60000).toInt().coerceAtLeast(1)
                                                    val distanceKm = tripDistanceMeters / 1000.0
                                                    val avgSpeedKmh = if (durationMinutes > 0) (distanceKm / (durationMinutes / 60.0)) else 0.0

                                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                                                    if (activeVehicle != null) {
                                                        viewModel.addTripLog(
                                                            TripLog(
                                                                vehicleId = activeVehicle.id,
                                                                vehicleName = activeVehicle.vehicleName,
                                                                startLocation = tripStartAddress,
                                                                endLocation = endAddress,
                                                                distanceKm = distanceKm,
                                                                durationMinutes = durationMinutes,
                                                                avgSpeedKmh = avgSpeedKmh,
                                                                maxSpeedKmh = tripMaxSpeedKmh,
                                                                tripDate = dateFormat.format(Date(tripStartTimeMillis)),
                                                                startTime = timeFormat.format(Date(tripStartTimeMillis)),
                                                                endTime = timeFormat.format(Date(endTimeMillis))
                                                            )
                                                        )
                                                        Toast.makeText(context, "Trip Logged: ${String.format(Locale.US, "%.1f", distanceKm)} km in $durationMinutes mins", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Stop & Save Trip")
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { showManualTelemetryDialog = true }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Manual Inputs")
                                        }
                                    }

                                    // Secondary Button: OBD2 Scanner (Optional)
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
                                                bluetoothPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.BLUETOOTH_CONNECT,
                                                        Manifest.permission.BLUETOOTH_SCAN
                                                    )
                                                )
                                            } else {
                                                showObdScannerDialog = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isObdConnected) "OBD2 Connected (${obdDeviceName ?: "Active"})" else "Connect OBD2 Scanner (Optional)")
                                    }

                                    if (isTripActive) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceAround,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Trip Distance: ${String.format(Locale.US, "%.2f", tripDistanceMeters / 1000.0)} km",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Max Speed: ${String.format(Locale.US, "%.0f", if (useMiles) tripMaxSpeedKmh * 0.621371 else tripMaxSpeedKmh)} ${if (useMiles) "mph" else "km/h"}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Real-Time Telemetry & Diagnostics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            val displaySpeed = if (useMiles) currentSpeedKmh * 0.621371 else currentSpeedKmh
                            val speedUnit = if (useMiles) "mph" else "km/h"
                            val fuelVal = if (isObdConnected) obdFuelLevel else manualFuelLevel
                            val fuelLabel = if (isObdConnected) "Fuel Level (OBD2)" else "Fuel Level (Manual)"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = "Current Speed (GPS)",
                                    value = "${String.format(Locale.US, "%.0f", displaySpeed)} $speedUnit",
                                    icon = Icons.Default.Speed,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = fuelLabel,
                                    value = "${fuelVal.toInt()}%",
                                    icon = Icons.Default.LocalGasStation,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            val voltVal = if (isObdConnected) obdBatteryVoltage else manualBatteryVoltage
                            val tempVal = if (isObdConnected) obdEngineTemp else manualEngineTemp
                            val voltLabel = if (isObdConnected) "Battery (OBD2)" else "Battery Voltage"
                            val tempLabel = if (isObdConnected) "Engine Temp (OBD2)" else "Engine Temp"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = voltLabel,
                                    value = "${String.format(Locale.US, "%.1f", voltVal)} V",
                                    icon = Icons.Default.ElectricCar,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = tempLabel,
                                    value = "${tempVal.toInt()} °C",
                                    icon = Icons.Default.Thermostat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (isObdConnected) {
                            item {
                                MetricCard(
                                    title = "Engine Speed (OBD2)",
                                    value = "$obdRpm RPM",
                                    icon = Icons.Default.Speed,
                                    modifier = Modifier.fillMaxWidth()
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
                                Text("Manual Log")
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
                                    Text("Start a real trip or manually log trips to track route history & stats.", style = MaterialTheme.typography.bodySmall)
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

                    if (vehicles.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Vehicles Registered", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Please add a vehicle to your garage before configuring safe perimeter geofences.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        val permissionStatus = geofencePermissionStatus
                        val statusLevel = permissionStatus.statusLevel

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (statusLevel) {
                                            GeofenceStatusLevel.ENABLED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            GeofenceStatusLevel.PARTIALLY_ENABLED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            GeofenceStatusLevel.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { showPermissionOnboardingDialog = true },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (statusLevel) {
                                                        GeofenceStatusLevel.ENABLED -> Icons.Default.CheckCircle
                                                        GeofenceStatusLevel.PARTIALLY_ENABLED -> Icons.Default.Tune
                                                        GeofenceStatusLevel.DISABLED -> Icons.Default.Info
                                                    },
                                                    contentDescription = null,
                                                    tint = when (statusLevel) {
                                                        GeofenceStatusLevel.ENABLED -> MaterialTheme.colorScheme.primary
                                                        GeofenceStatusLevel.PARTIALLY_ENABLED -> MaterialTheme.colorScheme.secondary
                                                        GeofenceStatusLevel.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = when (statusLevel) {
                                                            GeofenceStatusLevel.ENABLED -> AppStrings.get("geofencing_enabled", lang)
                                                            else -> AppStrings.get("safe_zone_title", lang)
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = when (statusLevel) {
                                                            GeofenceStatusLevel.ENABLED -> AppStrings.get("geofence_all_met", lang)
                                                            else -> AppStrings.get("geofence_bg_desc", lang)
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = { showPermissionOnboardingDialog = true },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = when (statusLevel) {
                                                        GeofenceStatusLevel.ENABLED -> MaterialTheme.colorScheme.primary
                                                        GeofenceStatusLevel.PARTIALLY_ENABLED -> MaterialTheme.colorScheme.secondary
                                                        GeofenceStatusLevel.DISABLED -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = when (statusLevel) {
                                                        GeofenceStatusLevel.ENABLED -> AppStrings.get("status_label", lang)
                                                        else -> AppStrings.get("enable_background_alerts", lang)
                                                    },
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            StatusCheckChip(label = AppStrings.get("fine_location", lang), isOk = permissionStatus.hasFineLocation)
                                            StatusCheckChip(label = AppStrings.get("background_gps", lang), isOk = permissionStatus.hasBackgroundLocation)
                                            StatusCheckChip(label = AppStrings.get("exact_alarms", lang), isOk = permissionStatus.canScheduleExactAlarms)
                                            StatusCheckChip(label = AppStrings.get("unrestricted_battery", lang), isOk = permissionStatus.isIgnoringBatteryOptimizations)
                                        }

                                        if (!permissionStatus.isIgnoringBatteryOptimizations) {
                                            Text(
                                                text = "• ${AppStrings.get("alerts_delayed_note", lang)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Safe Perimeter Zones (${filteredGeofences.size})", fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            if (!isGpsProviderOn) {
                                                Toast.makeText(context, "Please turn on Device Location (GPS) to configure Geofence zones.", Toast.LENGTH_LONG).show()
                                            } else {
                                                showAddGeofenceDialog = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Geofence")
                                    }
                                }
                            }

                            if (filteredGeofences.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.Fence, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No Geofence Zones Configured", fontWeight = FontWeight.SemiBold)
                                            Text("Set up real safe perimeter boundaries for Home, Garage, Work, or Valet zones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            } else {
                                items(filteredGeofences) { zone ->
                                    val vName = vehicles.find { it.id == zone.vehicleId }?.vehicleName
                                    GeofenceCard(
                                        zone = zone,
                                        vehicleName = vName,
                                        onEdit = { geofenceToEdit = zone },
                                        onDelete = { geofenceToDelete = zone }
                                    )
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
            currentLat = currentLatitude,
            currentLng = currentLongitude,
            onDismiss = { showAddGeofenceDialog = false }
        )
    }

    geofenceToEdit?.let { zone ->
        EditGeofenceDialog(
            viewModel = viewModel,
            geofence = zone,
            onDismiss = { geofenceToEdit = null }
        )
    }

    geofenceToDelete?.let { zone ->
        AlertDialog(
            onDismissRequest = { geofenceToDelete = null },
            title = { Text("Delete Geofence Zone?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${zone.zoneName}'? Perimeter monitoring for this zone will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGeofenceZone(zone)
                        geofenceToDelete = null
                        Toast.makeText(context, "Geofence '${zone.zoneName}' deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { geofenceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPermissionOnboardingDialog) {
        PermissionOnboardingDialog(
            onDismiss = {
                showPermissionOnboardingDialog = false
                geofencePermissionStatus = checkGeofencePermissionStatus(context)
            }
        )
    }

    if (showManualTelemetryDialog) {
        AlertDialog(
            onDismissRequest = { showManualTelemetryDialog = false },
            title = { Text("Manual Telemetry Inputs", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Adjust vehicle parameters that cannot be measured directly by device GPS.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = manualFuelLevel.toString(),
                        onValueChange = { manualFuelLevel = it.toDoubleOrNull() ?: manualFuelLevel },
                        label = { Text("Fuel Level (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualBatteryVoltage.toString(),
                        onValueChange = { manualBatteryVoltage = it.toDoubleOrNull() ?: manualBatteryVoltage },
                        label = { Text("Battery Voltage (V)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualEngineTemp.toString(),
                        onValueChange = { manualEngineTemp = it.toDoubleOrNull() ?: manualEngineTemp },
                        label = { Text("Engine Temp (°C)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedVehicle?.let { v ->
                            viewModel.addVehicleTelemetry(
                                VehicleTelemetry(
                                    vehicleId = v.id,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude,
                                    speedKmh = currentSpeedKmh,
                                    fuelLevelPct = manualFuelLevel,
                                    batteryVoltage = manualBatteryVoltage,
                                    engineTempC = manualEngineTemp
                                )
                            )
                        }
                        showManualTelemetryDialog = false
                        Toast.makeText(context, "Telemetry updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Telemetry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTelemetryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showObdScannerDialog) {
        ObdScannerDialog(
            context = context,
            onDeviceSelected = { device ->
                isObdConnected = true
                obdDeviceName = device.name
                showObdScannerDialog = false
                Toast.makeText(context, "Connected to OBD2 Scanner (${device.name})", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showObdScannerDialog = false }
        )
    }
}

@Composable
fun ObdScannerDialog(
    context: Context,
    onDeviceSelected: (DiscoveredObdDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleDevices = remember {
        listOf(
            DiscoveredObdDevice("OBDII ELM327 v2.1", "00:1D:A5:68:98:8B", isPaired = true),
            DiscoveredObdDevice("V-Gate iCar Pro BT4.0", "11:22:33:44:55:66", isPaired = true),
            DiscoveredObdDevice("VEEPEAK OBDCheck BLE", "AA:BB:CC:DD:EE:FF", isPaired = false),
            DiscoveredObdDevice("Car Diagnostic Scanner", "99:88:77:66:55:44", isPaired = false)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Select OBD2 Scanner", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a paired or nearby Bluetooth OBD2 device to stream live engine telemetry.", style = MaterialTheme.typography.bodySmall)
                Divider()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 240.dp)) {
                    items(sampleDevices) { dev ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(dev) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(dev.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (dev.isPaired) {
                                    SuggestionChip(onClick = { onDeviceSelected(dev) }, label = { Text("Paired") })
                                } else {
                                    OutlinedButton(onClick = { onDeviceSelected(dev) }) { Text("Connect") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private suspend fun fetchAddressString(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val a = addresses[0]
                a.getAddressLine(0) ?: "${a.locality ?: "Unknown City"}, ${a.countryName ?: ""}"
            } else {
                "Lat: ${String.format(Locale.US, "%.5f", lat)}, Lng: ${String.format(Locale.US, "%.5f", lng)}"
            }
        } catch (e: Exception) {
            "Lat: ${String.format(Locale.US, "%.5f", lat)}, Lng: ${String.format(Locale.US, "%.5f", lng)}"
        }
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${trip.startLocation} ➔ ${trip.endLocation}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
                Text("${trip.vehicleName} • ${trip.tripDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${String.format(Locale.US, "%.1f", trip.distanceKm)} km (${trip.durationMinutes} min)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GeofenceCard(
    zone: GeofenceZone,
    vehicleName: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Fence,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(zone.zoneName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "${zone.radiusMeters.toInt()}m Radius",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!vehicleName.isNullOrBlank()) {
                        Text(
                            "Vehicle: $vehicleName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        "Center: ${String.format(Locale.US, "%.4f", zone.centerLatitude)}, ${String.format(Locale.US, "%.4f", zone.centerLongitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Entry: ${if (zone.notifyOnEnter) "Enabled" else "Disabled"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (zone.notifyOnEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Exit: ${if (zone.notifyOnExit) "Enabled" else "Disabled"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (zone.notifyOnExit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Geofence", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Geofence", tint = MaterialTheme.colorScheme.error)
                }
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
    var startLoc by remember { mutableStateOf("Start Location") }
    var endLoc by remember { mutableStateOf("Destination") }
    var distanceStr by remember { mutableStateOf("10.0") }
    var durationStr by remember { mutableStateOf(20) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Trip Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startLoc, onValueChange = { startLoc = it }, label = { Text("Start Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endLoc, onValueChange = { endLoc = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = distanceStr, onValueChange = { distanceStr = it }, label = { Text("Distance (km)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = durationStr.toString(), onValueChange = { durationStr = it.toIntOrNull() ?: 0 }, label = { Text("Duration (mins)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle != null && startLoc.isNotBlank() && endLoc.isNotBlank()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.addTripLog(
                            TripLog(
                                vehicleId = selectedVehicle.id,
                                vehicleName = selectedVehicle.vehicleName,
                                startLocation = startLoc,
                                endLocation = endLoc,
                                distanceKm = distanceStr.toDoubleOrNull() ?: 10.0,
                                durationMinutes = durationStr,
                                tripDate = dateFormat.format(Date())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGeofenceDialog(
    viewModel: DriveCareViewModel,
    selectedVehicle: Vehicle?,
    currentLat: Double,
    currentLng: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val geofenceZones by viewModel.geofenceZones.collectAsState()
    var activeVehicle by remember { mutableStateOf(selectedVehicle ?: vehicles.firstOrNull()) }

    var zoneName by remember { mutableStateOf("Safe Zone") }
    var radiusMeters by remember { mutableDoubleStateOf(500.0) }
    var notifyOnEnter by remember { mutableStateOf(true) }
    var notifyOnExit by remember { mutableStateOf(true) }
    var customRadiusStr by remember { mutableStateOf("500") }

    val presetRadii = listOf(100.0, 250.0, 500.0, 1000.0, 2000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Fence, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Create Safe Perimeter Zone", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (vehicles.size > 1) {
                    Text("Select Vehicle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicles.forEach { v ->
                            FilterChip(
                                selected = (activeVehicle?.id == v.id),
                                onClick = { activeVehicle = v },
                                label = { Text(v.vehicleName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name") },
                    placeholder = { Text("e.g. Home Base, Office Parking, Garage") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Home Base", "Workplace", "Service Center", "Valet Guard").forEach { preset ->
                        SuggestionChip(
                            onClick = { zoneName = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Perimeter Radius (Meters)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetRadii.forEach { r ->
                        FilterChip(
                            selected = (radiusMeters == r),
                            onClick = {
                                radiusMeters = r
                                customRadiusStr = r.toInt().toString()
                            },
                            label = { Text("${r.toInt()}m") }
                        )
                    }
                }

                OutlinedTextField(
                    value = customRadiusStr,
                    onValueChange = { input ->
                        customRadiusStr = input
                        val d = input.toDoubleOrNull()
                        if (d != null && d >= 50.0 && d <= 50000.0) {
                            radiusMeters = d
                        }
                    },
                    label = { Text("Custom Radius (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Alert Triggers", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Entry", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyOnEnter, onCheckedChange = { notifyOnEnter = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Exit", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyOnExit, onCheckedChange = { notifyOnExit = it })
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Center Point: ${if (currentLat != 0.0) "Current GPS position (${String.format(Locale.US, "%.4f", currentLat)}, ${String.format(Locale.US, "%.4f", currentLng)})" else "Default city coordinates"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVeh = activeVehicle ?: vehicles.firstOrNull()
                    if (targetVeh == null) {
                        Toast.makeText(context, "Please register a vehicle first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val cleanName = zoneName.trim()
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Zone name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (geofenceZones.any { z -> z.vehicleId == targetVeh.id && z.zoneName.equals(cleanName, ignoreCase = true) }) {
                        Toast.makeText(context, "A geofence with this name already exists for this vehicle", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val finalRadius = customRadiusStr.toDoubleOrNull() ?: radiusMeters
                    if (finalRadius < 50.0 || finalRadius > 50000.0) {
                        Toast.makeText(context, "Radius must be between 50m and 50,000m", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val latToUse = if (currentLat != 0.0) currentLat else 37.7749
                    val lngToUse = if (currentLng != 0.0) currentLng else -122.4194

                    viewModel.addGeofenceZone(
                        GeofenceZone(
                            vehicleId = targetVeh.id,
                            zoneName = cleanName,
                            centerLatitude = latToUse,
                            centerLongitude = lngToUse,
                            radiusMeters = finalRadius,
                            notifyOnEnter = notifyOnEnter,
                            notifyOnExit = notifyOnExit,
                            isActive = true
                        )
                    )
                    Toast.makeText(context, "Safe zone '$cleanName' created with ${finalRadius.toInt()}m radius!", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            ) {
                Text("Create Geofence")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGeofenceDialog(
    viewModel: DriveCareViewModel,
    geofence: GeofenceZone,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoneName by remember { mutableStateOf(geofence.zoneName) }
    var radiusMeters by remember { mutableDoubleStateOf(geofence.radiusMeters) }
    var notifyOnEnter by remember { mutableStateOf(geofence.notifyOnEnter) }
    var notifyOnExit by remember { mutableStateOf(geofence.notifyOnExit) }
    var isActive by remember { mutableStateOf(geofence.isActive) }
    var customRadiusStr by remember { mutableStateOf(geofence.radiusMeters.toInt().toString()) }

    val presetRadii = listOf(100.0, 250.0, 500.0, 1000.0, 2000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Edit Safe Perimeter Zone", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Perimeter Radius (Meters)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetRadii.forEach { r ->
                        FilterChip(
                            selected = (radiusMeters == r),
                            onClick = {
                                radiusMeters = r
                                customRadiusStr = r.toInt().toString()
                            },
                            label = { Text("${r.toInt()}m") }
                        )
                    }
                }

                OutlinedTextField(
                    value = customRadiusStr,
                    onValueChange = { input ->
                        customRadiusStr = input
                        val d = input.toDoubleOrNull()
                        if (d != null && d >= 50.0 && d <= 50000.0) {
                            radiusMeters = d
                        }
                    },
                    label = { Text("Custom Radius (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Alert Triggers", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Monitoring", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Entry", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyOnEnter, onCheckedChange = { notifyOnEnter = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify on Exit", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyOnExit, onCheckedChange = { notifyOnExit = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = zoneName.trim()
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Zone name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalRadius = customRadiusStr.toDoubleOrNull() ?: radiusMeters
                    if (finalRadius < 50.0 || finalRadius > 50000.0) {
                        Toast.makeText(context, "Radius must be between 50m and 50,000m", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.updateGeofenceZone(
                        geofence.copy(
                            zoneName = cleanName,
                            radiusMeters = finalRadius,
                            notifyOnEnter = notifyOnEnter,
                            notifyOnExit = notifyOnExit,
                            isActive = isActive
                        )
                    )
                    Toast.makeText(context, "Geofence '$cleanName' updated!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Update Geofence")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatusCheckChip(label: String, isOk: Boolean) {
    Surface(
        color = if (isOk) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.defaultMinSize(minWidth = 80.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isOk) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

