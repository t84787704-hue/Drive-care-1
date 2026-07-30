package com.drivecare.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.drivecare.app.data.model.ChatMessage
import com.drivecare.app.ui.DriveCareViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.activeChatMessages.collectAsState()
    val friendUid by viewModel.activeChatFriendUid.collectAsState()
    val friendName by viewModel.activeChatFriendName.collectAsState()
    val friendEmail by viewModel.activeChatFriendEmail.collectAsState()
    val friendPhotoUrl by viewModel.activeChatFriendPhotoUrl.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()
    val friendPresence by viewModel.activeFriendPresence.collectAsState()

    var inputMessageText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var selectedImageForPreview by remember { mutableStateOf<ImageBitmap?>(null) }

    val currentUid = (currentUser?.uid ?: "").ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // Detect IME (soft keyboard) bottom padding changes
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // Timer for voice recording state
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (originalBitmap != null) {
                        // Scale bitmap to reasonable size for real-time Firestore chat payload
                        val maxDimension = 800
                        val width = originalBitmap.width
                        val height = originalBitmap.height
                        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                            val ratio = width.toFloat() / height.toFloat()
                            if (ratio > 1) {
                                android.graphics.Bitmap.createScaledBitmap(originalBitmap, maxDimension, (maxDimension / ratio).toInt(), true)
                            } else {
                                android.graphics.Bitmap.createScaledBitmap(originalBitmap, (maxDimension * ratio).toInt(), maxDimension, true)
                            }
                        } else {
                            originalBitmap
                        }

                        val outputStream = ByteArrayOutputStream()
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val byteArray = outputStream.toByteArray()
                        val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                        val imagePayload = "📷 Image [IMAGE]data:image/jpeg;base64,$base64Str"
                        viewModel.sendMessage(imagePayload) { success, errorMsg ->
                            if (!success) {
                                Toast.makeText(context, errorMsg ?: "Failed to send image", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Mark messages as read on entry and when friendUid changes
    LaunchedEffect(friendUid) {
        if (friendUid != null) {
            viewModel.markActiveChatAsRead()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, imeBottomPadding) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (friendPhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = friendPhotoUrl,
                                    contentDescription = "Friend Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initial = friendName.ifBlank { friendEmail }.take(1).uppercase(Locale.getDefault())
                                if (initial.isNotBlank() && initial != "F") {
                                    Text(
                                        text = initial,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = friendName.ifBlank { friendEmail.ifBlank { "Friend Chat" } },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isFriendTyping = friendPresence.typingToUserId == currentUid && currentUid.isNotBlank()
                            val statusText = when {
                                isFriendTyping -> "${friendName.ifBlank { "Friend" }} is typing..."
                                friendPresence.isOnline -> "Online"
                                else -> formatLastSeen(friendPresence.lastSeen)
                            }
                            val statusDotColor = when {
                                isFriendTyping || friendPresence.isOnline -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                else -> androidx.compose.ui.graphics.Color.Gray
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFriendTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isFriendTyping) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRecordingVoice) {
                        // Voice Recording Controls Bar
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )

                        IconButton(
                            onClick = { isRecordingVoice = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Recording", tint = MaterialTheme.colorScheme.error)
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "Recording Audio... 0:${if (recordingSeconds < 10) "0" else ""}$recordingSeconds",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        IconButton(
                            onClick = {
                                val duration = "0:${if (recordingSeconds < 10) "0" else ""}$recordingSeconds"
                                isRecordingVoice = false
                                val voicePayload = "🎤 Voice Message ($duration) [VOICE]$duration"
                                viewModel.sendMessage(voicePayload) { success, errorMsg ->
                                    if (!success) {
                                        Toast.makeText(context, errorMsg ?: "Failed to send voice message", Toast.LENGTH_SHORT).show()
                                    } else {
                                        coroutineScope.launch {
                                            if (messages.isNotEmpty()) {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Voice Message", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        // Image Attachment Button
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Send Photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Text Message Input
                        OutlinedTextField(
                            value = inputMessageText,
                            onValueChange = { newText ->
                                inputMessageText = newText
                                viewModel.setTypingStatus(newText.isNotBlank())
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        if (inputMessageText.isNotBlank()) {
                            // Send Text Message Button
                            IconButton(
                                onClick = {
                                    if (inputMessageText.isNotBlank() && !isSending) {
                                        val textToSend = inputMessageText
                                        inputMessageText = ""
                                        viewModel.setTypingStatus(false)
                                        viewModel.sendMessage(textToSend) { success, errorMsg ->
                                            if (!success) {
                                                Toast.makeText(context, errorMsg ?: "Failed to send message", Toast.LENGTH_SHORT).show()
                                                inputMessageText = textToSend
                                            } else {
                                                coroutineScope.launch {
                                                    if (messages.isNotEmpty()) {
                                                        listState.animateScrollToItem(messages.size - 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = inputMessageText.isNotBlank() && !isSending,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            // Microphone Button to trigger Voice Recording
                            IconButton(
                                onClick = { isRecordingVoice = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start a Conversation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send real-time text, images, or voice notes to ${friendName.ifBlank { friendEmail }}.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId.ifBlank { it.timestamp.toString() } }) { message ->
                        ChatMessageBubble(
                            message = message,
                            isFromMe = message.senderUid == currentUid,
                            onImageClick = { bitmap -> selectedImageForPreview = bitmap }
                        )
                    }
                }
            }
        }
    }

    // Image Fullscreen Preview Dialog
    selectedImageForPreview?.let { bitmap ->
        Dialog(onDismissRequest = { selectedImageForPreview = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Photo Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedImageForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Full photo preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    onImageClick: (ImageBitmap) -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) {
        if (message.timestamp > 0L) timeFormat.format(Date(message.timestamp)) else ""
    }

    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    // Parse image or voice payloads if present
    val isImageMsg = message.messageText.contains("[IMAGE]") || message.messageText.startsWith("data:image/")
    val isVoiceMsg = message.messageText.contains("[VOICE]") || message.messageText.contains("🎤 Voice Message")

    // Image bitmap decoding memory cache
    val decodedBitmap = remember(message.messageText) {
        if (isImageMsg) {
            try {
                val base64Index = message.messageText.indexOf("base64,")
                if (base64Index != -1) {
                    val cleanBase64 = message.messageText.substring(base64Index + 7).trim()
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = bubbleShape,
            color = if (isFromMe) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when {
                    decodedBitmap != null -> {
                        // Render Image Message Bubble
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(decodedBitmap) }
                        ) {
                            Image(
                                bitmap = decodedBitmap,
                                contentDescription = "Sent Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    isVoiceMsg -> {
                        // Render Voice Audio Message Bubble
                        var isPlaying by remember { mutableStateOf(false) }
                        var playProgress by remember { mutableFloatStateOf(0f) }

                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                playProgress = 0f
                                while (playProgress < 1f && isPlaying) {
                                    delay(100)
                                    playProgress += 0.05f
                                }
                                isPlaying = false
                                playProgress = 0f
                            }
                        }

                        val voiceDuration = remember(message.messageText) {
                            val idx = message.messageText.indexOf("[VOICE]")
                            if (idx != -1) message.messageText.substring(idx + 7).trim() else "0:05"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause Voice" else "Play Voice",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                LinearProgressIndicator(
                                    progress = { playProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🎤 Voice Message • $voiceDuration",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        // Standard Text Message Bubble
                        Text(
                            text = message.messageText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (isFromMe) {
                        val icon = if (message.isRead || message.isDelivered) Icons.Default.DoneAll else Icons.Default.Check
                        val tint = when {
                            message.isRead -> androidx.compose.ui.graphics.Color(0xFF0084FF)
                            message.isDelivered -> androidx.compose.ui.graphics.Color.Gray
                            else -> androidx.compose.ui.graphics.Color.Gray
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = if (message.isRead) "Read" else if (message.isDelivered) "Delivered" else "Sent",
                            tint = tint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return "Offline"
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    return when {
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) -> {
            "Last Seen Today $timeStr"
        }
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Last Seen Yesterday $timeStr"
        }
        else -> {
            val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            "Last Seen $dateStr $timeStr"
        }
    }
}

