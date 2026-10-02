package com.baris.tvappswitcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AccessibilityForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "accessibility_foreground_channel"
        private const val NOTIFICATION_ID = 1001

        // FOREGROUND_SERVICE_TYPE_ACCESSIBILITY = 1024 (added in API 34)
        private const val FOREGROUND_SERVICE_TYPE_ACCESSIBILITY = 1024
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_ACCESSIBILITY)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Accessibility Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps Liquid Switcher accessibility service running in background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.service_label))
        .setContentText(getString(R.string.accessibility_service_running))
        .setSmallIcon(R.drawable.ic_launcher)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setShowWhen(false)
        .build()
}
