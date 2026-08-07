package io.github.findanonymity.fa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.BootFlagMirror
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val app = context.applicationContext as FaApp
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            app.appScope.launch {
                app.configRepository.update { config ->
                    config.copy(rebootRule = config.rebootRule.copy(lastRebootEpochMillis = System.currentTimeMillis()))
                }
            }
        }

        if (BootFlagMirror.read(context)) {
            AutomationService.start(context)
        }
    }
}
