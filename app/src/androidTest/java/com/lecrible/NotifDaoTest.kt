package com.lecrible

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lecrible.data.AppDb
import com.lecrible.data.NotifDao
import com.lecrible.data.NotifEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotifDaoTest {

    private lateinit var db: AppDb
    private lateinit var dao: NotifDao

    private fun notif(key: String, pkg: String, time: Long, text: String = "texte") =
        NotifEntity(notifKey = key, pkg = pkg, appName = pkg, title = "titre", text = text, time = time)

    @Before fun creerBase() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).build()
        dao = db.dao()
    }

    @After fun fermerBase() = db.close()

    @Test fun insertion_et_lecture_triee_du_plus_recent_au_plus_ancien() = runBlocking {
        dao.insert(notif("a", "app.un", 1000))
        dao.insert(notif("b", "app.deux", 3000))
        dao.insert(notif("c", "app.un", 2000))
        assertEquals(listOf("b", "c", "a"), dao.all().first().map { it.notifKey })
    }

    @Test fun une_mise_a_jour_de_la_meme_notification_remplace_l_ancienne() = runBlocking {
        dao.insert(notif("a", "app.un", 1000, text = "v1"))
        dao.insert(notif("a", "app.un", 1500, text = "v2"))
        val toutes = dao.all().first()
        assertEquals(1, toutes.size)
        assertEquals("v2", toutes.first().text)
    }

    @Test fun suppression_unitaire_par_appli_et_totale() = runBlocking {
        dao.insert(notif("a", "app.un", 1))
        dao.insert(notif("b", "app.un", 2))
        dao.insert(notif("c", "app.deux", 3))

        dao.delete(dao.all().first().first { it.notifKey == "a" }.id)
        assertEquals(2, dao.all().first().size)

        dao.deleteApp("app.un")
        assertEquals(listOf("c"), dao.all().first().map { it.notifKey })

        dao.clear()
        assertEquals(0, dao.all().first().size)
    }

    @Test fun recherche_par_cle_et_vidage_sauf_permanentes() = runBlocking {
        dao.insert(notif("a", "app.un", 1))
        dao.insert(notif("p", "app.un", 2).copy(ongoing = true, link = "https://exemple.org"))
        assertEquals("https://exemple.org", dao.byKey("p")?.link)
        assertEquals(null, dao.byKey("absente"))

        dao.clearRegular()
        assertEquals(listOf("p"), dao.all().first().map { it.notifKey })
    }
}
