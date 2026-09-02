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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bloom.parental.data.Prefs
import com.bloom.parental.service.*
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        Toast.makeText(this, if (allGranted) "✅ Permissions accordées !" else "⚠️ Certaines permissions manquent", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent { BloomApp() }
        checkOrRequestPermissions()
    }

    fun checkOrRequestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.SEND_SMS)
            needed.add(Manifest.permission.RECEIVE_SMS)
            needed.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                needed.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) requestPermissionLauncher.launch(needed.toTypedArray())
        if (!hasUsageStatsPermission(this)) {
            Toast.makeText(this, "⚠️ Autorise l'accès aux données d'usage", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

fun hasUsageStatsPermission(ctx: Context): Boolean {
    return try {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }
}

val BloomPrimary = Color(0xFF6366F1)
val BloomSecondary = Color(0xFF10B981)
val BloomAccent = Color(0xFFF59E0B)
val BloomError = Color(0xFFEF4444)
val BloomBg = Color(0xFF0F172A)
val BloomSurface = Color(0xFF1E293B)
val BloomText = Color(0xFFF8FAFC)
val BloomTextSec = Color(0xFF94A3B8)

fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h${String.format("%02d", m)}" else "${m}min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BloomPrimary, secondary = BloomSecondary, error = BloomError,
            background = BloomBg, surface = BloomSurface,
            onPrimary = Color.White, onBackground = BloomText, onSurface = BloomText
        ), content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomApp() {
    val hasOther = Prefs.otherPhone.isNotEmpty()
    val isParentMode = Prefs.isParent
    BloomTheme {
        when {
            !hasOther -> RoleSelectionScreen()
            isParentMode -> ParentScreen()
            else -> ChildScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen() {
    val ctx = LocalContext.current
    var selectedMode by remember { mutableStateOf<String?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var codeSecret by remember { mutableStateOf("") }

    BloomTheme {
        Box(modifier = Modifier.fillMaxSize().background(BloomBg).padding(32.dp), contentAlignment = Alignment.Center) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("🌸", fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Bloom", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = BloomText)
                        Text("Contrôle parental par SMS", color = BloomTextSec, fontSize = 14.sp)
                    }
                }
                item { Text("Choisis ton espace", style = MaterialTheme.typography.titleMedium, color = BloomText) }
                item {
                    Card(modifier = Modifier.fillMaxWidth().clickable { selectedMode = "parent" }.border(
                        width = 2.dp, color = if (selectedMode == "parent") BloomPrimary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Filled.Lock, null, tint = BloomPrimary, modifier = Modifier.size(32.dp))
                            Column { Text("🔒 Espace Parent", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Tableau de bord complet", color = BloomTextSec, fontSize = 13.sp) }
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().clickable { selectedMode = "child" }.border(
                        width = 2.dp, color = if (selectedMode == "child") BloomSecondary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Filled.Person, null, tint = BloomSecondary, modifier = Modifier.size(32.dp))
                            Column { Text("👶 Espace Enfant", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Enfant connecté", color = BloomTextSec, fontSize = 13.sp) }
                        }
                    }
                }
                if (selectedMode != null) {
                    item { Divider(color = BloomSurface, thickness = 1.dp) }
                    if (selectedMode == "parent") {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🔐 Code secret (4 chiffres)", color = BloomText)
                                OutlinedTextField(value = codeSecret, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) codeSecret = it },
                                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("____") },
                                    visualTransformation = PasswordVisualTransformation(), singleLine = true)
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (selectedMode == "parent") "📱 Numéro de l'enfant" else "📱 Numéro du parent", color = BloomText)
                            OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it },
                                modifier = Modifier.fillMaxWidth(), placeholder = { Text("+336...") }, singleLine = true)
                        }
                    }
                    item {
                        Button(onClick = {
                            Prefs.otherPhone = phoneNumber
                            Prefs.isParent = selectedMode == "parent"
                            if (selectedMode == "parent") Prefs.codeSecret = codeSecret
                            Toast.makeText(ctx, "✅ Configuration sauvegardée", Toast.LENGTH_SHORT).show()
                            if (selectedMode == "child") {
                                EnvironmentMonitorService.start(ctx)
                                AppInstallMonitorService.start(ctx)
                            }
                        }, modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = phoneNumber.isNotEmpty() && (selectedMode != "parent" || codeSecret.length == 4)) {
                            Text("✅ Valider", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen() {
    val ctx = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(Prefs.remoteUsage) }
    var limit by remember { mutableStateOf(Prefs.remoteLimit) }
    var pendingApps by remember { mutableStateOf(Prefs.getAllPendingApps()) }
    var childPhone by remember { mutableStateOf(Prefs.otherPhone) }
    var lastLat by remember { mutableStateOf(Prefs.lastLat) }
    var lastLng by remember { mutableStateOf(Prefs.lastLng) }
    var lastAcc by remember { mutableStateOf(Prefs.lastAcc) }
    var lastDb by remember { mutableStateOf(Prefs.lastDb) }

    LaunchedEffect(Unit) {
        while (true) {
            usage = Prefs.remoteUsage
            limit = Prefs.remoteLimit
            lastLat = Prefs.lastLat
            lastLng = Prefs.lastLng
            lastAcc = Prefs.lastAcc
            lastDb = Prefs.lastDb
            pendingApps = Prefs.getAllPendingApps()
            delay(15000)
        }
    }

    BloomTheme {
        when {
            !isUnlocked && Prefs.codeSecret.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().background(BloomBg).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        Text("🔐 Code Secret", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BloomText)
                        OutlinedTextField(value = codeInput, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) codeInput = it },
                            placeholder = { Text("____") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 28.sp),
                            modifier = Modifier.fillMaxWidth(0.6f))
                        Button(onClick = {
                            if (codeInput == Prefs.codeSecret) isUnlocked = true
                            else { Toast.makeText(ctx, "❌ Code incorrect", Toast.LENGTH_SHORT).show(); codeInput = "" }
                        }, modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)) { Text("🔓 Déverrouiller", fontSize = 16.sp) }
                        TextButton(onClick = { Prefs.otherPhone = ""; Prefs.isParent = false; Prefs.codeSecret = "" }) { Text("Réinitialiser", color = BloomTextSec) }
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column {
                            Text("🌸 Tableau de Bord Parent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BloomText)
                            Text("Enfant : $childPhone", color = BloomTextSec, fontSize = 13.sp)
                        }
                    }

                    if (pendingApps.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BloomAccent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("📋 Demandes d'installation", fontWeight = FontWeight.Bold, color = BloomAccent)
                                        Badge(containerColor = BloomAccent) { Text("${pendingApps.size}", color = Color.Black) }
                                    }
                                    Divider()
                                    pendingApps.forEach { app ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(app.name, color = BloomText, fontWeight = FontWeight.Medium)
                                            Row {
                                                TextButton(onClick = { sendAppApproval(ctx, app.packageName, true); pendingApps = Prefs.getAllPendingApps() }) { Text("✅ Autoriser", color = BloomSecondary) }
                                                TextButton(onClick = { sendAppApproval(ctx, app.packageName, false); pendingApps = Prefs.getAllPendingApps() }) { Text("🚫 Refuser", color = BloomError) }
                                            }
                                        }
                                        Divider(color = BloomSurface)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("📊 Temps global", fontWeight = FontWeight.Bold, color = BloomText)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("⏳ Utilisé", color = BloomTextSec); Text(formatMinutes(usage), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BloomPrimary) }
                                    Column { Text("🎯 Limite", color = BloomTextSec); Text(formatMinutes(limit), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BloomAccent) }
                                }
                                Slider(value = limit.toFloat(), onValueChange = { limit = it.toInt() }, valueRange = 30f..480f, steps = 14)
                                Button(onClick = { Prefs.dailyLimit = limit; BloomSmsManager.sendCommand(ctx, "limit", limit.toString()); Toast.makeText(ctx, "✅ Limite envoyée", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("💾 Appliquer la limite")
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🎮 Commandes à distance", fontWeight = FontWeight.Bold, color = BloomText)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { BloomSmsManager.sendCommand(ctx, "pause", "1"); Toast.makeText(ctx, "⏸️ Pause activée", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = BloomAccent)) {
                                        Icon(Icons.Filled.Pause, null); Spacer(Modifier.width(4.dp)); Text("PAUSE")
                                    }
                                    Button(onClick = { BloomSmsManager.sendCommand(ctx, "pause", "0"); Toast.makeText(ctx, "▶️ Reprise", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = BloomSecondary)) {
                                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("REPRENDRE")
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { BloomSmsManager.sendCommand(ctx, "getloc", "1"); Toast.makeText(ctx, "📍 Position demandée", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.LocationOn, null); Spacer(Modifier.width(4.dp)); Text("LOCALISER")
                                    }
                                    Button(onClick = { BloomSmsManager.sendCommand(ctx, "getdb", "1"); Toast.makeText(ctx, "🎤 Niveau sonore demandé", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(4.dp)); Text("ÉCOUTE")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📍 Dernière position", fontWeight = FontWeight.Bold, color = BloomText)
                                if (lastLat != 0.0) {
                                    Text("Lat: $lastLat", color = BloomTextSec)
                                    Text("Lng: $lastLng", color = BloomTextSec)
                                    Text("Précision: ${lastAcc}m", color = BloomTextSec)
                                    val mapUrl = "geo:$lastLat,$lastLng?q=$lastLat,$lastLng"
                                    Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("🗺️ Ouvrir dans Maps")
                                    }
                                } else {
                                    Text("Aucune position reçue", color = BloomTextSec)
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎤 Niveau sonore ambiant", fontWeight = FontWeight.Bold, color = BloomText)
                                if (lastDb > 0) {
                                    Text("$lastDb dB", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BloomSecondary)
                                    Text(when {
                                        lastDb < 40 -> "😴 Silencieux"
                                        lastDb < 60 -> "💬 Normal"
                                        lastDb < 80 -> "🔔 Bruyant"
                                        else -> "🚨 Très bruyant"
                                    }, color = BloomTextSec)
                                } else {
                                    Text("Aucune mesure reçue", color = BloomTextSec)
                                }
                            }
                        }
                    }

                    item {
                        Button(onClick = { Prefs.otherPhone = ""; Prefs.isParent = false; Prefs.codeSecret = ""; Prefs.remoteUsage = 0; Prefs.remoteLimit = 240; Prefs.lastLat = 0.0; Prefs.lastLng = 0.0; Prefs.lastDb = 0; Prefs.lastAcc = 0; Toast.makeText(ctx, "✅ Déconnexion", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BloomError)) {
                            Text("🚪 Déconnexion / Réinitialiser")
                        }
                    }
                }
            }
        }
    }
}

fun sendAppApproval(ctx: Context, pkg: String, approved: Boolean) {
    val json = JSONObject().put("pkg", pkg).put("approved", approved)
    BloomSmsManager.sendCommand(ctx, "appapprove", json.toString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildScreen() {
    val ctx = LocalContext.current
    var usageMonitor = remember { UsageStatsMonitor(ctx) }
    var todayUsed by remember { mutableStateOf(usageMonitor.getTodayMinutes()) }
    var dailyLimit by remember { mutableStateOf(Prefs.dailyLimit) }
    var pauseEndTime by remember { mutableStateOf(Prefs.pauseEndTime) }
    var isPaused by remember { mutableStateOf(pauseEndTime > System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            todayUsed = usageMonitor.getTodayMinutes()
            dailyLimit = Prefs.dailyLimit
            pauseEndTime = Prefs.pauseEndTime
            isPaused = pauseEndTime > System.currentTimeMillis()
            BloomSmsManager.sendUsage(ctx, todayUsed, dailyLimit)
            delay(60000)
        }
    }

    BloomTheme {
        Box(modifier = Modifier.fillMaxSize().background(BloomBg).padding(24.dp), contentAlignment = Alignment.Center) {
            if (isPaused) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("⏸️ PAUSE ACTIVE", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BloomAccent)
                    Text("L'usage est temporairement désactivé", color = BloomTextSec)
                    Text("Reprise à : ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date(pauseEndTime))}", color = BloomTextSec)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    Box(Modifier.size(100.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text("👶", fontSize = 50.sp)
                    }
                    Text("Espace Enfant", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BloomText)

                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("⏱️ Temps restant aujourd'hui", color = BloomTextSec)
                            val remaining = (dailyLimit - todayUsed).coerceAtLeast(0)
                            Text(formatMinutes(remaining), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = if (remaining < 30) BloomError else BloomPrimary)
                            Text("sur ${formatMinutes(dailyLimit)} autorisés", color = BloomTextSec)
                            LinearProgressIndicator(progress = { (todayUsed.toFloat() / dailyLimit.toFloat()).coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = if (todayUsed > dailyLimit) BloomError else BloomPrimary)
                        }
                    }

                    Text("📱 Connecté au parent : ${Prefs.otherPhone}", color = BloomTextSec, fontSize = 13.sp)
                    Text("🌸 Bloom actif — Surveillance en cours", color = BloomSecondary, fontSize = 12.sp)

                    Button(onClick = { Prefs.otherPhone = ""; Prefs.isParent = false; Toast.makeText(ctx, "✅ Déconnecté", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = BloomSurface)) {
                        Text("🔄 Changer de compte", color = BloomText)
                    }
                }
            }
        }
    }
}
