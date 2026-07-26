package com.drivecare.app.data.tracker

import android.content.Context
import android.location.Location
import com.drivecare.app.data.dao.*
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.*
import com.drivecare.app.utils.GeofenceEvaluator
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class GpsTrackerRepository(
    private val context: Context,
    private val gpsTrackerDao: GpsTrackerDao,
    private val trackerLocationDao: TrackerLocationDao,
    private val geofenceZoneDao: GeofenceZoneDao,
    private val geofenceEventDao: GeofenceEventDao,
    private val vehicleDao: VehicleDao
) {
    fun getAllTrackers(): Flow<List<GpsTrackerDevice>> = gpsTrackerDao.getAllTrackers()

    suspend fun getAllTrackersSync(): List<GpsTrackerDevice> = gpsTrackerDao.getAllTrackersSync()

    fun getTrackerByVehicle(vehicleId: Long): Flow<GpsTrackerDevice?> = gpsTrackerDao.getTrackerByVehicle(vehicleId)

    suspend fun getTrackerByVehicleSync(vehicleId: Long): GpsTrackerDevice? = gpsTrackerDao.getTrackerByVehicleSync(vehicleId)

    suspend fun insertTracker(tracker: GpsTrackerDevice): Long = gpsTrackerDao.insertTracker(tracker)

    suspend fun updateTracker(tracker: GpsTrackerDevice) = gpsTrackerDao.updateTracker(tracker)

    suspend fun deleteTracker(tracker: GpsTrackerDevice) {
        gpsTrackerDao.deleteTracker(tracker)
        trackerLocationDao.deleteHistoryForTracker(tracker.trackerId)
    }

    suspend fun unassignTrackerFromVehicle(vehicleId: Long) = gpsTrackerDao.unassignTrackerFromVehicle(vehicleId)

    fun getAllLocations(): Flow<List<TrackerLocationPoint>> = trackerLocationDao.getAllLocations()

    suspend fun getAllLocationsSync(): List<TrackerLocationPoint> = trackerLocationDao.getAllLocationsSync()

    fun getLocationsByTracker(trackerId: String): Flow<List<TrackerLocationPoint>> = trackerLocationDao.getLocationsByTracker(trackerId)

    fun getLocationsByVehicle(vehicleId: Long): Flow<List<TrackerLocationPoint>> = trackerLocationDao.getLocationsByVehicle(vehicleId)

    suspend fun deleteHistoryForTracker(trackerId: String) = trackerLocationDao.deleteHistoryForTracker(trackerId)

    fun getAllGeofenceEvents(): Flow<List<GeofenceEventLog>> = geofenceEventDao.getAllEvents()

    suspend fun getAllGeofenceEventsSync(): List<GeofenceEventLog> = geofenceEventDao.getAllEventsSync()

    fun getGeofenceEventsByVehicle(vehicleId: Long): Flow<List<GeofenceEventLog>> = geofenceEventDao.getEventsByVehicle(vehicleId)

    suspend fun ingestPayload(payload: TrackerPayload) {
        // 1. Find or create tracker device
        var tracker = gpsTrackerDao.getTrackerByCodeOrImei(payload.trackerCode)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        val now = Date(payload.timestamp)

        if (tracker == null) {
            val newTracker = GpsTrackerDevice(
                trackerName = "Tracker ${payload.trackerCode}",
                trackerId = payload.trackerCode,
                imeiNumber = payload.trackerCode,
                simNumber = "",
                vehicleId = null,
                notes = "Auto-registered hardware tracker (${payload.protocolVendor})",
                isOnline = true,
                lastLatitude = payload.latitude,
                lastLongitude = payload.longitude,
                lastSpeedKmh = payload.speedKmh,
                lastUpdatedTime = payload.timestamp,
                createdDate = dateFormat.format(now)
            )
            val newId = gpsTrackerDao.insertTracker(newTracker)
            tracker = newTracker.copy(id = newId)
        } else {
            val updatedTracker = tracker.copy(
                isOnline = true,
                lastLatitude = payload.latitude,
                lastLongitude = payload.longitude,
                lastSpeedKmh = payload.speedKmh,
                lastUpdatedTime = payload.timestamp
            )
            gpsTrackerDao.updateTracker(updatedTracker)
            tracker = updatedTracker
        }

        // 2. Save location point to history
        val locationPoint = TrackerLocationPoint(
            trackerId = tracker.trackerId,
            vehicleId = tracker.vehicleId,
            latitude = payload.latitude,
            longitude = payload.longitude,
            speedKmh = payload.speedKmh,
            timestamp = payload.timestamp,
            addressName = payload.addressName
        )
        trackerLocationDao.insertLocation(locationPoint)

        // 3. Geofence evaluation if assigned to a vehicle
        val assignedVehicleId = tracker.vehicleId
        if (assignedVehicleId != null) {
            val vehicle = vehicleDao.getVehicleById(assignedVehicleId)
            val vehicleName = vehicle?.vehicleName ?: "Vehicle #$assignedVehicleId"
            val activeZones = geofenceZoneDao.getActiveGeofencesByVehicleSync(assignedVehicleId)

            val loc = Location("HardwareGPS").apply {
                latitude = payload.latitude
                longitude = payload.longitude
                speed = (payload.speedKmh / 3.6).toFloat()
                time = payload.timestamp
            }

            val appDb = AppDatabase.getDatabase(context)
            for (zone in activeZones) {
                GeofenceEvaluator.evaluateZoneForLocation(context, appDb, loc, zone, vehicleName)
            }
        }
    }
}
