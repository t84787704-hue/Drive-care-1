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

        suspend fun executeNotificationCheck(context: Context) {
            createNotificationChannel(context)

            val db = AppDatabase.getDatabase(context)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Calendar.getInstance().time)

            var notificationId = 1000

            // 1. Check Service & Maintenance Reminders
            val reminders = db.reminderDao().getAllRemindersSync()
            reminders.filter { !it.isCompleted }.forEach { r ->
                if (r.dueDate.isNotBlank() && r.dueDate <= todayStr) {
                    showNotification(
                        context = context,
                        id = notificationId++,
                        title = "Service Due: ${r.vehicleName}",
                        message = "${r.reminderTitle} is due on ${r.dueDate}. Keep your vehicle maintained!"
                    )
                }
            }

            // 2. Check Document Expiration
            val documents = db.documentDao().getAllDocumentsSync()
            val warnCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 15) }
            val warnDateStr = sdf.format(warnCalendar.time)

            documents.forEach { doc ->
                if (doc.expiryDate.isNotBlank()) {
                    if (doc.expiryDate < todayStr) {
                        showNotification(
                            context = context,
                            id = notificationId++,
                            title = "Document Expired: ${doc.docTitle}",
                            message = "${doc.docType} for ${doc.vehicleName} expired on ${doc.expiryDate}. Please renew immediately."
                        )
                    } else if (doc.expiryDate <= warnDateStr) {
                        showNotification(
                            context = context,
                            id = notificationId++,
                            title = "Document Expiring Soon: ${doc.docTitle}",
                            message = "${doc.docType} for ${doc.vehicleName} expires on ${doc.expiryDate}."
                        )
                    }
                }
            }

            // 3. Check Insurance Policy Expiration
            val insurancePolicies = db.insurancePolicyDao().getAllInsurancePoliciesSync()
            val insWarnCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
            val insWarnDateStr = sdf.format(insWarnCalendar.time)

            insurancePolicies.forEach { policy ->
                if (policy.expiryDate.isNotBlank()) {
                    if (policy.expiryDate < todayStr) {
                        showNotification(
                            context = context,
                            id = notificationId++,
                            title = "Insurance Expired: ${policy.vehicleName}",
                            message = "Insurance Policy #${policy.policyNumber} (${policy.providerName}) expired on ${policy.expiryDate}!"
                        )
                    } else if (policy.expiryDate <= insWarnDateStr) {
                        showNotification(
                            context = context,
                            id = notificationId++,
                            title = "Insurance Renewal Alert",
                            message = "${policy.vehicleName} policy #${policy.policyNumber} expires in less than 30 days (${policy.expiryDate})."
                        )
                    }
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

        private fun showNotification(context: Context, id: Int, title: String, message: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
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
