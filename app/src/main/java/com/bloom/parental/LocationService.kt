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
    private lateinit var lm: LocationManager
    private var destinataire = ""

    companion object {
        fun demanderPosition(c: Context, numParent: String) {
            val i = Intent(c, LocationService::class.java)
            i.putExtra("numParent", numParent)
            c.startService(i)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        destinataire = i?.getStringExtra("numParent") ?: ""
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, this)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, this)
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { envoyer(it) }
        }
        return START_NOT_STICKY
    }

    override fun onLocationChanged(loc: Location) {
        envoyer(loc)
        stopSelf()
    }

    private fun envoyer(loc: Location) {
        val sms = SmsManager.getDefault()
        val msg = "BLOOM_POS:${loc.latitude},${loc.longitude}"
        try {
            sms.sendTextMessage(destinataire, null, msg, null, null)
            Log.d("BLOOM-GPS", "✅ Position envoyée : $msg")
        } catch (e: Exception) {
            Log.e("BLOOM-GPS", "❌ Erreur : ${e.message}")
        }
        stopSelf()
    }

    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}
}
