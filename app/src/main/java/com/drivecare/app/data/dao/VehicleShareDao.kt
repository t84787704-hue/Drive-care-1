package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.VehicleShare
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleShareDao {
    @Query("SELECT * FROM vehicle_shares ORDER BY id DESC")
    fun getAllShares(): Flow<List<VehicleShare>>

    @Query("SELECT * FROM vehicle_shares WHERE vehicleId = :vehicleId")
    fun getSharesForVehicle(vehicleId: Long): Flow<List<VehicleShare>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: VehicleShare): Long

    @Delete
    suspend fun deleteShare(share: VehicleShare)
}
