package com.drivecare.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drivecare.app.MainActivity
import com.drivecare.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmNotificationManager {

    private const val TAG = "FcmNotificationManager"

    // Feature 7 Notification Channels
    const val CHANNEL_FRIEND_REQUESTS = "drivecare_friend_requests"
    const val CHANNEL_VEHICLE_SHARING = "drivecare_vehicle_sharing"
    const val CHANNEL_GENERAL_NOTIFICATIONS = "drivecare_general_notifications"
    const val CHANNEL_VEHICLE_ALERTS = "drivecare_vehicle_alerts"

    private var notificationListener: ListenerRegistration? = null
    private var isListenerActive = false
    private var activeUid: String? = null

    /**
     * Feature 7: Initialize all FCM & Application Notification Channels
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_FRIEND_REQUESTS,
                    "Friend Requests",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for incoming friend requests and friendship acceptances"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_VEHICLE_SHARING,
                    "Vehicle Sharing",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when vehicles are shared with you or access permissions update"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_GENERAL_NOTIFICATIONS,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General DriveCare system notifications and cloud updates"
                },
                NotificationChannel(
                    CHANNEL_VEHICLE_ALERTS,
                    "DriveCare Vehicle Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for maintenance due dates, expiring insurance, and document renewals"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
            Log.d(TAG, "Notification channels registered successfully")
        }
    }

    /**
     * Feature 8: Retrieve and update FCM Token for user in Firestore (users/{uid}/fcmToken)
     */
    fun syncFcmToken(context: Context, uid: String? = null) {
        val targetUid = uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (targetUid.isBlank()) return

        // Explicitly unsubscribe from global 'all' topic so push notifications send only to user-specific tokens
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
        } catch (e: Exception) {
            Log.w(TAG, "Unsubscribe from 'all' topic failed: ${e.message}")
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val token = task.result
                updateUserFcmTokenInFirestore(context, targetUid, token)
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    /**
     * Feature 8: Update specific token string in Firestore
     */
    fun updateUserFcmTokenInFirestore(context: Context, uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return

        // Save locally
        val prefs = context.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token_$uid", token).apply()

        // Sync to Firestore users/{uid} and user_tokens/{uid}
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val updateMap = mapOf(
                    "fcmToken" to token,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(updateMap, SetOptions.merge()).await()
                db.collection("user_tokens").document(uid).set(updateMap, SetOptions.merge()).await()
                Log.d(TAG, "FCM token updated successfully in Firestore for user $uid")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Feature 8: Remove FCM token upon sign out
     */
    fun removeFcmToken(context: Context, uid: String) {
        if (uid.isBlank()) return
        stopRealtimeNotificationListener()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val clearMap = mapOf(
                    "fcmToken" to "",
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(clearMap, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear FCM token: ${e.message}")
            }
        }
    }

    /**
     * Feature 10: Check user notification preferences
     */
    fun isNotificationEnabled(context: Context, type: String): Boolean {
        val prefs = context.getSharedPreferences("drivecare_prefs", Context.MODE_PRIVATE)
        return when (type.uppercase()) {
            "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> {
                prefs.getBoolean("notify_friend_requests", true)
            }
            "VEHICLE_SHARING", "VEHICLE_SHARED" -> {
                prefs.getBoolean("notify_vehicle_sharing", true)
            }
            else -> {
                prefs.getBoolean("notify_general", true)
            }
        }
    }

    /**
     * Feature 9 & 1-6: Display system notification bar item with proper channel & navigation intent
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String,
        friendUid: String? = null,
        friendName: String? = null,
        targetTab: String? = null,
        targetSection: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        if (!isNotificationEnabled(context, type)) {
            Log.d(TAG, "Notification type $type disabled in user settings; suppressing.")
            return
        }

        val channelId = when (type.uppercase()) {
            "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> CHANNEL_FRIEND_REQUESTS
            "VEHICLE_SHARING", "VEHICLE_SHARED" -> CHANNEL_VEHICLE_SHARING
            "SERVICE", "INSURANCE", "DOCUMENTS", "EXPENSES" -> CHANNEL_VEHICLE_ALERTS
            else -> CHANNEL_GENERAL_NOTIFICATIONS
        }

        // Determine destination tab & section for tap action
        val (finalTab, finalSection) = when {
            !targetTab.isNullOrBlank() -> Pair(targetTab, targetSection ?: "")
            type.startsWith("FRIEND") -> Pair("MORE", "FAMILY_SHARING")
            type.startsWith("VEHICLE") -> Pair("GARAGE", "MENU")
            else -> Pair("MORE", "MENU")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(DriveCareNotificationReceiver.EXTRA_TARGET_TAB, finalTab)
            if (finalSection.isNotBlank()) {
                putExtra(DriveCareNotificationReceiver.EXTRA_TARGET_SECTION, finalSection)
            }
            if (!friendUid.isNullOrBlank()) {
                putExtra("friend_uid", friendUid)
                putExtra("friend_name", friendName ?: "")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
            Log.d(TAG, "Displayed notification: $title - $body [$channelId]")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification: ${e.message}")
        }
    }

    /**
     * Send push notification document to recipient's Firestore inbox
     */
    fun sendPushNotification(
        recipientUid: String,
        title: String,
        body: String,
        type: String,
        friendUid: String? = null,
        friendName: String? = null,
        targetTab: String? = null,
        targetSection: String? = null
    ) {
        if (recipientUid.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val notifId = db.collection("users").document(recipientUid)
                    .collection("notifications").document().id
                val notifData = mapOf(
                    "id" to notifId,
                    "recipientUid" to recipientUid,
                    "title" to title,
                    "body" to body,
                    "message" to body,
                    "type" to type,
                    "friendUid" to (friendUid ?: ""),
                    "friendName" to (friendName ?: ""),
                    "targetTab" to (targetTab ?: ""),
                    "targetSection" to (targetSection ?: ""),
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("users").document(recipientUid)
                    .collection("notifications").document(notifId).set(notifData).await()
                Log.d(TAG, "Notification stored for $recipientUid: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification: ${e.message}")
            }
        }
    }

    /**
     * Feature 9: Realtime listener on user's notifications collection to deliver push alerts when app is active or in background
     */
    fun startRealtimeNotificationListener(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (isListenerActive && activeUid == uid) return
        stopRealtimeNotificationListener()

        val db = FirebaseFirestore.getInstance()
        val listenTime = System.currentTimeMillis() - 5000L // 5 seconds window

        isListenerActive = true
        activeUid = uid
        notificationListener = db.collection("users").document(uid)
            .collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user notifications: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (docChange in snapshot.documentChanges) {
                        if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val doc = docChange.document
                            val createdAt = doc.getLong("createdAt") ?: 0L
                            if (createdAt >= listenTime) {
                                val isRead = doc.getBoolean("isRead") ?: false
                                if (!isRead) {
                                    val title = doc.getString("title") ?: "DriveCare"
                                    val body = doc.getString("body") ?: doc.getString("message") ?: ""
                                    val type = doc.getString("type") ?: "GENERAL"
                                    val friendUid = doc.getString("friendUid")
                                    val friendName = doc.getString("friendName")
                                    val targetTab = doc.getString("targetTab")
                                    val targetSection = doc.getString("targetSection")

                                    showNotification(
                                        context = context,
                                        title = title,
                                        body = body,
                                        type = type,
                                        friendUid = friendUid,
                                        friendName = friendName,
                                        targetTab = targetTab,
                                        targetSection = targetSection,
                                        notificationId = doc.id.hashCode()
                                    )

                                    // Mark as read locally processed
                                    doc.reference.update("isRead", true)
                                }
                            }
                        }
                    }
                }
            }
        Log.d(TAG, "Realtime notification listener started for user $uid")
    }

    fun stopRealtimeNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
        isListenerActive = false
        activeUid = null
    }
}
