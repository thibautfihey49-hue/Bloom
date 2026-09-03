package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsMessage.createFromPdu(pdus[0] as ByteArray, intent.extras?.getString("format"))
        } else {
            SmsMessage.createFromPdu(pdus[0] as ByteArray)
        }
        
        val corps = message.messageBody ?: return
        if (!corps.startsWith("BLOOMGPS:")) return
        
        val contenu = corps.removePrefix("BLOOMGPS:")
        val parts = contenu.split(",")
        if (parts.size >= 3) {
            try {
                val lat = parts[0].toDouble()
                val lon = parts[1].toDouble()
                val vitesse = parts[2].toFloat()
                
                val updateIntent = Intent("BLOOMGPS_AUTRE_UPDATE").apply {
                    putExtra("lat", lat)
                    putExtra("lon", lon)
                    putExtra("speed", vitesse)
                }
                context.sendBroadcast(updateIntent)
                abortBroadcast()
            } catch (e: NumberFormatException) {}
        }
    }
}
