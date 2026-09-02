package com.bloom.parental.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.bloom.parental.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppInstallMonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastPackages = mutableSetOf<String>()
    private val CHANNEL_ID = "bloom_install_monitor"

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AppInstallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2, createNotification())
        scanPackages()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Surveillance des installations",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Fonctionnement en arrière-plan"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Bloom")
                .setContentText("Surveillance des applications active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Bloom")
                .setContentText("Surveillance des applications active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .build()
        }
    }

    private fun scanPackages() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        lastPackages = apps.map { it.packageName }.toMutableSet()
    }

    private fun startMonitoring() {
        scope.launch {
            while (true) {
                detectNewApps()
                delay(5000) // Vérifie toutes les 5 secondes
            }
        }
    }

    private fun detectNewApps() {
        val pm = packageManager
        val currentPackages = pm.getInstalledApplications(0).map { it.packageName }.toSet()

        val newPackages = currentPackages subtract lastPackages

        for (pkg in newPackages) {
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val name = info.loadLabel(pm).toString()
                val desc = getAppDescription(pm, pkg, info)

                // Ajoute en attente et notifie le parent par SMS
                Prefs.addPendingApp(pkg, name, desc)

                // Envoie la demande au parent
                val msg = JSONObject()
                    .put("pkg", pkg)
                    .put("name", name)
                    .put("description", desc)
                    .put("action", "NEW_APP_REQUEST")
                BloomSmsManager.sendCommand(this, "newapp", msg.toString())

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        lastPackages = currentPackages.toMutableSet()
    }

    private fun getAppDescription(pm: PackageManager, pkg: String, info: ApplicationInfo): String {
        return try {
            // Essaye de récupérer la description depuis le manifeste ou le nom + infos
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isDebug = (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val sourceDir = info.sourceDir

            """
📦 $pkg
🔧 Système: ${if (isSystem) "Oui" else "Non"}
🐛 Debug: ${if (isDebug) "Oui" else "Non"}
📂 Emplacement: ${sourceDir.take(40)}...
            """.trimIndent()
        } catch (e: Exception) {
            "Package: $pkg"
        }
    }
}
