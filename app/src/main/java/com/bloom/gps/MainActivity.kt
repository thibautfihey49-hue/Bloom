package com.bloom.gps

import android.Manifest
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

class MainActivity : AppCompatActivity(), LocationListener {
    
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
    private lateinit var locationManager: LocationManager
    
    private val PERMISSIONS_REQUEST = 1001
    private var reponseAutoActivee = false

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
        etMonNumero = findViewById(R.id.etMonNumero)
        etAutreNumero = findViewById(R.id.etAutreNumero)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnDemander = findViewById(R.id.btnDemander)
        btnRepondre = findViewById(R.id.btnRepondre)
        btnSuivre = findViewById(R.id.btnSuivre)
    }

    private fun chargerNumeros() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etMonNumero.setText(prefs.getString("MON_NUM", ""))
        etAutreNumero.setText(prefs.getString("AUTRE_NUM", ""))
        reponseAutoActivee = prefs.getBoolean("REP_AUTO", false)
        mettreAJourBoutonSuivre()
    }

    private fun configurerCarte() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5632))
        tvStatut.text = "✅ Prêt — entrez les numéros"
    }

    private fun demarrerGPS() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 30000, 50f, this
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 30000, 50f, this
        )
    }

    override fun onLocationChanged(location: Location) {
        mettreAJourMoi(location.latitude, location.longitude)
    }

    private fun configurerBoutons() {
        btnPermissions.setOnClickListener { demanderPermissions() }
        
        // 📥 DEMANDER SA POSITION
        btnDemander.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro de l'abord !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("AUTRE_NUM", num).apply()
            
            try {
                // 🔑 SMS DE DONNÉES — INVISIBLE
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

        // 📤 RÉPONDRE — ENVOYER MA POSITION
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

        // 🔄 RÉPONSE AUTOMATIQUE
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
            
            mettreAJourBoutonSuivre()
            
            if (reponseAutoActivee) {
                Toast.makeText(this, "🔄 RÉPONSE AUTO ACTIVE — Il demande → tu réponds TOUT SEUL", Toast.LENGTH_LONG).show()
                tvStatut.text = "✅ Réponse auto ACTIVE"
            } else {
                Toast.makeText(this, "🛑 Réponse auto désactivée", Toast.LENGTH_SHORT).show()
                tvStatut.text = "⏳ Réponse auto désactivée"
            }
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
                icon = getDrawable(android.R.drawable.ic_menu_myplaces)
            }
            mapView.overlays.add(marqueurLui)
        } else {
            marqueurLui!!.position = point
        }
        tvLui.text = "🔴 LUI: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
        tvStatut.text = "✅ Position de l'autre reçue !"
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
}
