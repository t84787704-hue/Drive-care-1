package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.DriverProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverProfileDao {
    @Query("SELECT * FROM driver_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<DriverProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DriverProfile): Long

    @Update
    suspend fun updateProfile(profile: DriverProfile)

    @Delete
    suspend fun deleteProfile(profile: DriverProfile)
}
