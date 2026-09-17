package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual primitives for the product's white/blue tactile UI. No gradients or shadow. */
enum class ApGlyphKind { DOCUMENT, SEARCH, BACK, IMPORT, OPEN, EDIT, TRASH, FOLDER }

/** Small rounded stroke glyphs. Custom drawn and independent from emoji/font availability.
 * Full Lucide asset integration is tracked separately; these are not presented as Lucide assets.
 */
@Composable
fun ApGlyph(kind: ApGlyphKind, modifier: Modifier = Modifier.size(24.dp), color: Color = ApColors.Navy) {
    Canvas(modifier) {
        val u = size.minDimension
        val w = u * .085f
        val line = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = w, cap = StrokeCap.Round)
        when (kind) {
            ApGlyphKind.DOCUMENT -> {
                drawRoundRect(color, topLeft = Offset(.24f * u, .12f * u), size = Size(.52f * u, .75f * u), cornerRadius = CornerRadius(.07f * u), style = line)
                segment(.34f, .46f, .66f, .46f)
                segment(.34f, .60f, .66f, .60f)
            }
            ApGlyphKind.SEARCH -> {
                drawCircle(color, radius = .245f * u, center = Offset(.42f * u, .42f * u), style = line)
                segment(.60f, .61f, .85f, .86f)
            }
            ApGlyphKind.BACK -> {
                segment(.65f, .18f, .34f, .50f)
                segment(.34f, .50f, .65f, .82f)
            }
            ApGlyphKind.IMPORT -> {
                segment(.50f, .12f, .50f, .67f)
                segment(.28f, .47f, .50f, .69f)
                segment(.50f, .69f, .72f, .47f)
                segment(.19f, .84f, .81f, .84f)
            }
            ApGlyphKind.OPEN -> {
                drawRoundRect(color, topLeft = Offset(.13f * u, .29f * u), size = Size(.56f * u, .57f * u), cornerRadius = CornerRadius(.08f * u), style = line)
                segment(.50f, .13f, .87f, .13f)
                segment(.87f, .13f, .87f, .50f)
                segment(.87f, .13f, .45f, .55f)
            }
            ApGlyphKind.EDIT -> {
                segment(.23f, .70f, .66f, .27f)
                segment(.32f, .80f, .75f, .37f)
                segment(.23f, .70f, .19f, .84f)
                segment(.19f, .84f, .32f, .80f)
            }
            ApGlyphKind.TRASH -> {
                drawRoundRect(color, topLeft = Offset(.27f * u, .32f * u), size = Size(.46f * u, .53f * u), cornerRadius = CornerRadius(.05f * u), style = line)
                segment(.19f, .23f, .81f, .23f)
                segment(.40f, .13f, .60f, .13f)
                segment(.42f, .44f, .42f, .71f)
                segment(.58f, .44f, .58f, .71f)
            }
            ApGlyphKind.FOLDER -> {
                drawRoundRect(color, topLeft = Offset(.13f * u, .35f * u), size = Size(.74f * u, .48f * u), cornerRadius = CornerRadius(.07f * u), style = line)
                segment(.17f, .35f, .17f, .22f)
                segment(.17f, .22f, .43f, .22f)
                segment(.43f, .22f, .55f, .35f)
            }
        }
    }
}

@Composable
fun ApRaisedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: ApGlyphKind? = null,
    secondary: Boolean = false,
) {
    Box(
        modifier.fillMaxWidth().height(58.dp)
            .background(if (secondary) Color(0xFFCDE8F8) else ApColors.Pressed, RoundedCornerShape(19.dp))
            .padding(bottom = 5.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (secondary) Color(0xFFEAF8FF) else ApColors.Primary,
                contentColor = if (secondary) ApColors.Navy else ApColors.White,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            if (glyph != null) {
                ApGlyph(glyph, modifier = Modifier.size(22.dp), color = if (secondary) ApColors.Navy else ApColors.White)
                Spacer(Modifier.width(9.dp))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ApCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ApColors.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFCDE8F8)),
    ) {
        Column(Modifier.padding(17.dp), content = content)
    }
}

@Composable
fun ApEyebrow(label: String, modifier: Modifier = Modifier) {
    Text(label.uppercase(), modifier = modifier, color = ApColors.Pressed, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
}
