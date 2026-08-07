package io.github.findanonymity.fa.ui.ruleeditor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleMode
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.data.model.ToggleTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RuleEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<FaApp>()

    val configFlow: StateFlow<AppConfig?> = app.configRepository.configFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    fun saveToggleRule(target: ToggleTarget, rule: ToggleRuleConfig) {
        val toPersist = if (rule.mode == ToggleMode.CYCLICAL) {
            rule.copy(cycleAnchorEpochMillis = System.currentTimeMillis())
        } else {
            rule
        }
        viewModelScope.launch {
            app.configRepository.update { config ->
                when (target) {
                    ToggleTarget.WIFI -> config.copy(wifiRule = toPersist)
                    ToggleTarget.MOBILE_DATA -> config.copy(dataRule = toPersist)
                    ToggleTarget.AIRPLANE_MODE -> config.copy(airplaneModeRule = toPersist)
                }
            }
        }
    }

    fun saveRebootRule(rule: RebootRuleConfig) {
        viewModelScope.launch {
            app.configRepository.update { config -> config.copy(rebootRule = rule) }
        }
    }
}
