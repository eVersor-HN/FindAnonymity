package io.github.findanonymity.fa.core.exec

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Root backend via libsu. The first [exec] call may surface the su-manager grant prompt
 * (Magisk/KernelSU) to the user; deliberately not probed automatically at every app start —
 * only from an explicit "Check for root" action in the setup UI.
 */
class RootExecutor : PrivilegedExecutor {

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        Shell.isAppGrantedRoot() ?: Shell.getShell().isRoot
    }

    override suspend fun hasPermission(): Boolean = isAvailable()

    override suspend fun requestPermission(): Boolean = withContext(Dispatchers.IO) {
        // If a non-root shell was cached from an earlier attempt (e.g. before the user granted
        // root in KernelSU, which — unlike Magisk — has no automatic prompt), drop it so a fresh
        // shell can pick up the new grant without needing an app restart.
        runCatching { Shell.getCachedShell()?.takeIf { !it.isRoot }?.close() }
        Shell.getShell().isRoot
    }

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        ShellResult(result.code, result.out, result.err)
    }
}
