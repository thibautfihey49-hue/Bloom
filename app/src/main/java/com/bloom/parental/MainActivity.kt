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
import com.bloom.parental.service.UsageStatsMonitor
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent { BloomApp() }
    }
}

fun hasUsageStatsPermission(ctx: Context): Boolean = try {
    val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
} catch(e: Exception) { false }

fun hasLocationPermission(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else true

val BloomPrimary = Color(0xFF6366F1)
val BloomSecondary = Color(0xFF10B981)
val BloomAccent = Color(0xFFF59E0B)
val BloomError = Color(0xFFEF4444)
val BloomBg = Color(0xFF0F172A)
val BloomSurface = Color(0xFF1E293B)
val BloomText = Color(0xFFF8FAFC)
val BloomTextSec = Color(0xFF94A3B8)

fun formatMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h${String.format("%02d", m)}" else "${m}min"
}

@Composable
fun BloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = BloomPrimary, secondary = BloomSecondary, error = BloomError,
        background = BloomBg, surface = BloomSurface,
        onPrimary = Color.White, onBackground = BloomText, onSurface = BloomText
    ), content = content)
}

@Composable
fun BloomApp() {
    val ctx = LocalContext.current
    var role by remember { mutableStateOf(Prefs.getRole(ctx)) }
    BloomTheme {
        when(role) {
            "NONE" -> RoleSelectionScreen { Prefs.setRole(ctx, it); role = it }
            "PARENT" -> ParentMainScreen { Prefs.setRole(ctx, "NONE"); role = "NONE" }
            "ENFANT" -> ChildMainScreen { Prefs.setRole(ctx, "NONE"); role = "NONE" }
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(BloomBg).padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(48.dp)) {
            Column(Alignment.CenterHorizontally) {
                Box(Modifier.size(100.dp).clip(CircleShape).background(BloomPrimary.copy(0.15f)), Alignment.Center) {
                    Text("🌸", fontSize = 52.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("Bloom", style = MaterialTheme.typography.displayMedium, FontWeight.Bold, color = BloomText)
                Text("Contrôle parental intelligent", color = BloomTextSec, fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Card(Modifier.fillMaxWidth().clickable { onRoleSelected("PARENT") }, RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(28.dp), Alignment.CenterVertically, Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(BloomPrimary.copy(0.15f)), Alignment.Center) {
                            Icon(Icons.Filled.Lock, null, tint = BloomPrimary, modifier = Modifier.size(28.dp))
                        }
                        Column { Text("Espace Parent", FontWeight.Bold); Text("Gérer les écrans", color = BloomTextSec, fontSize = 13.sp) }
                    }
                }
                Card(Modifier.fillMaxWidth().clickable { onRoleSelected("ENFANT") }, RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(28.dp), Alignment.CenterVertically, Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(BloomSecondary.copy(0.15f)), Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = BloomSecondary, modifier = Modifier.size(28.dp))
                        }
                        Column { Text("Espace Enfant", FontWeight.Bold); Text("Voir mon temps", color = BloomTextSec, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Enfants", "Apps", "Messages", "Paramètres")
    Scaffold(bottomBar = {
        NavigationBar(containerColor = BloomSurface) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(selected = selectedTab == i, onClick = { selectedTab = i },
                    icon = { Icon(when(i) { 0 -> Icons.Filled.Face; 1 -> Icons.Filled.Apps; 2 -> Icons.Filled.Send; else -> Icons.Filled.Settings }, null) },
                    label = { Text(t) })
            }
        }
    }) { p -> Box(Modifier.padding(p)) {
        when(selectedTab) { 0 -> ParentChildrenTab(); 1 -> ParentAppsTab(); 2 -> ParentMessagesTab(); 3 -> ParentSettingsTab(onLogout) }
    }}
}

@Composable
fun ParentChildrenTab() {
    val ctx = LocalContext.current
    var children by remember { mutableStateOf(ChildManager.getChildren(ctx)) }
    var showAddDialog by remember { mutableStateOf(false) }
    if (showAddDialog) AddChildDialog({ showAddDialog = false }) { n, l ->
        ChildManager.saveChild(ctx, ChildProfile(n.lowercase(), n, l, UsageStatsMonitor(ctx).getTodayMinutes()))
        children = ChildManager.getChildren(ctx); showAddDialog = false
    }
    LaunchedEffect(Unit) { while(true) { children = ChildManager.getChildren(ctx); delay(30000) } }
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("🌸 Mes Enfants", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold); Text("Gérer les limites", color = BloomTextSec, fontSize = 13.sp) }
            Button({ showAddDialog = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Ajouter") }
        }}
        if(children.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(40.dp), Alignment.CenterHorizontally) { Text("👶", 48.sp); Spacer(Modifier.height(16.dp)); Text("Aucun enfant", FontWeight.Bold); Text("Ajoutez-en un", color = BloomTextSec, fontSize = 13.sp) }}}
        else items(children) { ChildCard(it, { children = ChildManager.getChildren(ctx) }, { ChildManager.deleteChild(ctx, it.name); children = ChildManager.getChildren(ctx) }) }
        item { Spacer(Modifier.height(30.dp)); PermissionsCard() }
    }
}

