package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual primitives for the product's white/blue tactile UI. No gradients or blurred shadows. */
enum class ApGlyphKind {
    DOCUMENT, SEARCH, BACK, IMPORT, OPEN, EDIT, TRASH, FOLDER,
    HOME, CALENDAR, USERS, MORE, STAR, RESTORE, PLUS, CHECK
}

/** Rounded-stroke glyphs with one visual language while the app migrates away from emoji/font symbols. */
@Composable
fun ApGlyph(kind: ApGlyphKind, modifier: Modifier = Modifier.size(24.dp), color: Color = ApColors.Navy) {
    Canvas(modifier) {
        val u = size.minDimension
        val w = u * .082f
        val line = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = w, cap = StrokeCap.Round)
        when (kind) {
            ApGlyphKind.DOCUMENT -> {
                drawRoundRect(color, topLeft = Offset(.24f * u, .12f * u), size = Size(.52f * u, .75f * u), cornerRadius = CornerRadius(.07f * u), style = line)
                segment(.34f, .46f, .66f, .46f); segment(.34f, .60f, .66f, .60f)
            }
            ApGlyphKind.SEARCH -> {
                drawCircle(color, radius = .245f * u, center = Offset(.42f * u, .42f * u), style = line)
                segment(.60f, .61f, .85f, .86f)
            }
            ApGlyphKind.BACK -> { segment(.65f, .18f, .34f, .50f); segment(.34f, .50f, .65f, .82f) }
            ApGlyphKind.IMPORT -> {
                segment(.50f, .12f, .50f, .67f); segment(.28f, .47f, .50f, .69f)
                segment(.50f, .69f, .72f, .47f); segment(.19f, .84f, .81f, .84f)
            }
            ApGlyphKind.OPEN -> {
                drawRoundRect(color, topLeft = Offset(.13f * u, .29f * u), size = Size(.56f * u, .57f * u), cornerRadius = CornerRadius(.08f * u), style = line)
                segment(.50f, .13f, .87f, .13f); segment(.87f, .13f, .87f, .50f); segment(.87f, .13f, .45f, .55f)
            }
            ApGlyphKind.EDIT -> {
                segment(.23f, .70f, .66f, .27f); segment(.32f, .80f, .75f, .37f)
                segment(.23f, .70f, .19f, .84f); segment(.19f, .84f, .32f, .80f)
            }
            ApGlyphKind.TRASH -> {
                drawRoundRect(color, topLeft = Offset(.27f * u, .32f * u), size = Size(.46f * u, .53f * u), cornerRadius = CornerRadius(.05f * u), style = line)
                segment(.19f, .23f, .81f, .23f); segment(.40f, .13f, .60f, .13f)
                segment(.42f, .44f, .42f, .71f); segment(.58f, .44f, .58f, .71f)
            }
            ApGlyphKind.FOLDER -> {
                drawRoundRect(color, topLeft = Offset(.13f * u, .35f * u), size = Size(.74f * u, .48f * u), cornerRadius = CornerRadius(.07f * u), style = line)
                segment(.17f, .35f, .17f, .22f); segment(.17f, .22f, .43f, .22f); segment(.43f, .22f, .55f, .35f)
            }
            ApGlyphKind.HOME -> {
                segment(.16f, .48f, .50f, .18f); segment(.50f, .18f, .84f, .48f)
                segment(.24f, .43f, .24f, .83f); segment(.76f, .43f, .76f, .83f); segment(.24f, .83f, .76f, .83f)
            }
            ApGlyphKind.CALENDAR -> {
                drawRoundRect(color, topLeft = Offset(.16f*u,.24f*u), size = Size(.68f*u,.60f*u), cornerRadius = CornerRadius(.08f*u), style = line)
                segment(.16f,.41f,.84f,.41f); segment(.32f,.13f,.32f,.31f); segment(.68f,.13f,.68f,.31f)
                segment(.34f,.56f,.43f,.56f); segment(.57f,.56f,.66f,.56f); segment(.34f,.69f,.43f,.69f)
            }
            ApGlyphKind.USERS -> {
                drawCircle(color, radius = .15f*u, center = Offset(.40f*u,.36f*u), style = line)
                drawCircle(color, radius = .12f*u, center = Offset(.70f*u,.42f*u), style = line)
                drawArc(color, 205f, 130f, false, topLeft = Offset(.15f*u,.48f*u), size = Size(.50f*u,.38f*u), style = line)
                drawArc(color, 215f, 105f, false, topLeft = Offset(.52f*u,.56f*u), size = Size(.34f*u,.27f*u), style = line)
            }
            ApGlyphKind.MORE -> {
                drawCircle(color, radius = .055f*u, center = Offset(.25f*u,.50f*u))
                drawCircle(color, radius = .055f*u, center = Offset(.50f*u,.50f*u))
                drawCircle(color, radius = .055f*u, center = Offset(.75f*u,.50f*u))
            }
            ApGlyphKind.STAR -> {
                val p = listOf(.50f to .13f, .60f to .39f, .88f to .40f, .66f to .57f, .74f to .84f, .50f to .68f, .26f to .84f, .34f to .57f, .12f to .40f, .40f to .39f)
                for (i in p.indices) { val a=p[i]; val b=p[(i+1)%p.size]; segment(a.first,a.second,b.first,b.second) }
            }
            ApGlyphKind.RESTORE -> {
                drawArc(color, 45f, 285f, false, topLeft = Offset(.18f*u,.18f*u), size = Size(.64f*u,.64f*u), style = line)
                segment(.18f,.29f,.18f,.55f); segment(.18f,.29f,.43f,.29f)
            }
            ApGlyphKind.PLUS -> { segment(.50f,.20f,.50f,.80f); segment(.20f,.50f,.80f,.50f) }
            ApGlyphKind.CHECK -> { segment(.20f,.52f,.42f,.72f); segment(.42f,.72f,.80f,.28f) }
        }
    }
}

