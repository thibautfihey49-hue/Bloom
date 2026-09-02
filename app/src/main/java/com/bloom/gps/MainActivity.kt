package com.bloom.gps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var etMonNumero: EditText
    private lateinit var etAutreNumero: EditText
    private lateinit var tvMonStatut: TextView
    private lateinit var tvAutreStatut: TextView
    private lateinit var btnStartLocal: Button
    private lateinit var btnStopLocal: Button
    private lateinit var btnStartRemote: Button
    private lateinit var btnStopRemote: Button
    
    private var monMarqueur: Marker? = null
    private var autreMarqueur: Marker? = null

    private val maPositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra(LocationTrackerService.EXTRA_LAT, 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra(LocationTrackerService.EXTRA_LON, 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) mettreAJourMaPosition(lat, lon)
        }
    }

    private val autrePositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) mettreAJourAutrePosition(lat, lon)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        etMonNumero = findViewById(R.id.etMonNumero)
        etAutreNumero = findViewById(R.id.etAutreNumero)
        tvMonStatut = findViewById(R.id.tvMonStatut)
        tvAutreStatut = findViewById(R.id.tvAutreStatut)
        btnStartLocal = findViewById(R.id.btnStartLocal)
        btnStopLocal = findViewById(R.id.btnStopLocal)
        btnStartRemote = findViewById(R.id.btnStartRemote)
        btnStopRemote = findViewById(R.id.btnStopRemote)
        mapView = findViewById(R.id.mapView)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5632))

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etMonNumero.setText(prefs.getString("MON_NUMERO", ""))
        etAutreNumero.setText(prefs.getString("NUMERO_AUTRE", ""))

        etAutreNumero.setOnFocusChangeListener { _, _ ->
            prefs.edit().putString("NUMERO_AUTRE", etAutreNumero.text.toString().trim()).apply()
        }

        btnStartLocal.setOnClickListener { demarrerSuiviLocal() }
        btnStopLocal.setOnClickListener { arreterSuiviLocal() }
        btnStartRemote.setOnClickListener { demarrerSuiviADistance() }
        btnStopRemote.setOnClickListener { arreterSuiviADistance() }

        registerReceiver(maPositionReceiver, IntentFilter(LocationTrackerService.ACTION_POSITION_REÇUE))
        registerReceiver(autrePositionReceiver, IntentFilter("com.bloom.gps.AUTRE_POSITION"))
        demanderPermissions()
    }

    private fun demanderPermissions() {
        val perms = arrayOf(
            Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, perms, 100)
    }

    private fun demarrerSuiviLocal() {
        val numAutre = etAutreNumero.text.toString().trim()
        if (numAutre.isEmpty()) {
            Toast.makeText(this, "⚠️ Entre d'abord le numéro de l'autre !", Toast.LENGTH_SHORT).show()
            return
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("NUMERO_AUTRE", numAutre).apply()
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_DEMARRER
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        Toast.makeText(this, "▶️ SUIVI DÉMARRÉ", Toast.LENGTH_SHORT).show()
        tvMonStatut.text = "📍 Moi: ✅ ENVOI EN COURS"
        tvMonStatut.setTextColor(0xFF10B981.toInt())
    }

    private fun arreterSuiviLocal() {
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_ARRETER
        }
        startService(intent)
        Toast.makeText(this, "⏹️ SUIVI ARRÊTÉ", Toast.LENGTH_SHORT).show()
        tvMonStatut.text = "📍 Moi: — ARRÊTÉ"
        tvMonStatut.setTextColor(0xFFEF4444.toInt())
    }

    private fun demarrerSuiviADistance() {
        val numAutre = etAutreNumero.text.toString().trim()
        if (numAutre.isEmpty()) { Toast.makeText(this, "⚠️ Numéro de l'autre manquant !", Toast.LENGTH_SHORT).show(); return }
        SmsManager.getDefault().sendTextMessage(numAutre, null, "DEMARRE_SUIVI", null, null)
        Toast.makeText(this, "📲 Commande DÉMARRER envoyée", Toast.LENGTH_SHORT).show()
    }

    private fun arreterSuiviADistance() {
        val numAutre = etAutreNumero.text.toString().trim()
        if (numAutre.isEmpty()) { Toast.makeText(this, "⚠️ Numéro de l'autre manquant !", Toast.LENGTH_SHORT).show(); return }
        SmsManager.getDefault().sendTextMessage(numAutre, null, "ARRETE_SUIVI", null, null)
        Toast.makeText(this, "📲 Commande ARRÊTER envoyée", Toast.LENGTH_SHORT).show()
    }

    private fun mettreAJourMaPosition(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        if (monMarqueur == null) {
            monMarqueur = Marker(mapView).apply {
                position = point; title = "📍 MOI"; snippet = "$lat, $lon"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
            }
            mapView.overlays.add(monMarqueur)
        } else monMarqueur!!.position = point
        mapView.controller.setCenter(point)
        tvMonStatut.text = "📍 Moi: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
        mapView.invalidate()
    }

    private fun mettreAJourAutrePosition(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        if (autreMarqueur == null) {
            autreMarqueur = Marker(mapView).apply {
                position = point; title = "📍 AUTRE"; snippet = "$lat, $lon"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
            }
            mapView.overlays.add(autreMarqueur)
        } else autreMarqueur!!.position = point
        tvAutreStatut.text = "📍 Autre: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
        tvAutreStatut.setTextColor(0xFF6366F1.toInt())
        mapView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(maPositionReceiver)
        unregisterReceiver(autrePositionReceiver)
    }
}
