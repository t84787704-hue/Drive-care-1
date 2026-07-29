package com.drivecare.app.data.model

data class PublicUserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val country: String = "",
    val preferredCurrency: String = "",
    val photoUrl: String = "",
    val joinDate: Long = 0L,
    val vehicleCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val typingToUserId: String = ""
)
