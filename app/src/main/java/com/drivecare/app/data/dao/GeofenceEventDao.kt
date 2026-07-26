package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.GeofenceEventLog
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceEventDao {
    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<GeofenceEventLog>>

    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC")
    suspend fun getAllEventsSync(): List<GeofenceEventLog>

    @Query("SELECT * FROM geofence_events WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getEventsByVehicle(vehicleId: Long): Flow<List<GeofenceEventLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: GeofenceEventLog): Long

    @Query("DELETE FROM geofence_events WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM geofence_events")
    suspend fun deleteAllEvents()
}
