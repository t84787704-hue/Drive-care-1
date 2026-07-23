package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

enum class MoreSubSection {
    MENU,
    INSURANCE,
    EXPENSES,
    TIMELINE,
    GPS_TRACKING,
    FAMILY_SHARING,
    DOCUMENTS,
    EMERGENCY,
    ACHIEVEMENTS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    initialSubSection: MoreSubSection = MoreSubSection.MENU
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lang = LocalAppLanguage.current

    var currentSubSection by remember { mutableStateOf(initialSubSection) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }

    if (currentSubSection != MoreSubSection.MENU) {
        Column(modifier = modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        when (currentSubSection) {
                            MoreSubSection.INSURANCE -> "Insurance & Renewals"
                            MoreSubSection.EXPENSES -> "Expense Manager"
                            MoreSubSection.TIMELINE -> "Vehicle Timeline"
                            MoreSubSection.GPS_TRACKING -> "GPS Live Tracking & Trips"
                            MoreSubSection.FAMILY_SHARING -> "Family Sharing & Drivers"
                            MoreSubSection.DOCUMENTS -> AppStrings.get("tab_documents", lang)
                            MoreSubSection.EMERGENCY -> AppStrings.get("tab_emergency", lang)
                            MoreSubSection.ACHIEVEMENTS -> AppStrings.get("tab_achievements", lang)
                            MoreSubSection.SETTINGS -> AppStrings.get("settings_title", lang)
                            else -> AppStrings.get("tab_more", lang)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { currentSubSection = MoreSubSection.MENU }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            Box(modifier = Modifier.weight(1f)) {
                when (currentSubSection) {
                    MoreSubSection.INSURANCE -> InsuranceRenewalScreen(viewModel = viewModel)
                    MoreSubSection.EXPENSES -> ExpenseManagerScreen(viewModel = viewModel)
                    MoreSubSection.TIMELINE -> VehicleTimelineScreen(viewModel = viewModel)
                    MoreSubSection.GPS_TRACKING -> GpsTrackingScreen(viewModel = viewModel)
                    MoreSubSection.FAMILY_SHARING -> FamilySharingScreen(viewModel = viewModel)
                    MoreSubSection.DOCUMENTS -> DocumentsScreen(viewModel = viewModel)
                    MoreSubSection.EMERGENCY -> EmergencyScreen(viewModel = viewModel)
                    MoreSubSection.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                    MoreSubSection.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    else -> {}
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = AppStrings.get("tab_more", lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Grid / Section 1: Features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MANAGEMENT & SERVICES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    MoreMenuItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Insurance & Renewals",
                        subtitle = "Policy Details, Renewal Reminders & Coverage Tracking",
                        onClick = { currentSubSection = MoreSubSection.INSURANCE }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Expense Manager",
                        subtitle = "Track Parking, Tolls, Insurance & Taxes",
                        onClick = { currentSubSection = MoreSubSection.EXPENSES }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Timeline,
                        title = "Vehicle Timeline",
                        subtitle = "Chronological History of Refills, Services & Expenses",
                        onClick = { currentSubSection = MoreSubSection.TIMELINE }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.GpsFixed,
                        title = "GPS Tracking & Route History",
                        subtitle = "Live Vehicle Location, Trip Logs & Geofencing",
                        onClick = { currentSubSection = MoreSubSection.GPS_TRACKING }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Group,
                        title = "Family Sharing & Drivers",
                        subtitle = "Multi-User Vehicle Sharing, Driver Profiles & Transfers",
                        onClick = { currentSubSection = MoreSubSection.FAMILY_SHARING }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.FolderOpen,
                        title = AppStrings.get("tab_documents", lang),
                        subtitle = "Manage Insurance, Registration, PUC & Licenses",
                        onClick = { currentSubSection = MoreSubSection.DOCUMENTS }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Emergency,
                        title = AppStrings.get("tab_emergency", lang),
                        subtitle = "One-tap Calling for Towing, Mechanics & Contacts",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { currentSubSection = MoreSubSection.EMERGENCY }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.EmojiEvents,
                        title = AppStrings.get("tab_achievements", lang),
                        subtitle = "Milestone Badges & Driving Gamification",
                        onClick = { currentSubSection = MoreSubSection.ACHIEVEMENTS }
                    )
                }
            }

            // Section 2: Preferences & System
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "APP PREFERENCES & TOOLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        title = AppStrings.get("settings_title", lang),
                        subtitle = "Language, Notifications & Preferences",
                        onClick = { currentSubSection = MoreSubSection.SETTINGS }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Language,
                        title = AppStrings.get("language", lang),
                        subtitle = "Current: ${lang.displayName}",
                        onClick = { currentSubSection = MoreSubSection.SETTINGS }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Backup,
                        title = AppStrings.get("backup_restore", lang),
                        subtitle = "Export JSON Backup or Restore Records",
                        onClick = {
                            backupJsonText = viewModel.exportBackupJson()
                            showBackupDialog = true
                        }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Info,
                        title = AppStrings.get("about_drivecare", lang),
                        subtitle = "Version 2.0.0 Global Edition",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Backup Export & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(AppStrings.get("backup_restore", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Export your DriveCare data as JSON backup or restore from a saved string.")
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("JSON Backup Output") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(backupJsonText))
                            Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            showRestoreDialog = true
                        }
                    ) {
                        Text(AppStrings.get("restore_data", lang))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(AppStrings.get("restore_data", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your JSON backup string below to restore records:")
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            viewModel.restoreBackupJson(restoreJsonInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    showRestoreDialog = false
                                    restoreJsonInput = ""
                                }
                            }
                        }
                    }
                ) {
                    Text(AppStrings.get("restore_data", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(AppStrings.get("app_name", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DriveCare Worldwide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version 2.0.0 Global Edition", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Designed to empower drivers and fleet owners worldwide with intelligent health scoring, fuel tracking, maintenance recommendations, and multi-language support.")
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