@Composable
fun ChildCard(child: ChildProfile, onRefresh: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    if(showEditDialog) EditChildDialog(child, { showEditDialog = false }) { l ->
        child.dailyLimitMinutes = l; ChildManager.saveChild(ctx, child); onRefresh(); showEditDialog = false
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Alignment.CenterVertically, Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(BloomPrimary.copy(0.15f)), Alignment.Center) {
                        Text(child.name.first().uppercase(), 20.sp, FontWeight.Bold, color = BloomPrimary)
                    }
                    Column { Text(child.name, FontWeight.Bold); Text(if(child.isPaused) "⏸️ En pause" else "✅ Actif", if(child.isPaused) BloomError else BloomSecondary, fontSize = 12.sp) }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                Box { IconButton({ menuExpanded = true }) { Icon(Icons.Filled.MoreVert, null, tint = BloomTextSec) }
                    DropdownMenu(menuExpanded, { menuExpanded = false }) {
                        DropdownMenuItem({ Text("Modifier") }, { showEditDialog = true; menuExpanded = false })
                        DropdownMenuItem({ Text("Supprimer", color = BloomError) }, { onDelete(); menuExpanded = false })
                    }
                }
            }
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Temps utilisé", color = BloomTextSec, fontSize = 13.sp); Text("${formatMinutes(child.todayUsedMinutes)}/${formatMinutes(child.dailyLimitMinutes)}", fontSize = 13.sp) }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(child.usagePercent(), Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), when { child.usagePercent() >= 0.9f -> BloomError; child.usagePercent() >= 0.7f -> BloomAccent; else -> BloomPrimary })
                Text("${child.remainingMinutes()} min restantes", color = BloomTextSec, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(Alignment.CenterHorizontally) { Text("🔋 ${child.batteryLevel}%"); Text("Batterie", color = BloomTextSec, fontSize = 11.sp) }
                Column(Alignment.CenterHorizontally) { Text("📍"); Text(if(child.latitude != 0.0) "OK" else "—", color = BloomTextSec, fontSize = 11.sp) }
                Column(Alignment.CenterHorizontally) { Text("⏱️ ${formatMinutes(child.remainingMinutes())}"); Text("Restant", color = BloomTextSec, fontSize = 11.sp) }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                Button({ ChildManager.setPaused(ctx, child.name, !child.isPaused); onRefresh() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(if(child.isPaused) BloomSecondary else BloomError)) {
                    Text(if(child.isPaused) "▶️ Reprendre" else "⏸️ Pause")
                }
                Button({ val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${child.latitude},${child.longitude}?q=${child.name}")); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i) }, Modifier.weight(1f), enabled = child.latitude != 0.0) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Localiser")
                }
            }
        }
    }
}

