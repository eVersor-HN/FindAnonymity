package io.github.findanonymity.fa.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PanicLockConfig(
    val armed: Boolean = false,
    val passwordLength: Int = 100,
    val pressCount: Int = 5,
    val windowMillis: Long = 3_000L,
    val backupConfirmedAtEpochMillis: Long? = null,
    val pendingPasswordSetAtEpochMillis: Long? = null,
    val lastTriggeredEpochMillis: Long? = null,
)
