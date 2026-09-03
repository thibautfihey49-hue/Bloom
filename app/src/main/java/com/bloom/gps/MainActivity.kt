package com.bloom.gps

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
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
            val nouveauPoint = GeoPoint(location.latitude, location.longitude)
            monPositionActuelle = nouveauPoint
            monMarqueur?.position = nouveauPoint
            mapView.controller.setCenter(nouveauPoint)
            tvVitesse.text = "⚡ MA vitesse : ${(location.speed * 3.6).roundToInt()} km/h"
            tvStatut.text = "✅ MA position : %.6f, %.6f".format(location.latitude, location.longitude)
            mapView.invalidate()
            Log.d("BloomGPS", "📍 MA position GPS : $nouveauPoint")
        }
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String) {}
        override fun onProviderDisabled(p0: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initialiserDossierOsmdroid()
        initialiserVues()
        initialiserCarte()
        initialiserMonGPS()
        verifierPermissionsAuDemarrage()
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
            
            tvStatut.text = "⏳ En attente de votre position GPS..."
            Log.d("BloomGPS", "✅ Carte initialisée")
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ Erreur carte", e)
            Toast.makeText(this, "⚠️ Carte indisponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initialiserMonGPS() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 1f, monGpsListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000L, 1f, monGpsListener
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

    private fun normaliserNumero(numero: String): String {
        var nettoye = numero.replace(" ", "").replace("-", "")
        if (nettoye.startsWith("+33")) {
            nettoye = "0" + nettoye.substring(3)
        }
        if (nettoye.startsWith("0033")) {
            nettoye = "0" + nettoye.substring(4)
        }
        return nettoye
    }

    private fun demarrerLocal() {
        var num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez le numéro de l'autre téléphone", Toast.LENGTH_SHORT).show()
            return
        }
        num = normaliserNumero(num)
        etNumeroDest.setText(num)
        
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

    private fun envoyerCommandeADistance(commande: String) {
        var num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez le numéro d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        num = normaliserNumero(num)
        etNumeroDest.setText(num)
        
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putString("numero_dest", num).apply()
        
        val smsManager = SmsManager.getDefault()
        val contenu = "BLOOMGPS_CMD:$commande"
        Log.d("BloomGPS", "========================================")
        Log.d("BloomGPS", "📤 ENVOI COMMANDE À DISTANCE")
        Log.d("BloomGPS", "📤 Numéro : $num")
        Log.d("BloomGPS", "📤 Contenu : >>>$contenu<<<")
        Log.d("BloomGPS", "📤 Port SMS de données : $PORT_BLOOM")
        Log.d("BloomGPS", "========================================")
        
        var succesDonnees = false
        var succesTexte = false
        
        // 📡 1. SMS de données
        try {
            val donnees = contenu.toByteArray(Charsets.UTF_8)
            smsManager.sendDataMessage(num, null, PORT_BLOOM.toShort(), donnees, null, null)
            Log.d("BloomGPS", "✅ ✅ SMS DE DONNÉES ENVOYÉ !")
            succesDonnees = true
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ ÉCHEC SMS DE DONNÉES", e)
        }
        
        // 📩 2. SMS TEXTE de SECOURS
        try {
            smsManager.sendTextMessage(num, null, contenu, null, null)
            Log.d("BloomGPS", "✅ ✅ SMS TEXTE DE SECOURS ENVOYÉ !")
            succesTexte = true
        } catch (e: Exception) {
            Log.e("BloomGPS", "❌ ÉCHEC SMS TEXTE", e)
        }
        
        when {
            succesDonnees && succesTexte -> {
                Toast.makeText(this, "✅ ✅ Les DEUX SMS envoyés à $num !", Toast.LENGTH_LONG).show()
            }
            succesDonnees -> {
                Toast.makeText(this, "✅ SMS de données envoyé à $num", Toast.LENGTH_LONG).show()
            }
            succesTexte -> {
                Toast.makeText(this, "⚠️ Seul le SMS texte est passé à $num", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "❌ ÉCHEC — Vérifiez le numéro et la permission SMS", Toast.LENGTH_LONG).show()
            }
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
                Toast.makeText(this, "⚠️ Sans permissions SMS/GPS, l'application ne fonctionnera pas", Toast.LENGTH_LONG).show()
            }
        }
    }
}
