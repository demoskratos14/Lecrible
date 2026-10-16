package com.lecrible

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils

/** Dessine une page du widget : jusqu'à 4 bulles (2 x 2) avec leur nombre, et des points si plusieurs pages. */
object BadgeRenderer {

    fun renderPage(page: List<AppBubble>, index: Int, total: Int): Bitmap {
        val size = 360
        val d = 136          // diamètre d'une bulle
        val gap = 16
        val startX = 14f
        val startY = (size - (2 * d + gap)) / 2f

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        page.take(4).forEachIndexed { i, bubble ->
            val cx = startX + (i % 2) * (d + gap) + d / 2f
            val cy = startY + (i / 2) * (d + gap) + d / 2f
            fill.color = bubble.color
            canvas.drawCircle(cx, cy, d / 2f, fill)
            val label = BadgeLogic.label(bubble.count)
            text.textSize = if (label.length > 2) 46f else 62f
            text.color = if (ColorUtils.calculateLuminance(bubble.color) > 0.55) Color.BLACK else Color.WHITE
            canvas.drawText(label, cx, cy - (text.descent() + text.ascent()) / 2f, text)
        }

        // Petits points à droite : indiquent qu'il y a d'autres pages (glisser vers le haut ou le bas)
        if (total > 1) {
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            val shown = total.coerceAtMost(6)
            val spacing = 26f
            val top = size / 2f - (shown - 1) * spacing / 2f
            for (k in 0 until shown) {
                dot.color = if (k == index) Color.WHITE else 0x66FFFFFF
                canvas.drawCircle(size - 22f, top + k * spacing, 7f, dot)
            }
        }
        return bmp
    }
}
