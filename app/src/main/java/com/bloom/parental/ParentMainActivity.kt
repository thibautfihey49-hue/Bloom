package com.bloom.parental

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.preference.PreferenceManager

class ParentMainActivity : AppCompatActivity() {
    private lateinit var btnVerrouiller: Button
    private lateinit var btnDeverrouiller: Button
    private lateinit var btnPosition: Button
    private lateinit var tvStatut: TextView
    private lateinit var tvDemandeTemps: TextView
    private lateinit var mapView: MapView
    private var positionActuelle: GeoPoint? = null
    
    private val demandeTempsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            tvDemandeTemps.visibility = android.view.View.VISIBLE
        }
    }
    
    private val positionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) {
                mettreAJourPosition(lat, lon)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_main)
        
        // ✅ Initialiser osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        // ✅ Lier les vues
        tvStatut = findViewById(R.id.tvStatut)
        tvDemandeTemps = findViewById(R.id.tvDemandeTemps)
        btnVerrouiller = findViewById(R.id.btnVerrouiller)
        btnDeverrouiller = findViewById(R.id.btnDeverrouiller)
        btnPosition = findViewById(R.id.btnPosition)
        mapView = findViewById(R.id.mapView)
        
        // ✅ Configurer la carte
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val controller: IMapController = mapView.controller
        controller.setZoom(15.0)
        controller.setCenter(GeoPoint(47.4784, -0.5632)) // Angers
        
        // ✅ Boutons
        btnVerrouiller.setOnClickListener { verrouillerAppareil() }
        btnDeverrouiller.setOnClickListener { deverrouillerAppareil() }
        btnPosition.setOnClickListener { demanderPosition() }
        
        // ✅ Récepteurs
        registerReceiver(demandeTempsReceiver, IntentFilter("com.bloom.parental.AFFICHER_DEMANDE_TEMPS"))
        registerReceiver(positionReceiver, IntentFilter("com.bloom.parental.METTRE_A_JOUR_POSITION"))
        
        // ✅ Vérifier permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }
    
    private fun verrouillerAppareil() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isEmpty()) {
            Toast.makeText(this, "⚠️ Définis ton numéro dans les paramètres d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        SmsManager.getDefault().sendTextMessage(numeroParent, null, "VERROUILLER", null, null)
        Toast.makeText(this, "🔒 Ordre de verrouillage envoyé", Toast.LENGTH_SHORT).show()
        tvStatut.text = "🔒 Appareil verrouillé"
        tvStatut.setTextColor(0xFFEF4444.toInt())
    }
    
    private fun deverrouillerAppareil() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isEmpty()) {
            Toast.makeText(this, "⚠️ Définis ton numéro dans les paramètres d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        SmsManager.getDefault().sendTextMessage(numeroParent, null, "DEVERROUILLER", null, null)
        Toast.makeText(this, "🔓 Ordre de déverrouillage envoyé", Toast.LENGTH_SHORT).show()
        tvStatut.text = "✅ Appareil déverrouillé — Surveillance active"
        tvStatut.setTextColor(0xFF10B981.toInt())
    }
    
    private fun demanderPosition() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isEmpty()) {
            Toast.makeText(this, "⚠️ Définis ton numéro dans les paramètres d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        SmsManager.getDefault().sendTextMessage(numeroParent, null, "POSITION", null, null)
        Toast.makeText(this, "📍 Position demandée...", Toast.LENGTH_SHORT).show()
    }
    
    private fun mettreAJourPosition(lat: Double, lon: Double) {
        positionActuelle = GeoPoint(lat, lon)
        mapView.overlays.clear()
        
        val marqueur = Marker(mapView).apply {
            position = positionActuelle
            title = "Position de l'enfant"
            snippet = "$lat, $lon"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marqueur)
        mapView.controller.setCenter(positionActuelle)
        mapView.invalidate()
        Toast.makeText(this, "📍 Position mise à jour !", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(demandeTempsReceiver)
        unregisterReceiver(positionReceiver)
    }
}
