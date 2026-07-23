package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val title: String,
    val category: String, // Fuel, Maintenance, Insurance, Toll, Parking, Tax, Other
    val amount: Double,
    val date: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
