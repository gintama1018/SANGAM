package com.frameroom.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FrameRoomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SERVER,
                "FrameRoom Host Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the embedded FrameRoom server active during live events"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_SERVER = "frameroom_host_channel"
    }
}
