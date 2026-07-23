package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.GeofenceZone
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceZoneDao {
    @Query("SELECT * FROM geofence_zones ORDER BY id DESC")
    fun getAllGeofences(): Flow<List<GeofenceZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceZone): Long

    @Delete
    suspend fun deleteGeofence(geofence: GeofenceZone)
}
