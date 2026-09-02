package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-GPS", "📩 Reçu de $numeroExpediteur : $texte")

        when {
            texte.startsWith("POS:") -> {
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
                        Log.d("BLOOM-GPS", "✅ Position affichée : $lat, $lon")
                    } catch (e: Exception) {
                        Log.e("BLOOM-GPS", "❌ Erreur parsing: ${e.message}")
                    }
                }
                abortBroadcast()
            }
            texte == "DEMARRE_SUIVI" -> {
                val serviceIntent = Intent(context, LocationTrackerService::class.java)
                serviceIntent.action = LocationTrackerService.ACTION_DEMARRER
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                abortBroadcast()
            }
            texte == "ARRETE_SUIVI" -> {
                val serviceIntent = Intent(context, LocationTrackerService::class.java)
                serviceIntent.action = LocationTrackerService.ACTION_ARRETER
                context.startService(serviceIntent)
                abortBroadcast()
            }
        }
    }
}
