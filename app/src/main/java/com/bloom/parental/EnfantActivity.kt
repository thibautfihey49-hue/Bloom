package com.bloom.parental

import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class EnfantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enfant)

        val btnDemandeTemps = findViewById<Button>(R.id.btnDemandeTemps)
        btnDemandeTemps.setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val num = prefs.getString("NUMERO_PARENT", "") ?: ""
            if (num.isEmpty()) {
                Toast.makeText(this, "⚠️ Numéro parent manquant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SmsManager.getDefault().sendTextMessage(num, null, "PLUS DE TEMPS", null, null)
            val intent = Intent("bloom.DEMANDE_TEMPS")
            intent.setPackage(packageName)
            sendBroadcast(intent)
            Toast.makeText(this, "✅ Demande envoyée !", Toast.LENGTH_SHORT).show()
        }
    }
}
