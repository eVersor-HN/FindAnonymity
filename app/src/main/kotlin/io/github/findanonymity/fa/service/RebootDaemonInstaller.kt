package io.github.findanonymity.fa.service

import io.github.findanonymity.fa.core.exec.ShellResult

/**
 * Installs/removes a detached privileged daemon that reboots the device after a fixed delay,
 * independent of FA's own process. This is the "forced reboot": even if FA is stopped (a thief
 * tapping "Stop" on the notification, the OS killing the app, battery-optimisation), the pending
 * reboot still fires — returning the device to the locked, encrypted-at-rest before-first-unlock
 * state. Root re-arms it on every boot; under Shizuku it fires once (Shizuku is not up that early).
 */
class RebootDaemonInstaller(private val exec: suspend (String) -> ShellResult) {

    suspend fun arm(secondsUntilReboot: Long): Boolean {
        val secs = secondsUntilReboot.coerceAtLeast(1)
        exec("mkdir -p $BASE_DIR && chmod 700 $BASE_DIR")
        val script = "#!/system/bin/sh\nsleep $secs\nreboot\n"
        val write = exec("cat > $BASE_DIR/r.sh << 'FA_REBOOT_EOF'\n$script\nFA_REBOOT_EOF\nchmod 700 $BASE_DIR/r.sh")
        if (!write.isSuccess) return false
        // Replace any previous pending reboot, then launch fully detached.
        exec("pkill -f $BASE_DIR/r.sh")
        val start = exec("nohup sh $BASE_DIR/r.sh > /dev/null 2>&1 &")
        return start.isSuccess
    }

    suspend fun disarm(): Boolean {
        exec("pkill -f $BASE_DIR/r.sh")
        return exec("rm -rf $BASE_DIR").isSuccess
    }

    companion object {
        private const val BASE_DIR = "/data/local/fa_reboot"
    }
}
