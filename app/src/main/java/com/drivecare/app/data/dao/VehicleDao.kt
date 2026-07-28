package com.drivecare.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.drivecare.app.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    fun getVehiclesForUser(userId: String): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    suspend fun getVehiclesForUserSync(userId: String): List<Vehicle>

    @Query("UPDATE vehicles SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedVehicles(userId: String)

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("DELETE FROM vehicles WHERE isDemo = 1")
    suspend fun deleteDemoVehicles(): Int
}
