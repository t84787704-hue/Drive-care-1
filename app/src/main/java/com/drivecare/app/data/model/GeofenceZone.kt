package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_zones")
data class GeofenceZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val zoneName: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Double = 500.0,
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true,
    val isActive: Boolean = true
)
