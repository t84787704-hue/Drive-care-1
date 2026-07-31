package com.drivecare.app.data.model

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // "SENT", "DELIVERED", "READ"
    val mediaUrl: String? = null,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    val durationMs: Long? = null
)
