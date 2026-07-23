package com.drivecare.app.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DriveCareNotificationScheduler

data class CurrencyOption(val symbol: String, val name: String)

val currencyList = listOf(
    CurrencyOption("$", "USD ($) - US Dollar"),
    CurrencyOption("€", "EUR (€) - Euro"),
    CurrencyOption("£", "GBP (£) - British Pound"),
    CurrencyOption("₹", "INR (₹) - Indian Rupee"),
    CurrencyOption("CA$", "CAD (CA$) - Canadian Dollar"),
    CurrencyOption("AU$", "AUD (AU$) - Australian Dollar"),
    CurrencyOption("¥", "JPY (¥) - Japanese Yen"),
    CurrencyOption("R$", "BRL (R$) - Brazilian Real")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val currencySymbol by viewModel.currentCurrencySymbol.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val notifyService by viewModel.notifyService.collectAsState()
    val notifyInsurance by viewModel.notifyInsurance.collectAsState()
    val notifyDocuments by viewModel.notifyDocuments.collectAsState()
    val notifyExpenses by viewModel.notifyExpenses.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var backupJsonText by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }

    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = AppStrings.get("settings_title", currentLang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Cloud Account & Backup Section
        val currentUser by viewModel.currentUser.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        val syncState by viewModel.syncState.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cloud Account & Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentUser != null) "Account: ${currentUser?.email}" else "Account: Local (Offline)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Last Sync: ${viewModel.syncManager.formattedLastSync()} • Status: ${syncState.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AssistChip(
                        onClick = { viewModel.triggerManualSync() },
                        label = { Text("Sync Now") },
                        leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (currentUser == null) {
                    Text(
                        text = "Sign in to backup your vehicles, fuel entries, maintenance, expenses, documents, and reminders securely in the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Regional & Display Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Regional & Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                // Currency Selection Dropdown
                Text("Currency Symbol", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded }
                ) {
                    val currentCurrencyName = currencyList.find { it.symbol == currencySymbol }?.name ?: "USD ($)"
                    OutlinedTextField(
                        value = currentCurrencyName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        currencyList.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    viewModel.setCurrencySymbol(item.symbol)
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Language Selection Dropdown
                Text(AppStrings.get("language", currentLang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = languageDropdownExpanded,
                    onExpandedChange = { languageDropdownExpanded = !languageDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currentLang.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false }
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    languageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Theme Mode Preferences
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "App Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == "SYSTEM",
                        onClick = { viewModel.setThemeMode("SYSTEM") },
                        label = { Text("System Default") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeMode == "LIGHT",
                        onClick = { viewModel.setThemeMode("LIGHT") },
                        label = { Text("Light Mode") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeMode == "DARK",
                        onClick = { viewModel.setThemeMode("DARK") },
                        label = { Text("Dark Mode") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Android System Notification Engine Preferences
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Notification & Reminder Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "DriveCare uses WorkManager to deliver system alerts for service due dates, expiring insurance policies, and documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Service Due Reminders", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifyService,
                        onCheckedChange = { viewModel.setNotificationPreference("service", it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Insurance Expiry Alerts", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifyInsurance,
                        onCheckedChange = { viewModel.setNotificationPreference("insurance", it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Document Renewal Warnings", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifyDocuments,
                        onCheckedChange = { viewModel.setNotificationPreference("documents", it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expense & Refuel Alerts", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifyExpenses,
                        onCheckedChange = { viewModel.setNotificationPreference("expenses", it) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        DriveCareNotificationScheduler.triggerImmediateCheck(context)
                        Toast.makeText(context, "Notification check executed!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Notification Check Now")
                }
            }
        }

        // 4. Backup, Restore & Data Preferences
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Data Backup & Recovery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            backupJsonText = viewModel.exportBackupJson()
                            showBackupDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore JSON")
                    }
                }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Local Data (Reset)")
                }
            }
        }

        // 5. Feature Guides Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = AppStrings.get("reset_feature_guides", currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = AppStrings.get("reset_feature_guides_desc", currentLang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                OutlinedButton(
                    onClick = {
                        com.drivecare.app.utils.FeatureGuideManager.resetAllGuides(context)
                        Toast.makeText(
                            context,
                            AppStrings.get("guides_reset_toast", currentLang),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("reset_feature_guides", currentLang))
                }
            }
        }

        // About DriveCare
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStrings.get("app_name", currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("DriveCare Core Edition v2.0.0", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Complete vehicle care platform with Room DB, WorkManager notifications, Insurance Renewal tracking, and multi-currency support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Export Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(AppStrings.get("backup_data", currentLang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy your JSON backup string:")
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(backupJsonText))
                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(AppStrings.get("cancel", currentLang))
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your DriveCare JSON backup string below:")
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
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
                    Text("Restore Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(AppStrings.get("cancel", currentLang))
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently erase all local vehicle records, fuel logs, maintenance history, and insurance policies.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            Toast.makeText(context, "Local database reset successfully", Toast.LENGTH_SHORT).show()
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(AppStrings.get("cancel", currentLang))
                }
            }
        )
    }
}
