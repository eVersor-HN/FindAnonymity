package io.github.findanonymity.fa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.data.model.CycleTimeUnit
import io.github.findanonymity.fa.data.model.Duration2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPicker(
    label: String,
    value: Duration2,
    onValueChange: (Duration2) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(value.value) { mutableStateOf(value.value.toString()) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input.filter { it.isDigit() }
                    val parsed = textValue.toLongOrNull()
                    if (parsed != null && parsed > 0) {
                        onValueChange(value.copy(value = parsed))
                    }
                },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1.25f),
            ) {
                OutlinedTextField(
                    value = stringResource(value.unit.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.duration_picker_unit_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    CycleTimeUnit.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(stringResource(unit.labelRes)) },
                            onClick = {
                                onValueChange(value.copy(unit = unit))
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        if (value.unit == CycleTimeUnit.MONTHS || value.unit == CycleTimeUnit.YEARS) {
            Text(
                text = stringResource(R.string.duration_picker_approx_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
