package com.termux.ilynxos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.termux.x11.LorieApp

class ILynxApplication : LorieApp() {

    companion object {
        const val CHANNEL_ID = "ilynxos_runtime_channel"
        lateinit var instance: ILynxApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iLynxOS Desktop Runtime",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the iLynxOS Linux desktop session running in foreground"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
