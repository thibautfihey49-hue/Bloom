package com.bloom.parental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.bloom.parental.data.Prefs
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val body = msg.messageBody ?: continue
                val sender = msg.originatingAddress ?: continue

                if (body.startsWith("🌸BLOOM|")) {
                    abortBroadcast() // ❌ SUPPRIME LE SMS — JAMAIS DANS LA BOÎTE DE RÉCEPTION
                    processCommand(ctx, sender, body)
                }
            }
        }
    }

    private fun processCommand(ctx: Context, sender: String, body: String) {
        val parts = body.split("|")
        if (parts.size < 2) return

        when (parts[1]) {
            "USAGE" -> {
                // Parent reçoit temps utilisé
            }
            "CMD" -> {
                val cmd = parts.getOrNull(2) ?: return
                val value = parts.getOrNull(3) ?: ""
                when (cmd) {
                    "pause" -> {
                        Prefs.pauseEndTime = if (value == "1") Long.MAX_VALUE else 0L
                    }
                    "limit" -> {
                        value.toIntOrNull()?.let { Prefs.dailyLimit = it }
                    }
                }
            }
        }
    }
}
