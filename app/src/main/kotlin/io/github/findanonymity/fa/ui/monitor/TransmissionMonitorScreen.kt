package io.github.findanonymity.fa.ui.monitor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.ui.components.FormContainer
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoAmber
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoRed
import io.github.findanonymity.fa.ui.theme.CorpoYellow

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f kB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun formatRate(bytesPerSecond: Long): String = "${formatBytes(bytesPerSecond)}/s"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmissionMonitorScreen(
    onBack: () -> Unit,
    viewModel: TransmissionMonitorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.monitor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        FormContainer(scaffoldPadding = padding) {
            if (state.leak) {
                TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoRed) {
                    Text(
                        stringResource(R.string.monitor_leak_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = CorpoRed,
                    )
                    Text(
                        stringResource(R.string.monitor_leak_body),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (state.airplaneBypassed) {
                TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoAmber) {
                    Text(
                        stringResource(R.string.monitor_bypass_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = CorpoAmber,
                    )
                    Text(
                        stringResource(R.string.monitor_bypass_body),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // ── Live traffic ────────────────────────────────────────────────────────────────
            val active = state.txRate > 0 || state.rxRate > 0
            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (active) CorpoYellow else CorpoCyan) {
                Text(stringResource(R.string.monitor_traffic), style = MaterialTheme.typography.titleSmall, color = CorpoYellow)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    RateColumn(
                        iconRes = R.drawable.ic_upload,
                        label = stringResource(R.string.monitor_sent),
                        rate = state.txRate,
                        session = state.txSession,
                        accent = if (state.txRate > 0) CorpoYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    RateColumn(
                        iconRes = R.drawable.ic_download,
                        label = stringResource(R.string.monitor_received),
                        rate = state.rxRate,
                        session = state.rxSession,
                        accent = if (state.rxRate > 0) CorpoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                Sparkline(
                    values = state.history,
                    color = if (state.leak) CorpoRed else CorpoYellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 10.dp),
                )
                if (!active) {
                    Text(
                        stringResource(R.string.monitor_quiet),
                        style = MaterialTheme.typography.bodySmall,
                        color = CorpoCyan,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // ── Radio states ────────────────────────────────────────────────────────────────
            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (state.radiosSilent) CorpoCyan else CorpoAmber) {
                Text(stringResource(R.string.monitor_radios), style = MaterialTheme.typography.titleSmall, color = CorpoYellow)
                Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RadioRow(R.drawable.ic_airplane, stringResource(R.string.target_airplane_mode), state.airplaneOn, onIsSilent = true)
                    RadioRow(R.drawable.ic_wifi, stringResource(R.string.target_wifi), state.wifiOn)
                    RadioRow(R.drawable.ic_data, stringResource(R.string.target_mobile_data), state.dataOn)
                    RadioRow(R.drawable.ic_bluetooth, stringResource(R.string.target_bluetooth), state.bluetoothOn)
                    RadioRow(R.drawable.ic_location, stringResource(R.string.target_location), state.locationOn)
                }
            }

            // ── Outbound connections ────────────────────────────────────────────────────────
            val connections = state.connections
            TerminalCard(
                modifier = Modifier.fillMaxWidth(),
                accent = if (!connections.isNullOrEmpty()) CorpoAmber else CorpoCyan,
            ) {
                Text(stringResource(R.string.monitor_connections), style = MaterialTheme.typography.titleSmall, color = CorpoYellow)
                when {
                    !state.hasBackend -> Text(
                        stringResource(R.string.monitor_connections_need_backend),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    state.connectionsError != null -> Text(
                        stringResource(R.string.monitor_connections_failed, state.connectionsError!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = CorpoAmber,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    connections.isNullOrEmpty() -> Text(
                        stringResource(R.string.monitor_connections_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = CorpoCyan,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    else -> {
                        Text(
                            stringResource(R.string.monitor_connections_count, connections.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CorpoAmber,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            connections.take(MAX_LISTED_CONNECTIONS).forEach { c ->
                                Text(
                                    "${c.proto}  ${c.peer}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (connections.size > MAX_LISTED_CONNECTIONS) {
                                Text(
                                    "+${connections.size - MAX_LISTED_CONNECTIONS}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── The "mobile data off is not radio off" explainer ─────────────────────────────
            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoAmber) {
                Text(
                    stringResource(R.string.monitor_hint_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CorpoAmber,
                )
                Text(
                    stringResource(R.string.monitor_hint_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private const val MAX_LISTED_CONNECTIONS = 12

@Composable
private fun RateColumn(
    iconRes: Int,
    label: String,
    rate: Long,
    session: Long,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                "  $label",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatRate(rate), style = MaterialTheme.typography.titleMedium, color = accent)
        Text(
            "${formatBytes(session)} ${stringResource(R.string.monitor_total_session)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RadioRow(iconRes: Int, label: String, on: Boolean, onIsSilent: Boolean = false) {
    // Airplane mode is the one radio where "ON" means silence, so it reads cyan when enabled.
    val good = if (onIsSilent) on else !on
    val accent = if (good) CorpoCyan else CorpoAmber
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Text(
            "  $label",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(if (on) R.string.monitor_state_on else R.string.monitor_state_off),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

/** Sixty-second history of bytes sent per second — makes the "on" bursts visible at a glance. */
@Composable
private fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val baseline = size.height
        drawLine(outline, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1f)
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val step = size.width / HISTORY_SLOTS
        values.takeLast(HISTORY_SLOTS.toInt()).forEachIndexed { index, value ->
            val h = (value / max) * size.height
            if (h > 0f) {
                val x = index * step
                drawLine(
                    color = color,
                    start = Offset(x, baseline),
                    end = Offset(x, baseline - h),
                    strokeWidth = (step * 0.6f).coerceAtLeast(1.5f),
                )
            }
        }
    }
}

private const val HISTORY_SLOTS = 60f
