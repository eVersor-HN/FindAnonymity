package io.github.findanonymity.fa.ui.panic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.PanicLockConfig
import io.github.findanonymity.fa.panic.PanicDaemonInstaller
import io.github.findanonymity.fa.panic.PasswordGenerator
import io.github.findanonymity.fa.panic.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PanicViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<FaApp>()
    private val credentialStore = SecureCredentialStore(app)
    private val daemonInstaller = PanicDaemonInstaller(app.executorManager.rootOnlyExecutor())

    val configFlow: StateFlow<AppConfig?> = app.configRepository.configFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    private val _pendingPassword = MutableStateFlow(credentialStore.getNextPassword())
    val pendingPassword: StateFlow<String?> = _pendingPassword.asStateFlow()

    private val _armResult = MutableStateFlow<PanicDaemonInstaller.ArmResult?>(null)
    val armResult: StateFlow<PanicDaemonInstaller.ArmResult?> = _armResult.asStateFlow()

    fun generateNextPassword(length: Int) {
        val password = PasswordGenerator.generate(length)
        credentialStore.saveNextPassword(password)
        _pendingPassword.value = password
        updatePanicConfig { it.copy(passwordLength = length, backupConfirmedAtEpochMillis = null) }
    }

    fun setCurrentCredential(value: String) {
        credentialStore.saveCurrentCredential(value)
    }

    fun updateTuning(pressCount: Int, windowMillis: Long) {
        updatePanicConfig { it.copy(pressCount = pressCount, windowMillis = windowMillis) }
    }

    fun confirmBackupAndArm() {
        val old = credentialStore.getCurrentCredential()
        val next = credentialStore.getNextPassword()
        if (old.isNullOrEmpty() || next.isNullOrEmpty()) {
            _armResult.value = PanicDaemonInstaller.ArmResult.CommandFailed("Missing credential or pending password")
            return
        }
        viewModelScope.launch {
            val config = configFlow.value?.panicLock ?: PanicLockConfig()
            val result = daemonInstaller.arm(old, next, config.pressCount, config.windowMillis / 1000)
            _armResult.value = result
            if (result is PanicDaemonInstaller.ArmResult.Armed) {
                updatePanicConfig {
                    it.copy(
                        armed = true,
                        backupConfirmedAtEpochMillis = System.currentTimeMillis(),
                    )
                }
                // The daemon now holds both secrets in RAM; drop the app's own copies so the
                // real lock-screen password no longer lives in any at-rest store while armed.
                credentialStore.clear()
                _pendingPassword.value = null
            }
        }
    }

    fun disarm() {
        viewModelScope.launch {
            daemonInstaller.disarm()
            credentialStore.clear()
            _pendingPassword.value = null
            updatePanicConfig { PanicLockConfig() }
        }
    }

    private fun updatePanicConfig(transform: (PanicLockConfig) -> PanicLockConfig) {
        viewModelScope.launch {
            app.configRepository.update { it.copy(panicLock = transform(it.panicLock)) }
        }
    }
}
