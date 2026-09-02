package com.bloom.parental.data
import android.content.Context
import android.content.SharedPreferences
object Prefs {
    private lateinit var p: SharedPreferences
    fun init(ctx: Context) { p = ctx.getSharedPreferences("BloomPrefs", Context.MODE_PRIVATE) }
    fun getRole(ctx: Context): String = p.getString("role", "NONE") ?: "NONE"
    fun setRole(ctx: Context, r: String) = p.edit().putString("role", r).apply()
    fun getDailyLimit(ctx: Context): Int = p.getInt("limit", 240)
    fun setDailyLimit(ctx: Context, m: Int) = p.edit().putInt("limit", m).apply()
    fun getTodayUsed(ctx: Context): Int = p.getInt("used", 0)
    fun setTodayUsed(ctx: Context, m: Int) = p.edit().putInt("used", m).apply()
    fun isPaused(ctx: Context): Boolean = p.getBoolean("paused", false)
    fun setPaused(ctx: Context, v: Boolean) = p.edit().putBoolean("paused", v).apply()
}
