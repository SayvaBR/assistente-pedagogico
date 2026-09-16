package com.sayvabr.assistentepedagogico.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ApColors {
    val Primary = Color(0xFF1CB0F6)
    val Pressed = Color(0xFF1899D6)
    val Navy = Color(0xFF102A56)
    val Sky = Color(0xFFDDF4FF)
    val White = Color(0xFFFFFFFF)
    val Canvas = Sky
}

private val apColorScheme = lightColorScheme(
    primary = ApColors.Primary,
    onPrimary = ApColors.White,
    background = ApColors.Canvas,
    onBackground = ApColors.Navy,
    surface = ApColors.White,
    onSurface = ApColors.Navy,
)

@Composable
fun ApTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = apColorScheme, content = content)
}
