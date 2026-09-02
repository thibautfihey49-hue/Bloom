package com.bloom.parental

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class InstallRequestsActivity : AppCompatActivity() {
    private lateinit var txtDemandes: TextView
    private lateinit var btnAccepter: Button
    private lateinit var btnRefuser: Button
    private var numeroEnfant = ""
    private var appEnAttente = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_install_requests)
        
        numeroEnfant = intent.getStringExtra("numEnfant") ?: ""
        txtDemandes = findViewById(R.id.txt_demande_install)
        btnAccepter = findViewById(R.id.btn_accepter_install)
        btnRefuser = findViewById(R.id.btn_refuser_install)
        
        chargerDemandesEnAttente()
        
        btnAccepter.setOnClickListener {
            repondreDemande(true)
        }
        
        btnRefuser.setOnClickListener {
            repondreDemande(false)
        }
    }

    private fun chargerDemandesEnAttente() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val enAttente = prefs.getStringSet("APPS_EN_ATTENTE", emptySet()) ?: emptySet()
        
        if (enAttente.isEmpty()) {
            txtDemandes.text = "✅ Aucune demande d'installation en attente"
            btnAccepter.isEnabled = false
            btnRefuser.isEnabled = false
        } else {
            appEnAttente = enAttente.firstOrNull() ?: ""
            txtDemandes.text = "📲 Demande d'installation :\n\n$appEnAttente\n\nDe l'enfant : $numeroEnfant"
            btnAccepter.isEnabled = true
            btnRefuser.isEnabled = true
        }
    }

    private fun repondreDemande(autoriser: Boolean) {
        if (appEnAttente.isEmpty() || numeroEnfant.isEmpty()) return
        
        val sms = android.telephony.SmsManager.getDefault()
        val code = "BLOOM49"
        val commande = if (autoriser) 
            "BLOOM_INSTALL_OK:$code:$appEnAttente"
        else 
            "BLOOM_INSTALL_NON:$code:$appEnAttente"
        
        sms.sendTextMessage(numeroEnfant, null, commande, null, null)
        
        // Retirer de la liste d'attente
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val enAttente = prefs.getStringSet("APPS_EN_ATTENTE", mutableSetOf())!!.toMutableSet()
        enAttente.remove(appEnAttente)
        prefs.edit().putStringSet("APPS_EN_ATTENTE", enAttente).apply()
        
        Toast.makeText(this, 
            if (autoriser) "✅ Installation autorisée !" 
            else "❌ Installation refusée", 
            Toast.LENGTH_SHORT).show()
        
        chargerDemandesEnAttente()
    }
}
