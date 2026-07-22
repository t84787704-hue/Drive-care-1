package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Mechanic", // Mechanic, Family, Insurance, Towing
    val phoneNumber: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
