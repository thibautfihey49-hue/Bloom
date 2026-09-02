package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val texte = msgs.joinToString("") { it.messageBody }
            val num = msgs.firstOrNull()?.originatingAddress ?: ""

            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            val numParent = prefs.getString("NUMERO_PARENT", "") ?: ""

            if (num == numParent) {
                when {
                    texte.contains("PLUS DE TEMPS", ignoreCase = true) -> {
                        val i = Intent("bloom.DEMANDE_TEMPS")
                        i.setPackage(context.packageName)
                        context.sendBroadcast(i)
                        abortBroadcast() // 🔇 Pas dans la boîte SMS
                    }
                    texte.contains("VERROUILLER", ignoreCase = true) -> {
                        abortBroadcast()
                    }
                    texte.contains("DEVERROUILLER", ignoreCase = true) -> {
                        abortBroadcast()
                    }
                    texte.contains("POSITION", ignoreCase = true) -> {
                        abortBroadcast()
                    }
                }
            }
        }
    }
}
