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
    private val CHANNEL_ID = "bloom_gps_channel"
    private var locationManager: LocationManager? = null
    private var dernierEnvoi: Location? = null
    private var numeroDest: String = ""
    private val NOTIFICATION_ID = 12345

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            traiterNouvellePosition(location)
        }
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String) {}
        override fun onProviderDisabled(p0: String) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("commande") ?: return START_NOT_STICKY
        numeroDest = intent.getStringExtra("numero_dest") ?: ""
        
        when (cmd) {
            "START" -> demarrerSuivi()
            "STOP" -> arreterSuivi()
        }
        return START_STICKY
    }

    private fun demarrerSuivi() {
        creerCanalNotification()
        startForeground(NOTIFICATION_ID, creerNotification())
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000,
                10f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("BloomGPS", "Permissions GPS manquantes", e)
            stopSelf()
        }
    }

    private fun traiterNouvellePosition(location: Location) {
        val updateIntent = Intent("BLOOMGPS_UPDATE").apply {
            putExtra("lat", location.latitude)
            putExtra("lon", location.longitude)
            putExtra("speed", location.speed)
        }
        sendBroadcast(updateIntent)

        val dernier = dernierEnvoi
        if (dernier == null || location.distanceTo(dernier) >= 10) {
            envoyerPositionParSMS(location)
            dernierEnvoi = location
        }
    }

    private fun envoyerPositionParSMS(location: Location) {
        if (numeroDest.isEmpty()) return
        try {
            val smsManager = SmsManager.getDefault()
            val message = "BLOOMGPS:${location.latitude},${location.longitude},${location.speed}"
            smsManager.sendTextMessage(numeroDest, null, message, null, null)
            Log.d("BloomGPS", "Position envoyée : $message")
        } catch (e: Exception) {
            Log.e("BloomGPS", "Erreur envoi SMS", e)
        }
    }

    private fun arreterSuivi() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloom GPS — Suivi en cours",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service de localisation"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun creerNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Bloom GPS")
            .setContentText("Suivi de position actif")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
