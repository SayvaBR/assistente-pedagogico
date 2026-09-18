package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compatibility components preserved while integrating the improved vector/tactile system. */
@Composable
fun ApIconBadge(kind: ApGlyphKind, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val background = if (emphasized) ApColors.Primary else ApPalette.LightSurface
    val foreground = if (emphasized) ApColors.White else ApColors.Pressed
    Surface(
        modifier = modifier.size(52.dp),
        color = background,
        shape = RoundedCornerShape(ApShapeToken.Medium),
        border = if (emphasized) null else BorderStroke(1.dp, ApPalette.Outline),
    ) { Box(contentAlignment = Alignment.Center) { ApGlyph(kind, Modifier.size(29.dp), foreground) } }
}

/** Preserve the original call style used in existing planning and files screens. */
@Composable
fun ApSegmentedControl(options: List<String>, selected: String, onSelect: (String) -> Unit) =
    ApSegmentedControl(options, selected, Modifier, onSelect)

/** TalkBack exposes selection independently from the primary-blue visual treatment. */
@Composable
fun ApSegmentedControl(
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ApPalette.White,
        shape = RoundedCornerShape(ApShapeToken.Card),
        border = BorderStroke(1.dp, ApPalette.Outline),
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            options.forEach { option ->
                val active = option == selected
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = ApSizeToken.MinTouchTarget)
                        .selectable(selected = active, onClick = { onSelect(option) }),
                    color = if (active) ApColors.Primary else ApPalette.White,
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            option, color = if (active) ApColors.White else ApColors.Navy,
                            fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
