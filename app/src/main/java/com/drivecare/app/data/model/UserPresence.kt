package com.drivecare.app.data.model

data class UserPresence(
    val uid: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val typingToUserId: String = ""
)
