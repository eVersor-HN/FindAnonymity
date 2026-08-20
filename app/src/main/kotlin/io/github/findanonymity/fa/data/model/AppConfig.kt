package io.github.findanonymity.fa.data.model

import androidx.annotation.StringRes
import io.github.findanonymity.fa.R
import kotlinx.serialization.Serializable

@Serializable
enum class BackendPreference(@StringRes val labelRes: Int) {
    AUTO(R.string.backend_auto),
    ROOT(R.string.backend_root_only),
    SHIZUKU(R.string.backend_shizuku_only),
}

@Serializable
data class AppConfig(
    val masterAutomationEnabled: Boolean = false,
    val wifiRule: ToggleRuleConfig = ToggleRuleConfig(),
    val dataRule: ToggleRuleConfig = ToggleRuleConfig(),
    val airplaneModeRule: ToggleRuleConfig = ToggleRuleConfig(),
    val bluetoothRule: ToggleRuleConfig = ToggleRuleConfig(),
    val locationRule: ToggleRuleConfig = ToggleRuleConfig(),
    val rebootRule: RebootRuleConfig = RebootRuleConfig(),
    val panicLock: PanicLockConfig = PanicLockConfig(),
    val preferredBackend: BackendPreference = BackendPreference.AUTO,
)
