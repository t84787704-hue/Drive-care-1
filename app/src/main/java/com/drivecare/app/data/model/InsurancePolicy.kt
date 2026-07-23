package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insurance_policies")
data class InsurancePolicy(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val providerName: String,
    val policyNumber: String,
    val coverageType: String = "Comprehensive", // Comprehensive, Third-Party, Liability, Collision, Theft
    val premiumAmount: Double = 0.0,
    val startDate: String = "", // YYYY-MM-DD
    val expiryDate: String = "", // YYYY-MM-DD
    val agentContact: String = "",
    val notes: String = "",
    val isAutoRenewEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
