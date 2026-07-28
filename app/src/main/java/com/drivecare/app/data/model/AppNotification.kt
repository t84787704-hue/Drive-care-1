package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey
    val id: String = "",
    val recipientUid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // FRIEND_REQUEST, FRIEND_ACCEPTED, VEHICLE_SHARED, FAMILY_INVITE, INFO
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
