package com.lecrible

/** Une pastille : une couleur et le nombre de notifications en attente pour cette couleur. */
data class BadgeDot(val color: Int, val count: Int)

/** Une bulle du widget : une application, sa couleur et son nombre de notifications. */
data class AppBubble(val pkg: String, val color: Int, val count: Int)

object BadgePalette {
    val colors: List<Int> = listOf(
        0xFFFF5252, 0xFFFF9800, 0xFFFFD600, 0xFF4CAF50,
        0xFF2EE6C8, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63
    ).map { it.toInt() }

    /** Turquoise, comme le logo. */
    val default: Int = colors[4]
}

object BadgeLogic {
    /** Regroupe les notifications par couleur d'appli ; la couleur la plus fournie vient en premier. */
    fun dots(pkgs: List<String>, colorOf: (String) -> Int): List<BadgeDot> =
        pkgs.groupBy(colorOf)
            .map { (color, list) -> BadgeDot(color, list.size) }
            .sortedByDescending { it.count }

    fun label(count: Int): String = if (count > 99) "99+" else count.toString()

    /** Une bulle par application, la plus fournie en premier (puis ordre alphabétique pour rester stable). */
    fun bubbles(pkgs: List<String>, colorOf: (String) -> Int): List<AppBubble> =
        pkgs.groupingBy { it }.eachCount()
            .map { (pkg, n) -> AppBubble(pkg, colorOf(pkg), n) }
            .sortedWith(compareByDescending<AppBubble> { it.count }.thenBy { it.pkg })

    /** Découpe les bulles en pages (4 par défaut) : on change de page en glissant le doigt sur le widget. */
    fun pages(bubbles: List<AppBubble>, perPage: Int = 4): List<List<AppBubble>> = bubbles.chunked(perPage)
}