@Composable
fun AddChildDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var limitHours by remember { mutableStateOf(4f) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ajouter un enfant", FontWeight.Bold) },
        text = { Column(Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(name, { name = it }, { Text("Prénom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Column { Text("Limite: ${limitHours.toInt()}h${((limitHours % 1) * 60).toInt()}min"); Slider(limitHours, { limitHours = it }, 0.5f..8f, 14) }
        }},
        confirmButton = { Button({ onAdd(name.trim(), (limitHours * 60).toInt()) }, enabled = name.trim().isNotEmpty()) { Text("Ajouter") } },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun EditChildDialog(child: ChildProfile, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var limitHours by remember { mutableStateOf((child.dailyLimitMinutes / 60f).coerceIn(0.5f..8f)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Modifier — ${child.name}", FontWeight.Bold) },
        text = { Column { Text("Actuel: ${formatMinutes(child.dailyLimitMinutes)}", color = BloomTextSec); Spacer(Modifier.height(16.dp)); Text("Nouvelle: ${limitHours.toInt()}h${((limitHours % 1) * 60).toInt()}min"); Slider(limitHours, { limitHours = it }, 0.5f..8f, 14) } },
        confirmButton = { Button({ onSave((limitHours * 60).toInt()) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun PermissionsCard() {
    val ctx = LocalContext.current
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp)) {
            Text("⚠️ Permissions requises", FontWeight.Bold); Divider()
            if(!hasUsageStatsPermission(ctx)) Button({ ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Info, null); Spacer(Modifier.width(8.dp)); Text("Accès statistiques d'utilisation")
            } else Row { Text("✅"); Spacer(Modifier.width(8.dp)); Text("Statistiques : OK") }
            if(!hasLocationPermission(ctx)) Button({ ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LocationOn, null); Spacer(Modifier.width(8.dp)); Text("Accès position GPS")
            } else Row { Text("✅"); Spacer(Modifier.width(8.dp)); Text("GPS : OK") }
            Button({ ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(8.dp)); Text("Service d'accessibilité")
            }
        }
    }
}

@Composable
fun ParentAppsTab() {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    var apps by remember { mutableStateOf(emptyList<Triple<String, String, Int>>()) }
    val usage = UsageStatsMonitor(ctx).getByApp()
    LaunchedEffect(Unit) {
        apps = try {
            pm.getInstalledApplications(0).filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != ctx.packageName }
                .map { Triple(it.loadLabel(pm).toString(), it.packageName, usage[it.packageName] ?: 0) }.sortedByDescending { it.third }
        } catch(e: Exception) { emptyList() }
    }
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), Arrangement.spacedBy(10.dp)) {
        item { Column { Text("📱 Applications", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold); Text("Bloquer/autoriser", color = BloomTextSec, fontSize = 13.sp) } }
        if(apps.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("Chargement...", Modifier.padding(24.dp), color = BloomTextSec) } }
        else items(apps) { (name, pkg, use) ->
            var blocked by remember { mutableStateOf(AppBlocker.isAppBlocked(ctx, pkg)) }
            Card(shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(name, FontWeight.SemiBold); Text(if(use > 0) "$use min aujourd'hui" else "Non utilisée", color = BloomTextSec, fontSize = 12.sp) }
                    Row { Switch(!blocked, { AppBlocker.setAppBlocked(ctx, pkg, name, !it); blocked = !blocked }); Spacer(Modifier.width(8.dp)); Text(if(blocked) "Bloquée" else "Autorisée", fontSize = 12.sp, color = if(blocked) BloomError else BloomSecondary) }
                }
            }
        }
    }
}

@Composable
fun ParentMessagesTab() {
    var messages by remember { mutableStateOf(MsgManager.all()) }
    var selectedChild by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val children = ChildManager.getChildren(ctx)
    LaunchedEffect(Unit) { while(true) { messages = MsgManager.all(); delay(5000) } }
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), Arrangement.spacedBy(16.dp)) {
        item { Column { Text("💬 Messages", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold); Text("Communiquez", color = BloomTextSec, fontSize = 13.sp) } }
        item { Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                Text("Envoyer un message", FontWeight.Bold)
                if(children.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        OutlinedTextField(if(selectedChild.isEmpty()) "Choisir" else selectedChild, {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) { children.forEach { DropdownMenuItem({ Text(it.name) }, { selectedChild = it.name; expanded = false }) } }
                    }
                    OutlinedTextField(messageText, { messageText = it }, placeholder = { Text("Votre message...") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Button({ if(selectedChild.isNotEmpty() && messageText.isNotEmpty()) { MsgManager.sendMessage("Parent", selectedChild, messageText); messageText = "" } }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Envoyer")
                    }
                } else Text("Ajoutez un enfant d'abord", color = BloomTextSec)
            }
        }}
        item { Text("Historique", FontWeight.Bold, color = BloomTextSec) }
        if(messages.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("Aucun message", Modifier.padding(24.dp), color = BloomTextSec, textAlign = TextAlign.Center) } }
        else items(messages) { MessageBubble(it, it.from == "Parent") }
    }
}

