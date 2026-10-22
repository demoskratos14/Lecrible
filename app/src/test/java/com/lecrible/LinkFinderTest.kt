package com.lecrible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkFinderTest {

    @Test fun trouve_un_lien_dans_le_texte() {
        assertEquals("https://exemple.org/a?b=1", LinkFinder.first("Voir https://exemple.org/a?b=1 maintenant"))
    }

    @Test fun retire_la_ponctuation_finale() {
        assertEquals("https://exemple.org/page", LinkFinder.first("Lien : (https://exemple.org/page)."))
    }

    @Test fun cherche_dans_les_textes_dans_l_ordre_et_ignore_les_vides() {
        assertEquals("http://b.fr", LinkFinder.first(null, "", "rien ici", "http://b.fr", "https://c.fr"))
    }

    @Test fun renvoie_null_sans_lien() {
        assertNull(LinkFinder.first("pas de lien", null))
        assertNull(LinkFinder.first("https://"))
    }
}
