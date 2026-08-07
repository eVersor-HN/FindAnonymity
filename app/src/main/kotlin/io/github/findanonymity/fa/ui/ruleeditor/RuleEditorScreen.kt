package io.github.findanonymity.fa.ui.ruleeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.data.model.AppConfig
import io.github.findanonymity.fa.data.model.RebootRuleConfig
import io.github.findanonymity.fa.data.model.ToggleMode
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.data.model.ToggleTarget
import io.github.findanonymity.fa.ui.components.DurationPicker
import io.github.findanonymity.fa.ui.theme.TerminalRed

private fun ruleFor(config: AppConfig, target: ToggleTarget): ToggleRuleConfig = when (target) {
    ToggleTarget.WIFI -> config.wifiRule
    ToggleTarget.MOBILE_DATA -> config.dataRule
    ToggleTarget.AIRPLANE_MODE -> config.airplaneModeRule
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToggleRuleEditorScreen(
    target: ToggleTarget,
    onBack: () -> Unit,
    viewModel: RuleEditorViewModel = viewModel(),
) {
    val config by viewModel.configFlow.collectAsStateWithLifecycle()
    var rule by remember { mutableStateOf(ToggleRuleConfig()) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(config, target) {
        if (!initialized) {
            rule = ruleFor(config, target)
            initialized = true
        }
    }

    val isValid = rule.mode != ToggleMode.CYCLICAL ||
        (rule.activeDuration.toMillis() in 1 until rule.cycleInterval.toMillis().coerceAtLeast(1))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(target.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.rule_editor_enabled), style = MaterialTheme.typography.titleSmall)
                Switch(checked = rule.enabled, onCheckedChange = { rule = rule.copy(enabled = it) })
            }

            Text(stringResource(R.string.rule_editor_mode), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ToggleMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = rule.mode == mode,
                        onClick = { rule = rule.copy(mode = mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ToggleMode.entries.size,
                        ),
                    ) {
                        Text(stringResource(mode.labelRes), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (rule.mode == ToggleMode.CYCLICAL) {
                DurationPicker(
                    label = stringResource(R.string.rule_editor_cycle),
                    value = rule.cycleInterval,
                    onValueChange = { rule = rule.copy(cycleInterval = it) },
                )
                DurationPicker(
                    label = stringResource(R.string.rule_editor_active_duration),
                    value = rule.activeDuration,
                    onValueChange = { rule = rule.copy(activeDuration = it) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.rule_editor_start_on), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = rule.startOn, onCheckedChange = { rule = rule.copy(startOn = it) })
                }
                if (!isValid) {
                    Text(
                        stringResource(R.string.rule_editor_validation_error),
                        color = TerminalRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveToggleRule(target, rule)
                    onBack()
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebootRuleEditorScreen(
    onBack: () -> Unit,
    viewModel: RuleEditorViewModel = viewModel(),
) {
    val config by viewModel.configFlow.collectAsStateWithLifecycle()
    var rule by remember { mutableStateOf(RebootRuleConfig()) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(config) {
        if (!initialized) {
            rule = config.rebootRule
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_editor_reboot_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.rule_editor_enabled), style = MaterialTheme.typography.titleSmall)
                Switch(checked = rule.enabled, onCheckedChange = { rule = rule.copy(enabled = it) })
            }
            DurationPicker(
                label = stringResource(R.string.rule_editor_reboot_interval),
                value = rule.interval,
                onValueChange = { rule = rule.copy(interval = it) },
            )
            Text(
                stringResource(R.string.rule_editor_reboot_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    viewModel.saveRebootRule(rule)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}
