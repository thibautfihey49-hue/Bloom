package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import java.nio.charset.StandardCharsets

class DataSMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BloomGPS", "========================================")
        Log.d("BloomGPS", "📥 RÉCEPTEUR DÉCLENCHÉ !")
        Log.d("BloomGPS", "📥 Action: ${intent.action}")
        Log.d("BloomGPS", "========================================")
        
        when (intent.action) {
            "android.intent.action.DATA_SMS_RECEIVED" -> {
                Log.d("BloomGPS", "📡 → C'est un SMS DE DONNÉES !")
                traiterSmsDonnees(context, intent)
            }
            "android.provider.Telephony.SMS_RECEIVED" -> {
                Log.d("BloomGPS", "📩 → C'est un SMS TEXTE !")
                traiterSmsTexte(context, intent)
            }
            else -> {
                Log.d("BloomGPS", "⚠️ Action inconnue : ${intent.action}")
            }
        }
    }

    private fun traiterSmsDonnees(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle == null) {
            Log.e("BloomGPS", "❌ Bundle NULL !")
            return
        }
        
        val pdus = bundle.get("pdus") as? Array<ByteArray>
        if (pdus == null) {
            Log.e("BloomGPS", "❌ PDUs NULL !")
            return
        }
        
        Log.d("BloomGPS", "📡 Nombre de parties : ${pdus.size}")
        
        for ((index, pdu) in pdus.withIndex()) {
            try {
                val message = String(pdu, StandardCharsets.UTF_8)
                Log.d("BloomGPS", "📡 SMS $index reçu : >>>$message<<<")
                traiterCommande(context, message, "SMS DE DONNÉES")
            } catch (e: Exception) {
                Log.e("BloomGPS", "❌ Erreur lecture SMS données $index", e)
            }
        }
    }

    private fun traiterSmsTexte(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle == null) return
        
        val pdus = bundle.get("pdus") as? Array<ByteArray>
        if (pdus == null) return
        
        for ((index, pdu) in pdus.withIndex()) {
            try {
                val sms = SmsMessage.createFromPdu(pdu)
                val message = sms.messageBody
                val expediteur = sms.originatingAddress
                
                Log.d("BloomGPS", "📩 SMS TEXTE $index de $expediteur : >>>$message<<<")
                
                if (message.startsWith("BLOOMGPS")) {
                    traiterCommande(context, message, "SMS TEXTE")
                    abortBroadcast() // ✅ DISPARAÎT DE LA MESSAGERIE
                    Log.d("BloomGPS", "📩 SMS MASQUÉ de la messagerie !")
                } else {
                    Log.d("BloomGPS", "📩 SMS ignoré (ne commence pas par BLOOMGPS)")
                }
            } catch (e: Exception) {
                Log.e("BloomGPS", "❌ Erreur lecture SMS texte $index", e)
            }
        }
    }

    private fun traiterCommande(context: Context, message: String, type: String) {
        Log.d("BloomGPS", "🔧 TRAITEMENT COMMANDE : $message (via $type)")
        
        when {
            message.startsWith("BLOOMGPS:") -> {
                val contenu = message.removePrefix("BLOOMGPS:")
                val parts = contenu.split(",")
                Log.d("BloomGPS", "📍 Données position : ${parts.size} parties")
                
                if (parts.size >= 3) {
                    try {
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
                        Log.d("BloomGPS", "✅ ✅ Position transmise à l'interface !")
                    } catch (e: Exception) {
                        Log.e("BloomGPS", "❌ Erreur parsing position", e)
                    }
                }
            }
            message.startsWith("BLOOMGPS_CMD:") -> {
                val commande = message.removePrefix("BLOOMGPS_CMD:").trim()
                Log.d("BloomGPS", "🚀 COMMANDE DÉCODÉE : >>>$commande<<<")
                
                val serviceIntent = Intent(context, LocationTrackerService::class.java).apply {
                    putExtra("commande", commande)
                }
                
                when (commande) {
                    "START" -> {
                        Log.d("BloomGPS", "▶️ DÉMARRAGE DU SERVICE...")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        Log.d("BloomGPS", "✅ ✅ ✅ SERVICE DÉMARRÉ À DISTANCE ! ✅ ✅ ✅")
                    }
                    "STOP" -> {
                        Log.d("BloomGPS", "⏹️ ARRÊT DU SERVICE...")
                        context.startService(serviceIntent)
                        Log.d("BloomGPS", "✅ ✅ ✅ SERVICE ARRÊTÉ À DISTANCE ! ✅ ✅ ✅")
                    }
                    else -> {
                        Log.d("BloomGPS", "⚠️ Commande inconnue : $commande")
                    }
                }
            }
            else -> {
                Log.d("BloomGPS", "⚠️ Message non reconnu")
            }
        }
    }
}
