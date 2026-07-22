package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleName: String,
    val vehicleType: String = "Car",
    val brand: String = "",
    val model: String = "",
    val manufacturingYear: String = "",
    val registrationNumber: String = "",
    val fuelType: String = "Petrol",
    val odometerReading: String = "0",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
