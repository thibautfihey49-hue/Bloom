package com.bloom.parental

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ParentMainActivity : AppCompatActivity() {
    private lateinit var btnLocaliser: Button
    private lateinit var btnPause: Button
    private lateinit var seekTemps: SeekBar
    private lateinit var txtTemps: TextView
    private lateinit var txtPosition: TextView
    private lateinit var editNumEnfant: EditText
    private lateinit var mapView: MapView
    
    private val CODE_SECRET = "BLOOM49"
    private var numEnfant = ""
    private val smsManager = SmsManager.getDefault()
    
    // Récepteur des SMS DE RÉPONSE de l'enfant
    private val réponseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "SMS_RECEIVED_ACTION") {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (msg in messages) {
                    val contenu = msg.messageBody.trim()
                    if (contenu.startsWith("BLOOM_POS:")) {
                        val coords = contenu.removePrefix("BLOOM_POS:").split(",")
                        if (coords.size >= 2) {
                            val lat = coords[0].toDoubleOrNull()
                            val lon = coords[1].toDoubleOrNull()
                            if (lat != null && lon != null) {
                                runOnUiThread { afficherPosition(lat, lon) }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_main)
        
        // 🗺️ Initialiser carte hors connexion
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        
        référencerUI()
        configurerCarte()
        configurerCurseurTemps()
        configurerBoutons()
        demanderPermissions()
        
        // Enregistrer récepteur SMS de réponse
        registerReceiver(réponseReceiver, IntentFilter("SMS_RECEIVED_ACTION"))
    }
    
    private fun référencerUI() {
        btnLocaliser = findViewById(R.id.btn_localiser)
        btnPause = findViewById(R.id.btn_pause)
        seekTemps = findViewById(R.id.seek_temps)
        txtTemps = findViewById(R.id.txt_temps)
        txtPosition = findViewById(R.id.txt_position)
        editNumEnfant = findViewById(R.id.edit_num_enfant)
        mapView = findViewById(R.id.map_view)
    }
    
    private fun configurerCarte() {
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(12.5)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5784)) // Angers par défaut
    }
    
    private fun configurerCurseurTemps() {
        seekTemps.max = 24 // ⏳ EXACTEMENT 0 À 24 HEURES
        seekTemps.progress = 2
        mettreAJourTexteTemps(2)
        
        seekTemps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                mettreAJourTexteTemps(progress)
            }
            
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                val heures = sb?.progress ?: 2
                envoyerCommande("BLOOM_TIME_${CODE_SECRET}_$heures")
                Toast.makeText(this@ParentMainActivity, 
                    "⏳ $heures heure(s) envoyé(s)", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun mettreAJourTexteTemps(heures: Int) {
        txtTemps.text = when {
            heures == 0 -> "⏳ Illimité"
            heures == 1 -> "⏳ 1 heure"
            else -> "⏳ $heures heures"
        }
    }
    
    private fun configurerBoutons() {
        // 📍 BOUTON LOCALISER — 1 clic = SMS caché = position sur carte
        btnLocaliser.setOnClickListener {
            if (!validerNuméro()) return@setOnClickListener
            envoyerCommande("BLOOM_LOC_$CODE_SECRET")
            txtPosition.text = "📍 Demande envoyée... Attente réponse..."
            Toast.makeText(this, "📡 SMS de localisation envoyé", Toast.LENGTH_SHORT).show()
        }
        
        // ⏹️ BOUTON PAUSE — Couper l'accès immédiatement
        btnPause.setOnClickListener {
            if (!validerNuméro()) return@setOnClickListener
            envoyerCommande("BLOOM_STOP_$CODE_SECRET")
            Toast.makeText(this, "⏹️ Accès coupé sur l'appareil enfant", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validerNuméro(): Boolean {
        numEnfant = editNumEnfant.text.toString().trim()
        if (numEnfant.isEmpty()) {
            Toast.makeText(this, "⚠️ Entrez d'abord le numéro de l'enfant", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
    
    private fun envoyerCommande(commande: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS), 101)
            return
        }
        smsManager.sendTextMessage(numEnfant, null, commande, null, null)
    }
    
    // 📍 Afficher la position SUR LA CARTE quand réponse reçue
    private fun afficherPosition(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        
        // Centrer la carte
        mapView.controller.setCenter(point)
        
        // Ajouter un marqueur
        mapView.overlays.clear()
        val marqueur = Marker(mapView)
        marqueur.position = point
        marqueur.title = "📍 Enfant"
        marqueur.snippet = "%.5f, %.5f".format(lat, lon)
        marqueur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marqueur)
        mapView.invalidate()
        
        txtPosition.text = "✅ Position : %.4f, %.4f".format(lat, lon)
        Toast.makeText(this, "📍 Position mise à jour !", Toast.LENGTH_SHORT).show()
    }
    
    private fun demanderPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        ), 101)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(réponseReceiver)
    }
}
