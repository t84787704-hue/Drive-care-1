package com.drivecare.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Reminder
import com.drivecare.app.data.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DriveCareViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val vehicleDao = db.vehicleDao()
    private val fuelDao = db.fuelDao()
    private val maintenanceDao = db.maintenanceDao()
    private val reminderDao = db.reminderDao()

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelEntries: StateFlow<List<FuelEntry>> = fuelDao.getAllFuelEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceLogs: StateFlow<List<Maintenance>> = maintenanceDao.getAllMaintenance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected vehicle for Fuel Tracker filtering
    private val _selectedFuelVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedFuelVehicle: StateFlow<Vehicle?> = _selectedFuelVehicle.asStateFlow()

    fun selectFuelVehicle(vehicle: Vehicle?) {
        _selectedFuelVehicle.value = vehicle
    }

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.insertVehicle(vehicle)
        }
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
            if (_selectedFuelVehicle.value?.id == vehicle.id) {
                _selectedFuelVehicle.value = null
            }
        }
    }

    fun addFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelDao.insertFuelEntry(entry)
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelDao.deleteFuelEntry(entry)
        }
    }

    fun addMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.insertMaintenance(maintenance)
        }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.deleteMaintenance(maintenance)
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.insertReminder(reminder)
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }
}
