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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.lecrible.data.NotifEntity
import com.lecrible.data.Prefs

/**
 * Met à jour tout ce qui signale « il y a des notifications en attente » :
 *  - une notification silencieuse de Le Crible, qui fait apparaître la pastille native du lanceur sur l'icône ;
 *  - le widget d'écran d'accueil, avec une pastille par couleur.
 */
object BadgeUpdater {

    private const val CHANNEL = "en_attente"
    private const val NOTIF_ID = 1

    fun update(ctx: Context, notifs: List<NotifEntity>) {
        val app = ctx.applicationContext
        val prefs = Prefs(app)
        val dots = BadgeLogic.dots(notifs.map { it.pkg }, prefs::appColor)
        updateNotification(app, notifs.size)
        updateWidgets(app, dots)
    }

    private fun openAppIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

    private fun updateWidgets(ctx: Context, dots: List<BadgeDot>) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, BadgeWidget::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(ctx.packageName, R.layout.widget_badges)
        views.setImageViewBitmap(R.id.badges_image, render(dots))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(ctx))
        mgr.updateAppWidget(ids, views)
    }

    /** Dessine les pastilles colorées avec leur nombre ; une seule image, donc compatible avec tous les lanceurs. */
    private fun render(dots: List<BadgeDot>): Bitmap {
        val d = 120
        val gap = 28
        val h = 160
        if (dots.isEmpty()) {
            val bmp = Bitmap.createBitmap(640, h, Bitmap.Config.ARGB_8888)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF9AA7BD.toInt()
                textSize = 52f
            }
            Canvas(bmp).drawText("Aucune notification", 8f, h / 2f + 18f, p)
            return bmp
        }
        val shown = dots.take(8)
        val bmp = Bitmap.createBitmap(shown.size * (d + gap) + gap, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = 56f
        }
        shown.forEachIndexed { i, dot ->
            val cx = gap + i * (d + gap) + d / 2f
            val cy = h / 2f
            fill.color = dot.color
            canvas.drawCircle(cx, cy, d / 2f, fill)
            text.color = if (ColorUtils.calculateLuminance(dot.color) > 0.55) Color.BLACK else Color.WHITE
            canvas.drawText(BadgeLogic.label(dot.count), cx, cy - (text.descent() + text.ascent()) / 2f, text)
        }
        return bmp
    }
}
