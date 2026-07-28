package com.drivecare.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drivecare.app.data.model.Maintenance
import kotlinx.coroutines.flow.Flow

import androidx.room.Update

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance ORDER BY id DESC")
    fun getAllMaintenance(): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenance WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    fun getMaintenanceForUser(userId: String): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenance WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    suspend fun getMaintenanceForUserSync(userId: String): List<Maintenance>

    @Query("UPDATE maintenance SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedMaintenance(userId: String)

    @Query("SELECT * FROM maintenance ORDER BY id DESC")
    suspend fun getAllMaintenanceSync(): List<Maintenance>

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getMaintenanceByVehicle(vehicleId: Long): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Update
    suspend fun updateMaintenance(maintenance: Maintenance)

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)

    @Query("DELETE FROM maintenance WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM maintenance WHERE isDemo = 1")
    suspend fun deleteDemoMaintenance(): Int
}
