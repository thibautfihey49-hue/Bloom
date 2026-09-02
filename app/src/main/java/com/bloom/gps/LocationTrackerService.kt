package com.bloom.gps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

class LocationTrackerService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var numeroAutre = ""
    private var dernierePosition: Location? = null
    private var serviceDemarre = false

    companion object {
        const val ACTION_DEMARRER = "com.bloom.gps.DEMARRER_SUIVI"
        const val ACTION_ARRETER = "com.bloom.gps.ARRETER_SUIVI"
        const val ACTION_POSITION_REÇUE = "com.bloom.gps.POSITION_REÇUE"
        const val EXTRA_LAT = "latitude"
        const val EXTRA_LON = "longitude"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        creerCanalNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DEMARRER -> demarrerSuivi()
            ACTION_ARRETER -> arreterSuivi()
        }
        return START_STICKY
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel("BLOOM_GPS", "Bloom GPS", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun demarrerSuivi() {
        if (serviceDemarre) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        numeroAutre = prefs.getString("NUMERO_AUTRE", "") ?: ""
        
        if (numeroAutre.isEmpty()) {
            Log.e("BLOOM-GPS", "⚠️ Numéro de l'autre non configuré !")
            stopSelf()
            return
        }

        startForeground(1001, creerNotification())
        serviceDemarre = true

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLOOM-GPS", "⚠️ Permission localisation manquante")
            stopSelf()
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10f, this)
        Log.d("BLOOM-GPS", "✅ SUIVI DÉMARRÉ — Envoi à : $numeroAutre")
    }

    private fun arreterSuivi() {
        if (!serviceDemarre) return
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        serviceDemarre = false
        dernierePosition = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("BLOOM-GPS", "🛑 SUIVI ARRÊTÉ")
        stopSelf()
    }

    private fun creerNotification(): Notification {
        return Notification.Builder(this, "BLOOM_GPS")
            .setContentTitle("📍 Suivi GPS en cours")
            .setContentText("Envoi de votre position toutes les 10m...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onLocationChanged(nouvellePosition: Location) {
        dernierePosition = nouvellePosition
        val message = "POS:${nouvellePosition.latitude},${nouvellePosition.longitude}"
        try {
            SmsManager.getDefault().sendTextMessage(numeroAutre, null, message, null, null)
            Log.d("BLOOM-GPS", "📤 Position envoyée : $message")
            
            val intent = Intent(ACTION_POSITION_REÇUE)
            intent.setPackage(packageName)
            intent.putExtra(EXTRA_LAT, nouvellePosition.latitude)
            intent.putExtra(EXTRA_LON, nouvellePosition.longitude)
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "❌ Erreur envoi SMS: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arreterSuivi()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
