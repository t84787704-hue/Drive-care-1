package com.drivecare.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivecare.app.data.cloud.CloudUser
import com.drivecare.app.data.cloud.FirebaseSyncManager
import com.drivecare.app.data.cloud.SyncState
import com.drivecare.app.data.cloud.UserProfile
import com.drivecare.app.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.DriverProfile
import com.drivecare.app.data.model.EmergencyContact
import com.drivecare.app.data.model.Expense
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.GeofenceZone
import com.drivecare.app.data.model.InsurancePolicy
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Reminder
import com.drivecare.app.data.model.TripLog
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.data.model.VehicleShare
import com.drivecare.app.data.model.VehicleTelemetry
import com.drivecare.app.data.model.GpsTrackerDevice
import com.drivecare.app.data.model.TrackerLocationPoint
import com.drivecare.app.data.model.GeofenceEventLog
import com.drivecare.app.data.tracker.GpsTrackerRepository
import com.drivecare.app.data.tracker.TrackerPayload
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.DriveCareNotificationScheduler
import com.drivecare.app.utils.GeofenceManager
import com.drivecare.app.utils.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class AchievementItem(
    val titleKey: String,
    val desc: String,
    val isUnlocked: Boolean,
    val progress: Float
)

data class MaintenanceRecommendation(
    val titleKey: String,
    val reason: String,
    val urgency: String // HIGH, MEDIUM, LOW
)

data class TimelineEvent(
    val id: String,
    val title: String,
    val type: String, // Fuel, Service, Reminder, Document, Expense
    val date: String,
    val subtitle: String,
    val costOrAmount: String = ""
)

data class FuelEfficiencyStats(
    val kmPerLitre: Double,
    val litresPer100Km: Double,
    val totalDistanceTrackedKm: Double,
    val totalLitresConsumed: Double,
    val totalSpent: Double
)

data class VehicleDeletionSummary(
    val vehicleId: Long = 0L,
    val vehicleName: String = "",
    val vehicleImageUri: String = "",
    val fuelCount: Int = 0,
    val maintenanceCount: Int = 0,
    val documentsCount: Int = 0,
    val expensesCount: Int = 0,
    val insuranceCount: Int = 0,
    val remindersCount: Int = 0,
    val geofencesCount: Int = 0,
    val galleryCount: Int = 0,
    val estimatedStorageBytes: Long = 0L
) {
    fun formattedStorageSize(): String = com.drivecare.app.utils.DocumentFileHelper.formatFileSize(estimatedStorageBytes)
}

enum class DeletionStage {
    IDLE,
    ANALYZING,
    PREPARING,
    ROOM_CLEANUP,
    FIRESTORE_CLEANUP,
    STORAGE_CLEANUP,
    NOTIFICATION_CLEANUP,
    SUCCESS,
    FAILED
}

data class DeletionProgressState(
    val isDeleting: Boolean = false,
    val currentStage: DeletionStage = DeletionStage.IDLE,
    val statusMessage: String = "",
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null
)

class DriveCareViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getDatabase(application)

    // --- Firebase Auth & Cloud Sync Integration ---
    val syncManager = FirebaseSyncManager.getInstance().apply {
        init(application)
    }

    val currentUser: StateFlow<CloudUser?> = syncManager.currentUser
    val userProfile: StateFlow<UserProfile?> = syncManager.userProfile
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val lastSyncTime: StateFlow<Long> = syncManager.lastSyncTime
    val isFirebaseAvailable: StateFlow<Boolean> = syncManager.isFirebaseAvailable

    private val _deletionProgress = MutableStateFlow(DeletionProgressState())
    val deletionProgress: StateFlow<DeletionProgressState> = _deletionProgress.asStateFlow()

    private val vehicleDao = db.vehicleDao()
    private val fuelDao = db.fuelDao()
    private val maintenanceDao = db.maintenanceDao()
    private val reminderDao = db.reminderDao()
    private val documentDao = db.documentDao()
    private val emergencyContactDao = db.emergencyContactDao()
    private val expenseDao = db.expenseDao()
    private val driverProfileDao = db.driverProfileDao()
    private val vehicleShareDao = db.vehicleShareDao()
    private val tripLogDao = db.tripLogDao()
    private val geofenceZoneDao = db.geofenceZoneDao()
    private val vehicleTelemetryDao = db.vehicleTelemetryDao()
    private val insurancePolicyDao = db.insurancePolicyDao()
    private val gpsTrackerDao = db.gpsTrackerDao()
    private val trackerLocationDao = db.trackerLocationDao()
    private val geofenceEventDao = db.geofenceEventDao()

    val gpsTrackerRepository = GpsTrackerRepository(
        context = application,
        gpsTrackerDao = gpsTrackerDao,
        trackerLocationDao = trackerLocationDao,
        geofenceZoneDao = geofenceZoneDao,
        geofenceEventDao = geofenceEventDao,
        vehicleDao = vehicleDao
    )

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelEntries: StateFlow<List<FuelEntry>> = fuelDao.getAllFuelEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceLogs: StateFlow<List<Maintenance>> = maintenanceDao.getAllMaintenance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = documentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = emergencyContactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driverProfiles: StateFlow<List<DriverProfile>> = driverProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicleShares: StateFlow<List<VehicleShare>> = vehicleShareDao.getAllShares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tripLogs: StateFlow<List<TripLog>> = tripLogDao.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geofenceZones: StateFlow<List<GeofenceZone>> = geofenceZoneDao.getAllGeofences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTelemetry: StateFlow<List<VehicleTelemetry>> = vehicleTelemetryDao.getRecentTelemetry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insurancePolicies: StateFlow<List<InsurancePolicy>> = insurancePolicyDao.getAllInsurancePolicies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gpsTrackers: StateFlow<List<GpsTrackerDevice>> = gpsTrackerRepository.getAllTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackerLocations: StateFlow<List<TrackerLocationPoint>> = gpsTrackerRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geofenceEvents: StateFlow<List<GeofenceEventLog>> = gpsTrackerRepository.getAllGeofenceEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFuelVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedFuelVehicle: StateFlow<Vehicle?> = _selectedFuelVehicle.asStateFlow()

    private val _selectedDocumentVehicleId = MutableStateFlow<Long?>(null)
    val selectedDocumentVehicleId: StateFlow<Long?> = _selectedDocumentVehicleId.asStateFlow()

    fun selectDocumentVehicleFilter(vehicleId: Long?) {
        _selectedDocumentVehicleId.value = vehicleId
    }

    private val prefs = application.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE)

    private val _currentCurrencySymbol = MutableStateFlow(
        prefs.getString("selected_currency_symbol", "$") ?: "$"
    )
    val currentCurrencySymbol: StateFlow<String> = _currentCurrencySymbol.asStateFlow()

    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _notifyService = MutableStateFlow(prefs.getBoolean("notify_service", true))
    val notifyService: StateFlow<Boolean> = _notifyService.asStateFlow()

    private val _notifyInsurance = MutableStateFlow(prefs.getBoolean("notify_insurance", true))
    val notifyInsurance: StateFlow<Boolean> = _notifyInsurance.asStateFlow()

    private val _notifyDocuments = MutableStateFlow(prefs.getBoolean("notify_documents", true))
    val notifyDocuments: StateFlow<Boolean> = _notifyDocuments.asStateFlow()

    private val _notifyExpenses = MutableStateFlow(prefs.getBoolean("notify_expenses", true))
    val notifyExpenses: StateFlow<Boolean> = _notifyExpenses.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        try {
            val code = prefs.getString("selected_language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
            AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        // Synchronize Geofences with Google Play Services GeofencingClient
        viewModelScope.launch {
            try {
                geofenceZones.collect { zones ->
                    GeofenceManager.syncAllGeofences(getApplication(), zones)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Auto-Sync on User Login / Launch
        viewModelScope.launch {
            try {
                syncManager.currentUser.collect { user ->
                    if (user != null && user.uid.isNotBlank()) {
                        try {
                            syncManager.performFullBidirectionalSync(getApplication(), db)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed default emergency contacts if none exist
        viewModelScope.launch {
            try {
                val list = emergencyContactDao.getAllContacts().first()
                if (list.isEmpty()) {
                    emergencyContactDao.insertContact(EmergencyContact(name = "City Towing Service", category = "Towing", phoneNumber = "1-800-555-TOWS", notes = "24/7 Roadside Assistance"))
                    emergencyContactDao.insertContact(EmergencyContact(name = "AutoCare Workshop", category = "Mechanic", phoneNumber = "1-800-555-REPAIR", notes = "Official Garage Partner"))
                    emergencyContactDao.insertContact(EmergencyContact(name = "Insurance Claim Hotline", category = "Insurance", phoneNumber = "1-800-555-CLAIM", notes = "Policy #99824"))
                }
            } catch (e: Exception) {
                // Ignore seed error
            }
        }

        // Seed initial demo data (vehicles, fuel, maintenance, insurance, documents, geofences) if no vehicles exist
        viewModelScope.launch {
            try {
                val currentVehicles = vehicleDao.getAllVehicles().first()
                if (currentVehicles.isEmpty()) {
                    loadDemoData()
                }
            } catch (e: Exception) {
                // Ignore seed error
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("selected_language", language.code).apply()
        LocaleManager.applyLocale(getApplication(), language)
    }

    fun selectFuelVehicle(vehicle: Vehicle?) {
        _selectedFuelVehicle.value = vehicle
    }

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val vWithTimestamp = vehicle.copy(lastUpdated = System.currentTimeMillis())
            val newId = vehicleDao.insertVehicle(vWithTimestamp)
            val v = if (vWithTimestamp.id == 0L) vWithTimestamp.copy(id = newId) else vWithTimestamp
            syncManager.uploadSingleVehicle(v)
            triggerManualSync()
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val updated = vehicle.copy(lastUpdated = System.currentTimeMillis())
            vehicleDao.updateVehicle(updated)
            if (_selectedFuelVehicle.value?.id == updated.id) {
                _selectedFuelVehicle.value = updated
            }
            syncManager.uploadSingleVehicle(updated)
            triggerManualSync()
        }
    }

    suspend fun computeVehicleDeletionSummary(vehicle: Vehicle): VehicleDeletionSummary {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val vId = vehicle.id
            val fuelList = fuelDao.getAllFuelEntriesSync().filter { it.vehicleId == vId }
            val maintList = maintenanceDao.getAllMaintenanceSync().filter { it.vehicleId == vId }
            val docList = documentDao.getAllDocumentsSync().filter { it.vehicleId == vId }
            val expList = expenseDao.getAllExpensesSync().filter { it.vehicleId == vId }
            val insList = insurancePolicyDao.getAllInsurancePoliciesSync().filter { it.vehicleId == vId }
            val remList = reminderDao.getAllRemindersSync().filter { it.vehicleId == vId }
            val geoList = geofenceZoneDao.getGeofencesByVehicleSync(vId)

            val galleryCount = docList.count { it.docType == "Gallery" || it.docType == "Photo" }
            val documentsCount = docList.count { it.docType != "Gallery" && it.docType != "Photo" }

            var storageBytes = 0L
            docList.forEach { doc ->
                if (doc.fileSize > 0) {
                    storageBytes += doc.fileSize
                } else if (doc.fileUri.isNotBlank()) {
                    try {
                        val uri = android.net.Uri.parse(doc.fileUri)
                        if (uri.scheme == "file" && uri.path != null) {
                            val f = java.io.File(uri.path!!)
                            if (f.exists()) storageBytes += f.length()
                        }
                    } catch (_: Exception) {}
                }
            }

            if (vehicle.imageUri.isNotBlank()) {
                try {
                    val uri = android.net.Uri.parse(vehicle.imageUri)
                    if (uri.scheme == "file" && uri.path != null) {
                        val f = java.io.File(uri.path!!)
                        if (f.exists()) storageBytes += f.length()
                    }
                } catch (_: Exception) {}
            }

            insList.forEach { pol ->
                if (pol.documentUri.isNotBlank()) {
                    try {
                        val uri = android.net.Uri.parse(pol.documentUri)
                        if (uri.scheme == "file" && uri.path != null) {
                            val f = java.io.File(uri.path!!)
                            if (f.exists()) storageBytes += f.length()
                        }
                    } catch (_: Exception) {}
                }
            }

            maintList.forEach { m ->
                if (m.invoicePhotoUri.isNotBlank()) {
                    try {
                        val uri = android.net.Uri.parse(m.invoicePhotoUri)
                        if (uri.scheme == "file" && uri.path != null) {
                            val f = java.io.File(uri.path!!)
                            if (f.exists()) storageBytes += f.length()
                        }
                    } catch (_: Exception) {}
                }
            }

            VehicleDeletionSummary(
                vehicleId = vId,
                vehicleName = vehicle.vehicleName,
                vehicleImageUri = vehicle.imageUri,
                fuelCount = fuelList.size,
                maintenanceCount = maintList.size,
                documentsCount = documentsCount,
                expensesCount = expList.size,
                insuranceCount = insList.size,
                remindersCount = remList.size,
                geofencesCount = geoList.size,
                galleryCount = galleryCount,
                estimatedStorageBytes = storageBytes
            )
        }
    }

    fun deleteVehicleAdvanced(vehicle: Vehicle, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val logs = mutableListOf<String>()
            fun addLog(msg: String) {
                logs.add(msg)
                syncManager.addAuditLog(msg)
                _deletionProgress.value = _deletionProgress.value.copy(
                    logs = logs.toList(),
                    statusMessage = msg
                )
            }

            try {
                _deletionProgress.value = DeletionProgressState(
                    isDeleting = true,
                    currentStage = DeletionStage.PREPARING,
                    statusMessage = "[DELETE START] Initiating advanced vehicle cleanup for ${vehicle.vehicleName}",
                    logs = emptyList()
                )
                addLog("[DELETE START] Vehicle deletion requested for ID ${vehicle.id} (${vehicle.vehicleName})")

                val vehicleDocs = documentDao.getAllDocumentsSync().filter { it.vehicleId == vehicle.id }
                val vehicleInsurance = insurancePolicyDao.getAllInsurancePoliciesSync().filter { it.vehicleId == vehicle.id }
                val vehicleMaintenance = maintenanceDao.getAllMaintenanceSync().filter { it.vehicleId == vehicle.id }
                val vehicleReminders = reminderDao.getAllRemindersSync().filter { it.vehicleId == vehicle.id }
                val vehicleGeofences = geofenceZoneDao.getGeofencesByVehicleSync(vehicle.id)

                val remoteUrls = mutableListOf<String>()
                if (vehicle.imageUri.isNotBlank()) remoteUrls.add(vehicle.imageUri)
                vehicleDocs.forEach { if (it.fileUri.isNotBlank()) remoteUrls.add(it.fileUri) }
                vehicleInsurance.forEach { if (it.documentUri.isNotBlank()) remoteUrls.add(it.documentUri) }
                vehicleMaintenance.forEach { if (it.invoicePhotoUri.isNotBlank()) remoteUrls.add(it.invoicePhotoUri) }

                // 1. Room DB & Local Internal Storage Cleanup
                _deletionProgress.value = _deletionProgress.value.copy(currentStage = DeletionStage.ROOM_CLEANUP)
                addLog("Deleting local files from internal storage...")
                vehicleDocs.forEach { doc -> com.drivecare.app.utils.DocumentFileHelper.deleteFileFromInternalStorage(doc.fileUri) }
                vehicleInsurance.forEach { pol -> com.drivecare.app.utils.DocumentFileHelper.deleteFileFromInternalStorage(pol.documentUri) }
                vehicleMaintenance.forEach { m -> com.drivecare.app.utils.DocumentFileHelper.deleteFileFromInternalStorage(m.invoicePhotoUri) }
                if (vehicle.imageUri.isNotBlank()) {
                    com.drivecare.app.utils.DocumentFileHelper.deleteFileFromInternalStorage(vehicle.imageUri)
                }

                addLog("Purging Room Database tables for vehicle ${vehicle.id}...")
                fuelDao.deleteByVehicle(vehicle.id)
                maintenanceDao.deleteByVehicle(vehicle.id)
                documentDao.deleteByVehicle(vehicle.id)
                expenseDao.deleteByVehicle(vehicle.id)
                insurancePolicyDao.deleteByVehicle(vehicle.id)
                reminderDao.deleteByVehicle(vehicle.id)
                geofenceZoneDao.deleteByVehicle(vehicle.id)
                db.geofenceEventDao().deleteByVehicle(vehicle.id)
                db.trackerLocationDao().deleteHistoryForVehicle(vehicle.id)
                gpsTrackerDao.unassignTrackerFromVehicle(vehicle.id)
                vehicleShareDao.deleteByVehicle(vehicle.id)
                tripLogDao.deleteByVehicle(vehicle.id)
                vehicleDao.deleteVehicle(vehicle)
                addLog("[ROOM DELETE SUCCESS] Local database tables & internal files purged")

                // 2. Firestore Cloud Cleanup
                _deletionProgress.value = _deletionProgress.value.copy(currentStage = DeletionStage.FIRESTORE_CLEANUP)
                addLog("Deleting Firestore documents and linked subcollections...")
                syncManager.deleteSingleVehicle(vehicle.id)
                addLog("[FIRESTORE DELETE SUCCESS] Firestore cloud documents deleted")

                // 3. Firebase Storage Cleanup
                _deletionProgress.value = _deletionProgress.value.copy(currentStage = DeletionStage.STORAGE_CLEANUP)
                if (remoteUrls.any { it.startsWith("https://firebasestorage.googleapis.com") || it.startsWith("gs://") }) {
                    addLog("Deleting uploaded files from Firebase Storage...")
                    syncManager.deleteStorageFilesForUrls(remoteUrls)
                    addLog("[STORAGE DELETE SUCCESS] Firebase Storage objects deleted")
                } else {
                    addLog("[STORAGE DELETE SUCCESS] No remote Firebase Storage objects found")
                }

                // 4. Notification & Geofence Cleanup
                _deletionProgress.value = _deletionProgress.value.copy(currentStage = DeletionStage.NOTIFICATION_CLEANUP)
                addLog("Cancelling pending notifications & geofence alerts...")
                val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
                vehicleReminders.forEach { r -> notificationManager.cancel((10000 + r.id).toInt()) }
                vehicleDocs.forEach { d -> notificationManager.cancel((20000 + d.id).toInt()) }
                vehicleInsurance.forEach { p -> notificationManager.cancel((30000 + p.id).toInt()) }
                vehicleGeofences.forEach { g: com.drivecare.app.data.model.GeofenceZone -> GeofenceManager.unregisterGeofence(context, g.id) }
                addLog("[NOTIFICATIONS REMOVED] Active alerts and geofences unregistered")

                // 5. App State Refresh
                if (_selectedFuelVehicle.value?.id == vehicle.id) {
                    _selectedFuelVehicle.value = null
                }
                if (_selectedDocumentVehicleId.value == vehicle.id) {
                    _selectedDocumentVehicleId.value = null
                }
                triggerManualSync()

                addLog("[DELETE COMPLETE] Advanced vehicle deletion successfully finished")
                _deletionProgress.value = _deletionProgress.value.copy(
                    isDeleting = false,
                    currentStage = DeletionStage.SUCCESS,
                    statusMessage = "[DELETE COMPLETE] Vehicle ${vehicle.vehicleName} deleted successfully"
                )
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: "Unknown deletion failure"
                addLog("[DELETE FAILED] Error deleting vehicle: $errorMsg")
                _deletionProgress.value = _deletionProgress.value.copy(
                    isDeleting = false,
                    currentStage = DeletionStage.FAILED,
                    errorMessage = errorMsg
                )
                onComplete(false)
            }
        }
    }

    fun resetDeletionProgress() {
        _deletionProgress.value = DeletionProgressState()
    }

    fun deleteVehicle(vehicle: Vehicle) {
        deleteVehicleAdvanced(vehicle)
    }

    fun addFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            val id = fuelDao.insertFuelEntry(entry)
            val f = if (entry.id == 0L) entry.copy(id = id) else entry
            syncManager.uploadSingleFuelEntry(f)
            triggerManualSync()
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelDao.deleteFuelEntry(entry)
            syncManager.deleteSingleFuelEntry(entry.id)
            triggerManualSync()
        }
    }

    fun addMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            val id = maintenanceDao.insertMaintenance(maintenance)
            val m = if (maintenance.id == 0L) maintenance.copy(id = id) else maintenance
            syncManager.uploadSingleMaintenance(m)

            // Auto-create reminder if next due date or reminder date is provided
            val due = if (m.reminderDate.isNotBlank()) m.reminderDate else m.nextDueServiceDate
            if (due.isNotBlank()) {
                val rem = Reminder(
                    vehicleId = m.vehicleId,
                    vehicleName = m.vehicleName,
                    reminderTitle = "Next Service: ${m.serviceTitle}",
                    reminderType = "Service",
                    dueDate = due
                )
                val remId = reminderDao.insertReminder(rem)
                syncManager.uploadSingleReminder(rem.copy(id = remId))
            }
            triggerManualSync()
        }
    }

    fun updateMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.updateMaintenance(maintenance)
            syncManager.uploadSingleMaintenance(maintenance)
            triggerManualSync()
        }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.deleteMaintenance(maintenance)
            syncManager.deleteSingleMaintenance(maintenance.id)
            triggerManualSync()
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = reminderDao.insertReminder(reminder)
            val r = if (reminder.id == 0L) reminder.copy(id = id) else reminder
            syncManager.uploadSingleReminder(r)
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            reminderDao.updateReminder(updated)
            syncManager.uploadSingleReminder(updated)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            syncManager.deleteSingleReminder(reminder.id)
        }
    }

    fun addDocument(document: Document) {
        viewModelScope.launch {
            documentDao.insertDocument(document)
            syncManager.uploadSingleDocument(getApplication(), document)
            if (document.expiryDate.isNotBlank()) {
                val rem = Reminder(
                    vehicleId = document.vehicleId,
                    vehicleName = document.vehicleName,
                    reminderTitle = "Document Renewal: ${document.docTitle}",
                    reminderType = "Document",
                    dueDate = document.expiryDate
                )
                val remId = reminderDao.insertReminder(rem)
                syncManager.uploadSingleReminder(rem.copy(id = remId))
            }
        }
    }

    fun updateDocument(document: Document) {
        viewModelScope.launch {
            documentDao.updateDocument(document)
            syncManager.uploadSingleDocument(getApplication(), document)
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            com.drivecare.app.utils.DocumentFileHelper.deleteFileFromInternalStorage(document.fileUri)
            documentDao.deleteDocument(document)
            syncManager.deleteSingleDocument(document.id)
        }
    }

    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch { emergencyContactDao.insertContact(contact) }
    }

    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch { emergencyContactDao.deleteContact(contact) }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            val id = expenseDao.insertExpense(expense)
            val e = if (expense.id == 0L) expense.copy(id = id) else expense
            syncManager.uploadSingleExpense(e)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            syncManager.deleteSingleExpense(expense.id)
        }
    }

    // Driver Profiles
    fun addDriverProfile(profile: DriverProfile) {
        viewModelScope.launch { driverProfileDao.insertProfile(profile) }
    }

    fun updateDriverProfile(profile: DriverProfile) {
        viewModelScope.launch { driverProfileDao.updateProfile(profile) }
    }

    fun deleteDriverProfile(profile: DriverProfile) {
        viewModelScope.launch { driverProfileDao.deleteProfile(profile) }
    }

    // Vehicle Sharing & Family Access
    fun addVehicleShare(share: VehicleShare) {
        viewModelScope.launch { vehicleShareDao.insertShare(share) }
    }

    fun deleteVehicleShare(share: VehicleShare) {
        viewModelScope.launch { vehicleShareDao.deleteShare(share) }
    }

    fun transferVehicleOwnership(vehicleId: Long, newOwnerName: String) {
        viewModelScope.launch {
            val v = vehicles.value.find { it.id == vehicleId }
            if (v != null) {
                vehicleDao.updateVehicle(v.copy(registrationNumber = "${v.registrationNumber} (Transferred to $newOwnerName)"))
            }
        }
    }

    // Trips & GPS Tracking
    fun addTripLog(trip: TripLog) {
        viewModelScope.launch { tripLogDao.insertTrip(trip) }
    }

    fun deleteTripLog(trip: TripLog) {
        viewModelScope.launch { tripLogDao.deleteTrip(trip) }
    }

    // Geofences
    fun addGeofenceZone(geofence: GeofenceZone) {
        viewModelScope.launch {
            val generatedId = geofenceZoneDao.insertGeofence(geofence)
            val updatedZone = if (geofence.id == 0L) geofence.copy(id = generatedId) else geofence
            GeofenceManager.registerGeofence(getApplication(), updatedZone)
            syncManager.uploadSingleGeofence(updatedZone)
        }
    }

    fun updateGeofenceZone(geofence: GeofenceZone) {
        viewModelScope.launch {
            geofenceZoneDao.updateGeofence(geofence)
            GeofenceManager.registerGeofence(getApplication(), geofence)
            syncManager.uploadSingleGeofence(geofence)
        }
    }

    fun deleteGeofenceZone(geofence: GeofenceZone) {
        viewModelScope.launch {
            geofenceZoneDao.deleteGeofence(geofence)
            GeofenceManager.unregisterGeofence(getApplication(), geofence.id)
            syncManager.deleteSingleGeofence(geofence.id)
        }
    }

    // Telemetry
    fun addVehicleTelemetry(telemetry: VehicleTelemetry) {
        viewModelScope.launch { vehicleTelemetryDao.insertTelemetry(telemetry) }
    }

    fun getTimelineEvents(targetVehicleId: Long? = null): List<TimelineEvent> {
        val list = mutableListOf<TimelineEvent>()

        fuelEntries.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { f ->
                list.add(
                    TimelineEvent(
                        id = "fuel_${f.id}",
                        title = "Fuel Refill (${f.fuelQuantity} L)",
                        type = "Fuel",
                        date = f.fuelDate,
                        subtitle = "${f.vehicleName} • ${f.fuelStationName.ifEmpty { "Fuel Station" }}",
                        costOrAmount = "$${f.amountPaid}"
                    )
                )
            }

        maintenanceLogs.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { m ->
                list.add(
                    TimelineEvent(
                        id = "maint_${m.id}",
                        title = m.serviceTitle,
                        type = "Service",
                        date = m.serviceDate,
                        subtitle = "${m.vehicleName} • ${m.workshopName.ifEmpty { "Workshop" }}",
                        costOrAmount = "$${m.serviceCost}"
                    )
                )
            }

        reminders.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { r ->
                list.add(
                    TimelineEvent(
                        id = "rem_${r.id}",
                        title = r.reminderTitle,
                        type = "Reminder",
                        date = r.dueDate,
                        subtitle = "${r.vehicleName} • ${r.reminderType} (${if (r.isCompleted) "Completed" else "Due"})"
                    )
                )
            }

        documents.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { d ->
                list.add(
                    TimelineEvent(
                        id = "doc_${d.id}",
                        title = d.docTitle,
                        type = "Document",
                        date = d.issueDate.ifEmpty { d.expiryDate.ifEmpty { "Recently" } },
                        subtitle = "${d.vehicleName} • ${d.docType}"
                    )
                )
            }

        expenses.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { e ->
                list.add(
                    TimelineEvent(
                        id = "exp_${e.id}",
                        title = e.title,
                        type = "Expense",
                        date = e.date,
                        subtitle = "${e.vehicleName} • ${e.category}",
                        costOrAmount = "$${e.amount}"
                    )
                )
            }

        tripLogs.value
            .filter { targetVehicleId == null || targetVehicleId == -1L || it.vehicleId == targetVehicleId }
            .forEach { t ->
                list.add(
                    TimelineEvent(
                        id = "trip_${t.id}",
                        title = "Trip: ${t.startLocation} ➔ ${t.endLocation}",
                        type = "Trip",
                        date = t.tripDate,
                        subtitle = "${t.vehicleName} • Driver: ${t.driverName} • ${t.durationMinutes} mins",
                        costOrAmount = "${String.format(Locale.US, "%.2f", t.distanceKm)} km"
                    )
                )
            }

        return list.sortedByDescending { it.date }
    }

    // Dynamic Vehicle Health Score Algorithm (0 - 100) based on actual records
    fun calculateHealthScore(
        vehicle: Vehicle,
        remindersList: List<Reminder>,
        fuelList: List<FuelEntry>,
        serviceList: List<Maintenance>,
        documentList: List<Document> = emptyList()
    ): Int {
        var score = 100

        val vReminders = remindersList.filter { it.vehicleId == vehicle.id && !it.isCompleted }
        val vFuel = fuelList.filter { it.vehicleId == vehicle.id }
        val vService = serviceList.filter { it.vehicleId == vehicle.id }
        val vDocs = documentList.filter { it.vehicleId == vehicle.id }

        // Deduct 10 points for each pending reminder
        score -= (vReminders.size * 10)

        // Deduct 15 points if document expiry date is missing or contains past date
        vDocs.forEach { doc ->
            if (doc.expiryDate.isBlank()) {
                score -= 5
            }
        }

        // Deduct if no fuel entries added
        if (vFuel.isEmpty()) {
            score -= 10
        }

        // Deduct if no service history logged
        if (vService.isEmpty()) {
            score -= 15
        } else {
            // Reward for having logged services
            score += 5
        }

        // Check maintenance recommendations urgency
        val advisor = getMaintenanceAdvisorSuggestions(vehicle, serviceList)
        val highUrgencyCount = advisor.count { it.urgency == "HIGH" }
        score -= (highUrgencyCount * 10)

        return score.coerceIn(10, 100)
    }

    // Monthly Fuel Spend Data Aggregator
    fun getMonthlyFuelData(fuelList: List<FuelEntry>): Map<String, Double> {
        val result = LinkedHashMap<String, Double>()
        fuelList.sortedBy { it.fuelDate }.forEach { entry ->
            val monthKey = if (entry.fuelDate.length >= 7) entry.fuelDate.substring(0, 7) else "Recent"
            val cost = entry.amountPaid.toDoubleOrNull() ?: 0.0
            result[monthKey] = (result[monthKey] ?: 0.0) + cost
        }
        return result
    }

    // Category Breakdown Aggregator (Fuel, Service, Insurance, Parking, Tolls, Tax, Cleaning, Other)
    fun getExpenseCategoryBreakdown(
        fuelList: List<FuelEntry>,
        serviceList: List<Maintenance>,
        expenseList: List<Expense>
    ): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        
        val totalFuel = fuelList.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
        if (totalFuel > 0) map["Fuel"] = totalFuel

        val totalService = serviceList.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
        if (totalService > 0) map["Service"] = totalService

        expenseList.forEach { exp ->
            val cat = exp.category.ifBlank { "Other" }
            map[cat] = (map[cat] ?: 0.0) + exp.amount
        }

        return map.toList().sortedByDescending { it.second }.toMap()
    }

    // Calculate Fuel Efficiency for a Vehicle (km/L and L/100km)
    fun calculateVehicleFuelEfficiency(vehicle: Vehicle, fuelList: List<FuelEntry>): FuelEfficiencyStats {
        val vFuel = fuelList.filter { it.vehicleId == vehicle.id }
            .mapNotNull { entry ->
                val odo = entry.currentOdometer.toDoubleOrNull()
                val litres = entry.fuelQuantity.toDoubleOrNull()
                val cost = entry.amountPaid.toDoubleOrNull() ?: 0.0
                if (odo != null && litres != null && litres > 0) {
                    Triple(odo, litres, cost)
                } else null
            }
            .sortedBy { it.first }

        val totalLitres = vFuel.sumOf { it.second }
        val totalSpent = vFuel.sumOf { it.third }

        if (vFuel.size < 2) {
            // Not enough consecutive entries for exact delta calculation, return overall estimate
            val currentOdo = vehicle.odometerReading.toDoubleOrNull() ?: 0.0
            val kmPerL = if (totalLitres > 0 && currentOdo > 0) (currentOdo / totalLitres).coerceIn(2.0, 35.0) else 12.0
            val lPer100 = if (kmPerL > 0) 100.0 / kmPerL else 8.3
            return FuelEfficiencyStats(kmPerL, lPer100, currentOdo, totalLitres, totalSpent)
        }

        val distanceTracked = (vFuel.last().first - vFuel.first().first).coerceAtLeast(0.0)
        val litresUsedExceptFirst = vFuel.drop(1).sumOf { it.second }

        val kmPerLitre = if (litresUsedExceptFirst > 0 && distanceTracked > 0) {
            distanceTracked / litresUsedExceptFirst
        } else if (totalLitres > 0 && distanceTracked > 0) {
            distanceTracked / totalLitres
        } else {
            12.0
        }

        val litresPer100Km = if (kmPerLitre > 0) 100.0 / kmPerLitre else 8.3

        return FuelEfficiencyStats(
            kmPerLitre = kmPerLitre,
            litresPer100Km = litresPer100Km,
            totalDistanceTrackedKm = distanceTracked,
            totalLitresConsumed = totalLitres,
            totalSpent = totalSpent
        )
    }

    // Calculate Cost Per KM for a Vehicle
    fun calculateCostPerKm(
        vehicle: Vehicle,
        fuelList: List<FuelEntry>,
        serviceList: List<Maintenance>,
        expenseList: List<Expense>
    ): Double {
        val totalFuel = fuelList.filter { it.vehicleId == vehicle.id }.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
        val totalService = serviceList.filter { it.vehicleId == vehicle.id }.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
        val totalExpense = expenseList.filter { it.vehicleId == vehicle.id }.sumOf { it.amount }
        val grandTotal = totalFuel + totalService + totalExpense

        val odo = vehicle.odometerReading.toDoubleOrNull() ?: 0.0
        return if (odo > 0 && grandTotal > 0) grandTotal / odo else 0.0
    }

    // Smart Maintenance Advisor logic
    fun getMaintenanceAdvisorSuggestions(vehicle: Vehicle, serviceList: List<Maintenance>): List<MaintenanceRecommendation> {
        val odo = vehicle.odometerReading.toDoubleOrNull() ?: 0.0
        val vLogs = serviceList.filter { it.vehicleId == vehicle.id }
        val list = mutableListOf<MaintenanceRecommendation>()

        if (vLogs.none { it.serviceTitle.contains("Oil", ignoreCase = true) } || odo > 5000) {
            list.add(MaintenanceRecommendation("oil_change_due", "Recommended every 5,000 km or 6 months.", "HIGH"))
        }

        if (vLogs.none { it.serviceTitle.contains("Brake", ignoreCase = true) } || odo > 15000) {
            list.add(MaintenanceRecommendation("brake_inspection_due", "Essential safety check for brake pads & rotors.", "HIGH"))
        }

        if (vLogs.none { it.serviceTitle.contains("Filter", ignoreCase = true) }) {
            list.add(MaintenanceRecommendation("air_filter_due", "Improves fuel economy and engine performance.", "MEDIUM"))
        }

        if (vLogs.none { it.serviceTitle.contains("Battery", ignoreCase = true) }) {
            list.add(MaintenanceRecommendation("battery_check_due", "Prevents unexpected battery discharge on road.", "MEDIUM"))
        }

        if (vLogs.none { it.serviceTitle.contains("Tire", ignoreCase = true) }) {
            list.add(MaintenanceRecommendation("tire_rotation_due", "Ensures even tire wear and optimal traction.", "LOW"))
        }

        return list
    }

    // Comprehensive Backup JSON Generator
    suspend fun exportBackupJson(): String {
        val vList = vehicleDao.getAllVehicles().first()
        val fList = fuelDao.getAllFuelEntries().first()
        val mList = maintenanceDao.getAllMaintenance().first()
        val rList = reminderDao.getAllReminders().first()
        val dList = documentDao.getAllDocuments().first()
        val cList = emergencyContactDao.getAllContacts().first()
        val eList = expenseDao.getAllExpenses().first()
        val dpList = driverProfileDao.getAllProfiles().first()
        val vsList = vehicleShareDao.getAllShares().first()
        val tlList = tripLogDao.getAllTrips().first()
        val insList = insurancePolicyDao.getAllInsurancePolicies().first()

        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // Vehicles
        val vArray = JSONArray()
        vList.forEach { v ->
            vArray.put(JSONObject().apply {
                put("id", v.id)
                put("vehicleName", v.vehicleName)
                put("vehicleType", v.vehicleType)
                put("brand", v.brand)
                put("model", v.model)
                put("manufacturingYear", v.manufacturingYear)
                put("registrationNumber", v.registrationNumber)
                put("fuelType", v.fuelType)
                put("odometerReading", v.odometerReading)
                put("notes", v.notes)
                put("createdAt", v.createdAt)
            })
        }
        root.put("vehicles", vArray)

        // Fuel Entries
        val fArray = JSONArray()
        fList.forEach { f ->
            fArray.put(JSONObject().apply {
                put("id", f.id)
                put("vehicleId", f.vehicleId)
                put("vehicleName", f.vehicleName)
                put("fuelDate", f.fuelDate)
                put("fuelType", f.fuelType)
                put("fuelQuantity", f.fuelQuantity)
                put("amountPaid", f.amountPaid)
                put("currentOdometer", f.currentOdometer)
                put("fuelStationName", f.fuelStationName)
                put("notes", f.notes)
                put("createdAt", f.createdAt)
            })
        }
        root.put("fuel_entries", fArray)

        // Maintenance
        val mArray = JSONArray()
        mList.forEach { m ->
            mArray.put(JSONObject().apply {
                put("id", m.id)
                put("vehicleId", m.vehicleId)
                put("vehicleName", m.vehicleName)
                put("serviceTitle", m.serviceTitle)
                put("serviceType", m.serviceType)
                put("serviceDate", m.serviceDate)
                put("currentOdometer", m.currentOdometer)
                put("serviceCost", m.serviceCost)
                put("workshopName", m.workshopName)
                put("notes", m.notes)
                put("createdAt", m.createdAt)
            })
        }
        root.put("maintenance", mArray)

        // Reminders
        val rArray = JSONArray()
        rList.forEach { r ->
            rArray.put(JSONObject().apply {
                put("id", r.id)
                put("vehicleId", r.vehicleId)
                put("vehicleName", r.vehicleName)
                put("reminderTitle", r.reminderTitle)
                put("reminderType", r.reminderType)
                put("dueDate", r.dueDate)
                put("isCompleted", r.isCompleted)
                put("createdAt", r.createdAt)
            })
        }
        root.put("reminders", rArray)

        // Documents
        val dArray = JSONArray()
        dList.forEach { d ->
            dArray.put(JSONObject().apply {
                put("id", d.id)
                put("vehicleId", d.vehicleId)
                put("vehicleName", d.vehicleName)
                put("docTitle", d.docTitle)
                put("docType", d.docType)
                put("issueDate", d.issueDate)
                put("expiryDate", d.expiryDate)
                put("notes", d.notes)
                put("fileUri", d.fileUri)
                put("mimeType", d.mimeType)
                put("fileSize", d.fileSize)
                put("reminderDaysBefore", d.reminderDaysBefore)
                put("createdAt", d.createdAt)
            })
        }
        root.put("documents", dArray)

        // Emergency Contacts
        val cArray = JSONArray()
        cList.forEach { c ->
            cArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("category", c.category)
                put("phoneNumber", c.phoneNumber)
                put("notes", c.notes)
            })
        }
        root.put("emergency_contacts", cArray)

        // Expenses
        val eArray = JSONArray()
        eList.forEach { e ->
            eArray.put(JSONObject().apply {
                put("id", e.id)
                put("vehicleId", e.vehicleId)
                put("vehicleName", e.vehicleName)
                put("title", e.title)
                put("category", e.category)
                put("amount", e.amount)
                put("date", e.date)
                put("notes", e.notes)
                put("createdAt", e.createdAt)
            })
        }
        root.put("expenses", eArray)

        // Driver Profiles
        val dpArray = JSONArray()
        dpList.forEach { dp ->
            dpArray.put(JSONObject().apply {
                put("id", dp.id)
                put("name", dp.name)
                put("email", dp.email)
                put("phone", dp.phone)
                put("licenseNumber", dp.licenseNumber)
                put("rating", dp.rating)
                put("firebaseUserId", dp.firebaseUserId)
                put("profilePhotoUrl", dp.profilePhotoUrl)
                put("createdAt", dp.createdAt)
                put("lastLoginAt", dp.lastLoginAt)
            })
        }
        root.put("driver_profiles", dpArray)

        // Vehicle Shares
        val vsArray = JSONArray()
        vsList.forEach { vs ->
            vsArray.put(JSONObject().apply {
                put("id", vs.id)
                put("vehicleId", vs.vehicleId)
                put("vehicleName", vs.vehicleName)
                put("sharedWithEmail", vs.sharedWithEmail)
                put("role", vs.role)
                put("status", vs.status)
                put("sharedAt", vs.sharedAt)
            })
        }
        root.put("vehicle_shares", vsArray)

        // Trip Logs
        val tlArray = JSONArray()
        tlList.forEach { tl ->
            tlArray.put(JSONObject().apply {
                put("id", tl.id)
                put("vehicleId", tl.vehicleId)
                put("vehicleName", tl.vehicleName)
                put("driverName", tl.driverName)
                put("startLocation", tl.startLocation)
                put("endLocation", tl.endLocation)
                put("distanceKm", tl.distanceKm)
                put("durationMinutes", tl.durationMinutes)
                put("avgSpeedKmh", tl.avgSpeedKmh)
                put("maxSpeedKmh", tl.maxSpeedKmh)
                put("tripDate", tl.tripDate)
                put("startTime", tl.startTime)
                put("endTime", tl.endTime)
                put("fuelConsumedLiters", tl.fuelConsumedLiters)
                put("routePointsJson", tl.routePointsJson)
            })
        }
        root.put("trip_logs", tlArray)

        // Insurance Policies
        val insArray = JSONArray()
        insList.forEach { ins ->
            insArray.put(JSONObject().apply {
                put("id", ins.id)
                put("vehicleId", ins.vehicleId)
                put("vehicleName", ins.vehicleName)
                put("providerName", ins.providerName)
                put("policyNumber", ins.policyNumber)
                put("coverageType", ins.coverageType)
                put("premiumAmount", ins.premiumAmount)
                put("startDate", ins.startDate)
                put("expiryDate", ins.expiryDate)
                put("agentContact", ins.agentContact)
                put("notes", ins.notes)
                put("isAutoRenewEnabled", ins.isAutoRenewEnabled)
                put("createdAt", ins.createdAt)
            })
        }
        root.put("insurance_policies", insArray)

        return root.toString(2)
    }

    // Sample Backup Generator for quick test restores
    fun getSampleBackupJson(): String {
        val root = JSONObject()
        val vArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("vehicleType", "Sedan")
                put("brand", "Toyota")
                put("model", "Camry SE")
                put("manufacturingYear", "2023")
                put("registrationNumber", "ABC-9876")
                put("fuelType", "Hybrid")
                put("odometerReading", "24500")
                put("notes", "Sample Restored Vehicle")
            })
        }
        root.put("vehicles", vArr)

        val fArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 201L)
                put("vehicleId", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("fuelDate", "2026-07-20")
                put("fuelType", "Hybrid Petrol")
                put("fuelQuantity", "45.0")
                put("amountPaid", "68.50")
                put("currentOdometer", "24500")
                put("fuelStationName", "Shell Express")
                put("notes", "Full tank refill")
            })
        }
        root.put("fuel_entries", fArr)

        val mArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 301L)
                put("vehicleId", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("serviceTitle", "Synthetic Oil & Filter Replacement")
                put("serviceType", "Routine Service")
                put("serviceDate", "2026-06-15")
                put("currentOdometer", "23000")
                put("serviceCost", "120.00")
                put("workshopName", "Toyota Authorized Service")
                put("notes", "Oil filter, air filter replaced")
            })
        }
        root.put("maintenance", mArr)

        val rArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 401L)
                put("vehicleId", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("reminderTitle", "Tire Rotation & Alignment")
                put("reminderType", "Maintenance")
                put("dueDate", "2026-09-01")
                put("isCompleted", false)
            })
        }
        root.put("reminders", rArr)

        val expArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 501L)
                put("vehicleId", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("title", "City Parking Permit")
                put("category", "Parking")
                put("amount", 45.00)
                put("date", "2026-07-01")
                put("notes", "Monthly permit")
            })
        }
        root.put("expenses", expArr)

        val insArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", 601L)
                put("vehicleId", 101L)
                put("vehicleName", "Toyota Camry Hybrid")
                put("providerName", "Allstate Insurance")
                put("policyNumber", "POL-8839201")
                put("coverageType", "Comprehensive")
                put("premiumAmount", 850.00)
                put("startDate", "2026-01-01")
                put("expiryDate", "2026-12-31")
                put("notes", "Auto-renewal enabled")
                put("isAutoRenewEnabled", true)
            })
        }
        root.put("insurance_policies", insArr)

        return root.toString(2)
    }

    // Comprehensive Restore JSON Parser
    fun restoreBackupJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanJson = jsonString.trim()
            if (cleanJson.isBlank()) {
                onComplete(false, "Restore failed: Input JSON is empty.")
                return@launch
            }

            try {
                val root = JSONObject(cleanJson)
                val vehicleIdMap = mutableMapOf<Long, Long>()

                var countVehicles = 0
                var countFuel = 0
                var countMaintenance = 0
                var countReminders = 0
                var countDocuments = 0
                var countContacts = 0
                var countExpenses = 0
                var countProfiles = 0
                var countShares = 0
                var countTrips = 0
                var countInsurance = 0

                if (root.has("vehicles")) {
                    val arr = root.getJSONArray("vehicles")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val oldId = obj.optLong("id", 0L)
                        val v = Vehicle(
                            id = if (oldId > 0) oldId else 0L,
                            vehicleName = obj.optString("vehicleName", "Vehicle"),
                            vehicleType = obj.optString("vehicleType", "Car"),
                            brand = obj.optString("brand", ""),
                            model = obj.optString("model", ""),
                            manufacturingYear = obj.optString("manufacturingYear", obj.optString("year", "")),
                            registrationNumber = obj.optString("registrationNumber", obj.optString("plate", "")),
                            fuelType = obj.optString("fuelType", "Petrol"),
                            odometerReading = obj.optString("odometerReading", obj.optString("odometer", "0")),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val insertedId = vehicleDao.insertVehicle(v)
                        val targetId = if (insertedId > 0) insertedId else oldId
                        if (oldId > 0) {
                            vehicleIdMap[oldId] = targetId
                        }
                        countVehicles++
                    }
                }

                if (root.has("fuel_entries")) {
                    val arr = root.getJSONArray("fuel_entries")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val entryId = obj.optLong("id", 0L)
                        val f = FuelEntry(
                            id = if (entryId > 0) entryId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            fuelDate = obj.optString("fuelDate", "2026-07-22"),
                            fuelType = obj.optString("fuelType", "Petrol"),
                            fuelQuantity = obj.optString("fuelQuantity", "0"),
                            amountPaid = obj.optString("amountPaid", "0"),
                            currentOdometer = obj.optString("currentOdometer", "0"),
                            fuelStationName = obj.optString("fuelStationName", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        fuelDao.insertFuelEntry(f)
                        countFuel++
                    }
                }

                if (root.has("maintenance")) {
                    val arr = root.getJSONArray("maintenance")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val maintId = obj.optLong("id", 0L)
                        val m = Maintenance(
                            id = if (maintId > 0) maintId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            serviceTitle = obj.optString("serviceTitle", "Service"),
                            serviceType = obj.optString("serviceType", "Routine Service"),
                            serviceDate = obj.optString("serviceDate", "2026-07-22"),
                            currentOdometer = obj.optString("currentOdometer", "0"),
                            serviceCost = obj.optString("serviceCost", "0"),
                            workshopName = obj.optString("workshopName", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        maintenanceDao.insertMaintenance(m)
                        countMaintenance++
                    }
                }

                if (root.has("reminders")) {
                    val arr = root.getJSONArray("reminders")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val remId = obj.optLong("id", 0L)
                        val r = Reminder(
                            id = if (remId > 0) remId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            reminderTitle = obj.optString("reminderTitle", "Reminder"),
                            reminderType = obj.optString("reminderType", "Oil Change"),
                            dueDate = obj.optString("dueDate", "2026-12-31"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        reminderDao.insertReminder(r)
                        countReminders++
                    }
                }

                if (root.has("documents")) {
                    val arr = root.getJSONArray("documents")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val docId = obj.optLong("id", 0L)
                        val d = Document(
                            id = if (docId > 0) docId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            docTitle = obj.optString("docTitle", "Document"),
                            docType = obj.optString("docType", "Registration"),
                            issueDate = obj.optString("issueDate", ""),
                            expiryDate = obj.optString("expiryDate", ""),
                            notes = obj.optString("notes", ""),
                            fileUri = obj.optString("fileUri", ""),
                            mimeType = obj.optString("mimeType", ""),
                            fileSize = obj.optLong("fileSize", 0L),
                            reminderDaysBefore = obj.optInt("reminderDaysBefore", 7),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        documentDao.insertDocument(d)
                        countDocuments++
                    }
                }

                if (root.has("emergency_contacts")) {
                    val arr = root.getJSONArray("emergency_contacts")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val contactId = obj.optLong("id", 0L)
                        val c = EmergencyContact(
                            id = if (contactId > 0) contactId else 0L,
                            name = obj.optString("name", "Contact"),
                            category = obj.optString("category", "Mechanic"),
                            phoneNumber = obj.optString("phoneNumber", ""),
                            notes = obj.optString("notes", "")
                        )
                        emergencyContactDao.insertContact(c)
                        countContacts++
                    }
                }

                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val expId = obj.optLong("id", 0L)
                        val exp = Expense(
                            id = if (expId > 0) expId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            title = obj.optString("title", "Expense"),
                            category = obj.optString("category", "Other"),
                            amount = obj.optDouble("amount", 0.0),
                            date = obj.optString("date", "2026-07-22"),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        expenseDao.insertExpense(exp)
                        countExpenses++
                    }
                }

                if (root.has("driver_profiles")) {
                    val arr = root.getJSONArray("driver_profiles")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val profId = obj.optLong("id", 0L)
                        val dp = DriverProfile(
                            id = if (profId > 0) profId else 0L,
                            name = obj.optString("name", "Driver"),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", ""),
                            licenseNumber = obj.optString("licenseNumber", ""),
                            rating = obj.optDouble("rating", 5.0),
                            firebaseUserId = obj.optString("firebaseUserId", ""),
                            profilePhotoUrl = obj.optString("profilePhotoUrl", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            lastLoginAt = obj.optLong("lastLoginAt", System.currentTimeMillis())
                        )
                        driverProfileDao.insertProfile(dp)
                        countProfiles++
                    }
                }

                if (root.has("vehicle_shares")) {
                    val arr = root.getJSONArray("vehicle_shares")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val shareId = obj.optLong("id", 0L)
                        val vs = VehicleShare(
                            id = if (shareId > 0) shareId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            sharedWithEmail = obj.optString("sharedWithEmail", ""),
                            role = obj.optString("role", "DRIVER"),
                            status = obj.optString("status", "ACTIVE")
                        )
                        vehicleShareDao.insertShare(vs)
                        countShares++
                    }
                }

                if (root.has("trip_logs")) {
                    val arr = root.getJSONArray("trip_logs")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val tripId = obj.optLong("id", 0L)
                        val tl = TripLog(
                            id = if (tripId > 0) tripId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            driverName = obj.optString("driverName", "Primary Driver"),
                            startLocation = obj.optString("startLocation", "Start"),
                            endLocation = obj.optString("endLocation", "Destination"),
                            distanceKm = obj.optDouble("distanceKm", 0.0),
                            durationMinutes = obj.optInt("durationMinutes", 0),
                            avgSpeedKmh = obj.optDouble("avgSpeedKmh", 0.0),
                            maxSpeedKmh = obj.optDouble("maxSpeedKmh", 0.0),
                            tripDate = obj.optString("tripDate", "2026-07-23"),
                            startTime = obj.optString("startTime", ""),
                            endTime = obj.optString("endTime", ""),
                            fuelConsumedLiters = obj.optDouble("fuelConsumedLiters", 0.0),
                            routePointsJson = obj.optString("routePointsJson", "[]")
                        )
                        tripLogDao.insertTrip(tl)
                        countTrips++
                    }
                }

                if (root.has("insurance_policies")) {
                    val arr = root.getJSONArray("insurance_policies")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rawVehicleId = obj.optLong("vehicleId", 1L)
                        val targetVehicleId = vehicleIdMap[rawVehicleId] ?: rawVehicleId
                        val policyId = obj.optLong("id", 0L)
                        val ins = InsurancePolicy(
                            id = if (policyId > 0) policyId else 0L,
                            vehicleId = targetVehicleId,
                            vehicleName = obj.optString("vehicleName", ""),
                            providerName = obj.optString("providerName", "Insurance"),
                            policyNumber = obj.optString("policyNumber", ""),
                            coverageType = obj.optString("coverageType", "Comprehensive"),
                            premiumAmount = obj.optDouble("premiumAmount", 0.0),
                            startDate = obj.optString("startDate", ""),
                            expiryDate = obj.optString("expiryDate", ""),
                            agentContact = obj.optString("agentContact", ""),
                            notes = obj.optString("notes", ""),
                            isAutoRenewEnabled = obj.optBoolean("isAutoRenewEnabled", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        insurancePolicyDao.insertPolicy(ins)
                        countInsurance++
                    }
                }

                val totalRestored = countVehicles + countFuel + countMaintenance + countReminders +
                        countDocuments + countContacts + countExpenses + countProfiles +
                        countShares + countTrips + countInsurance

                if (totalRestored == 0) {
                    onComplete(false, "No valid DriveCare data records found in JSON.")
                    return@launch
                }

                val restoredVehicles = vehicleDao.getAllVehicles().first()
                if (restoredVehicles.isNotEmpty()) {
                    _selectedFuelVehicle.value = restoredVehicles.first()
                }

                DriveCareNotificationScheduler.triggerImmediateCheck(getApplication())

                val summaryMsg = buildString {
                    append("Restore Successful! ")
                    append("$countVehicles vehicle(s), ")
                    append("$countFuel fuel log(s), ")
                    append("$countMaintenance service record(s), ")
                    append("$countExpenses expense(s), ")
                    append("$countReminders reminder(s), ")
                    append("$countInsurance insurance policy(ies) restored.")
                }

                onComplete(true, summaryMsg)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Failed to restore data: ${e.localizedMessage ?: "Invalid or corrupt JSON format"}")
            }
        }
    }

    // Insurance Methods
    fun addInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            val id = insurancePolicyDao.insertPolicy(policy)
            val p = if (policy.id == 0L) policy.copy(id = id) else policy
            syncManager.uploadSingleInsurance(getApplication(), p)
            DriveCareNotificationScheduler.triggerImmediateCheck(getApplication())
        }
    }

    // GPS Hardware Tracker Methods
    fun addGpsTracker(tracker: GpsTrackerDevice) {
        viewModelScope.launch {
            gpsTrackerRepository.insertTracker(tracker)
        }
    }

    fun updateGpsTracker(tracker: GpsTrackerDevice) {
        viewModelScope.launch {
            gpsTrackerRepository.updateTracker(tracker)
        }
    }

    fun saveGpsTracker(tracker: GpsTrackerDevice) {
        viewModelScope.launch {
            if (tracker.id == 0L) {
                gpsTrackerRepository.insertTracker(tracker)
            } else {
                gpsTrackerRepository.updateTracker(tracker)
            }
        }
    }

    fun deleteGpsTracker(tracker: GpsTrackerDevice) {
        viewModelScope.launch {
            gpsTrackerRepository.deleteTracker(tracker)
        }
    }

    fun deleteLocationHistoryForTracker(trackerId: String) {
        viewModelScope.launch {
            gpsTrackerRepository.deleteHistoryForTracker(trackerId)
        }
    }

    fun ingestTrackerPayload(payload: TrackerPayload) {
        viewModelScope.launch {
            gpsTrackerRepository.ingestPayload(payload)
        }
    }

    fun updateInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            insurancePolicyDao.updatePolicy(policy)
            syncManager.uploadSingleInsurance(getApplication(), policy)
        }
    }

    fun deleteInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            insurancePolicyDao.deletePolicy(policy)
            syncManager.deleteSingleInsurance(policy.id)
        }
    }

    fun renewInsurancePolicy(
        policy: InsurancePolicy,
        newStartDate: String,
        newExpiryDate: String,
        newPremium: Double
    ) {
        viewModelScope.launch {
            val updated = policy.copy(
                startDate = newStartDate,
                expiryDate = newExpiryDate,
                premiumAmount = newPremium
            )
            insurancePolicyDao.updatePolicy(updated)

            if (newPremium > 0) {
                val expense = Expense(
                    vehicleId = policy.vehicleId,
                    vehicleName = policy.vehicleName,
                    title = "Insurance Renewal (${policy.providerName})",
                    category = "Insurance",
                    amount = newPremium,
                    date = newStartDate,
                    notes = "Policy #${policy.policyNumber} renewed until $newExpiryDate"
                )
                expenseDao.insertExpense(expense)
            }
        }
    }

    // Settings & Preferences Methods
    fun setCurrencySymbol(symbol: String) {
        _currentCurrencySymbol.value = symbol
        prefs.edit().putString("selected_currency_symbol", symbol).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setNotificationPreference(key: String, enabled: Boolean) {
        when (key) {
            "service" -> {
                _notifyService.value = enabled
                prefs.edit().putBoolean("notify_service", enabled).apply()
            }
            "insurance" -> {
                _notifyInsurance.value = enabled
                prefs.edit().putBoolean("notify_insurance", enabled).apply()
            }
            "documents" -> {
                _notifyDocuments.value = enabled
                prefs.edit().putBoolean("notify_documents", enabled).apply()
            }
            "expenses" -> {
                _notifyExpenses.value = enabled
                prefs.edit().putBoolean("notify_expenses", enabled).apply()
            }
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            db.clearAllTables()
            onComplete()
        }
    }

    // --- Firebase Auth & Cloud Sync Integration ---

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.signInWithEmail(email, pass)
            result.fold(
                onSuccess = {
                    triggerManualSync()
                    onResult(true, null)
                },
                onFailure = {
                    onResult(false, it.localizedMessage)
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, fullName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.signUpWithEmail(email, pass, fullName)
            result.fold(
                onSuccess = {
                    triggerManualSync()
                    onResult(true, null)
                },
                onFailure = {
                    onResult(false, it.localizedMessage)
                }
            )
        }
    }

    fun signInWithGoogleAccount(googleEmail: String, googleName: String, photoUrl: String? = null, onResult: (Boolean, String?) -> Unit) {
        signInWithGoogleAccount(null, googleEmail, googleName, photoUrl, onResult)
    }

    fun signInWithGoogleAccount(idToken: String?, googleEmail: String, googleName: String, photoUrl: String? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.signInWithGoogleCredential(idToken, googleEmail, googleName, photoUrl)
            if (res.isSuccess) {
                triggerManualSync()
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun signInWithDemoGoogleAccount(onResult: (Boolean, String?) -> Unit) {
        signInWithGoogleAccount(
            googleEmail = "user.drive@gmail.com",
            googleName = "Google User",
            photoUrl = null,
            onResult = onResult
        )
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.sendPasswordReset(email)
            res.onSuccess { onResult(true, null) }.onFailure { onResult(false, it.localizedMessage) }
        }
    }

    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val email = currentUser.value?.email ?: userProfile.value?.email ?: ""
            val res = syncManager.sendEmailVerification()
            if (email.isNotBlank()) {
                syncManager.sendPasswordReset(email)
            }
            if (res.isSuccess) {
                onResult(true, "Verification email sent to $email!")
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun signOut() {
        syncManager.signOut()
    }

    fun saveUserProfile(profile: UserProfile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.saveUserProfile(profile)
            onResult(res.isSuccess)
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            syncManager.performFullBidirectionalSync(getApplication(), db)
        }
    }

    // --- Developer Tools: Demo Data Management ---
    val syncDemoDataEnabled = MutableStateFlow(false)

    fun setSyncDemoData(enabled: Boolean) {
        syncDemoDataEnabled.value = enabled
        syncManager.syncDemoData = enabled
    }

    fun loadDemoData(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                // 1. Create 3 Demo Vehicles
                val v1 = Vehicle(
                    vehicleName = "Toyota Corolla GLi 2021",
                    brand = "Toyota",
                    model = "Corolla GLi",
                    manufacturingYear = "2021",
                    vehicleType = "Sedan",
                    registrationNumber = "DEMO-LEB-101",
                    vin = "DEMO-TOYOTA-2021-001",
                    fuelType = "Petrol",
                    odometerReading = "45200",
                    purchaseDate = "2021-03-15",
                    notes = "[DEMO_DATA] Primary daily commute vehicle",
                    isDemo = true
                )
                val v1Id = vehicleDao.insertVehicle(v1)

                val v2 = Vehicle(
                    vehicleName = "Honda Civic Oriel 2023",
                    brand = "Honda",
                    model = "Civic Oriel",
                    manufacturingYear = "2023",
                    vehicleType = "Sedan",
                    registrationNumber = "DEMO-ICT-202",
                    vin = "DEMO-HONDA-2023-002",
                    fuelType = "Petrol",
                    odometerReading = "18500",
                    purchaseDate = "2023-06-20",
                    notes = "[DEMO_DATA] Family tour and highway cruiser",
                    isDemo = true
                )
                val v2Id = vehicleDao.insertVehicle(v2)

                val v3 = Vehicle(
                    vehicleName = "Tesla Model Y Long Range 2024",
                    brand = "Tesla",
                    model = "Model Y",
                    manufacturingYear = "2024",
                    vehicleType = "Electric / SUV",
                    registrationNumber = "DEMO-EV-303",
                    vin = "DEMO-TESLA-2024-003",
                    fuelType = "Electric",
                    odometerReading = "8200",
                    purchaseDate = "2024-01-10",
                    notes = "[DEMO_DATA] Long-range electric vehicle",
                    isDemo = true
                )
                val v3Id = vehicleDao.insertVehicle(v3)

                // 2. Create Demo Fuel Records
                fuelDao.insertFuelEntry(
                    FuelEntry(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        fuelDate = "2026-07-20",
                        fuelType = "Petrol",
                        fuelQuantity = "45.0",
                        amountPaid = "12150",
                        currentOdometer = "44800",
                        fuelStationName = "Total Parco Station",
                        notes = "[DEMO_DATA] Full tank refuel",
                        isDemo = true
                    )
                )
                fuelDao.insertFuelEntry(
                    FuelEntry(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        fuelDate = "2026-07-26",
                        fuelType = "Petrol",
                        fuelQuantity = "42.5",
                        amountPaid = "11475",
                        currentOdometer = "45200",
                        fuelStationName = "Shell Express",
                        notes = "[DEMO_DATA] Weekly refuel",
                        isDemo = true
                    )
                )
                fuelDao.insertFuelEntry(
                    FuelEntry(
                        vehicleId = v2Id,
                        vehicleName = v2.vehicleName,
                        fuelDate = "2026-07-18",
                        fuelType = "Hi-Octane",
                        fuelQuantity = "50.0",
                        amountPaid = "14500",
                        currentOdometer = "18100",
                        fuelStationName = "PSO Hi-Octane Hub",
                        notes = "[DEMO_DATA] Highway trip fuel",
                        isDemo = true
                    )
                )
                fuelDao.insertFuelEntry(
                    FuelEntry(
                        vehicleId = v3Id,
                        vehicleName = v3.vehicleName,
                        fuelDate = "2026-07-25",
                        fuelType = "Electric",
                        fuelQuantity = "65.0",
                        amountPaid = "2600",
                        currentOdometer = "8200",
                        fuelStationName = "Tesla Supercharger Station",
                        notes = "[DEMO_DATA] Fast DC charging session 20-80%",
                        isDemo = true
                    )
                )

                // 3. Create Demo Maintenance Records
                maintenanceDao.insertMaintenance(
                    Maintenance(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        serviceTitle = "Periodic 45,000 km Oil & Filter Change",
                        serviceType = "Routine Service",
                        serviceDate = "2026-07-15",
                        currentOdometer = "45000",
                        serviceCost = "8500",
                        workshopName = "Toyota Central Motors",
                        notes = "[DEMO_DATA] Engine oil 5W-30 changed with genuine oil filter",
                        nextDueServiceDate = "2026-11-15",
                        reminderDate = "2026-11-01",
                        isDemo = true
                    )
                )
                maintenanceDao.insertMaintenance(
                    Maintenance(
                        vehicleId = v2Id,
                        vehicleName = v2.vehicleName,
                        serviceTitle = "Brake Pads & Fluid Inspection",
                        serviceType = "Brake Service",
                        serviceDate = "2026-07-10",
                        currentOdometer = "18000",
                        serviceCost = "12000",
                        workshopName = "Honda Classic Service Center",
                        notes = "[DEMO_DATA] Front brake pads replaced and brake fluid flushed",
                        nextDueServiceDate = "2027-01-10",
                        reminderDate = "2026-12-25",
                        isDemo = true
                    )
                )
                maintenanceDao.insertMaintenance(
                    Maintenance(
                        vehicleId = v3Id,
                        vehicleName = v3.vehicleName,
                        serviceTitle = "Tire Rotation & Cabin HEPA Filter",
                        serviceType = "Routine Inspection",
                        serviceDate = "2026-06-30",
                        currentOdometer = "7500",
                        serviceCost = "6500",
                        workshopName = "Tesla Authorized Service Hub",
                        notes = "[DEMO_DATA] Multi-point EV safety inspection and cabin filter replacement",
                        nextDueServiceDate = "2026-12-30",
                        reminderDate = "2026-12-15",
                        isDemo = true
                    )
                )

                // 4. Create Demo Insurance Records
                insurancePolicyDao.insertPolicy(
                    InsurancePolicy(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        providerName = "EFU General Insurance",
                        policyNumber = "EFU-DEMO-2026-881",
                        coverageType = "Comprehensive",
                        premiumAmount = 45000.0,
                        startDate = "2026-01-01",
                        expiryDate = "2026-12-31",
                        agentContact = "+92 300 1234567",
                        claimContact = "111-338-111",
                        emergencyContact = "0800-EFU-HELP",
                        notes = "[DEMO_DATA] Comprehensive coverage with tracker & zero-depreciation rider",
                        isAutoRenewEnabled = true,
                        isDemo = true
                    )
                )
                insurancePolicyDao.insertPolicy(
                    InsurancePolicy(
                        vehicleId = v2Id,
                        vehicleName = v2.vehicleName,
                        providerName = "Jubilee General Insurance",
                        policyNumber = "JUB-DEMO-2026-992",
                        coverageType = "Comprehensive",
                        premiumAmount = 58000.0,
                        startDate = "2026-03-01",
                        expiryDate = "2027-02-28",
                        agentContact = "+92 321 7654321",
                        claimContact = "111-654-111",
                        emergencyContact = "0800-JUB-HELP",
                        notes = "[DEMO_DATA] Full road-side assistance & glass protection inclusion",
                        isAutoRenewEnabled = true,
                        isDemo = true
                    )
                )
                insurancePolicyDao.insertPolicy(
                    InsurancePolicy(
                        vehicleId = v3Id,
                        vehicleName = v3.vehicleName,
                        providerName = "State Life Motor Cover",
                        policyNumber = "SLI-DEMO-2026-103",
                        coverageType = "Comprehensive",
                        premiumAmount = 72000.0,
                        startDate = "2026-02-15",
                        expiryDate = "2027-02-14",
                        agentContact = "+92 333 9876543",
                        claimContact = "111-777-222",
                        emergencyContact = "0800-SLI-AUTO",
                        notes = "[DEMO_DATA] EV Battery replacement cover included",
                        isAutoRenewEnabled = true,
                        isDemo = true
                    )
                )

                // 5. Create Demo Documents
                documentDao.insertDocument(
                    Document(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        docTitle = "Smart Card Registration",
                        docType = "Registration",
                        issueDate = "2021-03-20",
                        expiryDate = "2031-03-20",
                        notes = "[DEMO_DATA] Vehicle Registration Smart Card copy",
                        reminderDaysBefore = 30,
                        isDemo = true
                    )
                )
                documentDao.insertDocument(
                    Document(
                        vehicleId = v2Id,
                        vehicleName = v2.vehicleName,
                        docTitle = "Annual Fitness & Emission Certificate",
                        docType = "Permit",
                        issueDate = "2026-01-10",
                        expiryDate = "2027-01-10",
                        notes = "[DEMO_DATA] Motor vehicle fitness approval certificate",
                        reminderDaysBefore = 15,
                        isDemo = true
                    )
                )
                documentDao.insertDocument(
                    Document(
                        vehicleId = v3Id,
                        vehicleName = v3.vehicleName,
                        docTitle = "EV High-Voltage Battery Warranty",
                        docType = "Warranty",
                        issueDate = "2024-01-10",
                        expiryDate = "2032-01-10",
                        notes = "[DEMO_DATA] 8-Year Tesla Factory Battery Warranty",
                        reminderDaysBefore = 60,
                        isDemo = true
                    )
                )

                // 6. Create Demo Geofences
                geofenceZoneDao.insertGeofence(
                    GeofenceZone(
                        vehicleId = v1Id,
                        zoneName = "Home Safe Zone (Model Town)",
                        centerLatitude = 31.4826,
                        centerLongitude = 74.3228,
                        radiusMeters = 500.0,
                        notifyOnEnter = true,
                        notifyOnExit = true,
                        isActive = true,
                        isDemo = true
                    )
                )
                geofenceZoneDao.insertGeofence(
                    GeofenceZone(
                        vehicleId = v2Id,
                        zoneName = "Office Complex (Gulberg)",
                        centerLatitude = 31.5204,
                        centerLongitude = 74.3587,
                        radiusMeters = 300.0,
                        notifyOnEnter = true,
                        notifyOnExit = true,
                        isActive = true,
                        isDemo = true
                    )
                )
                geofenceZoneDao.insertGeofence(
                    GeofenceZone(
                        vehicleId = v3Id,
                        zoneName = "Supercharger Station Area",
                        centerLatitude = 31.4697,
                        centerLongitude = 74.2728,
                        radiusMeters = 400.0,
                        notifyOnEnter = true,
                        notifyOnExit = true,
                        isActive = true,
                        isDemo = true
                    )
                )

                // 7. Demo Expenses & Reminders
                expenseDao.insertExpense(
                    Expense(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        title = "Motorway Toll Tax & M-Tag Topup",
                        category = "Toll",
                        amount = 2500.0,
                        date = "2026-07-22",
                        notes = "[DEMO_DATA] Toll tax recharge for M-Tag",
                        isDemo = true
                    )
                )
                expenseDao.insertExpense(
                    Expense(
                        vehicleId = v2Id,
                        vehicleName = v2.vehicleName,
                        title = "Annual Vehicle Token Tax 2026",
                        category = "Tax",
                        amount = 14500.0,
                        date = "2026-07-05",
                        notes = "[DEMO_DATA] Excise token tax payment",
                        isDemo = true
                    )
                )

                reminderDao.insertReminder(
                    Reminder(
                        vehicleId = v1Id,
                        vehicleName = v1.vehicleName,
                        reminderTitle = "Wheel Alignment & Balancing",
                        reminderType = "Tire Service",
                        dueDate = "2026-08-15",
                        isCompleted = false,
                        isDemo = true
                    )
                )

                onComplete(true, "Loaded 3 demo vehicles, fuel records, maintenance, insurance, documents, and geofences successfully!")
            } catch (e: Exception) {
                onComplete(false, "Failed to load demo data: ${e.message}")
            }
        }
    }

    fun removeDemoData(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val vCount = vehicleDao.deleteDemoVehicles()
                val fCount = fuelDao.deleteDemoFuelEntries()
                val mCount = maintenanceDao.deleteDemoMaintenance()
                val iCount = insurancePolicyDao.deleteDemoInsurancePolicies()
                val dCount = documentDao.deleteDemoDocuments()
                val gCount = geofenceZoneDao.deleteDemoGeofences()
                val eCount = expenseDao.deleteDemoExpenses()
                val rCount = reminderDao.deleteDemoReminders()

                onComplete(true, "Demo data safely removed ($vCount vehicles, $fCount fuel, $mCount maintenance, $iCount insurance, $dCount docs, $gCount geofences). Real user records were not affected.")
            } catch (e: Exception) {
                onComplete(false, "Failed to remove demo data: ${e.message}")
            }
        }
    }
}