@Composable
fun MessageBubble(msg: Message, isFromParent: Boolean) {
    Row(Modifier.fillMaxWidth(), if(isFromParent) Arrangement.End else Arrangement.Start) {
        Card(shape = RoundedCornerShape(topStart = if(isFromParent) 16.dp else 4.dp, topEnd = if(isFromParent) 4.dp else 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(if(isFromParent) BloomPrimary else BloomSurface), modifier = Modifier.widthIn(max = 280.dp)) {
            Column(Modifier.padding(12.dp)) { Text(msg.content, Color.White, fontSize = 14.sp); Text("${msg.from} • ${msg.time()}", fontSize = 10.sp, color = Color.White.copy(0.7f)) }
        }
    }
}

@Composable
fun ParentSettingsTab(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), Arrangement.spacedBy(20.dp)) {
        item { Text("⚙️ Paramètres", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold) }
        item { Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(24.dp), Arrangement.spacedBy(16.dp)) {
                Text("Informations", style = MaterialTheme.typography.titleMedium, FontWeight.Bold); Divider()
                InfoRow("Mode", "Parent")
                InfoRow("Enfants", "${ChildManager.getChildren(ctx).size}")
                InfoRow("Apps bloquées", "${AppBlocker.getBlockedApps(ctx).count { it.blocked }}")
                Spacer(Modifier.height(8.dp))
                Button(onLogout, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("🔄 Déconnexion")
                }
            }
        }}
        item { PermissionsCard() }
    }
}

@Composable fun InfoRow(label: String, value: String) = Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(label, color = BloomTextSec); Text(value, FontWeight.Medium) }

@Composable
fun ChildMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Mon temps", "Demandes", "Paramètres")
    Scaffold(bottomBar = {
        NavigationBar(containerColor = BloomSurface) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(selected = selectedTab == i, onClick = { selectedTab = i },
                    icon = { Icon(when(i) { 0 -> Icons.Filled.Timer; 1 -> Icons.Filled.Message; else -> Icons.Filled.Settings }, null) },
                    label = { Text(t) })
            }
        }
    }) { p -> Box(Modifier.padding(p)) {
        when(selectedTab) { 0 -> ChildTimeTab(); 1 -> ChildRequestsTab(); 2 -> ChildSettingsTab(onLogout) }
    }}
}

