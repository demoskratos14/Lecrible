package com.lecrible

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Chien de garde : toutes les ~15 minutes, vérifie que le service de notifications est bien relié.
 * S'il ne l'est pas alors que l'accès est accordé, demande à Android de le relier de nouveau.
 */
class WatchdogJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = scope.launch {
            try {
                val ctx = applicationContext
                if (ServiceHealth.hasAccess(ctx) && !NotifService.connected) {
                    try {
                        NotificationListenerService.requestRebind(ComponentName(ctx, NotifService::class.java))
                    } catch (e: Exception) {
                        Log.w(TAG, "requestRebind", e)
                    }
                    delay(8_000)   // laisse le temps à Android de relier le service
                    if (!NotifService.connected) ServiceHealth.reconnect(ctx)
                }
            } catch (e: Exception) {
                Log.w(TAG, "chien de garde", e)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false
    }

    companion object {
        private const val TAG = "LeCrible"
        private const val JOB_ID = 4242

        fun schedule(ctx: Context) {
            try {
                val js = ctx.getSystemService(JobScheduler::class.java)
                if (js.getPendingJob(JOB_ID) != null) return
                js.schedule(
                    JobInfo.Builder(JOB_ID, ComponentName(ctx, WatchdogJobService::class.java))
                        .setPeriodic(15 * 60 * 1000L)
                        .setPersisted(true)
                        .build()
                )
            } catch (e: Exception) {
                Log.w(TAG, "planification du chien de garde impossible", e)
            }
        }
    }
}
