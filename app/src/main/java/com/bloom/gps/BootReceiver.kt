package com.bloom.gps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BLOOM-DATA", "📱 Téléphone redémarré — l'application se réactive")
            // L'application est prête à recevoir des commandes sans que personne ne l'ouvre
        }
    }
}
