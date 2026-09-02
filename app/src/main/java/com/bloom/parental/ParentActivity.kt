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

class ParentActivity : AppCompatActivity() {
    private lateinit var tvStatut: TextView
    private lateinit var tvDemande: TextView
    private lateinit var btnVerrou: Button
    private lateinit var btnDeVerrou: Button
    private lateinit var btnPos: Button

    private val demandeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            tvDemande.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent)

        tvStatut = findViewById(R.id.tvStatut)
        tvDemande = findViewById(R.id.tvDemande)
        btnVerrou = findViewById(R.id.btnVerrou)
        btnDeVerrou = findViewById(R.id.btnDeVerrou)
        btnPos = findViewById(R.id.btnPos)

        registerReceiver(demandeReceiver, IntentFilter("bloom.DEMANDE_TEMPS"))

        btnVerrou.setOnClickListener { envoyerSMS("VERROUILLER") }
        btnDeVerrou.setOnClickListener { envoyerSMS("DEVERROUILLER") }
        btnPos.setOnClickListener { envoyerSMS("POSITION") }
    }

    private fun envoyerSMS(texte: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val num = prefs.getString("NUMERO_PARENT", "") ?: ""
        if (num.isEmpty()) {
            Toast.makeText(this, "⚠️ Numéro manquant", Toast.LENGTH_SHORT).show()
            return
        }
        SmsManager.getDefault().sendTextMessage(num, null, texte, null, null)
        Toast.makeText(this, "✅ Envoyé : $texte", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(demandeReceiver)
    }
}
