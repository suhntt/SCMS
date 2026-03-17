package com.example.scms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

// Handles incoming Push Notifications from backend ("Your complaint was resolved!")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "SCMS Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new civic update."

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Whenever Firebase generates a new token for this phone,
        // we can send it to our MySQL backend so it knows where to push alerts!
        // For now, it will just log.
        println("FCM Token Generated: $token")
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "scms_alerts_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Civic Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates on your complaints"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // built-in info icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(Random.nextInt(), notification)
    }
}
