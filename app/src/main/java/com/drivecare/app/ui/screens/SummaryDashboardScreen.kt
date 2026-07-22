package com.drivecare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import java.util.Locale

@Composable
fun SummaryDashboardScreen(
    viewModel: DriveCareViewModel,
    onNavigateTab: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    val totalFuelSpent = fuelEntries.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
    val totalMaintenanceSpent = maintenanceLogs.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
    val activeRemindersCount = reminders.count { !it.isCompleted }

    // Average Fleet Health
    val fleetHealthAvg = if (vehicles.isNotEmpty()) {
        vehicles.map { viewModel.calculateHealthScore(it, reminders, fuelEntries, maintenanceLogs) }.average().toInt()
    } else 100

    val healthLabelKey = when {
        fleetHealthAvg >= 90 -> "health_excellent"
        fleetHealthAvg >= 70 -> "health_good"
        fleetHealthAvg >= 50 -> "health_fair"
        else -> "health_poor"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStrings.get("overview_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${AppStrings.get("vehicle_health_score", lang)}: $fleetHealthAvg% (${AppStrings.get(healthLabelKey, lang)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fleetHealthAvg / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (fleetHealthAvg > 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }

        // Top 6 Dashboard Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = AppStrings.get("total_vehicles", lang),
                    value = "${vehicles.size}",
                    icon = Icons.Default.DirectionsCar,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiCard(
                    title = AppStrings.get("upcoming_services", lang),
                    value = "$activeRemindersCount",
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = AppStrings.get("monthly_fuel_cost", lang),
                    value = "$${String.format(Locale.US, "%.2f", totalFuelSpent)}",
                    icon = Icons.Default.LocalGasStation,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiCard(
                    title = AppStrings.get("total_fuel_entries", lang),
                    value = "${fuelEntries.size}",
                    icon = Icons.Default.EvStation,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = AppStrings.get("service_spent", lang),
                    value = "$${String.format(Locale.US, "%.2f", totalMaintenanceSpent)}",
                    icon = Icons.Default.Build,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiCard(
                    title = AppStrings.get("vehicle_health_score", lang),
                    value = "$fleetHealthAvg%",
                    icon = Icons.Default.HealthAndSafety,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Fleet Quick Overview Section
        Text(
            text = AppStrings.get("fleet_summary", lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (vehicles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(AppStrings.get("no_vehicles_registered", lang), fontWeight = FontWeight.SemiBold)
                    Text(AppStrings.get("no_vehicles_desc", lang), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicles.forEach { v ->
                    val vHealth = viewModel.calculateHealthScore(v, reminders, fuelEntries, maintenanceLogs)
                    VehicleHealthRow(v, vHealth, lang)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trip Calculator Card
        TripCalculatorCard(lang)
    }
}

@Composable
fun DashboardKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VehicleHealthRow(vehicle: Vehicle, healthScore: Int, lang: com.drivecare.app.utils.AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(vehicle.vehicleName, fontWeight = FontWeight.Bold)
                    Text("${vehicle.vehicleType} • ${vehicle.odometerReading.ifBlank { "0" }} km", style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    color = if (healthScore >= 80) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "$healthScore%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { healthScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (healthScore >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun TripCalculatorCard(lang: com.drivecare.app.utils.AppLanguage) {
    var distanceStr by remember { mutableStateOf("150") }
    var fuelPriceStr by remember { mutableStateOf("1.50") }
    var mileageStr by remember { mutableStateOf("12") }
    var passengersStr by remember { mutableStateOf("3") }

    val distance = distanceStr.toDoubleOrNull() ?: 0.0
    val price = fuelPriceStr.toDoubleOrNull() ?: 0.0
    val mileage = mileageStr.toDoubleOrNull() ?: 1.0
    val passengers = (passengersStr.toIntOrNull() ?: 1).coerceAtLeast(1)

    val litresNeeded = if (mileage > 0) distance / mileage else 0.0
    val totalCost = litresNeeded * price
    val costPerPerson = totalCost / passengers

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = AppStrings.get("trip_calculator", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = distanceStr,
                    onValueChange = { distanceStr = it },
                    label = { Text(AppStrings.get("distance_km", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fuelPriceStr,
                    onValueChange = { fuelPriceStr = it },
                    label = { Text(AppStrings.get("fuel_price", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mileageStr,
                    onValueChange = { mileageStr = it },
                    label = { Text(AppStrings.get("avg_mileage", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = passengersStr,
                    onValueChange = { passengersStr = it },
                    label = { Text(AppStrings.get("passengers", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = AppStrings.get("estimated_cost", lang),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalCost)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = AppStrings.get("cost_per_person", lang),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", costPerPerson)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
