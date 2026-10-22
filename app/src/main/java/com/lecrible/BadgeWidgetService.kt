package com.lecrible

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lecrible.data.AppDb
import com.lecrible.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Fournit les pages du widget (une page = 4 applis au maximum). */
class BadgeWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = PagesFactory(applicationContext)

    private class PagesFactory(private val ctx: Context) : RemoteViewsFactory {
        private var pages: List<List<AppBubble>> = emptyList()

        override fun onCreate() = load()
        override fun onDataSetChanged() = load()
        override fun onDestroy() {}

        private fun load() {
            pages = try {
                val prefs = Prefs(ctx)
                val notifs = runBlocking(Dispatchers.IO) { AppDb.get(ctx).dao().all().first() }
                BadgeLogic.pages(BadgeLogic.bubbles(notifs.filter { !it.ongoing }.map { it.pkg }, prefs::appColor))
            } catch (e: Exception) {
                emptyList()
            }
        }

        override fun getCount(): Int = pages.size

        override fun getViewAt(position: Int): RemoteViews? {
            val page = pages.getOrNull(position) ?: return null
            val views = RemoteViews(ctx.packageName, R.layout.widget_page)
            views.setImageViewBitmap(R.id.page_image, BadgeRenderer.renderPage(page, position, pages.size))
            views.setOnClickFillInIntent(R.id.page_image, Intent())
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
