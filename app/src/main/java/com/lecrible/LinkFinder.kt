package com.lecrible

/** Cherche un lien http(s) dans le texte d'une notification (sans dépendance à Android : testable en JVM). */
object LinkFinder {
    private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private const val TRAILING = ".,;:!?)]}»…"

    /** Premier lien trouvé dans les textes donnés (dans l'ordre), ou null. */
    fun first(vararg texts: String?): String? {
        for (t in texts) {
            if (t.isNullOrBlank()) continue
            val match = URL.find(t) ?: continue
            val url = match.value.trimEnd { it in TRAILING }
            if (url.length > "https://".length) return url
        }
        return null
    }
}
