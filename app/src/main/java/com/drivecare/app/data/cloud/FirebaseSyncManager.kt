package com.drivecare.app.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudUser(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val creationTimestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val country: String = "Pakistan",
    val preferredLanguage: String = "en",
    val preferredCurrency: String = "PKR",
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

class FirebaseSyncManager private constructor() {

    private val _currentUser = MutableStateFlow<CloudUser?>(null)
    val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastDownloadTime = MutableStateFlow(0L)
    val lastDownloadTime: StateFlow<Long> = _lastDownloadTime.asStateFlow()

    private val _lastUploadTime = MutableStateFlow(0L)
    val lastUploadTime: StateFlow<Long> = _lastUploadTime.asStateFlow()

    private val _lastDownloadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lastDownloadCounts: StateFlow<Map<String, Int>> = _lastDownloadCounts.asStateFlow()

    private val _lastUploadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lastUploadCounts: StateFlow<Map<String, Int>> = _lastUploadCounts.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    private val _isFirebaseAvailable = MutableStateFlow(true)
    val isFirebaseAvailable: StateFlow<Boolean> = _isFirebaseAvailable.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firebaseStorage: FirebaseStorage? = null

    companion object {
        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstance(): FirebaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSyncManager().also { instance = it }
            }
        }
    }

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("drivecare_auth_prefs", Context.MODE_PRIVATE)

        try {
            if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(appContext)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            firebaseStorage = FirebaseStorage.getInstance()
            _isFirebaseAvailable.value = true
            addAuditLog("[INIT] Firebase Auth, Firestore, and Storage initialized successfully.")
        } catch (e: Exception) {
            _isFirebaseAvailable.value = false
            Log.w("FirebaseSyncManager", "Firebase services initialization info: ${e.message}")
            addAuditLog("[INIT WARN] Firebase services fallback: ${e.message}")
        }

        restoreSavedUserSession()
    }

    fun addAuditLog(msg: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val formattedLog = "[$timeStr] $msg"
        Log.d("FirebaseSyncManager", formattedLog)
        val currentList = _auditLogs.value.toMutableList()
        currentList.add(0, formattedLog)
        if (currentList.size > 80) {
            currentList.removeAt(currentList.lastIndex)
        }
        _auditLogs.value = currentList
    }

    private fun restoreSavedUserSession() {
        val fbUser = try { firebaseAuth?.currentUser } catch (_: Exception) { null }
        if (fbUser != null && !fbUser.email.isNullOrBlank()) {
            val photoUrl = fbUser.photoUrl?.toString() ?: ""
            val gName = fbUser.displayName?.ifBlank { null }
                ?: fbUser.email?.substringBefore("@")?.replace(".", " ")?.replace("-", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                ?: ""
            val user = CloudUser(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = gName,
                photoUrl = photoUrl
            )
            _currentUser.value = user
            _userProfile.value = UserProfile(
                uid = user.uid,
                fullName = gName,
                email = user.email,
                photoUrl = photoUrl
            )
            saveUserToPrefs(user, gName, photoUrl)
            addAuditLog("[SESSION RESTORE] Active Firebase user session: ${user.email} (UID: ${user.uid})")
            return
        }

        // Restore from SharedPreferences
        val p = prefs ?: return
        val uid = p.getString("uid", null)
        val email = p.getString("email", null)
        val fullName = p.getString("fullName", "") ?: ""
        val photoUrl = p.getString("photoUrl", "") ?: ""

        if (!uid.isNullOrBlank() && !email.isNullOrBlank()) {
            val user = CloudUser(
                uid = uid,
                email = email,
                displayName = fullName.ifBlank { email.substringBefore("@") },
                photoUrl = photoUrl
            )
            _currentUser.value = user
            _userProfile.value = UserProfile(
                uid = uid,
                fullName = user.displayName ?: "",
                email = email,
                photoUrl = photoUrl
            )
            addAuditLog("[SESSION RESTORE] Restored cached session: ${email} (UID: ${uid})")
        }
    }

    private fun saveUserToPrefs(user: CloudUser, fullName: String, photoUrl: String = "") {
        prefs?.edit()
            ?.putString("uid", user.uid)
            ?.putString("email", user.email)
            ?.putString("fullName", fullName)
            ?.putString("photoUrl", photoUrl)
            ?.apply()
    }

    private fun clearPrefs() {
        prefs?.edit()?.clear()?.apply()
    }

    // --- Authentication ---

    suspend fun signInWithGoogleCredential(
        idToken: String?,
        email: String,
        name: String,
        photoUrl: String? = null
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val emailUsernameFallback = cleanEmail.substringBefore("@").replace(".", " ").replace("-", " ").replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        
        val cleanName = if (name.isNotBlank() && !name.equals(cleanEmail.substringBefore("@"), ignoreCase = true)) {
            name.trim()
        } else {
            emailUsernameFallback
        }

        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email cannot be empty"))
        }

        val fa = firebaseAuth
        val projId = try { com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "drivecare-1734e" } catch (_: Exception) { "drivecare-1734e" }
        
        if (fa == null) {
            val err = "Firebase Auth service is not initialized on device."
            Log.e("GOOGLE_SIGN_IN", err)
            addAuditLog("[AUTH ERROR] $err")
            return@withContext Result.failure(IllegalStateException(err))
        }

        if (idToken.isNullOrBlank()) {
            val err = "Google ID Token is missing or null. Real Google Sign-In requires a valid Google ID Token."
            Log.e("GOOGLE_SIGN_IN", err)
            addAuditLog("[AUTH ERROR] $err")
            return@withContext Result.failure(IllegalArgumentException("Google ID Token is missing or invalid. Please sign in via Google."))
        }

        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = fa.signInWithCredential(credential).await()
            val fbUser = authResult.user
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                val gName = fbUser.displayName?.ifBlank { null }
                    ?: if (cleanName.isNotBlank() && cleanName != emailUsernameFallback) cleanName else emailUsernameFallback
                val gPhoto = fbUser.photoUrl?.toString() ?: photoUrl ?: ""
                val user = CloudUser(
                    uid = fbUser.uid,
                    email = fbUser.email ?: cleanEmail,
                    displayName = gName,
                    photoUrl = gPhoto
                )
                _currentUser.value = user
                val profile = UserProfile(uid = user.uid, fullName = gName, email = user.email, photoUrl = gPhoto)
                _userProfile.value = profile
                saveUserToPrefs(user, gName, gPhoto)
                
                val logText = """
                    [GOOGLE SIGN IN]
                    Selected Email: ${user.email}
                    ID Token: PRESENT (${idToken.take(15)}...)
                    Firebase UID: ${user.uid}
                    Project ID: $projId
                    Exception: None
                """.trimIndent()
                Log.i("GOOGLE_SIGN_IN", logText)
                val authSuccessLog = "[AUTH SUCCESS]\nUID=${user.uid}\nEMAIL=${user.email}"
                Log.i("FIREBASE_AUTH", authSuccessLog)
                addAuditLog(logText)
                addAuditLog(authSuccessLog)

                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Firebase user profile is null after credential sign-in"))
            }
        } catch (e: Exception) {
            val errLog = """
                [GOOGLE SIGN IN FAILED]
                Selected Email: $cleanEmail
                ID Token: PRESENT (${idToken.take(15)}...)
                Firebase UID: None
                Project ID: $projId
                Exception: ${e.message ?: e.javaClass.simpleName}
            """.trimIndent()
            Log.e("GOOGLE_SIGN_IN", errLog, e)
            addAuditLog(errLog)
            return@withContext Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        val fa = firebaseAuth
        if (fa == null) {
            return@withContext Result.failure(IllegalStateException("Firebase Auth is not initialized"))
        }

        try {
            val authResult = fa.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            val fbUser = authResult.user
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                val name = fbUser.displayName ?: cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                val user = CloudUser(
                    uid = fbUser.uid,
                    email = fbUser.email ?: cleanEmail,
                    displayName = name
                )
                _currentUser.value = user
                val profile = UserProfile(uid = user.uid, fullName = name, email = cleanEmail)
                _userProfile.value = profile
                saveUserToPrefs(user, name)
                val authSuccessLog = "[AUTH SUCCESS]\nUID=${user.uid}\nEMAIL=${user.email}"
                Log.i("FIREBASE_AUTH", authSuccessLog)
                addAuditLog(authSuccessLog)
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Failed to retrieve user profile from Firebase"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Firebase Auth sign in failed", e)
            addAuditLog("[AUTH ERROR] Email sign in failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, fullName: String): Result<CloudUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        val cleanName = fullName.trim().ifBlank { cleanEmail.substringBefore("@") }

        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (cleanPass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val fa = firebaseAuth
        if (fa == null) {
            return@withContext Result.failure(IllegalStateException("Firebase Auth is not initialized"))
        }

        try {
            val authResult = fa.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            val fbUser = authResult.user
            if (fbUser != null) {
                val user = CloudUser(
                    uid = fbUser.uid,
                    email = fbUser.email ?: cleanEmail,
                    displayName = cleanName
                )
                _currentUser.value = user
                val profile = UserProfile(uid = user.uid, fullName = cleanName, email = cleanEmail)
                _userProfile.value = profile
                saveUserToPrefs(user, cleanName)
                val authSuccessLog = "[AUTH SUCCESS]\nUID=${user.uid}\nEMAIL=${user.email}"
                Log.i("FIREBASE_AUTH", authSuccessLog)
                addAuditLog(authSuccessLog)
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Failed to create Firebase user account"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Firebase Auth sign up error", e)
            addAuditLog("[AUTH ERROR] Email sign up failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email is required"))
        }
        try {
            firebaseAuth?.sendPasswordResetEmail(cleanEmail)?.await()
            addAuditLog("[AUTH RESET] Password reset email sent to $cleanEmail")
        } catch (e: Exception) {
            Log.w("FirebaseSyncManager", "Firebase reset email warning: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth?.currentUser?.sendEmailVerification()?.await()
            addAuditLog("[AUTH VERIFY] Email verification sent")
        } catch (_: Exception) {}
        Result.success(Unit)
    }

    fun signOut() {
        val email = _currentUser.value?.email ?: ""
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        _userProfile.value = null
        _syncState.value = SyncState.IDLE
        clearPrefs()
        addAuditLog("[AUTH SIGNOUT] Signed out account $email")
    }

    // --- Profile Management ---

    fun formattedLastSync(): String {
        val time = _lastSyncTime.value
        return if (time == 0L) "Never" else {
            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            sdf.format(Date(time))
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _userProfile.value = profile
            val curUser = _currentUser.value
            if (curUser != null) {
                saveUserToPrefs(curUser, profile.fullName, profile.photoUrl)
                firestore?.collection("users")?.document(curUser.uid)?.set(
                    mapOf(
                        "uid" to profile.uid,
                        "fullName" to profile.fullName,
                        "email" to profile.email,
                        "photoUrl" to profile.photoUrl,
                        "country" to profile.country,
                        "preferredLanguage" to profile.preferredLanguage,
                        "preferredCurrency" to profile.preferredCurrency,
                        "lastSyncTime" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )?.await()
                addAuditLog("[PROFILE UPDATED] Saved profile for ${profile.email}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Cloud Storage Upload Helper ---

    suspend fun uploadFileToStorage(context: Context, fileUriString: String, docId: Long): String = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext fileUriString
        if (fileUriString.isBlank() || fileUriString.startsWith("http://") || fileUriString.startsWith("https://")) {
            return@withContext fileUriString
        }
        val st = firebaseStorage ?: return@withContext fileUriString
        try {
            val uri = Uri.parse(fileUriString)
            val ref = st.reference.child("users/${user.uid}/documents/doc_${docId}_${System.currentTimeMillis()}")
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) {
                ref.putStream(stream).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                addAuditLog("[STORAGE SUCCESS] Document $docId uploaded to Firebase Storage: $downloadUrl")
                return@withContext downloadUrl
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncManager", "Firebase Storage upload error: ${e.message}")
            addAuditLog("[STORAGE WARN] Upload to Storage failed for doc $docId: ${e.message}")
        }
        return@withContext fileUriString
    }

    // --- Download and Restore Cloud Data (Firestore -> Room DB) ---

    suspend fun downloadAndRestoreData(context: Context, database: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            _syncState.value = SyncState.OFFLINE
            addAuditLog("[RESTORE FAIL] Cannot restore: User is not signed in.")
            return@withContext Result.failure(Exception("Sign in required for cloud download"))
        }

        try {
            _syncState.value = SyncState.SYNCING
            addAuditLog("[RESTORE START] Pulling cloud records from Firestore for UID: ${user.uid}")

            val fs = firestore
            if (fs == null) {
                addAuditLog("[RESTORE WARN] Firestore service is null, skipping download.")
                _syncState.value = SyncState.OFFLINE
                return@withContext Result.success(0)
            }

            val userRef = fs.collection("users").document(user.uid)

            // Restore Profile
            try {
                val profDoc = userRef.get().await()
                if (profDoc.exists()) {
                    val pName = profDoc.getString("fullName") ?: profDoc.getString("displayName") ?: user.displayName ?: ""
                    val pPhoto = profDoc.getString("photoUrl") ?: user.photoUrl ?: ""
                    val pCountry = profDoc.getString("country") ?: "Pakistan"
                    val pLang = profDoc.getString("preferredLanguage") ?: "en"
                    val pCurr = profDoc.getString("preferredCurrency") ?: "PKR"

                    val p = UserProfile(
                        uid = user.uid,
                        fullName = pName,
                        email = user.email,
                        photoUrl = pPhoto,
                        country = pCountry,
                        preferredLanguage = pLang,
                        preferredCurrency = pCurr
                    )
                    _userProfile.value = p
                    saveUserToPrefs(user, pName, pPhoto)
                }
            } catch (e: Exception) {
                Log.w("FirebaseSyncManager", "Profile fetch warning: ${e.message}")
            }

            // 1. Restore Vehicles
            val vehicleDocs = userRef.collection("vehicles").get().await()
            var vehiclesRestored = 0
            for (doc in vehicleDocs.documents) {
                val v = doc.toVehicle()
                if (v != null) {
                    database.vehicleDao().insertVehicle(v)
                    vehiclesRestored++
                }
            }
            Log.i("FIRESTORE_SYNC", "[FIRESTORE VEHICLES FOUND]\ncount=$vehiclesRestored")
            Log.i("ROOM_RESTORE", "[ROOM RESTORE SUCCESS]\nvehiclesRestored=$vehiclesRestored")

            // 2. Restore Fuel Entries
            val fuelDocs = userRef.collection("fuelEntries").get().await()
            var fuelRestored = 0
            for (doc in fuelDocs.documents) {
                val f = doc.toFuelEntry()
                if (f != null) {
                    database.fuelDao().insertFuelEntry(f)
                    fuelRestored++
                }
            }

            // 3. Restore Maintenance Records
            val maintDocs = userRef.collection("maintenance").get().await()
            var maintRestored = 0
            for (doc in maintDocs.documents) {
                val m = doc.toMaintenance()
                if (m != null) {
                    database.maintenanceDao().insertMaintenance(m)
                    maintRestored++
                }
            }

            // 4. Restore Expenses
            val expDocs = userRef.collection("expenses").get().await()
            var expRestored = 0
            for (doc in expDocs.documents) {
                val e = doc.toExpense()
                if (e != null) {
                    database.expenseDao().insertExpense(e)
                    expRestored++
                }
            }

            // 5. Restore Documents
            val docDocs = userRef.collection("documents").get().await()
            var docRestored = 0
            for (doc in docDocs.documents) {
                val d = doc.toDocument()
                if (d != null) {
                    database.documentDao().insertDocument(d)
                    docRestored++
                }
            }

            // 6. Restore Insurance Policies
            val insDocs = userRef.collection("insurancePolicies").get().await()
            var insRestored = 0
            for (doc in insDocs.documents) {
                val i = doc.toInsurancePolicy()
                if (i != null) {
                    database.insurancePolicyDao().insertPolicy(i)
                    insRestored++
                }
            }

            // 7. Restore Reminders
            val remDocs = userRef.collection("reminders").get().await()
            var remRestored = 0
            for (doc in remDocs.documents) {
                val r = doc.toReminder()
                if (r != null) {
                    database.reminderDao().insertReminder(r)
                    remRestored++
                }
            }

            val totalRestored = vehiclesRestored + fuelRestored + maintRestored + expRestored + docRestored + insRestored + remRestored
            val counts = mapOf(
                "Vehicles" to vehiclesRestored,
                "Fuel Entries" to fuelRestored,
                "Maintenance" to maintRestored,
                "Expenses" to expRestored,
                "Documents" to docRestored,
                "Insurance" to insRestored,
                "Reminders" to remRestored
            )

            val now = System.currentTimeMillis()
            _lastDownloadTime.value = now
            _lastSyncTime.value = now
            _lastDownloadCounts.value = counts
            _syncState.value = SyncState.SUCCESS

            addAuditLog("[RESTORE SUCCESS] Restored $totalRestored records into Room DB ($vehiclesRestored Vehicles, $fuelRestored Fuel, $maintRestored Maint, $docRestored Docs, $expRestored Expenses, $insRestored Insurance, $remRestored Reminders)")

            Result.success(totalRestored)
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Download and restore error", e)
            _syncState.value = SyncState.ERROR
            addAuditLog("[RESTORE ERROR] Cloud restore failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Upload Local Room Data to Cloud (Firestore + Storage) ---

    suspend fun uploadAllData(
        context: Context,
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
            addAuditLog("[UPLOAD FAIL] Cannot upload: User not signed in.")
            return@withContext Result.failure(Exception("Sign in required for cloud sync"))
        }

        try {
            _syncState.value = SyncState.SYNCING
            addAuditLog("[UPLOAD START] Uploading local records to Firestore for UID: ${user.uid}")

            val fs = firestore
            if (fs != null) {
                val userRef = fs.collection("users").document(user.uid)
                val batch = fs.batch()

                val p = _userProfile.value
                val profileMap = mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to (p?.fullName?.ifBlank { null } ?: user.displayName ?: ""),
                    "photoUrl" to (p?.photoUrl?.ifBlank { null } ?: user.photoUrl ?: ""),
                    "country" to (p?.country ?: "Pakistan"),
                    "preferredLanguage" to (p?.preferredLanguage ?: "en"),
                    "preferredCurrency" to (p?.preferredCurrency ?: "PKR"),
                    "lastSyncTime" to System.currentTimeMillis()
                )
                batch.set(userRef, profileMap, SetOptions.merge())

                for (v in vehicles) {
                    batch.set(userRef.collection("vehicles").document(v.id.toString()), v.toMap(), SetOptions.merge())
                }
                for (f in fuelEntries) {
                    batch.set(userRef.collection("fuelEntries").document(f.id.toString()), f.toMap(), SetOptions.merge())
                }
                for (m in maintenanceRecords) {
                    batch.set(userRef.collection("maintenance").document(m.id.toString()), m.toMap(), SetOptions.merge())
                }
                for (e in expenses) {
                    batch.set(userRef.collection("expenses").document(e.id.toString()), e.toMap(), SetOptions.merge())
                }
                for (d in documents) {
                    val cloudUri = uploadFileToStorage(context, d.fileUri, d.id)
                    val updatedDoc = if (cloudUri != d.fileUri) d.copy(fileUri = cloudUri) else d
                    batch.set(userRef.collection("documents").document(updatedDoc.id.toString()), updatedDoc.toMap(), SetOptions.merge())
                }
                for (i in insurancePolicies) {
                    batch.set(userRef.collection("insurancePolicies").document(i.id.toString()), i.toMap(), SetOptions.merge())
                }
                for (r in reminders) {
                    batch.set(userRef.collection("reminders").document(r.id.toString()), r.toMap(), SetOptions.merge())
                }

                batch.commit().await()
                addAuditLog("[UPLOAD SUCCESS] Committed Firestore batch (${vehicles.size} vehicles, ${fuelEntries.size} fuel, ${maintenanceRecords.size} maint, ${documents.size} docs, ${expenses.size} expenses)")
            }

            val now = System.currentTimeMillis()
            _lastUploadTime.value = now
            _lastSyncTime.value = now
            _lastUploadCounts.value = mapOf(
                "Vehicles" to vehicles.size,
                "Fuel Entries" to fuelEntries.size,
                "Maintenance" to maintenanceRecords.size,
                "Expenses" to expenses.size,
                "Documents" to documents.size,
                "Insurance" to insurancePolicies.size,
                "Reminders" to reminders.size
            )
            _syncState.value = SyncState.SUCCESS
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Upload error", e)
            _syncState.value = SyncState.ERROR
            addAuditLog("[UPLOAD ERROR] Upload to Firestore failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncAllData(
        context: Context,
        vehicles: List<Vehicle>,
        fuelEntries: List<FuelEntry>,
        maintenanceRecords: List<Maintenance>,
        expenses: List<Expense>,
        documents: List<Document>,
        insurancePolicies: List<InsurancePolicy>,
        reminders: List<Reminder>
    ): Result<Unit> = uploadAllData(
        context = context,
        vehicles = vehicles,
        fuelEntries = fuelEntries,
        maintenanceRecords = maintenanceRecords,
        expenses = expenses,
        documents = documents,
        insurancePolicies = insurancePolicies,
        reminders = reminders
    )

    // --- Full Bidirectional Sync ---

    suspend fun performFullBidirectionalSync(context: Context, database: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            addAuditLog("[FULL SYNC CANCEL] User not signed in.")
            return@withContext Result.failure(Exception("Not signed in"))
        }

        addAuditLog("[FULL SYNC START] Initiating 2-Way Sync for ${user.email}...")

        // Step 1: Download & Restore from Cloud first
        downloadAndRestoreData(context, database)

        // Step 2: Fetch current local DB items to upload back to Cloud
        val localVehicles = database.vehicleDao().getAllVehicles().first()
        val localFuel = database.fuelDao().getAllFuelEntries().first()
        val localMaint = database.maintenanceDao().getAllMaintenance().first()
        val localExp = database.expenseDao().getAllExpenses().first()
        val localDocs = database.documentDao().getAllDocumentsSync()
        val localIns = database.insurancePolicyDao().getAllInsurancePolicies().first()
        val localRem = database.reminderDao().getAllReminders().first()

        // Step 3: Upload all local data
        uploadAllData(
            context = context,
            vehicles = localVehicles,
            fuelEntries = localFuel,
            maintenanceRecords = localMaint,
            expenses = localExp,
            documents = localDocs,
            insurancePolicies = localIns,
            reminders = localRem
        )
    }

    // --- Single Item Immediate Operations ---

    suspend fun uploadSingleVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("vehicles")
                ?.document(vehicle.id.toString())
                ?.set(vehicle.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved vehicle '${vehicle.vehicleName}' (ID: ${vehicle.id}) to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Vehicle upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleVehicle(vehicleId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("vehicles")
                ?.document(vehicleId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed vehicle ID $vehicleId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Vehicle delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleFuelEntry(fuelEntry: FuelEntry) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("fuelEntries")
                ?.document(fuelEntry.id.toString())
                ?.set(fuelEntry.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved fuel entry ${fuelEntry.id} to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Fuel entry upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleFuelEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("fuelEntries")
                ?.document(entryId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed fuel entry ID $entryId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Fuel entry delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleMaintenance(maintenance: Maintenance) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("maintenance")
                ?.document(maintenance.id.toString())
                ?.set(maintenance.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved maintenance '${maintenance.serviceTitle}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Maintenance upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleMaintenance(maintId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("maintenance")
                ?.document(maintId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed maintenance ID $maintId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Maintenance delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("expenses")
                ?.document(expense.id.toString())
                ?.set(expense.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved expense '${expense.title}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Expense upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleExpense(expenseId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("expenses")
                ?.document(expenseId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed expense ID $expenseId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Expense delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleDocument(context: Context, document: Document) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            val cloudUri = uploadFileToStorage(context, document.fileUri, document.id)
            val docToSave = if (cloudUri != document.fileUri) document.copy(fileUri = cloudUri) else document
            firestore?.collection("users")?.document(user.uid)?.collection("documents")
                ?.document(docToSave.id.toString())
                ?.set(docToSave.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved document '${document.docTitle}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Document upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleDocument(documentId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("documents")
                ?.document(documentId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed document ID $documentId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Document delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleInsurance(policy: InsurancePolicy) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("insurancePolicies")
                ?.document(policy.id.toString())
                ?.set(policy.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved insurance policy '${policy.policyNumber}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Insurance upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleInsurance(policyId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("insurancePolicies")
                ?.document(policyId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed insurance policy ID $policyId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Insurance delete failed: ${e.message}")
        }
    }

    suspend fun uploadSingleReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("reminders")
                ?.document(reminder.id.toString())
                ?.set(reminder.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved reminder '${reminder.reminderTitle}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Reminder upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleReminder(reminderId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("reminders")
                ?.document(reminderId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed reminder ID $reminderId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Reminder delete failed: ${e.message}")
        }
    }

    // --- Model Map Converters for Clean Firestore Serialization ---

    private fun Vehicle.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleName" to vehicleName,
        "vehicleType" to vehicleType,
        "brand" to brand,
        "model" to model,
        "manufacturingYear" to manufacturingYear,
        "registrationNumber" to registrationNumber,
        "fuelType" to fuelType,
        "odometerReading" to odometerReading,
        "notes" to notes,
        "createdAt" to createdAt
    )

    private fun FuelEntry.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "fuelDate" to fuelDate,
        "fuelType" to fuelType,
        "fuelQuantity" to fuelQuantity,
        "amountPaid" to amountPaid,
        "currentOdometer" to currentOdometer,
        "fuelStationName" to fuelStationName,
        "notes" to notes,
        "createdAt" to createdAt
    )

    private fun Maintenance.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "serviceTitle" to serviceTitle,
        "serviceType" to serviceType,
        "serviceDate" to serviceDate,
        "currentOdometer" to currentOdometer,
        "serviceCost" to serviceCost,
        "workshopName" to workshopName,
        "notes" to notes,
        "invoicePhotoUri" to invoicePhotoUri,
        "nextDueServiceDate" to nextDueServiceDate,
        "reminderDate" to reminderDate,
        "createdAt" to createdAt
    )

    private fun Expense.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "title" to title,
        "category" to category,
        "amount" to amount,
        "date" to date,
        "notes" to notes,
        "createdAt" to createdAt
    )

    private fun Document.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "docTitle" to docTitle,
        "docType" to docType,
        "issueDate" to issueDate,
        "expiryDate" to expiryDate,
        "notes" to notes,
        "fileUri" to fileUri,
        "mimeType" to mimeType,
        "fileSize" to fileSize,
        "reminderDaysBefore" to reminderDaysBefore,
        "createdAt" to createdAt
    )

    private fun InsurancePolicy.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "providerName" to providerName,
        "policyNumber" to policyNumber,
        "coverageType" to coverageType,
        "premiumAmount" to premiumAmount,
        "startDate" to startDate,
        "expiryDate" to expiryDate,
        "agentContact" to agentContact,
        "notes" to notes,
        "isAutoRenewEnabled" to isAutoRenewEnabled,
        "createdAt" to createdAt
    )

    private fun Reminder.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "reminderTitle" to reminderTitle,
        "reminderType" to reminderType,
        "dueDate" to dueDate,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt
    )

    // --- Document Mapping Extensions ---

    private fun DocumentSnapshot.toVehicle(): Vehicle? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return Vehicle(
            id = idVal,
            vehicleName = getString("vehicleName") ?: "",
            vehicleType = getString("vehicleType") ?: "Car",
            brand = getString("brand") ?: "",
            model = getString("model") ?: "",
            manufacturingYear = getString("manufacturingYear") ?: "",
            registrationNumber = getString("registrationNumber") ?: "",
            fuelType = getString("fuelType") ?: "Petrol",
            odometerReading = getString("odometerReading") ?: "0",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toFuelEntry(): FuelEntry? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return FuelEntry(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            fuelDate = getString("fuelDate") ?: "",
            fuelType = getString("fuelType") ?: "Petrol",
            fuelQuantity = getString("fuelQuantity") ?: "0",
            amountPaid = getString("amountPaid") ?: "0",
            currentOdometer = getString("currentOdometer") ?: "0",
            fuelStationName = getString("fuelStationName") ?: "",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toMaintenance(): Maintenance? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return Maintenance(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            serviceTitle = getString("serviceTitle") ?: getString("title") ?: "Service",
            serviceType = getString("serviceType") ?: "Routine Service",
            serviceDate = getString("serviceDate") ?: "",
            currentOdometer = getString("currentOdometer") ?: "0",
            serviceCost = getString("serviceCost") ?: "0",
            workshopName = getString("workshopName") ?: "",
            notes = getString("notes") ?: "",
            invoicePhotoUri = getString("invoicePhotoUri") ?: "",
            nextDueServiceDate = getString("nextDueServiceDate") ?: "",
            reminderDate = getString("reminderDate") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toExpense(): Expense? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return Expense(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            title = getString("title") ?: "",
            category = getString("category") ?: "Other",
            amount = getDouble("amount") ?: 0.0,
            date = getString("date") ?: "",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toDocument(): Document? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return Document(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            docTitle = getString("docTitle") ?: getString("title") ?: "",
            docType = getString("docType") ?: getString("category") ?: "Registration",
            issueDate = getString("issueDate") ?: "",
            expiryDate = getString("expiryDate") ?: "",
            notes = getString("notes") ?: "",
            fileUri = getString("fileUri") ?: getString("docPath") ?: "",
            mimeType = getString("mimeType") ?: "",
            fileSize = getLong("fileSize") ?: 0L,
            reminderDaysBefore = getLong("reminderDaysBefore")?.toInt() ?: 7,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toInsurancePolicy(): InsurancePolicy? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return InsurancePolicy(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            providerName = getString("providerName") ?: "",
            policyNumber = getString("policyNumber") ?: "",
            coverageType = getString("coverageType") ?: "Comprehensive",
            premiumAmount = getDouble("premiumAmount") ?: 0.0,
            startDate = getString("startDate") ?: "",
            expiryDate = getString("expiryDate") ?: "",
            agentContact = getString("agentContact") ?: "",
            notes = getString("notes") ?: "",
            isAutoRenewEnabled = getBoolean("isAutoRenewEnabled") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toReminder(): Reminder? {
        if (!exists()) return null
        val idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        return Reminder(
            id = idVal,
            vehicleId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L,
            vehicleName = getString("vehicleName") ?: "",
            reminderTitle = getString("reminderTitle") ?: getString("title") ?: "",
            reminderType = getString("reminderType") ?: getString("category") ?: "Oil Change",
            dueDate = getString("dueDate") ?: "",
            isCompleted = getBoolean("isCompleted") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }
}
