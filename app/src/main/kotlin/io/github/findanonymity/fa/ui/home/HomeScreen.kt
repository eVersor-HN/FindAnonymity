package io.github.findanonymity.fa.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.BuildConfig
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.core.exec.BackendState
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.data.model.ToggleTarget
import io.github.findanonymity.fa.service.RuleScheduler
import io.github.findanonymity.fa.ui.components.CorpoButton
import io.github.findanonymity.fa.ui.components.CorpoOutlinedButton
import io.github.findanonymity.fa.ui.components.StatusRow
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoAmber
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoRed
import io.github.findanonymity.fa.ui.theme.CorpoYellow
import kotlinx.coroutines.delay

private fun iconRes(target: ToggleTarget): Int = when (target) {
    ToggleTarget.WIFI -> R.drawable.ic_wifi
    ToggleTarget.MOBILE_DATA -> R.drawable.ic_data
    ToggleTarget.AIRPLANE_MODE -> R.drawable.ic_airplane
    ToggleTarget.BLUETOOTH -> R.drawable.ic_bluetooth
    ToggleTarget.LOCATION -> R.drawable.ic_location
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEditToggleRule: (ToggleTarget) -> Unit,
    onEditRebootRule: () -> Unit,
    onOpenBulk: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPanic: () -> Unit,
    onOpenMonitor: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val config by viewModel.configFlow.collectAsStateWithLifecycle()
    val backendState by viewModel.backendState.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffectTicker { now = System.currentTimeMillis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (config.masterAutomationEnabled) {
                            PulsingDot(CorpoCyan)
                            Text(" ", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(stringResource(R.string.home_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMonitor) {
                        Icon(
                            painterResource(R.drawable.ic_pulse),
                            contentDescription = stringResource(R.string.monitor_open),
                            tint = CorpoCyan,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tighten spacing on shorter screens so the dashboard still fits without scrolling.
            val compact = maxHeight < 760.dp
            val gap = if (compact) 6.dp else 10.dp
            val cardPad = if (compact) 10.dp else 14.dp
            val rowPad = if (compact) 6.dp else 10.dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MasterSwitchCard(config.masterAutomationEnabled, viewModel::setMasterEnabled, cardPad)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                BackendStatusCard(backendState, viewModel::connectShizuku, onOpenPermissions, cardPad)
            }

            item {
                TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoYellow, contentPadding = cardPad) {
                    // On short screens the section title is dropped — the rows are self-explanatory
                    // and the space is what keeps the dashboard scroll-free.
                    if (!compact) {
                        Text(stringResource(R.string.home_rules_title), style = MaterialTheme.typography.titleSmall, color = CorpoYellow)
                    }
                    Column(
                        modifier = Modifier.padding(top = if (compact) 0.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp),
                    ) {
                        listOf(
                            ToggleTarget.WIFI to config.wifiRule,
                            ToggleTarget.MOBILE_DATA to config.dataRule,
                            ToggleTarget.AIRPLANE_MODE to config.airplaneModeRule,
                            ToggleTarget.BLUETOOTH to config.bluetoothRule,
                            ToggleTarget.LOCATION to config.locationRule,
                        ).forEach { (target, rule) ->
                            ToggleRuleRow(target, rule, now, rowPad) { onEditToggleRule(target) }
                        }
                        RebootRuleRow(config.rebootRule, now, rowPad, onEditRebootRule)
                    }
                    CorpoOutlinedButton(
                        text = stringResource(R.string.home_bulk),
                        onClick = onOpenBulk,
                        icon = painterResource(R.drawable.ic_tune),
                        accent = CorpoCyan,
                        modifier = Modifier.fillMaxWidth().padding(top = if (compact) 6.dp else 8.dp),
                    )
                }
            }

            item {
                val panicAccent = if (config.panicLock.armed) CorpoRed else CorpoAmber
                // Title lives in the row label itself — one line keeps the dashboard scroll-free.
                TerminalCard(modifier = Modifier.fillMaxWidth(), accent = panicAccent, contentPadding = cardPad) {
                    RuleRowSurface(
                        icon = painterResource(R.drawable.ic_shield),
                        label = stringResource(R.string.home_panic_title),
                        statusText = stringResource(
                            if (config.panicLock.armed) R.string.home_panic_armed else R.string.home_panic_disarmed,
                        ),
                        accent = panicAccent,
                        verticalPadding = rowPad,
                        onClick = onOpenPanic,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .alpha(alpha)
            .background(color, CircleShape),
    )
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
private fun MasterSwitchCard(enabled: Boolean, onToggle: (Boolean) -> Unit, contentPadding: androidx.compose.ui.unit.Dp) {
    TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (enabled) CorpoYellow else MaterialTheme.colorScheme.outline, contentPadding = contentPadding) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_automation_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(if (enabled) R.string.home_automation_running else R.string.home_automation_stopped),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) CorpoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun BackendStatusCard(state: BackendState, onConnectShizuku: () -> Unit, onOpenSetup: () -> Unit, contentPadding: androidx.compose.ui.unit.Dp) {
    val connected = state is BackendState.RootAvailable || state is BackendState.ShizukuAvailable
    val statusText: String
    val accent: Color
    val showConnect: Boolean
    when (state) {
        is BackendState.RootAvailable -> { statusText = stringResource(R.string.home_backend_root); accent = CorpoCyan; showConnect = false }
        is BackendState.ShizukuAvailable -> { statusText = stringResource(R.string.home_backend_shizuku_ok); accent = CorpoCyan; showConnect = false }
        is BackendState.ShizukuNeedsPermission -> { statusText = stringResource(R.string.home_backend_shizuku_perm); accent = CorpoAmber; showConnect = true }
        is BackendState.NoneAvailable -> { statusText = stringResource(R.string.home_backend_none); accent = CorpoAmber; showConnect = true }
    }
    // Connected → a quiet, single-line status. Needs action → an emphasised card with buttons.
    TerminalCard(modifier = Modifier.fillMaxWidth(), accent = accent, strip = !connected, contentPadding = contentPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_link), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                "  ${stringResource(R.string.home_backend_title)}: $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) MaterialTheme.colorScheme.onSurface else accent,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (connected) {
                Text(
                    stringResource(R.string.home_backend_setup),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(onClick = onOpenSetup),
                )
            }
        }
        if (showConnect) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CorpoButton(
                    text = stringResource(R.string.home_backend_connect),
                    onClick = onConnectShizuku,
                    icon = painterResource(R.drawable.ic_link),
                    modifier = Modifier.weight(1f),
                )
                CorpoOutlinedButton(
                    text = stringResource(R.string.home_backend_setup),
                    onClick = onOpenSetup,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FooterLine(state: BackendState) {
    val backend = when (state) {
        is BackendState.RootAvailable -> "root"
        is BackendState.ShizukuAvailable -> "shizuku"
        is BackendState.ShizukuNeedsPermission -> "shizuku?"
        is BackendState.NoneAvailable -> "none"
    }
    Text(
        "backend: $backend · v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun ToggleRuleRow(target: ToggleTarget, rule: ToggleRuleConfig, now: Long, verticalPadding: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
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
    RuleRowSurface(painterResource(iconRes(target)), stringResource(target.labelRes), statusText, color, verticalPadding, onClick)
}

@Composable
private fun RebootRuleRow(rule: RebootRuleConfig, now: Long, verticalPadding: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val statusText = if (!rule.enabled) {
        stringResource(R.string.home_reboot_disabled)
    } else {
        stringResource(R.string.home_reboot_next, formatCountdown(RuleScheduler.millisUntilReboot(rule, now)))
    }
    RuleRowSurface(
        painterResource(R.drawable.ic_reboot),
        stringResource(R.string.home_reboot_label),
        statusText,
        if (rule.enabled) CorpoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
        verticalPadding,
        onClick,
    )
}

@Composable
private fun RuleRowSurface(
    icon: Painter,
    label: String,
    statusText: String,
    accent: Color,
    verticalPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    // Single line on purpose: six of these plus the other cards have to fit one portrait screen
    // without scrolling. The outline + chevron keep it obviously tappable.
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = verticalPadding, bottom = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp),
            )
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
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
