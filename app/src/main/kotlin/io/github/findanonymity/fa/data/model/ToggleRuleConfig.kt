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
    // SCHEDULED mode: a daily clock-time window on selected weekdays.
    val scheduleStartMinute: Int = 8 * 60,   // 08:00, minute-of-day
    val scheduleEndMinute: Int = 22 * 60,    // 22:00, minute-of-day
    val scheduleDaysMask: Int = 0x7F,        // bit0=Mon … bit6=Sun; all days by default
    val scheduleOnDuringWindow: Boolean = true, // true: ON inside window / OFF outside (invert if false)
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
    BLUETOOTH(R.string.target_bluetooth, TargetCommands("svc bluetooth enable", "svc bluetooth disable")),
    LOCATION(
        R.string.target_location,
        TargetCommands(
            on = "cmd location set-location-enabled true",
            off = "cmd location set-location-enabled false",
        ),
    ),
}

data class TargetCommands(val on: String, val off: String)
