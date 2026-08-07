package io.github.findanonymity.fa.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.data.AppConfigRepository
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.data.model.ToggleTarget
import io.github.findanonymity.fa.core.exec.PrivilegedExecutorManager
import io.github.findanonymity.fa.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AutomationService : LifecycleService() {

    private lateinit var configRepository: AppConfigRepository
    private lateinit var executorManager: PrivilegedExecutorManager
    private var wifiManager: WifiManager? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as FaApp
        configRepository = app.configRepository
        executorManager = app.executorManager
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager

        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildStatusNotification(this, getString(R.string.notification_initializing)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        lifecycleScope.launch {
            configRepository.configFlow.collectLatest { config ->
                if (!config.masterAutomationEnabled) {
                    stopSelf()
                    return@collectLatest
                }
                runAllRules(config)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private suspend fun runAllRules(config: AppConfig) = coroutineScope {
        launch { runToggleLoop(ToggleTarget.WIFI, config.wifiRule) }
        launch { runToggleLoop(ToggleTarget.MOBILE_DATA, config.dataRule) }
        launch { runToggleLoop(ToggleTarget.AIRPLANE_MODE, config.airplaneModeRule) }
        launch { runRebootLoop(config.rebootRule) }
        launch { runStatusNotificationLoop(config) }
    }

    private suspend fun runToggleLoop(target: ToggleTarget, ruleConfig: ToggleRuleConfig) {
        if (!ruleConfig.enabled) return
        val effectiveAnchor = ruleConfig.cycleAnchorEpochMillis ?: System.currentTimeMillis()
        val stableConfig = ruleConfig.copy(cycleAnchorEpochMillis = effectiveAnchor)
        while (true) {
            val now = System.currentTimeMillis()
            val phase = RuleScheduler.computeTogglePhase(stableConfig, now) ?: return
            ensureToggleState(target, phase.shouldBeOn)
            delay(phase.millisUntilNextTransition.coerceAtMost(WATCHDOG_INTERVAL_MS))
        }
    }

    private suspend fun runRebootLoop(rebootConfig: RebootRuleConfig) {
        if (!rebootConfig.enabled) return
        val effectiveLast = rebootConfig.lastRebootEpochMillis ?: System.currentTimeMillis()
        val stableConfig = rebootConfig.copy(lastRebootEpochMillis = effectiveLast)
        while (true) {
            val now = System.currentTimeMillis()
            if (RuleScheduler.isRebootDue(stableConfig, now)) {
                executorManager.exec("reboot")
                return // device is rebooting; BootReceiver will restart everything fresh
            }
            val sleep = RuleScheduler.millisUntilReboot(stableConfig, now).coerceAtMost(WATCHDOG_INTERVAL_MS)
            delay(sleep.coerceAtLeast(1_000L))
        }
    }

    private suspend fun runStatusNotificationLoop(config: AppConfig) {
        while (true) {
            updateNotification(config, buildStatusText(config))
            delay(1_000L)
        }
    }

    private fun buildStatusText(config: AppConfig): String {
        val now = System.currentTimeMillis()
        val parts = buildList {
            listOf(
                ToggleTarget.WIFI to config.wifiRule,
                ToggleTarget.MOBILE_DATA to config.dataRule,
                ToggleTarget.AIRPLANE_MODE to config.airplaneModeRule,
            ).forEach { (target, rule) ->
                val phase = RuleScheduler.computeTogglePhase(rule, now)
                val targetName = getString(target.labelRes)
                add(
                    when {
                        !rule.enabled -> getString(R.string.notification_disabled, targetName)
                        phase == null -> getString(R.string.notification_unmanaged, targetName)
                        else -> {
                            val state = getString(if (phase.shouldBeOn) R.string.home_state_on else R.string.home_state_off)
                            val eta = formatDuration(phase.millisUntilNextTransition)
                            getString(R.string.notification_toggle_line, targetName, state, eta)
                        }
                    }
                )
            }
            if (config.rebootRule.enabled) {
                add(
                    getString(
                        R.string.notification_reboot_line,
                        formatDuration(RuleScheduler.millisUntilReboot(config.rebootRule, now)),
                    ),
                )
            }
        }
        return parts.joinToString("\n")
    }

    private fun formatDuration(millis: Long): String {
        if (millis == Long.MAX_VALUE) return "-"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private suspend fun ensureToggleState(target: ToggleTarget, desiredOn: Boolean) {
        val actualOn = when (target) {
            ToggleTarget.WIFI -> wifiManager?.isWifiEnabled
            else -> null
        }
        if (actualOn == desiredOn) return
        val command = if (desiredOn) target.command.on else target.command.off
        executorManager.exec(command)
    }

    private fun updateNotification(config: AppConfig, fallbackText: String) {
        val text = if (config.masterAutomationEnabled) buildStatusText(config) else fallbackText
        NotificationHelper.ensureChannel(this)
        val notification = NotificationHelper.buildStatusNotification(this, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    companion object {
        private val WATCHDOG_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60)

        fun start(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
