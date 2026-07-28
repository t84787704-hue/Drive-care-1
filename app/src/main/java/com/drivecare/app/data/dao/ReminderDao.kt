package com.drivecare.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.drivecare.app.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY id DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    fun getRemindersForUser(userId: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    suspend fun getRemindersForUserSync(userId: String): List<Reminder>

    @Query("UPDATE reminders SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedReminders(userId: String)

    @Query("SELECT * FROM reminders ORDER BY id DESC")
    suspend fun getAllRemindersSync(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM reminders WHERE isDemo = 1")
    suspend fun deleteDemoReminders(): Int
}
