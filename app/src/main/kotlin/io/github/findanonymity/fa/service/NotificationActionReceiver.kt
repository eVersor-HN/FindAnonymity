package io.github.findanonymity.fa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.findanonymity.fa.FaApp
import kotlinx.coroutines.launch

/**
 * Handles the "Stop" action on the ongoing status notification: turns master automation off
 * (so it stays off across reboots via the boot flag mirror) and stops the foreground service.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        val app = context.applicationContext as FaApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.configRepository.update { it.copy(masterAutomationEnabled = false) }
            } finally {
                AutomationService.stop(context)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_STOP = "io.github.findanonymity.fa.action.STOP_AUTOMATION"
    }
}
