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
        abortBroadcast()
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val texte = messages.joinToString("") { it.messageBody }.trim()
        val numeroExpediteur = messages.firstOrNull()?.originatingAddress ?: ""

        Log.d("BLOOM-DATA", "📩 Reçu de $numeroExpediteur : $texte")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val reponseAutoActivee = prefs.getBoolean("REPONSE_AUTO", true)

        when {
            texte == "BLOOM_START" && reponseAutoActivee -> {
                prefs.edit().putBoolean("SUIVI_ACTIF", true).apply()
                prefs.edit().putString("NUMERO_CIBLE", numeroExpediteur).apply()
                Log.d("BLOOM-DATA", "🟢 SUIVI DÉMARRÉ")
                
                val startIntent = Intent("com.bloom.gps.DEMARRER_SUIVI")
                startIntent.setPackage(context.packageName)
                context.sendBroadcast(startIntent)
                
                envoyerPosition(context, numeroExpediteur)
            }

            texte == "BLOOM_STOP" -> {
                prefs.edit().putBoolean("SUIVI_ACTIF", false).apply()
                Log.d("BLOOM-DATA", "🔴 SUIVI ARRÊTÉ")
                
                val stopIntent = Intent("com.bloom.gps.ARRETER_SUIVI")
                stopIntent.setPackage(context.packageName)
                context.sendBroadcast(stopIntent)
            }

            // ✅ REÇOIT POSITION + VITESSE : BLOOM_POS:LAT:LON:VITESSE
            texte.startsWith("BLOOM_POS:") -> {
                val coords = texte.removePrefix("BLOOM_POS:").split(":")
                if (coords.size >= 2) {
                    try {
                        val lat = coords[0].toDouble()
                        val lon = coords[1].toDouble()
                        val vitesse = if (coords.size >= 3) coords[2].toDoubleOrNull() ?: 0.0 else 0.0
                        
                        val posIntent = Intent("com.bloom.gps.AUTRE_POSITION")
                        posIntent.setPackage(context.packageName)
                        posIntent.putExtra("latitude", lat)
                        posIntent.putExtra("longitude", lon)
                        posIntent.putExtra("vitesse", vitesse)
                        context.sendBroadcast(posIntent)
                        
                        Log.d("BLOOM-DATA", "✅ Position : $lat, $lon | Vitesse : $vitesse km/h")
                    } catch (e: Exception) {
                        Log.e("BLOOM-DATA", "❌ Erreur: ${e.message}")
                    }
                }
            }
        }
    }

    private fun envoyerPosition(context: Context, numeroCible: String) {
        if (ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val posActuelle = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (posActuelle != null) {
            // ✅ RÉCUPÉRER POSITION PRÉCÉDENTE POUR CALCULER LA VITESSE
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val ancienneLat = prefs.getFloat("DERNIERE_LAT", 0f).toDouble()
            val ancienneLon = prefs.getFloat("DERNIERE_LON", 0f).toDouble()
            val ancienTemps = prefs.getLong("DERNIERE_TEMPS", 0L)
            val tempsActuel = System.currentTimeMillis()

            var vitesse = 0.0

            // ✅ CALCULER LA VITESSE SI ON A UNE POSITION PRÉCÉDENTE
            if (ancienneLat != 0.0 && ancienneLon != 0.0 && ancienTemps != 0L) {
                val distance = calculerDistance(ancienneLat, ancienneLon, posActuelle.latitude, posActuelle.longitude)
                val tempsEnSecondes = (tempsActuel - ancienTemps) / 1000.0
                if (tempsEnSecondes > 0) {
                    val vitesseMS = distance / tempsEnSecondes
                    vitesse = vitesseMS * 3.6 // ✅ CONVERTIR EN KM/H
                }
            }

            // ✅ SAUVEGARDER LA POSITION ACTUELLE POUR LA PROCHAINE FOIS
            prefs.edit()
                .putFloat("DERNIERE_LAT", posActuelle.latitude.toFloat())
                .putFloat("DERNIERE_LON", posActuelle.longitude.toFloat())
                .putLong("DERNIERE_TEMPS", tempsActuel)
                .apply()

            // ✅ ENVOYER POSITION + VITESSE
            val message = "BLOOM_POS:${posActuelle.latitude}:${posActuelle.longitude}:${String.format("%.1f", vitesse)}"
            try {
                SmsManager.getDefault().sendDataMessage(
                    numeroCible, null, 10001.toShort(),
                    message.toByteArray(Charsets.UTF_8),
                    null, null
                )
                Log.d("BLOOM-DATA", "📤 Position + vitesse envoyée : $vitesse km/h")
                
                val posIntent = Intent("com.bloom.gps.MA_POSITION")
                posIntent.setPackage(context.packageName)
                posIntent.putExtra("latitude", posActuelle.latitude)
                posIntent.putExtra("longitude", posActuelle.longitude)
                context.sendBroadcast(posIntent)
                
            } catch (e: Exception) {
                Log.e("BLOOM-DATA", "❌ Erreur: ${e.message}")
            }
        }
    }

    // ✅ FORMULE DE HAVERSINE — CALCULER DISTANCE ENTRE 2 COORDONNÉES
    private fun calculerDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rayonTerre = 6371000.0 // mètres
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        return rayonTerre * c // distance en mètres
    }
}
