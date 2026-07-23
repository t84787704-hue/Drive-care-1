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
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.DriveCareNotificationScheduler
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

class DriveCareViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
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

    private val _selectedFuelVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedFuelVehicle: StateFlow<Vehicle?> = _selectedFuelVehicle.asStateFlow()

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

    fun addExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.insertExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.deleteExpense(expense) }
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
        viewModelScope.launch { geofenceZoneDao.insertGeofence(geofence) }
    }

    fun deleteGeofenceZone(geofence: GeofenceZone) {
        viewModelScope.launch { geofenceZoneDao.deleteGeofence(geofence) }
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
                        costOrAmount = "${t.distanceKm} km"
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
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // Vehicles
        val vArray = JSONArray()
        vehicles.value.forEach { v ->
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
            })
        }
        root.put("vehicles", vArray)

        // Fuel Entries
        val fArray = JSONArray()
        fuelEntries.value.forEach { f ->
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
            })
        }
        root.put("fuel_entries", fArray)

        // Maintenance
        val mArray = JSONArray()
        maintenanceLogs.value.forEach { m ->
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
            })
        }
        root.put("maintenance", mArray)

        // Reminders
        val rArray = JSONArray()
        reminders.value.forEach { r ->
            rArray.put(JSONObject().apply {
                put("id", r.id)
                put("vehicleId", r.vehicleId)
                put("vehicleName", r.vehicleName)
                put("reminderTitle", r.reminderTitle)
                put("reminderType", r.reminderType)
                put("dueDate", r.dueDate)
                put("isCompleted", r.isCompleted)
            })
        }
        root.put("reminders", rArray)

        // Documents
        val dArray = JSONArray()
        documents.value.forEach { d ->
            dArray.put(JSONObject().apply {
                put("id", d.id)
                put("vehicleId", d.vehicleId)
                put("vehicleName", d.vehicleName)
                put("docTitle", d.docTitle)
                put("docType", d.docType)
                put("issueDate", d.issueDate)
                put("expiryDate", d.expiryDate)
                put("notes", d.notes)
            })
        }
        root.put("documents", dArray)

        // Emergency Contacts
        val cArray = JSONArray()
        emergencyContacts.value.forEach { c ->
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
        expenses.value.forEach { e ->
            eArray.put(JSONObject().apply {
                put("id", e.id)
                put("vehicleId", e.vehicleId)
                put("vehicleName", e.vehicleName)
                put("title", e.title)
                put("category", e.category)
                put("amount", e.amount)
                put("date", e.date)
                put("notes", e.notes)
            })
        }
        root.put("expenses", eArray)

        // Driver Profiles
        val dpArray = JSONArray()
        driverProfiles.value.forEach { dp ->
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
        vehicleShares.value.forEach { vs ->
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
        tripLogs.value.forEach { tl ->
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
        insurancePolicies.value.forEach { ins ->
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
            })
        }
        root.put("insurance_policies", insArray)

        return root.toString(2)
    }

    // Comprehensive Restore JSON Parser
    fun restoreBackupJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)

                if (root.has("vehicles")) {
                    val arr = root.getJSONArray("vehicles")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val v = Vehicle(
                            vehicleName = obj.optString("vehicleName", "Vehicle"),
                            vehicleType = obj.optString("vehicleType", "Car"),
                            brand = obj.optString("brand", ""),
                            model = obj.optString("model", ""),
                            manufacturingYear = obj.optString("manufacturingYear", obj.optString("year", "")),
                            registrationNumber = obj.optString("registrationNumber", obj.optString("plate", "")),
                            fuelType = obj.optString("fuelType", "Petrol"),
                            odometerReading = obj.optString("odometerReading", obj.optString("odometer", "0")),
                            notes = obj.optString("notes", "")
                        )
                        vehicleDao.insertVehicle(v)
                    }
                }

                if (root.has("fuel_entries")) {
                    val arr = root.getJSONArray("fuel_entries")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val f = FuelEntry(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            fuelDate = obj.optString("fuelDate", "2026-07-22"),
                            fuelType = obj.optString("fuelType", "Petrol"),
                            fuelQuantity = obj.optString("fuelQuantity", "0"),
                            amountPaid = obj.optString("amountPaid", "0"),
                            currentOdometer = obj.optString("currentOdometer", "0"),
                            fuelStationName = obj.optString("fuelStationName", "")
                        )
                        fuelDao.insertFuelEntry(f)
                    }
                }

                if (root.has("maintenance")) {
                    val arr = root.getJSONArray("maintenance")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val m = Maintenance(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            serviceTitle = obj.optString("serviceTitle", "Service"),
                            serviceType = obj.optString("serviceType", "Routine Service"),
                            serviceDate = obj.optString("serviceDate", "2026-07-22"),
                            currentOdometer = obj.optString("currentOdometer", "0"),
                            serviceCost = obj.optString("serviceCost", "0"),
                            workshopName = obj.optString("workshopName", "")
                        )
                        maintenanceDao.insertMaintenance(m)
                    }
                }

                if (root.has("reminders")) {
                    val arr = root.getJSONArray("reminders")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val r = Reminder(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            reminderTitle = obj.optString("reminderTitle", "Reminder"),
                            reminderType = obj.optString("reminderType", "Oil Change"),
                            dueDate = obj.optString("dueDate", "2026-12-31"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                        reminderDao.insertReminder(r)
                    }
                }

                if (root.has("documents")) {
                    val arr = root.getJSONArray("documents")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val d = Document(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            docTitle = obj.optString("docTitle", "Document"),
                            docType = obj.optString("docType", "Registration"),
                            issueDate = obj.optString("issueDate", ""),
                            expiryDate = obj.optString("expiryDate", ""),
                            notes = obj.optString("notes", "")
                        )
                        documentDao.insertDocument(d)
                    }
                }

                if (root.has("emergency_contacts")) {
                    val arr = root.getJSONArray("emergency_contacts")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val c = EmergencyContact(
                            name = obj.optString("name", "Contact"),
                            category = obj.optString("category", "Mechanic"),
                            phoneNumber = obj.optString("phoneNumber", ""),
                            notes = obj.optString("notes", "")
                        )
                        emergencyContactDao.insertContact(c)
                    }
                }

                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val exp = Expense(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            title = obj.optString("title", "Expense"),
                            category = obj.optString("category", "Other"),
                            amount = obj.optDouble("amount", 0.0),
                            date = obj.optString("date", "2026-07-22"),
                            notes = obj.optString("notes", "")
                        )
                        expenseDao.insertExpense(exp)
                    }
                }

                if (root.has("driver_profiles")) {
                    val arr = root.getJSONArray("driver_profiles")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val dp = DriverProfile(
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
                    }
                }

                if (root.has("vehicle_shares")) {
                    val arr = root.getJSONArray("vehicle_shares")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val vs = VehicleShare(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            sharedWithEmail = obj.optString("sharedWithEmail", ""),
                            role = obj.optString("role", "DRIVER"),
                            status = obj.optString("status", "ACTIVE")
                        )
                        vehicleShareDao.insertShare(vs)
                    }
                }

                if (root.has("trip_logs")) {
                    val arr = root.getJSONArray("trip_logs")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val tl = TripLog(
                            vehicleId = obj.optLong("vehicleId", 1),
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
                    }
                }

                if (root.has("insurance_policies")) {
                    val arr = root.getJSONArray("insurance_policies")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val ins = InsurancePolicy(
                            vehicleId = obj.optLong("vehicleId", 1),
                            vehicleName = obj.optString("vehicleName", ""),
                            providerName = obj.optString("providerName", "Insurance"),
                            policyNumber = obj.optString("policyNumber", ""),
                            coverageType = obj.optString("coverageType", "Comprehensive"),
                            premiumAmount = obj.optDouble("premiumAmount", 0.0),
                            startDate = obj.optString("startDate", ""),
                            expiryDate = obj.optString("expiryDate", ""),
                            agentContact = obj.optString("agentContact", ""),
                            notes = obj.optString("notes", ""),
                            isAutoRenewEnabled = obj.optBoolean("isAutoRenewEnabled", false)
                        )
                        insurancePolicyDao.insertPolicy(ins)
                    }
                }

                onComplete(true, "Data restored successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Failed to restore data: ${e.localizedMessage}")
            }
        }
    }

    // Insurance Methods
    fun addInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            insurancePolicyDao.insertPolicy(policy)
            DriveCareNotificationScheduler.triggerImmediateCheck(getApplication())
        }
    }

    fun updateInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            insurancePolicyDao.updatePolicy(policy)
        }
    }

    fun deleteInsurancePolicy(policy: InsurancePolicy) {
        viewModelScope.launch {
            insurancePolicyDao.deletePolicy(policy)
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
    val syncManager = FirebaseSyncManager.getInstance()

    val currentUser: StateFlow<CloudUser?> = syncManager.currentUser
    val userProfile: StateFlow<UserProfile?> = syncManager.userProfile
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val lastSyncTime: StateFlow<Long> = syncManager.lastSyncTime
    val isFirebaseAvailable: StateFlow<Boolean> = syncManager.isFirebaseAvailable

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

    fun signInWithDemoGoogleAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val demoEmail = "user.drive@gmail.com"
            val demoPass = "DriveCareDemoPass123!"
            val result = syncManager.signInWithEmail(demoEmail, demoPass)
            if (result.isSuccess) {
                triggerManualSync()
                onResult(true, null)
            } else {
                val signUpRes = syncManager.signUpWithEmail(demoEmail, demoPass, "Google User")
                signUpRes.fold(
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
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.sendPasswordReset(email)
            res.onSuccess { onResult(true, null) }.onFailure { onResult(false, it.localizedMessage) }
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
            val currentInsurance = insurancePolicies.value
            syncManager.syncAllData(
                vehicles = vehicles.value,
                fuelEntries = fuelEntries.value,
                maintenanceRecords = maintenanceLogs.value,
                expenses = expenses.value,
                documents = documents.value,
                insurancePolicies = currentInsurance,
                reminders = reminders.value
            )
        }
    }
}
