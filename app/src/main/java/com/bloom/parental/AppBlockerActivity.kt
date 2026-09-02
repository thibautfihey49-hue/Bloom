package com.bloom.parental

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppBlockerActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptateur: ListeAppsAdaptateur
    private val listeApps = mutableListOf<InfoApp>()
    private val appsBloquees = mutableSetOf<String>()
    private var numeroEnfant = ""

    data class InfoApp(
        val nomPackage: String,
        val nomAffichage: String,
        var estBloquee: Boolean
    )

    inner class ListeAppsAdaptateur(
        private val liste: MutableList<InfoApp>
    ) : RecyclerView.Adapter<ListeAppsAdaptateur.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nomApp: TextView = view.findViewById(R.id.nom_app)
            val checkboxBloquer: CheckBox = view.findViewById(R.id.checkbox_bloquer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_bloquable, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = liste[position]
            holder.nomApp.text = app.nomAffichage
            holder.checkboxBloquer.isChecked = app.estBloquee
            holder.checkboxBloquer.setOnCheckedChangeListener { _, estCoche ->
                app.estBloquee = estCoche
                mettreAJourBlocage(app.nomPackage, estCoche)
            }
        }

        override fun getItemCount(): Int = liste.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_blocker)
        numeroEnfant = intent.getStringExtra("numEnfant") ?: ""
        
        recyclerView = findViewById(R.id.recycler_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        chargerListeApps()
        adaptateur = ListeAppsAdaptateur(listeApps)
        recyclerView.adapter = adaptateur
    }

    private fun chargerListeApps() {
        val pm = packageManager
        appsBloquees.addAll(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet("APPS_BLOQUEES", emptySet()) ?: emptySet()
        )
        
        val toutesLesApps = pm.getInstalledApplications(0)
        for (app in toutesLesApps) {
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                listeApps.add(
                    InfoApp(
                        nomPackage = app.packageName,
                        nomAffichage = app.loadLabel(pm).toString(),
                        estBloquee = appsBloquees.contains(app.packageName)
                    )
                )
            }
        }
        listeApps.sortBy { it.nomAffichage.lowercase() }
    }

    private fun mettreAJourBlocage(packageNom: String, bloquer: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val ensemble = appsBloquees.toMutableSet()
        if (bloquer) ensemble.add(packageNom) else ensemble.remove(packageNom)
        prefs.edit().putStringSet("APPS_BLOQUEES", ensemble).apply()
        
        // Envoyer la commande à l'enfant par SMS
        if (numeroEnfant.isNotEmpty()) {
            envoyerCommandeSMS(packageNom, bloquer)
        }
    }

    private fun envoyerCommandeSMS(packageNom: String, bloquer: Boolean) {
        val sms = android.telephony.SmsManager.getDefault()
        val code = "BLOOM49"
        val valeur = if (bloquer) "1" else "0"
        sms.sendTextMessage(numeroEnfant, null, "BLOOM_BLOCAGE:$code:$packageNom:$valeur", null, null)
    }
}
