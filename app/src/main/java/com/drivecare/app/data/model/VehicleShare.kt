package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_shares")
data class VehicleShare(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val sharedWithEmail: String,
    val role: String = "DRIVER", // OWNER, DRIVER, VIEWER
    val status: String = "ACTIVE", // ACTIVE, PENDING, REVOKED
    val sharedAt: Long = System.currentTimeMillis()
)
