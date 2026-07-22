package com.drivecare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.ui.AchievementItem
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

@Composable
fun AchievementsScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()

    val totalKmDriven = vehicles.sumOf { it.odometerReading.toDoubleOrNull() ?: 0.0 }

    val achievements = listOf(
        AchievementItem("first_vehicle", "Add your 1st vehicle to garage.", vehicles.isNotEmpty(), if (vehicles.isNotEmpty()) 1.0f else 0.0f),
        AchievementItem("first_fuel", "Log your 1st fuel refuel entry.", fuelEntries.isNotEmpty(), if (fuelEntries.isNotEmpty()) 1.0f else 0.0f),
        AchievementItem("first_service", "Log your 1st vehicle service.", maintenanceLogs.isNotEmpty(), if (maintenanceLogs.isNotEmpty()) 1.0f else 0.0f),
        AchievementItem("distance_1000km", "Drive 1,000 KM across fleet.", totalKmDriven >= 1000, (totalKmDriven / 1000.0).coerceIn(0.0, 1.0).toFloat()),
        AchievementItem("distance_5000km", "Drive 5,000 KM across fleet.", totalKmDriven >= 5000, (totalKmDriven / 5000.0).coerceIn(0.0, 1.0).toFloat()),
        AchievementItem("fuel_10_entries", "Complete 10 fuel log entries.", fuelEntries.size >= 10, (fuelEntries.size / 10.0).coerceIn(0.0, 1.0).toFloat()),
        AchievementItem("fuel_25_entries", "Complete 25 fuel log entries.", fuelEntries.size >= 25, (fuelEntries.size / 25.0).coerceIn(0.0, 1.0).toFloat()),
        AchievementItem("fuel_50_entries", "Complete 50 fuel log entries.", fuelEntries.size >= 50, (fuelEntries.size / 50.0).coerceIn(0.0, 1.0).toFloat())
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = AppStrings.get("achievements_title", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (item.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = AppStrings.get(item.titleKey, lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.desc,
                            style = MaterialTheme.typography.labelSmall
                        )
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                        )
                    }
                }
            }
        }
    }
}
