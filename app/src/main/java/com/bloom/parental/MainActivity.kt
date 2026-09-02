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
            "NONE" -> RoleSelectionScreen(onRoleSelected = { Prefs.setRole(ctx, it); role = it })
            "PARENT" -> ParentMainScreen(onLogout = { Prefs.setRole(ctx, "NONE"); role = "NONE" })
            "ENFANT" -> ChildMainScreen(onLogout = { Prefs.setRole(ctx, "NONE"); role = "NONE" })
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BloomBg).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌸", fontSize = 52.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bloom", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = BloomText)
                Text("Contrôle parental intelligent", color = BloomTextSec, fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onRoleSelected("PARENT") },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = BloomPrimary, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("Espace Parent", fontWeight = FontWeight.Bold)
                            Text("Gérer les écrans", color = BloomTextSec, fontSize = 13.sp)
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onRoleSelected("ENFANT") },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(BloomSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = BloomSecondary, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("Espace Enfant", fontWeight = FontWeight.Bold)
                            Text("Voir mon temps", color = BloomTextSec, fontSize = 13.sp)
                        }
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
                NavigationBarItem(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    icon = {
                        val icon = when(i) {
                            0 -> Icons.Filled.Face
                            1 -> Icons.Filled.Apps
                            2 -> Icons.Filled.Send
                            else -> Icons.Filled.Settings
                        }
                        Icon(icon, null)
                    },
                    label = { Text(t) }
                )
            }
        }
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when(selectedTab) {
                0 -> ParentChildrenTab()
                1 -> ParentAppsTab()
                2 -> ParentMessagesTab()
                3 -> ParentSettingsTab(onLogout)
            }
        }
    }
}

@Composable
fun ParentChildrenTab() {
    val ctx = LocalContext.current
    var children by remember { mutableStateOf(ChildManager.getChildren(ctx)) }
    var showAddDialog by remember { mutableStateOf(false) }
    if (showAddDialog) {
        AddChildDialog(onDismiss = { showAddDialog = false }) { name, limit ->
            ChildManager.saveChild(ctx, ChildProfile(
                id = name.lowercase(),
                name = name,
                dailyLimitMinutes = limit,
                todayUsedMinutes = UsageStatsMonitor(ctx).getTodayMinutes()
            ))
            children = ChildManager.getChildren(ctx)
            showAddDialog = false
        }
    }
    LaunchedEffect(Unit) {
        while(true) {
            children = ChildManager.getChildren(ctx)
            delay(30000)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🌸 Mes Enfants", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Gérer les limites", color = BloomTextSec, fontSize = 13.sp)
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }
        if(children.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👶", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucun enfant", fontWeight = FontWeight.Bold)
                        Text("Ajoutez-en un", color = BloomTextSec, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(children) { child ->
                ChildCard(
                    child = child,
                    onRefresh = { children = ChildManager.getChildren(ctx) },
                    onDelete = {
                        ChildManager.deleteChild(ctx, child.name)
                        children = ChildManager.getChildren(ctx)
                    }
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
            PermissionsCard()
        }
    }
}

@Composable
fun ChildCard(
    child: ChildProfile,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    if(showEditDialog) {
        EditChildDialog(
            child = child,
            onDismiss = { showEditDialog = false },
            onSave = { newLimit ->
                child.dailyLimitMinutes = newLimit
                ChildManager.saveChild(ctx, child)
                onRefresh()
                showEditDialog = false
            }
        )
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = child.name.first().uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BloomPrimary
                        )
                    }
                    Column {
                        Text(child.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = if(child.isPaused) "⏸️ En pause" else "✅ Actif",
                            color = if(child.isPaused) BloomError else BloomSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, null, tint = BloomTextSec)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            onClick = { showEditDialog = true; menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = BloomError) },
                            onClick = { onDelete(); menuExpanded = false }
                        )
                    }
                }
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temps utilisé", color = BloomTextSec, fontSize = 13.sp)
                    Text("${formatMinutes(child.todayUsedMinutes)}/${formatMinutes(child.dailyLimitMinutes)}", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = child.usagePercent(),
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = when {
                        child.usagePercent() >= 0.9f -> BloomError
                        child.usagePercent() >= 0.7f -> BloomAccent
                        else -> BloomPrimary
                    }
                )
                Text("${child.remainingMinutes()} min restantes", color = BloomTextSec, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔋 ${child.batteryLevel}%")
                    Text("Batterie", color = BloomTextSec, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍")
                    Text(if(child.latitude != 0.0) "OK" else "—", color = BloomTextSec, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱️ ${formatMinutes(child.remainingMinutes())}")
                    Text("Restant", color = BloomTextSec, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        ChildManager.setPaused(ctx, child.name, !child.isPaused)
                        onRefresh()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        if(child.isPaused) BloomSecondary else BloomError
                    )
                ) {
                    Text(if(child.isPaused) "▶️ Reprendre" else "⏸️ Pause")
                }
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:${child.latitude},${child.longitude}?q=${child.name}")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = child.latitude != 0.0
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Localiser")
                }
            }
        }
    }
}

