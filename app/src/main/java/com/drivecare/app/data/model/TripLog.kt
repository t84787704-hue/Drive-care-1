package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_logs")
data class TripLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val driverName: String = "Primary Driver",
    val startLocation: String,
    val endLocation: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val tripDate: String,
    val startTime: String = "",
    val endTime: String = "",
    val fuelConsumedLiters: Double = 0.0,
    val routePointsJson: String = "[]" // JSON array of lat/lng string representation
)
