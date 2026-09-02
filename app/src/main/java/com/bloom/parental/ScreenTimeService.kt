package com.bloom.parental

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

class ScreenTimeService {
    companion object {
        fun definirLimite(contexte: Context, heures: Int) {
            val limites = PreferenceManager.getDefaultSharedPreferences(contexte).edit()
            val heuresSures = heures.coerceIn(0, 24)
            limites.putInt("LIMITE_HEURES", heuresSures)
            limites.putLong("TEMPS_FIN",
                if (heuresSures > 0) System.currentTimeMillis() + heuresSures * 3600000L
                else Long.MAX_VALUE)
            limites.apply()
            Log.d("BLOOM-TIME", "Limite : $heuresSures heure(s)")
        }

        fun couperAcces(contexte: Context) {
            val limites = PreferenceManager.getDefaultSharedPreferences(contexte).edit()
            limites.putLong("TEMPS_FIN", 0)
            limites.apply()
            Log.d("BLOOM-TIME", "Accès coupé")
        }

        fun recupererLimite(contexte: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(contexte)
                .getInt("LIMITE_HEURES", 2)
        }

        fun estTempsEcoule(contexte: Context): Boolean {
            val tempsFin = PreferenceManager.getDefaultSharedPreferences(contexte)
                .getLong("TEMPS_FIN", Long.MAX_VALUE)
            return System.currentTimeMillis() > tempsFin
        }
    }
}
