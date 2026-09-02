package com.bloom.parental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppUsageActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptateur: ListeUsageAdaptateur
    private val listeUsage = mutableListOf<InfoUsage>()

    data class InfoUsage(
        val nomApp: String,
        val tempsSecondes: Long
    )

    inner class ListeUsageAdaptateur(
        private val liste: List<InfoUsage>
    ) : RecyclerView.Adapter<ListeUsageAdaptateur.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nomApp: TextView = view.findViewById(R.id.nom_app)
            val tempsUtilisation: TextView = view.findViewById(R.id.temps_utilisation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_usage_app, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val info = liste[position]
            holder.nomApp.text = info.nomApp
            holder.tempsUtilisation.text = formaterTemps(info.tempsSecondes)
        }

        override fun getItemCount(): Int = liste.size

        private fun formaterTemps(secondes: Long): String {
            val h = secondes / 3600
            val m = (secondes % 3600) / 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}min"
                h > 0 -> "${h}h"
                m > 0 -> "${m}min"
                else -> "${secondes}s"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_usage)
        
        recyclerView = findViewById(R.id.recycler_usage)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        chargerDonneesUsage()
        adaptateur = ListeUsageAdaptateur(listeUsage)
        recyclerView.adapter = adaptateur
    }

    private fun chargerDonneesUsage() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val pm = packageManager
        
        // Parcourir toutes les préférences pour trouver les temps d'utilisation
        val toutesPrefs = prefs.all
        for ((cle, valeur) in toutesPrefs) {
            if (cle.startsWith("TEMPS_APP_") && valeur is Long) {
                val packageNom = cle.removePrefix("TEMPS_APP_")
                val tempsSec = valeur / 1000 // ms → secondes
                if (tempsSec > 0) {
                    val nomAffichage = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageNom, 0)).toString()
                    } catch (e: Exception) {
                        packageNom
                    }
                    listeUsage.add(InfoUsage(nomAffichage, tempsSec))
                }
            }
        }
        // Trier par temps décroissant
        listeUsage.sortByDescending { it.tempsSecondes }
    }
}
