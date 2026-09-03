package com.bloom.gps

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
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
import android.telephony.SmsManager
import kotlin.math.roundToInt

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
    private lateinit var monMarqueur: Marker
    private lateinit var autreMarqueur: Marker

    private val PERMISSIONS_REQUEST = 1001
    private val REQUEST_NOTIFICATION_ACCESS = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_main)

        initialiserVues()
        initialiserCarte()
        verifierPermissions()
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

        val prefs = getSharedPreferences("BloomGPS", Context.MODE_PRIVATE)
        etNumeroDest.setText(prefs.getString("numero_dest", ""))

        btnStart.setOnClickListener { demarrerLocal() }
        btnStop.setOnClickListener { arreterLocal() }
        btnStartDist.setOnClickListener { envoyerCommande("START") }
        btnStopDist.setOnClickListener { envoyerCommande("STOP") }
        btnNotif.setOnClickListener { demanderPermissionNotifications() }
        btnPermissions.setOnClickListener { demanderPermissions() }
    }

    private fun initialiserCarte() {
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
    }

    fun mettreAJourMaPosition(lat: Double, lon: Double, vitesse: Float) {
        val point = GeoPoint(lat, lon)
        monMarqueur.position = point
        mapView.controller.setCenter(point)
        tvVitesse.text = "⚡ Vitesse : ${(vitesse * 3.6).roundToInt()} km/h"
        mapView.invalidate()
    }

    fun mettreAJourPositionAutre(lat: Double, lon: Double, vitesse: Float) {
        val point = GeoPoint(lat, lon)
        autreMarqueur.position = point
        tvVitesse.text = "⚡ Vitesse de l'autre : ${(vitesse * 3.6).roundToInt()} km/h"
        mapView.invalidate()
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
        val a = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val b = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val c = aPermissionNotifications()
        
        if (a == PackageManager.PERMISSION_GRANTED && b == PackageManager.PERMISSION_GRANTED && c) {
            tvStatut.text = "✅ TOUT PRÊT !"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            btnNotif.text = "✅ Notifications"
            btnNotif.isEnabled = false
        } else {
            val manquantes: MutableList<String> = mutableListOf()
            if (a != PackageManager.PERMISSION_GRANTED) manquantes.add("SMS")
            if (b != PackageManager.PERMISSION_GRANTED) manquantes.add("GPS")
            if (!c) manquantes.add("Notifications")
            tvStatut.text = "⚠️ Manquant : ${manquantes.joinToString(", ")}"
        }
    }

    private fun demanderPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
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
        if (num.isEmpty()) { Toast.makeText(this, "⚠️ Entrez le numéro de l'autre téléphone", Toast.LENGTH_SHORT).show(); return }
        getSharedPreferences("BloomGPS", Context.MODE_PRIVATE).edit().putString("numero_dest", num).apply()
        
        val intent = Intent(this, LocationTrackerService::class.java)
        intent.putExtra("commande", "START")
        intent.putExtra("numero_dest", num)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        tvStatut.text = "✅ Suivi DÉMARRÉ — Envoi toutes les 10m"
        Toast.makeText(this, "📡 Envoi position vers $num", Toast.LENGTH_SHORT).show()
    }

    private fun arreterLocal() {
        val intent = Intent(this, LocationTrackerService::class.java)
        stopService(intent)
        tvStatut.text = "🛑 Suivi ARRÊTÉ"
        Toast.makeText(this, "⏹️ Service arrêté", Toast.LENGTH_SHORT).show()
    }

    private fun envoyerCommande(commande: String) {
        val num = etNumeroDest.text.toString().trim()
        if (num.isEmpty()) { Toast.makeText(this, "⚠️ Entrez le numéro", Toast.LENGTH_SHORT).show(); return }
        getSharedPreferences("BloomGPS", Context.MODE_PRIVATE).edit().putString("numero_dest", num).apply()
        
        val smsManager = SmsManager.getDefault()
        val message = "BLOOMGPS_CMD:$commande"
        // ✅ sendTextMessage = 3 paramètres SIMPLES !
        smsManager.sendTextMessage(num, null, message, null, null)
        Toast.makeText(this, "📤 Commande '$commande' envoyée à distance", Toast.LENGTH_SHORT).show()
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
        registerReceiver(positionUpdateReceiver, IntentFilter("BLOOMGPS_UPDATE"))
        registerReceiver(autreUpdateReceiver, IntentFilter("BLOOMGPS_AUTRE_UPDATE"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(positionUpdateReceiver)
        unregisterReceiver(autreUpdateReceiver)
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
}
