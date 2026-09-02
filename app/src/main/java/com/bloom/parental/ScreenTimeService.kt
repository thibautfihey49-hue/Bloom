package com.bloom.parental

import android.content.Context
import androidx.preference.PreferenceManager

class ScreenTimeService {
    companion object {
        fun definirLimite(c: Context, heures: Int) {
            val h = heures.coerceIn(0, 24)
            PreferenceManager.getDefaultSharedPreferences(c).edit()
                .putInt("LIMITE_HEURES", h)
                .putLong("TEMPS_FIN", if (h > 0) System.currentTimeMillis() + h*3600000L else Long.MAX_VALUE)
                .apply()
        }
        fun couperAcces(c: Context) {
            PreferenceManager.getDefaultSharedPreferences(c).edit().putLong("TEMPS_FIN", 0).apply()
        }
        fun tempsEcoule(c: Context): Boolean = 
            System.currentTimeMillis() > PreferenceManager.getDefaultSharedPreferences(c)
                .getLong("TEMPS_FIN", Long.MAX_VALUE)
    }
}
