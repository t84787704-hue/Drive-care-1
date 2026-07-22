package com.drivecare.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drivecare.app.data.dao.DocumentDao
import com.drivecare.app.data.dao.EmergencyContactDao
import com.drivecare.app.data.dao.FuelDao
import com.drivecare.app.data.dao.MaintenanceDao
import com.drivecare.app.data.dao.ReminderDao
import com.drivecare.app.data.dao.VehicleDao
import com.drivecare.app.data.model.Document
import com.drivecare.app.data.model.EmergencyContact
import com.drivecare.app.data.model.FuelEntry
import com.drivecare.app.data.model.Maintenance
import com.drivecare.app.data.model.Reminder
import com.drivecare.app.data.model.Vehicle

@Database(
    entities = [Vehicle::class, FuelEntry::class, Maintenance::class, Reminder::class, Document::class, EmergencyContact::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelDao(): FuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun documentDao(): DocumentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drivecare_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
