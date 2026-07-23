package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.TripLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TripLogDao {
    @Query("SELECT * FROM trip_logs ORDER BY id DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Query("SELECT * FROM trip_logs WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getTripsForVehicle(vehicleId: Long): Flow<List<TripLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripLog): Long

    @Delete
    suspend fun deleteTrip(trip: TripLog)
}
