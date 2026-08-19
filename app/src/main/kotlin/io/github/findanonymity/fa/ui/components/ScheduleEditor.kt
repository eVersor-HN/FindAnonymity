package io.github.findanonymity.fa.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.data.model.ToggleRuleConfig
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoYellow

private val WEEKDAY_LABELS = intArrayOf(
    R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed, R.string.weekday_thu,
    R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun,
)

fun formatMinuteOfDay(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** Weekday chips + from/to time pickers + an "on during window" switch for [ToggleMode.SCHEDULED]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleFields(rule: ToggleRuleConfig, onChange: (ToggleRuleConfig) -> Unit) {
    val context = LocalContext.current

    Text(stringResource(R.string.schedule_days), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        for (bit in 0..6) {
            val selected = (rule.scheduleDaysMask shr bit) and 1 == 1
            FilterChip(
                selected = selected,
                onClick = { onChange(rule.copy(scheduleDaysMask = rule.scheduleDaysMask xor (1 shl bit))) },
                label = { Text(stringResource(WEEKDAY_LABELS[bit])) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CorpoCyan.copy(alpha = 0.20f),
                    selectedLabelColor = CorpoCyan,
                ),
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CorpoOutlinedButton(
            text = "${stringResource(R.string.schedule_from)} ${formatMinuteOfDay(rule.scheduleStartMinute)}",
            onClick = {
                pickTime(context, rule.scheduleStartMinute) { onChange(rule.copy(scheduleStartMinute = it)) }
            },
            accent = CorpoCyan,
            modifier = Modifier.weight(1f),
        )
        CorpoOutlinedButton(
            text = "${stringResource(R.string.schedule_to)} ${formatMinuteOfDay(rule.scheduleEndMinute)}",
            onClick = {
                pickTime(context, rule.scheduleEndMinute) { onChange(rule.copy(scheduleEndMinute = it)) }
            },
            accent = CorpoCyan,
            modifier = Modifier.weight(1f),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(
                if (rule.scheduleOnDuringWindow) R.string.schedule_on_during else R.string.schedule_off_during,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (rule.scheduleOnDuringWindow) CorpoYellow else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Switch(
            checked = rule.scheduleOnDuringWindow,
            onCheckedChange = { onChange(rule.copy(scheduleOnDuringWindow = it)) },
        )
    }
}

private fun pickTime(context: android.content.Context, currentMinute: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        currentMinute / 60,
        currentMinute % 60,
        true,
    ).show()
}
