package com.bloom.gps

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class SecretInputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_input)

        val etCode = findViewById<EditText>(R.id.etCodeSecret)
        val btnValider = findViewById<Button>(R.id.btnValider)
        val codeSecretAttendu = getString(R.string.secret_code)

        btnValider.setOnClickListener {
            val codeSaisi = etCode.text.toString().trim()
            
            if (codeSaisi == codeSecretAttendu) {
                // ✅ RÉACTIVER L'ICÔNE PRINCIPALE
                val pm = packageManager
                pm.setComponentEnabledSetting(
                    ComponentName(this, "com.bloom.gps.MainActivity"),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                
                Toast.makeText(this, "✅ Application réapparaît !", Toast.LENGTH_LONG).show()
                
                // ✅ OUVRIR L'APP PRINCIPALE
                val openIntent = packageManager.getLaunchIntentForPackage(packageName)
                openIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(openIntent)
                finish()
            } else {
                Toast.makeText(this, "❌ Code incorrect", Toast.LENGTH_SHORT).show()
                etCode.text.clear()
            }
        }
    }
}
