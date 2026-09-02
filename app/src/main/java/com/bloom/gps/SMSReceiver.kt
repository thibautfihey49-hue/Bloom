package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 🔇 ABORT — LA MESSAGERIE NE VOIT RIEN
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // 📥 Récupérer le SMS de données
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }.trim()
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-DATA", "📩 Reçu de $numeroExpediteur : $texte")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val reponseAutoActivee = prefs.getBoolean("REPONSE_AUTO", false)

        when {
            // 📥 DEMANDE → ENVOYER MA POSITION PAR SMS DE DONNÉES
            texte == "BLOOM_REQ" && reponseAutoActivee -> {
                Log.d("BLOOM-DATA", "📩 DEMANDE — Envoi position à $numeroExpediteur")
                envoyerPosition(context, numeroExpediteur)
            }

            // 📥 POSITION REÇUE → AFFICHER SUR LA CARTE
            texte.startsWith("BLOOM_POS:") -> {
                val coords = texte.removePrefix("BLOOM_POS:").split(":")
                if (coords.size == 2) {
                    try {
                        val lat = coords[0].toDouble()
                        val lon = coords[1].toDouble()
                        
                        val posIntent = Intent("com.bloom.gps.AUTRE_POSITION")
                        posIntent.setPackage(context.packageName)
                        posIntent.putExtra("latitude", lat)
                        posIntent.putExtra("longitude", lon)
                        context.sendBroadcast(posIntent)
                        
                        Log.d("BLOOM-DATA", "✅ Position affichée : $lat, $lon")
                    } catch (e: Exception) {
                        Log.e("BLOOM-DATA", "❌ Erreur parsing: ${e.message}")
                    }
                }
            }
        }
    }

    // 📤 ENVOI PAR SMS DE DONNÉES — PORT 10001 = INVISIBLE
    private fun envoyerPosition(context: Context, numeroCible: String) {
        if (ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BLOOM-DATA", "⚠️ Permission localisation manquante")
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val pos = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (pos != null) {
            val message = "BLOOM_POS:${pos.latitude}:${pos.longitude}"
            try {
                // 🔑 SMS DE DONNÉES SUR PORT 10001 = JAMAIS VISIBLE !
                SmsManager.getDefault().sendDataMessage(
                    numeroCible,
                    null,
                    10001.toShort(),
                    message.toByteArray(Charsets.UTF_8),
                    null, null
                )
                Log.d("BLOOM-DATA", "📤 Envoyé à $numeroCible")
                
                // ✅ Mettre à jour MA position
                val posIntent = Intent("com.bloom.gps.MA_POSITION")
                posIntent.setPackage(context.packageName)
                posIntent.putExtra("latitude", pos.latitude)
                posIntent.putExtra("longitude", pos.longitude)
                context.sendBroadcast(posIntent)
                
            } catch (e: Exception) {
                Log.e("BLOOM-DATA", "❌ Erreur envoi: ${e.message}")
            }
        } else {
            Log.w("BLOOM-DATA", "⚠️ Position non disponible")
        }
    }
}
