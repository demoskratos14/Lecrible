package com.lecrible

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lecrible.data.AppDb
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(
            colorScheme = darkColorScheme(
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White
            )
        ) { App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(false) }
    var batteryRestricted by remember { mutableStateOf(false) }

    // Android 13+ : autorisation d'afficher la notification silencieuse qui fait apparaître la pastille sur l'icône
    val askPostNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) askPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LifecycleResumeEffect(Unit) {
        allowed = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        if (allowed) {
            // Si Android a coupé le service en arrière-plan, on lui demande de le relancer
            try {
                NotificationListenerService.requestRebind(ComponentName(ctx, NotifService::class.java))
            } catch (e: Exception) { }
        }
        batteryRestricted = !ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
        onPauseOrDispose { }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fond_le_crible),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color(0xB3060B18)))   // voile pour la lisibilité
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            topBar = {
                TopAppBar(
                    title = { Text("Le Crible") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad)) {
                if (!allowed) {
                    Card(Modifier.padding(12.dp).fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("L'accès aux notifications n'est pas activé.")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Si Android affiche « Paramètre restreint » : ouvre d'abord les infos de l'appli, " +
                                    "touche ⋮ en haut à droite puis « Autoriser les paramètres restreints ».",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                                )
                            }) { Text("1. Ouvrir les infos de l'appli") }
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = {
                                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }) { Text("2. Autoriser l'accès") }
                        }
                    }
                }
                if (allowed && batteryRestricted) {
                    Card(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("L'optimisation de batterie peut couper Le Crible en arrière-plan : des notifications risquent d'être manquées.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                                )
                            }) { Text("Désactiver l'optimisation") }
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Reçues") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Réglages") })
                }
                if (tab == 0) InboxScreen() else SettingsScreen()
            }
        }
    }
}

@Composable
fun InboxScreen() {
    val ctx = LocalContext.current
    val dao = remember { AppDb.get(ctx).dao() }
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    val notifs by dao.all().collectAsState(initial = emptyList())
    val groups = remember(notifs) { notifs.groupBy { it.pkg } }
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    if (notifs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucune notification") }
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = { scope.launch { dao.clear() } }) { Text("Tout vider") }
        }
        items(groups.entries.toList(), key = { it.key }) { (pkg, list) ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC101A30), contentColor = Color.White)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).background(Color(prefs.appColor(pkg)), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${list.first().appName} (${list.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).clickable {
                                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
                            }
                        )
                        TextButton(onClick = { scope.launch { dao.deleteApp(pkg) } }) { Text("Vider") }
                    }
                    list.forEach { n -> NotifRow(n, df.format(Date(n.time))) { scope.launch { dao.delete(n.id) } } }
                }
            }
        }
    }
}

@Composable
private fun NotifRow(n: NotifEntity, date: String, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 6.dp)) {
        n.title?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        n.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(date, style = MaterialTheme.typography.labelSmall)
    }
}

private data class AppItem(val pkg: String, val name: String)

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val dao = remember { AppDb.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    var includeMode by remember { mutableStateOf(prefs.includeMode) }
    var includeOngoing by remember { mutableStateOf(prefs.includeOngoing) }
    var excluded by remember { mutableStateOf(prefs.excluded) }
    var included by remember { mutableStateOf(prefs.included) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    val colors = remember { mutableStateMapOf<String, Int>() }
    var colorFor by remember { mutableStateOf<AppItem?>(null) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .distinctBy { it.pkg }
                .sortedBy { it.name.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (includeMode) "Aspirer uniquement les applis cochées"
                    else "Aspirer tout, sauf les applis cochées",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = includeMode, onCheckedChange = { includeMode = it; prefs.includeMode = it })
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Notifications permanentes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aspirer aussi celles qui restent affichées (navigation privée, connexion en cours...). " +
                        "Android refuse souvent de les retirer de l'écran : elles sont copiées ici mais peuvent y rester.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = includeOngoing, onCheckedChange = { includeOngoing = it; prefs.includeOngoing = it })
        }
        Text(
            "Touche le rond de couleur d'une appli pour choisir la couleur de sa pastille.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider()
        LazyColumn {
            items(apps, key = { it.pkg }) { app ->
                val checked = if (includeMode) app.pkg in included else app.pkg in excluded
                val color = colors[app.pkg] ?: prefs.appColor(app.pkg)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (includeMode) {
                            included = if (checked) included - app.pkg else included + app.pkg
                            prefs.included = included
                        } else {
                            excluded = if (checked) excluded - app.pkg else excluded + app.pkg
                            prefs.excluded = excluded
                        }
                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.name, Modifier.weight(1f))
                    Box(
                        Modifier.size(28.dp).background(Color(color), CircleShape).clickable { colorFor = app }
                    )
                    Spacer(Modifier.width(12.dp))
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
    }

    colorFor?.let { app ->
        AlertDialog(
            onDismissRequest = { colorFor = null },
            title = { Text("Couleur : ${app.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BadgePalette.colors.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { c ->
                                Box(
                                    Modifier.size(44.dp).background(Color(c), CircleShape).clickable {
                                        prefs.setAppColor(app.pkg, c)
                                        colors[app.pkg] = c
                                        colorFor = null
                                        // rafraîchit tout de suite le widget
                                        scope.launch { BadgeUpdater.update(ctx, dao.all().first()) }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colorFor = null }) { Text("Fermer") } },
            dismissButton = {
                TextButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.pkg)
                    )
                    colorFor = null
                }) { Text("Réglages Android de l'appli") }
            }
        )
    }
}
