package com.bloom.parental.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ChildManager {
    private fun p(ctx: Context) = ctx.getSharedPreferences("bloom_kids", Context.MODE_PRIVATE)
    private val gson = Gson()
    fun getChildren(ctx: Context): List<ChildProfile> {
        val j = p(ctx).getString("list", "[]")
        return gson.fromJson(j, object : TypeToken<List<ChildProfile>>() {}.type) ?: emptyList()
    }
    fun saveChild(ctx: Context, c: ChildProfile) {
        val l = getChildren(ctx).filter { it.id != c.id } + c
        p(ctx).edit().putString("list", gson.toJson(l)).apply()
    }
    fun deleteChild(ctx: Context, n: String) {
        p(ctx).edit().putString("list", gson.toJson(getChildren(ctx).filter { it.name != n })).apply()
    }
    fun setPaused(ctx: Context, n: String, v: Boolean) {
        val l = getChildren(ctx).map { if(it.name==n) it.copy(isPaused=v) else it }
        p(ctx).edit().putString("list", gson.toJson(l)).apply()
    }
}
