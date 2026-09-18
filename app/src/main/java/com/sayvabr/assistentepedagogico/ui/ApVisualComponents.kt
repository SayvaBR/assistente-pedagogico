package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ApGlyphKind { DOCUMENT, SEARCH, BACK, IMPORT, OPEN, EDIT, TRASH, FOLDER, HOME, CALENDAR, USERS, MORE, STAR, RESTORE, PLUS, CHECK, CLOCK, NOTE }

@Composable fun ApGlyph(kind: ApGlyphKind, modifier: Modifier = Modifier.size(24.dp), color: Color = ApColors.Navy) {
    Canvas(modifier) {
        val u = size.minDimension; val w = u * .085f; val line = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(x1*u,y1*u), Offset(x2*u,y2*u), strokeWidth=w, cap=StrokeCap.Round)
        when(kind) {
            ApGlyphKind.DOCUMENT, ApGlyphKind.NOTE -> { drawRoundRect(color, Offset(.24f*u,.12f*u), Size(.52f*u,.75f*u), CornerRadius(.07f*u), style=line); segment(.34f,.46f,.66f,.46f); segment(.34f,.60f,.66f,.60f) }
            ApGlyphKind.SEARCH -> { drawCircle(color,.245f*u,Offset(.42f*u,.42f*u),style=line); segment(.60f,.61f,.85f,.86f) }
            ApGlyphKind.BACK -> { segment(.65f,.18f,.34f,.50f); segment(.34f,.50f,.65f,.82f) }
            ApGlyphKind.IMPORT -> { segment(.50f,.12f,.50f,.67f); segment(.28f,.47f,.50f,.69f); segment(.50f,.69f,.72f,.47f); segment(.19f,.84f,.81f,.84f) }
            ApGlyphKind.OPEN -> { drawRoundRect(color,Offset(.13f*u,.29f*u),Size(.56f*u,.57f*u),CornerRadius(.08f*u),style=line); segment(.50f,.13f,.87f,.13f); segment(.87f,.13f,.87f,.50f); segment(.87f,.13f,.45f,.55f) }
            ApGlyphKind.EDIT -> { segment(.23f,.70f,.66f,.27f); segment(.32f,.80f,.75f,.37f); segment(.23f,.70f,.19f,.84f); segment(.19f,.84f,.32f,.80f) }
            ApGlyphKind.TRASH -> { drawRoundRect(color,Offset(.27f*u,.32f*u),Size(.46f*u,.53f*u),CornerRadius(.05f*u),style=line); segment(.19f,.23f,.81f,.23f); segment(.40f,.13f,.60f,.13f); segment(.42f,.44f,.42f,.71f); segment(.58f,.44f,.58f,.71f) }
            ApGlyphKind.FOLDER -> { drawRoundRect(color,Offset(.13f*u,.35f*u),Size(.74f*u,.48f*u),CornerRadius(.07f*u),style=line); segment(.17f,.35f,.17f,.22f); segment(.17f,.22f,.43f,.22f); segment(.43f,.22f,.55f,.35f) }
            ApGlyphKind.HOME -> { segment(.13f,.45f,.50f,.13f); segment(.50f,.13f,.87f,.45f); segment(.22f,.39f,.22f,.84f); segment(.78f,.39f,.78f,.84f); segment(.22f,.84f,.41f,.84f); segment(.41f,.84f,.41f,.62f); segment(.41f,.62f,.59f,.62f); segment(.59f,.62f,.59f,.84f); segment(.59f,.84f,.78f,.84f) }
            ApGlyphKind.CALENDAR -> { drawRoundRect(color,Offset(.16f*u,.23f*u),Size(.68f*u,.66f*u),CornerRadius(.07f*u),style=line); segment(.16f,.43f,.84f,.43f); segment(.35f,.12f,.35f,.31f); segment(.65f,.12f,.65f,.31f); drawCircle(color,.045f*u,Offset(.39f*u,.63f*u)); drawCircle(color,.045f*u,Offset(.60f*u,.63f*u)) }
            ApGlyphKind.USERS -> { drawCircle(color,.135f*u,Offset(.39f*u,.32f*u),style=line); drawCircle(color,.10f*u,Offset(.72f*u,.36f*u),style=line); segment(.13f,.82f,.13f,.69f); segment(.13f,.69f,.24f,.57f); segment(.24f,.57f,.54f,.57f); segment(.54f,.57f,.66f,.69f); segment(.66f,.69f,.66f,.82f); segment(.13f,.82f,.66f,.82f); segment(.70f,.57f,.84f,.63f); segment(.84f,.63f,.87f,.81f) }
            ApGlyphKind.MORE -> { drawCircle(color,.07f*u,Offset(.22f*u,.50f*u)); drawCircle(color,.07f*u,Offset(.50f*u,.50f*u)); drawCircle(color,.07f*u,Offset(.78f*u,.50f*u)) }
            ApGlyphKind.STAR -> { val p=listOf(.50f to .11f,.62f to .39f,.91f to .43f,.68f to .62f,.75f to .89f,.50f to .74f,.25f to .89f,.32f to .62f,.09f to .43f,.38f to .39f,.50f to .11f); p.zipWithNext().forEach{(a,b)->segment(a.first,a.second,b.first,b.second)} }
            ApGlyphKind.RESTORE -> { segment(.31f,.20f,.13f,.38f); segment(.13f,.38f,.34f,.38f); segment(.13f,.38f,.13f,.18f); drawArc(color,-65f,305f,false,Offset(.19f*u,.19f*u),Size(.64f*u,.64f*u),style=line) }
            ApGlyphKind.PLUS -> { segment(.50f,.16f,.50f,.84f); segment(.16f,.50f,.84f,.50f) }
            ApGlyphKind.CHECK -> { segment(.18f,.53f,.42f,.75f); segment(.42f,.75f,.84f,.26f) }
            ApGlyphKind.CLOCK -> { drawCircle(color,.34f*u,Offset(.50f*u,.50f*u),style=line); segment(.50f,.29f,.50f,.50f); segment(.50f,.50f,.68f,.61f) }
        }
    }
}

