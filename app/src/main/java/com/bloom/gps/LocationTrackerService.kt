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
    private val PORT_BLOOM = 50006

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
        
        Log.d("BloomGPS", "========================================")
        Log.d("BloomGPS", "🔧 SERVICE DÉMARRÉ")
        Log.d("BloomGPS", "🔧 Commande : $cmd")
        Log.d("BloomGPS", "🔧 Destinataire : $numeroDest")
        Log.d("BloomGPS", "========================================")
        
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
                0,
                10f,
                locationListener
            )
            Log.d("BloomGPS", "✅ ✅ SUIVI GPS ACTIF — Envoi toutes les 10m")
        } catch (e: SecurityException) {
            Log.e("BloomGPS", "❌ Permissions GPS manquantes", e)
            stopSelf()
        }
    }

    private fun traiterNouvellePosition(location: Location) {
        if (!premierEnvoiFait) {
            Log.d("BloomGPS", "📍 Première position : ${location.latitude}, ${location.longitude}")
            envoyerPosition(location)
            dernierEnvoi = location
            premierEnvoiFait = true
            return
        }

        val dernier = dernierEnvoi
        if (dernier != null && location.distanceTo(dernier) >= 10) {
            Log.d("BloomGPS", "📍 +10m — Envoi position...")
            envoyerPosition(location)
            dernierEnvoi = location
        }
    }

    private fun envoyerPosition(location: Location) {
        if (numeroDest.isEmpty()) {
            Log.w("BloomGPS", "⚠️ Aucun numéro de destination !")
            return
        }
        
        val contenu = "BLOOMGPS:${location.latitude},${location.longitude},${location.speed}"
        val smsManager = SmsManager.getDefault()
        var succes = false
        
        Log.d("BloomGPS", "📤 Envoi position à $numeroDest")
        
        // 📡 SMS de données
        try {
            val donnees = contenu.toByteArray(Charsets.UTF_8)
            smsManager.sendDataMessage(numeroDest, null, PORT_BLOOM.toShort(), donnees, null, null)
            Log.d("BloomGPS", "✅ SMS DE DONNÉES envoyé")
            succes = true
        } catch (e: Exception) {
            Log.w("BloomGPS", "⚠️ SMS de données échoué", e)
        }
        
        // 📩 SMS TEXTE de SECOURS
        try {
            smsManager.sendTextMessage(numeroDest, null, contenu, null, null)
            Log.d("BloomGPS", "✅ SMS TEXTE de SECOURS envoyé")
            succes = true
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ ÉCHEC envoi position", e)
        }
    }

    private fun arreterSuivi() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("BloomGPS", "🛑 SUIVI ARRÊTÉ")
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
            .setContentText("Suivi actif — Envoi toutes les 10m")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
