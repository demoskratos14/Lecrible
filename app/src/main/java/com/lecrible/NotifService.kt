package com.lecrible

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.lecrible.data.AppDb
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs
import kotlinx.coroutines.*

class NotifService : NotificationListenerService() {

    // Une erreur dans une coroutine ne doit jamais arrêter toute l'appli : on la note (onglet « État ») et on continue
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Erreur en arrière-plan", e)
        try { Prefs(applicationContext).recordError("service", e) } catch (t: Throwable) { }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)
    private var watchJob: Job? = null

    override fun onListenerConnected() {
        connected = true
        Prefs(this).lastConnected = System.currentTimeMillis()
        watchJob?.cancel()
        watchJob = scope.launch {
            AppDb.get(applicationContext).dao().all().collect {
                try {
                    BadgeUpdater.update(applicationContext, it)
                } catch (e: Exception) {
                    Log.w(TAG, "mise à jour des pastilles", e)
                }
            }
        }
        // Rattrapage : les notifications arrivées pendant que le service était coupé sont encore dans le volet
        try {
            activeNotifications?.forEach { safeCapture(it) }
        } catch (e: Exception) {
            // service pas encore prêt : on réessaiera à la prochaine connexion
        }
    }

    override fun onListenerDisconnected() {
        connected = false
        try { Prefs(this).lastDisconnected = System.currentTimeMillis() } catch (e: Exception) { }
        watchJob?.cancel()
        // Demande au système de relancer le service tout de suite, sans attendre l'ouverture de l'appli
        try {
            requestRebind(ComponentName(this, NotifService::class.java))
        } catch (e: Exception) {
            // rien d'autre à faire
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        safeCapture(sbn)
    }

    private fun safeCapture(sbn: StatusBarNotification) {
        try {
            capture(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "capture", e)
            try { Prefs(this).recordError("capture", e) } catch (t: Throwable) { }
        }
    }

    private fun capture(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!shouldCapture(prefs, sbn)) return

        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) { sbn.packageName }

        val key = sbn.key
        val ongoing = sbn.isOngoing
        val link = LinkFinder.first(text, title)
        val pkg = sbn.packageName
        val postTime = sbn.postTime
        val channelId = n.channelId

        // Garde l'intention d'origine en mémoire pour pouvoir rouvrir l'écran exact depuis Le Crible
        PendingIntentStore.put(key, n.contentIntent)
        scope.launch {
            val dao = AppDb.get(applicationContext).dao()
            val old = dao.byKey(key)
            // Une permanente qui se met à jour sans rien changer (ex. progression) ne doit pas être réécrite en boucle
            val unchanged = ongoing && old != null && old.ongoing && old.title == title && old.text == text
            if (!unchanged) {
                dao.insert(
                    NotifEntity(
                        id = old?.id ?: 0, notifKey = key, pkg = pkg, appName = appName,
                        title = title, text = text, time = postTime,
                        channelId = channelId, link = link, ongoing = ongoing
                    )
                )
                Prefs(applicationContext).lastCapture = System.currentTimeMillis()
            }
            // On ne retire de l'écran qu'une fois sauvegardée (Android refuse pour certaines permanentes)
            cancelNotification(key)
        }
    }

    private fun shouldCapture(prefs: Prefs, sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        return NotifFilter.shouldCapture(
            pkg = sbn.packageName,
            ownPackage = packageName,
            isOngoing = sbn.isOngoing,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            category = n.category,
            includeMode = prefs.includeMode,
            included = prefs.included,
            excluded = prefs.excluded,
            includeOngoing = prefs.includeOngoing
        )
    }

    override fun onDestroy() {
        connected = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LeCrible"

        /** Vrai tant qu'Android garde le service relié à l'appli (diagnostic et chien de garde). */
        @Volatile var connected = false
    }
}
