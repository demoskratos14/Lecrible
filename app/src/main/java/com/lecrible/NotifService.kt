package com.lecrible

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lecrible.data.AppDb
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs
import kotlinx.coroutines.*

class NotifService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null

    override fun onListenerConnected() {
        watchJob?.cancel()
        watchJob = scope.launch {
            AppDb.get(applicationContext).dao().all().collect { BadgeUpdater.update(applicationContext, it) }
        }
        // Rattrapage : les notifications arrivées pendant que le service était coupé sont encore dans le volet
        try {
            activeNotifications?.forEach { capture(it) }
        } catch (e: Exception) {
            // service pas encore prêt : on réessaiera à la prochaine connexion
        }
    }

    override fun onListenerDisconnected() {
        watchJob?.cancel()
        // Demande au système de relancer le service tout de suite, sans attendre l'ouverture de l'appli
        try {
            requestRebind(ComponentName(this, NotifService::class.java))
        } catch (e: Exception) {
            // rien d'autre à faire
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        capture(sbn)
    }

    private fun capture(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!shouldCapture(prefs, sbn)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) { sbn.packageName }

        val entity = NotifEntity(
            notifKey = sbn.key, pkg = sbn.packageName, appName = appName,
            title = title, text = text, time = sbn.postTime
        )
        val key = sbn.key
        scope.launch {
            AppDb.get(applicationContext).dao().insert(entity)
            // On ne retire de l'écran qu'une fois sauvegardée
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
        scope.cancel()
        super.onDestroy()
    }
}
