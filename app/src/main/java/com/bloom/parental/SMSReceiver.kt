package com.bloom.parental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.preference.PreferenceManager

class SMSReceiver : BroadcastReceiver() {
    private val CODE = "BLOOM49"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val de = msg.originatingAddress ?: ""
            val texte = msg.messageBody.trim()
            Log.d("BLOOM-SMS", "📩 $de → $texte")

            when {
                // ═══ COMMANDES DU PARENT VERS L'ENFANT ═══
                texte.startsWith("BLOOM_LOC_") -> {
                    abortBroadcast()
                    if (texte.removePrefix("BLOOM_LOC_") == CODE) {
                        LocationService.demanderPosition(context, de)
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }
                texte.startsWith("BLOOM_TIME_") -> {
                    abortBroadcast()
                    val parts = texte.removePrefix("BLOOM_TIME_").split("_")
                    if (parts.size >= 2 && parts[0] == CODE) {
                        val h = parts[1].toIntOrNull() ?: 2
                        ScreenTimeService.definirLimite(context, h)
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }
                texte.startsWith("BLOOM_STOP_") -> {
                    abortBroadcast()
                    if (texte.removePrefix("BLOOM_STOP_") == CODE) {
                        ScreenTimeService.couperAcces(context)
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }
                texte.startsWith("BLOOM_BLOCAGE:") -> {
                    abortBroadcast()
                    val p = texte.removePrefix("BLOOM_BLOCAGE:").split(":", limit=3)
                    if (p.size >= 3 && p[0] == CODE) {
                        gererBlocage(context, p[1], p[2] == "1")
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }
                texte.startsWith("BLOOM_INSTALL_OK:") -> {
                    abortBroadcast()
                    val p = texte.removePrefix("BLOOM_INSTALL_OK:").split(":", limit=2)
                    if (p.size >= 2 && p[0] == CODE) {
                        gererReponseInstall(context, p[1], true)
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }
                texte.startsWith("BLOOM_INSTALL_NON:") -> {
                    abortBroadcast()
                    val p = texte.removePrefix("BLOOM_INSTALL_NON:").split(":", limit=2)
                    if (p.size >= 2 && p[0] == CODE) {
                        gererReponseInstall(context, p[1], false)
                        sauvegarderNum(context, "NUM_PARENT", de)
                    }
                }

                // ═══ DEMANDES DE L'ENFANT VERS LE PARENT ═══
                texte.startsWith("BLOOM_DEMAND_TEMPS:$CODE") -> {
                    context.sendBroadcast(Intent("BLOOM_DEMANDE_TEMPS").putExtra("numEnfant", de))
                }
                texte.startsWith("BLOOM_DEMAND_INSTALL:") -> {
                    val p = texte.removePrefix("BLOOM_DEMAND_INSTALL:").split(":", limit=2)
                    if (p.size >= 2 && p[0] == CODE) {
                        context.sendBroadcast(Intent("BLOOM_DEMANDE_INSTALL")
                            .putExtra("numEnfant", de).putExtra("nomApp", p[1]))
                    }
                }
                texte.startsWith("BLOOM_POS:") -> {
                    context.sendBroadcast(Intent("BLOOM_POSITION").putExtra("numEnfant", de).putExtra("coord", texte))
                }
            }
        }
    }

    private fun sauvegarderNum(ctx: Context, cle: String, valeur: String) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString(cle, valeur).apply()
    }

    private fun gererBlocage(ctx: Context, pkg: String, bloquer: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val set = prefs.getStringSet("APPS_BLOQUEES", mutableSetOf())!!.toMutableSet()
        if (bloquer) set.add(pkg) else set.remove(pkg)
        prefs.edit().putStringSet("APPS_BLOQUEES", set).apply()
    }

    private fun gererReponseInstall(ctx: Context, pkg: String, ok: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val attente = prefs.getStringSet("APPS_EN_ATTENTE", mutableSetOf())!!.toMutableSet()
        val autorisees = prefs.getStringSet("APPS_AUTORISEES", mutableSetOf())!!.toMutableSet()
        attente.remove(pkg)
        if (ok) autorisees.add(pkg) else autorisees.remove(pkg)
        prefs.edit().putStringSet("APPS_EN_ATTENTE", attente).putStringSet("APPS_AUTORISEES", autorisees).apply()
    }
}
