package com.sayvabr.assistentepedagogico.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Canonical blue/white palette; consumed by ApTheme and shared components. */
object ApPalette {
    val Primary = Color(0xFF1CB0F6)
    val Pressed = Color(0xFF1899D6)
    val ActionGradientTop = Color(0xFF0874D1)
    val Action = Color(0xFF0063D9)
    val ActionPressed = Color(0xFF0054B8)
    val DisabledSurface = Color(0xFFDBE7F0)
    val DisabledText = Color(0xFF526A80)
    val Navy = Color(0xFF102A56)
    val Sky = Color(0xFFDDF4FF)
    val White = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFEAF8FF)
    val Outline = Color(0xFFCDE8F8)
}

/** Semantic spacing in dp; use these rather than introducing arbitrary per-screen values. */
object ApSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Base = 16.dp
    val Lg = 20.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

object ApShapeToken {
    val Small = 12.dp
    val Medium = 16.dp
    val Card = 20.dp
    val Hero = 24.dp
    val Pill = 50.dp
}

object ApSizeToken {
    /** Lower solid strip on pressable elements; never a blurred drop shadow. */
    val ButtonDepth = 5.dp
    val MinTouchTarget = 48.dp
    val StandardIcon = 24.dp
}
