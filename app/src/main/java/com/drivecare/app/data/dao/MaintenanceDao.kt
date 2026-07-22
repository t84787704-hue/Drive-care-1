package com.drivecare.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drivecare.app.data.model.Maintenance
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance ORDER BY id DESC")
    fun getAllMaintenance(): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getMaintenanceByVehicle(vehicleId: Long): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)

    @Query("DELETE FROM maintenance WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)
}
