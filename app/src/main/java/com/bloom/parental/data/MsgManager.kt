package com.bloom.parental.data
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
object MsgManager {
    private lateinit var p:android.content.SharedPreferences
    private val gson=Gson()
    fun init(c:Context){p=c.getSharedPreferences("bloom_msg",Context.MODE_PRIVATE)}
    fun send(c:Context,f:String,t:String,text:String){
        init(c)
        val l=all(c)+Message(from=f,to=t,content=text)
        p.edit().putString("mlist",gson.toJson(l.takeLast(50))).apply()
    }
    fun all(c:Context):List<Message>{
        init(c)
        val j=p.getString("mlist","[]")
        return gson.fromJson(j,object:TypeToken<List<Message>>(){}.type)?:emptyList()
    }
}
