package com.bloom.gps

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private var monPositionActuelle: GeoPoint? = null
    private val PORT_BLOOM = 50006
    private lateinit var locationManager: LocationManager
    private var monGpsDisponible = false

    private val PERMISSIONS = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val monGpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val nouveauPoint = GeoPoint(location.latitude, location.longitude)
            monPositionActuelle = nouveauPoint
            monMarqueur?.position = nouveauPoint
            mapView.controller.setCenter(nouveauPoint)
            tvVitesse.text = "⚡ MA vitesse : ${(location.speed * 3.6).roundToInt()} km/h"
            tvStatut.text = "✅ MA position : %.6f, %.6f".format(location.latitude, location.longitude)
            mapView.invalidate()
            monGpsDisponible = true
            Log.d("BloomGPS", "📍 MA position GPS : $nouveauPoint")
        }
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String) { monGpsDisponible = true }
        override fun onProviderDisabled(p0: String) { monGpsDisponible = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            
            initialiserDossierOsmdroid()
            initialiserVues()
            initialiserCarte()
            initialiserMonGPS()
            verifierPermissionsAuDemarrage()
            
        } catch (e: Exception) {
            montrerErreur("Erreur démarrage : ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initialiserDossierOsmdroid() {
        try {
            val osmConfig = Configuration.getInstance()
            val baseDir = File(getExternalFilesDir(null), "osmdroid")
            baseDir.mkdirs()
            osmConfig.osmdroidBasePath = baseDir
            osmConfig.osmdroidTileCache = File(baseDir, "tiles")
            Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        } catch (e: Exception) {
            Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        }
    }

    private fun initialiserVues() {
        tvStatut = findViewById(R.id.tvStatut)
        tvVitesse = findViewById(R.id.tvVitesse)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnStartDist = findViewById(R.id.btnStartDist)
        btnStopDist = findViewById(R.id.btnStopDist)
        etNumeroDest = findViewById(R.id.etNumeroDest)
        mapView = findViewById(R.id.mapView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etNumeroDest.setText(prefs.getString("numero_dest", ""))

        btnStart.setOnClickListener { demarrerLocal() }
        btnStop.setOnClickListener { arreterLocal() }
        btnStartDist.setOnClickListener { envoyerCommandeADistance("START") }
        btnStopDist.setOnClickListener { envoyerCommandeADistance("STOP") }
    }

    private fun initialiserCarte() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(15.0)

            monMarqueur = Marker(mapView).apply {
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
                title = "📍 MOI"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            autreMarqueur = Marker(mapView).apply {
                icon = resources.getDrawable(android.R.drawable.ic_menu_compass, null)
                title = "📍 LUI/ELLE"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(monMarqueur)
            mapView.overlays.add(autreMarqueur)
            
            // ⚡ PAS de position par défaut — attendre le GPS réel
            tvStatut.text = "⏳ En attente de votre position GPS..."
            
            Log.d("BloomGPS", "✅ Carte initialisée — en attente GPS")
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ Erreur carte", e)
            Toast.makeText(this, "⚠️ Carte indisponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initialiserMonGPS() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            // Demander les mises à jour de position GPS en temps réel
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,   // 1 seconde
                1f,      // 1 mètre
                monGpsListener
            )
            // Essayer aussi le réseau si GPS lent
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                1f,
                monGpsListener
            )
        } catch (e: SecurityException) {
            Log.e("BloomGPS", "❌ Permissions GPS manquantes", e)
        }
    }

    fun mettreAJourPositionAutre(lat: Double, lon: Double, vitesse: Float) {
        try {
            val position = GeoPoint(lat, lon)
            autreMarqueur?.position = position
            tvVitesse.text = "⚡ LUI/ELLE : ${(vitesse * 3.6).roundToInt()} km/h"
            tvStatut.text = "✅ Lui/Elle : %.6f, %.6f".format(lat, lon)
            mapView.invalidate()
            Toast.makeText(this, "📥 Position reçue !", Toast.LENGTH_SHORT).show()
            Log.d("BloomGPS", "📍 Position de l'autre : $lat, $lon")
        } catch (e: Exception) {}
    }

    private fun verifierPermissionsAuDemarrage() {
        val manquantes = PERMISSIONS.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()
        
        if (manquantes.isEmpty()) {
            tvStatut.text = "⏳ En attente de votre position GPS..."
        } else {
            tvStatut.text = "⚠️ Autorisations manquantes"
            ActivityCompat.requestPermissions(this, manquantes.toTypedArray(), 1001)
        }
    }

    private fun demarrerLocal() {
        val num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez le numéro de l'autre téléphone", Toast.LENGTH_SHORT).show()
            return
        }
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putString("numero_dest", num).apply()
        
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            putExtra("commande", "START")
            putExtra("numero_dest", num)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            tvStatut.text = "✅ SUIVI DÉMARRÉ — 10m entre chaque envoi"
            Toast.makeText(this, "📡 Envoi vers $num — SMS de données", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun arreterLocal() {
        try {
            startService(Intent(this, LocationTrackerService::class.java).apply {
                putExtra("commande", "STOP")
            })
            tvStatut.text = "🛑 SUIVI ARRÊTÉ"
            Toast.makeText(this, "⏹️ Service arrêté", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {}
    }

    // ✅ ENVOI DE COMMANDE À DISTANCE — SIMPLIFIÉ ET FIABLE
    private fun envoyerCommandeADistance(commande: String) {
        val num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez le numéro d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val smsManager = SmsManager.getDefault()
            val contenu = "BLOOMGPS_CMD:$commande"
            val donnees = contenu.toByteArray(Charsets.UTF_8)
            smsManager.sendDataMessage(num, null, PORT_BLOOM.toShort(), donnees, null, null)
            Toast.makeText(this, "📤 Commande « $commande » envoyée à $num", Toast.LENGTH_LONG).show()
            Log.d("BloomGPS", "📤 Commande $commande envoyée à $num")
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erreur envoi : ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("BloomGPS", "❌ Erreur envoi commande", e)
        }
    }

    private val autreUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.extras?.let {
                mettreAJourPositionAutre(
                    it.getDouble("lat"),
                    it.getDouble("lon"),
                    it.getFloat("speed")
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            registerReceiver(autreUpdateReceiver, IntentFilter("BLOOMGPS_AUTRE_UPDATE"))
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(autreUpdateReceiver)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(monGpsListener)
        } catch (e: Exception) {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val toutesAccordees = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (toutesAccordees) {
                tvStatut.text = "⏳ En attente de votre position GPS..."
            } else {
                tvStatut.text = "⚠️ Certaines autorisations manquent"
                Toast.makeText(this, "⚠️ Sans toutes les permissions, l'application ne fonctionnera pas", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun montrerErreur(message: String) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Erreur")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
