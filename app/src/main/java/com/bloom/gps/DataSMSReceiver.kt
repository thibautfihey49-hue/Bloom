package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
                Log.d("BloomGPS", "📥 SMS DATA reçu : $message")

                when {
                    message.startsWith("BLOOMGPS:") -> {
                        val parts = message.removePrefix("BLOOMGPS:").split(",")
                        if (parts.size >= 3) {
                            val lat = parts[0].toDoubleOrNull() ?: return
                            val lon = parts[1].toDoubleOrNull() ?: return
                            val speed = parts[2].toFloatOrNull() ?: 0f
                            val update = Intent("BLOOMGPS_POSITION_UPDATE")
                            update.setPackage(context.packageName)
                            update.putExtra("lat", lat)
                            update.putExtra("lon", lon)
                            update.putExtra("speed", speed)
                            context.sendBroadcast(update)
                            abortBroadcast()
                        }
                    }
                    message.startsWith("BLOOMGPS_CMD:") -> {
                        val cmd = message.removePrefix("BLOOMGPS_CMD:").trim()
                        val service = Intent(context, LocationTrackerService::class.java)
                        service.putExtra("commande", cmd)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(service)
                        } else {
                            context.startService(service)
                        }
                        abortBroadcast()
                    }
                }
            } catch (e: Exception) {
                Log.e("BloomGPS", "Erreur récepteur", e)
            }
        }
    }
}
