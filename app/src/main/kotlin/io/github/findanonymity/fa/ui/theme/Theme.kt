package io.github.findanonymity.fa.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CorpoColorScheme = darkColorScheme(
    primary = CorpoYellow,
    onPrimary = CorpoVoid,
    secondary = CorpoCyan,
    onSecondary = CorpoVoid,
    tertiary = CorpoAmber,
    onTertiary = CorpoVoid,
    error = CorpoRed,
    onError = CorpoVoid,
    background = CorpoVoid,
    onBackground = CorpoTextPrimary,
    surface = CorpoSurface,
    onSurface = CorpoTextPrimary,
    surfaceVariant = CorpoSurfaceRaised,
    onSurfaceVariant = CorpoTextSecondary,
    outline = CorpoSteel,
    outlineVariant = CorpoSteel,
)

// Corpo-cyberpunk geometry: hard rectangular edges, with a single chamfered corner on
// larger surfaces (the clipped-corner motif of corporate HUD panels).
private val CorpoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = CutCornerShape(topEnd = 10.dp),
    large = CutCornerShape(topEnd = 14.dp),
    extraLarge = CutCornerShape(topEnd = 18.dp),
)

@Composable
fun FaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CorpoColorScheme,
        typography = CorpoTypography,
        shapes = CorpoShapes,
        content = content,
    )
}
