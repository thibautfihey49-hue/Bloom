package com.bloom.parental.data
import java.text.SimpleDateFormat;import java.util.*
data class Message(val id:Long=System.currentTimeMillis(),val from:String,val to:String,val content:String,val ts:Long=System.currentTimeMillis()){
    fun time()=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(ts))
}
