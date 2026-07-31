package com.drivecare.app.data.cloud

import android.util.Log
import com.drivecare.app.data.model.ChatMessage
import com.drivecare.app.data.model.UserChat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val TAG = "ChatRepository"

        /**
         * Generates deterministic chatId by sorting user IDs alphabetically and joining with underscore.
         * Example: sort("uidB", "uidA") -> "uidA_uidB"
         */
        fun getChatId(uid1: String, uid2: String): String {
            val u1 = uid1.trim()
            val u2 = uid2.trim()
            if (u1.isBlank() || u2.isBlank()) return ""
            return listOf(u1, u2).sorted().joinToString("_")
        }
    }

    /**
     * Listens to user's active chats where participants array contains currentUserId.
     * Orders chats by lastMessageTime descending.
     */
    fun getUserChatsFlow(currentUserId: String): Flow<List<UserChat>> = callbackFlow {
        val uid = currentUserId.trim()
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to conversations for $uid: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val chatId = doc.id
                        val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                        val participantNames = (doc.get("participantNames") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                        val participantEmails = (doc.get("participantEmails") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val updatedAt = doc.getLong("updatedAt") ?: doc.getLong("lastMessageTime") ?: 0L
                        val lastSenderId = doc.getString("lastSenderId") ?: doc.getString("lastSender") ?: ""
                        val unreadCounts = (doc.get("unreadCounts") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toLong() ?: 0L } ?: emptyMap()
                        val typingStatus = (doc.get("typingStatus") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                        val onlineStatus = (doc.get("onlineStatus") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                        val lastSeen = (doc.get("lastSeen") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Number)?.toLong() ?: 0L } ?: emptyMap()

                        UserChat(
                            chatId = chatId,
                            participants = participants,
                            participantNames = participantNames,
                            participantEmails = participantEmails,
                            lastMessage = lastMessage,
                            lastMessageTime = updatedAt,
                            lastSenderId = lastSenderId,
                            unreadCounts = unreadCounts,
                            typingStatus = typingStatus,
                            onlineStatus = onlineStatus,
                            lastSeen = lastSeen
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing UserChat doc ${doc.id}: ${e.message}")
                        null
                    }
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Listens ONLY to specific chatId's messages sub-collection: conversations/{chatId}/messages
     */
    fun getChatMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages for chatId $chatId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            chatId = doc.getString("chatId") ?: chatId,
                            senderId = doc.getString("senderId") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            status = doc.getString("status") ?: "SENT",
                            mediaUrl = doc.getString("mediaUrl"),
                            mediaType = doc.getString("mediaType") ?: "TEXT",
                            durationMs = doc.getLong("durationMs")
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing ChatMessage ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Sends a 1-to-1 message inside conversations/{chatId}/messages
     */
    suspend fun sendMessage(
        senderUid: String,
        senderName: String,
        senderEmail: String,
        receiverUid: String,
        receiverName: String,
        receiverEmail: String,
        text: String,
        mediaUrl: String? = null,
        mediaType: String = "TEXT",
        durationMs: Long? = null
    ): Result<String> {
        return try {
            val sUid = senderUid.trim()
            val rUid = receiverUid.trim()
            if (sUid.isBlank() || rUid.isBlank() || sUid.equals(rUid, ignoreCase = true)) {
                return Result.failure(IllegalArgumentException("Invalid sender or receiver ID"))
            }

            val chatId = getChatId(sUid, rUid)
            val now = System.currentTimeMillis()

            val msgRef = firestore.collection("conversations")
                .document(chatId)
                .collection("messages")
                .document()

            val messageId = msgRef.id
            val messageData = mutableMapOf<String, Any?>(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to sUid,
                "receiverId" to rUid,
                "text" to text,
                "timestamp" to now,
                "status" to "SENT",
                "mediaType" to mediaType
            )
            if (mediaUrl != null) messageData["mediaUrl"] = mediaUrl
            if (durationMs != null) messageData["durationMs"] = durationMs

            // 1. Write message to conversations/{chatId}/messages/{messageId}
            msgRef.set(messageData).await()

            // 2. Update parent conversations/{chatId} metadata
            val chatDocRef = firestore.collection("conversations").document(chatId)
            val chatSnapshot = chatDocRef.get().await()

            val existingUnreadCounts = (chatSnapshot.get("unreadCounts") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? Number)?.toLong() ?: 0L }
                ?.toMutableMap() ?: mutableMapOf()

            val currentUnread = existingUnreadCounts[rUid] ?: 0L
            existingUnreadCounts[rUid] = currentUnread + 1L

            val chatUpdate = mapOf(
                "chatId" to chatId,
                "participants" to listOf(sUid, rUid),
                "participantNames" to mapOf(sUid to senderName, rUid to receiverName),
                "participantEmails" to mapOf(sUid to senderEmail, rUid to receiverEmail),
                "lastMessage" to text,
                "lastMessageTime" to now,
                "updatedAt" to now,
                "lastSenderId" to sUid,
                "unreadCounts" to existingUnreadCounts
            )

            chatDocRef.set(chatUpdate, SetOptions.merge()).await()

            // 3. Send Push Notification to receiver
            try {
                com.drivecare.app.utils.FcmNotificationManager.sendPushNotification(
                    recipientUid = rUid,
                    title = senderName.ifBlank { "New Message" },
                    body = text,
                    type = "CHAT_TEXT",
                    friendUid = sUid,
                    friendName = senderName
                )
            } catch (e: Exception) {
                Log.w(TAG, "Push notification trigger failed: ${e.message}")
            }

            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marks messages as READ for a specific chatId where receiverId == currentUserId
     */
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        val uid = currentUserId.trim()
        if (chatId.isBlank() || uid.isBlank()) return

        try {
            val messagesRef = firestore.collection("conversations")
                .document(chatId)
                .collection("messages")

            val unreadDocs = messagesRef
                .whereEqualTo("receiverId", uid)
                .whereNotEqualTo("status", "READ")
                .get()
                .await()

            if (!unreadDocs.isEmpty) {
                val batch = firestore.batch()
                for (doc in unreadDocs.documents) {
                    batch.update(doc.reference, "status", "READ")
                }
                batch.commit().await()
            }

            // Reset unread count for current user in conversations/{chatId}
            val chatDocRef = firestore.collection("conversations").document(chatId)
            val chatSnapshot = chatDocRef.get().await()
            if (chatSnapshot.exists()) {
                val existingUnreadCounts = (chatSnapshot.get("unreadCounts") as? Map<*, *>)
                    ?.mapKeys { it.key.toString() }
                    ?.mapValues { (it.value as? Number)?.toLong() ?: 0L }
                    ?.toMutableMap() ?: mutableMapOf()

                if ((existingUnreadCounts[uid] ?: 0L) > 0L) {
                    existingUnreadCounts[uid] = 0L
                    chatDocRef.update("unreadCounts", existingUnreadCounts).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "markMessagesAsRead failed for $chatId: ${e.message}")
        }
    }

    /**
     * Updates user typing and online status in conversations/{chatId}
     */
    suspend fun updatePresenceAndTyping(
        chatId: String,
        currentUserId: String,
        isOnline: Boolean,
        isTyping: Boolean
    ) {
        val uid = currentUserId.trim()
        if (chatId.isBlank() || uid.isBlank()) return

        try {
            val chatDocRef = firestore.collection("conversations").document(chatId)
            val now = System.currentTimeMillis()

            val updates = mutableMapOf<String, Any>(
                "typingStatus.$uid" to isTyping,
                "onlineStatus.$uid" to isOnline,
                "lastSeen.$uid" to now
            )

            chatDocRef.set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "updatePresenceAndTyping failed for $chatId: ${e.message}")
        }
    }
}
