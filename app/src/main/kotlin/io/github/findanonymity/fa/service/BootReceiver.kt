package io.github.findanonymity.fa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.BootFlagMirror
import io.github.findanonymity.fa.data.model.PanicLockConfig
import io.github.findanonymity.fa.panic.PanicDaemonInstaller
import io.github.findanonymity.fa.panic.SecureCredentialStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val app = context.applicationContext as FaApp
        val isBoot = intent.action == Intent.ACTION_BOOT_COMPLETED

        // Keep the process alive across the async work: reboot bookkeeping, panic re-arm, and
        // (re)starting the foreground service all need to finish after onReceive() returns.
        val pending = goAsync()
        app.appScope.launch {
            try {
                if (isBoot) {
                    app.configRepository.update { config ->
                        config.copy(rebootRule = config.rebootRule.copy(lastRebootEpochMillis = System.currentTimeMillis()))
                    }
                    // A reboot (including our own scheduled one) kills the detached panic daemon.
                    // If the user left it armed, bring it back up so protection is not silently lost.
                    val config = app.configRepository.configFlow.first()
                    if (config.panicLock.armed) {
                        rearmPanic(app, config.panicLock)
                    }
                }

                if (BootFlagMirror.read(context)) {
                    AutomationService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Re-installs the panic daemon from the Keystore-encrypted credentials. Fail-closed: if the
     * credentials are missing or root is unavailable, nothing happens and no password is touched.
     * After an untriggered reboot the stored `old` credential is still the current one, so this
     * restores the exact armed state. If a trigger had already rotated the password, the stale
     * `old` simply makes a future trigger a no-op (the lock screen stays on the new password)
     * until the user reconfigures — never a lockout.
     */
    private suspend fun rearmPanic(app: FaApp, panic: PanicLockConfig) {
        val store = SecureCredentialStore(app)
        val old = store.getCurrentCredential()
        val next = store.getNextPassword()
        if (old.isNullOrEmpty() || next.isNullOrEmpty()) return

        val installer = PanicDaemonInstaller(app.executorManager.rootOnlyExecutor())
        installer.arm(old, next, panic.pressCount, panic.windowMillis / 1000)
    }
}
