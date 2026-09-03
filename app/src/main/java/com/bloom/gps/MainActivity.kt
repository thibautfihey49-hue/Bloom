package com.bloom.gps

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.telephony.SmsManager
import kotlin.math.roundToInt
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatut: TextView
    private lateinit var tvVitesse: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnStartDist: Button
    private lateinit var btnStopDist: Button
    private lateinit var etNumeroDest: EditText
    private lateinit var mapView: MapView
    private var monMarqueur: Marker? = null
    private var autreMarqueur: Marker? = null
    private lateinit var locationManager: LocationManager
    private val PORT = 50006

    private val PERMISSIONS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val monGpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val pt = GeoPoint(location.latitude, location.longitude)
            monMarqueur?.position = pt
            mapView.controller.setCenter(pt)
            tvVitesse.text = "🟦 MA vitesse : ${(location.speed * 3.6).roundToInt()} km/h"
            tvStatut.text = "✅ Position mise à jour"
            mapView.invalidate()
        }
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String) {}
        override fun onProviderDisabled(p0: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        tvStatut = findViewById(R.id.tvStatut)
        tvVitesse = findViewById(R.id.tvVitesse)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnStartDist = findViewById(R.id.btnStartDist)
        btnStopDist = findViewById(R.id.btnStopDist)
        etNumeroDest = findViewById(R.id.etNumeroDest)
        mapView = findViewById(R.id.mapView)

        etNumeroDest.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("numero_dest", ""))

        // 🗺️ Carte
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // 🟦 MOI = BLEU
        monMarqueur = Marker(mapView)
        monMarqueur?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        monMarqueur?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        monMarqueur?.title = "🟦 MOI"
        monMarqueur?.icon?.setTint(Color.BLUE)
        mapView.overlays.add(monMarqueur)

        // 🟥 L'AUTRE = ROUGE
        autreMarqueur = Marker(mapView)
        autreMarqueur?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        autreMarqueur?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        autreMarqueur?.title = "🟥 L'AUTRE"
        autreMarqueur?.icon?.setTint(Color.RED)
        mapView.overlays.add(autreMarqueur)

        // 📍 GPS
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5f, monGpsListener)
        }

        // 📡 Récepteur position de l'autre
        registerReceiver(autreUpdateReceiver, IntentFilter("BLOOMGPS_AUTRE_UPDATE"))

        // 🔘 Boutons
        btnStart.setOnClickListener { demarrerLocal() }
        btnStop.setOnClickListener { arreterLocal() }
        btnStartDist.setOnClickListener { envoyerCommande("START") }
        btnStopDist.setOnClickListener { envoyerCommande("STOP") }

        verifierPermissions()
    }

    private val autreUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "BLOOMGPS_AUTRE_UPDATE") {
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                val vit = intent.getFloatExtra("speed", 0f)
                autreMarqueur?.position = GeoPoint(lat, lon)
                tvStatut.text = "🟥 Position de l'autre : %.4f, %.4f".format(lat, lon)
                tvVitesse.text = "🟦 Ma vitesse | 🟥 Vitesse autre : ${(vit * 3.6).roundToInt()} km/h"
                mapView.invalidate()
                Toast.makeText(this@MainActivity, "📥 Position de l'autre reçue !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normaliserNumero(num: String): String {
        var n = num.replace(" ", "").replace("-", "")
        if (n.startsWith("+33")) n = "0" + n.substring(3)
        if (n.startsWith("0033")) n = "0" + n.substring(4)
        return n
    }

    private fun envoyerCommande(cmd: String) {
        var num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez un numéro", Toast.LENGTH_SHORT).show()
            return
        }
        num = normaliserNumero(num)
        etNumeroDest.setText(num)
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("numero_dest", num).apply()
        
        try {
            val contenu = "BLOOMGPS_CMD:$cmd".toByteArray(Charsets.UTF_8)
            SmsManager.getDefault().sendDataMessage(num, null, PORT.toShort(), contenu, null, null)
            Toast.makeText(this, "✅ Commande envoyée à $num", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Échec — Vérifiez la permission SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun demarrerLocal() {
        val intent = Intent(this, LocationTrackerService::class.java)
        intent.putExtra("commande", "START")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        Toast.makeText(this, "✅ Suivi démarré", Toast.LENGTH_SHORT).show()
    }

    private fun arreterLocal() {
        val intent = Intent(this, LocationTrackerService::class.java)
        intent.putExtra("commande", "STOP")
        startService(intent)
        Toast.makeText(this, "✅ Suivi arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun verifierPermissions() {
        val manquantes = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (manquantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, manquantes, 1001)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "✅ Toutes les permissions accordées", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Certaines permissions manquent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(autreUpdateReceiver) } catch (e: Exception) {}
        try { locationManager.removeUpdates(monGpsListener) } catch (e: Exception) {}
    }
}
