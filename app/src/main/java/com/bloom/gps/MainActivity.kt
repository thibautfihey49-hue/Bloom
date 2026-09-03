package com.bloom.gps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatut: TextView
    private lateinit var tvVitesseMoi: TextView
    private lateinit var tvVitesseAutre: TextView
    private lateinit var etNumeroAutre: EditText
    private lateinit var btnDemarrerMoi: Button
    private lateinit var btnArreterMoi: Button
    private lateinit var btnDemarrerAutre: Button
    private lateinit var btnArreterAutre: Button
    private lateinit var map: MapView
    private var marqueurMoi: Marker? = null
    private var marqueurAutre: Marker? = null

    private val toutesLesPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_SMS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val demandePermissions = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { resultats ->
        val toutesAccordees = resultats.all { it.value }
        if (toutesAccordees) {
            Toast.makeText(this, "✅ TOUTES les permissions accordées !", Toast.LENGTH_LONG).show()
            tvStatut.text = "✅ Prêt — entrez un numéro et démarrez"
        } else {
            val refusees = resultats.filter { !it.value }.keys.joinToString("\n• ")
            tvStatut.text = "⚠️ Permissions manquantes :\n• $refusees"
        }
    }

    private val demandePermissionAfficher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (peutAfficherParDessus()) {
            Toast.makeText(this, "✅ Affichage par-dessus : ACTIVÉ", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialiserVues()
        initialiserCarte()
        verifierEtDemanderPermissions()
        configurerBoutons()
        enregistrerRecepteurs()
    }

    private fun initialiserVues() {
        tvStatut = findViewById(R.id.tvStatut)
        tvVitesseMoi = findViewById(R.id.tvVitesseMoi)
        tvVitesseAutre = findViewById(R.id.tvVitesseAutre)
        etNumeroAutre = findViewById(R.id.etNumeroAutre)
        btnDemarrerMoi = findViewById(R.id.btnDemarrerMoi)
        btnArreterMoi = findViewById(R.id.btnArreterMoi)
        btnDemarrerAutre = findViewById(R.id.btnDemarrerAutre)
        btnArreterAutre = findViewById(R.id.btnArreterAutre)
        map = findViewById(R.id.map)
    }

    private fun initialiserCarte() {
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        map.setMultiTouchControls(true)
        map.controller?.setZoom(15.0)
        map.controller?.setCenter(GeoPoint(47.47, -0.55))

        marqueurMoi = Marker(map)
        marqueurMoi?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        marqueurMoi?.title = "Ma position"
        map.overlays.add(marqueurMoi)

        marqueurAutre = Marker(map)
        marqueurAutre?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass)
        marqueurAutre?.title = "Position de l'autre"
        map.overlays.add(marqueurAutre)
    }

    private fun verifierEtDemanderPermissions() {
        tvStatut.text = "🔍 Vérification des permissions..."

        val manquantes = toutesLesPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (manquantes.isNotEmpty()) {
            tvStatut.text = "⚠️ ${manquantes.size} permission(s) manquante(s)"
            demandePermissions.launch(manquantes)
        } else {
            tvStatut.text = "✅ Toutes permissions système accordées"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !peutAfficherParDessus()) {
            Toast.makeText(this, "ℹ️ Autorisez 'Afficher par-dessus les autres apps'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            demandePermissionAfficher.launch(intent)
        }
    }

    private fun peutAfficherParDessus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun configurerBoutons() {
        btnDemarrerMoi.setOnClickListener {
            val numero = etNumeroAutre.text.toString().trim()
            if (numero.isBlank()) {
                Toast.makeText(this, "⚠️ Entrez d'abord le numéro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "⚠️ Accorde la position en mode 'Tout le temps'", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val service = Intent(this, LocationTrackerService::class.java)
            service.putExtra("commande", "DEMARRER:$numero")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service)
            } else {
                startService(service)
            }
            tvStatut.text = "✅ 📍 Suivi démarré vers $numero"
        }

        btnArreterMoi.setOnClickListener {
            val service = Intent(this, LocationTrackerService::class.java)
            service.putExtra("commande", "ARRETER")
            startService(service)
            tvStatut.text = "⏹️ Suivi arrêté"
        }

        btnDemarrerAutre.setOnClickListener {
            val numero = etNumeroAutre.text.toString().trim()
            if (numero.isBlank()) {
                Toast.makeText(this, "⚠️ Entrez d'abord le numéro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            envoyerCommandeAutre("DEMARRER", numero)
            Toast.makeText(this, "📤 DÉMARRER → $numero", Toast.LENGTH_SHORT).show()
        }

        btnArreterAutre.setOnClickListener {
            val numero = etNumeroAutre.text.toString().trim()
            if (numero.isBlank()) {
                Toast.makeText(this, "⚠️ Entrez d'abord le numéro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            envoyerCommandeAutre("ARRETER", numero)
            Toast.makeText(this, "📤 ARRÊTER → $numero", Toast.LENGTH_SHORT).show()
        }
    }

    private fun envoyerCommandeAutre(commande: String, numero: String) {
        val sms = android.telephony.SmsManager.getDefault()
        val message = "BLOOMGPS_CMD:$commande"
        sms.sendDataMessage(numero, null, 50006, message.toByteArray(), null, null)
    }

    private fun enregistrerRecepteurs() {
        val monUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != "BLOOMGPS_MA_POSITION") return
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                val vitesse = intent.getFloatExtra("vitesse", 0f)
                runOnUiThread {
                    tvVitesseMoi.text = "Ma vitesse : ${String.format("%.1f", vitesse)} km/h"
                    if (lat != 0.0 && lon != 0.0) {
                        marqueurMoi?.position = GeoPoint(lat, lon)
                        map.controller?.animateTo(GeoPoint(lat, lon))
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            monUpdateReceiver,
            IntentFilter("BLOOMGPS_MA_POSITION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val autreUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != "BLOOMGPS_POSITION_UPDATE") return
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                val vitesse = intent.getFloatExtra("speed", 0f)
                runOnUiThread {
                    tvVitesseAutre.text = "Vitesse autre : ${String.format("%.1f", vitesse)} km/h"
                    if (lat != 0.0 && lon != 0.0) {
                        marqueurAutre?.position = GeoPoint(lat, lon)
                        map.invalidate()
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            autreUpdateReceiver,
            IntentFilter("BLOOMGPS_POSITION_UPDATE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
