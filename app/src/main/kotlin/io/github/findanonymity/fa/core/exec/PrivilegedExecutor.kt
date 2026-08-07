package io.github.findanonymity.fa.core.exec

import kotlinx.serialization.Serializable

@Serializable
data class ShellResult(val exitCode: Int, val stdout: List<String>, val stderr: List<String>) {
    val isSuccess: Boolean get() = exitCode == 0
}

interface PrivilegedExecutor {
    suspend fun isAvailable(): Boolean
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun exec(command: String): ShellResult
}
