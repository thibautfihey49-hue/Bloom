package com.bloom.parental

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bloom.parental.data.*
import com.bloom.parental.service.BloomSmsManager
import com.bloom.parental.service.UsageStatsMonitor
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent { BloomApp() }
    }
}

fun hasUsageStatsPermission(ctx: Context): Boolean = try {
    val ops=ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),ctx.packageName)==AppOpsManager.MODE_ALLOWED
} catch(e:Exception){false}

fun hasLocationPermission(ctx: Context): Boolean =
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    else true

val BloomPrimary=Color(0xFF6366F1)
val BloomSecondary=Color(0xFF10B981)
val BloomAccent=Color(0xFFF59E0B)
val BloomError=Color(0xFFEF4444)
val BloomBg=Color(0xFF0F172A)
val BloomSurface=Color(0xFF1E293B)
val BloomText=Color(0xFFF8FAFC)
val BloomTextSec=Color(0xFF94A3B8)

fun formatMinutes(t:Int):String{val h=t/60;val m=t%60;return if(h>0)"${h}h${String.format("%02d",m)}" else "${m}min"}

@Composable
fun BloomTheme(c:@Composable ()->Unit)=MaterialTheme(colorScheme=darkColorScheme(
    primary=BloomPrimary,secondary=BloomSecondary,error=BloomError,
    background=BloomBg,surface=BloomSurface,onPrimary=Color.White,
    onBackground=BloomText,onSurface=BloomText
),content=c)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomApp(){
    val ctx=LocalContext.current
    var role by remember{mutableStateOf(Prefs.getRole(ctx))}
    BloomTheme{
        when(role){
            "NONE"->RoleSelectionScreen{Prefs.setRole(ctx,it);role=it}
            "PARENT"->ParentScreen{Prefs.setRole(ctx,"NONE");role="NONE"}
            "ENFANT"->ChildScreen{Prefs.setRole(ctx,"NONE");role="NONE"}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(onRoleSelected:(String)->Unit){
    val ctx=LocalContext.current
    Box(modifier=Modifier.fillMaxSize().background(BloomBg).padding(32.dp),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(48.dp)){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Box(Modifier.size(100.dp).clip(CircleShape).background(BloomPrimary.copy(0.15f)),contentAlignment=Alignment.Center){
                    Text("🌸",fontSize=52.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("Bloom",style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Bold,color=BloomText)
                Text("Contrôle parental • SMS direct",color=BloomTextSec,fontSize=14.sp)
            }
            Card(modifier=Modifier.fillMaxWidth().clickable{onRoleSelected("PARENT")},shape=RoundedCornerShape(24.dp)){
                Row(modifier=Modifier.padding(28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)){
                    Box(Modifier.size(56.dp).clip(CircleShape).background(BloomPrimary.copy(0.15f)),contentAlignment=Alignment.Center){
                        Icon(Icons.Filled.Lock,null,tint=BloomPrimary,modifier=Modifier.size(28.dp))
                    }
                    Column{Text("📱 Espace Parent",fontWeight=FontWeight.Bold);Text("Contrôler l'appareil de l'enfant par SMS",color=BloomTextSec,fontSize=13.sp)}
                }
            }
            Card(modifier=Modifier.fillMaxWidth().clickable{onRoleSelected("ENFANT")},shape=RoundedCornerShape(24.dp)){
                Row(modifier=Modifier.padding(28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)){
                    Box(Modifier.size(56.dp).clip(CircleShape).background(BloomSecondary.copy(0.15f)),contentAlignment=Alignment.Center){
                        Icon(Icons.Filled.Person,null,tint=BloomSecondary,modifier=Modifier.size(28.dp))
                    }
                    Column{Text("👶 Espace Enfant",fontWeight=FontWeight.Bold);Text("Partager mon temps d'écran",color=BloomTextSec,fontSize=13.sp)}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(onLogout:()->Unit){
    val ctx=LocalContext.current
    var otherPhone by remember{mutableStateOf(Prefs.getOtherPhone(ctx)?"")}
    var children by remember{mutableStateOf(ChildManager.getChildren(ctx))}
    var usage by remember{mutableStateOf(0)}
    var limit by remember{mutableStateOf(240)}
    var battery by remember{mutableStateOf(100)}
    var lastUpdate by remember{mutableStateOf("Jamais")}

    LaunchedEffect(Unit){
        while(true){
            BloomSmsManager.cmd.collect{cmd->
                if(cmd?.first=="USAGE"){
                    usage=cmd.second.optInt("u",0)
                    limit=cmd.second.optInt("l",240)
                    lastUpdate="À l'instant"
                }
                if(cmd?.first=="LOC"){
                    battery=cmd.second.optInt("b",100)
                }
            }
        }
    }

    BloomTheme{
        LazyColumn(modifier=Modifier.fillMaxSize().background(BloomBg).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
            item{
                Column{
                    Text("🌸 Espace Parent",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text("Connexion par SMS — SANS INTERNET",color=BloomSecondary,fontSize=12.sp)
                }
            }
            item{
                Card(shape=RoundedCornerShape(16.dp)){
                    Column(modifier=Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Text("📱 Numéro de l'appareil ENFANT",fontWeight=FontWeight.Bold)
                        OutlinedTextField(value=otherPhone,onValueChange={otherPhone=it},
                            label={Text("Numéro de téléphone")},
                            placeholder={Text("+336...")},
                            modifier=Modifier.fillMaxWidth(),singleLine=true
                        )
                        Button(onClick={Prefs.setOtherPhone(ctx,otherPhone);children=ChildManager.getChildren(ctx)},
                            modifier=Modifier.fillMaxWidth()){
                            Icon(Icons.Filled.Check,null);Spacer(Modifier.width(8.dp));Text("Enregistrer")
                        }
                    }
                }
            }
            if(otherPhone.isNotEmpty()){
                item{
                    Card(shape=RoundedCornerShape(20.dp)){
                        Column(modifier=Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                Text("📊 Données en temps réel",fontWeight=FontWeight.Bold)
                                Text("MAJ: $lastUpdate",color=BloomTextSec,fontSize=11.sp)
                            }
                            Divider()
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                Column{Text("⏱️ Utilisé aujourd'hui",color=BloomTextSec);Text(formatMinutes(usage),fontSize=FontWeight.Bold,fontSize=20.sp)}
                                Column{Text("🔋 Batterie",color=BloomTextSec);Text("$battery%",fontSize=FontWeight.Bold,fontSize=20.sp)}
                                Column{Text("⏳ Limite",color=BloomTextSec);Text(formatMinutes(limit),fontSize=FontWeight.Bold,fontSize=20.sp)}
                            }
                            Column{
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                    Text("Progression")
                                    Text("${(usage*100/limit.coerceAtLeast(1))}%")
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(progress=usage.toFloat()/limit.coerceAtLeast(1),
                                    modifier=Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color=if(usage*100/limit>=90)BloomError else if(usage*100/limit>=70)BloomAccent else BloomPrimary
                                )
                            }
                            Divider()
                            Text("🎮 Commandes à distance",fontWeight=FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                                Button(onClick={BloomSmsManager.sendCommand(ctx,"pause","1")},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(BloomError)){
                                    Icon(Icons.Filled.Pause,null);Spacer(Modifier.width(4.dp));Text("⏸️ Pause")
                                }
                                Button(onClick={BloomSmsManager.sendCommand(ctx,"pause","0")},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(BloomSecondary)){
                                    Icon(Icons.Filled.PlayArrow,null);Spacer(Modifier.width(4.dp));Text("▶️ Reprendre")
                                }
                            }
                            var mins by remember{mutableStateOf(30f)}
                            Column{
                                Text("⏱️ Nouvelle limite: ${mins.toInt()} min")
                                Slider(value=mins,onValueChange={mins=it},valueRange=15f..480f,steps=29)
                            }
                            Button(onClick={BloomSmsManager.sendCommand(ctx,"limit",mins.toInt().toString())},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Filled.Timer,null);Spacer(Modifier.width(8.dp));Text("⏱️ Envoyer la limite")
                            }
                            Button(onClick={BloomSmsManager.sendCommand(ctx,"loc","1")},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Filled.LocationOn,null);Spacer(Modifier.width(8.dp));Text("📍 Demander la position")
                            }
                        }
                    }
                }
            }
            item{
                Card(shape=RoundedCornerShape(16.dp)){
                    Column(modifier=Modifier.padding(20.dp)){
                        Text("⚙️ Informations",fontWeight=FontWeight.Bold)
                        Divider(Modifier.padding(vertical=12.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                            Text("Mode",color=BloomTextSec);Text("Parent")
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                            Text("Connexion",color=BloomTextSec);Text("📡 SMS direct — SANS INTERNET",color=BloomSecondary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick=onLogout,modifier=Modifier.fillMaxWidth()){
                            Icon(Icons.Filled.Logout,null);Spacer(Modifier.width(8.dp));Text("🔄 Déconnexion")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildScreen(onLogout:()->Unit){
    val ctx=LocalContext.current
    var otherPhone by remember{mutableStateOf(Prefs.getOtherPhone(ctx)?"")}
    val monitor=UsageStatsMonitor(ctx)
    var used by remember{mutableStateOf(monitor.getTodayMinutes())}
    val limit=Prefs.getDailyLimit(ctx)
    val paused=Prefs.isPaused(ctx)
    val rem=(limit-used).coerceAtLeast(0)
    val pct=if(limit>0)used.toFloat()/limit else 0f

    LaunchedEffect(Unit){
        while(true){
            used=monitor.getTodayMinutes()
            Prefs.setTodayUsed(ctx,used)
            if(otherPhone.isNotEmpty()){
                BloomSmsManager.sendUsage(ctx,used,limit)
            }
            delay(5*60*1000)
        }
    }

    LaunchedEffect(Unit){
        BloomSmsManager.cmd.collect{cmd->
            if(cmd?.first=="CMD"){
                val c=cmd.second.optString("c","")
                val v=cmd.second.optString("v","")
                when(c){
                    "pause"->Prefs.setPaused(ctx,v=="1")
                    "limit"->Prefs.setDailyLimit(ctx,v.toIntOrNull()?:240)
                }
            }
        }
    }

    BloomTheme{
        Box(modifier=Modifier.fillMaxSize().background(BloomBg).padding(24.dp)){
            LazyColumn(modifier=Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(24.dp)){
                item{
                    Column{
                        Text("🌸 Espace Enfant",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                        Text("Données envoyées par SMS — SANS INTERNET",color=BloomSecondary,fontSize=12.sp)
                    }
                }
                item{
                    Card(shape=RoundedCornerShape(16.dp)){
                        Column(modifier=Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            Text("📱 Numéro de l'appareil PARENT",fontWeight=FontWeight.Bold)
                            OutlinedTextField(value=otherPhone,onValueChange={otherPhone=it},
                                label={Text("Numéro de téléphone")},
                                placeholder={Text("+336...")},
                                modifier=Modifier.fillMaxWidth(),singleLine=true
                            )
                            Button(onClick={Prefs.setOtherPhone(ctx,otherPhone)},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Filled.Check,null);Spacer(Modifier.width(8.dp));Text("✅ Enregistrer — Envoi automatique toutes les 5min")
                            }
                        }
                    }
                }
                if(otherPhone.isNotEmpty()){
                    item{
                        if(paused){
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp),
                                modifier=Modifier.fillMaxSize().padding(top=40.dp)){
                                Text("🔒",fontSize=80.sp)
                                Text("APPAREIL EN PAUSE",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=BloomError)
                                Text("Le parent a suspendu l'utilisation",color=BloomTextSec,textAlign=TextAlign.Center)
                            }
                        }else{
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(32.dp)){
                                Box(modifier=Modifier.size(220.dp),contentAlignment=Alignment.Center){
                                    CircularProgressIndicator(progress=pct,modifier=Modifier.fillMaxSize(),
                                        strokeWidth=16.dp,
                                        color=if(pct>=0.9f)BloomError else if(pct>=0.7f)BloomAccent else BloomPrimary,
                                        trackColor=Color(0xFF1E293B)
                                    )
                                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                                        Text(formatMinutes(rem),style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Bold)
                                        Text("disponibles",color=BloomTextSec,fontSize=16.sp)
                                    }
                                }
                                Card(shape=RoundedCornerShape(16.dp)){
                                    Column(modifier=Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                        Text("Résumé du jour",fontWeight=FontWeight.Bold,fontSize=16.sp)
                                        Spacer(Modifier.height(16.dp))
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                            Text("⏱️ Utilisé")
                                            Text(formatMinutes(used),fontWeight=FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                            Text("🎯 Limite")
                                            Text(formatMinutes(limit),fontWeight=FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                            Text("⏳ Restant")
                                            Text(formatMinutes(rem),fontWeight=FontWeight.Bold,color=BloomPrimary)
                                        }
                                    }
                                }
                                Card(shape=RoundedCornerShape(16.dp)){
                                    Column(modifier=Modifier.padding(16.dp)){
                                        Text("📡 État de la connexion SMS",fontWeight=FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        Row{
                                            Text("✅",color=BloomSecondary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Données envoyées au parent toutes les 5 minutes",color=BloomSecondary)
                                        }
                                        Row{
                                            Text("✅",color=BloomSecondary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Commandes reçues instantanément",color=BloomSecondary)
                                        }
                                        Row{
                                            Text("✅",color=BloomSecondary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Aucune connexion Internet nécessaire",color=BloomSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item{
                    Card(shape=RoundedCornerShape(16.dp)){
                        Column(modifier=Modifier.padding(20.dp)){
                            Text("⚙️ Permissions requises",fontWeight=FontWeight.Bold)
                            Divider(Modifier.padding(vertical=12.dp))
                            if(!hasUsageStatsPermission(ctx)){
                                Button(onClick={ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))},modifier=Modifier.fillMaxWidth()){
                                    Icon(Icons.Filled.Info,null);Spacer(Modifier.width(8.dp));Text("📊 Accès statistiques d'utilisation")
                                }
                            }else{
                                Row{Text("✅");Spacer(Modifier.width(8.dp));Text("Statistiques d'utilisation : OK")}
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick={ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${ctx.packageName}")))},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Filled.Sms,null);Spacer(Modifier.width(8.dp));Text("📩 Autoriser l'envoi et la réception SMS")
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick={ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Filled.Settings,null);Spacer(Modifier.width(8.dp));Text("♿ Activer le service d'accessibilité")
                            }
                        }
                    }
                }
                item{
                    Button(onClick=onLogout,modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Filled.Logout,null);Spacer(Modifier.width(8.dp));Text("🔄 Changer de profil")
                    }
                }
            }
        }
    }
}
