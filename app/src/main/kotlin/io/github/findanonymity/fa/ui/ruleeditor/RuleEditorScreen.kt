package io.github.findanonymity.fa.ui.ruleeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.github.findanonymity.fa.ui.components.CorpoButton
import io.github.findanonymity.fa.ui.components.DurationPicker
import io.github.findanonymity.fa.ui.components.FormContainer
import io.github.findanonymity.fa.ui.theme.CorpoRed

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
    var rule by remember(target) { mutableStateOf<ToggleRuleConfig?>(null) }
    LaunchedEffect(config, target) {
        val loaded = config ?: return@LaunchedEffect
        if (rule == null) rule = ruleFor(loaded, target)
    }
    val currentRule = rule ?: return

    val isValid = currentRule.mode != ToggleMode.CYCLICAL ||
        (currentRule.activeDuration.toMillis() in 1 until currentRule.cycleInterval.toMillis().coerceAtLeast(1))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(target.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        FormContainer(scaffoldPadding = padding) {
            Text(stringResource(R.string.rule_editor_mode), style = MaterialTheme.typography.titleSmall)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ToggleMode.entries.forEach { mode ->
                    ModeOptionRow(
                        label = stringResource(mode.labelRes),
                        selected = currentRule.mode == mode,
                        // Choosing a managing mode arms the rule in one tap; "unmanaged" turns it off.
                        onClick = { rule = currentRule.copy(mode = mode, enabled = mode != ToggleMode.UNMANAGED) },
                    )
                }
            }

            if (currentRule.mode == ToggleMode.CYCLICAL) {
                DurationPicker(
                    label = stringResource(R.string.rule_editor_cycle),
                    value = currentRule.cycleInterval,
                    onValueChange = { rule = currentRule.copy(cycleInterval = it) },
                )
                DurationPicker(
                    label = stringResource(R.string.rule_editor_active_duration),
                    value = currentRule.activeDuration,
                    onValueChange = { rule = currentRule.copy(activeDuration = it) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.rule_editor_start_on), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = currentRule.startOn, onCheckedChange = { rule = currentRule.copy(startOn = it) })
                }
                if (!isValid) {
                    Text(
                        stringResource(R.string.rule_editor_validation_error),
                        color = CorpoRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            CorpoButton(
                text = stringResource(R.string.common_save),
                onClick = { viewModel.saveToggleRule(target, currentRule); onBack() },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
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
    var rule by remember { mutableStateOf<RebootRuleConfig?>(null) }
    LaunchedEffect(config) {
        val loaded = config ?: return@LaunchedEffect
        if (rule == null) rule = loaded.rebootRule
    }
    val currentRule = rule ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_editor_reboot_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        FormContainer(scaffoldPadding = padding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.rule_editor_enabled), style = MaterialTheme.typography.titleSmall)
                Switch(checked = currentRule.enabled, onCheckedChange = { rule = currentRule.copy(enabled = it) })
            }
            DurationPicker(
                label = stringResource(R.string.rule_editor_reboot_interval),
                value = currentRule.interval,
                onValueChange = { rule = currentRule.copy(interval = it) },
            )
            if (currentRule.enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(stringResource(R.string.rule_editor_force_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.rule_editor_force_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = currentRule.forced, onCheckedChange = { rule = currentRule.copy(forced = it) })
                }
            }
            Text(
                stringResource(R.string.rule_editor_reboot_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CorpoButton(
                text = stringResource(R.string.common_save),
                onClick = { viewModel.saveRebootRule(currentRule); onBack() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRuleEditorScreen(
    onBack: () -> Unit,
    viewModel: RuleEditorViewModel = viewModel(),
) {
    var rule by remember {
        mutableStateOf(
            ToggleRuleConfig(
                enabled = true,
                mode = ToggleMode.CYCLICAL,
                cycleInterval = io.github.findanonymity.fa.data.model.Duration2(60, io.github.findanonymity.fa.data.model.CycleTimeUnit.MINUTES),
                activeDuration = io.github.findanonymity.fa.data.model.Duration2(5, io.github.findanonymity.fa.data.model.CycleTimeUnit.MINUTES),
            ),
        )
    }
    val isValid = rule.mode != ToggleMode.CYCLICAL ||
        (rule.activeDuration.toMillis() in 1 until rule.cycleInterval.toMillis().coerceAtLeast(1))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_editor_bulk_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        FormContainer(scaffoldPadding = padding) {
            Text(
                stringResource(R.string.rule_editor_bulk_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.rule_editor_mode), style = MaterialTheme.typography.titleSmall)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ToggleMode.entries.forEach { mode ->
                    ModeOptionRow(
                        label = stringResource(mode.labelRes),
                        selected = rule.mode == mode,
                        onClick = { rule = rule.copy(mode = mode, enabled = mode != ToggleMode.UNMANAGED) },
                    )
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.rule_editor_start_on), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = rule.startOn, onCheckedChange = { rule = rule.copy(startOn = it) })
                }
                if (!isValid) {
                    Text(stringResource(R.string.rule_editor_validation_error), color = CorpoRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            CorpoButton(
                text = stringResource(R.string.rule_editor_bulk_apply),
                onClick = { viewModel.saveAllToggleRules(rule); onBack() },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
