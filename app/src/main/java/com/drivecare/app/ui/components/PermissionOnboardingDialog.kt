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
import android.widget.Toast
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

enum class GeofenceStatusLevel {
    ENABLED,
    PARTIALLY_ENABLED,
    DISABLED
}

data class PermissionStatusState(
    val hasFineLocation: Boolean,
    val hasBackgroundLocation: Boolean,
    val canScheduleExactAlarms: Boolean,
    val isIgnoringBatteryOptimizations: Boolean
) {
    val isFullyConfigured: Boolean
        get() = hasFineLocation && hasBackgroundLocation && canScheduleExactAlarms && isIgnoringBatteryOptimizations

    val statusLevel: GeofenceStatusLevel
        get() = when {
            isFullyConfigured -> GeofenceStatusLevel.ENABLED
            hasFineLocation -> GeofenceStatusLevel.PARTIALLY_ENABLED
            else -> GeofenceStatusLevel.DISABLED
        }
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
    val lang = LocalAppLanguage.current
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

    // Re-check state automatically when Activity resumes from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statusState = checkGeofencePermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    Text(
                        text = AppStrings.get("safe_zone_title", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = AppStrings.get("safe_zone_sub", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    text = AppStrings.get("geofence_bg_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Overall Status Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (statusState.statusLevel) {
                            GeofenceStatusLevel.ENABLED -> MaterialTheme.colorScheme.primaryContainer
                            GeofenceStatusLevel.PARTIALLY_ENABLED -> MaterialTheme.colorScheme.secondaryContainer
                            GeofenceStatusLevel.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
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
                            imageVector = when (statusState.statusLevel) {
                                GeofenceStatusLevel.ENABLED -> Icons.Default.CheckCircle
                                GeofenceStatusLevel.PARTIALLY_ENABLED -> Icons.Default.Tune
                                GeofenceStatusLevel.DISABLED -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (statusState.statusLevel) {
                                GeofenceStatusLevel.ENABLED -> MaterialTheme.colorScheme.primary
                                GeofenceStatusLevel.PARTIALLY_ENABLED -> MaterialTheme.colorScheme.secondary
                                GeofenceStatusLevel.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (statusState.statusLevel) {
                                    GeofenceStatusLevel.ENABLED -> AppStrings.get("status_enabled", lang)
                                    GeofenceStatusLevel.PARTIALLY_ENABLED -> AppStrings.get("status_partially_enabled", lang)
                                    GeofenceStatusLevel.DISABLED -> AppStrings.get("status_disabled", lang)
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (statusState.statusLevel) {
                                    GeofenceStatusLevel.ENABLED -> AppStrings.get("geofence_all_met", lang)
                                    else -> AppStrings.get("geofence_bg_desc", lang)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // STEP 1: Location Permission
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
                                imageVector = if (statusState.hasFineLocation) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (statusState.hasFineLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = AppStrings.get("step_location", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = AppStrings.get("fine_location", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.hasFineLocation) "• ${AppStrings.get("fine_location", lang)}: ${AppStrings.get("granted", lang)}" else "• ${AppStrings.get("fine_location", lang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.hasFineLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                enabled = !statusState.hasFineLocation,
                                modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (statusState.hasFineLocation) AppStrings.get("granted", lang) else AppStrings.get("grant_location", lang),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // STEP 2: Background Location
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
                                imageVector = if (statusState.hasBackgroundLocation) Icons.Default.CheckCircle else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (statusState.hasBackgroundLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = AppStrings.get("step_bg_location", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = AppStrings.get("background_gps", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.hasBackgroundLocation) "• ${AppStrings.get("background_gps", lang)}: ${AppStrings.get("granted", lang)}" else "• ${AppStrings.get("background_gps", lang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.hasBackgroundLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        try {
                                            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                enabled = !statusState.hasBackgroundLocation,
                                modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (statusState.hasBackgroundLocation) AppStrings.get("granted", lang) else AppStrings.get("allow_background", lang),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // STEP 3: Exact Alarm Permission
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
                            Text(
                                text = AppStrings.get("step_exact_alarms", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = AppStrings.get("exact_alarms", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.canScheduleExactAlarms) "• ${AppStrings.get("exact_alarms", lang)}: ${AppStrings.get("granted", lang)}" else "• ${AppStrings.get("exact_alarms", lang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.canScheduleExactAlarms) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

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
                                },
                                enabled = !statusState.canScheduleExactAlarms,
                                modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (statusState.canScheduleExactAlarms) AppStrings.get("granted", lang) else AppStrings.get("enable_alarms", lang),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // STEP 4: Battery Optimization Guidance
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
                            Text(
                                text = AppStrings.get("step_battery", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = AppStrings.get("unrestricted_battery", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (statusState.isIgnoringBatteryOptimizations) "• ${AppStrings.get("unrestricted_battery", lang)}: ${AppStrings.get("granted", lang)}" else "• ${AppStrings.get("unrestricted_battery", lang)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (statusState.isIgnoringBatteryOptimizations) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { openBatterySettings(context, lang) },
                                modifier = Modifier.defaultMinSize(minWidth = 80.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (statusState.isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = AppStrings.get("open_battery_settings", lang),
                                    maxLines = 1,
                                    softWrap = false
                                )
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
                Text(AppStrings.get("done", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("skip_close", lang))
            }
        }
    )
}

fun openBatterySettings(context: Context, lang: AppLanguage = AppLanguage.ENGLISH) {
    var opened = false
    // 1. First try direct battery optimization request
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
        opened = true
    } catch (e: Exception) {
        // Fallback 1
    }

    if (!opened) {
        // 2. Try general battery optimization settings screen
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            // Fallback 2
        }
    }

    if (!opened) {
        // 3. Fall back to application details settings
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            opened = true
        } catch (e: Exception) {
            // Fallback 3
        }
    }

    if (opened) {
        Toast.makeText(
            context,
            AppStrings.get("battery_unrestricted_toast", lang),
            Toast.LENGTH_LONG
        ).show()
    } else {
        Toast.makeText(
            context,
            AppStrings.get("battery_manual_toast", lang),
            Toast.LENGTH_LONG
        ).show()
    }
}


