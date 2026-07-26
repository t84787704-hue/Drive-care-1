package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.TrackerLocationPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerLocationDao {
    @Query("SELECT * FROM tracker_location_history ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<TrackerLocationPoint>>

    @Query("SELECT * FROM tracker_location_history ORDER BY timestamp DESC")
    suspend fun getAllLocationsSync(): List<TrackerLocationPoint>

    @Query("SELECT * FROM tracker_location_history WHERE trackerId = :trackerId ORDER BY timestamp DESC")
    fun getLocationsByTracker(trackerId: String): Flow<List<TrackerLocationPoint>>

    @Query("SELECT * FROM tracker_location_history WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getLocationsByVehicle(vehicleId: Long): Flow<List<TrackerLocationPoint>>

    @Query("SELECT * FROM tracker_location_history WHERE trackerId = :trackerId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(trackerId: String): TrackerLocationPoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: TrackerLocationPoint): Long

    @Query("DELETE FROM tracker_location_history WHERE trackerId = :trackerId")
    suspend fun deleteHistoryForTracker(trackerId: String)

    @Query("DELETE FROM tracker_location_history WHERE vehicleId = :vehicleId")
    suspend fun deleteHistoryForVehicle(vehicleId: Long)

    @Query("DELETE FROM tracker_location_history")
    suspend fun deleteAllLocations()
}
