package com.bloom.parental.service

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocationData(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val time: Long
)

class LocationMonitorService : Service(), LocationListener {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val CHANNEL_ID = "bloom_location"
    private var locationManager: LocationManager? = null
    private var lastLocation: LocationData? = null

    companion object {
        private val _currentLocation = MutableStateFlow<LocationData?>(null)
        val currentLocation: StateFlow<LocationData?> = _currentLocation

        fun start(context: Context) {
            val intent = Intent(context, LocationMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(3, createNotification())
            initLocation()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        locationManager?.removeUpdates(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Géolocalisation", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Suivi de position en arrière-plan"
                    setShowBadge(false); enableVibration(false); setSound(null, null)
                }
            )
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Bloom")
                .setContentText("Géolocalisation active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Bloom")
                .setContentText("Géolocalisation active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true).build()
        }
    }

    private fun initLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 50f, this)
        } catch (e: SecurityException) {}
        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300000, 50f, this)
        } catch (e: SecurityException) {}
    }

    override fun onLocationChanged(location: Location) {
        val data = LocationData(location.latitude, location.longitude, location.accuracy, System.currentTimeMillis())
        lastLocation = data
        _currentLocation.value = data
        BloomSmsManager.sendLocation(this, location.latitude, location.longitude, location.accuracy.toInt())
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
