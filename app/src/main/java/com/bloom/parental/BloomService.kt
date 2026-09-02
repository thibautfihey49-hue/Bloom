package com.bloom.parental

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import android.os.IBinder

class BloomService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("BLOOM", "Bloom", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
        val notif = Notification.Builder(this, "BLOOM")
            .setContentTitle("🌸 Bloom actif")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        startForeground(1, notif)
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
