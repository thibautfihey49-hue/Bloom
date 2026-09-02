package com.bloom.parental

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat

class LocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var destinataire = ""

    companion object {
        fun demanderPosition(contexte: Context, numeroParent: String) {
            val intent = Intent(contexte, LocationService::class.java)
            intent.putExtra("numParent", numeroParent)
            contexte.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        destinataire = intent?.getStringExtra("numParent") ?: ""

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 1f, this)
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000, 1f, this)

            val dernierePos = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            dernierePos?.let { envoyerPosition(it) }
        }
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        envoyerPosition(location)
        stopSelf()
    }

    private fun envoyerPosition(pos: Location) {
        val sms = SmsManager.getDefault()
        val message = "BLOOM_POS:${pos.latitude},${pos.longitude}"
        try {
            sms.sendTextMessage(destinataire, null, message, null, null)
            Log.d("BLOOM-GPS", "Position envoyée : $message")
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "Erreur SMS : ${e.message}")
        }
        stopSelf()
    }

    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}
}
