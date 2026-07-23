package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Reminder
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (vehicles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.AddAlarm, contentDescription = AppStrings.get("add_reminder", lang)) },
                    text = { Text(AppStrings.get("add_reminder", lang)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = AppStrings.get("tab_reminders", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppStrings.get("no_reminders", lang), style = MaterialTheme.typography.titleMedium)
                        Text(AppStrings.get("no_reminders_desc", lang), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reminders, key = { it.id }) { rem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (rem.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = rem.isCompleted,
                                        onCheckedChange = { viewModel.toggleReminder(rem) }
                                    )
                                    Column {
                                        Text(rem.reminderTitle, fontWeight = FontWeight.Bold)
                                        Text("${rem.vehicleName} • ${rem.dueDate}")
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteReminder(rem) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && vehicles.isNotEmpty()) {
        AddReminderDialog(
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = { reminder ->
                viewModel.addReminder(reminder)
                showAddDialog = false
            },
            lang = lang
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    lang: com.drivecare.app.utils.AppLanguage
) {
    val context = LocalContext.current

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("2026-08-15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_reminder", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Vehicle *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.vehicleName} (${v.brand} ${v.model})") },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(AppStrings.get("reminder_title", lang) + " *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(AppStrings.get("due_date", lang) + " (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle
                    if (v == null) {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "Please enter reminder title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val cleanDueDate = dueDate.trim()
                    if (cleanDueDate.isBlank()) {
                        Toast.makeText(context, "Please enter due date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val reminder = Reminder(
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        reminderTitle = cleanTitle,
                        dueDate = cleanDueDate
                    )
                    onSave(reminder)
                }
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}
