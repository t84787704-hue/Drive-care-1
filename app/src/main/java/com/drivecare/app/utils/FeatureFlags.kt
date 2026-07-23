package com.drivecare.app.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeatureFlags {
    private const val PREFS_NAME = "drivecare_feature_flags"
    private const val KEY_GPS_TRACKING = "flag_gps_tracking"
    private const val KEY_GEOFENCING = "flag_geofencing"
    private const val KEY_TELEMETRY = "flag_telemetry"
    private const val KEY_FAMILY_SHARING = "flag_family_sharing"

    private val _gpsTrackingEnabled = MutableStateFlow(true)
    val gpsTrackingEnabled: StateFlow<Boolean> = _gpsTrackingEnabled.asStateFlow()

    private val _geofencingEnabled = MutableStateFlow(true)
    val geofencingEnabled: StateFlow<Boolean> = _geofencingEnabled.asStateFlow()

    private val _telemetryEnabled = MutableStateFlow(true)
    val telemetryEnabled: StateFlow<Boolean> = _telemetryEnabled.asStateFlow()

    private val _familySharingEnabled = MutableStateFlow(true)
    val familySharingEnabled: StateFlow<Boolean> = _familySharingEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _gpsTrackingEnabled.value = prefs.getBoolean(KEY_GPS_TRACKING, true)
        _geofencingEnabled.value = prefs.getBoolean(KEY_GEOFENCING, true)
        _telemetryEnabled.value = prefs.getBoolean(KEY_TELEMETRY, true)
        _familySharingEnabled.value = prefs.getBoolean(KEY_FAMILY_SHARING, true)
    }

    fun setGpsTrackingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_GPS_TRACKING, enabled).apply()
        _gpsTrackingEnabled.value = enabled
    }

    fun setGeofencingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_GEOFENCING, enabled).apply()
        _geofencingEnabled.value = enabled
    }

    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_TELEMETRY, enabled).apply()
        _telemetryEnabled.value = enabled
    }

    fun setFamilySharingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_FAMILY_SHARING, enabled).apply()
        _familySharingEnabled.value = enabled
    }
}
