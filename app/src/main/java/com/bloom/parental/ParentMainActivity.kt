package com.bloom.parental

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.telephony.SmsManager
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
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
            tvDemandeTemps.visibility = View.VISIBLE
            tvDemandeTemps.text = "⏳ \"Je peux avoir plus de temps ?\""
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
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        // ✅ Layout dynamique moderne
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }
        
        // 📌 Titre
        val titre = TextView(this).apply {
            text = "🌸 Bloom — Espace Parent"
            textSize = 28f
            setTextColor(0xFF6366F1.toInt())
            setPadding(0, 0, 0, 40)
        }
        container.addView(titre)
        
        // 📌 Statut
        tvStatut = TextView(this).apply {
            text = "✅ Connecté — Surveillance active"
            textSize = 16f
            setTextColor(0xFF10B981.toInt())
            setPadding(0, 0, 0, 30)
        }
        container.addView(tvStatut)
        
        // 📌 Demande de temps (affichée EN CLAIR)
        tvDemandeTemps = TextView(this).apply {
            visibility = View.GONE
            text = "⏳ \"Je peux avoir plus de temps ?\""
            textSize = 18f
            setBackgroundColor(0xFFFEF3C7.toInt())
            setTextColor(0xFF92400E.toInt())
            setPadding(24, 16, 24, 16)
            setTextIsSelectable(true)
            setPadding(0, 0, 0, 30)
        }
        container.addView(tvDemandeTemps)
        
        // 📌 Bouton : Verrouiller
        btnVerrouiller = Button(this).apply {
            text = "🔒 VERROUILLER L'APPAREIL"
            textSize = 16f
            setBackgroundColor(0xFFEF4444.toInt())
            setTextColor(Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { verrouillerAppareil() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        container.addView(btnVerrouiller)
        
        // 📌 Bouton : Déverrouiller
        btnDeverrouiller = Button(this).apply {
            text = "🔓 DÉVERROUILLER L'APPAREIL"
            textSize = 16f
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { deverrouillerAppareil() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        container.addView(btnDeverrouiller)
        
        // 📌 Bouton : Obtenir position
        btnPosition = Button(this).apply {
            text = "📍 OBTENIR LA POSITION"
            textSize = 16f
            setBackgroundColor(0xFF6366F1.toInt())
            setTextColor(Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { demanderPosition() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }
        }
        container.addView(btnPosition)
        
        // 📌 Carte
        val carteTitre = TextView(this).apply {
            text = "🗺️ Position de l'enfant"
            textSize = 20f
            setTextColor(0xFF374151.toInt())
            setPadding(0, 0, 0, 16)
        }
        container.addView(carteTitre)
        
        mapView = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600
            )
            setBackgroundColor(0xFFE5E7EB.toInt())
        }
        container.addView(mapView)
        
        scroll.addView(container)
        setContentView(scroll)
        
        // 📍 Initialiser la carte
        val controller: IMapController = mapView.controller
        controller.setZoom(15.0)
        controller.setCenter(GeoPoint(47.4784, -0.5632)) // Angers par défaut
        
        // 📡 Enregistrer les récepteurs
        registerReceiver(demandeTempsReceiver, IntentFilter("com.bloom.parental.AFFICHER_DEMANDE_TEMPS"))
        registerReceiver(positionReceiver, IntentFilter("com.bloom.parental.METTRE_A_JOUR_POSITION"))
        
        // 📋 Demander numéro parent si pas encore fait
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getString("NUMERO_PARENT", null) == null) {
            Toast.makeText(this, "⚠️ Définis ton numéro dans les paramètres", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun verrouillerAppareil() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isNotEmpty()) {
            SmsManager.getDefault().sendTextMessage(numeroParent, null, "VERROUILLER", null, null)
            Toast.makeText(this, "🔒 Ordre de verrouillage envoyé", Toast.LENGTH_SHORT).show()
        }
        tvStatut.text = "🔒 Appareil verrouillé"
        tvStatut.setTextColor(0xFFEF4444.toInt())
    }
    
    private fun deverrouillerAppareil() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isNotEmpty()) {
            SmsManager.getDefault().sendTextMessage(numeroParent, null, "DEVERROUILLER", null, null)
            Toast.makeText(this, "🔓 Ordre de déverrouillage envoyé", Toast.LENGTH_SHORT).show()
        }
        tvStatut.text = "✅ Appareil déverrouillé — Surveillance active"
        tvStatut.setTextColor(0xFF10B981.toInt())
    }
    
    private fun demanderPosition() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isNotEmpty()) {
            SmsManager.getDefault().sendTextMessage(numeroParent, null, "POSITION", null, null)
            Toast.makeText(this, "📍 Position demandée...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun mettreAJourPosition(lat: Double, lon: Double) {
        positionActuelle = GeoPoint(lat, lon)
        mapView.overlays.clear()
        
        // 📍 Marqueur sur la carte
        val marqueur = Marker(mapView).apply {
            position = positionActuelle
            title = "Position de l'enfant"
            snippet = "$lat, $lon"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@ParentMainActivity, android.R.drawable.ic_menu_mylocation)
        }
        mapView.overlays.add(marqueur)
        
        // 🎯 Centrer la carte
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
