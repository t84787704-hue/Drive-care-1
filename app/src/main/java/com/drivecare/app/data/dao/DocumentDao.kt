package com.drivecare.app.data.dao

import androidx.room.*
import com.drivecare.app.data.model.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE (:userId != '' AND ownerUserId = :userId) OR (:userId = '' AND ownerUserId = '') ORDER BY createdAt DESC")
    fun getDocumentsForUser(userId: String): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE (:userId != '' AND ownerUserId = :userId) OR (:userId = '' AND ownerUserId = '') ORDER BY createdAt DESC")
    suspend fun getDocumentsForUserSync(userId: String): List<Document>

    @Query("UPDATE documents SET ownerUserId = :userId WHERE ownerUserId = ''")
    suspend fun claimUnassignedDocuments(userId: String)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    suspend fun getAllDocumentsSync(): List<Document>

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun getDocumentsByVehicle(vehicleId: Long): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("DELETE FROM documents WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicle(vehicleId: Long)

    @Query("DELETE FROM documents WHERE isDemo = 1")
    suspend fun deleteDemoDocuments(): Int
}
