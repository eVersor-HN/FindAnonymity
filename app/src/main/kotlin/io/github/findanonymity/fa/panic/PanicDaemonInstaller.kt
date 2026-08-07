package io.github.findanonymity.fa.panic

import io.github.findanonymity.fa.core.exec.PrivilegedExecutor

/**
 * Installs/removes the detached root daemon that watches for rapid power-button presses (via raw
 * `getevent` input-device monitoring, the only way to observe the power key outside the normal
 * Android app framework) and applies the password rotation directly from root context — so it
 * still works even if FA's own process/foreground service has been killed.
 *
 * Root only by design (see plan): reliably bypassing `locksettings set-password`'s old-credential
 * check is uncertain under Shizuku's shell-uid process, so this feature does not offer a Shizuku
 * path. The credential files this writes are plaintext while armed, restricted to a root-owned,
 * mode-700 directory outside any world-writable path (deliberately NOT /data/local/tmp, which is
 * world-writable on stock Android) — an accepted, disclosed residual risk of a daemon that must
 * keep working even when FA itself is not running.
 */
class PanicDaemonInstaller(private val executor: PrivilegedExecutor) {

    suspend fun findPowerKeyDevice(): String? {
        val result = executor.exec("getevent -pl")
        if (!result.isSuccess) return null
        var currentPath: String? = null
        val addDeviceRegex = Regex("""add device \d+: (/dev/input/\S+)""")
        for (line in result.stdout) {
            addDeviceRegex.find(line)?.let { currentPath = it.groupValues[1] }
            if (line.contains("KEY_POWER") && currentPath != null) {
                return currentPath
            }
        }
        return null
    }

    suspend fun arm(
        oldCredential: String,
        newPassword: String,
        pressCount: Int,
        windowSeconds: Long,
    ): ArmResult {
        val device = findPowerKeyDevice() ?: return ArmResult.NoPowerKeyDeviceFound

        val setupResult = executor.exec("mkdir -p $BASE_DIR && chmod 700 $BASE_DIR")
        if (!setupResult.isSuccess) return ArmResult.CommandFailed(setupResult.stderr.joinToString())

        val writeOld = executor.exec(
            "printf '%s' ${shellSingleQuote(oldCredential)} > $BASE_DIR/credential.old && chmod 600 $BASE_DIR/credential.old",
        )
        val writeNew = executor.exec(
            "printf '%s' ${shellSingleQuote(newPassword)} > $BASE_DIR/credential.new && chmod 600 $BASE_DIR/credential.new",
        )
        if (!writeOld.isSuccess || !writeNew.isSuccess) {
            return ArmResult.CommandFailed((writeOld.stderr + writeNew.stderr).joinToString())
        }

        val script = buildWatcherScript(device, pressCount, windowSeconds)
        val writeScript = executor.exec(
            "cat > $BASE_DIR/watch.sh << 'FA_PANIC_EOF'\n$script\nFA_PANIC_EOF\nchmod 700 $BASE_DIR/watch.sh",
        )
        if (!writeScript.isSuccess) return ArmResult.CommandFailed(writeScript.stderr.joinToString())

        // Stop any previous instance before starting a fresh one, then launch fully detached.
        executor.exec("pkill -f $BASE_DIR/watch.sh")
        val start = executor.exec("nohup sh $BASE_DIR/watch.sh > /dev/null 2>&1 &")
        return if (start.isSuccess) ArmResult.Armed else ArmResult.CommandFailed(start.stderr.joinToString())
    }

    suspend fun disarm(): Boolean {
        executor.exec("pkill -f $BASE_DIR/watch.sh")
        val result = executor.exec("rm -rf $BASE_DIR")
        return result.isSuccess
    }

    private fun buildWatcherScript(device: String, pressCount: Int, windowSeconds: Long): String = """
        #!/system/bin/sh
        DEV='$device'
        PRESS_COUNT=$pressCount
        WINDOW_S=$windowSeconds
        BASE_DIR='$BASE_DIR'
        count=0
        last_ts=0
        getevent -l "${'$'}DEV" | while read -r line; do
          case "${'$'}line" in
            *EV_KEY*KEY_POWER*DOWN*)
              now=${'$'}(date +%s)
              elapsed=${'$'}((now - last_ts))
              if [ "${'$'}elapsed" -gt "${'$'}WINDOW_S" ]; then
                count=1
              else
                count=${'$'}((count + 1))
              fi
              last_ts=${'$'}now
              if [ "${'$'}count" -ge "${'$'}PRESS_COUNT" ]; then
                OLD=${'$'}(cat "${'$'}BASE_DIR/credential.old")
                NEW=${'$'}(cat "${'$'}BASE_DIR/credential.new")
                locksettings set-password --old "${'$'}OLD" "${'$'}NEW"
                count=0
              fi
              ;;
          esac
        done
    """.trimIndent()

    private fun shellSingleQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    sealed interface ArmResult {
        data object Armed : ArmResult
        data object NoPowerKeyDeviceFound : ArmResult
        data class CommandFailed(val message: String) : ArmResult
    }

    companion object {
        private const val BASE_DIR = "/data/local/fa_panic"
    }
}
