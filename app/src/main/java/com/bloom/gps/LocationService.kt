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
    private var numeroCible = ""
    private var envoiContinu = false
    private var uneFoisEnvoye = false

    companion object {
        const val ACTION_DEMARRER_SUIVI = "com.bloom.gps.DEMARRER_SUIVI"
        const val ACTION_ARRETER = "com.bloom.gps.ARRETER"
        const val ACTION_ENVOYER_UNE_FOIS = "com.bloom.gps.ENVOYER_UNE_FOIS"
        const val ACTION_MA_POSITION = "com.bloom.gps.MA_POSITION"
        const val EXTRA_LAT = "latitude"
        const val EXTRA_LON = "longitude"
        const val EXTRA_NUMERO_CIBLE = "NUMERO_CIBLE"
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
        numeroCible = intent?.getStringExtra(EXTRA_NUMERO_CIBLE) ?: ""
        
        when (intent?.action) {
            ACTION_ENVOYER_UNE_FOIS -> envoyerPositionUneFois()
            ACTION_DEMARRER_SUIVI -> demarrerSuiviContinu()
            ACTION_ARRETER -> arreterTout()
        }
        return START_STICKY
    }

    private fun envoyerPositionUneFois() {
        Log.d("BLOOM-GPS", "📤 ENVOI UNE FOIS À $numeroCible")
        
        if (numeroCible.isEmpty()) {
            Log.e("BLOOM-GPS", "⚠️ Aucun numéro cible !")
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

        // 🎯 Récupérer la DERNIÈRE position connue immédiatement
        val dernierePosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (dernierePosition != null) {
            envoyerSMS(dernierePosition.latitude, dernierePosition.longitude)
        } else {
            Log.d("BLOOM-GPS", "⏳ Attente position GPS...")
            uneFoisEnvoye = false
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
            startForeground(1001, creerNotificationDiscrete())
            return
        }
        
        stopSelf()
    }

    private fun demarrerSuiviContinu() {
        if (envoiContinu) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        numeroCible = prefs.getString("NUMERO_AUTRE", "") ?: ""
        
        if (numeroCible.isEmpty()) {
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
        envoiContinu = true
        
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 15000, 20f, this
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 15000, 20f, this
        )
        
        Log.d("BLOOM-GPS", "✅ SUIVI CONTINU DÉMARRÉ → $numeroCible")
    }

    private fun arreterTout() {
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        envoiContinu = false
        uneFoisEnvoye = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("BLOOM-GPS", "🛑 TOUT ARRÊTÉ")
        stopSelf()
    }

    private fun creerNotificationDiscrete(): Notification {
        return Notification.Builder(this, "BLOOM_GPS")
            .setContentTitle("📍 Bloom GPS")
            .setContentText("Envoi position en arrière-plan")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun envoyerSMS(lat: Double, lon: Double) {
        val message = "POS:$lat,$lon"
        try {
            SmsManager.getDefault().sendTextMessage(numeroCible, null, message, null, null)
            Log.d("BLOOM-GPS", "📤 ENVOYÉ INVISIBLE À $numeroCible : $message")

            // ✅ Mettre à jour MA position sur la carte
            val intent = Intent(ACTION_MA_POSITION)
            intent.setPackage(packageName)
            intent.putExtra(EXTRA_LAT, lat)
            intent.putExtra(EXTRA_LON, lon)
            sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "❌ Erreur envoi SMS: ${e.message}")
        }
    }

    override fun onLocationChanged(nouvellePosition: Location) {
        if (!uneFoisEnvoye) {
            envoyerSMS(nouvellePosition.latitude, nouvellePosition.longitude)
            uneFoisEnvoye = true
            if (!envoiContinu) {
                arreterTout()
                return
            }
        }
        
        if (envoiContinu) {
            envoyerSMS(nouvellePosition.latitude, nouvellePosition.longitude)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arreterTout()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    override fun onProviderEnabled(p0: String) {}
    override fun onProviderDisabled(p0: String) {}
}
