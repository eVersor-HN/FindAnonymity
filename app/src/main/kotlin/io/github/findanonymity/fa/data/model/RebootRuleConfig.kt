package io.github.findanonymity.fa.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RebootRuleConfig(
    val enabled: Boolean = false,
    val interval: Duration2 = Duration2(1, CycleTimeUnit.DAYS),
    val lastRebootEpochMillis: Long? = null,
    /**
     * When true the reboot is driven by a detached privileged daemon (root/Shizuku shell) that
     * survives FA's own process being stopped — including a thief tapping "Stop" on the
     * notification — so the protective reboot still fires. When false the reboot is handled by the
     * foreground service and stops if the app is stopped.
     */
    val forced: Boolean = false,
)
