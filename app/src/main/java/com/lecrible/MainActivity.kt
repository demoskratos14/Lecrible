package com.lecrible

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lecrible.data.AppDb
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        allowed = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
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
        topBar = {
            TopAppBar(
                title = { Text("Le Crible") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (!allowed) {
                Card(Modifier.padding(12.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("L'accès aux notifications n'est pas activé.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }) { Text("Autoriser l'accès") }
                    }
                }
            }
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
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
                colors = CardDefaults.cardColors(containerColor = Color(0xCC101A30))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
    var includeMode by remember { mutableStateOf(prefs.includeMode) }
    var excluded by remember { mutableStateOf(prefs.excluded) }
    var included by remember { mutableStateOf(prefs.included) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

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
        HorizontalDivider()
        LazyColumn {
            items(apps, key = { it.pkg }) { app ->
                val checked = if (includeMode) app.pkg in included else app.pkg in excluded
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
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
    }
}
