package io.github.findanonymity.fa.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RebootRuleConfig(
    val enabled: Boolean = false,
    val interval: Duration2 = Duration2(1, CycleTimeUnit.DAYS),
    val lastRebootEpochMillis: Long? = null,
)
