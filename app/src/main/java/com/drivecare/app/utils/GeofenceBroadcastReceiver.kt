package com.drivecare.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.drivecare.app.data.db.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val allZones = db.geofenceZoneDao().getGeofencesForUserSync(uid)
                    val allVehicles = db.vehicleDao().getVehiclesForUserSync(uid)

                    var triggeringLocation = geofencingEvent.triggeringLocation
                    if (triggeringLocation == null) {
                        try {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            triggeringLocation = com.google.android.gms.tasks.Tasks.await(fusedClient.lastLocation)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to retrieve last known location for geofence event", e)
                        }
                    }

                    if (triggeringLocation == null) {
                        Log.w(TAG, "Geofence event ignored: Triggering location unavailable.")
                        return@launch
                    }

                    for (geofence in triggeringGeofences) {
                        val requestId = geofence.requestId
                        val zoneId = requestId.removePrefix("geofence_").toLongOrNull() ?: continue
                        val zone = allZones.find { it.id == zoneId } ?: continue

                        val vehicle = allVehicles.find { it.id == zone.vehicleId }
                        val vehicleName = vehicle?.vehicleName ?: "Your Vehicle"

                        // Evaluate transition using GeofenceEvaluator to enforce state memory, hysteresis, accuracy, & debouncing
                        GeofenceEvaluator.evaluateZoneForLocation(
                            context = context,
                            db = db,
                            location = triggeringLocation,
                            zone = zone,
                            vehicleName = vehicleName
                        )
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

