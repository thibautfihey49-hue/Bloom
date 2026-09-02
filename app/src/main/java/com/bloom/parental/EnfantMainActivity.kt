package com.bloom.parental

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class EnfantMainActivity : AppCompatActivity() {
    private val CODE = "BLOOM49"
    private lateinit var editNumParent: EditText
    private lateinit var txtTemps: TextView
    private lateinit var txtStatut: TextView
    private lateinit var btnDemanderTemps: Button
    private lateinit var btnDemanderInstall: Button
    private var numParent = ""
    private var compteur: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enfant_main)
        initUI()
        chargerNumParent()
        demarrerServiceSurveillance()
    }

    private fun initUI() {
        editNumParent = findViewById(R.id.edit_num_parent)
        txtTemps = findViewById(R.id.txt_temps_restant)
        txtStatut = findViewById(R.id.txt_statut)
        btnDemanderTemps = findViewById(R.id.btn_demander_temps)
        btnDemanderInstall = findViewById(R.id.btn_demander_install)

        editNumParent.setOnFocusChangeListener { _ , perdu ->
            if (!perdu) return@setOnFocusChangeListener
            val n = editNumParent.text.toString().trim()
            if (n.isNotEmpty()) {
                numParent = n
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("NUM_PARENT", n).apply()
                btnDemanderTemps.isEnabled = true
                btnDemanderInstall.isEnabled = true
                txtStatut.text = "✅ Connecté au parent"
                Toast.makeText(this, "Numéro parent sauvegardé !", Toast.LENGTH_SHORT).show()
            }
        }

        btnDemanderTemps.setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            envoyerSMS("BLOOM_DEMAND_TEMPS:$CODE")
            Toast.makeText(this, "📤 Demande envoyée !", Toast.LENGTH_SHORT).show()
            btnDemanderTemps.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ btnDemanderTemps.isEnabled = true }, 10000)
        }

        btnDemanderInstall.setOnClickListener {
            if (!verifNum()) return@setOnClickListener
            val app = "NomDeLApp" // À améliorer avec sélecteur d'app
            envoyerSMS("BLOOM_DEMAND_INSTALL:$CODE:$app")
            Toast.makeText(this, "📤 Demande d'installation envoyée !", Toast.LENGTH_SHORT).show()
            btnDemanderInstall.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ btnDemanderInstall.isEnabled = true }, 10000)
        }
    }

    private fun chargerNumParent() {
        numParent = PreferenceManager.getDefaultSharedPreferences(this).getString("NUM_PARENT", "") ?: ""
        if (numParent.isNotEmpty()) {
            editNumParent.setText(numParent)
            btnDemanderTemps.isEnabled = true
            btnDemanderInstall.isEnabled = true
        }
        mettreAJourAffichageTemps()
    }

    private fun verifNum(): Boolean {
        if (numParent.isEmpty()) {
            Toast.makeText(this, "Saisis d'abord le numéro de ton parent !", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun mettreAJourAffichageTemps() {
        val tempsFin = PreferenceManager.getDefaultSharedPreferences(this)
            .getLong("TEMPS_FIN", Long.MAX_VALUE)
        val reste = tempsFin - System.currentTimeMillis()

        if (reste <= 0 || reste > 31536000000L) {
            txtTemps.text = "⏳ Illimité"
            txtTemps.setTextColor(0xFF38A169.toInt())
            txtStatut.text = if (numParent.isEmpty()) "📱 Saisis le numéro de ton parent" else "✅ En attente des commandes"
            return
        }

        val h = reste / 3600000
        val m = (reste % 3600000) / 60000
        txtTemps.text = if (h <= 0) "⏳ $m min restant" else if (m == 0L) "⏳ $h h restant" else "⏳ $h h $m min restant"
        txtTemps.setTextColor(if (h < 1) 0xFFED8936.toInt() else 0xFF38A169.toInt())
        txtStatut.text = "✅ Temps d'écran actif"
    }

    private fun envoyerSMS(texte: String) {
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return
        SmsManager.getDefault().sendTextMessage(numParent, null, texte, null, null)
    }

    private fun demarrerServiceSurveillance() {
        startForegroundService(Intent(this, AppMonitorService::class.java))
    }

    override fun onResume() { super.onResume(); mettreAJourAffichageTemps() }
    override fun onDestroy() { super.onDestroy(); compteur?.cancel() }
}
