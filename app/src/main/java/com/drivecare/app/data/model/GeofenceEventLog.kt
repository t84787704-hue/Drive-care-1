package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_events")
data class GeofenceEventLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val trackerId: String = "",
    val zoneName: String,
    val eventType: String, // "ENTRY" or "EXIT"
    val latitude: Double,
    val longitude: Double,
    val eventDate: String,
    val eventTime: String,
    val timestamp: Long = System.currentTimeMillis()
)
