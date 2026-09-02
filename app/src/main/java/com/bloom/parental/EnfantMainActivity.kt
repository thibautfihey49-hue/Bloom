package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class EnfantMainActivity : AppCompatActivity() {
    private lateinit var tvStatut: TextView
    private lateinit var tvDemandeTemps: TextView
    private lateinit var btnDemanderTemps: Button
    
    private val demandeTempsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            tvDemandeTemps.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFFF0FDF4.toInt())
        }
        
        // 📌 Titre
        val titre = TextView(this).apply {
            text = "🌸 Bloom — Mon Espace"
            textSize = 28f
            setTextColor(0xFF059669.toInt())
            setPadding(0, 0, 0, 40)
        }
        container.addView(titre)
        
        // 📌 Statut
        tvStatut = TextView(this).apply {
            text = "✅ Connecté — Surveillance active"
            textSize = 16f
            setTextColor(0xFF10B981.toInt())
            setPadding(0, 0, 0, 40)
        }
        container.addView(tvStatut)
        
        // 📌 Demande de temps (affichée EN CLAIR)
        tvDemandeTemps = TextView(this).apply {
            visibility = View.GONE
            text = "⏳ Demande envoyée : \"Je peux avoir plus de temps ?\""
            textSize = 18f
            setBackgroundColor(0xFFD1FAE5.toInt())
            setTextColor(0xFF065F46.toInt())
            setPadding(24, 16, 24, 16)
            setPadding(0, 0, 0, 40)
        }
        container.addView(tvDemandeTemps)
        
        // 📌 Bouton : Demander plus de temps
        btnDemanderTemps = Button(this).apply {
            text = "⏳ DEMANDER PLUS DE TEMPS"
            textSize = 16f
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(android.graphics.Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener { demanderPlusDeTemps() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        container.addView(btnDemanderTemps)
        
        scroll.addView(container)
        setContentView(scroll)
        
        registerReceiver(demandeTempsReceiver, IntentFilter("com.bloom.parental.AFFICHER_DEMANDE_TEMPS"))
    }
    
    private fun demanderPlusDeTemps() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        
        if (numeroParent.isNotEmpty()) {
            android.telephony.SmsManager.getDefault().sendTextMessage(
                numeroParent, null, "PLUS DE TEMPS", null, null
            )
            tvDemandeTemps.visibility = View.VISIBLE
        } else {
            android.widget.Toast.makeText(this, "⚠️ Numéro parent non configuré", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(demandeTempsReceiver)
    }
}
