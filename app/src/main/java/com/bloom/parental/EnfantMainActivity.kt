package com.bloom.parental

import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class EnfantMainActivity : AppCompatActivity() {
    private lateinit var txtTempsRestant: TextView
    private lateinit var btnDemanderTemps: Button
    private lateinit var txtStatut: TextView

    private val CODE_SECRET = "BLOOM49"
    private var numParent = ""
    private var compteur: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enfant_main)

        txtTempsRestant = findViewById(R.id.txt_temps_restant)
        btnDemanderTemps = findViewById(R.id.btn_demander_temps)
        txtStatut = findViewById(R.id.txt_statut)

        chargerNumParent()
        demarrerCompteurTemps()
        configurerBoutonDemande()
    }

    private fun chargerNumParent() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        numParent = prefs.getString("NUM_PARENT", "") ?: ""
    }

    private fun demarrerCompteurTemps() {
        val tempsFin = PreferenceManager.getDefaultSharedPreferences(this)
            .getLong("TEMPS_FIN", Long.MAX_VALUE)
        val maintenant = System.currentTimeMillis()
        val tempsRestant = tempsFin - maintenant

        // ✅ Si temps infini ou > 1 an = afficher "Illimité"
        if (tempsRestant <= 0 || tempsRestant > 31536000000L) {
            txtTempsRestant.text = "⏳ Illimité"
            txtTempsRestant.setTextColor(0xFF38A169.toInt())
            txtStatut.text = "✅ En attente de la limite définie par le parent"
            btnDemanderTemps.isEnabled = true
            return
        }

        compteur?.cancel()
        compteur = object : CountDownTimer(tempsRestant, 60000) {
            override fun onTick(millisRestants: Long) {
                mettreAJourAffichage(millisRestants)
            }
            override fun onFinish() {
                txtTempsRestant.text = "⏹️ Temps écoulé"
                txtTempsRestant.setTextColor(0xFFE53E3E.toInt())
                txtStatut.text = "⏹️ Accès coupé par le parent"
                btnDemanderTemps.isEnabled = true
            }
        }.start()

        mettreAJourAffichage(tempsRestant)
        txtStatut.text = "✅ Temps d'écran actif"
    }

    private fun mettreAJourAffichage(ms: Long) {
        val heures = ms / 3600000
        val minutes = (ms % 3600000) / 60000

        txtTempsRestant.text = when {
            heures <= 0 && minutes <= 0 -> "⏹️ Temps écoulé"
            heures <= 0 -> "⏳ $minutes min restant"
            minutes == 0L -> "⏳ $heures h restant"
            else -> "⏳ $heures h $minutes min restant"
        }

        txtTempsRestant.setTextColor(
            when {
                heures <= 0 -> 0xFFE53E3E.toInt()
                heures < 1 -> 0xFFED8936.toInt()
                else -> 0xFF38A169.toInt()
            }
        )
    }

    private fun configurerBoutonDemande() {
        btnDemanderTemps.setOnClickListener {
            if (numParent.isEmpty()) {
                Toast.makeText(this, "Numéro parent non configuré", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            envoyerDemandeTemps()
        }
    }

    private fun envoyerDemandeTemps() {
        try {
            val sms = SmsManager.getDefault()
            sms.sendTextMessage(numParent, null, "BLOOM_DEMAND_TEMPS:$CODE_SECRET", null, null)
            Toast.makeText(this, "📤 Demande envoyée à tes parents !", Toast.LENGTH_SHORT).show()
            btnDemanderTemps.isEnabled = false
            btnDemanderTemps.text = "✅ Demande envoyée"

            android.os.Handler().postDelayed({
                btnDemanderTemps.isEnabled = true
                btnDemanderTemps.text = "🙏 Demander plus de temps"
            }, 10000)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur : vérifie le numéro", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        demarrerCompteurTemps()
    }

    override fun onDestroy() {
        super.onDestroy()
        compteur?.cancel()
    }
}
