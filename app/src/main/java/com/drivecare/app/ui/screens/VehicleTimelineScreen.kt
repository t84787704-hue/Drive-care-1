package com.drivecare.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.TimelineEvent
import com.drivecare.app.utils.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleTimelineScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelEntries by viewModel.fuelEntries.collectAsState()
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var selectedVehicleId by remember { mutableStateOf<Long?>(-1L) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val timelineEvents = remember(vehicles, fuelEntries, maintenanceLogs, reminders, documents, expenses, selectedVehicleId) {
        viewModel.getTimelineEvents(selectedVehicleId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Vehicle Filter Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Vehicle Timeline",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Complete chronological event history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filter Button / Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                FilterChip(
                    selected = true,
                    onClick = { expandedDropdown = true },
                    label = {
                        val name = if (selectedVehicleId == null || selectedVehicleId == -1L) "All Vehicles" else vehicles.find { it.id == selectedVehicleId }?.vehicleName ?: "Selected"
                        Text(name)
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Vehicles") },
                        onClick = {
                            selectedVehicleId = -1L
                            expandedDropdown = false
                        }
                    )
                    vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.vehicleName) },
                            onClick = {
                                selectedVehicleId = v.id
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }
        }

        if (timelineEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Timeline Activity", fontWeight = FontWeight.SemiBold)
                    Text("Add fuel entries, maintenance, or expenses to view chronological logs here.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(timelineEvents) { event ->
                    TimelineEventRow(event)
                }
            }
        }
    }
}

@Composable
fun TimelineEventRow(event: TimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline Dot Icon
        val (icon, color) = when (event.type) {
            "Fuel" -> Icons.Default.LocalGasStation to MaterialTheme.colorScheme.primary
            "Service" -> Icons.Default.Build to MaterialTheme.colorScheme.tertiary
            "Reminder" -> Icons.Default.Notifications to MaterialTheme.colorScheme.secondary
            "Document" -> Icons.Default.Description to MaterialTheme.colorScheme.outline
            "Expense" -> Icons.Default.AttachMoney to MaterialTheme.colorScheme.error
            else -> Icons.Default.Event to MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = event.date,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (event.costOrAmount.isNotBlank()) {
                        Text(
                            text = event.costOrAmount,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
