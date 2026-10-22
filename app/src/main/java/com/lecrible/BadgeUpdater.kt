package com.lecrible

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.lecrible.data.NotifEntity

/**
 * Met à jour tout ce qui signale « il y a des notifications en attente » :
 *  - une notification silencieuse de Le Crible, qui fait apparaître la pastille native du lanceur sur l'icône ;
 *  - le widget d'écran d'accueil (taille d'une icône) : une bulle colorée par application, 4 par page.
 */
object BadgeUpdater {

    private const val CHANNEL = "en_attente"
    private const val NOTIF_ID = 1

    fun update(ctx: Context, notifs: List<NotifEntity>) {
        val app = ctx.applicationContext
        // Les permanentes ne sont pas « en attente » : elles ne comptent pas dans la pastille ni dans le widget
        val waiting = notifs.count { !it.ongoing }
        try { updateNotification(app, waiting) } catch (e: Exception) { Log.w("LeCrible", "pastille", e) }
        try { updateWidgets(app) } catch (e: Exception) { Log.w("LeCrible", "widget", e) }
    }

    private fun openAppIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Modèle de clic pour les pages du widget : doit être modifiable pour recevoir l'intention de remplissage. */
    private fun pageClickTemplate(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun updateNotification(ctx: Context, count: Int) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (count == 0) {
            nm.cancel(NOTIF_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        // IMPORTANCE_MIN : ni son, ni bandeau, ni icône dans la barre d'état ; reste dans le volet et fait le point sur l'icône
        val channel = NotificationChannel(CHANNEL, ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN)
        channel.setShowBadge(true)
        nm.createNotificationChannel(channel)

        val notif = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_crible)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(ctx.getString(R.string.pending_text, count))
            .setNumber(count)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun updateWidgets(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, BadgeWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(ctx.packageName, R.layout.widget_badges)
        views.setRemoteAdapter(R.id.badge_stack, Intent(ctx, BadgeWidgetService::class.java))
        views.setEmptyView(R.id.badge_stack, R.id.badge_empty)
        views.setPendingIntentTemplate(R.id.badge_stack, pageClickTemplate(ctx))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(ctx))
        mgr.updateAppWidget(ids, views)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.badge_stack)
    }
}