/** Tactile CTA: solid lower strip only, matching the product rule against soft shadows/gradients. */
@Composable
fun ApRaisedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: ApGlyphKind? = null,
    secondary: Boolean = false,
) {
    val base = if (secondary) ApPalette.Outline else ApColors.Pressed
    val face = if (secondary) ApPalette.White else ApColors.Primary
    val content = if (secondary) ApColors.Navy else ApColors.White
    Box(
        modifier.fillMaxWidth().height(60.dp)
            .background(base, RoundedCornerShape(ApShapeToken.Card))
            .padding(bottom = ApSizeToken.ButtonDepth),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(ApShapeToken.Medium),
            border = if (secondary) BorderStroke(1.dp, ApPalette.Outline) else null,
            colors = ButtonDefaults.buttonColors(containerColor = face, contentColor = content),
            contentPadding = PaddingValues(horizontal = ApSpace.Base),
        ) {
            if (glyph != null) {
                ApGlyph(glyph, modifier = Modifier.size(22.dp), color = content)
                Spacer(Modifier.width(ApSpace.Sm))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = .1.sp)
        }
    }
}

@Composable
fun ApCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ApColors.White,
        shape = RoundedCornerShape(ApShapeToken.Card),
        border = BorderStroke(1.dp, ApPalette.Outline),
    ) { Column(Modifier.padding(ApSpace.Base), content = content) }
}

/** Small icon surface used by rows/cards instead of emoji. */
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

/** Consistent blue/white segmented navigation for Dia/Semana/Mês, Todos/Favoritos/Lixeira, etc. */
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
                    modifier = Modifier.weight(1f).heightIn(min = ApSizeToken.MinTouchTarget).clickable { onSelect(option) },
                    color = if (active) ApColors.Primary else ApPalette.White,
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = if (active) ApColors.White else ApColors.Navy,
                            fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun ApEyebrow(label: String, modifier: Modifier = Modifier) {
    Text(label.uppercase(), modifier = modifier, color = ApColors.Pressed,
        fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
}