@Composable
fun ChildTimeTab() {
    val ctx = LocalContext.current
    val monitor = remember { UsageStatsMonitor(ctx) }
    var used by remember { mutableStateOf(monitor.getTodayMinutes()) }
    val limit = Prefs.getDailyLimit(ctx)
    val paused = Prefs.isPaused(ctx)
    val rem = (limit - used).coerceAtLeast(0)
    val pct = if(limit > 0) used.toFloat() / limit.toFloat() else 0f
    LaunchedEffect(Unit) { while(true) { delay(30000); used = monitor.getTodayMinutes() } }
    Box(Modifier.fillMaxSize().background(BloomBg).padding(24.dp)) {
        Column(Modifier.fillMaxSize(), Alignment.CenterHorizontally, Arrangement.SpaceBetween) {
            Column(Alignment.CenterHorizontally) { Text("🌸 Mon Temps", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold); Text("Aujourd'hui", color = BloomTextSec, fontSize = 13.sp) }
            if(paused) Column(Alignment.CenterHorizontally, Arrangement.spacedBy(24.dp)) {
                Text("🔒", 80.sp)
                Text("APPAREIL EN PAUSE", style = MaterialTheme.typography.headlineMedium, FontWeight.Bold, color = BloomError)
                Text("L'utilisation est suspendue", color = BloomTextSec, textAlign = TextAlign.Center)
            } else Column(Alignment.CenterHorizontally, Arrangement.spacedBy(32.dp)) {
                Box(Modifier.size(240.dp), Alignment.Center) {
                    CircularProgressIndicator(pct, Modifier.fillMaxSize(), strokeWidth = 16.dp,
                        color = when { pct >= 0.9f -> BloomError; pct >= 0.7f -> BloomAccent; else -> BloomPrimary },
                        trackColor = Color(0xFF1E293B))
                    Column(Alignment.CenterHorizontally) { Text(formatMinutes(rem), style = MaterialTheme.typography.displayLarge, FontWeight.Bold); Text("disponibles", color = BloomTextSec, fontSize = 16.sp) }
                }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp), Alignment.CenterHorizontally) {
                        Text("Résumé du jour", FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Utilisé"); Text(formatMinutes(used)) }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Limite"); Text(formatMinutes(limit)) }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Restant", FontWeight.Bold); Text(formatMinutes(rem), FontWeight.Bold, color = BloomPrimary) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ChildRequestsTab() {
    var messages by remember { mutableStateOf(MsgManager.all()) }
    var newText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while(true) { messages = MsgManager.all(); delay(5000) } }
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), Arrangement.spacedBy(16.dp)) {
        item { Column { Text("💬 Mes Demandes", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold); Text("Demander plus de temps", color = BloomTextSec, fontSize = 13.sp) } }
        item { Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                Text("Demander du temps", FontWeight.Bold)
                OutlinedTextField(newText, { newText = it }, placeholder = { Text("Ex: 15 min pour mes devoirs") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button({ if(newText.isNotEmpty()) { MsgManager.sendMessage("Enfant", "Parent", newText); newText = "" } }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Envoyer")
                }
            }
        }}
        item { Text("Messages", FontWeight.Bold, color = BloomTextSec) }
        val filtered = messages.filter { it.to == "Enfant" || it.from == "Enfant" }
        if(filtered.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("Aucun message", Modifier.padding(24.dp), color = BloomTextSec, textAlign = TextAlign.Center) } }
        else items(filtered) { MessageBubble(it, it.from != "Enfant") }
    }
}

@Composable
fun ChildSettingsTab(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().background(BloomBg).padding(20.dp), Arrangement.spacedBy(20.dp)) {
        item { Text("⚙️ Paramètres", style = MaterialTheme.typography.headlineSmall, FontWeight.Bold) }
        item { Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(24.dp), Arrangement.spacedBy(16.dp)) {
                Text("Mon profil", style = MaterialTheme.typography.titleMedium, FontWeight.Bold); Divider()
                InfoRow("Mode", "Enfant")
                InfoRow("Limite quotidienne", formatMinutes(Prefs.getDailyLimit(ctx)))
                InfoRow("Utilisé aujourd'hui", formatMinutes(Prefs.getTodayUsed(ctx)))
                InfoRow("Statut", if(Prefs.isPaused(ctx)) "⏸️ En pause" else "✅ Actif")
                Spacer(Modifier.height(8.dp))
                Button(onLogout, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("🔄 Changer de profil")
                }
            }
        }}
        item { PermissionsCard() }
    }
}
