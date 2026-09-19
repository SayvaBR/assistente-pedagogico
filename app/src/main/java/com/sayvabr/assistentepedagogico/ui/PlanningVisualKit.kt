package com.sayvabr.assistentepedagogico.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.LessonStatus

/** A single presentation vocabulary for Planning; all actions are backed by actual routes. */
@Composable
fun PlanningTopBar(title: String, subtitle: String? = null, back: (() -> Unit)? = null,
                   action: (() -> Unit)? = null, actionDescription: String = "Abrir calendário") {
    val shape = RoundedCornerShape(ApShapeToken.Hero)
    Box(Modifier.fillMaxWidth().clip(shape).background(ApPalette.Primary)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = .14f), radius = 64.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width + 8.dp.toPx(), -5.dp.toPx()))
            drawCircle(ApPalette.Navy.copy(alpha = .13f), radius = 43.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 10.dp.toPx(), size.height + 15.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (back != null) {
                Surface(Modifier.size(48.dp).clickable(role = Role.Button, onClick = back).semantics { contentDescription = "Voltar" },
                    color = Color.White, shape = RoundedCornerShape(17.dp)) {
                    Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(24.dp), ApPalette.Pressed) }
                }
            } else {
                Surface(Modifier.size(48.dp), color = Color.White.copy(alpha = .20f), shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .38f))) {
                    Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.CALENDAR, Modifier.size(26.dp), Color.White) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, fontWeight = FontWeight.Black, fontSize = if (back == null) 25.sp else 22.sp,
                    lineHeight = 29.sp, letterSpacing = (-.5).sp, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = Color.White.copy(alpha = .94f), fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, lineHeight = 17.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            if (action != null) {
                Surface(Modifier.size(48.dp).clickable(role = Role.Button, onClick = action)
                    .semantics { contentDescription = actionDescription }, color = Color.White,
                    shape = RoundedCornerShape(17.dp)) {
                    Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.CALENDAR, Modifier.size(25.dp), ApPalette.Pressed) }
                }
            }
        }
    }
}

@Composable
fun PlanningTabs(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = ApPalette.White, shape = RoundedCornerShape(ApShapeToken.Card),
        border = BorderStroke(1.dp, ApPalette.Outline)) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Dia", "Semana", "Mês").forEach { option ->
                val current = selected == option
                val scale by animateFloatAsState(if (current) 1f else .975f,
                    animationSpec = spring(stiffness = 500f), label = "Selecionar período")
                Box(Modifier.weight(1f).heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (current) ApPalette.Primary else ApPalette.LightSurface)
                    .clickable(role = Role.Tab, onClick = { onSelect(option) })
                    .semantics { contentDescription = "$option${if (current) ", selecionado" else ""}" },
                    contentAlignment = Alignment.Center) {
                    Text(option, color = if (current) Color.White else ApPalette.Navy,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PlanningLessonTile(lesson: Lesson, classroomName: String, onOpen: () -> Unit,
                       archived: Boolean = false) {
    val action = if (archived) "Restaurar plano" else if (lesson.status == LessonStatus.DRAFT)
        "Abrir e editar plano" else "Consultar e gerenciar estado"
    val status = if (archived) "Arquivado" else lesson.status.label
    val tileShape = RoundedCornerShape(ApShapeToken.Card)
    Column(Modifier.fillMaxWidth().clip(tileShape).background(ApPalette.Outline)
        .padding(bottom = ApSizeToken.ButtonDepth).clip(tileShape).background(Color.White)
        .border(1.dp, ApPalette.Outline, tileShape)
        .clickable(role = Role.Button, onClick = onOpen)
        .semantics { contentDescription = "${lesson.title}, $status, $classroomName, ${lesson.time}. $action" }
        .padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Surface(Modifier.size(52.dp), color = ApPalette.LightSurface, shape = RoundedCornerShape(16.dp)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(27.dp), ApPalette.Primary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(lesson.subject.ifBlank { lesson.title }, color = ApPalette.Navy,
                    fontWeight = FontWeight.Black, fontSize = 16.sp, lineHeight = 20.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$classroomName  ·  ${lesson.time}", color = ApPalette.Navy.copy(alpha = .76f),
                    fontSize = 12.sp, lineHeight = 17.sp)
                if (lesson.title != lesson.subject) Text(lesson.title, color = ApPalette.Navy,
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Surface(color = ApPalette.LightSurface, shape = CircleShape) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    ApGlyph(ApGlyphKind.OPEN, Modifier.size(19.dp), ApPalette.Pressed)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(ApPalette.Outline))
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = ApPalette.LightSurface, shape = CircleShape) {
                Text(status, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp,
                    color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ApPalette.Pressed,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Timeline guide stretches with multi-line cards; no fixed 93dp line that cuts through content. */
@Composable
fun PlanningDayEntry(time: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
        Text(time, Modifier.width(51.dp).padding(top = 18.dp), fontSize = 13.sp,
            color = ApPalette.Navy, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.width(19.dp).fillMaxHeight().padding(top = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(12.dp).background(ApPalette.Primary, CircleShape)
                .border(2.dp, Color.White, CircleShape))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.width(2.dp).weight(1f).background(ApPalette.Outline))
        }
        Spacer(Modifier.width(5.dp))
        Box(Modifier.weight(1f).padding(bottom = 8.dp)) { content() }
    }
}

@Composable
fun PlanningInspiration(onExplore: () -> Unit) {
    val shape = RoundedCornerShape(ApShapeToken.Hero)
    Box(Modifier.fillMaxWidth().clip(shape).background(ApPalette.Navy)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(ApPalette.Primary.copy(alpha = .55f), 68.dp.toPx(),
                androidx.compose.ui.geometry.Offset(size.width + 20.dp.toPx(), 15.dp.toPx()))
        }
        Column(Modifier.padding(19.dp)) {
            Text("PREPARE A PRÓXIMA AULA", fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp, letterSpacing = 1.sp, color = ApPalette.LightSurface)
            Spacer(Modifier.height(6.dp))
            Text("Precisa de inspiração?", fontWeight = FontWeight.Black, fontSize = 21.sp,
                lineHeight = 26.sp, color = Color.White)
            Spacer(Modifier.height(5.dp))
            Text("Explore suas atividades e organize as próximas aulas.", color = Color.White,
                fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(15.dp))
            ApRaisedButton("Explorar atividades", onClick = onExplore,
                glyph = ApGlyphKind.DOCUMENT, secondary = true)
        }
    }
}
