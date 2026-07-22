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

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getFuelEntriesByVehicle(vehicleId: Long): Flow<List<FuelEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(fuelEntry: FuelEntry): Long

    @Delete
    suspend fun deleteFuelEntry(fuelEntry: FuelEntry)

    @Query("DELETE FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)
}
