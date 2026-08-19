package io.github.findanonymity.fa.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoYellow
import io.github.findanonymity.fa.ui.theme.CorpoAmber
import io.github.findanonymity.fa.ui.theme.CorpoRed
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MasterSwitchCard(
                    enabled = config.masterAutomationEnabled,
                    onToggle = viewModel::setMasterEnabled,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                BackendStatusCard(
                    state = backendState,
                    onConnectShizuku = viewModel::connectShizuku,
                    onOpenSetup = onOpenPermissions,
                )
            }

            item {
                TerminalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.home_rules_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = CorpoYellow,
                    )
                    Text(
                        stringResource(R.string.home_rules_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        color = CorpoRed,
                    )
                    StatusRow(
                        label = stringResource(R.string.settings_title),
                        statusText = stringResource(
                            if (config.panicLock.armed) R.string.home_panic_armed else R.string.home_panic_disarmed,
                        ),
                        accentColor = if (config.panicLock.armed) CorpoRed else CorpoAmber,
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
                    color = if (enabled) CorpoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun BackendStatusCard(
    state: BackendState,
    onConnectShizuku: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val statusText: String
    val accent: androidx.compose.ui.graphics.Color
    val showConnect: Boolean
    when (state) {
        is BackendState.RootAvailable -> {
            statusText = stringResource(R.string.home_backend_root); accent = CorpoCyan; showConnect = false
        }
        is BackendState.ShizukuAvailable -> {
            statusText = stringResource(R.string.home_backend_shizuku_ok); accent = CorpoCyan; showConnect = false
        }
        is BackendState.ShizukuNeedsPermission -> {
            statusText = stringResource(R.string.home_backend_shizuku_perm); accent = CorpoAmber; showConnect = true
        }
        is BackendState.NoneAvailable -> {
            statusText = stringResource(R.string.home_backend_none); accent = CorpoAmber; showConnect = true
        }
    }
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_backend_title), style = MaterialTheme.typography.titleSmall, color = CorpoYellow)
        StatusRow(
            label = stringResource(R.string.home_backend_label),
            statusText = statusText,
            accentColor = accent,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showConnect) {
                Button(onClick = onConnectShizuku) {
                    Text(stringResource(R.string.home_backend_connect))
                }
            }
            TextButton(onClick = onOpenSetup) {
                Text(stringResource(R.string.home_backend_setup))
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
        phase?.shouldBeOn == true -> CorpoYellow
        else -> CorpoAmber
    }
    RuleRowSurface(
        label = stringResource(target.labelRes),
        statusText = statusText,
        accent = color,
        onClick = onClick,
    )
}

@Composable
private fun RebootRuleRow(rule: RebootRuleConfig, now: Long, onClick: () -> Unit) {
    val statusText = if (!rule.enabled) {
        stringResource(R.string.home_reboot_disabled)
    } else {
        stringResource(R.string.home_reboot_next, formatCountdown(RuleScheduler.millisUntilReboot(rule, now)))
    }
    RuleRowSurface(
        label = stringResource(R.string.home_reboot_label),
        statusText = statusText,
        accent = if (rule.enabled) CorpoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick,
    )
}

/**
 * A clearly-tappable rule row: its own outlined surface with a ripple and a trailing chevron, so
 * it reads as an interactive list item rather than plain status text.
 */
@Composable
private fun RuleRowSurface(label: String, statusText: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = accent)
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
            )
        }
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
