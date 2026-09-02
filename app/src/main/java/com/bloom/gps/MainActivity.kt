package com.bloom.gps

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), LocationListener {
    
    private lateinit var mapView: MapView
    private lateinit var tvStatut: TextView
    private lateinit var tvMoi: TextView
    private lateinit var tvLui: TextView
    private lateinit var tvDistance: TextView
    private lateinit var etMonNumero: EditText
    private lateinit var etAutreNumero: EditText
    private lateinit var btnPermissions: Button
    private lateinit var btnDemander: Button
    private lateinit var btnRepondre: Button
    private lateinit var btnSuivre: Button
    private lateinit var btnSuiviContinu: Button
    
    private var marqueurMoi: Marker? = null
    private var marqueurLui: Marker? = null
    private lateinit var locationManager: LocationManager
    
    private val PERMISSIONS_REQUEST = 1001
    private var reponseAutoActivee = false
    private var suiviContinuActif = false
    private var dernierePosition: Location? = null
    private var derniereDistanceEnvoi = 0f  // Compteur pour les 10 mètres

    private val maPositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
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
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
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
        tvDistance = findViewById(R.id.tvDistance)
        etMonNumero = findViewById(R.id.etMonNumero)
        etAutreNumero = findViewById(R.id.etAutreNumero)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnDemander = findViewById(R.id.btnDemander)
        btnRepondre = findViewById(R.id.btnRepondre)
        btnSuivre = findViewById(R.id.btnSuivre)
        btnSuiviContinu = findViewById(R.id.btnSuiviContinu)
    }

    private fun chargerNumeros() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etMonNumero.setText(prefs.getString("MON_NUM", ""))
        etAutreNumero.setText(prefs.getString("AUTRE_NUM", ""))
        reponseAutoActivee = prefs.getBoolean("REP_AUTO", false)
        suiviContinuActif = prefs.getBoolean("SUIVI_CONTINU", false)
        mettreAJourBoutons()
    }

    private fun configurerCarte() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5632))
        tvStatut.text = "✅ Prêt — entrez les numéros"
    }

    private fun demarrerGPS() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        // 📡 Mise à jour TOUTES LES 1 SECONDE ou TOUS LES 1 MÈTRE
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000, 1f, this
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 1000, 1f, this
        )
    }

    override fun onLocationChanged(nouvellePosition: Location) {
        mettreAJourMoi(nouvellePosition.latitude, nouvellePosition.longitude)
        
        // 📡 SUIVI CONTINU : Envoyer seulement si on a bougé de 10m
        if (suiviContinuActif && dernierePosition != null) {
            val distance = nouvellePosition.distanceTo(dernierePosition!!)
            derniereDistanceEnvoi += distance
            
            tvDistance.text = "📡 Déplacement: ${derniereDistanceEnvoi.roundToInt()}m / 10m"
            
            // ✅ Envoyer quand on a atteint 10 mètres
            if (derniereDistanceEnvoi >= 10f) {
                envoyerPositionSuiviContinu(nouvellePosition)
                derniereDistanceEnvoi = 0f  // Réinitialiser
            }
        } else if (dernierePosition == null) {
            // Première position — sauvegarder sans envoyer
            derniereDistanceEnvoi = 0f
        }
        
        dernierePosition = nouvellePosition
    }

    private fun configurerBoutons() {
        btnPermissions.setOnClickListener { demanderPermissions() }
        
        // 📥 DEMANDER SA POSITION UNE FOIS
        btnDemander.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro de l'abord !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("AUTRE_NUM", num).apply()
            
            try {
                SmsManager.getDefault().sendDataMessage(
                    num, null, 10001.toShort(),
                    "BLOOM_REQ".toByteArray(Charsets.UTF_8),
                    null, null
                )
                Toast.makeText(this, "📥 Demande envoyée !", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ En attente de sa position..."
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 📤 RÉPONDRE UNE FOIS
        btnRepondre.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro de l'abord !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("AUTRE_NUM", num).apply()
            
            envoyerMaPosition(num)
        }

        // 🔄 RÉPONSE AUX DEMANDES
        btnSuivre.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro de l'abord !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("AUTRE_NUM", num).apply()
            
            reponseAutoActivee = !reponseAutoActivee
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putBoolean("REP_AUTO", reponseAutoActivee).apply()
            
            mettreAJourBoutons()
            
            if (reponseAutoActivee) {
                Toast.makeText(this, "🔄 RÉPONSE AUTO ACTIVE — Il demande → tu réponds TOUT SEUL", Toast.LENGTH_LONG).show()
                tvStatut.text = "✅ Réponse auto ACTIVE"
            } else {
                Toast.makeText(this, "🛑 Réponse auto désactivée", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ Réponse auto désactivée"
            }
        }

        // 📡 SUIVI CONTINU — ENVOI TOUTES LES 10 MÈTRES
        btnSuiviContinu.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro de l'abord !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("AUTRE_NUM", num).apply()
            
            suiviContinuActif = !suiviContinuActif
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putBoolean("SUIVI_CONTINU", suiviContinuActif).apply()
            
            derniereDistanceEnvoi = 0f
            dernierePosition = null
            
            mettreAJourBoutons()
            
            if (suiviContinuActif) {
                AlertDialog.Builder(this)
                    .setTitle("📡 SUIVI CONTINU ACTIVÉ")
                    .setMessage("✅ Votre position sera envoyée automatiquement TOUTES LES 10 MÈTRES à :\n$num\n\n🕵️ Invisible — rien dans la messagerie !")
                    .setPositiveButton("OK") { _, _ -> }
                    .show()
                tvStatut.text = "📡 SUIVI CONTINU ACTIF — Envoi toutes les 10m"
                // 📡 Prévenir l'autre téléphone que le suivi commence
                try {
                    SmsManager.getDefault().sendDataMessage(
                        num, null, 10001.toShort(),
                        "BLOOM_START".toByteArray(Charsets.UTF_8),
                        null, null
                    )
                } catch (e: Exception) {}
            } else {
                Toast.makeText(this, "🛑 SUIVI CONTINU ARRÊTÉ", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ Suivi continu arrêté"
                tvDistance.text = "📡 Dernier envoi: —"
                // 📡 Prévenir l'autre téléphone que le suivi s'arrête
                try {
                    SmsManager.getDefault().sendDataMessage(
                        num, null, 10001.toShort(),
                        "BLOOM_STOP".toByteArray(Charsets.UTF_8),
                        null, null
                    )
                } catch (e: Exception) {}
            }
        }
    }

    private fun envoyerPositionSuiviContinu(pos: Location) {
        val num = etAutreNumero.text.toString().trim()
        if (num.isEmpty()) return

        val message = "BLOOM_POS:${pos.latitude}:${pos.longitude}"
        try {
            SmsManager.getDefault().sendDataMessage(
                num, null, 10001.toShort(),
                message.toByteArray(Charsets.UTF_8),
                null, null
            )
            tvDistance.text = "✅ ENVOYÉ ! Nouvelle position à ${pos.latitude}, ${pos.longitude}"
            tvStatut.text = "📡 Envoi automatique — OK"
        } catch (e: Exception) {
            tvDistance.text = "❌ Erreur envoi: ${e.message}"
        }
    }

    private fun envoyerMaPosition(numeroCible: String) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "⚠️ Permission GPS manquante", Toast.LENGTH_SHORT).show()
            return
        }

        val pos = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (pos != null) {
            val message = "BLOOM_POS:${pos.latitude}:${pos.longitude}"
            try {
                SmsManager.getDefault().sendDataMessage(
                    numeroCible, null, 10001.toShort(),
                    message.toByteArray(Charsets.UTF_8),
                    null, null
                )
                Toast.makeText(this, "📤 Position envoyée !", Toast.LENGTH_SHORT).show()
                tvStatut.text = "✅ Position envoyée"
                mettreAJourMoi(pos.latitude, pos.longitude)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "⏳ GPS non disponible — activez le GPS", Toast.LENGTH_LONG).show()
        }
    }

    private fun mettreAJourBoutons() {
        if (reponseAutoActivee) {
            btnSuivre.text = "🔄 ACTIF"
            btnSuivre.setBackgroundColor(Color.parseColor("#10B981"))
        } else {
            btnSuivre.text = "🔄 Réponse auto"
            btnSuivre.setBackgroundColor(Color.parseColor("#F59E0B"))
        }

        if (suiviContinuActif) {
            btnSuiviContinu.text = "📡 ACTIF"
            btnSuiviContinu.setBackgroundColor(Color.parseColor("#10B981"))
        } else {
            btnSuiviContinu.text = "📡 Suivi continu"
            btnSuiviContinu.setBackgroundColor(Color.parseColor("#2563EB"))
        }
    }

    private fun enregistrerReceveurs() {
        registerReceiver(maPositionReceiver, IntentFilter("com.bloom.gps.MA_POSITION"), RECEIVER_NOT_EXPORTED)
        registerReceiver(autrePositionReceiver, IntentFilter("com.bloom.gps.AUTRE_POSITION"), RECEIVER_NOT_EXPORTED)
    }

    private fun verifierPermissions() {
        val a = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val b = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val c = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        if (a == PackageManager.PERMISSION_GRANTED && b == PackageManager.PERMISSION_GRANTED && c == PackageManager.PERMISSION_GRANTED) {
            tvStatut.text = "✅ Prêt !"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            demarrerGPS()
        }
    }

    private fun demanderPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ),
            PERMISSIONS_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            tvStatut.text = "✅ TOUT EST PRÊT !"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            Toast.makeText(this, "🎉 Permissions accordées !", Toast.LENGTH_SHORT).show()
            demarrerGPS()
        } else {
            tvStatut.text = "❌ Permissions refusées"
            Toast.makeText(this, "⚠️ Accordez toutes les permissions", Toast.LENGTH_LONG).show()
        }
    }

    private fun mettreAJourMoi(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        if (marqueurMoi == null) {
            marqueurMoi = Marker(mapView).apply {
                position = point
                title = "📍 MOI"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getDrawable(android.R.drawable.ic_menu_myplaces)
            }
            mapView.overlays.add(marqueurMoi)
        } else {
            marqueurMoi!!.position = point
        }
        tvMoi.text = "🔵 MOI: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
        mapView.controller.setCenter(point)
        mapView.invalidate()
    }

    private fun mettreAJourLui(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        if (marqueurLui == null) {
            marqueurLui = Marker(mapView).apply {
                position = point
                title = "📍 LUI"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getDrawable(android.R.drawable.ic_menu_mylocation)
            }
            mapView.overlays.add(marqueurLui)
        } else {
            marqueurLui!!.position = point
        }
        tvLui.text = "🔴 LUI: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
        tvStatut.text = "✅ Position de l'autre mise à jour !"
        mapView.invalidate()
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(maPositionReceiver)
        unregisterReceiver(autrePositionReceiver)
        locationManager.removeUpdates(this)
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    override fun onProviderEnabled(p0: String) {}
    override fun onProviderDisabled(p0: String) {}
}
