package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class EnfantMainActivity : AppCompatActivity() {
    private lateinit var tvStatut: TextView
    private lateinit var tvDemandeTemps: TextView
    private lateinit var btnDemanderTemps: Button
    
    private val demandeTempsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            tvDemandeTemps.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enfant_main)
        
        tvStatut = findViewById(R.id.tvStatut)
        tvDemandeTemps = findViewById(R.id.tvDemandeTemps)
        btnDemanderTemps = findViewById(R.id.btnDemanderTemps)
        
        btnDemanderTemps.setOnClickListener { demanderPlusDeTemps() }
        
        registerReceiver(demandeTempsReceiver, IntentFilter("com.bloom.parental.AFFICHER_DEMANDE_TEMPS"))
    }
    
    private fun demanderPlusDeTemps() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
        
        if (numeroParent.isEmpty()) {
            Toast.makeText(this, "⚠️ Numéro parent non configuré", Toast.LENGTH_SHORT).show()
            return
        }
        
        SmsManager.getDefault().sendTextMessage(numeroParent, null, "PLUS DE TEMPS", null, null)
        tvDemandeTemps.visibility = android.view.View.VISIBLE
        Toast.makeText(this, "✅ Demande envoyée !", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(demandeTempsReceiver)
    }
}
