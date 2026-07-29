package com.drivecare.app.services

import android.util.Log
import com.drivecare.app.utils.FcmNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriveCareMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("DriveCareMessagingService", "New FCM registration token received: $token")
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (currentUid.isNotBlank()) {
            FcmNotificationManager.updateUserFcmTokenInFirestore(this, currentUid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "DriveCare"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["message"]
            ?: "You have a new notification."
            
        val type = remoteMessage.data["type"] ?: "GENERAL"
        val friendUid = remoteMessage.data["friendUid"] ?: remoteMessage.data["friend_uid"]
        val friendName = remoteMessage.data["friendName"] ?: remoteMessage.data["friend_name"]
        val targetTab = remoteMessage.data["targetTab"] ?: remoteMessage.data["target_tab"]
        val targetSection = remoteMessage.data["targetSection"] ?: remoteMessage.data["target_section"]

        Log.d("DriveCareMessagingService", "FCM Message received: Title='$title', Body='$body', Type='$type'")

        FcmNotificationManager.showNotification(
            context = this,
            title = title,
            body = body,
            type = type,
            friendUid = friendUid,
            friendName = friendName,
            targetTab = targetTab,
            targetSection = targetSection
        )
    }
}

