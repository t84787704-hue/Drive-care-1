package com.drivecare.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.*
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.utils.FeatureFlags
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySharingScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val profiles by viewModel.driverProfiles.collectAsState()
    val legacyShares by viewModel.vehicleShares.collectAsState()
    val isFamilySharingEnabled by FeatureFlags.familySharingEnabled.collectAsState()

    val searchResults by viewModel.userSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingUsers.collectAsState()
    val incomingRequests by viewModel.incomingFriendRequests.collectAsState()
    val outgoingRequests by viewModel.outgoingFriendRequests.collectAsState()
    val friendships by viewModel.friendships.collectAsState()
    val sharedVehicles by viewModel.sharedVehiclesV2.collectAsState()
    val familyGroups by viewModel.familyGroups.collectAsState()
    val notifications by viewModel.appNotifications.collectAsState()
    val selectedPublicProfile by viewModel.selectedPublicProfile.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Search, 1: Friends, 2: Vehicle Access, 3: Family Groups, 4: Drivers & Ownership
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFamilyDialog by remember { mutableStateOf(false) }
    var showShareVehicleDialog by remember { mutableStateOf(false) }
    var selectedVehicleForShare by remember { mutableStateOf<Vehicle?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showTransferOwnershipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSocialData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Family Sharing Header
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
                        Text("Family Sharing & Fleet Multi-User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("User discovery, vehicle sharing, family access & friend requests", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = isFamilySharingEnabled,
                        onCheckedChange = { FeatureFlags.setFamilySharingEnabled(context, it) }
                    )
                }
            }
        }

        if (!isFamilySharingEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Family Sharing is turned off in settings.")
            }
        } else {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Search Users") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Friends")
                            if (incomingRequests.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text("${incomingRequests.size}") }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Vehicle Access") },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Family Groups") },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Drivers") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // PHASE 6A — USER SEARCH
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchUsers(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search users by Name or Email...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.clearUserSearch()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        if (isSearching) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    "No registered users found matching '$searchQuery'.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else if (searchResults.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Global User Discovery", fontWeight = FontWeight.SemiBold)
                                    Text("Search for friends, family, or drivers across DriveCare.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchResults) { user ->
                                    UserSearchResultCard(
                                        user = user,
                                        onViewProfile = { viewModel.fetchPublicUserProfile(user.uid) },
                                        onSendRequest = {
                                            viewModel.sendFriendRequest(user) { success, msg ->
                                                Toast.makeText(context, msg ?: if (success) "Request sent" else "Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // PHASE 6B — FRIEND REQUEST SYSTEM & FRIENDS
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (incomingRequests.isNotEmpty()) {
                            Text("Incoming Requests (${incomingRequests.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                                items(incomingRequests) { req ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.senderName.ifBlank { req.senderEmail }, fontWeight = FontWeight.Bold)
                                                Text(req.senderEmail, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        viewModel.acceptFriendRequest(req) { success, msg ->
                                                            Toast.makeText(context, msg ?: "Accepted", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Accept")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.rejectFriendRequest(req.id) {
                                                            Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Reject")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text("My Friends (${friendships.size})", fontWeight = FontWeight.Bold)
                        if (friendships.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Friends Added Yet", fontWeight = FontWeight.SemiBold)
                                    Text("Search for users in the 'Search Users' tab to send friend requests.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            val currentUid = viewModel.currentUser.collectAsState().value?.uid ?: ""
                            val activeUid = currentUid.ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(friendships) { friendship ->
                                    val isUser1Current = activeUid.isNotBlank() && friendship.user1Uid.equals(activeUid, ignoreCase = true)
                                    val isUser2Current = activeUid.isNotBlank() && friendship.user2Uid.equals(activeUid, ignoreCase = true)
                                    val friendUid = when {
                                        isUser1Current -> friendship.user2Uid
                                        isUser2Current -> friendship.user1Uid
                                        else -> friendship.user2Uid.ifBlank { friendship.user1Uid }
                                    }
                                    val friendName = when {
                                        isUser1Current -> friendship.user2Name
                                        isUser2Current -> friendship.user1Name
                                        else -> friendship.user2Name.ifBlank { friendship.user1Name }
                                    }
                                    val friendEmail = when {
                                        isUser1Current -> friendship.user2Email
                                        isUser2Current -> friendship.user1Email
                                        else -> friendship.user2Email.ifBlank { friendship.user1Email }
                                    }

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable { viewModel.fetchPublicUserProfile(friendUid) }
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                Column {
                                                    Text(friendName.ifBlank { friendEmail }, fontWeight = FontWeight.Bold)
                                                    Text(friendEmail, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = {
                                                    viewModel.openChat(friendUid, friendName, friendEmail)
                                                }) {
                                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { viewModel.fetchPublicUserProfile(friendUid) }) {
                                                    Icon(Icons.Default.AccountBox, contentDescription = "Public Profile")
                                                }
                                                IconButton(onClick = {
                                                    viewModel.removeFriendship(friendship.id) {
                                                        Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PHASE 6C — VEHICLE SHARING & PERMISSIONS
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shared Vehicles (${sharedVehicles.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showShareVehicleDialog = true }, enabled = vehicles.isNotEmpty()) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Vehicle")
                            }
                        }

                        if (sharedVehicles.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Shared Vehicles Active", fontWeight = FontWeight.SemiBold)
                                    Text("Grant Viewer, Editor, or Manager access to your vehicles.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sharedVehicles) { sv ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(sv.vehicleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                    Text("Shared with: ${sv.sharedWithName} (${sv.sharedWithEmail})", style = MaterialTheme.typography.bodySmall)
                                                    Text("Owner: ${sv.ownerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                                }
                                                IconButton(onClick = {
                                                    viewModel.revokeVehicleShare(sv.id) {
                                                        Toast.makeText(context, "Share revoked", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Permission:", style = MaterialTheme.typography.labelMedium)
                                                listOf("Viewer", "Editor", "Manager").forEach { perm ->
                                                    FilterChip(
                                                        selected = sv.permission.equals(perm, ignoreCase = true),
                                                        onClick = {
                                                            viewModel.updateVehicleSharePermission(sv.id, perm) {
                                                                Toast.makeText(context, "Permission updated to $perm", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        label = { Text(perm) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // PHASE 6D — FAMILY ACCESS & GROUPS
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Family Groups (${familyGroups.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showCreateFamilyDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Family Group")
                            }
                        }

                        if (familyGroups.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Family Groups Created", fontWeight = FontWeight.SemiBold)
                                    Text("Create a family group to grant collective vehicle access to family members.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(familyGroups) { group ->
                                    FamilyGroupCard(group = group, viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // DRIVER PROFILES & OWNERSHIP TRANSFER (Legacy Compatible)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Registered Drivers (${profiles.size})", fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddProfileDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Driver")
                            }
                        }

                        if (profiles.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Driver Profiles Yet", fontWeight = FontWeight.SemiBold)
                                    Text("Add authorized drivers for vehicle logging.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(profiles) { profile ->
                                    DriverProfileCard(profile = profile, onDelete = { viewModel.deleteDriverProfile(profile) })
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Vehicle Ownership Transfer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Transfer master title registration and vehicle history to another registered driver profile or email address.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showTransferOwnershipDialog = true },
                                    enabled = vehicles.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Initiate Ownership Transfer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Public Profile Modal
    selectedPublicProfile?.let { publicProfile ->
        PublicProfileDialog(
            profile = publicProfile,
            onDismiss = { viewModel.clearSelectedPublicProfile() },
            onSendRequest = {
                viewModel.sendFriendRequest(publicProfile) { success, msg ->
                    Toast.makeText(context, msg ?: if (success) "Sent" else "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Dialogs
    if (showCreateFamilyDialog) {
        CreateFamilyGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateFamilyDialog = false }
        )
    }

    if (showShareVehicleDialog) {
        ShareVehicleDialogV2(
            viewModel = viewModel,
            vehicles = vehicles,
            friends = friendships,
            onDismiss = { showShareVehicleDialog = false }
        )
    }

    if (showAddProfileDialog) {
        AddDriverProfileDialog(
            viewModel = viewModel,
            onDismiss = { showAddProfileDialog = false }
        )
    }

    if (showTransferOwnershipDialog) {
        TransferOwnershipDialog(
            viewModel = viewModel,
            vehicles = vehicles,
            profiles = profiles,
            onDismiss = { showTransferOwnershipDialog = false }
        )
    }
}

@Composable
fun UserSearchResultCard(
    user: PublicUserProfile,
    onViewProfile: () -> Unit,
    onSendRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewProfile() }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(user.displayName, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    if (user.country.isNotBlank()) {
                        Text("${user.country} • ${user.preferredCurrency}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Row {
                IconButton(onClick = onViewProfile) {
                    Icon(Icons.Default.AccountBox, contentDescription = "View Profile")
                }
                IconButton(onClick = onSendRequest) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun FamilyGroupCard(
    group: FamilyGroup,
    viewModel: DriveCareViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInviteDialog by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }

    LaunchedEffect(group.id) {
        members = viewModel.syncManager.getFamilyMembers(group.id)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(group.groupName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Owner: ${group.ownerName}", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { showInviteDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invite Member")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Members (${members.size})", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)

            members.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${member.name} (${member.role})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("${member.email} • ${member.permission} [${member.status}]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (member.role != "Owner") {
                        IconButton(onClick = {
                            viewModel.removeFamilyMember(group.id, member.id) {
                                members = members.filter { it.id != member.id }
                                Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        InviteFamilyMemberDialog(
            groupId = group.id,
            groupName = group.groupName,
            viewModel = viewModel,
            onDismiss = {
                showInviteDialog = false
                scope.launch {
                    members = viewModel.syncManager.getFamilyMembers(group.id)
                }
            }
        )
    }
}

@Composable
fun PublicProfileDialog(
    profile: PublicUserProfile,
    onDismiss: () -> Unit,
    onSendRequest: () -> Unit
) {
    val dateStr = if (profile.joinDate > 0) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(profile.joinDate))
    } else "Recent"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(profile.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Public Profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Email:", fontWeight = FontWeight.SemiBold)
                    Text(profile.email)
                }
                if (profile.country.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Country:", fontWeight = FontWeight.SemiBold)
                        Text(profile.country)
                    }
                }
                if (profile.preferredCurrency.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Currency:", fontWeight = FontWeight.SemiBold)
                        Text(profile.preferredCurrency)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Member Since:", fontWeight = FontWeight.SemiBold)
                    Text(dateStr)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Registered Vehicles:", fontWeight = FontWeight.SemiBold)
                    Text("${profile.vehicleCount}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSendRequest()
                onDismiss()
            }) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Friend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateFamilyGroupDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Family Access Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Family Group Name") },
                    placeholder = { Text("e.g., Khan Family Fleet") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (groupName.isNotBlank()) {
                    viewModel.createFamilyGroup(groupName) { success, msg ->
                        Toast.makeText(context, msg ?: if (success) "Group created" else "Failed", Toast.LENGTH_SHORT).show()
                        if (success) onDismiss()
                    }
                }
            }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFamilyMemberDialog(
    groupId: String,
    groupName: String,
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Member") }
    var permission by remember { mutableStateOf("Viewer") }

    val roles = listOf("Father", "Mother", "Son", "Daughter", "Member")
    val permissions = listOf("Viewer", "Editor", "Manager")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Family Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Member Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Family Role:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    roles.take(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Access Permission:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    permissions.forEach { p ->
                        FilterChip(
                            selected = permission == p,
                            onClick = { permission = p },
                            label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isNotBlank()) {
                    viewModel.inviteFamilyMember(groupId, groupName, email, role, permission) { success, msg ->
                        Toast.makeText(context, msg ?: if (success) "Invited" else "Failed", Toast.LENGTH_SHORT).show()
                        if (success) onDismiss()
                    }
                }
            }) {
                Text("Send Invite")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareVehicleDialogV2(
    viewModel: DriveCareViewModel,
    vehicles: List<Vehicle>,
    friends: List<Friendship>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    var recipientEmail by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf("Viewer") }

    val currentUid = viewModel.currentUser.collectAsState().value?.uid ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Vehicle Access", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.vehicleName) },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text("Recipient Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (friends.isNotEmpty()) {
                    Text("Or Select Friend:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(friends) { f ->
                            val isUser1Current = currentUid.isNotBlank() && f.user1Uid.equals(currentUid, ignoreCase = true)
                            val fEmail = if (isUser1Current) f.user2Email else f.user1Email
                            TextButton(onClick = { recipientEmail = fEmail }) {
                                Text(fEmail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Text("Permission Level:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Viewer", "Editor", "Manager").forEach { perm ->
                        FilterChip(
                            selected = permission == perm,
                            onClick = { permission = perm },
                            label = { Text(perm) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = selectedVehicle
                if (v != null && recipientEmail.isNotBlank()) {
                    viewModel.shareVehicleWithUser(v, "", recipientEmail, recipientEmail.substringBefore("@"), permission) { success, msg ->
                        Toast.makeText(context, msg ?: if (success) "Shared" else "Failed", Toast.LENGTH_SHORT).show()
                        if (success) onDismiss()
                    }
                }
            }) {
                Text("Share")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DriverProfileCard(profile: DriverProfile, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(profile.name, fontWeight = FontWeight.Bold)
                    Text(profile.email.ifBlank { profile.phone.ifBlank { "Driver Profile" } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (profile.licenseNumber.isNotBlank()) {
                        Text("License: ${profile.licenseNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddDriverProfileDialog(
    viewModel: DriveCareViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Driver Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("Driver License #") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addDriverProfile(
                            DriverProfile(
                                name = name,
                                email = email,
                                phone = phone,
                                licenseNumber = license
                            )
                        )
                        Toast.makeText(context, "Driver Profile Created!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferOwnershipDialog(
    viewModel: DriveCareViewModel,
    vehicles: List<Vehicle>,
    profiles: List<DriverProfile>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var expandedVehicleDropdown by remember { mutableStateOf(false) }
    var newOwnerName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Vehicle Ownership", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedVehicleDropdown,
                    onExpandedChange = { expandedVehicleDropdown = !expandedVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle?.vehicleName ?: "Select Vehicle",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle to Transfer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVehicleDropdown,
                        onDismissRequest = { expandedVehicleDropdown = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.vehicleName) },
                                onClick = {
                                    selectedVehicle = v
                                    expandedVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newOwnerName,
                    onValueChange = { newOwnerName = it },
                    label = { Text("New Owner Name / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle != null && newOwnerName.isNotBlank()) {
                        viewModel.transferVehicleOwnership(selectedVehicle!!.id, newOwnerName)
                        Toast.makeText(context, "Ownership transferred to $newOwnerName!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
