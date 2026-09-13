package com.frameroom.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Canvas Base & Surface Elevation (Stitch DESIGN.md)
val SurfaceOnyx = Color(0xFF111318)
val SurfaceDim = Color(0xFF111318)
val SurfaceBright = Color(0xFF37393E)
val SurfaceContainerLowest = Color(0xFF0C0E12)
val SurfaceContainerLow = Color(0xFF1A1C20)
val SurfaceContainer = Color(0xFF1E2024)
val SurfaceContainerHigh = Color(0xFF282A2E)
val SurfaceContainerHighest = Color(0xFF333539)

// Typography & Text colors
val OnSurface = Color(0xFFE2E2E8)
val OnSurfaceVariant = Color(0xFF94A3B8)
val OnSurfaceMuted = Color(0xFFD8C3AD)
val InverseSurface = Color(0xFFE2E2E8)
val InverseOnSurface = Color(0xFF2F3035)

// Primary: Warm Amber / Gold (Vintage Tungsten Flash)
val AmberPrimary = Color(0xFFFFC174)
val OnAmber = Color(0xFF472A00)
val AmberContainer = Color(0xFFF59E0B)
val OnAmberContainer = Color(0xFF613B00)
val AmberFixed = Color(0xFFFFDDB8)
val AmberFixedDim = Color(0xFFFFB95F)
val OnAmberFixed = Color(0xFF2A1700)
val OnAmberFixedVariant = Color(0xFF653E00)
val AmberSurfaceTint = Color(0xFFFFB95F)

// Secondary: Indigo / Violet (Collaborators & Room Aura)
val IndigoSecondary = Color(0xFFC0C1FF)
val OnIndigo = Color(0xFF1000A9)
val IndigoContainer = Color(0xFF3131C0)
val IndigoAccent = Color(0xFF6366F1)
val OnIndigoContainer = Color(0xFFB0B2FF)
val IndigoFixed = Color(0xFFE1E0FF)
val IndigoFixedDim = Color(0xFFC0C1FF)

// Tertiary: Cyan / Ice Blue (Telemetry & Highlights)
val TertiaryBlue = Color(0xFF8FD5FF)
val OnTertiary = Color(0xFF00344A)
val TertiaryContainer = Color(0xFF1ABDFF)
val OnTertiaryContainer = Color(0xFF004966)
val TertiaryFixed = Color(0xFFC5E7FF)
val TertiaryFixedDim = Color(0xFF7FD0FF)

// Error
val ErrorColor = Color(0xFFFFB4AB)
val OnError = Color(0xFF690005)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)

// Outlines & Borders
val OutlineColor = Color(0xFFA08E7A)
val OutlineVariant = Color(0xFF534434)
val HairlineBorder = Color(0x1FFFFFFF) // rgba(255, 255, 255, 0.08)
val HairlineBorderLight = Color(0x33FFFFFF) // rgba(255, 255, 255, 0.20)

// Premium Ambient Gradients
val AmberButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFFFB95F), Color(0xFFFFC174))
)

val CardGlowGradient = Brush.radialGradient(
    colors = listOf(Color(0x33F59E0B), Color.Transparent)
)

val HeroVignetteGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0x99111318), Color(0xFF111318))
)

val AmberGlow = Color(0x33F59E0B)
val IndigoPrimary = IndigoSecondary

object DarkSurfaces {
    val Secondary = IndigoSecondary
    val SecondaryContainer = IndigoContainer
}

