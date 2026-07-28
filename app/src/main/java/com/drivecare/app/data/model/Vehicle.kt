package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String = "",
    val vehicleName: String = "",
    val vehicleType: String = "Car",
    val brand: String = "",
    val model: String = "",
    val manufacturingYear: String = "",
    val registrationNumber: String = "",
    val fuelType: String = "Petrol",
    val odometerReading: String = "0",
    val notes: String = "",
    val vin: String = "",
    val purchaseDate: String = "",
    val imageUri: String = "",
    val isDemo: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

