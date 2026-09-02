package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 🔇 RENDRE LE SMS INVISIBLE — pas de vibration, pas de notification
        abortBroadcast()

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-SMS", "📩 Reçu de $numeroExpediteur : $texte")

        // 📍 FORMAT ATTENDU : POS:47.4784,-0.5632
        if (texte.startsWith("POS:")) {
            val coords = texte.removePrefix("POS:").split(",")
            if (coords.size == 2) {
                try {
                    val lat = coords[0].toDouble()
                    val lon = coords[1].toDouble()
                    
                    // ✅ Envoyer la position à la carte
                    val posIntent = Intent("com.bloom.gps.AUTRE_POSITION")
                    posIntent.setPackage(context.packageName)
                    posIntent.putExtra("latitude", lat)
                    posIntent.putExtra("longitude", lon)
                    context.sendBroadcast(posIntent)
                    
                    Log.d("BLOOM-SMS", "✅ Position de l'autre affichée : $lat, $lon")
                } catch (e: Exception) {
                    Log.e("BLOOM-SMS", "❌ Erreur parsing: ${e.message}")
                }
            }
        }
    }
}
