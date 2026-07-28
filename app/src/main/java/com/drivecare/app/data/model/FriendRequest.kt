package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friend_requests")
data class FriendRequest(
    @PrimaryKey
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhoto: String = "",
    val receiverUid: String = "",
    val receiverName: String = "",
    val receiverEmail: String = "",
    val receiverPhoto: String = "",
    val status: String = "Pending", // Pending, Accepted, Rejected, Cancelled
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
