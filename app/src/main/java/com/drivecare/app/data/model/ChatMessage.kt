package com.drivecare.app.data.model

data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = true,
    val readAt: Long = 0L
)
