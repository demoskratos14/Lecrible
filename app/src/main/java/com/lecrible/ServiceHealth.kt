package com.lecrible

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat

object ServiceHealth {

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    /**
     * Coupe puis réactive le composant du service : force Android à le relier de nouveau quand un simple
     * requestRebind ne suffit pas (service resté « mort » alors que l'accès est bien accordé).
     */
    fun reconnect(ctx: Context) {
        val cn = ComponentName(ctx, NotifService::class.java)
        try {
            val pm = ctx.packageManager
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        } catch (e: Exception) {
            Log.w("LeCrible", "reconnect : bascule du composant impossible", e)
        }
        try {
            NotificationListenerService.requestRebind(cn)
        } catch (e: Exception) {
            Log.w("LeCrible", "reconnect : requestRebind impossible", e)
        }
    }
}
