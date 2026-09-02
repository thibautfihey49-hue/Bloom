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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bloom.parental.data.Prefs
import com.bloom.parental.service.BloomSmsManager
import com.bloom.parental.service.EnvironmentMonitorService
import com.bloom.parental.service.UsageStatsMonitor
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Permissions nécessaires pour fonctionner", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContent { BloomApp() }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
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
    } catch (e: Exception) {
        false
    }
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
            primary = BloomPrimary,
            secondary = BloomSecondary,
            error = BloomError,
            background = BloomBg,
            surface = BloomSurface,
            onPrimary = Color.White,
            onBackground = BloomText,
            onSurface = BloomText
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomApp() {
    val ctx = LocalContext.current
    val startMode = Prefs.isParent
    val hasOther = Prefs.otherPhone.isNotEmpty()

    BloomTheme {
        when {
            !hasOther -> RoleSelectionScreen()
            startMode -> ParentScreen()
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
    var messageText by remember { mutableStateOf(TextFieldValue("Papa je peux avoir plus de temps.")) }

    BloomTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(BloomBg).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(80.dp).clip(CircleShape).background(BloomPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌸", fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Bloom", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = BloomText)
                        Text("Contrôle parental par SMS", color = BloomTextSec, fontSize = 14.sp)
                    }
                }

                item {
                    Text("Choisis ton espace", style = MaterialTheme.typography.titleMedium, color = BloomText)
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "parent" }
                            .border(
                                width = 2.dp,
                                color = if (selectedMode == "parent") BloomPrimary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = BloomPrimary, modifier = Modifier.size(32.dp))
                            Column {
                                Text("🔒 Espace Parent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Tableau de bord complet", color = BloomTextSec, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "child" }
                            .border(
                                width = 2.dp,
                                color = if (selectedMode == "child") BloomSecondary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Filled.Person, null, tint = BloomSecondary, modifier = Modifier.size(32.dp))
                            Column {
                                Text("👶 Espace Enfant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Enfant connecté", color = BloomTextSec, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (selectedMode != null) {
                    item {
                        Divider(color = BloomSurface, thickness = 1.dp)
                    }

                    if (selectedMode == "parent") {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🔐 Code secret (4 chiffres)", color = BloomText)
                                OutlinedTextField(
                                    value = codeSecret,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) codeSecret = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("____") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 20.sp)
                                )
                                Text("⚠️ Ce code protège l'accès à l'espace parent", color = BloomTextSec, fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (selectedMode == "parent") "📱 Numéro de l'enfant" else "📱 Numéro du parent",
                                color = BloomText
                            )
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("+336...") },
                                singleLine = true
                            )
                        }
                    }

                    if (selectedMode == "child") {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("💬 Message au parent (pré-rempli)", color = BloomText)
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    placeholder = { Text("Ton message...") },
                                    maxLines = 4
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                Prefs.otherPhone = phoneNumber
                                Prefs.isParent = selectedMode == "parent"
                                if (selectedMode == "parent") {
                                    Prefs.codeSecret = codeSecret
                                }
                                Toast.makeText(ctx, "Configuration sauvegardée", Toast.LENGTH_SHORT).show()
                                (ctx as MainActivity).checkOrRequestPermissions()
                                if (selectedMode == "child") {
                                    EnvironmentMonitorService.start(ctx)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = phoneNumber.isNotEmpty() && (selectedMode != "parent" || codeSecret.length == 4)
                        ) {
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
    var usage by remember { mutableStateOf(0) }
    var limit by remember { mutableStateOf(Prefs.dailyLimit) }
    var battery by remember { mutableStateOf(85) }
    var childPhone by remember { mutableStateOf(Prefs.otherPhone) }
    var showCodeScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            usage = UsageStatsMonitor(ctx).getTodayMinutes()
            delay(30000)
        }
    }

    BloomTheme {
        when {
            showCodeScreen && Prefs.codeSecret.isNotEmpty() -> {
                // Écran de saisie du code secret
                Box(
                    modifier = Modifier.fillMaxSize().background(BloomBg).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Text("🔐 Code Secret", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BloomText)
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) codeInput = it },
                            placeholder = { Text("____") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 28.sp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                        Button(
                            onClick = {
                                if (codeInput == Prefs.codeSecret) {
                                    isUnlocked = true
                                    showCodeScreen = false
                                } else {
                                    Toast.makeText(ctx, "Code incorrect", Toast.LENGTH_SHORT).show()
                                    codeInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
                        ) {
                            Text("🔓 Déverrouiller", fontSize = 16.sp)
                        }
                        TextButton(onClick = {
                            Prefs.otherPhone = ""
                            Prefs.isParent = false
                            Prefs.codeSecret = ""
                        }) {
                            Text("Réinitialiser", color = BloomTextSec)
                        }
                    }
                }
            }
            else -> {
                // Tableau de bord Parent
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Column {
                            Text("🌸 Tableau de Bord Parent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BloomText)
                            Text("Enfant : $childPhone", color = BloomTextSec, fontSize = 13.sp)
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("📊 Aujourd'hui", fontWeight = FontWeight.Bold, color = BloomText)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("⏳ Temps utilisé", color = BloomTextSec, fontSize = 13.sp)
                                        Text(formatMinutes(usage), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BloomPrimary)
                                    }
                                    Column {
                                        Text("🔋 Batterie", color = BloomTextSec, fontSize = 13.sp)
                                        Text("$battery%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BloomSecondary)
                                    }
                                }
                                Divider()
                                Text("⏱️ Limite quotidienne : ${formatMinutes(limit)}", color = BloomText)
                                Slider(
                                    value = limit.toFloat(),
                                    onValueChange = { limit = it.toInt() },
                                    valueRange = 30f..360f,
                                    steps = 10,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        Prefs.dailyLimit = limit
                                        BloomSmsManager.sendCommand(ctx, "limit", limit.toString())
                                        Toast.makeText(ctx, "Limite mise à jour", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("✅ Appliquer la limite")
                                }
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    BloomSmsManager.sendCommand(ctx, "pause", "1")
                                    Toast.makeText(ctx, "⏸️ Enfant en pause", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(70.dp),
                                colors = ButtonDefaults.buttonColors(BloomError)
                            ) {
                                Icon(Icons.Filled.Pause, null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("⏸️ Pause", fontSize = 15.sp)
                            }
                            Button(
                                onClick = {
                                    BloomSmsManager.sendCommand(ctx, "pause", "0")
                                    Toast.makeText(ctx, "▶️ Reprise", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(70.dp),
                                colors = ButtonDefaults.buttonColors(BloomSecondary)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("▶️ Reprendre", fontSize = 15.sp)
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("📡 Commandes à distance", fontWeight = FontWeight.Bold, color = BloomText)
                                Button(
                                    onClick = {
                                        BloomSmsManager.sendCommand(ctx, "ring", "1")
                                        Toast.makeText(ctx, "📞 Sonnerie envoyée", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Phone, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("📞 Faire sonner l'appareil")
                                }
                                Button(
                                    onClick = {
                                        BloomSmsManager.sendCommand(ctx, "locate", "1")
                                        Toast.makeText(ctx, "📍 Position demandée", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("📍 Localiser l'appareil")
                                }
                            }
                        }
                    }

                    item {
                        TextButton(onClick = {
                            Prefs.otherPhone = ""
                            Prefs.isParent = false
                            Prefs.codeSecret = ""
                        }) {
                            Text("🔄 Déconnecter / Réinitialiser", color = BloomTextSec)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildScreen() {
    val ctx = LocalContext.current
    var used by remember { mutableStateOf(UsageStatsMonitor(ctx).getTodayMinutes()) }
    var limit by remember { mutableStateOf(Prefs.dailyLimit) }
    var isPaused by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("Papa je peux avoir plus de temps.") }
    var soundLevel by remember { mutableStateOf(0) }
    var parentPhone by remember { mutableStateOf(Prefs.otherPhone) }

    LaunchedEffect(Unit) {
        while (true) {
            used = UsageStatsMonitor(ctx).getTodayMinutes()
            BloomSmsManager.sendUsage(ctx, used, limit)
            delay(60000)
        }
    }

    LaunchedEffect(Unit) {
        EnvironmentMonitorService.soundLevel.collect {
            soundLevel = it
        }
    }

    BloomTheme {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(BloomBg).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    Text("🌸 Espace Enfant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BloomText)
                    Text("Connecté à : $parentPhone", color = BloomTextSec, fontSize = 13.sp)
                }
            }

            if (isPaused) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BloomError.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏸️ EN PAUSE", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BloomError)
                            Text("Demande au parent de reprendre", color = BloomTextSec)
                        }
                    }
                }
            } else {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏳ Temps restant", color = BloomTextSec)
                            Text(
                                formatMinutes((limit - used).coerceAtLeast(0)),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = BloomPrimary
                            )
                            Text("sur ${formatMinutes(limit)}", color = BloomTextSec)
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💬 Demander plus de temps", fontWeight = FontWeight.Bold, color = BloomText)
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                maxLines = 3
                            )
                            Button(
                                onClick = {
                                    BloomSmsManager.sendCommand(ctx, "msg", messageText)
                                    Toast.makeText(ctx, "✅ Message envoyé au parent", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text("📤 Envoyer au parent")
                            }
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("🎤 Surveillance ambiante", fontWeight = FontWeight.Bold, color = BloomText)
                            Text("Niveau sonore : $soundLevel dB", color = BloomTextSec)
                            Text("✅ Actif en arrière-plan", color = BloomSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                TextButton(onClick = {
                    Prefs.otherPhone = ""
                    Prefs.isParent = false
                    EnvironmentMonitorService.stop(ctx)
                }) {
                    Text("🔄 Déconnecter", color = BloomTextSec)
                }
            }
        }
    }
}
