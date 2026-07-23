package com.drivecare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.TimelineEvent
import com.drivecare.app.ui.components.QuickActionRow
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
    val expenses by viewModel.expenses.collectAsState()
    val documents by viewModel.documents.collectAsState()

    val totalFuelSpent = fuelEntries.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
    val totalMaintenanceSpent = maintenanceLogs.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
    val totalOtherExpenses = expenses.sumOf { it.amount }
    val grandTotalSpent = totalFuelSpent + totalMaintenanceSpent + totalOtherExpenses

    val activeRemindersCount = reminders.count { !it.isCompleted }

    // Dynamic Fleet Health Score
    val fleetHealthAvg = if (vehicles.isNotEmpty()) {
        vehicles.map { viewModel.calculateHealthScore(it, reminders, fuelEntries, maintenanceLogs, documents) }.average().toInt()
    } else 100

    val healthLabelKey = when {
        fleetHealthAvg >= 85 -> "health_excellent"
        fleetHealthAvg >= 70 -> "health_good"
        fleetHealthAvg >= 50 -> "health_fair"
        else -> "health_poor"
    }

    val monthlyFuelData = remember(fuelEntries) {
        viewModel.getMonthlyFuelData(fuelEntries)
    }

    val expenseCategories = remember(fuelEntries, maintenanceLogs, expenses) {
        viewModel.getExpenseCategoryBreakdown(fuelEntries, maintenanceLogs, expenses)
    }

    val recentTimeline = remember(vehicles, fuelEntries, maintenanceLogs, reminders, expenses, documents) {
        viewModel.getTimelineEvents().take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner with Fleet Health Visualization
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.get("overview_title", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (vehicles.isNotEmpty()) "${vehicles.size} ${AppStrings.get("vehicles_active_fleet", lang)}" else AppStrings.get("no_vehicles_registered", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (fleetHealthAvg >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$fleetHealthAvg%",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStrings.get("fleet_health_label", lang)}: ${AppStrings.get(healthLabelKey, lang)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeRemindersCount > 0) "$activeRemindersCount ${AppStrings.get("tasks_pending", lang)}" else AppStrings.get("all_tasks_up_to_date", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { fleetHealthAvg / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (fleetHealthAvg >= 75) MaterialTheme.colorScheme.primary else if (fleetHealthAvg >= 50) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                )
            }
        }

        // Quick Actions Shortcut Bar
        QuickActionRow(viewModel = viewModel)

        // KPI Summary Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = AppStrings.get("total_vehicles", lang),
                    value = "${vehicles.size}",
                    icon = Icons.Default.DirectionsCar,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke("GARAGE") }
                )
                DashboardKpiCard(
                    title = AppStrings.get("upcoming_services", lang),
                    value = "$activeRemindersCount",
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke("SERVICES") }
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
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke("FUEL") }
                )
                DashboardKpiCard(
                    title = AppStrings.get("total_expense_logged", lang),
                    value = "$${String.format(Locale.US, "%.2f", grandTotalSpent)}",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke("EXPENSES") }
                )
            }
        }

        // Analytics & Charts Section
        if (monthlyFuelData.isNotEmpty() || expenseCategories.isNotEmpty()) {
            Text(
                text = AppStrings.get("analytics_cost_breakdown", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Monthly Fuel Cost Bar Chart
            if (monthlyFuelData.isNotEmpty()) {
                MonthlyFuelChartCard(monthlyFuelData)
            }

            // Expense Category Breakdown
            if (expenseCategories.isNotEmpty()) {
                ExpenseCategoryBreakdownCard(expenseCategories, grandTotalSpent)
            }
        }

        // Vehicle Health & Efficiency Breakdown
        Text(
            text = AppStrings.get("fleet_summary", lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (vehicles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        AppStrings.get("no_vehicles_registered", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        AppStrings.get("no_vehicles_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onNavigateTab?.invoke("GARAGE") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("add_vehicle", lang))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                vehicles.forEach { v ->
                    val vHealth = viewModel.calculateHealthScore(v, reminders, fuelEntries, maintenanceLogs, documents)
                    val vEfficiency = viewModel.calculateVehicleFuelEfficiency(v, fuelEntries)
                    val vCostPerKm = viewModel.calculateCostPerKm(v, fuelEntries, maintenanceLogs, expenses)

                    VehicleHealthCard(
                        vehicle = v,
                        healthScore = vHealth,
                        efficiency = vEfficiency,
                        costPerKm = vCostPerKm,
                        lang = lang,
                        onClick = { onNavigateTab?.invoke("GARAGE") }
                    )
                }
            }
        }

        // Recent Timeline Events Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.get("recent_timeline_activity", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (recentTimeline.isNotEmpty()) {
                TextButton(onClick = { onNavigateTab?.invoke("TIMELINE") }) {
                    Text(
                        text = AppStrings.get("view_all", lang),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (recentTimeline.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("no_recent_activity", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentTimeline.forEach { event ->
                    DashboardTimelineEventRow(event)
                }
            }
        }

        // Trip Calculator Card
        TripCalculatorCard(lang)
    }
}

@Composable
fun DashboardKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MonthlyFuelChartCard(monthlyFuelData: Map<String, Double>) {
    val lang = LocalAppLanguage.current
    val maxCost = (monthlyFuelData.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val entries = monthlyFuelData.entries.toList().takeLast(6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = AppStrings.get("monthly_fuel_expenditure", lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { entry ->
                    val monthKey = entry.key
                    val cost = entry.value
                    val fraction = (cost / maxCost).toFloat().coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$${cost.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (monthKey.length >= 7) monthKey.substring(5) else monthKey,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoryBreakdownCard(expenseCategories: Map<String, Double>, totalSpent: Double) {
    val lang = LocalAppLanguage.current
    val categoriesList = expenseCategories.entries.toList().take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = AppStrings.get("expense_category_breakdown", lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            categoriesList.forEach { entry ->
                val category = entry.key
                val amount = entry.value
                val pct = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$${String.format(Locale.US, "%.2f", amount)} (${String.format(Locale.US, "%.1f", pct)}%)", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (pct / 100f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (category) {
                            "Fuel" -> MaterialTheme.colorScheme.primary
                            "Service" -> MaterialTheme.colorScheme.secondary
                            "Insurance" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleHealthCard(
    vehicle: Vehicle,
    healthScore: Int,
    efficiency: com.drivecare.app.ui.FuelEfficiencyStats,
    costPerKm: Double,
    lang: com.drivecare.app.utils.AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = when (vehicle.vehicleType.lowercase()) {
                                "suv", "van" -> Icons.Default.AirportShuttle
                                "motorcycle", "bike" -> Icons.Default.TwoWheeler
                                "truck" -> Icons.Default.LocalShipping
                                "electric", "ev" -> Icons.Default.EvStation
                                else -> Icons.Default.DirectionsCar
                            },
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(vehicle.vehicleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${vehicle.brand} ${vehicle.model} • ${vehicle.odometerReading} km", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Surface(
                    color = if (healthScore >= 80) MaterialTheme.colorScheme.primaryContainer else if (healthScore >= 50) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
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

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { healthScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (healthScore >= 80) MaterialTheme.colorScheme.primary else if (healthScore >= 50) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Efficiency & Cost stats badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${AppStrings.get("fuel_efficiency", lang)}: ${String.format(Locale.US, "%.1f", efficiency.kmPerLitre)} km/L",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${AppStrings.get("cost_label", lang)}: ${if (costPerKm > 0) String.format(Locale.US, "$%.2f/km", costPerKm) else "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTimelineEventRow(event: TimelineEvent) {
    val (icon, color) = when (event.type) {
        "Fuel" -> Icons.Default.LocalGasStation to MaterialTheme.colorScheme.primary
        "Service" -> Icons.Default.Build to MaterialTheme.colorScheme.secondary
        "Reminder" -> Icons.Default.Notifications to MaterialTheme.colorScheme.tertiary
        "Document" -> Icons.Default.Description to MaterialTheme.colorScheme.outline
        else -> Icons.Default.Receipt to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(event.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (event.costOrAmount.isNotBlank()) {
                        Text(event.costOrAmount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
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

            Spacer(modifier = Modifier.height(14.dp))

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
