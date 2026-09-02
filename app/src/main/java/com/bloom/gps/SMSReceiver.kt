package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }.trim()
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-DATA", "📩 Reçu de $numeroExpediteur : $texte")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val reponseAutoActivee = prefs.getBoolean("REPONSE_AUTO", false)

        when {
            // 📥 DEMANDE UNE POSITION
            texte == "BLOOM_REQ" && reponseAutoActivee -> {
                Log.d("BLOOM-DATA", "📩 DEMANDE — Envoi position à $numeroExpediteur")
                envoyerPosition(context, numeroExpediteur)
            }

            // 📡 DÉMARRER MON SUIVI À DISTANCE
            texte == "BLOOM_START" && reponseAutoActivee -> {
                prefs.edit().putBoolean("MON_SUIVI_ACTIF", true).apply()
                prefs.edit().putString("NUMERO_CIBLE", numeroExpediteur).apply()
                Log.d("BLOOM-DATA", "📡 SUIVI DÉMARRÉ À DISTANCE PAR $numeroExpediteur")
                
                val startIntent = Intent("com.bloom.gps.DEMARRER_SUIVI")
                startIntent.setPackage(context.packageName)
                context.sendBroadcast(startIntent)
                
                envoyerPosition(context, numeroExpediteur) // Envoyer immédiatement
            }

            // ⏹️ ARRÊTER MON SUIVI À DISTANCE
            texte == "BLOOM_STOP" -> {
                prefs.edit().putBoolean("MON_SUIVI_ACTIF", false).apply()
                Log.d("BLOOM-DATA", "⏹️ SUIVI ARRÊTÉ À DISTANCE PAR $numeroExpediteur")
                
                val stopIntent = Intent("com.bloom.gps.ARRETER_SUIVI")
                stopIntent.setPackage(context.packageName)
                context.sendBroadcast(stopIntent)
            }

            // 📥 POSITION REÇUE DE L'AUTRE
            texte.startsWith("BLOOM_POS:") -> {
                val coords = texte.removePrefix("BLOOM_POS:").split(":")
                if (coords.size >= 2) {
                    try {
                        val lat = coords[0].toDouble()
                        val lon = coords[1].toDouble()
                        
                        val posIntent = Intent("com.bloom.gps.AUTRE_POSITION")
                        posIntent.setPackage(context.packageName)
                        posIntent.putExtra("latitude", lat)
                        posIntent.putExtra("longitude", lon)
                        context.sendBroadcast(posIntent)
                        
                        Log.d("BLOOM-DATA", "✅ Position mise à jour : $lat, $lon")
                    } catch (e: Exception) {
                        Log.e("BLOOM-DATA", "❌ Erreur parsing: ${e.message}")
                    }
                }
            }
        }
    }

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
                SmsManager.getDefault().sendDataMessage(
                    numeroCible, null, 10001.toShort(),
                    message.toByteArray(Charsets.UTF_8),
                    null, null
                )
                Log.d("BLOOM-DATA", "📤 Position envoyée à $numeroCible")
                
                val posIntent = Intent("com.bloom.gps.MA_POSITION")
                posIntent.setPackage(context.packageName)
                posIntent.putExtra("latitude", pos.latitude)
                posIntent.putExtra("longitude", pos.longitude)
                context.sendBroadcast(posIntent)
                
            } catch (e: Exception) {
                Log.e("BLOOM-DATA", "❌ Erreur envoi: ${e.message}")
            }
        }
    }
}
