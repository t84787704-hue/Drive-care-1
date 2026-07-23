package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_telemetry")
data class VehicleTelemetry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val fuelLevelPct: Double = 100.0,
    val batteryVoltage: Double = 12.6,
    val engineTempC: Double = 90.0,
    val timestamp: Long = System.currentTimeMillis()
)
