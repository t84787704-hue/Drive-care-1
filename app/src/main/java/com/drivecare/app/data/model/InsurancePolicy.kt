package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Entity(tableName = "insurance_policies")
data class InsurancePolicy(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String = "",
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val providerName: String = "", // Insurance Company
    val policyNumber: String = "",
    val coverageType: String = "Comprehensive", // Comprehensive, Third-Party, Liability, Collision, Theft
    val premiumAmount: Double = 0.0,
    val startDate: String = "", // YYYY-MM-DD
    val expiryDate: String = "", // YYYY-MM-DD
    val agentContact: String = "",
    val claimContact: String = "",
    val emergencyContact: String = "",
    val notes: String = "",
    val isAutoRenewEnabled: Boolean = false,
    val documentUri: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val insuranceCompany: String get() = providerName

    fun calculateDaysUntilExpiry(): Long? {
        if (expiryDate.isBlank()) return null
        return try {
            val cleanStr = expiryDate.trim().take(10)
            val formats = arrayOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy")
            var parsedDate: Date? = null
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    sdf.isLenient = false
                    parsedDate = sdf.parse(cleanStr)
                    if (parsedDate != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            val exp = parsedDate ?: return null
            val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdfDay.format(Date())
            val today = sdfDay.parse(todayStr) ?: return null
            val diffMs = exp.time - today.time
            TimeUnit.MILLISECONDS.toDays(diffMs)
        } catch (e: Exception) {
            null
        }
    }

    fun getExpiryCountdownText(): String {
        val days = calculateDaysUntilExpiry() ?: return "No Expiry Date"
        return try {
            when {
                days < 0 -> {
                    val absDays = if (days == Long.MIN_VALUE) Long.MAX_VALUE else -days
                    "Expired $absDays day${if (absDays > 1L) "s" else ""} ago"
                }
                days == 0L -> "Expires Today"
                days == 1L -> "Expires Tomorrow"
                else -> "Expires in $days days"
            }
        } catch (e: Exception) {
            "No Expiry Date"
        }
    }

    fun getPolicyStatus(): String {
        return try {
            val days = calculateDaysUntilExpiry() ?: return "ACTIVE"
            when {
                days < 0 -> "EXPIRED"
                days <= 30 -> "EXPIRING_SOON"
                else -> "ACTIVE"
            }
        } catch (e: Exception) {
            "ACTIVE"
        }
    }
}

