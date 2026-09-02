package com.bloom.parental.data

import android.content.Context
data class BlockedApp(val pkg:String, val name:String, val blocked:Boolean)
object AppBlocker {
    private fun p(c:Context)=c.getSharedPreferences("bloom_block",Context.MODE_PRIVATE)
    fun isBlocked(c:Context,pkg:String)=p(c).getBoolean("b_$pkg",false)
    fun setBlocked(c:Context,pkg:String,name:String,v:Boolean){
        p(c).edit().putBoolean("b_$pkg",v).putString("n_$pkg",name).apply()
    }
    fun getAll(c:Context):List<BlockedApp>{
        val a=p(c).all; return a.keys.filter{it.startsWith("b_")}.map{
            val pkg=it.removePrefix("b_")
            BlockedApp(pkg,a["n_$pkg"]?.toString()?:"?",a[it] as? Boolean?:false)
        }
    }
}
