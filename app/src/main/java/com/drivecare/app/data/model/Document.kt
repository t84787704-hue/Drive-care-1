package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val docTitle: String,
    val docType: String = "Registration", // Registration, Insurance, License, Bill, Warranty, Photo
    val issueDate: String = "",
    val expiryDate: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
