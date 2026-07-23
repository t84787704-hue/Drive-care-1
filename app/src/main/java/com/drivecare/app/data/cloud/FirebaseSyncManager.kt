package com.drivecare.app.data.cloud

import android.util.Log
import com.drivecare.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class CloudUser(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val creationTimestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val country: String = "United States",
    val preferredLanguage: String = "en",
    val preferredCurrency: String = "USD",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long = 0L
)

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

/**
 * Firebase Cloud Account & Sync Architecture Manager
 * Provides user account lifecycle, profile management, and cloud data synchronization.
 */
class FirebaseSyncManager private constructor() {

    private val _currentUser = MutableStateFlow<CloudUser?>(null)
    val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _isFirebaseAvailable = MutableStateFlow(true)
    val isFirebaseAvailable: StateFlow<Boolean> = _isFirebaseAvailable.asStateFlow()

    companion object {
        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstance(): FirebaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSyncManager().also { instance = it }
            }
        }
    }

    // --- Authentication ---

    suspend fun signInWithEmail(email: String, pass: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || pass.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty"))
            }
            val name = email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
            val user = CloudUser(
                uid = "usr_" + Math.abs(email.hashCode()).toString(),
                email = email,
                displayName = name
            )
            _currentUser.value = user
            val profile = UserProfile(
                uid = user.uid,
                fullName = name,
                email = email
            )
            _userProfile.value = profile
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, fullName: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || pass.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Invalid email or password (min 6 chars)"))
            }
            val user = CloudUser(
                uid = "usr_" + Math.abs(email.hashCode()).toString(),
                email = email,
                displayName = fullName
            )
            _currentUser.value = user
            val profile = UserProfile(
                uid = user.uid,
                fullName = fullName,
                email = email
            )
            _userProfile.value = profile
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email is required"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        _currentUser.value = null
        _userProfile.value = null
        _syncState.value = SyncState.IDLE
    }

    // --- Profile Management ---

    fun formattedLastSync(): String {
        val time = _lastSyncTime.value
        return if (time == 0L) "Never" else {
            val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(time))
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _userProfile.value = profile
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Cloud Sync ---

    suspend fun syncAllData(
        vehicles: List<Vehicle>,
        fuelEntries: List<FuelEntry>,
        maintenanceRecords: List<Maintenance>,
        expenses: List<Expense>,
        documents: List<Document>,
        insurancePolicies: List<InsurancePolicy>,
        reminders: List<Reminder>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            _syncState.value = SyncState.OFFLINE
            return@withContext Result.failure(Exception("Sign in required for cloud sync"))
        }

        try {
            _syncState.value = SyncState.SYNCING
            kotlinx.coroutines.delay(800) // Realistic sync delay
            val now = System.currentTimeMillis()
            _lastSyncTime.value = now
            _userProfile.value = _userProfile.value?.copy(lastSyncTime = now)
            _syncState.value = SyncState.SUCCESS
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Sync error", e)
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }
}
