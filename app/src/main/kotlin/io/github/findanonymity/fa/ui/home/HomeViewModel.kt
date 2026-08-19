package io.github.findanonymity.fa.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.core.exec.BackendState
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.service.AutomationService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<FaApp>()

    val configFlow: StateFlow<AppConfig> = app.configRepository.configFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConfig(),
    )

    val backendState: StateFlow<BackendState> = app.executorManager.state

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.configRepository.update { it.copy(masterAutomationEnabled = enabled) }
            if (enabled) {
                AutomationService.start(app)
            } else {
                AutomationService.stop(app)
            }
        }
    }

    fun refreshBackendState() {
        viewModelScope.launch { app.executorManager.refreshState() }
    }

    /** Triggers Shizuku's own "connect / grant permission" dialog (the Canta-style flow). */
    fun connectShizuku() {
        viewModelScope.launch { app.executorManager.requestShizukuPermission() }
    }
}
