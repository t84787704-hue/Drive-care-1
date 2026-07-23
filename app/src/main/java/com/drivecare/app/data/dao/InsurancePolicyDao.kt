package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.InsurancePolicy
import kotlinx.coroutines.flow.Flow

@Dao
interface InsurancePolicyDao {
    @Query("SELECT * FROM insurance_policies ORDER BY expiryDate ASC")
    fun getAllInsurancePolicies(): Flow<List<InsurancePolicy>>

    @Query("SELECT * FROM insurance_policies WHERE vehicleId = :vehicleId ORDER BY expiryDate ASC")
    fun getPoliciesForVehicle(vehicleId: Long): Flow<List<InsurancePolicy>>

    @Query("SELECT * FROM insurance_policies ORDER BY expiryDate ASC")
    suspend fun getAllInsurancePoliciesSync(): List<InsurancePolicy>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: InsurancePolicy): Long

    @Update
    suspend fun updatePolicy(policy: InsurancePolicy)

    @Delete
    suspend fun deletePolicy(policy: InsurancePolicy)

    @Query("DELETE FROM insurance_policies WHERE id = :id")
    suspend fun deletePolicyById(id: Long)
}
