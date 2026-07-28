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

                            ChatMessage(
                                messageId = messageId,
                                conversationId = convId,
                                senderUid = senderUid,
                                receiverUid = receiverUid,
                                messageText = messageText,
                                timestamp = timestamp,
                                isRead = isRead,
                                isDelivered = isDelivered
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
                isDelivered = true
            )

            val msgData = mapOf(
                "messageId" to chatMessage.messageId,
                "conversationId" to chatMessage.conversationId,
                "senderUid" to chatMessage.senderUid,
                "receiverUid" to chatMessage.receiverUid,
                "messageText" to chatMessage.messageText,
                "timestamp" to chatMessage.timestamp,
                "isRead" to chatMessage.isRead,
                "isDelivered" to chatMessage.isDelivered
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

                val updateMap = mapOf(
                    "lastMessage" to chatMessage.messageText,
                    "lastMessageTimestamp" to timestamp,
                    "lastSenderUid" to senderUid,
                    "unreadCounts" to updatedUnreadCounts,
                    "updatedAt" to timestamp,
                    "participantNames" to mapOf(senderUid to senderName, receiverUid to receiverName),
                    "participantEmails" to mapOf(senderUid to senderEmail, receiverUid to receiverEmail)
                )
                convRef.set(updateMap, SetOptions.merge()).await()
            }

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
                val batch = firestore.batch()
                for (doc in unreadDocs.documents) {
                    batch.update(doc.reference, "isRead", true)
                    // Also update in root messages collection if present
                    val rootMsgRef = firestore.collection("messages").document(doc.id)
                    batch.update(rootMsgRef, "isRead", true)
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
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_CHAT,
                    "DriveCare Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for incoming real-time messages from friends"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("target_tab", "MORE")
                putExtra("target_section", "CHAT")
                putExtra("conversation_id", conversationId)
                putExtra("friend_uid", senderUid)
                putExtra("friend_name", senderName)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                conversationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Message from $senderName")
                .setContentText(messageText)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            notificationManager.notify(conversationId.hashCode(), notification)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error showing message notification: ${e.message}")
        }
    }
}
