package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Compatibility aliases while screens move to the canonical ApPalette tokens. */
object ApColors {
    val Primary = ApPalette.Primary
    val Pressed = ApPalette.Pressed
    val Navy = ApPalette.Navy
    val Sky = ApPalette.Sky
    val White = ApPalette.White
    val Canvas = ApPalette.Sky
}

/**
 * Every Material 3 semantic color role must honor the product palette.
 * Unspecified secondary/tertiary containers fall back to lavender in the stock
 * light theme (observed on Files filter chips in real Android screenshots).
 */
private val apColorScheme = lightColorScheme(
    primary = ApPalette.Primary,
    onPrimary = ApPalette.White,
    primaryContainer = ApPalette.LightSurface,
    onPrimaryContainer = ApPalette.Navy,
    secondary = ApPalette.Pressed,
    onSecondary = ApPalette.White,
    secondaryContainer = ApPalette.LightSurface,
    onSecondaryContainer = ApPalette.Navy,
    tertiary = ApPalette.Primary,
    onTertiary = ApPalette.White,
    tertiaryContainer = ApPalette.LightSurface,
    onTertiaryContainer = ApPalette.Navy,
    background = ApPalette.Sky,
    onBackground = ApPalette.Navy,
    surface = ApPalette.White,
    onSurface = ApPalette.Navy,
    surfaceTint = ApPalette.Primary,
    outline = ApPalette.Outline,
    surfaceVariant = ApPalette.LightSurface,
    onSurfaceVariant = ApPalette.Navy,
)

private val apShapes = Shapes(
    extraSmall = RoundedCornerShape(ApShapeToken.Small),
    small = RoundedCornerShape(ApShapeToken.Medium),
    medium = RoundedCornerShape(ApShapeToken.Card),
    large = RoundedCornerShape(ApShapeToken.Hero),
    extraLarge = RoundedCornerShape(ApShapeToken.Hero),
)

private val apTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun ApTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = apColorScheme, shapes = apShapes, typography = apTypography, content = content)
}
