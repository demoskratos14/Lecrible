package com.lecrible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifFilterTest {

    private val own = "com.lecrible"

    private fun capture(
        pkg: String,
        ongoing: Boolean = false,
        summary: Boolean = false,
        category: String? = null,
        includeMode: Boolean = false,
        included: Set<String> = emptySet(),
        excluded: Set<String> = NotifFilter.DEFAULT_EXCLUDED
    ) = NotifFilter.shouldCapture(pkg, own, ongoing, summary, category, includeMode, included, excluded)

    @Test fun `mode exclusion - une appli quelconque est aspiree`() {
        assertTrue(capture("com.exemple.news"))
    }

    @Test fun `mode exclusion - WhatsApp est exclu par defaut`() {
        assertFalse(capture("com.whatsapp"))
    }

    @Test fun `mode exclusion - les applis SMS et telephone sont exclues par defaut`() {
        listOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.google.android.dialer",
            "com.android.server.telecom"
        ).forEach { assertFalse("$it devrait être exclu", capture(it)) }
    }

    @Test fun `mode exclusion - une exclusion personnalisee est respectee`() {
        assertFalse(capture("com.exemple.news", excluded = setOf("com.exemple.news")))
        assertTrue(capture("com.whatsapp", excluded = emptySet()))
    }

    @Test fun `mode inclusion - seules les applis cochees sont aspirees`() {
        val included = setOf("com.exemple.news")
        assertTrue(capture("com.exemple.news", includeMode = true, included = included))
        assertFalse(capture("com.exemple.autre", includeMode = true, included = included))
    }

    @Test fun `mode inclusion - liste vide n'aspire rien`() {
        assertFalse(capture("com.exemple.news", includeMode = true))
    }

    @Test fun `les notifications en cours sont ignorees`() {
        assertFalse(capture("com.exemple.musique", ongoing = true))
    }

    @Test fun `les resumes de groupe sont ignores`() {
        assertFalse(capture("com.exemple.news", summary = true))
    }

    @Test fun `les appels sont ignores quelle que soit l'appli`() {
        assertFalse(capture("com.exemple.visio", category = NotifFilter.CATEGORY_CALL))
        assertTrue(capture("com.exemple.visio", category = "msg"))
    }

    @Test fun `les notifications de Le Crible sont ignorees meme en mode inclusion`() {
        assertFalse(capture(own, includeMode = true, included = setOf(own)))
    }

    @Test fun `la categorie d'appel correspond a la constante Android`() {
        assertEquals("call", NotifFilter.CATEGORY_CALL)
    }

    @Test fun `avec l'option permanentes, les notifications en cours sont aspirees`() {
        assertTrue(
            NotifFilter.shouldCapture("com.exemple.irc", own, true, false, null, false, emptySet(), emptySet(), includeOngoing = true)
        )
    }

    @Test fun `l'option permanentes n'autorise pas les resumes de groupe ni les appels`() {
        assertFalse(
            NotifFilter.shouldCapture("com.exemple.irc", own, true, true, null, false, emptySet(), emptySet(), includeOngoing = true)
        )
        assertFalse(
            NotifFilter.shouldCapture("com.exemple.visio", own, true, false, NotifFilter.CATEGORY_CALL, false, emptySet(), emptySet(), includeOngoing = true)
        )
    }
}
