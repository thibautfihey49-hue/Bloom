package com.bloom.parental.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var p: SharedPreferences
    fun init(ctx: Context) { p = ctx.getSharedPreferences("bloom", Context.MODE_PRIVATE) }
    fun setRole(ctx: Context, r: String) { init(ctx); p.edit().putString("role", r).apply() }
    fun getRole(ctx: Context): String { init(ctx); return p.getString("role", "NONE") ?: "NONE" }
    fun setDailyLimit(ctx: Context, v: Int) { init(ctx); p.edit().putInt("limit", v).apply() }
    fun getDailyLimit(ctx: Context): Int { init(ctx); return p.getInt("limit", 240) }
    fun setTodayUsed(ctx: Context, v: Int) { init(ctx); p.edit().putInt("used", v).apply() }
    fun getTodayUsed(ctx: Context): Int { init(ctx); return p.getInt("used", 0) }
    fun setPaused(ctx: Context, v: Boolean) { init(ctx); p.edit().putBoolean("paused", v).apply() }
    fun isPaused(ctx: Context): Boolean { init(ctx); return p.getBoolean("paused", false) }
    fun setOtherPhone(ctx: Context, num: String) { init(ctx); p.edit().putString("other_phone", num).apply() }
    fun getOtherPhone(ctx: Context): String? { init(ctx); return p.getString("other_phone", null) }
}
