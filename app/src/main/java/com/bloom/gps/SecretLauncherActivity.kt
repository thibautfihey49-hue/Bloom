package com.bloom.gps

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager

class SecretLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val codeSecret = getString(R.string.secret_code)
        
        val input = EditText(this)
        input.hint = "Code secret"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("🔓 Déverrouiller")
            .setMessage("Entrez le code secret pour faire réapparaître l'application")
            .setView(input)
            .setPositiveButton("✅ Déverrouiller") { _, _ ->
                val codeSaisi = input.text.toString()
                if (codeSaisi == codeSecret) {
                    // ✅ RÉACTIVER L'ICÔNE DANS LE TIROIR
                    val pm = packageManager
                    pm.setComponentEnabledSetting(
                        ComponentName(this, "com.bloom.gps.MainActivity"),
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    Toast.makeText(this, "✅ Application réapparaît !", Toast.LENGTH_LONG).show()
                    
                    // ✅ OUVRIR L'APP
                    val openIntent = packageManager.getLaunchIntentForPackage(packageName)
                    openIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(openIntent)
                } else {
                    Toast.makeText(this, "❌ Code incorrect", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton("Annuler") { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .show()
    }
}
