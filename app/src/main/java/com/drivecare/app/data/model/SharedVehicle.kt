package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_vehicles_v2")
data class SharedVehicle(
    @PrimaryKey
    val id: String = "",
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val sharedWithUid: String = "",
    val sharedWithEmail: String = "",
    val sharedWithName: String = "",
    val permission: String = "Viewer", // Viewer, Editor, Manager
    val createdAt: Long = System.currentTimeMillis()
)
