package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.drivecare.app.data.cloud.CloudUser
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.cloud.UserProfile
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf(userProfile?.fullName ?: "") }
    var country by remember { mutableStateOf(userProfile?.country ?: "United States") }
    var preferredCurrency by remember { mutableStateOf(userProfile?.preferredCurrency ?: "USD") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.fullName
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val initialLetter = (userProfile?.fullName?.take(1) ?: currentUser?.email?.take(1) ?: "D").uppercase(Locale.getDefault())
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = userProfile?.fullName?.ifBlank { null } ?: currentUser?.displayName ?: "DriveCare User",
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

                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Edit Profile"
                        )
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = preferredCurrency,
                        onValueChange = { preferredCurrency = it },
                        label = { Text("Preferred Currency (e.g. USD, EUR, INR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val updated = UserProfile(
                                uid = currentUser?.uid ?: "",
                                fullName = fullName,
                                email = currentUser?.email ?: userProfile?.email ?: "",
                                country = country,
                                preferredCurrency = preferredCurrency,
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes")
                    }
                } else {
                    ProfileItemRow(icon = Icons.Default.Person, title = "Full Name", value = userProfile?.fullName?.ifBlank { null } ?: "Not set")
                    Divider()
                    ProfileItemRow(icon = Icons.Default.Email, title = "Email Address", value = currentUser?.email ?: "Not signed in")
                    Divider()
                    ProfileItemRow(icon = Icons.Default.Public, title = "Country", value = userProfile?.country ?: "Global")
                    Divider()
                    ProfileItemRow(icon = Icons.Default.AttachMoney, title = "Preferred Currency", value = userProfile?.preferredCurrency ?: "USD")
                    Divider()
                    ProfileItemRow(icon = Icons.Default.CalendarToday, title = "Account Creation Date", value = createdDateFormatted)
                    Divider()
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
                onClick = { viewModel.signOut() },
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
