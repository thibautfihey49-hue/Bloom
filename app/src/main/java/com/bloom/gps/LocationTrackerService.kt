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
    private var premierEnvoiFait = false
    private val PORT_BLOOM = 50006 // 📡 Port dédié SMS de données

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
        premierEnvoiFait = false
        dernierEnvoi = null
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,           // ⚡ AUCUN délai — dès que 10m
                10f,         // ⚡ 10 MÈTRES EXACTEMENT
                locationListener
            )
            Log.d("BloomGPS", "✅ Suivi démarré — SMS de DONNÉES sur port $PORT_BLOOM")
        } catch (e: SecurityException) {
            Log.e("BloomGPS", "❌ Permissions GPS manquantes", e)
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

        // 📤 1er envoi IMMÉDIAT, puis toutes les 10m
        if (!premierEnvoiFait) {
            envoyerParDataSMS("BLOOMGPS:${location.latitude},${location.longitude},${location.speed}")
            dernierEnvoi = location
            premierEnvoiFait = true
            Log.d("BloomGPS", "📤 1er ENVOI — SMS de DONNÉES")
            return
        }

        val dernier = dernierEnvoi
        if (dernier != null && location.distanceTo(dernier) >= 10) {
            envoyerParDataSMS("BLOOMGPS:${location.latitude},${location.longitude},${location.speed}")
            dernierEnvoi = location
            Log.d("BloomGPS", "📤 +10m → SMS de DONNÉES envoyé")
        }
    }

    private fun envoyerParDataSMS(contenu: String) {
        if (numeroDest.isEmpty()) return
        try {
            val smsManager = SmsManager.getDefault()
            val donnees = contenu.toByteArray(Charsets.UTF_8)
            // 📡 ENVOI EN SMS DE DONNÉES — JAMAIS visible !
            smsManager.sendDataMessage(numeroDest, null, PORT_BLOOM.toShort(), donnees, null, null)
            Log.d("BloomGPS", "✅ SMS DE DONNÉES envoyé à $numeroDest")
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ Erreur envoi SMS de données", e)
        }
    }

    fun envoyerCommandeDistante(numero: String, commande: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val contenu = "BLOOMGPS_CMD:$commande"
            val donnees = contenu.toByteArray(Charsets.UTF_8)
            smsManager.sendDataMessage(numero, null, PORT_BLOOM.toShort(), donnees, null, null)
            Log.d("BloomGPS", "✅ Commande $commande envoyée à $numero")
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ Erreur envoi commande", e)
        }
    }

    private fun arreterSuivi() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("BloomGPS", "🛑 Suivi arrêté")
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
            .setContentText("Suivi actif — SMS de données 10m")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
