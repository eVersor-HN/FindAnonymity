package io.github.findanonymity.fa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TerminalColorScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = TerminalBackground,
    secondary = TerminalCyan,
    onSecondary = TerminalBackground,
    tertiary = TerminalAmber,
    error = TerminalRed,
    onError = TerminalBackground,
    background = TerminalBackground,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = TerminalBorder,
)

// Sharp, near-rectangular corners — a restrained terminal look, not a rounded consumer-app one.
private val TerminalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

@Composable
fun FaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        shapes = TerminalShapes,
        content = content,
    )
}
