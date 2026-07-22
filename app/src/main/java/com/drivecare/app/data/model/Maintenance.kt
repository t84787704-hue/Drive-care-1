package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance")
data class Maintenance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val serviceTitle: String,
    val serviceType: String = "Routine Service",
    val serviceDate: String,
    val currentOdometer: String = "0",
    val serviceCost: String = "0",
    val workshopName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
