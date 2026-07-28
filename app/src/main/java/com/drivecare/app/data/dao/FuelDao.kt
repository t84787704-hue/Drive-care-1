package com.drivecare.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drivecare.app.data.model.FuelEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries ORDER BY id DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    fun getFuelEntriesForUser(userId: String): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    suspend fun getFuelEntriesForUserSync(userId: String): List<FuelEntry>

    @Query("UPDATE fuel_entries SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedFuelEntries(userId: String)

    @Query("SELECT * FROM fuel_entries ORDER BY id DESC")
    suspend fun getAllFuelEntriesSync(): List<FuelEntry>

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getFuelEntriesByVehicle(vehicleId: Long): Flow<List<FuelEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(fuelEntry: FuelEntry): Long

    @Delete
    suspend fun deleteFuelEntry(fuelEntry: FuelEntry)

    @Query("DELETE FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM fuel_entries WHERE isDemo = 1")
    suspend fun deleteDemoFuelEntries(): Int
}
