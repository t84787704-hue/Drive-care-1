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
        VehicleTelemetry::class,
        InsurancePolicy::class,
        GpsTrackerDevice::class,
        TrackerLocationPoint::class,
        GeofenceEventLog::class
    ],
    version = 12,
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
    abstract fun insurancePolicyDao(): InsurancePolicyDao
    abstract fun gpsTrackerDao(): GpsTrackerDao
    abstract fun trackerLocationDao(): TrackerLocationDao
    abstract fun geofenceEventDao(): GeofenceEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `docPath` TEXT NOT NULL,
                        `expiryDate` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `emergency_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `serviceType` TEXT NOT NULL,
                        `isPrimary` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `expenseDate` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
            }
        }

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `insurance_policies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `providerName` TEXT NOT NULL,
                        `policyNumber` TEXT NOT NULL,
                        `coverageType` TEXT NOT NULL,
                        `premiumAmount` REAL NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `expiryDate` TEXT NOT NULL,
                        `agentContact` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isAutoRenewEnabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `fileUri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `mimeType` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `fileSize` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `maintenance` ADD COLUMN `invoicePhotoUri` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `maintenance` ADD COLUMN `nextDueServiceDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `maintenance` ADD COLUMN `reminderDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `reminderDaysBefore` INTEGER NOT NULL DEFAULT 7")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `gps_trackers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerName` TEXT NOT NULL,
                        `trackerId` TEXT NOT NULL,
                        `imeiNumber` TEXT NOT NULL,
                        `simNumber` TEXT NOT NULL DEFAULT '',
                        `vehicleId` INTEGER,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `isOnline` INTEGER NOT NULL DEFAULT 1,
                        `lastLatitude` REAL,
                        `lastLongitude` REAL,
                        `lastSpeedKmh` REAL,
                        `lastUpdatedTime` INTEGER,
                        `createdDate` TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracker_location_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerId` TEXT NOT NULL,
                        `vehicleId` INTEGER,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `speedKmh` REAL NOT NULL DEFAULT 0.0,
                        `timestamp` INTEGER NOT NULL,
                        `addressName` TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `geofence_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `trackerId` TEXT NOT NULL DEFAULT '',
                        `zoneName` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `eventDate` TEXT NOT NULL,
                        `eventTime` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `vin` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `purchaseDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `imageUri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `insurance_policies` ADD COLUMN `claimContact` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `insurance_policies` ADD COLUMN `emergencyContact` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `insurance_policies` ADD COLUMN `documentUri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `insurance_policies` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `fuel_entries` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `maintenance` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `insurance_policies` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `geofence_zones` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drivecare_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
