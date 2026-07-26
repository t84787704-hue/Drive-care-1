package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracker_location_history")
data class TrackerLocationPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerId: String,
    val vehicleId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val addressName: String = ""
)
