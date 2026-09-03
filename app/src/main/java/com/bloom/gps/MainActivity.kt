package com.bloom.gps

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
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
    private lateinit var btnNotif: Button
    private lateinit var btnPermissions: Button
    private lateinit var etNumeroDest: EditText
    private lateinit var mapView: MapView
    private var monMarqueur: Marker? = null
    private var autreMarqueur: Marker? = null

    private val PERMISSIONS_REQUEST = 1001
    private val REQUEST_NOTIFICATION_ACCESS = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            
            initialiserDossierOsmdroid()
            initialiserVues()
            initialiserCarte()
            verifierPermissions()
            
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
        btnNotif = findViewById(R.id.btnNotif)
        btnPermissions = findViewById(R.id.btnPermissions)
        etNumeroDest = findViewById(R.id.etNumeroDest)
        mapView = findViewById(R.id.mapView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        etNumeroDest.setText(prefs.getString("numero_dest", ""))

        btnStart.setOnClickListener { demarrerLocal() }
        btnStop.setOnClickListener { arreterLocal() }
        btnStartDist.setOnClickListener { envoyerCommande("START") }
        btnStopDist.setOnClickListener { envoyerCommande("STOP") }
        btnNotif.setOnClickListener { demanderPermissionNotifications() }
        btnPermissions.setOnClickListener { demanderPermissions() }
    }

    private fun initialiserCarte() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.controller.setZoom(15.0)

            monMarqueur = Marker(mapView).apply {
                icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
                title = "📍 Moi"
            }
            autreMarqueur = Marker(mapView).apply {
                icon = resources.getDrawable(android.R.drawable.ic_menu_compass, null)
                title = "📍 Lui/Elle"
            }
            mapView.overlays.add(monMarqueur)
            mapView.overlays.add(autreMarqueur)
            
            mapView.controller.setCenter(GeoPoint(48.95, 2.35)) // Paris par défaut
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Carte indisponible", Toast.LENGTH_SHORT).show()
        }
    }

    fun mettreAJourMaPosition(lat: Double, lon: Double, vitesse: Float) {
        try {
            monMarqueur?.position = GeoPoint(lat, lon)
            mapView.controller.setCenter(GeoPoint(lat, lon))
            tvVitesse.text = "⚡ Vitesse : ${(vitesse * 3.6).roundToInt()} km/h"
            mapView.invalidate()
        } catch (e: Exception) {}
    }

    fun mettreAJourPositionAutre(lat: Double, lon: Double, vitesse: Float) {
        try {
            autreMarqueur?.position = GeoPoint(lat, lon)
            tvVitesse.text = "⚡ Vitesse de l'autre : ${(vitesse * 3.6).roundToInt()} km/h"
            mapView.invalidate()
        } catch (e: Exception) {}
    }

    private fun aPermissionNotifications(): Boolean {
        val cn = ComponentName(this, MainActivity::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return if (!TextUtils.isEmpty(flat)) flat.split(":").contains(cn.flattenToString()) else false
    }

    private fun demanderPermissionNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivityForResult(intent, REQUEST_NOTIFICATION_ACCESS)
            Toast.makeText(this, "👉 Recherche 'Bloom GPS' → Activez !", Toast.LENGTH_LONG).show()
        }
    }

    private fun verifierPermissions() {
        val a = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val b = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val c = aPermissionNotifications()
        
        if (a == PackageManager.PERMISSION_GRANTED && b == PackageManager.PERMISSION_GRANTED) {
            tvStatut.text = "✅ PRÊT"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            if (c) {
                btnNotif.text = "✅ Notifications"
                btnNotif.isEnabled = false
            }
        } else {
            val manquantes: MutableList<String> = mutableListOf()
            if (a != PackageManager.PERMISSION_GRANTED) manquantes.add("SMS")
            if (b != PackageManager.PERMISSION_GRANTED) manquantes.add("GPS")
            tvStatut.text = "⚠️ Manquant : ${manquantes.joinToString(", ")}"
        }
    }

    private fun demanderPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        ), PERMISSIONS_REQUEST)
    }

    private fun demarrerLocal() {
        val num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) { 
            Toast.makeText(this, "⚠️ Entrez le numéro de l'autre téléphone", Toast.LENGTH_SHORT).show()
            return 
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("numero_dest", num).apply()
        
        val intent = Intent(this, LocationTrackerService::class.java)
        intent.putExtra("commande", "START")
        intent.putExtra("numero_dest", num)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            tvStatut.text = "✅ Suivi DÉMARRÉ — Envoi toutes les 10m"
            Toast.makeText(this, "📡 Envoi position vers $num", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erreur démarrage : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun arreterLocal() {
        try {
            val intent = Intent(this, LocationTrackerService::class.java)
            stopService(intent)
            tvStatut.text = "🛑 Suivi ARRÊTÉ"
            Toast.makeText(this, "⏹️ Service arrêté", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erreur arrêt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun envoyerCommande(commande: String) {
        val num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) { 
            Toast.makeText(this, "⚠️ Entrez le numéro", Toast.LENGTH_SHORT).show()
            return 
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("numero_dest", num).apply()
        
        try {
            val smsManager = SmsManager.getDefault()
            val message = "BLOOMGPS_CMD:$commande"
            smsManager.sendTextMessage(num, null, message, null, null)
            Toast.makeText(this, "📤 Commande '$commande' envoyée", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erreur envoi SMS : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val positionUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.extras?.let {
                mettreAJourMaPosition(
                    it.getDouble("lat"),
                    it.getDouble("lon"),
                    it.getFloat("speed")
                )
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
            registerReceiver(positionUpdateReceiver, IntentFilter("BLOOMGPS_UPDATE"))
            registerReceiver(autreUpdateReceiver, IntentFilter("BLOOMGPS_AUTRE_UPDATE"))
        } catch (e: Exception) {}
        verifierPermissions()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(positionUpdateReceiver)
            unregisterReceiver(autreUpdateReceiver)
        } catch (e: Exception) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NOTIFICATION_ACCESS) {
            Toast.makeText(this, if (aPermissionNotifications()) "✅ Notifications : ACCORDÉ !" else "⚠️ Activez Bloom GPS !", Toast.LENGTH_SHORT).show()
            verifierPermissions()
        }
    }

    override fun onRequestPermissionsResult(rq: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rq, p, g)
        verifierPermissions()
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
