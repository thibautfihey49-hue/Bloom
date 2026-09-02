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

class LocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var numeroAutre = ""
    private var envoiActif = false

    companion object {
        const val ACTION_DEMARRER = "com.bloom.gps.DEMARRER"
        const val ACTION_ARRETER = "com.bloom.gps.ARRETER"
        const val ACTION_MA_POSITION = "com.bloom.gps.MA_POSITION"
        const val EXTRA_LAT = "latitude"
        const val EXTRA_LON = "longitude"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        creerCanalNotification()
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel("BLOOM_GPS", "Bloom GPS", NotificationManager.IMPORTANCE_LOW)
            canal.setShowBadge(false)
            canal.enableVibration(false)
            canal.setSound(null, null)
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DEMARRER -> demarrerEnvoi()
            ACTION_ARRETER -> arreterEnvoi()
        }
        return START_STICKY
    }

    private fun demarrerEnvoi() {
        if (envoiActif) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        numeroAutre = prefs.getString("NUMERO_AUTRE", "") ?: ""
        
        if (numeroAutre.isEmpty()) {
            Log.e("BLOOM-GPS", "⚠️ Numéro de l'autre manquant !")
            stopSelf()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLOOM-GPS", "⚠️ Permission localisation manquante")
            stopSelf()
            return
        }

        startForeground(1001, creerNotificationDiscrete())
        envoiActif = true
        
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 10000, 10f, this
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 10000, 10f, this
        )
        
        Log.d("BLOOM-GPS", "✅ ENVOI INVISIBLE DÉMARRÉ → $numeroAutre")
    }

    private fun arreterEnvoi() {
        if (!envoiActif) return
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        envoiActif = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("BLOOM-GPS", "🛑 ENVOI ARRÊTÉ")
        stopSelf()
    }

    private fun creerNotificationDiscrete(): Notification {
        return Notification.Builder(this, "BLOOM_GPS")
            .setContentTitle("📍 Suivi actif")
            .setContentText("Envoi position en arrière-plan")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onLocationChanged(nouvellePosition: Location) {
        val message = "POS:${nouvellePosition.latitude},${nouvellePosition.longitude}"
        
        try {
            SmsManager.getDefault().sendTextMessage(
                numeroAutre, null, message, null, null
            )
            Log.d("BLOOM-GPS", "📤 ENVOYÉ INVISIBLE : $message")

            val intent = Intent(ACTION_MA_POSITION)
            intent.setPackage(packageName)
            intent.putExtra(EXTRA_LAT, nouvellePosition.latitude)
            intent.putExtra(EXTRA_LON, nouvellePosition.longitude)
            sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "❌ Erreur envoi: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arreterEnvoi()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    override fun onProviderEnabled(p0: String) {}
    override fun onProviderDisabled(p0: String) {}
}
