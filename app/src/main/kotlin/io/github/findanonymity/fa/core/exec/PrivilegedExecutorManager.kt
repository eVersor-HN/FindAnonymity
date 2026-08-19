package io.github.findanonymity.fa.core.exec

import android.content.Context
import io.github.findanonymity.fa.data.model.BackendPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BackendState {
    data object NoneAvailable : BackendState
    data object RootAvailable : BackendState
    data object ShizukuNeedsPermission : BackendState
    data object ShizukuAvailable : BackendState
}

/**
 * Detects and selects between the Root and Shizuku backends. Deliberately re-probes on every
 * exec() failure rather than caching "unavailable" forever — lets the app self-heal once Shizuku
 * is reopened by the user after a reboot, or once root access is granted mid-session.
 */
class PrivilegedExecutorManager(context: Context) {

    private val appContext = context.applicationContext
    private val rootExecutor = RootExecutor()
    private val shizukuExecutor = ShizukuExecutor(appContext)

    private val _state = MutableStateFlow<BackendState>(BackendState.NoneAvailable)
    val state: StateFlow<BackendState> = _state.asStateFlow()

    var preference: BackendPreference = BackendPreference.AUTO

    suspend fun refreshState() {
        _state.value = detectState()
    }

    suspend fun requestShizukuPermission(): Boolean {
        val granted = shizukuExecutor.requestPermission()
        refreshState()
        return granted
    }

    suspend fun checkRoot(): Boolean {
        val granted = rootExecutor.requestPermission()
        refreshState()
        return granted
    }

    /**
     * Executes [command] on whichever backend is currently usable, honoring [preference].
     * On failure, re-probes the other backend before giving up (self-healing).
     */
    suspend fun exec(command: String): ShellResult {
        val primary = selectExecutor()
        if (primary != null) {
            val result = primary.exec(command)
            if (result.isSuccess) return result
        }
        // Primary failed or unavailable: re-probe and try whichever backend the failed one wasn't.
        refreshState()
        val fallback = selectExecutor()?.takeIf { it !== primary }
        return fallback?.exec(command)
            ?: ShellResult(-1, emptyList(), listOf("No privileged access available (neither root nor Shizuku)"))
    }

    private suspend fun selectExecutor(): PrivilegedExecutor? {
        return when (preference) {
            BackendPreference.ROOT -> rootExecutor.takeIf { it.hasPermission() }
            BackendPreference.SHIZUKU -> shizukuExecutor.takeIf { it.hasPermission() }
            BackendPreference.AUTO -> when {
                rootExecutor.hasPermission() -> rootExecutor
                shizukuExecutor.hasPermission() -> shizukuExecutor
                else -> null
            }
        }
    }

    private suspend fun detectState(): BackendState {
        if (rootExecutor.hasPermission()) return BackendState.RootAvailable
        if (shizukuExecutor.isAvailable()) {
            return if (shizukuExecutor.hasPermission()) {
                BackendState.ShizukuAvailable
            } else {
                BackendState.ShizukuNeedsPermission
            }
        }
        return BackendState.NoneAvailable
    }

    /** Root executor — the strong, reboot-persistent backend for the panic-lock. */
    fun rootOnlyExecutor(): PrivilegedExecutor = rootExecutor

    /**
     * Shizuku executor — the experimental panic-lock backend. Weaker guarantee than root:
     * the watcher orphans to a shell-uid (2000) process that survives until reboot, but there
     * is no auto re-arm on boot (Shizuku is not running that early), and `locksettings
     * set-password` from shell uid is not guaranteed across all OEM/Android versions.
     */
    fun shizukuOnlyExecutor(): PrivilegedExecutor = shizukuExecutor
}
