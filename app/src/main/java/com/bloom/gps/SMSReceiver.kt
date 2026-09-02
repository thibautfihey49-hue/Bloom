package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 🔇 D'ABORT D'ABORT — AVANT TOUT !
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-SMS", "📩 Reçu INVISIBLE de $numeroExpediteur : $texte")

        if (texte.startsWith("POS:")) {
            val coords = texte.removePrefix("POS:").split(",")
            if (coords.size == 2) {
                try {
                    val lat = coords[0].toDouble()
                    val lon = coords[1].toDouble()
                    
                    val posIntent = Intent("com.bloom.gps.AUTRE_POSITION")
                    posIntent.setPackage(context.packageName)
                    posIntent.putExtra("latitude", lat)
                    posIntent.putExtra("longitude", lon)
                    context.sendBroadcast(posIntent)
                    
                    Log.d("BLOOM-SMS", "✅ Position affichée — RIEN DANS LA MESSAGERIE !")
                } catch (e: Exception) {
                    Log.e("BLOOM-SMS", "❌ Erreur: ${e.message}")
                }
            }
        }
    }
}
