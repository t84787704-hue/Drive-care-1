package com.drivecare.app.data.cloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.drivecare.app.MainActivity
import com.drivecare.app.R
import com.drivecare.app.data.model.ChatMessage
import com.drivecare.app.data.model.Conversation
import com.drivecare.app.data.model.UserPresence
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        const val CHANNEL_ID_CHAT = "drivecare_chat_notifications"

        fun getConversationId(uid1: String, uid2: String): String {
            val u1 = uid1.trim()
            val u2 = uid2.trim()
            return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
        }
    }

    /**
     * Real-time listener for all conversations where currentUid is a participant.
     */
    fun getConversationsFlow(currentUid: String): Flow<List<Conversation>> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching conversations: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val participants = doc.get("participants") as? List<String> ?: emptyList()
                            val participantNames = doc.get("participantNames") as? Map<String, String> ?: emptyMap()
                            val participantEmails = doc.get("participantEmails") as? Map<String, String> ?: emptyMap()
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L
                            val lastSenderUid = doc.getString("lastSenderUid") ?: ""
                            val unreadCounts = doc.get("unreadCounts") as? Map<String, Long> ?: emptyMap()
                            val createdAt = doc.getLong("createdAt") ?: 0L
                            val updatedAt = doc.getLong("updatedAt") ?: lastMessageTimestamp

                            Conversation(
                                conversationId = id,
                                participants = participants,
                                participantNames = participantNames,
                                participantEmails = participantEmails,
                                lastMessage = lastMessage,
                                lastMessageTimestamp = lastMessageTimestamp,
                                lastSenderUid = lastSenderUid,
                                unreadCounts = unreadCounts,
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error parsing conversation doc ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.updatedAt }

                    trySend(conversations)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Real-time listener for messages in a specific conversation.
     */
    fun getMessagesFlow(conversationId: String, limit: Long = 100): Flow<List<ChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching messages: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val messageId = doc.getString("messageId") ?: doc.id
                            val convId = doc.getString("conversationId") ?: conversationId
                            val senderUid = doc.getString("senderUid") ?: ""
                            val receiverUid = doc.getString("receiverUid") ?: ""
                            val messageText = doc.getString("messageText") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            val isRead = doc.getBoolean("isRead") ?: false
                            val isDelivered = doc.getBoolean("isDelivered") ?: true
                            val readAt = doc.getLong("readAt") ?: 0L

                            ChatMessage(
                                messageId = messageId,
                                conversationId = convId,
                                senderUid = senderUid,
                                receiverUid = receiverUid,
                                messageText = messageText,
                                timestamp = timestamp,
                                isRead = isRead,
                                isDelivered = isDelivered,
                                readAt = readAt
                            )
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error parsing message doc ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    trySend(messages)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Sends a message to a conversation.
     * Creates conversation document if it doesn't exist.
     * Stores message in both conversations/{conversationId}/messages/{messageId} and top-level messages/{messageId}.
     */
    suspend fun sendMessage(
        senderUid: String,
        senderName: String,
        senderEmail: String,
        receiverUid: String,
        receiverName: String,
        receiverEmail: String,
        messageText: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            if (senderUid.isBlank() || receiverUid.isBlank() || messageText.isBlank()) {
                return@withContext Result.failure(Exception("Invalid message params"))
            }

            val conversationId = getConversationId(senderUid, receiverUid)
            val timestamp = System.currentTimeMillis()
            val msgRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()
            val messageId = msgRef.id

            val chatMessage = ChatMessage(
                messageId = messageId,
                conversationId = conversationId,
                senderUid = senderUid,
                receiverUid = receiverUid,
                messageText = messageText.trim(),
                timestamp = timestamp,
                isRead = false,
                isDelivered = true,
                readAt = 0L
            )

            val msgData = mapOf(
                "messageId" to chatMessage.messageId,
                "conversationId" to chatMessage.conversationId,
                "senderUid" to chatMessage.senderUid,
                "receiverUid" to chatMessage.receiverUid,
                "messageText" to chatMessage.messageText,
                "timestamp" to chatMessage.timestamp,
                "isRead" to chatMessage.isRead,
                "isDelivered" to chatMessage.isDelivered,
                "readAt" to chatMessage.readAt
            )

            // 1. Write message to conversations/{conversationId}/messages/{messageId}
            msgRef.set(msgData).await()

            // 2. Write message to root collection messages/{messageId} for top-level indexing
            firestore.collection("messages").document(messageId).set(msgData, SetOptions.merge()).await()

            // 3. Update conversation root metadata
            val convRef = firestore.collection("conversations").document(conversationId)
            val convDoc = convRef.get().await()

            if (!convDoc.exists()) {
                val newConvData = mapOf(
                    "conversationId" to conversationId,
                    "participants" to listOf(senderUid, receiverUid),
                    "participantNames" to mapOf(senderUid to senderName, receiverUid to receiverName),
                    "participantEmails" to mapOf(senderUid to senderEmail, receiverUid to receiverEmail),
                    "lastMessage" to chatMessage.messageText,
                    "lastMessageTimestamp" to timestamp,
                    "lastSenderUid" to senderUid,
                    "unreadCounts" to mapOf(senderUid to 0L, receiverUid to 1L),
                    "createdAt" to timestamp,
                    "updatedAt" to timestamp
                )
                convRef.set(newConvData).await()
            } else {
                val currentUnreadCounts = convDoc.get("unreadCounts") as? Map<String, Long> ?: emptyMap()
                val newUnreadForReceiver = (currentUnreadCounts[receiverUid] ?: 0L) + 1L
                val updatedUnreadCounts = currentUnreadCounts.toMutableMap().apply {
                    put(senderUid, currentUnreadCounts[senderUid] ?: 0L)
                    put(receiverUid, newUnreadForReceiver)
                }

                val existingNames = convDoc.get("participantNames") as? Map<String, String> ?: emptyMap()
                val updatedNames = existingNames.toMutableMap().apply {
                    if (senderName.isNotBlank() && senderName != "DriveCare User") put(senderUid, senderName)
                    if (receiverName.isNotBlank() && receiverName != "DriveCare User" && !receiverName.equals(senderName, ignoreCase = true)) {
                        put(receiverUid, receiverName)
                    }
                }

                val existingEmails = convDoc.get("participantEmails") as? Map<String, String> ?: emptyMap()
                val updatedEmails = existingEmails.toMutableMap().apply {
                    if (senderEmail.isNotBlank()) put(senderUid, senderEmail)
                    if (receiverEmail.isNotBlank()) put(receiverUid, receiverEmail)
                }

                val updateMap = mapOf(
                    "lastMessage" to chatMessage.messageText,
                    "lastMessageTimestamp" to timestamp,
                    "lastSenderUid" to senderUid,
                    "unreadCounts" to updatedUnreadCounts,
                    "updatedAt" to timestamp,
                    "participantNames" to updatedNames,
                    "participantEmails" to updatedEmails
                )
                convRef.set(updateMap, SetOptions.merge()).await()
            }

            // Trigger FCM / Firestore push notification to recipient
            val previewText = when {
                chatMessage.messageText.contains("📷 Image") || chatMessage.messageText.startsWith("[Image]") || chatMessage.messageText.startsWith("[Photo]") || chatMessage.messageText.contains("image_picker") || chatMessage.messageText.contains(".jpg") || chatMessage.messageText.contains(".png") -> "📷 Image"
                chatMessage.messageText.contains("🎤 Voice Message") || chatMessage.messageText.startsWith("[Voice]") || chatMessage.messageText.startsWith("[Audio]") -> "🎤 Voice Message"
                else -> chatMessage.messageText
            }
            val notifType = when (previewText) {
                "🎤 Voice Message" -> "CHAT_VOICE"
                "📷 Image" -> "CHAT_IMAGE"
                else -> "CHAT_TEXT"
            }

            com.drivecare.app.utils.FcmNotificationManager.sendPushNotification(
                recipientUid = receiverUid,
                title = senderName.ifBlank { "DriveCare User" },
                body = previewText,
                type = notifType,
                friendUid = senderUid,
                friendName = senderName,
                targetTab = "CHAT",
                targetSection = "CHAT"
            )

            Result.success(chatMessage)
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marks all unread messages received by currentUid in the given conversation as read.
     */
    suspend fun markMessagesAsRead(conversationId: String, currentUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (conversationId.isBlank() || currentUid.isBlank()) return@withContext Result.success(Unit)

            val convRef = firestore.collection("conversations").document(conversationId)
            val unreadDocs = convRef.collection("messages")
                .whereEqualTo("receiverUid", currentUid)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (!unreadDocs.isEmpty) {
                val now = System.currentTimeMillis()
                val batch = firestore.batch()
                for (doc in unreadDocs.documents) {
                    batch.update(doc.reference, mapOf("isRead" to true, "isDelivered" to true, "readAt" to now))
                    // Also update in root messages collection if present
                    val rootMsgRef = firestore.collection("messages").document(doc.id)
                    batch.update(rootMsgRef, mapOf("isRead" to true, "isDelivered" to true, "readAt" to now))
                }
                batch.commit().await()
            }

            // Reset unread count for currentUid in conversation metadata
            val convDoc = convRef.get().await()
            if (convDoc.exists()) {
                val currentUnreadCounts = (convDoc.get("unreadCounts") as? Map<String, Long> ?: emptyMap()).toMutableMap()
                currentUnreadCounts[currentUid] = 0L
                convRef.update("unreadCounts", currentUnreadCounts).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "markMessagesAsRead failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Shows a local system notification for new incoming messages.
     */
    fun showLocalMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        conversationId: String,
        senderUid: String
    ) {
        val previewText = when {
            messageText.contains("Voice Message") || messageText.startsWith("[Voice]") || messageText.startsWith("[Audio]") -> "🎤 Voice Message"
            messageText.contains("Image") || messageText.startsWith("[Image]") || messageText.startsWith("[Photo]") -> "📷 Image"
            else -> messageText
        }
        val type = when {
            previewText.contains("Voice") -> "CHAT_VOICE"
            previewText.contains("Image") -> "CHAT_IMAGE"
            else -> "CHAT_TEXT"
        }
        com.drivecare.app.utils.FcmNotificationManager.showNotification(
            context = context,
            title = senderName.ifBlank { "Friend" },
            body = previewText,
            type = type,
            friendUid = senderUid,
            friendName = senderName,
            targetTab = "MORE",
            targetSection = "CHAT",
            notificationId = conversationId.hashCode()
        )
    }

    /**
     * Updates real-time presence (online status, last seen, typing target) for a user.
     */
    suspend fun updateUserPresence(
        uid: String,
        isOnline: Boolean,
        typingToUserId: String? = null
    ) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        try {
            val now = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any>(
                "uid" to uid,
                "isOnline" to isOnline,
                "lastSeen" to now
            )
            if (typingToUserId != null) {
                updates["typingToUserId"] = typingToUserId
            }
            firestore.collection("user_presence").document(uid)
                .set(updates, SetOptions.merge())
                .await()

            firestore.collection("users").document(uid)
                .set(mapOf("isOnline" to isOnline, "lastSeen" to now), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error updating user presence for $uid: ${e.message}")
        }
    }

    /**
     * Real-time listener for user presence (online status, last seen, typing indicator).
     */
    fun getUserPresenceFlow(uid: String): Flow<UserPresence> = callbackFlow {
        if (uid.isBlank()) {
            trySend(UserPresence())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("user_presence")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(UserPresence(uid = uid))
                    return@addSnapshotListener
                }
                try {
                    val isOnline = snapshot.getBoolean("isOnline") ?: false
                    val lastSeen = snapshot.getLong("lastSeen") ?: 0L
                    val typingTo = snapshot.getString("typingToUserId") ?: ""
                    trySend(
                        UserPresence(
                            uid = uid,
                            isOnline = isOnline,
                            lastSeen = lastSeen,
                            typingToUserId = typingTo
                        )
                    )
                } catch (e: Exception) {
                    trySend(UserPresence(uid = uid))
                }
            }

        awaitClose { listener.remove() }
    }
}
