package com.bloom.parental.data
import android.content.Context
import org.json.JSONArray
object ChildManager {
    private fun p(ctx: Context) = ctx.getSharedPreferences("BloomKids", Context.MODE_PRIVATE)
    fun saveChild(ctx: Context, c: ChildProfile) {
        val l = getChildren(ctx).filter { it.id != c.id } + c
        val a = JSONArray()
        l.forEach { a.put(org.json.JSONObject().apply {
            put("id", it.id); put("name", it.name); put("limit", it.dailyLimitMinutes)
            put("used", it.todayUsedMinutes); put("paused", it.isPaused)
        })}
        p(ctx).edit().putString("list", a.toString()).apply()
    }
    fun deleteChild(ctx: Context, n: String) {
        val l = getChildren(ctx).filter { it.name != n }
        val a = JSONArray()
        l.forEach { a.put(org.json.JSONObject().apply {
            put("id", it.id); put("name", it.name); put("limit", it.dailyLimitMinutes)
            put("used", it.todayUsedMinutes); put("paused", it.isPaused)
        })}
        p(ctx).edit().putString("list", a.toString()).apply()
    }
    fun setPaused(ctx: Context, n: String, v: Boolean) {
        val l = getChildren(ctx).map { if (it.name == n) it.copy(isPaused = v) else it }
        val a = JSONArray()
        l.forEach { a.put(org.json.JSONObject().apply {
            put("id", it.id); put("name", it.name); put("limit", it.dailyLimitMinutes)
            put("used", it.todayUsedMinutes); put("paused", it.isPaused)
        })}
        p(ctx).edit().putString("list", a.toString()).apply()
    }
    fun getChildren(ctx: Context): List<ChildProfile> = try {
        val a = JSONArray(p(ctx).getString("list", "[]")!!)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            ChildProfile(o.getString("id"), o.getString("name"),
                o.optInt("limit", 240), o.optInt("used", 0), o.optBoolean("paused", false))
        }
    } catch(e: Exception) { emptyList() }
}
