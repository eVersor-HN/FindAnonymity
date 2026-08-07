package io.github.findanonymity.fa.data.model

import androidx.annotation.StringRes
import io.github.findanonymity.fa.R
import kotlinx.serialization.Serializable

@Serializable
data class ToggleRuleConfig(
    val enabled: Boolean = false,
    val mode: ToggleMode = ToggleMode.UNMANAGED,
    val cycleInterval: Duration2 = Duration2(60, CycleTimeUnit.MINUTES),
    val activeDuration: Duration2 = Duration2(5, CycleTimeUnit.MINUTES),
    val startOn: Boolean = true,
    val cycleAnchorEpochMillis: Long? = null,
)

enum class ToggleTarget(@StringRes val labelRes: Int, val command: TargetCommands) {
    WIFI(R.string.target_wifi, TargetCommands("svc wifi enable", "svc wifi disable")),
    MOBILE_DATA(R.string.target_mobile_data, TargetCommands("svc data enable", "svc data disable")),
    AIRPLANE_MODE(
        R.string.target_airplane_mode,
        TargetCommands(
            on = "settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true",
            off = "settings put global airplane_mode_on 0 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false",
        ),
    ),
}

data class TargetCommands(val on: String, val off: String)
