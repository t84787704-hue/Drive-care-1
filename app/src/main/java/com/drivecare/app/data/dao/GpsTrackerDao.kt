package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.GpsTrackerDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsTrackerDao {
    @Query("SELECT * FROM gps_trackers ORDER BY id DESC")
    fun getAllTrackers(): Flow<List<GpsTrackerDevice>>

    @Query("SELECT * FROM gps_trackers ORDER BY id DESC")
    suspend fun getAllTrackersSync(): List<GpsTrackerDevice>

    @Query("SELECT * FROM gps_trackers WHERE id = :id LIMIT 1")
    suspend fun getTrackerById(id: Long): GpsTrackerDevice?

    @Query("SELECT * FROM gps_trackers WHERE trackerId = :trackerId OR imeiNumber = :trackerId LIMIT 1")
    suspend fun getTrackerByCodeOrImei(trackerId: String): GpsTrackerDevice?

    @Query("SELECT * FROM gps_trackers WHERE vehicleId = :vehicleId LIMIT 1")
    fun getTrackerByVehicle(vehicleId: Long): Flow<GpsTrackerDevice?>

    @Query("SELECT * FROM gps_trackers WHERE vehicleId = :vehicleId LIMIT 1")
    suspend fun getTrackerByVehicleSync(vehicleId: Long): GpsTrackerDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: GpsTrackerDevice): Long

    @Update
    suspend fun updateTracker(tracker: GpsTrackerDevice)

    @Delete
    suspend fun deleteTracker(tracker: GpsTrackerDevice)

    @Query("UPDATE gps_trackers SET vehicleId = NULL WHERE vehicleId = :vehicleId")
    suspend fun unassignTrackerFromVehicle(vehicleId: Long)
}
