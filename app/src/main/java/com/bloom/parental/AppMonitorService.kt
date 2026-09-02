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
        creerCanalNotification()
        // ✅ SANS 3e paramètre — utilise celui du Manifest
        startForeground(1001, creerNotification())
        handler.postDelayed(loop, 3000)
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "BLOOM_MONITOR",
                "Bloom — Surveillance",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun creerNotification(): Notification {
        return Notification.Builder(this, "BLOOM_MONITOR")
            .setContentTitle("🌸 Bloom — Surveillance active")
            .setContentText("Contrôle parental en cours")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setOngoing(true)
            .build()
    }

    private fun verifierBlocage() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val bloquees = prefs.getStringSet("APPS_BLOQUEES", emptySet()) ?: return
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.getRunningTasks(50).forEach { tache ->
            val pkg = tache.baseActivity?.packageName ?: return@forEach
            if (bloquees.contains(pkg)) am.killBackgroundProcesses(pkg)
        }
    }

    private fun enregistrerTemps() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val topApp = am.getRunningTasks(1).firstOrNull()?.baseActivity?.packageName ?: return
        val tempsActuel = prefs.getLong("TEMPS_APP_$topApp", 0L)
        prefs.edit().putLong("TEMPS_APP_$topApp", tempsActuel + 15_000).apply()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loop)
    }
}
