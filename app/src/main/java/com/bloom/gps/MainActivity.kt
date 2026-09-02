package com.bloom.gps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    
    private lateinit var mapView: MapView
    private lateinit var tvStatut: TextView
    private lateinit var btnPermissions: Button
    private val PERMISSIONS_DEMANDEES = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        
        setContentView(R.layout.activity_main)

        tvStatut = findViewById(R.id.tvStatut)
        btnPermissions = findViewById(R.id.btnPermissions)
        mapView = findViewById(R.id.mapView)

        configurerCarte()

        btnPermissions.setOnClickListener { demanderPermissions() }

        if (verifierPermissions()) {
            tvStatut.text = "✅ Permissions OK — Carte prête"
            btnPermissions.text = "✅ Permissions OK"
            btnPermissions.isEnabled = false
        } else {
            tvStatut.text = "⚠️ Permissions manquantes"
        }
    }

    private fun configurerCarte() {
        try {
            tvStatut.text = "🔄 Chargement de la carte..."
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.isTilesScaledToDpi = true
            mapView.controller.setCenter(GeoPoint(47.4784, -0.5632))
            mapView.controller.setZoom(12.0)
            tvStatut.text = "✅ Carte chargée !"
        } catch (e: Exception) {
            tvStatut.text = "❌ Erreur: ${e.message}"
        }
    }

    private fun verifierPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun demanderPermissions() {
        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, perms, PERMISSIONS_DEMANDEES)
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        if (r == PERMISSIONS_DEMANDEES && verifierPermissions()) {
            tvStatut.text = "✅ Permissions OK — Carte OK"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}
