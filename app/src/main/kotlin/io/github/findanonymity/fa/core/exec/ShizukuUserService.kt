package io.github.findanonymity.fa.core.exec

import io.github.findanonymity.fa.IUserService
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

/**
 * Runs inside the process Shizuku binds with shell (uid 2000) or root (uid 0) privileges,
 * depending on whether the user paired via Wireless Debugging or a root-mode Shizuku (Sui).
 * Must NOT reference Android Context/Application state — this is a bare privileged process.
 */
class ShizukuUserService : IUserService.Stub() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun exec(command: String): String {
        val result = runCommand(command)
        return json.encodeToString(ShellResult.serializer(), result)
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun runCommand(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(false).start()
            // Drain stdout/stderr on separate threads: reading them sequentially on this thread
            // would deadlock if the process fills the OS pipe buffer on the stream read second.
            var stdout: List<String> = emptyList()
            val stdoutThread = Thread {
                stdout = process.inputStream.bufferedReader().readLines()
            }
            stdoutThread.start()
            val stderr = process.errorStream.bufferedReader().readLines()
            stdoutThread.join()
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, emptyList(), listOf(e.message ?: "unknown error"))
        }
    }
}
