package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.drivecare.app.data.cloud.UserProfile
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CountryOption(
    val flag: String,
    val name: String,
    val currency: String
) {
    val displayName: String get() = "$flag $name ($currency)"
}

private val countriesList = listOf(
    CountryOption("🇵🇰", "Pakistan", "PKR"),
    CountryOption("🇺🇸", "United States", "USD"),
    CountryOption("🇸🇦", "Saudi Arabia", "SAR"),
    CountryOption("🇦🇪", "UAE", "AED"),
    CountryOption("🇬🇧", "United Kingdom", "GBP"),
    CountryOption("🇨🇦", "Canada", "CAD"),
    CountryOption("🇦🇺", "Australia", "AUD"),
    CountryOption("🇮🇳", "India", "INR"),
    CountryOption("🇩🇪", "Germany", "EUR"),
    CountryOption("🇫🇷", "France", "EUR"),
    CountryOption("🇶🇦", "Qatar", "QAR"),
    CountryOption("🇰🇼", "Kuwait", "KWD"),
    CountryOption("🇴🇲", "Oman", "OMR"),
    CountryOption("🇧🇭", "Bahrain", "BHD"),
    CountryOption("🇯🇵", "Japan", "JPY"),
    CountryOption("🇸🇬", "Singapore", "SGD"),
    CountryOption("🇲🇾", "Malaysia", "MYR"),
    CountryOption("🇹🇷", "Turkey", "TRY"),
    CountryOption("🇿🇦", "South Africa", "ZAR")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf(userProfile?.fullName ?: "") }
    var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var country by remember { mutableStateOf(userProfile?.country ?: "Pakistan") }
    var preferredCurrency by remember { mutableStateOf(userProfile?.preferredCurrency ?: "PKR") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var currencyError by remember { mutableStateOf<String?>(null) }

    var showCountryDropdown by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(countrySearchQuery) {
        if (countrySearchQuery.isBlank()) countriesList
        else countriesList.filter {
            it.name.contains(countrySearchQuery, ignoreCase = true) ||
            it.currency.contains(countrySearchQuery, ignoreCase = true) ||
            it.displayName.contains(countrySearchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.fullName
            phone = it.phone
            country = it.country
            preferredCurrency = it.preferredCurrency
        }
    }

    val createdDateFormatted = remember(userProfile?.createdAt, currentUser) {
        val ts = userProfile?.createdAt ?: currentUser?.creationTimestamp ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(ts))
    }

    val lastSyncFormatted = remember(lastSyncTime) {
        if (lastSyncTime == 0L) "Never"
        else {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(lastSyncTime))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar & Basic Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val photoUrl = userProfile?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.ifBlank { null }
                val displayNameToShow = userProfile?.fullName?.ifBlank { null }
                    ?: currentUser?.displayName?.ifBlank { null }
                    ?: currentUser?.email?.substringBefore("@")
                        ?.replace(".", " ")
                        ?.replace("-", " ")
                        ?.replace("_", " ")
                        ?.split(" ")
                        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    ?: "DriveCare User"

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initialLetter = displayNameToShow.take(1).uppercase(Locale.getDefault())
                        Text(
                            text = initialLetter,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = displayNameToShow,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentUser?.email ?: userProfile?.email ?: "Offline Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AssistChip(
                    onClick = {},
                    label = { Text(if (currentUser != null) "Cloud Account Connected" else "Local Account (Offline)") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (currentUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentUser != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.signOut(context) {
                                    onNavigateToAuth()
                                }
                                Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }

                        Button(
                            onClick = {
                                viewModel.signOut(context) {
                                    onNavigateToAuth()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Account")
                        }
                    } else {
                        Button(
                            onClick = { onNavigateToAuth() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In / Create Account")
                        }
                    }
                }
            }
        }

        // Edit Mode / Details Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNT DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            isEditing = !isEditing
                            nameError = null
                            countryError = null
                            currencyError = null
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancel editing" else "Edit Account Details"
                        )
                    }
                }

                if (isEditing) {
                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (it.isNotBlank()) nameError = null
                        },
                        label = { Text("Full Name *") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // WhatsApp / Phone Number Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp Number (e.g. +923001234567)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "WhatsApp Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Searchable Country Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showCountryDropdown,
                            onExpandedChange = { showCountryDropdown = !showCountryDropdown }
                        ) {
                            val selectedCountryObj = countriesList.find {
                                it.name.equals(country, ignoreCase = true) ||
                                (it.name == "UAE" && (country.equals("United Arab Emirates", ignoreCase = true) || country.equals("UAE", ignoreCase = true))) ||
                                (it.name == "United States" && (country.equals("USA", ignoreCase = true) || country.equals("United States", ignoreCase = true)))
                            }
                            val displayValue = selectedCountryObj?.displayName ?: if (country.isNotBlank()) country else "Select Country"

                            OutlinedTextField(
                                value = displayValue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Country *") },
                                isError = countryError != null,
                                supportingText = countryError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                leadingIcon = {
                                    val flag = selectedCountryObj?.flag ?: "🌐"
                                    Text(flag, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCountryDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = showCountryDropdown,
                                onDismissRequest = {
                                    showCountryDropdown = false
                                    countrySearchQuery = ""
                                },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                OutlinedTextField(
                                    value = countrySearchQuery,
                                    onValueChange = { countrySearchQuery = it },
                                    placeholder = { Text("Search country or currency...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search country") },
                                    trailingIcon = if (countrySearchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { countrySearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                                HorizontalDivider()

                                if (filteredCountries.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No matching countries found", color = MaterialTheme.colorScheme.outline) },
                                        onClick = {}
                                    )
                                } else {
                                    filteredCountries.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(item.flag, style = MaterialTheme.typography.titleMedium)
                                                    Text(
                                                        item.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        "(${item.currency})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                country = item.name
                                                preferredCurrency = item.currency
                                                countryError = null
                                                currencyError = null
                                                showCountryDropdown = false
                                                countrySearchQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCountryDropdown = !showCountryDropdown }
                        )
                    }

                    // Quick Country Selection Chips (FlowRow)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Quick Select Country",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickChips = listOf(
                                Triple("🇵🇰 Pakistan", "Pakistan", "PKR"),
                                Triple("🇺🇸 USA", "United States", "USD"),
                                Triple("🇸🇦 Saudi Arabia", "Saudi Arabia", "SAR"),
                                Triple("🇦🇪 UAE", "UAE", "AED")
                            )

                            quickChips.forEach { (chipText, cName, curr) ->
                                val isSelected = country.equals(cName, ignoreCase = true) ||
                                        (cName == "United States" && country.equals("USA", ignoreCase = true)) ||
                                        (cName == "UAE" && country.equals("United Arab Emirates", ignoreCase = true))

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        country = cName
                                        preferredCurrency = curr
                                        countryError = null
                                        currencyError = null
                                    },
                                    label = { Text(chipText, style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Preferred Currency Field
                    OutlinedTextField(
                        value = preferredCurrency,
                        onValueChange = {
                            preferredCurrency = it.uppercase(Locale.getDefault())
                            if (it.isNotBlank()) currencyError = null
                        },
                        label = { Text("Preferred Currency * (e.g. PKR, USD, AED, SAR)") },
                        isError = currencyError != null,
                        supportingText = currencyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Currency") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save Button
                    Button(
                        onClick = {
                            var valid = true
                            val cleanName = fullName.trim()
                            val cleanCountry = country.trim()
                            val cleanCurrency = preferredCurrency.trim()

                            if (cleanName.isBlank()) {
                                nameError = "Full Name cannot be empty"
                                valid = false
                            } else {
                                nameError = null
                            }

                            if (cleanCountry.isBlank()) {
                                countryError = "Country is required"
                                valid = false
                            } else {
                                countryError = null
                            }

                            if (cleanCurrency.isBlank()) {
                                currencyError = "Preferred currency is required"
                                valid = false
                            } else {
                                currencyError = null
                            }

                            if (!valid) {
                                Toast.makeText(context, "Please fix the errors above", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val updated = UserProfile(
                                uid = currentUser?.uid ?: userProfile?.uid ?: "",
                                fullName = cleanName,
                                email = currentUser?.email ?: userProfile?.email ?: "",
                                phone = phone.trim(),
                                country = cleanCountry,
                                preferredCurrency = cleanCurrency,
                                createdAt = userProfile?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveUserProfile(updated) { success ->
                                if (success) {
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                } else {
                                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Profile")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    ProfileItemRow(icon = Icons.Default.Person, title = "Full Name", value = userProfile?.fullName?.ifBlank { null } ?: "Not set")
                    HorizontalDivider()
                    ProfileItemRow(icon = Icons.Default.Email, title = "Email Address", value = currentUser?.email ?: userProfile?.email ?: "Not signed in")
                    HorizontalDivider()
                    ProfileItemRow(icon = Icons.Default.Phone, title = "WhatsApp / Phone", value = userProfile?.phone?.ifBlank { null } ?: "Not set")
                    HorizontalDivider()

                    val currentCountryName = userProfile?.country ?: "Pakistan"
                    val displayCountryFlag = countriesList.find {
                        it.name.equals(currentCountryName, ignoreCase = true) ||
                        (it.name == "UAE" && (currentCountryName.equals("United Arab Emirates", ignoreCase = true) || currentCountryName.equals("UAE", ignoreCase = true))) ||
                        (it.name == "United States" && (currentCountryName.equals("USA", ignoreCase = true) || currentCountryName.equals("United States", ignoreCase = true)))
                    }?.flag ?: "🌐"

                    ProfileItemRow(
                        icon = Icons.Default.Public,
                        title = "Country",
                        value = "$displayCountryFlag $currentCountryName"
                    )

                    HorizontalDivider()
                    ProfileItemRow(icon = Icons.Default.AttachMoney, title = "Preferred Currency", value = userProfile?.preferredCurrency ?: "PKR")
                    HorizontalDivider()
                    ProfileItemRow(icon = Icons.Default.CalendarToday, title = "Account Creation Date", value = createdDateFormatted)
                    HorizontalDivider()
                    ProfileItemRow(icon = Icons.Default.Sync, title = "Last Cloud Sync Time", value = lastSyncFormatted)
                }
            }
        }

        // Actions
        if (currentUser != null) {
            Button(
                onClick = { viewModel.triggerManualSync() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Data Now")
            }

            OutlinedButton(
                onClick = {
                    viewModel.signOut(context) {
                        onNavigateToAuth()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun ProfileItemRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

