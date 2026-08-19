package io.github.findanonymity.fa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Corpo HUD typography: a monospace "display" voice for titles, labels, and data readouts
// (countdowns, statuses) keeps the terminal feel; body copy uses a clean sans for readability
// and a more modern, polished look — the classic display+body split.
private val Mono = FontFamily.Monospace
private val Body = FontFamily.SansSerif

val CorpoTypography = Typography(
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 2.0.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.5.sp),
    titleSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.2.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.0.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

/** Monospace style for inline data/countdowns inside sans body contexts. */
val MonoData = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium)
