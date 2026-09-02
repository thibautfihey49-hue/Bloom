package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context?, i: Intent?) {
        if (i?.action == Intent.ACTION_BOOT_COMPLETED && c != null) {
            val s = Intent(c, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(s)
            else c.startService(s)
        }
    }
}
