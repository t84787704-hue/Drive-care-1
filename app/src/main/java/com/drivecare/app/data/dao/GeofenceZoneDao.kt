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

    @Query("SELECT * FROM geofence_zones WHERE vehicleId = :vehicleId ORDER BY id DESC")
    suspend fun getGeofencesByVehicleSync(vehicleId: Long): List<GeofenceZone>

    @Query("SELECT * FROM geofence_zones ORDER BY id DESC")
    suspend fun getAllGeofencesSync(): List<GeofenceZone>

    @Query("SELECT * FROM geofence_zones WHERE vehicleId = :vehicleId AND isActive = 1 ORDER BY id DESC")
    suspend fun getActiveGeofencesByVehicleSync(vehicleId: Long): List<GeofenceZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceZone): Long

    @Update
    suspend fun updateGeofence(geofence: GeofenceZone)

    @Delete
    suspend fun deleteGeofence(geofence: GeofenceZone)

    @Query("DELETE FROM geofence_zones WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM geofence_zones WHERE isDemo = 1")
    suspend fun deleteDemoGeofences(): Int
}
