package com.bloom.parental

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnParent = findViewById<Button>(R.id.btnParent)
        val btnEnfant = findViewById<Button>(R.id.btnEnfant)

        demanderPermissions()

        btnParent.setOnClickListener { demanderNumeroEtOuvrir("parent") }
        btnEnfant.setOnClickListener { demanderNumeroEtOuvrir("enfant") }
    }

    private fun demanderPermissions() {
        val perms = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, perms, 100)
    }

    private fun demanderNumeroEtOuvrir(role: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val cle = if (role == "parent") "NUMERO_PARENT" else "NUMERO_PARENT"

        if (prefs.getString(cle, null) != null) {
            ouvrir(role)
            return
        }

        val input = EditText(this)
        input.hint = "Numéro de téléphone"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE

        AlertDialog.Builder(this)
            .setTitle("📞 Numéro")
            .setMessage("Entre le numéro${if (role == "parent") " de l'enfant" else " du parent"} :")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val num = input.text.toString().trim()
                if (num.isNotEmpty()) {
                    prefs.edit().putString(cle, num).apply()
                    ouvrir(role)
                }
            }
            .show()
    }

    private fun ouvrir(role: String) {
        if (role == "parent") {
            startActivity(Intent(this, ParentActivity::class.java))
        } else {
            startActivity(Intent(this, EnfantActivity::class.java))
        }
    }
}
