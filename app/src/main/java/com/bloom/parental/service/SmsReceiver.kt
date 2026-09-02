package com.bloom.parental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.bloom.parental.data.Prefs
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            for (msg in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                val body = msg.messageBody ?: continue
                if (body.startsWith("🌸BLOOM|")) {
                    abortBroadcast()
                    processCommand(ctx, body.split("|"))
                }
            }
        }
    }

    private fun processCommand(ctx: Context, parts: List<String>) {
        if (parts.size < 3) return
        when (parts[1]) {
            "CMD" -> {
                val json = JSONObject(parts[2])
                when (json.optString("c")) {
                    "pause" -> Prefs.pauseEndTime = if (json.optString("v") == "1") Long.MAX_VALUE else 0L
                    "limit" -> json.optString("v").toIntOrNull()?.let { Prefs.dailyLimit = it }
                    "applimit" -> JSONObject(json.optString("v")).let {
                        Prefs.setAppLimit(it.optString("pkg"), it.optInt("limit"))
                    }
                    "appblocked" -> JSONObject(json.optString("v")).let {
                        Prefs.setAppBlocked(it.optString("pkg"), it.optBoolean("blocked"))
                    }
                    "appapprove" -> JSONObject(json.optString("v")).let {
                        Prefs.removePendingApp(it.optString("pkg"))
                        Toast.makeText(ctx, if (it.optBoolean("approved")) "✅ Autorisé" else "🚫 Refusé", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
