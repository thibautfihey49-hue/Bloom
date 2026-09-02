package com.bloom.parental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.bloom.parental.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

object BloomSmsManager {
    private const val PREFIX = "🌸BLOOM|"
    private val sms = SmsManager.getDefault()
    private val _cmd = MutableStateFlow<Pair<String, JSONObject>?>(null)
    val cmd: StateFlow<Pair<String, JSONObject>?> = _cmd

    fun sendData(ctx: Context, type: String, payload: JSONObject): Boolean {
        val dest = Prefs.getOtherPhone(ctx) ?: return false
        return try {
            val msg = "$PREFIX$type|$payload"
            if (msg.length <= 160) {
                sms.sendTextMessage(dest, null, msg, null, null)
            } else {
                val parts = sms.divideMessage(msg)
                sms.sendMultipartTextMessage(dest, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendUsage(ctx: Context, usage: Int, limit: Int) {
        sendData(ctx, "USAGE", JSONObject().put("u", usage).put("l", limit))
    }

    fun sendLocation(ctx: Context, lat: Double, lng: Double, batt: Int) {
        sendData(ctx, "LOC", JSONObject().put("la", lat).put("ln", lng).put("b", batt))
    }

    fun sendCommand(ctx: Context, cmd: String, value: String = "") {
        sendData(ctx, "CMD", JSONObject().put("c", cmd).put("v", value))
    }

    fun processMessage(sender: String, body: String): Boolean {
        if (!body.startsWith(PREFIX)) return false
        val content = body.removePrefix(PREFIX)
        val parts = content.split("|", limit = 2)
        if (parts.size < 2) return false
        val payload = try {
            JSONObject(parts[1])
        } catch (e: Exception) {
            JSONObject()
        }
        _cmd.value = Pair(parts[0], payload)
        return true
    }
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
            val messages = pdus.map { SmsMessage.createFromPdu(it as ByteArray) }
            val sender = messages.first().originatingAddress ?: return
            val body = messages.joinToString(" ") { it.messageBody }
            if (BloomSmsManager.processMessage(sender, body)) {
                abortBroadcast()
            }
        }
    }
}
