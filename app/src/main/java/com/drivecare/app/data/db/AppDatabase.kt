package com.drivecare.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drivecare.app.data.dao.*
import com.drivecare.app.data.model.*

@Database(
    entities = [
        Vehicle::class,
        FuelEntry::class,
        Maintenance::class,
        Reminder::class,
        Document::class,
        EmergencyContact::class,
        Expense::class,
        DriverProfile::class,
        VehicleShare::class,
        TripLog::class,
        GeofenceZone::class,
        VehicleTelemetry::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelDao(): FuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun documentDao(): DocumentDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun driverProfileDao(): DriverProfileDao
    abstract fun vehicleShareDao(): VehicleShareDao
    abstract fun tripLogDao(): TripLogDao
    abstract fun geofenceZoneDao(): GeofenceZoneDao
    abstract fun vehicleTelemetryDao(): VehicleTelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `driver_profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `licenseNumber` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `firebaseUserId` TEXT NOT NULL,
                        `profilePhotoUrl` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastLoginAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `vehicle_shares` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `sharedWithEmail` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `sharedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `trip_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `driverName` TEXT NOT NULL,
                        `startLocation` TEXT NOT NULL,
                        `endLocation` TEXT NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `avgSpeedKmh` REAL NOT NULL,
                        `maxSpeedKmh` REAL NOT NULL,
                        `tripDate` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `fuelConsumedLiters` REAL NOT NULL,
                        `routePointsJson` TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `geofence_zones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `zoneName` TEXT NOT NULL,
                        `centerLatitude` REAL NOT NULL,
                        `centerLongitude` REAL NOT NULL,
                        `radiusMeters` REAL NOT NULL,
                        `notifyOnEnter` INTEGER NOT NULL,
                        `notifyOnExit` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `vehicle_telemetry` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `speedKmh` REAL NOT NULL,
                        `fuelLevelPct` REAL NOT NULL,
                        `batteryVoltage` REAL NOT NULL,
                        `engineTempC` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )"""
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drivecare_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
