package com.drivecare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecare.app.data.model.UserChat
import com.drivecare.app.ui.DriveCareViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    viewModel: DriveCareViewModel,
    onOpenChat: (friendUid: String, friendName: String, friendEmail: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.chats.collectAsState()
    val friendships by viewModel.friendships.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val myUid = currentUser?.uid ?: ""

    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredChats = remember(chats, searchQuery, myUid) {
        chats.filter { chat ->
            val otherUid = chat.participants.find { !it.equals(myUid, ignoreCase = true) } ?: ""
            val name = chat.participantNames[otherUid] ?: ""
            val email = chat.participantEmails[otherUid] ?: ""
            searchQuery.isBlank() ||
                name.contains(searchQuery, ignoreCase = true) ||
                email.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search chats or friends...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No chats found matching '$searchQuery'" else "No conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect with family and friends to start chatting in real time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredChats, key = { it.chatId }) { chat ->
                        UserChatCard(
                            chat = chat,
                            myUid = myUid,
                            onClick = {
                                val otherUid = chat.participants.find { !it.equals(myUid, ignoreCase = true) } ?: ""
                                val otherName = chat.participantNames[otherUid] ?: "Friend"
                                val otherEmail = chat.participantEmails[otherUid] ?: ""
                                onOpenChat(otherUid, otherName, otherEmail)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start 1-to-1 Chat", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a friend from your DriveCare network:")
                    if (friendships.isEmpty()) {
                        Text(
                            "You have no connected friends yet. Add friends under 'Family & Friends Sharing'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(friendships) { friendship ->
                                val friendUid = if (friendship.user1Uid.equals(myUid, ignoreCase = true)) friendship.user2Uid else friendship.user1Uid
                                val friendName = if (friendship.user1Uid.equals(myUid, ignoreCase = true)) friendship.user2Name else friendship.user1Name
                                val friendEmail = if (friendship.user1Uid.equals(myUid, ignoreCase = true)) friendship.user2Email else friendship.user1Email

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNewChatDialog = false
                                            onOpenChat(friendUid, friendName, friendEmail)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = friendName.ifBlank { friendEmail },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (friendEmail.isNotBlank()) {
                                                Text(
                                                    text = friendEmail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
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
fun UserChatCard(
    chat: UserChat,
    myUid: String,
    onClick: () -> Unit
) {
    val otherUid = chat.participants.find { !it.equals(myUid, ignoreCase = true) } ?: ""
    val displayName = chat.participantNames[otherUid]?.ifBlank { chat.participantEmails[otherUid] } ?: "Friend"
    val unreadCount = chat.unreadCounts[myUid] ?: 0L
    val isOnline = chat.onlineStatus[otherUid] == true
    val isTyping = chat.typingStatus[otherUid] == true

    val formattedTime = remember(chat.lastMessageTime) {
        if (chat.lastMessageTime <= 0L) ""
        else {
            val date = Date(chat.lastMessageTime)
            val now = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { time = date }
            if (now.get(Calendar.DATE) == msgCal.get(Calendar.DATE)) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unreadCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .align(Alignment.BottomEnd)
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
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTyping) "typing..." else chat.lastMessage.ifBlank { "Tap to open chat" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTyping) MaterialTheme.colorScheme.primary else if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isTyping || unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(text = "$unreadCount", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
