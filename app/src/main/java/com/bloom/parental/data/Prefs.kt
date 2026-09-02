package com.bloom.parental.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object Prefs {
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("bloom_prefs", Context.MODE_PRIVATE)
    }

    var isParent: Boolean
        get() = prefs.getBoolean("is_parent", false)
        set(value) = prefs.edit().putBoolean("is_parent", value).apply()

    var otherPhone: String
        get() = prefs.getString("other_phone", "") ?: ""
        set(value) = prefs.edit().putString("other_phone", value).apply()

    fun getOtherPhone(ctx: Context): String? = otherPhone.ifEmpty { null }

    var dailyLimit: Int
        get() = prefs.getInt("daily_limit", 240)
        set(value) = prefs.edit().putInt("daily_limit", value).apply()

    var codeSecret: String
        get() = prefs.getString("code_secret", "") ?: ""
        set(value) = prefs.edit().putString("code_secret", value).apply()

    var pauseEndTime: Long
        get() = prefs.getLong("pause_end", 0L)
        set(value) = prefs.edit().putLong("pause_end", value).apply()

    private const val APP_LIMITS = "app_limits"
    private const val APP_BLOCKED = "app_blocked"
    private const val PENDING_APPS = "pending_apps"

    fun setAppLimit(packageName: String, minutes: Int) {
        val json = JSONObject(prefs.getString(APP_LIMITS, "{}") ?: "{}")
        json.put(packageName, minutes)
        prefs.edit().putString(APP_LIMITS, json.toString()).apply()
    }

    fun getAppLimit(packageName: String, default: Int = 0): Int {
        val json = JSONObject(prefs.getString(APP_LIMITS, "{}") ?: "{}")
        return json.optInt(packageName, default)
    }

    fun setAppBlocked(packageName: String, blocked: Boolean) {
        val json = JSONObject(prefs.getString(APP_BLOCKED, "{}") ?: "{}")
        if (blocked) json.put(packageName, true) else json.remove(packageName)
        prefs.edit().putString(APP_BLOCKED, json.toString()).apply()
    }

    fun isAppBlocked(packageName: String): Boolean {
        val json = JSONObject(prefs.getString(APP_BLOCKED, "{}") ?: "{}")
        return json.optBoolean(packageName, false)
    }

    data class PendingApp(val packageName: String, val name: String, val description: String, val timestamp: Long)

    fun addPendingApp(pkg: String, name: String, desc: String) {
        val json = JSONObject(prefs.getString(PENDING_APPS, "{}") ?: "{}")
        val appJson = JSONObject().put("name", name).put("description", desc).put("timestamp", System.currentTimeMillis())
        json.put(pkg, appJson)
        prefs.edit().putString(PENDING_APPS, json.toString()).apply()
    }

    fun removePendingApp(pkg: String) {
        val json = JSONObject(prefs.getString(PENDING_APPS, "{}") ?: "{}")
        json.remove(pkg)
        prefs.edit().putString(PENDING_APPS, json.toString()).apply()
    }

    fun getAllPendingApps(): List<PendingApp> {
        val json = JSONObject(prefs.getString(PENDING_APPS, "{}") ?: "{}")
        val list = mutableListOf<PendingApp>()
        json.keys().forEach { pkg ->
            val obj = json.getJSONObject(pkg)
            list.add(PendingApp(pkg, obj.optString("name",""), obj.optString("description",""), obj.optLong("timestamp",0)))
        }
        return list.sortedByDescending { it.timestamp }
    }
}
