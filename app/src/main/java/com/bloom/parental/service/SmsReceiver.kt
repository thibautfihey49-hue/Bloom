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
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val body = msg.messageBody ?: continue
                val sender = msg.originatingAddress ?: continue

                if (body.startsWith("🌸BLOOM|")) {
                    abortBroadcast()
                    processCommand(ctx, sender, body)
                }
            }
        }
    }

    private fun processCommand(ctx: Context, sender: String, body: String) {
        val parts = body.split("|")
        if (parts.size < 3) return
        val type = parts[1]
        val payload = parts[2]

        when (type) {
            "CMD" -> {
                try {
                    val json = JSONObject(payload)
                    val cmd = json.optString("c", "")
                    val value = json.optString("v", "")
                    when (cmd) {
                        "pause" -> Prefs.pauseEndTime = if (value == "1") Long.MAX_VALUE else 0L
                        "limit" -> value.toIntOrNull()?.let { Prefs.dailyLimit = it }
                        "applimit" -> {
                            val appJson = JSONObject(value)
                            val pkg = appJson.optString("pkg", "")
                            val lim = appJson.optInt("limit", 0)
                            if (pkg.isNotEmpty()) Prefs.setAppLimit(pkg, lim)
                        }
                        "appblocked" -> {
                            val appJson = JSONObject(value)
                            val pkg = appJson.optString("pkg", "")
                            val blocked = appJson.optBoolean("blocked", false)
                            if (pkg.isNotEmpty()) Prefs.setAppBlocked(pkg, blocked)
                        }
                        "appapprove" -> {
                            val appJson = JSONObject(value)
                            val pkg = appJson.optString("pkg", "")
                            val approved = appJson.optBoolean("approved", false)
                            if (pkg.isNotEmpty()) {
                                Prefs.removePendingApp(pkg)
                                Toast.makeText(ctx, if (approved) "✅ Installation autorisée" else "🚫 Installation refusée", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
            "USAGE", "LOC", "NEWAPP" -> BloomSmsManager.processMessage(sender, body)
        }
    }
}
