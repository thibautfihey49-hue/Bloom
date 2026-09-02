package com.bloom.parental.data
import android.content.Context
import org.json.JSONArray
data class BlockedApp(val pkg: String, val name: String, val blocked: Boolean)
object AppBlocker {
    private fun p(ctx: Context) = ctx.getSharedPreferences("BloomBlock", Context.MODE_PRIVATE)
    fun setAppBlocked(ctx: Context, pkg: String, name: String, b: Boolean) {
        val l = getBlockedApps(ctx).filter { it.pkg != pkg } + BlockedApp(pkg, name, b)
        val a = JSONArray()
        l.forEach { a.put(org.json.JSONObject().apply {
            put("pkg", it.pkg); put("name", it.name); put("blocked", it.blocked)
        })}
        p(ctx).edit().putString("list", a.toString()).apply()
    }
    fun isAppBlocked(ctx: Context, pkg: String): Boolean = getBlockedApps(ctx).find { it.pkg == pkg }?.blocked ?: false
    fun getBlockedApps(ctx: Context): List<BlockedApp> = try {
        val a = JSONArray(p(ctx).getString("list", "[]")!!)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            BlockedApp(o.getString("pkg"), o.getString("name"), o.getBoolean("blocked"))
        }
    } catch(e: Exception) { emptyList() }
}
