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
                Log.d("BloomGPS", "📥 SMS reçu : $message")
                
                when {
                    message.startsWith("BLOOMGPS:") -> {
                        val parts = message.removePrefix("BLOOMGPS:").split(",")
                        if (parts.size >= 3) {
                            val lat = parts[0].toDouble()
                            val lon = parts[1].toDouble()
                            val vitesse = parts[2].toFloat()
                            val update = Intent("BLOOMGPS_AUTRE_UPDATE")
                            update.setPackage(context.packageName)
                            update.putExtra("lat", lat)
                            update.putExtra("lon", lon)
                            update.putExtra("speed", vitesse)
                            context.sendBroadcast(update)
                        }
                    }
                    message.startsWith("BLOOMGPS_CMD:") -> {
                        val cmd = message.removePrefix("BLOOMGPS_CMD:").trim()
                        val service = Intent(context, LocationTrackerService::class.java)
                        service.putExtra("commande", cmd)
                        if (cmd == "START" || cmd == "STOP") {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(service)
                            } else {
                                context.startService(service)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BloomGPS", "Erreur récepteur", e)
            }
        }
    }
}
