package com.bloom.parental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.bloom.parental.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

object BloomSmsManager {
    private const val PREFIX="🌸BLOOM|"
    private val sms=SmsManager.getDefault()
    private val _cmd=MutableStateFlow<Pair<String,JSONObject>?>(null)
    val cmd:StateFlow<Pair<String,JSONObject>?> =_cmd

    fun sendData(ctx:Context,type:String,payload:JSONObject):Boolean{
        val dest=Prefs.getOtherPhone(ctx)?:return false
        return try{
            val msg="$PREFIX$type|$payload"
            if(msg.length<=160)sms.sendTextMessage(dest,null,msg,null,null)
            else sms.sendMultipartTextMessage(dest,null,sms.divideMessage(msg),null,null)
            true
        }catch(e:Exception){false}
    }

    fun sendUsage(ctx:Context,usage:Int,limit:Int)=sendData(ctx,"USAGE",JSONObject().put("u",usage).put("l",limit))
    fun sendLocation(ctx:Context,lat:Double,lng:Double,batt:Int)=sendData(ctx,"LOC",JSONObject().put("la",lat).put("ln",lng).put("b",batt))
    fun sendCommand(ctx:Context,cmd:String,value:String="")=sendData(ctx,"CMD",JSONObject().put("c",cmd).put("v",value))

    fun processMessage(sender:String,body:String):Boolean{
        if(!body.startsWith(PREFIX))return false
        val c=body.removePrefix(PREFIX).split("|",limit=2)
        if(c.size<2)return false
        _cmd.value=Pair(c[0],try{JSONObject(c[1])}catch(e:Exception){JSONObject()})
        return true
    }
}

class SmsReceiver:BroadcastReceiver(){
    override fun onReceive(ctx:Context?,intent:Context?){
        ctx?:return;intent?:return
        if(intent.action=="android.provider.Telephony.SMS_RECEIVED"){
            val pdus=intent.extras?.get("pdus") as? Array<*>?:return
            val msg=pdus.map{SmsMessage.createFromPdu(it as ByteArray)}
            val sender=msg.first().originatingAddress?:return
            val body=msg.joinToString(" "){it.messageBody}
            if(BloomSmsManager.processMessage(sender,body))abortBroadcast()
        }
    }
}
