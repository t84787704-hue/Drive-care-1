package com.drivecare.app.utils

import android.content.Context
import android.content.SharedPreferences

enum class AppFeature(val key: String) {
    DASHBOARD("dashboard"),
    GARAGE("garage"),
    FUEL("fuel"),
    SERVICES("services"),
    EXPENSES("expenses"),
    DOCUMENTS("documents"),
    INSURANCE("insurance"),
    REMINDERS("reminders"),
    NOTIFICATIONS("notifications"),
    GPS_TRACKING("gps_tracking"),
    FAMILY_SHARING("family_sharing"),
    SETTINGS("settings")
}

object FeatureGuideManager {
    private const val PREF_NAME = "drivecare_feature_guides"
    private const val KEY_PREFIX = "guide_shown_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isGuideShown(context: Context, feature: AppFeature): Boolean {
        return getPrefs(context).getBoolean(KEY_PREFIX + feature.key, false)
    }

    fun setGuideShown(context: Context, feature: AppFeature, shown: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PREFIX + feature.key, shown).apply()
    }

    fun resetAllGuides(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
