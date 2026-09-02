package com.bloom.parental

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.preference.PreferenceManager

class AppMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            verifierBlocage()
            enregistrerTemps()
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("BLOOM", "Bloom", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        startForeground(1001, Notification.Builder(this, "BLOOM")
            .setContentTitle("🌸 Bloom actif")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setOngoing(true).build())
        handler.postDelayed(loop, 3000)
    }

    private fun verifierBlocage() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val bloquees = prefs.getStringSet("APPS_BLOQUEES", emptySet()) ?: return
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.getRunningTasks(50).forEach { t ->
            val pkg = t.baseActivity?.packageName ?: return@forEach
            if (bloquees.contains(pkg)) am.killBackgroundProcesses(pkg)
        }
    }

    private fun enregistrerTemps() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val top = am.getRunningTasks(1).firstOrNull()?.baseActivity?.packageName ?: return
        prefs.edit().putLong("TEMPS_APP_$top", prefs.getLong("TEMPS_APP_$top", 0)+15).apply()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(loop) }
}
