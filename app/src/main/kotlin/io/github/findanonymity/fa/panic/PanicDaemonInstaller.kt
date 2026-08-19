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
 * path.
 *
 * Credentials at rest: the two secrets are written to a root-owned, mode-700 directory (mode-600
 * files) outside any world-writable path (deliberately NOT /data/local/tmp, which is world-writable
 * on stock Android) only for the brief moment between arming and the daemon starting. The daemon's
 * first act is to read them into its own memory and shred+unlink the files, so for the rest of the
 * armed lifetime the credentials live only in the daemon's RAM — nothing persists at rest to be
 * recovered by later root access, forensic imaging, or before-first-unlock device-encrypted storage.
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
        // Shred any credential files the daemon may not have wiped (e.g. it died before
        // its startup self-wipe ran) before unlinking the directory.
        executor.exec(
            "for f in $BASE_DIR/credential.old $BASE_DIR/credential.new; do " +
                "[ -f \"\$f\" ] && dd if=/dev/urandom of=\"\$f\" bs=512 count=1 2>/dev/null; done",
        )
        val result = executor.exec("rm -rf $BASE_DIR")
        return result.isSuccess
    }

    private fun buildWatcherScript(device: String, pressCount: Int, windowSeconds: Long): String = """
        #!/system/bin/sh
        DEV='$device'
        PRESS_COUNT=$pressCount
        WINDOW_S=$windowSeconds
        BASE_DIR='$BASE_DIR'
        # Load both secrets into this process's memory, then shred+remove them from disk.
        # For the rest of the armed lifetime the credentials live only in RAM, so no
        # plaintext lock-screen password persists at rest (readable by later root access,
        # forensic imaging, or before-first-unlock in device-encrypted storage).
        OLD=${'$'}(cat "${'$'}BASE_DIR/credential.old" 2>/dev/null)
        NEW=${'$'}(cat "${'$'}BASE_DIR/credential.new" 2>/dev/null)
        for f in "${'$'}BASE_DIR/credential.old" "${'$'}BASE_DIR/credential.new"; do
          [ -f "${'$'}f" ] && dd if=/dev/urandom of="${'$'}f" bs=512 count=1 2>/dev/null
          rm -f "${'$'}f"
        done
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
                if [ -n "${'$'}NEW" ]; then
                  locksettings set-password --old "${'$'}OLD" "${'$'}NEW"
                fi
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
