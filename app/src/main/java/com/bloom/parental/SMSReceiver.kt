package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val expéditeur = msg.originatingAddress ?: ""
            val contenu = msg.messageBody.trim()
            
            Log.d("BLOOM-SMS", "Reçu de $expéditeur : $contenu")
            
            // 🔑 COMMANDES SECRÈTES — INVISIBLES DANS LA BOÎTE SMS
            when {
                // 📍 DEMANDE DE POSITION
                contenu.startsWith("BLOOM_LOC_") -> {
                    abortBroadcast() // ✅ SMS CACHÉ — n'apparaît PAS
                    val codeSecret = contenu.removePrefix("BLOOM_LOC_")
                    if (codeSecret == BuildConfig.SECRET_CODE) {
                        LocationService.demanderPosition(context, expéditeur)
                    }
                }
                
                // ⏳ DÉFINIR TEMPS D'ÉCRAN
                contenu.startsWith("BLOOM_TIME_") -> {
                    abortBroadcast() // ✅ SMS CACHÉ
                    val parts = contenu.removePrefix("BLOOM_TIME_").split("_")
                    if (parts.size >= 2 && parts[0] == BuildConfig.SECRET_CODE) {
                        val heures = parts[1].toIntOrNull() ?: 2
                        ScreenTimeService.définirLimite(context, heures)
                    }
                }
                
                // ⏹️ COUPER ACCÈS
                contenu.startsWith("BLOOM_STOP_") -> {
                    abortBroadcast() // ✅ SMS CACHÉ
                    val codeSecret = contenu.removePrefix("BLOOM_STOP_")
                    if (codeSecret == BuildConfig.SECRET_CODE) {
                        ScreenTimeService.couperAccès(context)
                    }
                }
            }
        }
    }
}
