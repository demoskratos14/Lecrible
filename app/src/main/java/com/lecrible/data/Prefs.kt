package com.lecrible.data

import android.content.Context
import androidx.core.content.edit
import com.lecrible.BadgePalette
import com.lecrible.NotifFilter

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** false = on aspire tout sauf les exclusions ; true = seulement les applis cochées */
    var includeMode: Boolean
        get() = sp.getBoolean("includeMode", false)
        set(v) = sp.edit { putBoolean("includeMode", v) }

    var excluded: Set<String>
        get() = sp.getStringSet("excluded", NotifFilter.DEFAULT_EXCLUDED)!!.toSet()
        set(v) = sp.edit { putStringSet("excluded", v) }

    var included: Set<String>
        get() = sp.getStringSet("included", emptySet())!!.toSet()
        set(v) = sp.edit { putStringSet("included", v) }

    /** Aspirer aussi les notifications permanentes (ex. Chrome navigation privée, IRC). Désactivé par défaut. */
    var includeOngoing: Boolean
        get() = sp.getBoolean("includeOngoing", false)
        set(v) = sp.edit { putBoolean("includeOngoing", v) }

    /** Couleur de la pastille d'une application (turquoise par défaut). */
    fun appColor(pkg: String): Int = sp.getInt("color_$pkg", BadgePalette.default)

    fun setAppColor(pkg: String, color: Int) = sp.edit { putInt("color_$pkg", color) }
}
