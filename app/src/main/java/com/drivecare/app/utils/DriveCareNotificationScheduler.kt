package com.drivecare.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DriveCareNotificationScheduler {
    private const val REQUEST_CODE = 9901

    fun schedulePeriodicCheck(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DriveCareNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val intervalMillis = AlarmManager.INTERVAL_HALF_DAY
            val triggerAtMillis = System.currentTimeMillis() + 60_000L

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Run immediate check in background thread
        triggerImmediateCheck(context)
    }

    fun triggerImmediateCheck(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DriveCareNotificationReceiver.executeNotificationCheck(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
