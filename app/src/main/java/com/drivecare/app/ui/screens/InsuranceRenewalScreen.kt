package com.drivecare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.InsurancePolicy
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceRenewalScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    highlightRecordId: Long? = null
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val insurancePolicies by viewModel.insurancePolicies.collectAsState()
    val currencySymbol by viewModel.currentCurrencySymbol.collectAsState()

    var selectedVehicleFilter by remember { mutableStateOf<Long?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, EXPIRING, EXPIRED

    var showAddDialog by remember { mutableStateOf(false) }
    var policyToEdit by remember { mutableStateOf<InsurancePolicy?>(null) }
    var policyToRenew by remember { mutableStateOf<InsurancePolicy?>(null) }
    var policyToDelete by remember { mutableStateOf<InsurancePolicy?>(null) }

    LaunchedEffect(highlightRecordId, insurancePolicies) {
        if (highlightRecordId != null) {
            val matchingPolicy = insurancePolicies.find { it.id == highlightRecordId }
            if (matchingPolicy != null) {
                selectedStatusFilter = "ALL"
                selectedVehicleFilter = null
                policyToRenew = matchingPolicy
            }
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val warn30DaysStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    // Filter policies
    val filteredPolicies = remember(insurancePolicies, selectedVehicleFilter, selectedStatusFilter) {
        insurancePolicies.filter { p ->
            (selectedVehicleFilter == null || p.vehicleId == selectedVehicleFilter) &&
                    when (selectedStatusFilter) {
                        "ACTIVE" -> p.expiryDate.isBlank() || p.expiryDate >= warn30DaysStr
                        "EXPIRING" -> p.expiryDate in todayStr..warn30DaysStr
                        "EXPIRED" -> p.expiryDate.isNotBlank() && p.expiryDate < todayStr
                        else -> true
                    }
        }
    }

    val activeCount = remember(insurancePolicies) {
        insurancePolicies.count { it.expiryDate.isBlank() || it.expiryDate >= warn30DaysStr }
    }
    val expiringCount = remember(insurancePolicies) {
        insurancePolicies.count { it.expiryDate in todayStr..warn30DaysStr }
    }
    val expiredCount = remember(insurancePolicies) {
        insurancePolicies.count { it.expiryDate.isNotBlank() && it.expiryDate < todayStr }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vehicles.isEmpty()) {
                        Toast.makeText(context, "Please add a vehicle first in Garage", Toast.LENGTH_SHORT).show()
                    } else {
                        policyToEdit = null
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Insurance Policy")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("insurance_policies_title", lang),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = AppStrings.get("insurance_policies_sub", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Metric Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsuranceMetricCard(
                        title = AppStrings.get("status_active", lang),
                        count = activeCount,
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    InsuranceMetricCard(
                        title = AppStrings.get("status_expiring_soon", lang),
                        count = expiringCount,
                        containerColor = Color(0xFFFFF8E1),
                        contentColor = Color(0xFFF57F17),
                        modifier = Modifier.weight(1f)
                    )
                    InsuranceMetricCard(
                        title = AppStrings.get("status_expired", lang),
                        count = expiredCount,
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" },
                        label = { Text("${AppStrings.get("filter_all", lang)} (${insurancePolicies.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "ACTIVE",
                        onClick = { selectedStatusFilter = "ACTIVE" },
                        label = { Text("${AppStrings.get("status_active", lang)} ($activeCount)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "EXPIRING",
                        onClick = { selectedStatusFilter = "EXPIRING" },
                        label = { Text("${AppStrings.get("filter_expiring", lang)} ($expiringCount)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "EXPIRED",
                        onClick = { selectedStatusFilter = "EXPIRED" },
                        label = { Text("${AppStrings.get("status_expired", lang)} ($expiredCount)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            // Vehicle Filter Dropdown
            if (vehicles.isNotEmpty()) {
                item {
                    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
                    val currentVehicleName = remember(selectedVehicleFilter, vehicles) {
                        vehicles.find { it.id == selectedVehicleFilter }?.vehicleName ?: "All Vehicles"
                    }

                    ExposedDropdownMenuBox(
                        expanded = vehicleDropdownExpanded,
                        onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Vehicle: $currentVehicleName",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Vehicles") },
                                onClick = {
                                    selectedVehicleFilter = null
                                    vehicleDropdownExpanded = false
                                }
                            )
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.vehicleName} (${v.brand} ${v.model})") },
                                    onClick = {
                                        selectedVehicleFilter = v.id
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Policy List
            if (filteredPolicies.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Insurance Policies Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the + button below to add your vehicle insurance policy and set renewal reminders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredPolicies, key = { it.id }) { policy ->
                        InsurancePolicyCard(
                            policy = policy,
                            currencySymbol = currencySymbol,
                            todayStr = todayStr,
                            warn30DaysStr = warn30DaysStr,
                            onRenew = { policyToRenew = policy },
                            onEdit = {
                                policyToEdit = policy
                                showAddDialog = true
                            },
                            onDelete = { policyToDelete = policy },
                            onCallAgent = {
                                if (policy.agentContact.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${policy.agentContact}"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "No agent phone number provided", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            }
        }
    }

    // Add / Edit Policy Dialog
    if (showAddDialog) {
        AddEditInsuranceDialog(
            vehicles = vehicles,
            editingPolicy = policyToEdit,
            currencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { policy ->
                if (policyToEdit == null) {
                    viewModel.addInsurancePolicy(policy)
                    Toast.makeText(context, "Insurance Policy Added!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateInsurancePolicy(policy)
                    Toast.makeText(context, "Policy Updated!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    // Renew Policy Dialog
    policyToRenew?.let { policy ->
        RenewPolicyDialog(
            policy = policy,
            currencySymbol = currencySymbol,
            onDismiss = { policyToRenew = null },
            onConfirmRenew = { newStart, newExpiry, newPremium ->
                viewModel.renewInsurancePolicy(policy, newStart, newExpiry, newPremium)
                Toast.makeText(context, "Policy renewed and expense recorded!", Toast.LENGTH_SHORT).show()
                policyToRenew = null
            }
        )
    }

    // Delete Policy Dialog
    policyToDelete?.let { policy ->
        AlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = { Text("Delete Insurance Policy") },
            text = { Text("Are you sure you want to delete policy #${policy.policyNumber} for ${policy.vehicleName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInsurancePolicy(policy)
                        Toast.makeText(context, "Policy Deleted", Toast.LENGTH_SHORT).show()
                        policyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { policyToDelete = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun InsuranceMetricCard(
    title: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
fun InsurancePolicyCard(
    policy: InsurancePolicy,
    currencySymbol: String,
    todayStr: String,
    warn30DaysStr: String,
    onRenew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCallAgent: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isExpired = policy.expiryDate.isNotBlank() && policy.expiryDate < todayStr
    val isExpiringSoon = !isExpired && policy.expiryDate.isNotBlank() && policy.expiryDate <= warn30DaysStr

    val statusBg = when {
        isExpired -> Color(0xFFFFEBEE)
        isExpiringSoon -> Color(0xFFFFF8E1)
        else -> Color(0xFFE8F5E9)
    }
    val statusFg = when {
        isExpired -> Color(0xFFC62828)
        isExpiringSoon -> Color(0xFFF57F17)
        else -> Color(0xFF2E7D32)
    }
    val statusText = when {
        isExpired -> AppStrings.get("status_expired", lang)
        isExpiringSoon -> AppStrings.get("status_expiring_soon", lang)
        else -> AppStrings.get("status_active", lang)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Provider & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = policy.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${AppStrings.get("policy_no_prefix", lang)} ${policy.policyNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            Divider()

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(AppStrings.get("vehicle_linked", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(policy.vehicleName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column {
                    Text(AppStrings.get("coverage_type", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(policy.coverageType, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column {
                    Text(AppStrings.get("premium", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$currencySymbol${policy.premiumAmount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Validity Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Valid: ${policy.startDate.ifEmpty { "N/A" }}  ➔  ${policy.expiryDate.ifEmpty { "N/A" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (policy.isAutoRenewEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Auto-Renew On",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (policy.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${policy.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRenew,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpired || isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Renew")
                    }

                    if (policy.agentContact.isNotBlank()) {
                        OutlinedButton(onClick = onCallAgent) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agent")
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Policy")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Policy", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInsuranceDialog(
    vehicles: List<Vehicle>,
    editingPolicy: InsurancePolicy?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (InsurancePolicy) -> Unit
) {
    var selectedVehicle by remember { mutableStateOf(vehicles.find { it.id == editingPolicy?.vehicleId } ?: vehicles.firstOrNull()) }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }

    var providerName by remember { mutableStateOf(editingPolicy?.providerName ?: "") }
    var policyNumber by remember { mutableStateOf(editingPolicy?.policyNumber ?: "") }
    var coverageType by remember { mutableStateOf(editingPolicy?.coverageType ?: "Comprehensive") }
    var premiumAmount by remember { mutableStateOf(editingPolicy?.premiumAmount?.toString() ?: "0.0") }
    var startDate by remember { mutableStateOf(editingPolicy?.startDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var expiryDate by remember {
        mutableStateOf(editingPolicy?.expiryDate ?: run {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, 1)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        })
    }
    var agentContact by remember { mutableStateOf(editingPolicy?.agentContact ?: "") }
    var notes by remember { mutableStateOf(editingPolicy?.notes ?: "") }
    var isAutoRenewEnabled by remember { mutableStateOf(editingPolicy?.isAutoRenewEnabled ?: false) }

    var coverageDropdownExpanded by remember { mutableStateOf(false) }
    val coverageOptions = listOf("Comprehensive", "Third-Party Liability", "Collision & Theft", "Full Coverage", "Zero Depreciation")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editingPolicy == null) "Add Insurance Policy" else "Edit Insurance Policy",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Vehicle Selection
                    ExposedDropdownMenuBox(
                        expanded = vehicleDropdownExpanded,
                        onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle Linked") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.vehicleName) },
                                    onClick = {
                                        selectedVehicle = v
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Insurance Provider Name") },
                        placeholder = { Text("e.g. Geico, Progressive, Allianz, AXA") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = policyNumber,
                        onValueChange = { policyNumber = it },
                        label = { Text("Policy Number") },
                        placeholder = { Text("e.g. POL-9982410") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = coverageDropdownExpanded,
                        onExpandedChange = { coverageDropdownExpanded = !coverageDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = coverageType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Coverage Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coverageDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = coverageDropdownExpanded,
                            onDismissRequest = { coverageDropdownExpanded = false }
                        ) {
                            coverageOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        coverageType = opt
                                        coverageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = premiumAmount,
                        onValueChange = { premiumAmount = it },
                        label = { Text("Premium Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Expiry Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = agentContact,
                        onValueChange = { agentContact = it },
                        label = { Text("Agent Phone / Hotline") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        placeholder = { Text("e.g. +1-800-555-0199") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Deductible Info") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Auto-Renew Flag", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isAutoRenewEnabled,
                            onCheckedChange = { isAutoRenewEnabled = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = selectedVehicle ?: return@Button
                    val p = InsurancePolicy(
                        id = editingPolicy?.id ?: 0,
                        vehicleId = v.id,
                        vehicleName = v.vehicleName,
                        providerName = providerName.ifBlank { "Insurance Provider" },
                        policyNumber = policyNumber.ifBlank { "POL-${System.currentTimeMillis() % 100000}" },
                        coverageType = coverageType,
                        premiumAmount = premiumAmount.toDoubleOrNull() ?: 0.0,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        agentContact = agentContact,
                        notes = notes,
                        isAutoRenewEnabled = isAutoRenewEnabled
                    )
                    onSave(p)
                }
            ) {
                Text(if (editingPolicy == null) "Save Policy" else "Update Policy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenewPolicyDialog(
    policy: InsurancePolicy,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmRenew: (newStart: String, newExpiry: String, newPremium: Double) -> Unit
) {
    var newStart by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var newExpiry by remember {
        mutableStateOf(run {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, 1)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        })
    }
    var newPremium by remember { mutableStateOf(policy.premiumAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renew Policy #${policy.policyNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vehicle: ${policy.vehicleName} (${policy.providerName})", style = MaterialTheme.typography.bodyMedium)
                Text("Renewing this policy will automatically update the policy period and log a new Expense record.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = newStart,
                    onValueChange = { newStart = it },
                    label = { Text("New Start Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newExpiry,
                    onValueChange = { newExpiry = it },
                    label = { Text("New Expiry Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPremium,
                    onValueChange = { newPremium = it },
                    label = { Text("Renewal Premium Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmRenew(newStart, newExpiry, newPremium.toDoubleOrNull() ?: 0.0)
                }
            ) {
                Text("Confirm Renewal & Log Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
