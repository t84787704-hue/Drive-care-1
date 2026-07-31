package com.drivecare.app.data.model

data class UserChat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantEmails: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val unreadCounts: Map<String, Long> = emptyMap(),
    val typingStatus: Map<String, Boolean> = emptyMap(),
    val onlineStatus: Map<String, Boolean> = emptyMap(),
    val lastSeen: Map<String, Long> = emptyMap()
)