@Composable fun ApRaisedButton(label:String,onClick:()->Unit,modifier:Modifier=Modifier,glyph:ApGlyphKind?=null,secondary:Boolean=false,enabled:Boolean=true) {
    val interaction=remember{MutableInteractionSource()}; val pressed by interaction.collectIsPressedAsState(); val depth=if(pressed&&enabled)1.dp else 5.dp
    val top=if(secondary)ApPalette.LightSurface else ApColors.Primary; val bottom=if(secondary)ApPalette.Outline else ApColors.Pressed
    Box(modifier.fillMaxWidth().height(60.dp).background(if(enabled)bottom else ApPalette.Outline,RoundedCornerShape(19.dp)).padding(bottom=depth,top=5.dp-depth)) {
        Button(onClick=onClick,enabled=enabled,interactionSource=interaction,modifier=Modifier.fillMaxSize().semantics{contentDescription=label},shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=top,contentColor=if(secondary)ApColors.Navy else ApColors.White,disabledContainerColor=ApPalette.Outline,disabledContentColor=ApColors.Navy),contentPadding=PaddingValues(horizontal=12.dp)) {
            if(glyph!=null){ApGlyph(glyph,Modifier.size(22.dp),if(secondary)ApColors.Navy else ApColors.White);Spacer(Modifier.width(9.dp))}
            Text(label,fontSize=15.sp,lineHeight=19.sp,fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable fun ApCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){ Surface(modifier=modifier.fillMaxWidth(),color=ApColors.White,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,ApPalette.Outline)){Column(Modifier.padding(17.dp),content=content)} }

/** Current eyebrow is a hero-card primitive; white is the safe default on primary blue. */
@Composable fun ApEyebrow(label:String,modifier:Modifier=Modifier,onPrimary:Boolean=true){ Text(label.uppercase(),modifier=modifier,color=if(onPrimary)ApColors.White else ApColors.Pressed,fontWeight=FontWeight.Black,fontSize=11.sp,lineHeight=16.sp,letterSpacing=1.sp) }

@Composable fun ApSectionHeading(title:String,description:String?=null,modifier:Modifier=Modifier){ Column(modifier.fillMaxWidth()){Text(title,color=ApColors.Navy,fontSize=21.sp,lineHeight=27.sp,fontWeight=FontWeight.Black);if(description!=null){Spacer(Modifier.height(4.dp));Text(description,color=ApColors.Navy,fontSize=14.sp,lineHeight=20.sp)}} }
