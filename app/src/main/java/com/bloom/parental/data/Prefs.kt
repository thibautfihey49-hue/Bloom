package com.bloom.parental.data

import android.content.Context
import android.content.SharedPreferences

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

    var dailyLimit: Int
        get() = prefs.getInt("daily_limit", 240)
        set(value) = prefs.edit().putInt("daily_limit", value).apply()

    var codeSecret: String
        get() = prefs.getString("code_secret", "") ?: ""
        set(value) = prefs.edit().putString("code_secret", value).apply()

    var pauseEndTime: Long
        get() = prefs.getLong("pause_end", 0L)
        set(value) = prefs.edit().putLong("pause_end", value).apply()
}
