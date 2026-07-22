package com.drivecare.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.EmergencyContact
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Reminder
import com.drivecare.app.data.model.Vehicle
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

class DriveCareViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val vehicleDao = db.vehicleDao()
    private val fuelDao = db.fuelDao()
    private val maintenanceDao = db.maintenanceDao()
    private val reminderDao = db.reminderDao()
    private val documentDao = db.documentDao()
    private val emergencyContactDao = db.emergencyContactDao()

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

    private val _selectedFuelVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedFuelVehicle: StateFlow<Vehicle?> = _selectedFuelVehicle.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        // Seed default emergency contacts if none exist
        viewModelScope.launch {
            emergencyContactDao.getAllContacts().collect { list ->
                if (list.isEmpty()) {
                    emergencyContactDao.insertContact(EmergencyContact(name = "City Towing Service", category = "Towing", phoneNumber = "1-800-555-TOWS", notes = "24/7 Roadside Assistance"))
                    emergencyContactDao.insertContact(EmergencyContact(name = "AutoCare Workshop", category = "Mechanic", phoneNumber = "1-800-555-REPAIR", notes = "Official Garage Partner"))
                    emergencyContactDao.insertContact(EmergencyContact(name = "Insurance Claim Hotline", category = "Insurance", phoneNumber = "1-800-555-CLAIM", notes = "Policy #99824"))
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        LocaleManager.applyLocale(getApplication(), language)
    }

    fun selectFuelVehicle(vehicle: Vehicle?) {
        _selectedFuelVehicle.value = vehicle
    }

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch { vehicleDao.insertVehicle(vehicle) }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.updateVehicle(vehicle)
            if (_selectedFuelVehicle.value?.id == vehicle.id) {
                _selectedFuelVehicle.value = vehicle
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.deleteVehicle(vehicle)
            fuelDao.deleteByVehicle(vehicle.id)
            maintenanceDao.deleteByVehicle(vehicle.id)
            reminderDao.deleteByVehicle(vehicle.id)
            documentDao.deleteByVehicle(vehicle.id)
            if (_selectedFuelVehicle.value?.id == vehicle.id) {
                _selectedFuelVehicle.value = null
            }
        }
    }

    fun addFuelEntry(entry: FuelEntry) {
        viewModelScope.launch { fuelDao.insertFuelEntry(entry) }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch { fuelDao.deleteFuelEntry(entry) }
    }

    fun addMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { maintenanceDao.insertMaintenance(maintenance) }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch { maintenanceDao.deleteMaintenance(maintenance) }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch { reminderDao.insertReminder(reminder) }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { reminderDao.deleteReminder(reminder) }
    }

    fun addDocument(document: Document) {
        viewModelScope.launch { documentDao.insertDocument(document) }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch { documentDao.deleteDocument(document) }
    }

    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch { emergencyContactDao.insertContact(contact) }
    }

    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch { emergencyContactDao.deleteContact(contact) }
    }

    // Vehicle Health Score Algorithm (0 - 100)
    fun calculateHealthScore(vehicle: Vehicle, remindersList: List<Reminder>, fuelList: List<FuelEntry>, serviceList: List<Maintenance>): Int {
        var score = 100

        val vReminders = remindersList.filter { it.vehicleId == vehicle.id && !it.isCompleted }
        val vFuel = fuelList.filter { it.vehicleId == vehicle.id }
        val vService = serviceList.filter { it.vehicleId == vehicle.id }

        // Deduct for overdue pending tasks/reminders
        score -= vReminders.size * 12

        // Deduct if no fuel entries added
        if (vFuel.isEmpty()) score -= 15

        // Deduct if no service history logged
        if (vService.isEmpty()) score -= 20

        // Reward for recent service
        if (vService.isNotEmpty()) score += 10

        return score.coerceIn(20, 100)
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

    // Backup JSON Generator
    fun exportBackupJson(): String {
        val root = JSONObject()
        val vArray = JSONArray()
        vehicles.value.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("vehicleName", v.vehicleName)
                put("brand", v.brand)
                put("model", v.model)
                put("year", v.manufacturingYear)
                put("plate", v.registrationNumber)
                put("fuelType", v.fuelType)
                put("odometer", v.odometerReading)
            }
            vArray.put(obj)
        }
        root.put("vehicles", vArray)
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        return root.toString(2)
    }
}
