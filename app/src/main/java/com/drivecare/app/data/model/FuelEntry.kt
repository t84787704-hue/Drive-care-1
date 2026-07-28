package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String = "",
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val fuelDate: String = "",
    val fuelType: String = "Petrol",
    val fuelQuantity: String = "0",
    val amountPaid: String = "0",
    val currentOdometer: String = "0",
    val fuelStationName: String = "",
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
