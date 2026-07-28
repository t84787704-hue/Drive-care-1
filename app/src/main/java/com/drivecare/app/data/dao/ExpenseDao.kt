package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    fun getExpensesForUser(userId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE ownerUserId = :userId OR (ownerUserId = '' AND :userId = '') ORDER BY id DESC")
    suspend fun getExpensesForUserSync(userId: String): List<Expense>

    @Query("UPDATE expenses SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedExpenses(userId: String)

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    suspend fun getAllExpensesSync(): List<Expense>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<Expense>>

    @Query("DELETE FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE isDemo = 1")
    suspend fun deleteDemoExpenses(): Int
}
