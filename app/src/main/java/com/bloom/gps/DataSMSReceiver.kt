package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.nio.charset.StandardCharsets

class DataSMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.DATA_SMS_RECEIVED") return
        
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<ByteArray> ?: return
        
        for (pdu in pdus) {
            try {
                val message = String(pdu, StandardCharsets.UTF_8)
                Log.d("BloomGPS", "📥 SMS DE DONNÉES reçu : $message")
                
                when {
                    message.startsWith("BLOOMGPS:") -> {
                        val contenu = message.removePrefix("BLOOMGPS:")
                        val parts = contenu.split(",")
                        if (parts.size >= 3) {
                            val lat = parts[0].toDouble()
                            val lon = parts[1].toDouble()
                            val vitesse = parts[2].toFloat()
                            
                            val updateIntent = Intent("BLOOMGPS_AUTRE_UPDATE").apply {
                                setPackage(context.packageName)
                                putExtra("lat", lat)
                                putExtra("lon", lon)
                                putExtra("speed", vitesse)
                            }
                            context.sendBroadcast(updateIntent)
                            Log.d("BloomGPS", "✅ Position de l'autre affichée !")
                        }
                    }
                    message.startsWith("BLOOMGPS_CMD:") -> {
                        val commande = message.removePrefix("BLOOMGPS_CMD:").trim()
                        Log.d("BloomGPS", "🔧 Commande reçue : $commande")
                        
                        val serviceIntent = Intent(context, LocationTrackerService::class.java).apply {
                            putExtra("commande", commande)
                        }
                        
                        when (commande) {
                            "START" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                Log.d("BloomGPS", "✅ ✅ DÉMARRAGE À DISTANCE ACTIVÉ !")
                            }
                            "STOP" -> {
                                context.startService(serviceIntent)
                                Log.d("BloomGPS", "✅ ✅ ARRÊT À DISTANCE ACTIVÉ !")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BloomGPS", "❌ Erreur SMS de données", e)
            }
        }
    }
}
