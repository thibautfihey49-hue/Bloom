package com.bloom.gps

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatut: TextView
    private lateinit var btnPermissions: Button
    private lateinit var btnNotif: Button
    private lateinit var mapView: MapView

    private val PERMISSIONS_REQUEST = 1001
    private val REQUEST_NOTIFICATION_ACCESS = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContentView(R.layout.activity_main)

        initialiserVues()
        verifierPermissions()
    }

    private fun initialiserVues() {
        tvStatut = findViewById(R.id.tvStatut)
        btnPermissions = findViewById(R.id.btnPermissions)
        mapView = findViewById(R.id.mapView)
        
        val layoutPrincipal = findViewById<LinearLayout>(android.R.id.content).getChildAt(0) as LinearLayout

        // ✅ Bouton pour demander la permission notifications
        btnNotif = Button(this).apply {
            text = "🔔 Autoriser lecture notifications"
            setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { demanderPermissionNotifications() }
            layoutPrincipal.addView(this)
        }

        btnPermissions.setOnClickListener { demanderPermissions() }
    }

    // ✅ VÉRIFIE SI LA PERMISSION NOTIFICATIONS EST ACCORDÉE
    private fun aPermissionNotifications(): Boolean {
        val cn = ComponentName(this, MainActivity::class.java)
        val flat = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_NOTIFICATION_LISTENERS)
        return if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            names.contains(cn.flattenToString())
        } else false
    }

    // ✅ OUVRE LA PAGE POUR ACTIVER LA PERMISSION
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
        
        if (a == PackageManager.PERMISSION_GRANTED && 
            b == PackageManager.PERMISSION_GRANTED && c) {
            tvStatut.text = "✅ TOUTES PERMISSIONS ACCORDÉES !"
            btnPermissions.text = "✅ OK"
            btnPermissions.isEnabled = false
            btnNotif.text = "✅ Notifications activées"
            btnNotif.isEnabled = false
        } else {
            val manquantes: MutableList<String> = mutableListOf()
            if (a != PackageManager.PERMISSION_GRANTED) manquantes.add("SMS")
            if (b != PackageManager.PERMISSION_GRANTED) manquantes.add("Position GPS")
            if (!c) manquantes.add("Lecture notifications")
            tvStatut.text = "⚠️ Manquant : ${manquantes.joinToString(", ")}"
        }
    }

    private fun demanderPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
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
            ),
            PERMISSIONS_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NOTIFICATION_ACCESS) {
            if (aPermissionNotifications()) {
                Toast.makeText(this, "✅ Notifications : ACCORDÉ !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Recherche 'Bloom GPS' et activez-la !", Toast.LENGTH_LONG).show()
            }
            verifierPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        verifierPermissions()
    }
}
