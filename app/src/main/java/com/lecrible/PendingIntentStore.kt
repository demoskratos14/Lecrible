package com.lecrible

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lecrible.data.NotifEntity
import java.util.concurrent.ConcurrentHashMap

/** Ce qui a réellement été lancé quand on touche une notification. */
enum class OpenResult { ORIGINAL, LINK, APP, FAILED }

/**
 * Garde en mémoire l'intention d'origine de chaque notification aspirée, pour la rouvrir comme Android le ferait
 * (par exemple directement la bonne conversation). Cette mémoire est perdue si Android arrête l'appli ou au
 * redémarrage du téléphone : on ouvre alors le lien trouvé dans la notification, à défaut l'application d'origine.
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

    /** Ouvre la notification, du plus précis au plus général : intention d'origine, lien, application. */
    fun open(ctx: Context, n: NotifEntity): OpenResult {
        map[n.notifKey]?.let { pi ->
            try {
                val options = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= 34) {
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pi.send(ctx, 0, null, null, null, null, options.toBundle())
                return OpenResult.ORIGINAL
            } catch (e: Exception) {
                // intention périmée : on tente le lien puis l'appli ci-dessous
            }
        }
        n.link?.let { url ->
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return OpenResult.LINK
            } catch (e: Exception) {
                // aucune appli pour ce lien
            }
        }
        val launch = ctx.packageManager.getLaunchIntentForPackage(n.pkg) ?: return OpenResult.FAILED
        return try {
            ctx.startActivity(launch)
            OpenResult.APP
        } catch (e: Exception) {
            OpenResult.FAILED
        }
    }
}
