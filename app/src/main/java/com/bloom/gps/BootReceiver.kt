package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BloomGPS", "🔄 Téléphone démarré — Récepteur SMS ACTIVÉ")
            // Le récepteur est déjà déclaré dans le manifest, il sera actif
        }
    }
}
