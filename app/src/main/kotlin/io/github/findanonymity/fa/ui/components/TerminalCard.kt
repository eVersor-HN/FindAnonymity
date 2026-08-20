package io.github.findanonymity.fa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Corpo HUD panel: cold raised surface, hard steel outline, a chamfered top-end corner (theme's
 * medium shape). The header accent [strip] is state-coloured so it carries meaning (cyan = ok,
 * amber = attention, red = armed) instead of every card shouting the same; pass strip = false for
 * quiet, secondary panels so the emphasised ones stand out.
 */
@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
    strip: Boolean = true,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            if (strip) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(accent),
                ) {}
            }
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Convenience: a strip colour that is dimmed for a "quiet" secondary panel. */
@Composable
fun quietAccent(): Color = MaterialTheme.colorScheme.outline
