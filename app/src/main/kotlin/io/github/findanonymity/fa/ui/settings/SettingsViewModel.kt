package io.github.findanonymity.fa.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.BackendPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<FaApp>()

    val configFlow: StateFlow<AppConfig> = app.configRepository.configFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConfig(),
    )

    fun setBackendPreference(preference: BackendPreference) {
        viewModelScope.launch {
            app.configRepository.update { it.copy(preferredBackend = preference) }
        }
    }
}
