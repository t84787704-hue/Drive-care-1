package com.drivecare.app.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    var syncDemoData: Boolean = false

    private val syncMutex = Mutex()

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

        if (firebaseAuth != null) {
            _currentUser.value = null
            _userProfile.value = null
            clearPrefs()
            return
        }

        // Restore from SharedPreferences only if firebaseAuth is not initialized
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
        try {
            prefs?.edit()?.clear()?.commit()
        } catch (_: Exception) {}
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

    fun signOut(context: Context? = null) {
        val email = _currentUser.value?.email ?: ""
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Firebase signOut warning: ${e.message}")
        }
        if (context != null) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (e: Exception) {
                Log.e("FirebaseSyncManager", "GoogleSignIn signOut warning: ${e.message}")
            }
        }
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
                    val vWithOwner = if (v.ownerUserId.isBlank()) v.copy(ownerUserId = user.uid) else v
                    database.vehicleDao().insertVehicle(vWithOwner)
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
                    val fWithOwner = if (f.ownerUserId.isBlank()) f.copy(ownerUserId = user.uid) else f
                    database.fuelDao().insertFuelEntry(fWithOwner)
                    fuelRestored++
                }
            }

            // 3. Restore Maintenance Records
            val maintDocs = userRef.collection("maintenance").get().await()
            var maintRestored = 0
            for (doc in maintDocs.documents) {
                val m = doc.toMaintenance()
                if (m != null) {
                    val mWithOwner = if (m.ownerUserId.isBlank()) m.copy(ownerUserId = user.uid) else m
                    database.maintenanceDao().insertMaintenance(mWithOwner)
                    maintRestored++
                }
            }

            // 4. Restore Expenses
            val expDocs = userRef.collection("expenses").get().await()
            var expRestored = 0
            for (doc in expDocs.documents) {
                val e = doc.toExpense()
                if (e != null) {
                    val eWithOwner = if (e.ownerUserId.isBlank()) e.copy(ownerUserId = user.uid) else e
                    database.expenseDao().insertExpense(eWithOwner)
                    expRestored++
                }
            }

            // 5. Restore Documents
            val docDocs = userRef.collection("documents").get().await()
            var docRestored = 0
            for (doc in docDocs.documents) {
                val d = doc.toDocument()
                if (d != null) {
                    val dWithOwner = if (d.ownerUserId.isBlank()) d.copy(ownerUserId = user.uid) else d
                    database.documentDao().insertDocument(dWithOwner)
                    docRestored++
                }
            }

            // 6. Restore Insurance Policies
            val insDocs = userRef.collection("insurancePolicies").get().await()
            var insRestored = 0
            for (doc in insDocs.documents) {
                val i = doc.toInsurancePolicy()
                if (i != null) {
                    val iWithOwner = if (i.ownerUserId.isBlank()) i.copy(ownerUserId = user.uid) else i
                    database.insurancePolicyDao().insertPolicy(iWithOwner)
                    insRestored++
                }
            }

            // 7. Restore Reminders
            val remDocs = userRef.collection("reminders").get().await()
            var remRestored = 0
            for (doc in remDocs.documents) {
                val r = doc.toReminder()
                if (r != null) {
                    val rWithOwner = if (r.ownerUserId.isBlank()) r.copy(ownerUserId = user.uid) else r
                    database.reminderDao().insertReminder(rWithOwner)
                    remRestored++
                }
            }

            // 8. Restore Geofences
            val geoDocs = userRef.collection("geofences").get().await()
            var geoRestored = 0
            for (doc in geoDocs.documents) {
                val g = doc.toGeofenceZone()
                if (g != null) {
                    val gWithOwner = if (g.ownerUserId.isBlank()) g.copy(ownerUserId = user.uid) else g
                    database.geofenceZoneDao().insertGeofence(gWithOwner)
                    geoRestored++
                }
            }

            val totalRestored = vehiclesRestored + fuelRestored + maintRestored + expRestored + docRestored + insRestored + remRestored + geoRestored
            val counts = mapOf(
                "Vehicles" to vehiclesRestored,
                "Fuel Entries" to fuelRestored,
                "Maintenance" to maintRestored,
                "Expenses" to expRestored,
                "Documents" to docRestored,
                "Insurance" to insRestored,
                "Reminders" to remRestored,
                "Geofences" to geoRestored
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
        reminders: List<Reminder>,
        geofences: List<GeofenceZone> = emptyList()
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

                val vList = if (syncDemoData) vehicles else vehicles.filter { !it.isDemo }
                val fList = if (syncDemoData) fuelEntries else fuelEntries.filter { !it.isDemo }
                val mList = if (syncDemoData) maintenanceRecords else maintenanceRecords.filter { !it.isDemo }
                val eList = if (syncDemoData) expenses else expenses.filter { !it.isDemo }
                val dList = if (syncDemoData) documents else documents.filter { !it.isDemo }
                val iList = if (syncDemoData) insurancePolicies else insurancePolicies.filter { !it.isDemo }
                val rList = if (syncDemoData) reminders else reminders.filter { !it.isDemo }
                val gList = if (syncDemoData) geofences else geofences.filter { !it.isDemo }

                for (v in vList) {
                    batch.set(userRef.collection("vehicles").document(v.id.toString()), v.toMap(), SetOptions.merge())
                }
                for (f in fList) {
                    batch.set(userRef.collection("fuelEntries").document(f.id.toString()), f.toMap(), SetOptions.merge())
                }
                for (m in mList) {
                    batch.set(userRef.collection("maintenance").document(m.id.toString()), m.toMap(), SetOptions.merge())
                }
                for (e in eList) {
                    batch.set(userRef.collection("expenses").document(e.id.toString()), e.toMap(), SetOptions.merge())
                }
                for (d in dList) {
                    val cloudUri = uploadFileToStorage(context, d.fileUri, d.id)
                    val updatedDoc = if (cloudUri != d.fileUri) d.copy(fileUri = cloudUri) else d
                    batch.set(userRef.collection("documents").document(updatedDoc.id.toString()), updatedDoc.toMap(), SetOptions.merge())
                }
                for (i in iList) {
                    batch.set(userRef.collection("insurancePolicies").document(i.id.toString()), i.toMap(), SetOptions.merge())
                }
                for (r in rList) {
                    batch.set(userRef.collection("reminders").document(r.id.toString()), r.toMap(), SetOptions.merge())
                }
                for (g in gList) {
                    batch.set(userRef.collection("geofences").document(g.id.toString()), g.toMap(), SetOptions.merge())
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

    suspend fun performFullBidirectionalSync(context: Context, database: AppDatabase): Result<Unit> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val user = _currentUser.value
            if (user == null) {
                addAuditLog("[FULL SYNC CANCEL] User not signed in.")
                return@withContext Result.failure(Exception("Not signed in"))
            }

            addAuditLog("[FULL SYNC START] Initiating 2-Way Sync for ${user.email}...")

            // Step 1: Download & Restore from Cloud first
            downloadAndRestoreData(context, database)

            // Step 2: Fetch current local DB items to upload back to Cloud
            val localVehicles = database.vehicleDao().getVehiclesForUserSync(user.uid)
            val localFuel = database.fuelDao().getFuelEntriesForUserSync(user.uid)
            val localMaint = database.maintenanceDao().getMaintenanceForUserSync(user.uid)
            val localExp = database.expenseDao().getExpensesForUserSync(user.uid)
            val localDocs = database.documentDao().getDocumentsForUserSync(user.uid)
            val localIns = database.insurancePolicyDao().getPoliciesForUserSync(user.uid)
            val localRem = database.reminderDao().getRemindersForUserSync(user.uid)
            val localGeo = database.geofenceZoneDao().getGeofencesForUserSync(user.uid)

            // Step 3: Upload all local data
            uploadAllData(
                context = context,
                vehicles = localVehicles,
                fuelEntries = localFuel,
                maintenanceRecords = localMaint,
                expenses = localExp,
                documents = localDocs,
                insurancePolicies = localIns,
                reminders = localRem,
                geofences = localGeo
            )
        }
    }

    // --- Single Item Immediate Operations ---

    suspend fun uploadSingleVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        if (vehicle.isDemo && !syncDemoData) return@withContext
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
            val userRef = firestore?.collection("users")?.document(user.uid) ?: return@withContext
            userRef.collection("vehicles").document(vehicleId.toString()).delete().await()

            val childCollections = listOf("fuelEntries", "maintenance", "documents", "expenses", "insurancePolicies", "reminders", "geofences", "vehicleShares", "tripLogs")
            childCollections.forEach { col ->
                try {
                    val querySnap = userRef.collection(col).whereEqualTo("vehicleId", vehicleId).get().await()
                    for (doc in querySnap.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            addAuditLog("[FIRESTORE DELETE SUCCESS] Removed vehicle ID $vehicleId and linked subcollections from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Vehicle delete failed: ${e.message}")
            throw e
        }
    }

    suspend fun deleteStorageFilesForUrls(urls: List<String>) = withContext(Dispatchers.IO) {
        val st = firebaseStorage ?: return@withContext
        urls.distinct().filter { it.isNotBlank() }.forEach { url ->
            try {
                if (url.startsWith("https://firebasestorage.googleapis.com") || url.startsWith("gs://")) {
                    st.getReferenceFromUrl(url).delete().await()
                    addAuditLog("[STORAGE DELETE SUCCESS] Deleted Firebase Storage object: $url")
                }
            } catch (e: Exception) {
                Log.w("FirebaseSyncManager", "Storage file delete error: ${e.message}")
            }
        }
    }

    suspend fun uploadSingleFuelEntry(fuelEntry: FuelEntry) = withContext(Dispatchers.IO) {
        if (fuelEntry.isDemo && !syncDemoData) return@withContext
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
        if (maintenance.isDemo && !syncDemoData) return@withContext
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
        if (expense.isDemo && !syncDemoData) return@withContext
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
        if (document.isDemo && !syncDemoData) return@withContext
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

    suspend fun uploadSingleInsurance(context: Context? = null, policy: InsurancePolicy) = withContext(Dispatchers.IO) {
        if (policy.isDemo && !syncDemoData) return@withContext
        val user = _currentUser.value ?: return@withContext
        try {
            var updatedPolicy = policy
            if (context != null && policy.documentUri.isNotBlank() && !policy.documentUri.startsWith("http://") && !policy.documentUri.startsWith("https://")) {
                val cloudUrl = uploadFileToStorage(context, policy.documentUri, policy.id)
                if (cloudUrl != policy.documentUri) {
                    updatedPolicy = policy.copy(documentUri = cloudUrl)
                }
            }
            firestore?.collection("users")?.document(user.uid)?.collection("insurancePolicies")
                ?.document(updatedPolicy.id.toString())
                ?.set(updatedPolicy.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved insurance policy '${updatedPolicy.policyNumber}' to Firestore")
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
        if (reminder.isDemo && !syncDemoData) return@withContext
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

    suspend fun uploadSingleGeofence(geofence: GeofenceZone) = withContext(Dispatchers.IO) {
        if (geofence.isDemo && !syncDemoData) return@withContext
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("geofences")
                ?.document(geofence.id.toString())
                ?.set(geofence.toMap(), SetOptions.merge())?.await()
            addAuditLog("[REALTIME PUSH] Saved geofence '${geofence.zoneName}' to Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Geofence upload failed: ${e.message}")
        }
    }

    suspend fun deleteSingleGeofence(geofenceId: Long) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        try {
            firestore?.collection("users")?.document(user.uid)?.collection("geofences")
                ?.document(geofenceId.toString())
                ?.delete()?.await()
            addAuditLog("[REALTIME DELETE] Removed geofence ID $geofenceId from Firestore")
        } catch (e: Exception) {
            addAuditLog("[REALTIME ERROR] Geofence delete failed: ${e.message}")
        }
    }

    // --- User Discovery, Social, Vehicle Sharing & Family Access Methods ---

    suspend fun searchUsers(query: String): List<PublicUserProfile> = withContext(Dispatchers.IO) {
        val currentUid = _currentUser.value?.uid ?: ""
        val results = mutableListOf<PublicUserProfile>()
        if (query.isBlank()) return@withContext results
        try {
            val qTrim = query.trim().lowercase(Locale.ROOT)
            val snapshot = firestore?.collection("users")?.get()?.await()
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    if (uid == currentUid) continue
                    val email = doc.getString("email") ?: ""
                    val name = doc.getString("fullName") ?: doc.getString("displayName") ?: doc.getString("name") ?: ""
                    val country = doc.getString("country") ?: ""
                    val currency = doc.getString("preferredCurrency") ?: "PKR"
                    val photo = doc.getString("photoUrl") ?: ""
                    val joinDate = doc.getLong("createdAt") ?: 0L

                    if (email.lowercase(Locale.ROOT).contains(qTrim) || name.lowercase(Locale.ROOT).contains(qTrim)) {
                        results.add(
                            PublicUserProfile(
                                uid = uid,
                                displayName = name.ifBlank { email.substringBefore("@") },
                                email = email,
                                country = country,
                                preferredCurrency = currency,
                                photoUrl = photo,
                                joinDate = joinDate
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] User search failed: ${e.message}")
        }
        results
    }

    suspend fun sendFriendRequest(targetUser: PublicUserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val sender = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val senderProf = _userProfile.value
        if (sender.uid == targetUser.uid) return@withContext Result.failure(Exception("Cannot send request to yourself"))

        try {
            val reqQuery = firestore?.collection("friend_requests")
                ?.whereEqualTo("senderUid", sender.uid)
                ?.whereEqualTo("receiverUid", targetUser.uid)
                ?.get()?.await()
            if (reqQuery != null && !reqQuery.isEmpty) {
                val existing = reqQuery.documents.firstOrNull()?.getString("status")
                if (existing == "Pending" || existing == "Accepted") {
                    return@withContext Result.failure(Exception("Request or friendship already exists"))
                }
            }

            val sortedId = listOf(sender.uid, targetUser.uid).sorted().joinToString("_")
            val friendshipDoc = firestore?.collection("friendships")?.document(sortedId)?.get()?.await()
            if (friendshipDoc != null && friendshipDoc.exists()) {
                return@withContext Result.failure(Exception("You are already friends with this user"))
            }

            val reqId = firestore?.collection("friend_requests")?.document()?.id ?: System.currentTimeMillis().toString()
            val requestData = mapOf(
                "id" to reqId,
                "senderUid" to sender.uid,
                "senderName" to (senderProf?.fullName ?: sender.displayName ?: "Driver"),
                "senderEmail" to sender.email,
                "senderPhoto" to (senderProf?.photoUrl ?: sender.photoUrl ?: ""),
                "receiverUid" to targetUser.uid,
                "receiverName" to targetUser.displayName,
                "receiverEmail" to targetUser.email,
                "receiverPhoto" to targetUser.photoUrl,
                "status" to "Pending",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore?.collection("friend_requests")?.document(reqId)?.set(requestData)?.await()

            sendNotification(
                recipientUid = targetUser.uid,
                title = "New Friend Request",
                message = "${senderProf?.fullName ?: sender.displayName ?: "A user"} sent you a friend request.",
                type = "FRIEND_REQUEST"
            )

            addAuditLog("[SOCIAL] Sent friend request to ${targetUser.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] Send friend request failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("friend_requests")?.document(request.id)
                ?.update(mapOf("status" to "Accepted", "updatedAt" to System.currentTimeMillis()))?.await()

            val sortedId = listOf(request.senderUid, request.receiverUid).sorted().joinToString("_")
            val friendshipData = mapOf(
                "id" to sortedId,
                "user1Uid" to request.senderUid,
                "user2Uid" to request.receiverUid,
                "user1Name" to request.senderName,
                "user1Email" to request.senderEmail,
                "user1Photo" to request.senderPhoto,
                "user2Name" to request.receiverName,
                "user2Email" to request.receiverEmail,
                "user2Photo" to request.receiverPhoto,
                "createdAt" to System.currentTimeMillis()
            )

            firestore?.collection("friendships")?.document(sortedId)?.set(friendshipData, SetOptions.merge())?.await()

            sendNotification(
                recipientUid = request.senderUid,
                title = "Friend Request Accepted",
                message = "${request.receiverName} accepted your friend request!",
                type = "FRIEND_ACCEPTED"
            )

            addAuditLog("[SOCIAL] Accepted friend request from ${request.senderEmail}")
            Result.success(Unit)
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] Accept friend request failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("friend_requests")?.document(requestId)
                ?.update(mapOf("status" to "Rejected", "updatedAt" to System.currentTimeMillis()))?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("friend_requests")?.document(requestId)
                ?.update(mapOf("status" to "Cancelled", "updatedAt" to System.currentTimeMillis()))?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIncomingFriendRequests(): List<FriendRequest> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val list = mutableListOf<FriendRequest>()
        try {
            val snapshot = firestore?.collection("friend_requests")
                ?.whereEqualTo("receiverUid", uid)
                ?.whereEqualTo("status", "Pending")
                ?.get()?.await()
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    list.add(
                        FriendRequest(
                            id = doc.id,
                            senderUid = doc.getString("senderUid") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            senderPhoto = doc.getString("senderPhoto") ?: "",
                            receiverUid = doc.getString("receiverUid") ?: "",
                            receiverName = doc.getString("receiverName") ?: "",
                            receiverEmail = doc.getString("receiverEmail") ?: "",
                            receiverPhoto = doc.getString("receiverPhoto") ?: "",
                            status = doc.getString("status") ?: "Pending",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] Fetch incoming requests failed: ${e.message}")
        }
        list
    }

    suspend fun getOutgoingFriendRequests(): List<FriendRequest> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val list = mutableListOf<FriendRequest>()
        try {
            val snapshot = firestore?.collection("friend_requests")
                ?.whereEqualTo("senderUid", uid)
                ?.get()?.await()
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    list.add(
                        FriendRequest(
                            id = doc.id,
                            senderUid = doc.getString("senderUid") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            senderPhoto = doc.getString("senderPhoto") ?: "",
                            receiverUid = doc.getString("receiverUid") ?: "",
                            receiverName = doc.getString("receiverName") ?: "",
                            receiverEmail = doc.getString("receiverEmail") ?: "",
                            receiverPhoto = doc.getString("receiverPhoto") ?: "",
                            status = doc.getString("status") ?: "Pending",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] Fetch outgoing requests failed: ${e.message}")
        }
        list
    }

    suspend fun getFriendships(): List<Friendship> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val list = mutableListOf<Friendship>()
        try {
            val snap1 = firestore?.collection("friendships")?.whereEqualTo("user1Uid", uid)?.get()?.await()
            val snap2 = firestore?.collection("friendships")?.whereEqualTo("user2Uid", uid)?.get()?.await()
            val allDocs = (snap1?.documents ?: emptyList()) + (snap2?.documents ?: emptyList())
            val seenIds = mutableSetOf<String>()

            for (doc in allDocs) {
                if (doc.id in seenIds) continue
                seenIds.add(doc.id)
                list.add(
                    Friendship(
                        id = doc.id,
                        user1Uid = doc.getString("user1Uid") ?: "",
                        user2Uid = doc.getString("user2Uid") ?: "",
                        user1Name = doc.getString("user1Name") ?: "",
                        user1Email = doc.getString("user1Email") ?: "",
                        user1Photo = doc.getString("user1Photo") ?: "",
                        user2Name = doc.getString("user2Name") ?: "",
                        user2Email = doc.getString("user2Email") ?: "",
                        user2Photo = doc.getString("user2Photo") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            addAuditLog("[SOCIAL ERROR] Fetch friendships failed: ${e.message}")
        }
        list
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("friendships")?.document(friendshipId)?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareVehicleWithUser(
        vehicle: Vehicle,
        targetUserUid: String,
        targetEmail: String,
        targetName: String,
        permission: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val owner = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val ownerProf = _userProfile.value
        try {
            val shareId = firestore?.collection("shared_vehicles")?.document()?.id ?: System.currentTimeMillis().toString()
            val data = mapOf(
                "id" to shareId,
                "vehicleId" to vehicle.id,
                "vehicleName" to vehicle.vehicleName,
                "ownerUid" to owner.uid,
                "ownerName" to (ownerProf?.fullName ?: owner.displayName ?: "Owner"),
                "sharedWithUid" to targetUserUid,
                "sharedWithEmail" to targetEmail,
                "sharedWithName" to targetName,
                "permission" to permission,
                "createdAt" to System.currentTimeMillis()
            )

            firestore?.collection("shared_vehicles")?.document(shareId)?.set(data)?.await()

            if (targetUserUid.isNotBlank()) {
                sendNotification(
                    recipientUid = targetUserUid,
                    title = "Vehicle Access Granted",
                    message = "${ownerProf?.fullName ?: owner.displayName ?: "Owner"} shared vehicle '${vehicle.vehicleName}' with you ($permission level).",
                    type = "VEHICLE_SHARED"
                )
            }

            addAuditLog("[SHARING] Shared vehicle '${vehicle.vehicleName}' with $targetEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            addAuditLog("[SHARING ERROR] Share vehicle failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getSharedVehiclesForUser(): List<SharedVehicle> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val email = _currentUser.value?.email ?: ""
        val list = mutableListOf<SharedVehicle>()
        try {
            val snapUid = firestore?.collection("shared_vehicles")?.whereEqualTo("sharedWithUid", uid)?.get()?.await()
            val snapEmail = if (email.isNotBlank()) firestore?.collection("shared_vehicles")?.whereEqualTo("sharedWithEmail", email)?.get()?.await() else null
            val allDocs = (snapUid?.documents ?: emptyList()) + (snapEmail?.documents ?: emptyList())
            val seenIds = mutableSetOf<String>()

            for (doc in allDocs) {
                if (doc.id in seenIds) continue
                seenIds.add(doc.id)
                list.add(
                    SharedVehicle(
                        id = doc.id,
                        vehicleId = doc.getLong("vehicleId") ?: 0L,
                        vehicleName = doc.getString("vehicleName") ?: "",
                        ownerUid = doc.getString("ownerUid") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        sharedWithUid = doc.getString("sharedWithUid") ?: "",
                        sharedWithEmail = doc.getString("sharedWithEmail") ?: "",
                        sharedWithName = doc.getString("sharedWithName") ?: "",
                        permission = doc.getString("permission") ?: "Viewer",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            addAuditLog("[SHARING ERROR] Get shared vehicles failed: ${e.message}")
        }
        list
    }

    suspend fun updateVehicleSharePermission(shareId: String, newPermission: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("shared_vehicles")?.document(shareId)
                ?.update("permission", newPermission)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeVehicleShare(shareId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("shared_vehicles")?.document(shareId)?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFamilyGroup(groupName: String): Result<FamilyGroup> = withContext(Dispatchers.IO) {
        val owner = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        val ownerProf = _userProfile.value
        try {
            val groupId = firestore?.collection("family_groups")?.document()?.id ?: System.currentTimeMillis().toString()
            val ownerName = ownerProf?.fullName ?: owner.displayName ?: "Owner"
            val groupData = mapOf(
                "id" to groupId,
                "groupName" to groupName,
                "ownerUid" to owner.uid,
                "ownerName" to ownerName,
                "createdAt" to System.currentTimeMillis()
            )
            firestore?.collection("family_groups")?.document(groupId)?.set(groupData)?.await()

            val memberId = "${groupId}_${owner.uid}"
            val memberData = mapOf(
                "id" to memberId,
                "groupId" to groupId,
                "uid" to owner.uid,
                "email" to owner.email,
                "name" to ownerName,
                "photoUrl" to (ownerProf?.photoUrl ?: owner.photoUrl ?: ""),
                "role" to "Owner",
                "permission" to "Manager",
                "status" to "Accepted",
                "joinedAt" to System.currentTimeMillis()
            )
            firestore?.collection("family_groups")?.document(groupId)?.collection("members")
                ?.document(memberId)?.set(memberData)?.await()

            val fg = FamilyGroup(id = groupId, groupName = groupName, ownerUid = owner.uid, ownerName = ownerName)
            Result.success(fg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inviteFamilyMember(
        groupId: String,
        groupName: String,
        targetEmail: String,
        role: String,
        permission: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val users = searchUsers(targetEmail)
            val target = users.find { it.email.equals(targetEmail, ignoreCase = true) }
            val targetUid = target?.uid ?: ""
            val targetName = target?.displayName ?: targetEmail.substringBefore("@")

            val memberId = "${groupId}_${if (targetUid.isNotBlank()) targetUid else targetEmail.hashCode()}"
            val memberData = mapOf(
                "id" to memberId,
                "groupId" to groupId,
                "uid" to targetUid,
                "email" to targetEmail,
                "name" to targetName,
                "photoUrl" to (target?.photoUrl ?: ""),
                "role" to role,
                "permission" to permission,
                "status" to "Pending",
                "joinedAt" to System.currentTimeMillis()
            )

            firestore?.collection("family_groups")?.document(groupId)?.collection("members")
                ?.document(memberId)?.set(memberData)?.await()

            if (targetUid.isNotBlank()) {
                sendNotification(
                    recipientUid = targetUid,
                    title = "Family Group Invite",
                    message = "You have been invited to join family group '$groupName' as $role ($permission).",
                    type = "FAMILY_INVITE"
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyFamilyGroups(): List<FamilyGroup> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val list = mutableListOf<FamilyGroup>()
        try {
            val snap = firestore?.collection("family_groups")?.get()?.await()
            if (snap != null) {
                for (doc in snap.documents) {
                    val ownerUid = doc.getString("ownerUid") ?: ""
                    val groupId = doc.id
                    var isMember = (ownerUid == uid)
                    if (!isMember) {
                        val mSnap = firestore?.collection("family_groups")?.document(groupId)
                            ?.collection("members")?.whereEqualTo("uid", uid)?.get()?.await()
                        if (mSnap != null && !mSnap.isEmpty) isMember = true
                    }
                    if (isMember) {
                        list.add(
                            FamilyGroup(
                                id = groupId,
                                groupName = doc.getString("groupName") ?: "Family Group",
                                ownerUid = ownerUid,
                                ownerName = doc.getString("ownerName") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            addAuditLog("[FAMILY ERROR] Get family groups failed: ${e.message}")
        }
        list
    }

    suspend fun getFamilyMembers(groupId: String): List<FamilyMember> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FamilyMember>()
        try {
            val snap = firestore?.collection("family_groups")?.document(groupId)
                ?.collection("members")?.get()?.await()
            if (snap != null) {
                for (doc in snap.documents) {
                    list.add(
                        FamilyMember(
                            id = doc.id,
                            groupId = groupId,
                            uid = doc.getString("uid") ?: "",
                            email = doc.getString("email") ?: "",
                            name = doc.getString("name") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            role = doc.getString("role") ?: "Member",
                            permission = doc.getString("permission") ?: "Viewer",
                            status = doc.getString("status") ?: "Accepted",
                            joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            addAuditLog("[FAMILY ERROR] Get family members failed: ${e.message}")
        }
        list
    }

    suspend fun removeFamilyMember(groupId: String, memberId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore?.collection("family_groups")?.document(groupId)
                ?.collection("members")?.document(memberId)?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        recipientUid: String,
        title: String,
        message: String,
        type: String
    ) = withContext(Dispatchers.IO) {
        if (recipientUid.isBlank()) return@withContext
        try {
            val notifId = firestore?.collection("users")?.document(recipientUid)
                ?.collection("notifications")?.document()?.id ?: System.currentTimeMillis().toString()
            val notifData = mapOf(
                "id" to notifId,
                "recipientUid" to recipientUid,
                "title" to title,
                "message" to message,
                "type" to type,
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            )
            firestore?.collection("users")?.document(recipientUid)
                ?.collection("notifications")?.document(notifId)?.set(notifData)?.await()
        } catch (e: Exception) {
            addAuditLog("[NOTIF ERROR] Send notification failed: ${e.message}")
        }
    }

    suspend fun getUserNotifications(): List<AppNotification> = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext emptyList()
        val list = mutableListOf<AppNotification>()
        try {
            val snap = firestore?.collection("users")?.document(uid)
                ?.collection("notifications")?.get()?.await()
            if (snap != null) {
                for (doc in snap.documents) {
                    list.add(
                        AppNotification(
                            id = doc.id,
                            recipientUid = uid,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "INFO",
                            isRead = doc.getBoolean("isRead") ?: false,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            addAuditLog("[NOTIF ERROR] Get notifications failed: ${e.message}")
        }
        list.sortedByDescending { it.createdAt }
    }

    suspend fun markNotificationAsRead(notifId: String) = withContext(Dispatchers.IO) {
        val uid = _currentUser.value?.uid ?: return@withContext
        try {
            firestore?.collection("users")?.document(uid)
                ?.collection("notifications")?.document(notifId)
                ?.update("isRead", true)?.await()
        } catch (e: Exception) {
            addAuditLog("[NOTIF ERROR] Mark read failed: ${e.message}")
        }
    }

    suspend fun fetchPublicUserProfile(targetUid: String): PublicUserProfile? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore?.collection("users")?.document(targetUid)?.get()?.await()
            if (doc != null && doc.exists()) {
                val email = doc.getString("email") ?: ""
                val name = doc.getString("fullName") ?: doc.getString("displayName") ?: doc.getString("name") ?: ""
                val country = doc.getString("country") ?: ""
                val currency = doc.getString("preferredCurrency") ?: "PKR"
                val photo = doc.getString("photoUrl") ?: ""
                val joinDate = doc.getLong("createdAt") ?: 0L

                val vSnap = firestore?.collection("users")?.document(targetUid)?.collection("vehicles")?.get()?.await()
                val vCount = vSnap?.size() ?: 0

                return@withContext PublicUserProfile(
                    uid = targetUid,
                    displayName = name.ifBlank { email.substringBefore("@") },
                    email = email,
                    country = country,
                    preferredCurrency = currency,
                    photoUrl = photo,
                    joinDate = joinDate,
                    vehicleCount = vCount
                )
            }
        } catch (e: Exception) {
            addAuditLog("[PROFILE ERROR] Fetch public profile failed: ${e.message}")
        }
        null
    }

    // --- Model Map Converters for Clean Firestore Serialization ---

    private fun Vehicle.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
        "vehicleName" to vehicleName,
        "vehicleType" to vehicleType,
        "brand" to brand,
        "model" to model,
        "manufacturingYear" to manufacturingYear,
        "registrationNumber" to registrationNumber,
        "fuelType" to fuelType,
        "odometerReading" to odometerReading,
        "notes" to notes,
        "vin" to vin,
        "purchaseDate" to purchaseDate,
        "imageUri" to imageUri,
        "lastUpdated" to lastUpdated,
        "createdAt" to createdAt
    )

    private fun FuelEntry.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
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
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
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
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
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
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "docTitle" to docTitle,
        "docType" to docType,
        "issueDate" to issueDate,
        "expiryDate" to expiryDate,
        "notes" to notes,
        "fileUri" to fileUri,
        "fileName" to fileName,
        "mimeType" to mimeType,
        "fileSize" to fileSize,
        "reminderDaysBefore" to reminderDaysBefore,
        "createdAt" to createdAt
    )

    private fun InsurancePolicy.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "providerName" to providerName,
        "policyNumber" to policyNumber,
        "coverageType" to coverageType,
        "premiumAmount" to premiumAmount,
        "startDate" to startDate,
        "expiryDate" to expiryDate,
        "agentContact" to agentContact,
        "claimContact" to claimContact,
        "emergencyContact" to emergencyContact,
        "notes" to notes,
        "isAutoRenewEnabled" to isAutoRenewEnabled,
        "documentUri" to documentUri,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun Reminder.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
        "vehicleId" to vehicleId,
        "vehicleName" to vehicleName,
        "reminderTitle" to reminderTitle,
        "reminderType" to reminderType,
        "dueDate" to dueDate,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt
    )

    private fun GeofenceZone.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId.ifBlank { _currentUser.value?.uid ?: "" },
        "vehicleId" to vehicleId,
        "zoneName" to zoneName,
        "centerLatitude" to centerLatitude,
        "centerLongitude" to centerLongitude,
        "radiusMeters" to radiusMeters,
        "notifyOnEnter" to notifyOnEnter,
        "notifyOnExit" to notifyOnExit,
        "isActive" to isActive
    )

    // --- Document Mapping Extensions ---

    private fun DocumentSnapshot.toVehicle(): Vehicle? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val name = getString("vehicleName") ?: getString("name") ?: getString("title") ?: getString("vehicle_name") ?: ""
        if (name.isBlank() && idVal == 0L) return null
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return Vehicle(
            id = idVal,
            ownerUserId = ownerId,
            vehicleName = if (name.isNotBlank()) name else "Vehicle",
            vehicleType = getString("vehicleType") ?: getString("type") ?: "Car",
            brand = getString("brand") ?: getString("make") ?: "",
            model = getString("model") ?: "",
            manufacturingYear = getString("manufacturingYear") ?: getString("year") ?: "",
            registrationNumber = getString("registrationNumber") ?: getString("plate") ?: getString("plateNumber") ?: getString("registration") ?: "",
            fuelType = getString("fuelType") ?: "Petrol",
            odometerReading = getString("odometerReading") ?: getString("odometer") ?: "0",
            notes = getString("notes") ?: "",
            vin = getString("vin") ?: "",
            purchaseDate = getString("purchaseDate") ?: "",
            imageUri = getString("imageUri") ?: "",
            lastUpdated = getLong("lastUpdated") ?: getLong("createdAt") ?: System.currentTimeMillis(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toFuelEntry(): FuelEntry? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return FuelEntry(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            fuelDate = getString("fuelDate") ?: getString("date") ?: "",
            fuelType = getString("fuelType") ?: "Petrol",
            fuelQuantity = getString("fuelQuantity") ?: get("fuelQuantity")?.toString() ?: "0",
            amountPaid = getString("amountPaid") ?: get("amountPaid")?.toString() ?: "0",
            currentOdometer = getString("currentOdometer") ?: getString("odometer") ?: "0",
            fuelStationName = getString("fuelStationName") ?: getString("station") ?: "",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toMaintenance(): Maintenance? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return Maintenance(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            serviceTitle = getString("serviceTitle") ?: getString("title") ?: "Service",
            serviceType = getString("serviceType") ?: "Routine Service",
            serviceDate = getString("serviceDate") ?: getString("date") ?: "",
            currentOdometer = getString("currentOdometer") ?: getString("odometer") ?: "0",
            serviceCost = getString("serviceCost") ?: get("serviceCost")?.toString() ?: getString("cost") ?: "0",
            workshopName = getString("workshopName") ?: getString("workshop") ?: "",
            notes = getString("notes") ?: "",
            invoicePhotoUri = getString("invoicePhotoUri") ?: "",
            nextDueServiceDate = getString("nextDueServiceDate") ?: "",
            reminderDate = getString("reminderDate") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toExpense(): Expense? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val amt = getDouble("amount") ?: get("amount")?.toString()?.toDoubleOrNull() ?: 0.0
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return Expense(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            title = getString("title") ?: "Expense",
            category = getString("category") ?: "Other",
            amount = amt,
            date = getString("date") ?: "",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toDocument(): Document? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val uriStr = getString("fileUri") ?: getString("docPath") ?: ""
        val fName = getString("fileName") ?: if (uriStr.isNotBlank()) uriStr.substringAfterLast("/") else ""
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return Document(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            docTitle = getString("docTitle") ?: getString("title") ?: "",
            docType = getString("docType") ?: getString("category") ?: "Registration",
            issueDate = getString("issueDate") ?: "",
            expiryDate = getString("expiryDate") ?: "",
            notes = getString("notes") ?: "",
            fileUri = uriStr,
            fileName = fName,
            mimeType = getString("mimeType") ?: "",
            fileSize = getLong("fileSize") ?: 0L,
            reminderDaysBefore = getLong("reminderDaysBefore")?.toInt() ?: 7,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toInsurancePolicy(): InsurancePolicy? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val prem = getDouble("premiumAmount") ?: get("premiumAmount")?.toString()?.toDoubleOrNull() ?: 0.0
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return InsurancePolicy(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            providerName = getString("providerName") ?: "",
            policyNumber = getString("policyNumber") ?: "",
            coverageType = getString("coverageType") ?: "Comprehensive",
            premiumAmount = prem,
            startDate = getString("startDate") ?: "",
            expiryDate = getString("expiryDate") ?: "",
            agentContact = getString("agentContact") ?: "",
            claimContact = getString("claimContact") ?: "",
            emergencyContact = getString("emergencyContact") ?: "",
            notes = getString("notes") ?: "",
            isAutoRenewEnabled = getBoolean("isAutoRenewEnabled") ?: false,
            documentUri = getString("documentUri") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toReminder(): Reminder? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return Reminder(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            vehicleName = getString("vehicleName") ?: "",
            reminderTitle = getString("reminderTitle") ?: getString("title") ?: "",
            reminderType = getString("reminderType") ?: getString("category") ?: "Oil Change",
            dueDate = getString("dueDate") ?: "",
            isCompleted = getBoolean("isCompleted") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toGeofenceZone(): GeofenceZone? {
        if (!exists()) return null
        var idVal = getLong("id") ?: get("id")?.toString()?.toLongOrNull() ?: id.toLongOrNull() ?: 0L
        if (idVal == 0L && id.isNotBlank()) {
            idVal = id.hashCode().toLong().let { if (it < 0) -it else it }
        }
        val vId = getLong("vehicleId") ?: get("vehicleId")?.toString()?.toLongOrNull() ?: 0L
        val lat = getDouble("centerLatitude") ?: get("centerLatitude")?.toString()?.toDoubleOrNull() ?: 0.0
        val lng = getDouble("centerLongitude") ?: get("centerLongitude")?.toString()?.toDoubleOrNull() ?: 0.0
        val rad = getDouble("radiusMeters") ?: get("radiusMeters")?.toString()?.toDoubleOrNull() ?: 500.0
        val ownerId = getString("ownerUserId") ?: _currentUser.value?.uid ?: ""
        return GeofenceZone(
            id = idVal,
            ownerUserId = ownerId,
            vehicleId = vId,
            zoneName = getString("zoneName") ?: "Geofence Zone",
            centerLatitude = lat,
            centerLongitude = lng,
            radiusMeters = rad,
            notifyOnEnter = getBoolean("notifyOnEnter") ?: true,
            notifyOnExit = getBoolean("notifyOnExit") ?: true,
            isActive = getBoolean("isActive") ?: true
        )
    }
}
