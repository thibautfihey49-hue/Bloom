package com.bloom.gps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class LocationTrackerService : Service() {
    private val CHANNEL_ID = "BloomGPS_Service"
    private val PORT = 50006.toShort()
    private var locationManager: LocationManager? = null
    private var dernierEnvoi: Location? = null
    private var numeroDest: String = ""
    private var premierEnvoi = false
    private var estActif = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!estActif) return
            if (!premierEnvoi) {
                envoyerPosition(location)
                dernierEnvoi = location
                premierEnvoi = true
                return
            }
            val dernier = dernierEnvoi
            if (dernier == null || location.distanceTo(dernier) >= 10) {
                envoyerPosition(location)
                dernierEnvoi = location
            }
        }
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String) {}
        override fun onProviderDisabled(p0: String) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("commande") ?: return START_NOT_STICKY
        numeroDest = intent?.getStringExtra("numero_dest") ?: ""
        when (cmd) {
            "START" -> demarrerSuivi()
            "STOP" -> arreterSuivi()
        }
        return START_STICKY
    }

    private fun demarrerSuivi() {
        if (estActif) return
        creerCanalNotification()
        startForeground(1, creerNotificationSilencieuse())
        premierEnvoi = false
        dernierEnvoi = null
        estActif = true
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 10f, locationListener
            )
            Log.d("BloomGPS", "✅ Suivi actif — SMS DATA vers $numeroDest")
        } catch (e: SecurityException) {
            Log.e("BloomGPS", "❌ Permission GPS", e)
            stopSelf()
        }
    }

    private fun envoyerPosition(location: Location) {
        if (numeroDest.isEmpty()) return
        val message = "BLOOMGPS:${location.latitude},${location.longitude},${location.speed}"
        try {
            SmsManager.getDefault().sendDataMessage(
                numeroDest, null, PORT,
                message.toByteArray(Charsets.UTF_8),
                null, null
            )
            Log.d("BloomGPS", "✅ SMS DATA envoyé — invisible")
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ Échec envoi", e)
        }
    }

    private fun arreterSuivi() {
        estActif = false
        try { locationManager?.removeUpdates(locationListener) } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("BloomGPS", "🛑 Suivi arrêté")
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Bloom GPS", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setSound(null, null)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun creerNotificationSilencieuse(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Bloom GPS")
            .setContentText("Suivi en arrière-plan")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .setVibrate(longArrayOf(0))
            .setSound(null)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
