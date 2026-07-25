package com.drivecare.app.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class PermissionStatusState(
    val hasFineLocation: Boolean,
    val hasBackgroundLocation: Boolean,
    val canScheduleExactAlarms: Boolean,
    val isIgnoringBatteryOptimizations: Boolean
) {
    val isFullyConfigured: Boolean
        get() = hasFineLocation && hasBackgroundLocation && canScheduleExactAlarms && isIgnoringBatteryOptimizations
}

fun checkGeofencePermissionStatus(context: Context): PermissionStatusState {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val canScheduleAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: true
    } else {
        true
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    } else {
        true
    }

    return PermissionStatusState(
        hasFineLocation = hasFineLocation,
        hasBackgroundLocation = hasBackgroundLocation,
        canScheduleExactAlarms = canScheduleAlarms,
        isIgnoringBatteryOptimizations = isIgnoringBattery
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingDialog(
    onDismiss: () -> Unit,
    onCompleted: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    var statusState by remember { mutableStateOf(checkGeofencePermissionStatus(context)) }

    // Launcher for Fine + Coarse Location
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        statusState = checkGeofencePermissionStatus(context)
    }

    // Launcher for Background Location
    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        statusState = checkGeofencePermissionStatus(context)
    }

    // Re-check state when composable resumes
    DisposableEffect(Unit) {
        onDispose { }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("Enable Tracking & Alerts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Safe Zone & Maintenance Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "DriveCare uses high-precision background geofencing and scheduled alarms to alert you when your vehicle enters or exits safe perimeter zones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Overall Status Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (statusState.isFullyConfigured) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (statusState.isFullyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (statusState.isFullyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (statusState.isFullyConfigured) "Geofence System Ready" else "System Requirements Incomplete",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (statusState.isFullyConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (statusState.isFullyConfigured) "All permissions and system settings configured." else "Complete the steps below for reliable background alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusState.isFullyConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // STEP 1: Location & Background Location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (statusState.hasFineLocation && statusState.hasBackgroundLocation) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (statusState.hasFineLocation && statusState.hasBackgroundLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Text("Step 1: Location & Background Access", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        Text(
                            "Fine location accurately centers safe zones. Background location allows entry & exit detection while DriveCare is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    statusState.hasFineLocation && statusState.hasBackgroundLocation -> "• Foreground & Background: Granted"
                                    statusState.hasFineLocation -> "• Foreground: Granted | Background: Missing"
                                    else -> "• Location Permission: Not Granted"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.hasFineLocation && statusState.hasBackgroundLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )

                            Button(
                                onClick = {
                                    if (!statusState.hasFineLocation) {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !statusState.hasBackgroundLocation) {
                                        try {
                                            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                    statusState = checkGeofencePermissionStatus(context)
                                },
                                enabled = !(statusState.hasFineLocation && statusState.hasBackgroundLocation)
                            ) {
                                Text(
                                    when {
                                        statusState.hasFineLocation && statusState.hasBackgroundLocation -> "Granted"
                                        statusState.hasFineLocation -> "Allow Background"
                                        else -> "Grant Location"
                                    }
                                )
                            }
                        }
                    }
                }

                // STEP 2: Exact Alarm Permission
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (statusState.canScheduleExactAlarms) Icons.Default.CheckCircle else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (statusState.canScheduleExactAlarms) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Text("Step 2: Exact Alarm Schedules", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        Text(
                            "Android requires exact alarm permission for precise geofence boundary re-checks and maintenance due date alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.canScheduleExactAlarms) "• Exact Alarms: Enabled" else "• Exact Alarms: Restricted",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.canScheduleExactAlarms) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                    statusState = checkGeofencePermissionStatus(context)
                                },
                                enabled = !statusState.canScheduleExactAlarms
                            ) {
                                Text(if (statusState.canScheduleExactAlarms) "Granted" else "Grant Permission")
                            }
                        }
                    }
                }

                // STEP 3: Battery Optimization Guidance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (statusState.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (statusState.isIgnoringBatteryOptimizations) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Text("Step 3: Battery Optimization Guidance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        Text(
                            "Prevent Android system doze mode from putting DriveCare geofence background listeners to sleep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Recommended Battery Setup:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text("1. Set Battery Usage to 'Unrestricted'", style = MaterialTheme.typography.bodySmall)
                                Text("2. Disable 'Pause app activity if unused'", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.isIgnoringBatteryOptimizations) "• Battery: Unrestricted" else "• Battery: Optimized (May delay alerts)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.isIgnoringBatteryOptimizations) Color(0xFF2E7D32) else MaterialTheme.colorScheme.tertiary
                            )

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                    statusState = checkGeofencePermissionStatus(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (statusState.isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (statusState.isIgnoringBatteryOptimizations) "Open Settings" else "Configure Battery")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCompleted()
                }
            ) {
                Text(if (statusState.isFullyConfigured) "Done" else "Continue Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
