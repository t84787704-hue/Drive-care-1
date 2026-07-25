package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.GeofenceZone
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceZoneDao {
    @Query("SELECT * FROM geofence_zones ORDER BY id DESC")
    fun getAllGeofences(): Flow<List<GeofenceZone>>

    @Query("SELECT * FROM geofence_zones WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getGeofencesByVehicle(vehicleId: Long): Flow<List<GeofenceZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceZone): Long

    @Update
    suspend fun updateGeofence(geofence: GeofenceZone)

    @Delete
    suspend fun deleteGeofence(geofence: GeofenceZone)

    @Query("DELETE FROM geofence_zones WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)
}
