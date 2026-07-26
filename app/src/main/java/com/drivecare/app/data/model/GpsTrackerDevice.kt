package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_trackers")
data class GpsTrackerDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerName: String,
    val trackerId: String,
    val imeiNumber: String,
    val simNumber: String = "",
    val vehicleId: Long? = null,
    val notes: String = "",
    val isOnline: Boolean = true,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastSpeedKmh: Double? = null,
    val lastUpdatedTime: Long? = null,
    val createdDate: String = ""
)
