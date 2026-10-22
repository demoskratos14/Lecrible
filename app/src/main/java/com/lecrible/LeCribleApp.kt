package com.lecrible

import android.app.Application
import com.lecrible.data.Prefs

class LeCribleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Garde la trace du dernier plantage (visible dans l'onglet « État »), puis laisse Android faire son travail
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                Prefs(this).recordError("plantage (${thread.name})", e)
            } catch (t: Throwable) {
                // rien de plus à faire
            }
            previous?.uncaughtException(thread, e)
        }
        WatchdogJobService.schedule(this)
    }
}
