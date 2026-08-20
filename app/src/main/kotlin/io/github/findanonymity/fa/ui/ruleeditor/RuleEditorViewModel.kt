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
                    ToggleTarget.BLUETOOTH -> config.copy(bluetoothRule = toPersist)
                    ToggleTarget.LOCATION -> config.copy(locationRule = toPersist)
                }
            }
        }
    }

    /** Applies one toggle rule to all three targets (Wi-Fi, mobile data, airplane) at once. */
    fun saveAllToggleRules(rule: ToggleRuleConfig) {
        val toPersist = if (rule.mode == ToggleMode.CYCLICAL) {
            rule.copy(cycleAnchorEpochMillis = System.currentTimeMillis())
        } else {
            rule
        }
        viewModelScope.launch {
            app.configRepository.update { config ->
                config.copy(
                    wifiRule = toPersist,
                    dataRule = toPersist,
                    airplaneModeRule = toPersist,
                    bluetoothRule = toPersist,
                    locationRule = toPersist,
                )
            }
        }
    }

    fun saveRebootRule(rule: RebootRuleConfig) {
        // Anchor the interval to "now" on save so both the countdown and the forced daemon start fresh.
        val toPersist = rule.copy(lastRebootEpochMillis = System.currentTimeMillis())
        viewModelScope.launch {
            app.configRepository.update { config -> config.copy(rebootRule = toPersist) }
            val installer = io.github.findanonymity.fa.service.RebootDaemonInstaller(app.executorManager::exec)
            if (toPersist.enabled && toPersist.forced) {
                installer.arm(toPersist.interval.toMillis() / 1000)
            } else {
                installer.disarm()
            }
        }
    }
}
