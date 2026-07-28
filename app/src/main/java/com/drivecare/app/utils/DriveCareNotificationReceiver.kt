package com.drivecare.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drivecare.app.MainActivity
import com.drivecare.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DriveCareNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    DriveCareNotificationScheduler.schedulePeriodicCheck(context)
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val geofences = db.geofenceZoneDao().getGeofencesForUserSync(uid)
                        GeofenceManager.syncAllGeofences(context, geofences)
                    } catch (e: Exception) {
                        android.util.Log.e("DriveCareNotificationReceiver", "Error re-registering geofences on boot", e)
                    }
                } else {
                    executeNotificationCheck(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "drivecare_vehicle_alerts"
        const val EXTRA_TARGET_TAB = "EXTRA_TARGET_TAB"
        const val EXTRA_TARGET_SECTION = "EXTRA_TARGET_SECTION"
        const val EXTRA_RECORD_ID = "EXTRA_RECORD_ID"

        suspend fun executeNotificationCheck(context: Context) {
            createNotificationChannel(context)

            val db = AppDatabase.getDatabase(context)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Calendar.getInstance().time)

            val prefs = context.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE)
            val langCode = prefs.getString("selected_language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
            val lang = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.ENGLISH

            var notificationId = 1000

            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

            // 1. Check Service & Maintenance Reminders
            val reminders = db.reminderDao().getRemindersForUserSync(uid)
            reminders.filter { !it.isCompleted }.forEach { r ->
                if (r.dueDate.isNotBlank() && r.dueDate <= todayStr) {
                    val titleFormat = AppStrings.get("notif_service_due_title", lang)
                    val msgFormat = AppStrings.get("notif_service_due_msg", lang)
                    showNotification(
                        context = context,
                        id = (10000 + r.id).toInt(),
                        title = String.format(titleFormat, r.vehicleName),
                        message = String.format(msgFormat, r.reminderTitle, r.dueDate),
                        targetTab = "SERVICE",
                        recordId = r.id
                    )
                }
            }

            // 2. Check Document Expiration
            val documents = db.documentDao().getDocumentsForUserSync(uid)
            val warnCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 15) }
            val warnDateStr = sdf.format(warnCalendar.time)

            documents.forEach { doc ->
                if (doc.expiryDate.isNotBlank()) {
                    if (doc.expiryDate < todayStr) {
                        val titleFormat = AppStrings.get("notif_doc_expired_title", lang)
                        val msgFormat = AppStrings.get("notif_doc_expired_msg", lang)
                        showNotification(
                            context = context,
                            id = (20000 + doc.id).toInt(),
                            title = String.format(titleFormat, doc.docTitle),
                            message = String.format(msgFormat, doc.docType, doc.vehicleName, doc.expiryDate),
                            targetTab = "MORE",
                            targetSection = "DOCUMENTS",
                            recordId = doc.id
                        )
                    } else if (doc.expiryDate <= warnDateStr) {
                        val titleFormat = AppStrings.get("notif_doc_expiring_title", lang)
                        val msgFormat = AppStrings.get("notif_doc_expiring_msg", lang)
                        showNotification(
                            context = context,
                            id = (20000 + doc.id).toInt(),
                            title = String.format(titleFormat, doc.docTitle),
                            message = String.format(msgFormat, doc.docType, doc.vehicleName, doc.expiryDate),
                            targetTab = "MORE",
                            targetSection = "DOCUMENTS",
                            recordId = doc.id
                        )
                    }
                }
            }

            // 3. Check Insurance Policy Expiration (90, 60, 30, 15, 7, 1, 0 days, or expired)
            val insurancePolicies = db.insurancePolicyDao().getPoliciesForUserSync(uid)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
            val currentTimeStr = timeFormat.format(Calendar.getInstance().time)

            insurancePolicies.forEach { policy ->
                if (policy.expiryDate.isNotBlank()) {
                    val daysRemaining = policy.calculateDaysUntilExpiry()
                    if (daysRemaining != null) {
                        val isTriggerDay = daysRemaining in listOf(90L, 60L, 30L, 15L, 7L, 1L, 0L) || daysRemaining < 0L
                        if (isTriggerDay) {
                            val notifTitle = when {
                                daysRemaining < 0 -> "Expired Insurance Alert: ${policy.vehicleName}"
                                daysRemaining == 0L -> "Insurance Expires TODAY: ${policy.vehicleName}"
                                daysRemaining == 1L -> "Insurance Expires TOMORROW: ${policy.vehicleName}"
                                else -> "Insurance Renewal Reminder ($daysRemaining Days Left): ${policy.vehicleName}"
                            }

                            val notifMsg = "Vehicle: ${policy.vehicleName} | Policy #: ${policy.policyNumber} | Company: ${policy.providerName} | Date: ${policy.expiryDate} | Time: $currentTimeStr"

                            showNotification(
                                context = context,
                                id = (30000 + policy.id).toInt(),
                                title = notifTitle,
                                message = notifMsg,
                                targetTab = "MORE",
                                targetSection = "INSURANCE",
                                recordId = policy.id
                            )
                        }
                    }
                }
            }

            // 4. Check GPS Hardware Trackers Connectivity Status
            val trackers = db.gpsTrackerDao().getAllTrackersSync()
            val nowMs = System.currentTimeMillis()
            val offlineThresholdMs = 24 * 60 * 60 * 1000L // 24 hours silent

            trackers.forEach { tracker ->
                val lastUpdate = tracker.lastUpdatedTime
                if (lastUpdate != null && (nowMs - lastUpdate) > offlineThresholdMs) {
                    val title = AppStrings.get("notif_tracker_offline_title", lang)
                    val msgFormat = AppStrings.get("notif_tracker_offline_msg", lang)
                    showNotification(
                        context = context,
                        id = (40000 + tracker.id).toInt(),
                        title = title,
                        message = String.format(msgFormat, tracker.trackerName, tracker.imeiNumber),
                        targetTab = "MORE",
                        targetSection = "GPS",
                        recordId = tracker.id
                    )
                }
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "DriveCare Vehicle Alerts"
                val descriptionText = "Notifications for vehicle maintenance due dates, document renewals, and insurance expirations"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun showNotification(
            context: Context,
            id: Int,
            title: String,
            message: String,
            targetTab: String,
            targetSection: String? = null,
            recordId: Long? = null
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TARGET_TAB, targetTab)
                if (targetSection != null) {
                    putExtra(EXTRA_TARGET_SECTION, targetSection)
                }
                if (recordId != null) {
                    putExtra(EXTRA_RECORD_ID, recordId)
                }
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(id, builder.build())
                }
            } catch (e: SecurityException) {
                // Permission missing
            }
        }
    }
}
