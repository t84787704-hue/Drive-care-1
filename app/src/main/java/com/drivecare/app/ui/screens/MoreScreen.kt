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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    SETTINGS,
    PROFILE,
    AUTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    subSection: MoreSubSection = MoreSubSection.MENU,
    highlightRecordId: Long? = null,
    onSubSectionSelect: (MoreSubSection) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lang = LocalAppLanguage.current

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }

    if (subSection != MoreSubSection.MENU) {
        Box(modifier = modifier.fillMaxSize()) {
            when (subSection) {
                MoreSubSection.INSURANCE -> InsuranceRenewalScreen(viewModel = viewModel, highlightRecordId = highlightRecordId)
                MoreSubSection.EXPENSES -> ExpenseManagerScreen(viewModel = viewModel, highlightRecordId = highlightRecordId)
                MoreSubSection.TIMELINE -> VehicleTimelineScreen(viewModel = viewModel)
                MoreSubSection.GPS_TRACKING -> GpsTrackingScreen(viewModel = viewModel)
                MoreSubSection.FAMILY_SHARING -> FamilySharingScreen(viewModel = viewModel)
                MoreSubSection.DOCUMENTS -> DocumentsScreen(viewModel = viewModel, highlightRecordId = highlightRecordId)
                MoreSubSection.EMERGENCY -> EmergencyScreen(viewModel = viewModel)
                MoreSubSection.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                MoreSubSection.SETTINGS -> SettingsScreen(viewModel = viewModel)
                MoreSubSection.PROFILE -> ProfileScreen(viewModel = viewModel)
                MoreSubSection.AUTH -> AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = { onSubSectionSelect(MoreSubSection.PROFILE) }
                )
                else -> {}
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

            // Cloud Account Status Card
            val currentUser by viewModel.currentUser.collectAsState()
            val userProfile by viewModel.userProfile.collectAsState()
            val syncState by viewModel.syncState.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentUser != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentUser != null) (userProfile?.fullName?.ifBlank { null } ?: currentUser?.email ?: "Cloud Account") else "Local Account (Offline)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUser != null) "Cloud Sync Active • ${viewModel.syncManager.formattedLastSync()}" else "Sign in to backup & sync data across devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (currentUser != null) {
                                onSubSectionSelect(MoreSubSection.PROFILE)
                            } else {
                                onSubSectionSelect(MoreSubSection.AUTH)
                            }
                        }
                    ) {
                        Text(if (currentUser != null) "Profile" else "Sign In")
                    }
                }
            }

            // Grid / Section 1: Features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = AppStrings.get("management_services", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    MoreMenuItem(
                        icon = Icons.Default.VerifiedUser,
                        title = AppStrings.get("insurance_renewals_menu", lang),
                        subtitle = AppStrings.get("insurance_policies_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.INSURANCE) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.AttachMoney,
                        title = AppStrings.get("expense_manager_menu", lang),
                        subtitle = AppStrings.get("expense_manager_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.EXPENSES) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Timeline,
                        title = AppStrings.get("vehicle_timeline_menu", lang),
                        subtitle = AppStrings.get("vehicle_timeline_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.TIMELINE) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.GpsFixed,
                        title = AppStrings.get("gps_tracking_menu", lang),
                        subtitle = AppStrings.get("gps_tracking_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.GPS_TRACKING) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Group,
                        title = AppStrings.get("family_sharing_menu", lang),
                        subtitle = AppStrings.get("family_sharing_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.FAMILY_SHARING) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.FolderOpen,
                        title = AppStrings.get("tab_documents", lang),
                        subtitle = AppStrings.get("documents_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.DOCUMENTS) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Emergency,
                        title = AppStrings.get("tab_emergency", lang),
                        subtitle = AppStrings.get("emergency_sub", lang),
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { onSubSectionSelect(MoreSubSection.EMERGENCY) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.EmojiEvents,
                        title = AppStrings.get("tab_achievements", lang),
                        subtitle = AppStrings.get("achievements_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.ACHIEVEMENTS) }
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
                        text = AppStrings.get("app_preferences_tools", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        title = AppStrings.get("settings_title", lang),
                        subtitle = AppStrings.get("settings_sub", lang),
                        onClick = { onSubSectionSelect(MoreSubSection.SETTINGS) }
                    )

                    Divider()

                    MoreMenuItem(
                        icon = Icons.Default.Language,
                        title = AppStrings.get("language", lang),
                        subtitle = "${AppStrings.get("current_language", lang)}: ${lang.displayName}",
                        onClick = { onSubSectionSelect(MoreSubSection.SETTINGS) }
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
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
