package com.lecrible

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.lecrible.data.NotifEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * Garde en mémoire l'intention d'origine de chaque notification aspirée, pour la rouvrir comme Android le ferait
 * (par exemple directement la bonne conversation). Cette mémoire est perdue si Android arrête l'appli ou au
 * redémarrage du téléphone : on lance alors simplement l'application d'origine.
 */
object PendingIntentStore {
    private val map = ConcurrentHashMap<String, PendingIntent>()

    fun put(key: String, pi: PendingIntent?) {
        if (pi != null) map[key] = pi
    }

    fun remove(key: String) {
        map.remove(key)
    }

    fun clear() = map.clear()

    /** Ouvre la notification ; renvoie false si rien n'a pu être lancé. */
    fun open(ctx: Context, n: NotifEntity): Boolean {
        map[n.notifKey]?.let { pi ->
            try {
                val options = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= 34) {
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pi.send(ctx, 0, null, null, null, null, options.toBundle())
                return true
            } catch (e: Exception) {
                // intention périmée : on tente le lancement simple ci-dessous
            }
        }
        val launch = ctx.packageManager.getLaunchIntentForPackage(n.pkg) ?: return false
        return try {
            ctx.startActivity(launch)
            true
        } catch (e: Exception) {
            false
        }
    }
}
