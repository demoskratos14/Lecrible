package com.lecrible.data

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** false = on aspire tout sauf les exclusions ; true = seulement les applis cochées */
    var includeMode: Boolean
        get() = sp.getBoolean("includeMode", false)
        set(v) = sp.edit().putBoolean("includeMode", v).apply()

    var excluded: Set<String>
        get() = sp.getStringSet("excluded", DEFAULT_EXCLUDED)!!.toSet()
        set(v) = sp.edit().putStringSet("excluded", v).apply()

    var included: Set<String>
        get() = sp.getStringSet("included", emptySet())!!.toSet()
        set(v) = sp.edit().putStringSet("included", v).apply()

    companion object {
        // SMS, WhatsApp, téléphone (variantes courantes selon les constructeurs)
        val DEFAULT_EXCLUDED = setOf(
            "com.whatsapp", "com.whatsapp.w4b",
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
            "com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer",
            "com.android.server.telecom", "com.android.phone", "com.samsung.android.incallui"
        )
    }
}