@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitHours by remember { mutableStateOf(4f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un enfant", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text("Limite: ${limitHours.toInt()}h${((limitHours % 1) * 60).toInt()}min")
                    Slider(
                        value = limitHours,
                        onValueChange = { limitHours = it },
                        valueRange = 0.5f..8f,
                        steps = 14
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name.trim(), (limitHours * 60).toInt()) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditChildDialog(
    child: ChildProfile,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var limitHours by remember {
        mutableStateOf((child.dailyLimitMinutes / 60f).coerceIn(0.5f..8f))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier — ${child.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Actuel: ${formatMinutes(child.dailyLimitMinutes)}", color = BloomTextSec)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nouvelle: ${limitHours.toInt()}h${((limitHours % 1) * 60).toInt()}min")
                Slider(
                    value = limitHours,
                    onValueChange = { limitHours = it },
                    valueRange = 0.5f..8f,
                    steps = 14
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave((limitHours * 60).toInt()) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun PermissionsCard() {
    val ctx = LocalContext.current
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("⚠️ Permissions requises", fontWeight = FontWeight.Bold)
            Divider()
            if(!hasUsageStatsPermission(ctx)) {
                Button(
                    onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Info, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accès statistiques d'utilisation")
                }
            } else {
                Row {
                    Text("✅")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Statistiques : OK")
                }
            }
            if(!hasLocationPermission(ctx)) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${ctx.packageName}")
                        )
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.LocationOn, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accès position GPS")
                }
            } else {
                Row {
                    Text("✅")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GPS : OK")
                }
            }
            Button(
                onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Settings, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Service d'accessibilité")
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
            pm.getInstalledApplications(0)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != ctx.packageName }
                .map {
                    Triple(
                        it.loadLabel(pm).toString(),
                        it.packageName,
                        usage[it.packageName] ?: 0
                    )
                }
                .sortedByDescending { it.third }
        } catch(e: Exception) {
            emptyList()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("📱 Applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Bloquer/autoriser", color = BloomTextSec, fontSize = 13.sp)
            }
        }
        if(apps.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Chargement...", modifier = Modifier.padding(24.dp), color = BloomTextSec)
                }
            }
        } else {
            items(apps) { (name, pkg, use) ->
                var blocked by remember { mutableStateOf(AppBlocker.isAppBlocked(ctx, pkg)) }
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if(use > 0) "$use min aujourd'hui" else "Non utilisée",
                                color = BloomTextSec,
                                fontSize = 12.sp
                            )
                        }
                        Row {
                            Switch(
                                checked = !blocked,
                                onCheckedChange = {
                                    AppBlocker.setAppBlocked(ctx, pkg, name, !it)
                                    blocked = !blocked
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if(blocked) "Bloquée" else "Autorisée",
                                fontSize = 12.sp,
                                color = if(blocked) BloomError else BloomSecondary
                            )
                        }
                    }
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
    LaunchedEffect(Unit) {
        while(true) {
            messages = MsgManager.all()
            delay(5000)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("💬 Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Communiquez", color = BloomTextSec, fontSize = 13.sp)
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Envoyer un message", fontWeight = FontWeight.Bold)
                    if(children.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = if(selectedChild.isEmpty()) "Choisir" else selectedChild,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                children.forEach { child ->
                                    DropdownMenuItem(
                                        text = { Text(child.name) },
                                        onClick = {
                                            selectedChild = child.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Votre message...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                if(selectedChild.isNotEmpty() && messageText.isNotEmpty()) {
                                    MsgManager.sendMessage("Parent", selectedChild, messageText)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Envoyer")
                        }
                    } else {
                        Text("Ajoutez un enfant d'abord", color = BloomTextSec)
                    }
                }
            }
        }
        item {
            Text("Historique", fontWeight = FontWeight.Bold, color = BloomTextSec)
        }
        if(messages.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Aucun message",
                        modifier = Modifier.padding(24.dp),
                        color = BloomTextSec,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(messages) { msg ->
                MessageBubble(msg, isFromParent = msg.from == "Parent")
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message, isFromParent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if(isFromParent) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = if(isFromParent) 16.dp else 4.dp,
                topEnd = if(isFromParent) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if(isFromParent) BloomPrimary else BloomSurface
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(msg.content, color = Color.White, fontSize = 14.sp)
                Text(
                    "${msg.from} • ${msg.time()}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ParentSettingsTab(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("⚙️ Paramètres", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Informations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    InfoRow("Mode", "Parent")
                    InfoRow("Enfants", "${ChildManager.getChildren(ctx).size}")
                    InfoRow("Apps bloquées", "${AppBlocker.getBlockedApps(ctx).count { it.blocked }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔄 Déconnexion")
                    }
                }
            }
        }
        item { PermissionsCard() }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BloomTextSec)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ChildMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Mon temps", "Demandes", "Paramètres")
    Scaffold(bottomBar = {
        NavigationBar(containerColor = BloomSurface) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    icon = {
                        val icon = when(i) {
                            0 -> Icons.Filled.Timer
                            1 -> Icons.Filled.Message
                            else -> Icons.Filled.Settings
                        }
                        Icon(icon, null)
                    },
                    label = { Text(t) }
                )
            }
        }
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when(selectedTab) {
                0 -> ChildTimeTab()
                1 -> ChildRequestsTab()
                2 -> ChildSettingsTab(onLogout)
            }
        }
    }
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
    LaunchedEffect(Unit) {
        while(true) {
            delay(30000)
            used = monitor.getTodayMinutes()
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(BloomBg).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌸 Mon Temps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Aujourd'hui", color = BloomTextSec, fontSize = 13.sp)
            }
            if(paused) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text("🔒", fontSize = 80.sp)
                    Text(
                        "APPAREIL EN PAUSE",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = BloomError
                    )
                    Text("L'utilisation est suspendue", color = BloomTextSec, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = pct,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 16.dp,
                            color = when {
                                pct >= 0.9f -> BloomError
                                pct >= 0.7f -> BloomAccent
                                else -> BloomPrimary
                            },
                            trackColor = Color(0xFF1E293B)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatMinutes(rem), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                            Text("disponibles", color = BloomTextSec, fontSize = 16.sp)
                        }
                    }
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Résumé du jour", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Utilisé")
                                Text(formatMinutes(used))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Limite")
                                Text(formatMinutes(limit))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Restant", fontWeight = FontWeight.Bold)
                                Text(formatMinutes(rem), fontWeight = FontWeight.Bold, color = BloomPrimary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ChildRequestsTab() {
    var messages by remember { mutableStateOf(MsgManager.all()) }
    var newText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while(true) {
            messages = MsgManager.all()
            delay(5000)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("💬 Mes Demandes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Demander plus de temps", color = BloomTextSec, fontSize = 13.sp)
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Demander du temps", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        placeholder = { Text("Ex: 15 min pour mes devoirs") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Button(
                        onClick = {
                            if(newText.isNotEmpty()) {
                                MsgManager.sendMessage("Enfant", "Parent", newText)
                                newText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Send, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Envoyer")
                    }
                }
            }
        }
        item {
            Text("Messages", fontWeight = FontWeight.Bold, color = BloomTextSec)
        }
        val filtered = messages.filter { it.to == "Enfant" || it.from == "Enfant" }
        if(filtered.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Aucun message",
                        modifier = Modifier.padding(24.dp),
                        color = BloomTextSec,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filtered) { msg ->
                MessageBubble(msg, isFromParent = msg.from != "Enfant")
            }
        }
    }
}

@Composable
fun ChildSettingsTab(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("⚙️ Paramètres", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Mon profil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    InfoRow("Mode", "Enfant")
                    InfoRow("Limite quotidienne", formatMinutes(Prefs.getDailyLimit(ctx)))
                    InfoRow("Utilisé aujourd'hui", formatMinutes(Prefs.getTodayUsed(ctx)))
                    InfoRow("Statut", if(Prefs.isPaused(ctx)) "⏸️ En pause" else "✅ Actif")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔄 Changer de profil")
                    }
                }
            }
        }
        item { PermissionsCard() }
    }
}
