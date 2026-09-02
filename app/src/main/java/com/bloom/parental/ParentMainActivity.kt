package com.bloom.parental

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ParentMainActivity : AppCompatActivity() {
    private val CODE = "BLOOM49"
    private lateinit var numEnfant: EditText
    private lateinit var mapView: MapView
    private lateinit var txtPos: TextView
    private lateinit var txtDemandes: TextView
    private lateinit var seekTemps: SeekBar
    private lateinit var txtTemps: TextView
    private val sms = SmsManager.getDefault()
    private var numeroEnfant = ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                "BLOOM_POSITION" -> {
                    val coord = i.getStringExtra("coord") ?: return
                    val de = i.getStringExtra("numEnfant") ?: "??"
                    val ll = coord.removePrefix("BLOOM_POS:").split(",")
                    if (ll.size >= 2) runOnUiThread {
                        val lat = ll[0].toDouble()
                        val lon = ll[1].toDouble()
                        txtPos.text = "✅ Position de $de : $lat, $lon"
                        mapView.controller.setCenter(GeoPoint(lat, lon))
                        mapView.overlays.clear()
                        mapView.overlays.add(Marker(mapView).apply {
                            position = GeoPoint(lat, lon)
                            title = "📍 Enfant"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                        mapView.invalidate()
                    }
                }
                "BLOOM_DEMANDE_TEMPS" -> runOnUiThread {
                    val de = i.getStringExtra("numEnfant") ?: "??"
                    txtDemandes.text = "📩 Demande de temps de $de\n→ Augmente le temps d'écran !"
                    txtDemandes.setBackgroundColor(0xFFFEF3C7.toInt())
                }
                "BLOOM_DEMANDE_INSTALL" -> runOnUiThread {
                    val de = i.getStringExtra("numEnfant") ?: "??"
                    val app = i.getStringExtra("nomApp") ?: "??"
                    txtDemandes.text = "📲 Demande d'installation de $app\nde $de\n→ Réponds en bas !"
                    txtDemandes.setBackgroundColor(0xFFE0E7FF.toInt())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_main)
        initUI()
        initCarte()
        registerReceiver(receiver, IntentFilter("BLOOM_POSITION"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiver, IntentFilter("BLOOM_DEMANDE_TEMPS"), RECEIVER_NOT_EXPORTED)
        registerReceiver(receiver, IntentFilter("BLOOM_DEMANDE_INSTALL"), RECEIVER_NOT_EXPORTED)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 101)
    }

    private fun initUI() {
        numEnfant = findViewById(R.id.edit_num_enfant)
        txtPos = findViewById(R.id.txt_position)
        txtDemandes = findViewById(R.id.txt_demandes)
        seekTemps = findViewById(R.id.seek_temps)
        txtTemps = findViewById(R.id.txt_temps)
        mapView = findViewById(R.id.map_view)

        findViewById<Button>(R.id.btn_localiser).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            envoyerSMS("BLOOM_LOC_$CODE")
            txtPos.text = "📍 Demande envoyée..."
        }
        findViewById<Button>(R.id.btn_pause).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            envoyerSMS("BLOOM_STOP_$CODE")
            Toast.makeText(this, "⏹️ Accès coupé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_bloquer_apps).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            startActivity(Intent(this, AppBlockerActivity::class.java).putExtra("numEnfant", numeroEnfant))
        }
        findViewById<Button>(R.id.btn_usage).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            startActivity(Intent(this, AppUsageActivity::class.java))
        }
        findViewById<Button>(R.id.btn_demandes_install).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            startActivity(Intent(this, InstallRequestsActivity::class.java).putExtra("numEnfant", numeroEnfant))
        }
        findViewById<Button>(R.id.btn_accepter_install).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            val app = txtDemandes.text.split("\n").getOrNull(1)?.removePrefix("→ ") ?: return@setOnClickListener
            envoyerSMS("BLOOM_INSTALL_OK:$CODE:$app")
            Toast.makeText(this, "✅ Installation autorisée", Toast.LENGTH_SHORT).show()
            txtDemandes.text = "📩 Aucune demande en attente"
            txtDemandes.setBackgroundColor(0xFFF7FAFC.toInt())
        }
        findViewById<Button>(R.id.btn_refuser_install).setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            val app = txtDemandes.text.split("\n").getOrNull(1)?.removePrefix("→ ") ?: return@setOnClickListener
            envoyerSMS("BLOOM_INSTALL_NON:$CODE:$app")
            Toast.makeText(this, "❌ Installation refusée", Toast.LENGTH_SHORT).show()
            txtDemandes.text = "📩 Aucune demande en attente"
            txtDemandes.setBackgroundColor(0xFFF7FAFC.toInt())
        }

        seekTemps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                txtTemps.text = if (p == 0) "⏳ Illimité" else if (p == 1) "⏳ 1 heure" else "⏳ $p heures"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                val h = sb?.progress ?: 2
                envoyerSMS("BLOOM_TIME_${CODE}_$h")
                Toast.makeText(this@ParentMainActivity, "$h heure(s) envoyée(s)", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initCarte() {
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(12.5)
        mapView.controller.setCenter(GeoPoint(47.4784, -0.5784))
    }

    private fun verifNum(): Boolean {
        numeroEnfant = numEnfant.text.toString().trim()
        if (numeroEnfant.isEmpty()) {
            Toast.makeText(this, "Saisis le numéro de l'enfant d'abord !", Toast.LENGTH_LONG).show()
            return false
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("NUM_ENFANT", numeroEnfant).apply()
        return true
    }

    private fun envoyerSMS(texte: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) return
        sms.sendTextMessage(numeroEnfant, null, texte, null, null)
    }

    override fun onDestroy() { super.onDestroy(); unregisterReceiver(receiver) }
}
