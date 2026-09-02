package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ✅ Intercepter et ANNULER la diffusion pour que le SMS N'APPARAISSE PAS dans la boîte de réception
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val messageComplet = StringBuilder()
            val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""
            
            for (msg in messages) {
                messageComplet.append(msg.messageBody)
            }
            
            val texte = messageComplet.toString()
            Log.d("BLOOM-SMS", "📩 Reçu de $numeroExpediteur : $texte")
            
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val numeroParent = prefs.getString("NUMERO_PARENT", "") ?: ""
            
            // ✅ COMMANDES DU PARENT
            if (numeroExpediteur == numeroParent) {
                when {
                    texte.contains("VERROUILLER", ignoreCase = true) -> {
                        ScreenLockReceiver.verrouiller(context)
                        abortBroadcast() // 🔇 NE PAS ENREGISTRER DANS LA BOÎTE SMS
                        return
                    }
                    texte.contains("DEVERROUILLER", ignoreCase = true) -> {
                        ScreenLockReceiver.deverrouiller(context)
                        abortBroadcast() // 🔇 NE PAS ENREGISTRER DANS LA BOÎTE SMS
                        return
                    }
                    texte.contains("POSITION", ignoreCase = true) -> {
                        LocationService.demanderPosition(context, numeroParent)
                        abortBroadcast() // 🔇 NE PAS ENREGISTRER DANS LA BOÎTE SMS
                        return
                    }
                    texte.contains("PLUS DE TEMPS", ignoreCase = true) -> {
                        // ✅ Afficher "Je peux avoir plus de temps" EN CLAIR sur l'écran enfant
                        val showIntent = Intent("com.bloom.parental.AFFICHER_DEMANDE_TEMPS")
                        showIntent.setPackage(context.packageName)
                        context.sendBroadcast(showIntent)
                        abortBroadcast() // 🔇 NE PAS ENREGISTRER DANS LA BOÎTE SMS
                        return
                    }
                }
            }
            
            // ✅ ANNULER TOUS LES SMS — rien n'apparaît dans la boîte de réception
            abortBroadcast()
        }
    }
}
