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
        // Primary failed or unavailable: re-probe and try the fallback.
        refreshState()
        val fallback = selectExecutor()
        return fallback?.exec(command)
            ?: ShellResult(-1, emptyList(), listOf("Kein privilegierter Zugriff verfügbar (weder Root noch Shizuku)"))
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

    /** Root-only executor, used exclusively by the panic-lock feature (see plan: root-only by design). */
    fun rootOnlyExecutor(): PrivilegedExecutor = rootExecutor
}
