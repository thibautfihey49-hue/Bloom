package com.bloom.gps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {
    
    private lateinit var mapView: MapView
    private lateinit var tvStatut: TextView
    private lateinit var tvMoi: TextView
    private lateinit var tvLui: TextView
    private lateinit var etMonNumero: EditText
    private lateinit var etAutreNumero: EditText
    private lateinit var btnPermissions: Button
    private lateinit var btnDemander: Button
    private lateinit var btnRepondre: Button
    private lateinit var btnSuivre: Button
    
    private var marqueurMoi: Marker? = null
    private var marqueurLui: Marker? = null
    
    private val PERMISSIONS = 1001
    private var reponseAutoActivee = false

    private val maPositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra(LocationService.EXTRA_LAT, 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra(LocationService.EXTRA_LON, 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) mettreAJourMoi(lat, lon)
        }
    }

    private val autrePositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) mettreAJourLui(lat, lon)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        
        setContentView(R.layout.activity_main)
        initialiserVues()
        chargerNumeros()
        configurerCarte()
        configurerBoutons()
        enregistrerReceveurs()
        verifierPermissions()
    }

    private fun initialiserVues() {
        mapView = findViewById(R.id.mapView)
        tvStatut = findViewById(R.id.tvStatut)
        tvMoi = findViewById(R.id.tvMoi)
        tvLui = findViewById(R.id.tvLui)
        etMonNumero = findViewById(R.id.etMonNumero)
        etAutreNumero = findViewById(R.id.etAutreNumero)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnDemander = findViewById(R.id.btnDemander)
        btnRepondre = findViewById(R.id.btnRepondre)
        btnSuivre = findViewById(R.id.btnSuivre)
    }

    private fun chargerNumeros() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etMonNumero.setText(prefs.getString("MON_NUMERO", ""))
        etAutreNumero.setText(prefs.getString("NUMERO_AUTRE", ""))
        reponseAutoActivee = prefs.getBoolean("REPONSE_AUTO", false)
        mettreAJourBoutonSuivre()
    }

    private fun configurerCarte() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5632))
        tvStatut.text = "✅ Carte prête — entrez les numéros"
    }

    private fun configurerBoutons() {
        btnPermissions.setOnClickListener { demanderPermissions() }
        
        // 📥 DEMANDER SA POSITION — ENVOIE "DEMANDE:" À LUI
        btnDemander.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entre d'abord le numéro de l'autre !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("NUMERO_AUTRE", num).apply()
            
            try {
                SmsManager.getDefault().sendTextMessage(num, null, "DEMANDE:", null, null)
                Toast.makeText(this, "📥 DEMANDE ENVOYÉE ! Attends sa position...", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ Demande envoyée — en attente de sa position..."
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 📤 RÉPONDRE — ENVOIE MA POSITION UNE FOIS
        btnRepondre.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entre d'abord le numéro de l'autre !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("NUMERO_AUTRE", num).apply()
            
            val intent = Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_ENVOYER_UNE_FOIS
                putExtra(LocationService.EXTRA_NUMERO_CIBLE, num)
            }
            startForegroundService(intent)
            Toast.makeText(this, "📤 POSITION ENVOYÉE !", Toast.LENGTH_SHORT).show()
            tvStatut.text = "✅ Position envoyée à l'autre"
        }

        // 🔄 RÉPONSE AUTOMATIQUE — IL DEMANDE → TU RÉPONDS TOUT SEUL
        btnSuivre.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entre d'abord le numéro de l'autre !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("NUMERO_AUTRE", num).apply()
            
            reponseAutoActivee = !reponseAutoActivee
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putBoolean("REPONSE_AUTO", reponseAutoActivee).apply()
            
            mettreAJourBoutonSuivre()
            
            if (reponseAutoActivee) {
                Toast.makeText(this, "🔄 RÉPONSE AUTOMATIQUE ACTIVÉE ! Il demande → tu réponds TOUT SEUL", Toast.LENGTH_LONG).show()
                tvStatut.text = "✅ Réponse auto ACTIVE — il demande, tu envoies"
            } else {
                Toast.makeText(this, "🛑 Réponse automatique désactivée", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ Réponse auto désactivée"
            }
        }
    }

    private fun mettreAJourBoutonSuivre() {
        if (reponseAutoActivee) {
            btnSuivre.text = "🔄 ACTIF"
            btnSuivre.setBackgroundColor(Color.parseColor("#10B981"))
        } else {
            btnSuivre.text = "🔄 Réponse auto"
            btnSuivre.setBackgroundColor(Color.parseColor("#F59E0B"))
        }
    }

    private fun enregistrerReceveurs() {
        registerReceiver(maPositionReceiver, IntentFilter(LocationService.ACTION_MA_POSITION), RECEIVER_NOT_EXPORTED)
        registerReceiver(autrePositionReceiver, IntentFilter("com.bloom.gps.AUTRE_POSITION"), RECEIVER_NOT_EXPORTED)
    }

    private fun verifierPermissions() {
        val a = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val b = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (a == PackageManager.PERMISSION_GRANTED && b == PackageManager.PERMISSION_GRANTED) {
            tvStatut.text = "✅ Permissions OK — Prêt"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
        }
    }

    private fun demanderPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, perms, PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            tvStatut.text = "✅ TOUT EST PRÊT !"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            Toast.makeText(this, "🎉 Permissions accordées !", Toast.LENGTH_SHORT).show()
        } else {
            tvStatut.text = "❌ Permissions refusées"
            Toast.makeText(this, "Sans permissions, l'application ne fonctionnera pas", Toast.LENGTH_LONG).show()
        }
    }

    private fun mettreAJourMoi(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        
        if (marqueurMoi == null) {
            marqueurMoi = Marker(mapView).apply {
                position = point
                title = "📍 MOI"
                snippet = "$lat, $lon"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
            }
            mapView.overlays.add(marqueurMoi)
        } else {
            marqueurMoi!!.position = point
        }
        
        tvMoi.text = "🔵 MOI: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
        mapView.controller.setCenter(point)
        mapView.invalidate()
    }

    private fun mettreAJourLui(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        
        if (marqueurLui == null) {
            marqueurLui = Marker(mapView).apply {
                position = point
                title = "📍 LUI"
                snippet = "$lat, $lon"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
            }
            mapView.overlays.add(marqueurLui)
        } else {
            marqueurLui!!.position = point
        }
        
        tvLui.text = "🔴 LUI: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
        tvStatut.text = "✅ Position de l'autre reçue !"
        mapView.invalidate()
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(maPositionReceiver)
        unregisterReceiver(autrePositionReceiver)
    }
}
