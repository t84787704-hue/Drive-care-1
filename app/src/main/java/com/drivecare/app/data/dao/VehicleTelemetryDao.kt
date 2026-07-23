package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.VehicleTelemetry
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleTelemetryDao {
    @Query("SELECT * FROM vehicle_telemetry WHERE vehicleId = :vehicleId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestTelemetryForVehicle(vehicleId: Long): Flow<VehicleTelemetry?>

    @Query("SELECT * FROM vehicle_telemetry ORDER BY timestamp DESC LIMIT 50")
    fun getRecentTelemetry(): Flow<List<VehicleTelemetry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: VehicleTelemetry): Long
}
