package com.lecrible

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.lecrible.data.AppDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Widget d'écran d'accueil : une pastille colorée (avec son nombre) par couleur d'application. */
class BadgeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BadgeUpdater.update(app, AppDb.get(app).dao().all().first())
            } finally {
                pending.finish()
            }
        }
    }
}
