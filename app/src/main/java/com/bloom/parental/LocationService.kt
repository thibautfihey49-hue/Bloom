package com.bloom.parental

import android.Manifest
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

class LocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var numeroDestinataire = ""

    companion object {
        fun demanderPosition(context: Context, numeroParent: String) {
            val intent = Intent(context, LocationService::class.java)
            intent.putExtra("numParent", numeroParent)
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        numeroDestinataire = intent?.getStringExtra("numParent") ?: ""
        initialiserServicePremierPlan()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        demanderMisesAJourPosition()
        return START_NOT_STICKY
    }

    private fun initialiserServicePremierPlan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "BLOOM_LOCATION",
                "Bloom — Localisation",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }

        val notification = Notification.Builder(this, "BLOOM_LOCATION")
            .setContentTitle("🌸 Bloom — Localisation en cours")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                startForeground(1002, notification, Service.FOREGROUND_SERVICE_TYPE_LOCATION)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                startForeground(1002, notification)
            }
        }
    }

    private fun demanderMisesAJourPosition() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 1f, this
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000, 1f, this
            )
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { envoyerPosition(it) }
        }
    }

    override fun onLocationChanged(localisation: Location) {
        envoyerPosition(localisation)
        stopSelf()
    }

    private fun envoyerPosition(localisation: Location) {
        val sms = SmsManager.getDefault()
        val message = "BLOOM_POS:${localisation.latitude},${localisation.longitude}"
        try {
            sms.sendTextMessage(numeroDestinataire, null, message, null, null)
            Log.d("BLOOM-GPS", "✅ Position envoyée : $message")
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "❌ Erreur envoi : ${e.message}")
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
    }

    override fun onStatusChanged(fournisseur: String?, statut: Int, extras: Bundle?) {}
    override fun onProviderEnabled(fournisseur: String) {}
    override fun onProviderDisabled(fournisseur: String) {}
}
