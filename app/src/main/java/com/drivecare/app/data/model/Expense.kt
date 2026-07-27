package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val title: String = "",
    val category: String = "Other", // Fuel, Maintenance, Insurance, Toll, Parking, Tax, Other
    val amount: Double = 0.0,
    val date: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
