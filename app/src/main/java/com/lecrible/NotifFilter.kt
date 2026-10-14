package com.lecrible

/** Règles de tri des notifications, sans dépendance à Android pour pouvoir être testées en JVM. */
object NotifFilter {

    /** Valeur de Notification.CATEGORY_CALL (appels entrants / manqués). */
    const val CATEGORY_CALL = "call"

    /** SMS, WhatsApp, téléphone (variantes courantes selon les constructeurs). */
    val DEFAULT_EXCLUDED = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
        "com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer",
        "com.android.server.telecom", "com.android.phone", "com.samsung.android.incallui"
    )

    fun shouldCapture(
        pkg: String,
        ownPackage: String,
        isOngoing: Boolean,
        isGroupSummary: Boolean,
        category: String?,
        includeMode: Boolean,
        included: Set<String>,
        excluded: Set<String>
    ): Boolean {
        if (pkg == ownPackage) return false          // jamais nos propres notifications
        if (isOngoing) return false                  // musique, navigation, appel en cours
        if (isGroupSummary) return false             // résumé de groupe
        if (category == CATEGORY_CALL) return false  // appels entrants / manqués
        return if (includeMode) pkg in included else pkg !in excluded
    }
}
