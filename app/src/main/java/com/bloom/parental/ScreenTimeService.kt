package com.bloom.parental

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager

class ScreenTimeService {
    companion object {
        // Définir la limite — 0 à 24h
        fun définirLimite(contexte: Context, heures: Int) {
            val limites = PreferenceManager.getDefaultSharedPreferences(contexte).edit()
            val heuresSûres = heures.coerceIn(0, 24) // 🔒 MAX 24H
            limites.putInt("LIMITE_HEURES", heuresSûres)
            limites.putLong("TEMPS_FIN", 
                if (heuresSûres > 0) System.currentTimeMillis() + heuresSûres * 3600000L 
                else Long.MAX_VALUE)
            limites.apply()
            Log.d("BLOOM-TIME", "⏳ Limite : $heuresSûres heure(s)")
        }
        
        // Couper l'accès immédiatement
        fun couperAccès(contexte: Context) {
            val limites = PreferenceManager.getDefaultSharedPreferences(contexte).edit()
            limites.putLong("TEMPS_FIN", 0)
            limites.apply()
            Log.d("BLOOM-TIME", "⏹️ Accès coupé immédiatement")
        }
        
        // Récupérer la limite configurée
        fun récupérerLimite(contexte: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(contexte)
                .getInt("LIMITE_HEURES", 2)
        }
        
        // Savoir si le temps est écoulé
        fun estTempsÉcoulé(contexte: Context): Boolean {
            val tempsFin = PreferenceManager.getDefaultSharedPreferences(contexte)
                .getLong("TEMPS_FIN", Long.MAX_VALUE)
            return System.currentTimeMillis() > tempsFin
        }
    }
}
