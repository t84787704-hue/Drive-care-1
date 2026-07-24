package com.drivecare.app.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.drivecare.app.data.model.GeofenceZone
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val TAG = "GeofenceManager"
    private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 1001

    private fun getGeofencingClient(context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, GEOFENCE_PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun registerGeofence(context: Context, zone: GeofenceZone, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!zone.isActive) {
            unregisterGeofence(context, zone.id)
            onResult(true, "Geofence inactive")
            return
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            onResult(false, "Fine location permission required for geofencing")
            return
        }

        var transitionTypes = 0
        if (zone.notifyOnEnter) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_ENTER
        }
        if (zone.notifyOnExit) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
        }

        if (transitionTypes == 0) {
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val geofence = Geofence.Builder()
            .setRequestId("geofence_${zone.id}")
            .setCircularRegion(
                zone.centerLatitude,
                zone.centerLongitude,
                zone.radiusMeters.toFloat().coerceAtLeast(50f)
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setNotificationResponsiveness(5000) // 5s responsiveness for battery balance
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            getGeofencingClient(context)
                .addGeofences(request, getGeofencePendingIntent(context))
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered geofence: ${zone.zoneName} (ID: ${zone.id})")
                    onResult(true, null)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to register geofence: ${zone.zoneName}", exception)
                    onResult(false, exception.localizedMessage)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering geofence", e)
            onResult(false, "Location permission missing")
        }
    }

    fun unregisterGeofence(context: Context, zoneId: Long) {
        val requestId = "geofence_$zoneId"
        getGeofencingClient(context)
            .removeGeofences(listOf(requestId))
            .addOnSuccessListener {
                Log.d(TAG, "Successfully removed geofence: $requestId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence: $requestId", e)
            }
    }

    fun syncAllGeofences(context: Context, zones: List<GeofenceZone>) {
        zones.forEach { zone ->
            if (zone.isActive) {
                registerGeofence(context, zone)
            } else {
                unregisterGeofence(context, zone.id)
            }
        }
    }
}
