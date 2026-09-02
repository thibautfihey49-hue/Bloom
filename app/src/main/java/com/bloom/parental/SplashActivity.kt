package com.bloom.parental

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 100, 48, 48)
            setBackgroundColor(0xFFF5F7FA.toInt())
            gravity = android.view.Gravity.CENTER
        }
        
        val titre = android.widget.TextView(this).apply {
            text = "🌸 Bloom"
            textSize = 42f
            setTextColor(0xFF6366F1.toInt())
            setPadding(0, 0, 0, 60)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(titre)
        
        val btnParent = Button(this).apply {
            text = "👨‍👩‍👧 Je suis le PARENT"
            textSize = 18f
            setBackgroundColor(0xFF6366F1.toInt())
            setTextColor(android.graphics.Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { ouvrirParent() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        container.addView(btnParent)
        
        val btnEnfant = Button(this).apply {
            text = "👶 Je suis l'ENFANT"
            textSize = 18f
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(android.graphics.Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { ouvrirEnfant() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(btnEnfant)
        
        setContentView(container)
        
        demanderPermissions()
    }
    
    private fun demanderPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, permissions, 101)
    }
    
    private fun ouvrirParent() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getString("NUMERO_PARENT", null) == null) {
            val numero = android.widget.EditText(this).apply {
                hint = "Ton numéro de téléphone"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            android.app.AlertDialog.Builder(this)
                .setTitle("📞 Ton numéro")
                .setMessage("Entre ton numéro pour envoyer les commandes à l'enfant :")
                .setView(numero)
                .setPositiveButton("OK") { _, _ ->
                    val num = numero.text.toString().trim()
                    if (num.isNotEmpty()) {
                        prefs.edit().putString("NUMERO_PARENT", num).apply()
                        startActivity(Intent(this, ParentMainActivity::class.java))
                    }
                }
                .show()
        } else {
            startActivity(Intent(this, ParentMainActivity::class.java))
        }
    }
    
    private fun ouvrirEnfant() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (numeroParent.isEmpty()) {
            val numero = android.widget.EditText(this).apply {
                hint = "Numéro du parent"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            android.app.AlertDialog.Builder(this)
                .setTitle("📞 Numéro du parent")
                .setMessage("Entre le numéro du parent qui te surveille :")
                .setView(numero)
                .setPositiveButton("OK") { _, _ ->
                    val num = numero.text.toString().trim()
                    if (num.isNotEmpty()) {
                        prefs.edit().putString("NUMERO_PARENT", num).apply()
                        startActivity(Intent(this, EnfantMainActivity::class.java))
                    }
                }
                .show()
        } else {
            startActivity(Intent(this, EnfantMainActivity::class.java))
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            val ok = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!ok) {
                Toast.makeText(this, "⚠️ Certaines permissions sont nécessaires", Toast.LENGTH_LONG).show()
            }
        }
    }
}
