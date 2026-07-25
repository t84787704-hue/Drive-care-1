package com.drivecare.app.utils

import android.content.Context
import android.location.Location
import android.util.Log
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.GeofenceZone
import com.drivecare.app.data.model.TripLog
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeofenceEvaluator {
    private const val TAG = "GeofenceEvaluator"
    private const val PREFS_NAME = "drivecare_geofence_eval_prefs"
    private const val DEBOUNCE_MS = 30_000L // 30 seconds debounce
    private const val MAX_GPS_ACCURACY_METERS = 50.0f
    private const val MIN_MOVEMENT_METERS = 5.0f
    private const val MAX_LOCATION_AGE_MS = 120_000L // 2 minutes max age

    /**
     * Evaluates a single location against all active geofence zones.
     */
    suspend fun evaluateAllGeofences(context: Context, location: Location) {
        val db = AppDatabase.getDatabase(context)
        val allZones = db.geofenceZoneDao().getAllGeofences().firstOrNull() ?: emptyList()
        val allVehicles = db.vehicleDao().getAllVehicles().firstOrNull() ?: emptyList()

        for (zone in allZones) {
            if (!zone.isActive) continue
            val vehicle = allVehicles.find { it.id == zone.vehicleId }
            val vehicleName = vehicle?.vehicleName ?: "Your Vehicle"
            evaluateZoneForLocation(context, db, location, zone, vehicleName)
        }
    }

    /**
     * Evaluates a location against a specific geofence zone.
     */
    suspend fun evaluateZoneForLocation(
        context: Context,
        db: AppDatabase,
        location: Location,
        zone: GeofenceZone,
        vehicleName: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowMs = System.currentTimeMillis()

        // 1. Invalid Location Checks
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = getStoredState(prefs, zone.id),
                currentState = getStoredState(prefs, zone.id),
                distanceMeters = 0.0,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = 0.0f,
                reason = "Ignored: Invalid coordinates (0.0, 0.0)"
            )
            return
        }

        if (location.latitude < -90.0 || location.latitude > 90.0 || location.longitude < -180.0 || location.longitude > 180.0) {
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = getStoredState(prefs, zone.id),
                currentState = getStoredState(prefs, zone.id),
                distanceMeters = 0.0,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = 0.0f,
                reason = "Ignored: Coordinates out of valid latitude/longitude bounds"
            )
            return
        }

        if (location.time > 0 && (nowMs - location.time) > MAX_LOCATION_AGE_MS) {
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = getStoredState(prefs, zone.id),
                currentState = getStoredState(prefs, zone.id),
                distanceMeters = 0.0,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = 0.0f,
                reason = "Ignored: Location is stale (${(nowMs - location.time) / 1000}s old)"
            )
            return
        }

        val accuracy = if (location.hasAccuracy()) location.accuracy else 999.0f
        if (accuracy > MAX_GPS_ACCURACY_METERS) {
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = getStoredState(prefs, zone.id),
                currentState = getStoredState(prefs, zone.id),
                distanceMeters = 0.0,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "Ignored: GPS accuracy is too poor (${String.format(Locale.US, "%.1f", accuracy)}m > ${MAX_GPS_ACCURACY_METERS}m threshold)"
            )
            return
        }

        // 2. Distance Calculation & Hysteresis Buffer
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            zone.centerLatitude,
            zone.centerLongitude,
            results
        )
        val distanceMeters = results[0].toDouble()

        val hysteresisBuffer = maxOf(20.0, zone.radiusMeters * 0.05) // 20m or 5% of radius
        val entryThreshold = zone.radiusMeters - hysteresisBuffer
        val exitThreshold = zone.radiusMeters + hysteresisBuffer

        // 3. Retrieve Stored Previous State & Last Location
        val prevState = getStoredState(prefs, zone.id)
        val lastEvalLat = prefs.getFloat("zone_last_lat_${zone.id}", 0f).toDouble()
        val lastEvalLng = prefs.getFloat("zone_last_lng_${zone.id}", 0f).toDouble()

        // 4. Minimum Movement Validation
        if (lastEvalLat != 0.0 && lastEvalLng != 0.0 && prevState != "UNKNOWN") {
            val movementResults = FloatArray(1)
            Location.distanceBetween(lastEvalLat, lastEvalLng, location.latitude, location.longitude, movementResults)
            val movedDistance = movementResults[0]
            if (movedDistance < MIN_MOVEMENT_METERS) {
                logEvaluation(
                    zoneName = zone.zoneName,
                    vehicleName = vehicleName,
                    prevState = prevState,
                    currentState = prevState,
                    distanceMeters = distanceMeters,
                    radiusMeters = zone.radiusMeters,
                    accuracyMeters = accuracy,
                    reason = "Ignored: Device moved only ${String.format(Locale.US, "%.1f", movedDistance)}m (< ${MIN_MOVEMENT_METERS}m threshold)"
                )
                return
            }
        }

        // 5. Determine Computed State with Hysteresis
        val computedState = when {
            distanceMeters <= entryThreshold -> "INSIDE"
            distanceMeters >= exitThreshold -> "OUTSIDE"
            else -> {
                // In hysteresis boundary zone between entryThreshold and exitThreshold
                if (prevState == "INSIDE" || prevState == "OUTSIDE") {
                    prevState // Retain current state to avoid flickering
                } else {
                    if (distanceMeters <= zone.radiusMeters) "INSIDE" else "OUTSIDE"
                }
            }
        }

        // Save last evaluated location
        prefs.edit()
            .putFloat("zone_last_lat_${zone.id}", location.latitude.toFloat())
            .putFloat("zone_last_lng_${zone.id}", location.longitude.toFloat())
            .apply()

        // 6. Handle First Run / UNKNOWN Baseline State
        if (prevState == "UNKNOWN") {
            setStoredState(prefs, zone.id, computedState)
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = "UNKNOWN",
                currentState = computedState,
                distanceMeters = distanceMeters,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "Baseline initialized to '$computedState' silently. First evaluation."
            )
            return
        }

        // 7. Check State Transition
        if (prevState == computedState) {
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = prevState,
                currentState = computedState,
                distanceMeters = distanceMeters,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "No state transition. Device remains $computedState."
            )
            return
        }

        // We have a state transition!
        val isEnter = (prevState == "OUTSIDE" && computedState == "INSIDE")
        val isExit = (prevState == "INSIDE" && computedState == "OUTSIDE")

        if (isEnter && !zone.notifyOnEnter) {
            setStoredState(prefs, zone.id, computedState)
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = prevState,
                currentState = computedState,
                distanceMeters = distanceMeters,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "Entered zone, but notifyOnEnter setting is disabled."
            )
            return
        }

        if (isExit && !zone.notifyOnExit) {
            setStoredState(prefs, zone.id, computedState)
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = prevState,
                currentState = computedState,
                distanceMeters = distanceMeters,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "Exited zone, but notifyOnExit setting is disabled."
            )
            return
        }

        // 8. Notification Debounce
        val notifType = if (isEnter) "ENTER" else "EXIT"
        val lastNotifTime = prefs.getLong("zone_last_notif_time_${zone.id}", 0L)
        val lastNotifType = prefs.getString("zone_last_notif_type_${zone.id}", "")

        if (lastNotifType == notifType && (nowMs - lastNotifTime) < DEBOUNCE_MS) {
            setStoredState(prefs, zone.id, computedState)
            logEvaluation(
                zoneName = zone.zoneName,
                vehicleName = vehicleName,
                prevState = prevState,
                currentState = computedState,
                distanceMeters = distanceMeters,
                radiusMeters = zone.radiusMeters,
                accuracyMeters = accuracy,
                reason = "Ignored: Notification debounced (${(nowMs - lastNotifTime) / 1000}s < ${DEBOUNCE_MS / 1000}s threshold)"
            )
            return
        }

        // 9. Update Saved State & Timestamps
        setStoredState(prefs, zone.id, computedState)
        prefs.edit()
            .putLong("zone_last_notif_time_${zone.id}", nowMs)
            .putString("zone_last_notif_type_${zone.id}", notifType)
            .apply()

        // 10. Format Notification & History Entry
        val dateNow = Date()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

        val dateStr = dateFormat.format(dateNow)
        val dayStr = dayFormat.format(dateNow)
        val timeStr = timeFormat.format(dateNow)

        val eventLabel = if (isEnter) "Entered Safe Zone: ${zone.zoneName}" else "Exited Safe Zone: ${zone.zoneName}"
        val notifMessage = "Vehicle: $vehicleName\n$eventLabel\n$dayStr, $dateStr\n$timeStr"

        logEvaluation(
            zoneName = zone.zoneName,
            vehicleName = vehicleName,
            prevState = prevState,
            currentState = computedState,
            distanceMeters = distanceMeters,
            radiusMeters = zone.radiusMeters,
            accuracyMeters = accuracy,
            reason = "TRIGGERED NOTIFICATION: Valid transition ($prevState -> $computedState)"
        )

        // Post System Notification
        DriveCareNotificationReceiver.showNotification(
            context = context,
            id = (zone.id.toInt() * 100) + (if (isEnter) 1 else 2),
            title = "📍 DriveCare",
            message = notifMessage,
            targetTab = "MORE",
            targetSection = "GPS",
            recordId = zone.id
        )

        // Save Complete Event History to Room DB
        val routePointsData = JSONObject().apply {
            put("type", "GEOFENCE_EVENT")
            put("safeZone", zone.zoneName)
            put("eventType", if (isEnter) "Entered" else "Exited")
            put("vehicle", vehicleName)
            put("date", dateStr)
            put("day", dayStr)
            put("time", timeStr)
            put("lat", location.latitude)
            put("lng", location.longitude)
            put("accuracy", accuracy)
            put("distanceMeters", distanceMeters)
        }.toString()

        val logEntry = TripLog(
            vehicleId = zone.vehicleId,
            vehicleName = vehicleName,
            driverName = "Safe Zone Guard",
            startLocation = if (isEnter) "Entered Safe Zone" else "Exited Safe Zone",
            endLocation = zone.zoneName,
            distanceKm = distanceMeters / 1000.0,
            durationMinutes = 0,
            avgSpeedKmh = if (location.hasSpeed()) (location.speed * 3.6) else 0.0,
            maxSpeedKmh = if (location.hasSpeed()) (location.speed * 3.6) else 0.0,
            tripDate = dateStr,
            startTime = timeStr,
            endTime = dayStr,
            fuelConsumedLiters = 0.0,
            routePointsJson = routePointsData
        )

        db.tripLogDao().insertTrip(logEntry)
    }

    private fun getStoredState(prefs: android.content.SharedPreferences, zoneId: Long): String {
        return prefs.getString("zone_state_$zoneId", "UNKNOWN") ?: "UNKNOWN"
    }

    private fun setStoredState(prefs: android.content.SharedPreferences, zoneId: Long, state: String) {
        prefs.edit().putString("zone_state_$zoneId", state).apply()
    }

    private fun logEvaluation(
        zoneName: String,
        vehicleName: String,
        prevState: String,
        currentState: String,
        distanceMeters: Double,
        radiusMeters: Double,
        accuracyMeters: Float,
        reason: String
    ) {
        Log.i(
            TAG,
            """
            --------------------------------------------------
            [GEOFENCE EVALUATION LOG]
            • Zone Name: $zoneName
            • Vehicle: $vehicleName
            • Previous State: $prevState
            • Current State: $currentState
            • Distance from Center: ${String.format(Locale.US, "%.1f", distanceMeters)}m
            • Geofence Radius: ${radiusMeters.toInt()}m
            • GPS Accuracy: ${String.format(Locale.US, "%.1f", accuracyMeters)}m
            • Decision: $reason
            --------------------------------------------------
            """.trimIndent()
        )
    }
}
