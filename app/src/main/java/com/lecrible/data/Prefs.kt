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

    // --- Diagnostic : à quel moment le service a été relié / coupé, et dernière erreur ---

    var lastConnected: Long
        get() = sp.getLong("lastConnected", 0L)
        set(v) = sp.edit { putLong("lastConnected", v) }

    var lastDisconnected: Long
        get() = sp.getLong("lastDisconnected", 0L)
        set(v) = sp.edit { putLong("lastDisconnected", v) }

    var lastCapture: Long
        get() = sp.getLong("lastCapture", 0L)
        set(v) = sp.edit { putLong("lastCapture", v) }

    val lastError: String? get() = sp.getString("lastError", null)
    val lastErrorTime: Long get() = sp.getLong("lastErrorTime", 0L)

    /** Écriture immédiate (commit) : le processus peut s'arrêter juste après un plantage. */
    fun recordError(where: String, e: Throwable) {
        sp.edit(commit = true) {
            putLong("lastErrorTime", System.currentTimeMillis())
            putString("lastError", "$where : ${e.stackTraceToString().take(1200)}")
        }
    }

    fun clearError() = sp.edit { remove("lastError"); remove("lastErrorTime") }
}
