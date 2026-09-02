package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 🔇 INTERCEPTE ET CACHE TOUT D'ABORD
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }.trim()
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-SMS", "📩 Reçu INVISIBLE de $numeroExpediteur : $texte")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val numeroAutre = prefs.getString("NUMERO_AUTRE", "") ?: ""
        val reponseAutoActivee = prefs.getBoolean("REPONSE_AUTO", false)

        when {
            // 📥 REÇOIT UNE DEMANDE → ENVOIE MA POSITION AUTOMATIQUEMENT
            texte == "DEMANDE:" && reponseAutoActivee -> {
                Log.d("BLOOM-SMS", "📩 DEMANDE REÇUE DE $numeroExpediteur — RÉPONSE AUTOMATIQUE ACTIVE")
                
                val positionIntent = Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_ENVOYER_UNE_FOIS
                    putExtra("NUMERO_CIBLE", numeroExpediteur)
                }
                context.startForegroundService(positionIntent)
            }

            // 📥 REÇOIT UNE POSITION DE L'AUTRE → L'AFFICHE SUR LA CARTE
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
                        
                        Log.d("BLOOM-SMS", "✅ Position de l'autre affichée : $lat, $lon")
                    } catch (e: Exception) {
                        Log.e("BLOOM-SMS", "❌ Erreur parsing POS: ${e.message}")
                    }
                }
            }

            // 📥 REÇOIT UNE RÉPONSE → AFFICHE SIMPLEMENT
            texte.startsWith("REPONSE:") -> {
                Log.d("BLOOM-SMS", "✅ Réponse confirmée")
            }
        }
    }
}
