package io.github.findanonymity.fa.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.core.exec.BackendState
import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.data.model.ToggleTarget
import io.github.findanonymity.fa.service.RuleScheduler
import io.github.findanonymity.fa.ui.components.StatusRow
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.PhosphorGreen
import io.github.findanonymity.fa.ui.theme.TerminalAmber
import io.github.findanonymity.fa.ui.theme.TerminalRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEditToggleRule: (ToggleTarget) -> Unit,
    onEditRebootRule: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPanic: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val config by viewModel.configFlow.collectAsStateWithLifecycle()
    val backendState by viewModel.backendState.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffectTicker { now = System.currentTimeMillis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                MasterSwitchCard(
                    enabled = config.masterAutomationEnabled,
                    onToggle = viewModel::setMasterEnabled,
                )
            }

            if (backendState is BackendState.NoneAvailable) {
                item { NoBackendBanner(onClick = onOpenPermissions) }
            }

            item {
                TerminalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.home_rules_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = PhosphorGreen,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            ToggleTarget.WIFI to config.wifiRule,
                            ToggleTarget.MOBILE_DATA to config.dataRule,
                            ToggleTarget.AIRPLANE_MODE to config.airplaneModeRule,
                        ).forEach { (target, rule) ->
                            ToggleRuleRow(
                                target = target,
                                rule = rule,
                                now = now,
                                onClick = { onEditToggleRule(target) },
                            )
                        }
                        RebootRuleRow(
                            rule = config.rebootRule,
                            now = now,
                            onClick = onEditRebootRule,
                        )
                    }
                }
            }

            item {
                TerminalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.home_panic_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalRed,
                    )
                    StatusRow(
                        label = stringResource(R.string.settings_title),
                        statusText = stringResource(
                            if (config.panicLock.armed) R.string.home_panic_armed else R.string.home_panic_disarmed,
                        ),
                        accentColor = if (config.panicLock.armed) TerminalRed else TerminalAmber,
                    )
                    Button(onClick = onOpenPanic, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.home_panic_configure))
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchedEffectTicker(onTick: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            onTick()
        }
    }
}

@Composable
private fun MasterSwitchCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.home_automation_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        if (enabled) R.string.home_automation_running else R.string.home_automation_stopped,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) PhosphorGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun NoBackendBanner(onClick: () -> Unit) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = TerminalAmber)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    stringResource(R.string.home_no_backend_title),
                    color = TerminalAmber,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.home_no_backend_body),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.home_no_backend_button))
                }
            }
        }
    }
}

@Composable
private fun ToggleRuleRow(target: ToggleTarget, rule: ToggleRuleConfig, now: Long, onClick: () -> Unit) {
    val phase = RuleScheduler.computeTogglePhase(rule, now)
    val statusText = when {
        !rule.enabled -> stringResource(R.string.home_status_disabled)
        phase == null -> stringResource(rule.mode.labelRes)
        phase.millisUntilNextTransition == Long.MAX_VALUE ->
            stringResource(if (phase.shouldBeOn) R.string.home_status_always_on else R.string.home_status_always_off)
        else -> stringResource(
            R.string.home_status_next_change,
            stringResource(if (phase.shouldBeOn) R.string.home_state_on else R.string.home_state_off),
            formatCountdown(phase.millisUntilNextTransition),
        )
    }
    val color = when {
        !rule.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        phase?.shouldBeOn == true -> PhosphorGreen
        else -> TerminalAmber
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatusRow(label = stringResource(target.labelRes), statusText = statusText, accentColor = color)
    }
}

@Composable
private fun RebootRuleRow(rule: RebootRuleConfig, now: Long, onClick: () -> Unit) {
    val statusText = if (!rule.enabled) {
        stringResource(R.string.home_reboot_disabled)
    } else {
        stringResource(R.string.home_reboot_next, formatCountdown(RuleScheduler.millisUntilReboot(rule, now)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        StatusRow(
            label = stringResource(R.string.home_reboot_label),
            statusText = statusText,
            accentColor = if (rule.enabled) PhosphorGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatCountdown(millis: Long): String {
    if (millis == Long.MAX_VALUE) return "-"
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
