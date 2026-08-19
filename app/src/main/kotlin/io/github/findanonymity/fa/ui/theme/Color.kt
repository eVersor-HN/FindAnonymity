package io.github.findanonymity.fa.ui.theme

import androidx.compose.ui.graphics.Color

// Cyberpunk-corpo palette: cold blue-black steel surfaces, iconic neon-yellow primary,
// cyan for "online/active" data, Arasaka red for danger. Angular, high-contrast, HUD-like.

val CorpoVoid = Color(0xFF000000)          // background — true black for OLED
val CorpoSurface = Color(0xFF07090F)       // near-black: top bar, inset rule chips
val CorpoSurfaceRaised = Color(0xFF12161F) // elevated panels (visible against pure black)
val CorpoSteel = Color(0xFF283245)         // borders / dividers — cold steel

val CorpoYellow = Color(0xFFFCEE0A)        // primary brand accent (the corpo signature)
val CorpoYellowDim = Color(0xFF8A8410)     // pressed / disabled yellow
val CorpoCyan = Color(0xFF34E7F5)          // secondary — online / active / positive
val CorpoAmber = Color(0xFFFF9E3D)         // warning / idle
val CorpoRed = Color(0xFFFF2E43)           // danger / armed — Arasaka red

val CorpoTextPrimary = Color(0xFFE9EEF7)   // cold near-white
val CorpoTextSecondary = Color(0xFF7E8CA3) // muted steel-grey
