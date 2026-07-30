package com.drivecare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivecare.app.data.model.Conversation
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onOpenChat: (friendUid: String, friendName: String, friendEmail: String) -> Unit = { _, _, _ -> }
) {
    val conversations by viewModel.conversations.collectAsState()
    val friendships by viewModel.friendships.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val currentUid = currentUser?.uid ?: ""
    val activeUid = currentUid.ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, searchQuery, activeUid) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                val otherUid = conv.participants.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
                    ?: conv.participantNames.keys.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
                    ?: conv.participants.firstOrNull { !it.equals(activeUid, ignoreCase = true) }
                    ?: ""
                val otherName = conv.participantNames[otherUid] ?: ""
                val otherEmail = conv.participantEmails[otherUid] ?: ""
                otherName.contains(searchQuery, ignoreCase = true) ||
                        otherEmail.contains(searchQuery, ignoreCase = true) ||
                        conv.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Direct Messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instant 1-on-1 chat with connected friends & family",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { showNewChatDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Chat")
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search conversations by friend name or message...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Conversations List
        if (filteredConversations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Active Chats", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap 'New Chat' above or go to Friends to start a conversation with any connected friend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredConversations, key = { it.conversationId }) { conversation ->
                    ConversationItemCard(
                        conversation = conversation,
                        currentUid = activeUid,
                        onClick = {
                            val otherUid = conversation.participants.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
                                ?: conversation.participantNames.keys.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
                                ?: conversation.participants.firstOrNull { !it.equals(activeUid, ignoreCase = true) }
                                ?: ""
                            val otherName = conversation.participantNames[otherUid] ?: ""
                            val otherEmail = conversation.participantEmails[otherUid] ?: ""
                            onOpenChat(otherUid, otherName, otherEmail)
                        }
                    )
                }
            }
        }
    }

    // New Chat Dialog with Friends
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Select Friend to Message", fontWeight = FontWeight.Bold) },
            text = {
                if (friendships.isEmpty()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("You don't have any added friends yet.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Search users in Family Sharing to send friend requests first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNewChatDialog = false
                                        onOpenChat(friendUid, friendName, friendEmail)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(friendName.ifBlank { friendEmail }, fontWeight = FontWeight.Bold)
                                        Text(friendEmail, style = MaterialTheme.typography.bodySmall)
                                    }

                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ConversationItemCard(
    conversation: Conversation,
    currentUid: String,
    onClick: () -> Unit
) {
    val activeUid = currentUid.ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val otherUid = conversation.participants.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
        ?: conversation.participantNames.keys.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
        ?: conversation.participantEmails.keys.find { activeUid.isNotBlank() && !it.equals(activeUid, ignoreCase = true) }
        ?: conversation.participants.firstOrNull { !it.equals(activeUid, ignoreCase = true) }
        ?: ""
    val otherName = conversation.participantNames[otherUid] ?: ""
    val otherEmail = conversation.participantEmails[otherUid] ?: ""
    val unreadCount = conversation.unreadCounts[activeUid] ?: 0L

    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(conversation.lastMessageTimestamp) {
        if (conversation.lastMessageTimestamp > 0L) timeFormat.format(Date(conversation.lastMessageTimestamp)) else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherName.ifBlank { otherEmail },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = conversation.lastMessage.ifBlank { "No messages yet" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text("$unreadCount", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
