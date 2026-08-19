package io.github.findanonymity.fa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.github.findanonymity.fa.ui.theme.CorpoVoid
import io.github.findanonymity.fa.ui.theme.CorpoYellow

/** Angular, chamfered corner to match the HUD panels (never the default rounded pill). */
private val CorpoButtonShape = CutCornerShape(topEnd = 10.dp)

/**
 * Primary call-to-action: solid accent fill, sharp corner, monospace label. Use ONE per screen —
 * everything else should be a [CorpoOutlinedButton] or text button so the accent keeps its weight.
 */
@Composable
fun CorpoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = CorpoYellow,
    onContainer: Color = CorpoVoid,
    icon: Painter? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CorpoButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = onContainer),
        modifier = modifier,
    ) {
        ButtonContent(text, icon)
    }
}

/** Secondary / ghost action: transparent fill, accent outline, accent label. */
@Composable
fun CorpoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = CorpoYellow,
    icon: Painter? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CorpoButtonShape,
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.8f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        modifier = modifier,
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun ButtonContent(text: String, icon: Painter?) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
