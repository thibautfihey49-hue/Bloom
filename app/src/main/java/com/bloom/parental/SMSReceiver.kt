package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val expediteur = msg.originatingAddress ?: ""
            val contenu = msg.messageBody.trim()

            Log.d("BLOOM-SMS", "Reçu de $expediteur : $contenu")

            when {
                contenu.startsWith("BLOOM_LOC_") -> {
                    abortBroadcast()
                    val codeSecret = contenu.removePrefix("BLOOM_LOC_")
                    if (codeSecret == BuildConfig.SECRET_CODE) {
                        LocationService.demanderPosition(context, expediteur)
                        sauvegarderNumeroParent(context, expediteur)
                    }
                }

                contenu.startsWith("BLOOM_TIME_") -> {
                    abortBroadcast()
                    val parts = contenu.removePrefix("BLOOM_TIME_").split("_")
                    if (parts.size >= 2 && parts[0] == BuildConfig.SECRET_CODE) {
                        val heures = parts[1].toIntOrNull() ?: 2
                        ScreenTimeService.definirLimite(context, heures)
                        sauvegarderNumeroParent(context, expediteur)
                    }
                }

                contenu.startsWith("BLOOM_STOP_") -> {
                    abortBroadcast()
                    val codeSecret = contenu.removePrefix("BLOOM_STOP_")
                    if (codeSecret == BuildConfig.SECRET_CODE) {
                        ScreenTimeService.couperAcces(context)
                        sauvegarderNumeroParent(context, expediteur)
                    }
                }

                contenu.startsWith("BLOOM_DEMAND_TEMPS:") -> {
                    val codeSecret = contenu.removePrefix("BLOOM_DEMAND_TEMPS:")
                    if (codeSecret == BuildConfig.SECRET_CODE) {
                        Log.d("BLOOM-DEMAND", "Demande de temps de $expediteur")
                        val notif = Intent("BLOOM_DEMANDE_TEMPS")
                        notif.putExtra("numEnfant", expediteur)
                        context.sendBroadcast(notif)
                    }
                }
            }
        }
    }

    private fun sauvegarderNumeroParent(contexte: Context, numero: String) {
        PreferenceManager.getDefaultSharedPreferences(contexte).edit()
            .putString("NUM_PARENT", numero).apply()
    }
}
