package com.bloom.parental.service

import android.content.Context
import android.telephony.SmsManager
import android.widget.Toast
import com.bloom.parental.data.Prefs
import org.json.JSONObject

object BloomSmsManager {
    private fun getPhone(ctx: Context): String? = Prefs.otherPhone.ifEmpty { null }

    fun sendCommand(ctx: Context, cmd: String, value: String) {
        val phone = getPhone(ctx) ?: return
        try {
            val msg = "🌸BLOOM|CMD|{\"c\":\"$cmd\",\"v\":\"$value\"}"
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {
            Toast.makeText(ctx, "❌ SMS non envoyé", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendUsage(ctx: Context, used: Int, limit: Int) {
        val phone = getPhone(ctx) ?: return
        try {
            val json = JSONObject().put("used", used).put("limit", limit)
            val msg = "🌸BLOOM|USAGE|$json"
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {}
    }

    fun sendLocation(ctx: Context, lat: Double, lng: Double, acc: Int) {
        val phone = getPhone(ctx) ?: return
        try {
            val json = JSONObject().put("lat", lat).put("lng", lng).put("acc", acc)
            val msg = "🌸BLOOM|LOC|$json"
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {}
    }

    fun sendDb(ctx: Context, db: Int) {
        val phone = getPhone(ctx) ?: return
        try {
            val json = JSONObject().put("db", db)
            val msg = "🌸BLOOM|DB|$json"
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {}
    }

    fun sendNewApp(ctx: Context, pkg: String, name: String, desc: String) {
        val phone = getPhone(ctx) ?: return
        try {
            val json = JSONObject().put("pkg", pkg).put("name", name).put("description", desc)
            val msg = "🌸BLOOM|NEWAPP|$json"
            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {}
    }
}
