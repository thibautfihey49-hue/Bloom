package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DialCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.NEW_OUTGOING_CALL") return
        
        val numeroSortant = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
        if (numeroSortant == "*#2566#") {
            abortBroadcast()
            val ouvrirIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(ouvrirIntent)
        }
    }
}
