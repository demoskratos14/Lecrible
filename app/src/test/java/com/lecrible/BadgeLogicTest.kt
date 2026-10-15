package com.lecrible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeLogicTest {

    private val rouge = BadgePalette.colors[0]
    private val bleu = BadgePalette.colors[5]

    @Test fun `les applis de meme couleur sont regroupees dans une seule pastille`() {
        val couleurs = mapOf("a" to rouge, "b" to rouge, "c" to bleu)
        val dots = BadgeLogic.dots(listOf("a", "b", "c", "a")) { couleurs.getValue(it) }
        assertEquals(2, dots.size)
        assertEquals(BadgeDot(rouge, 3), dots.first())
        assertEquals(BadgeDot(bleu, 1), dots.last())
    }

    @Test fun `aucune notification donne aucune pastille`() {
        assertTrue(BadgeLogic.dots(emptyList()) { rouge }.isEmpty())
    }

    @Test fun `la pastille la plus fournie vient en premier`() {
        val couleurs = mapOf("a" to rouge, "b" to bleu)
        val dots = BadgeLogic.dots(listOf("b", "b", "a")) { couleurs.getValue(it) }
        assertEquals(bleu, dots.first().color)
    }

    @Test fun `le nombre est plafonne a 99+`() {
        assertEquals("5", BadgeLogic.label(5))
        assertEquals("99", BadgeLogic.label(99))
        assertEquals("99+", BadgeLogic.label(100))
    }

    @Test fun `la couleur par defaut fait partie de la palette`() {
        assertTrue(BadgePalette.default in BadgePalette.colors)
    }
}
