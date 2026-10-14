package com.lecrible

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lecrible.data.AppDb
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs
import kotlinx.coroutines.*

class NotifService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
        if (sbn.packageName == packageName) return false
        if (sbn.isOngoing) return false                                   // musique, navigation, appel en cours
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return false
        if (n.category == Notification.CATEGORY_CALL) return false        // appels entrants / manqués
        return if (prefs.includeMode) sbn.packageName in prefs.included
        else sbn.packageName !in prefs.excluded
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
