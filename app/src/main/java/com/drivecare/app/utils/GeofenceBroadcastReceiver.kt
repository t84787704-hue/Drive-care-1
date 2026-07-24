package com.drivecare.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.drivecare.app.data.db.AppDatabase
import com.drivecare.app.data.model.TripLog
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error event: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            val isEnter = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val allZones = db.geofenceZoneDao().getAllGeofences().firstOrNull() ?: emptyList()
                    val allVehicles = db.vehicleDao().getAllVehicles().firstOrNull() ?: emptyList()

                    for (geofence in triggeringGeofences) {
                        val requestId = geofence.requestId
                        val zoneId = requestId.removePrefix("geofence_").toLongOrNull() ?: continue
                        val zone = allZones.find { it.id == zoneId } ?: continue

                        val vehicle = allVehicles.find { it.id == zone.vehicleId }
                        val vehicleName = vehicle?.vehicleName ?: "Your Vehicle"

                        val title = if (isEnter) "Safe Zone Entered" else "Safe Zone Exited"
                        val message = if (isEnter) {
                            "Vehicle '$vehicleName' entered safe zone '${zone.zoneName}'."
                        } else {
                            "Vehicle '$vehicleName' exited safe zone '${zone.zoneName}'."
                        }

                        // 1. Post System Notification
                        DriveCareNotificationReceiver.showNotification(
                            context = context,
                            id = (zone.id.toInt() * 10) + (if (isEnter) 1 else 2),
                            title = title,
                            message = message,
                            targetTab = "MORE",
                            targetSection = "GPS",
                            recordId = zone.id
                        )

                        // 2. Log event into Trip History Database
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val now = Date()

                        val eventTypeLabel = if (isEnter) "Geofence Entry" else "Geofence Exit"
                        val logEntry = TripLog(
                            vehicleId = zone.vehicleId,
                            vehicleName = vehicleName,
                            driverName = "Geofence Guard",
                            startLocation = "$eventTypeLabel: ${zone.zoneName}",
                            endLocation = zone.zoneName,
                            distanceKm = 0.0,
                            durationMinutes = 0,
                            avgSpeedKmh = 0.0,
                            maxSpeedKmh = 0.0,
                            tripDate = dateFormat.format(now),
                            startTime = timeFormat.format(now),
                            endTime = timeFormat.format(now)
                        )
                        db.tripLogDao().insertTrip(logEntry)

                        Log.d(TAG, "Geofence transition processed: $eventTypeLabel for ${zone.zoneName}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition in receiver", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
