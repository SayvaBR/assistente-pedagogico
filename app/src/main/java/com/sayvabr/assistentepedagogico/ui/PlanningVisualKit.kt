package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.LessonStatus

/** Planning-only presentation: shared palette, solid borders, accessible touch targets. */
@Composable
fun PlanningTopBar(title: String, subtitle: String? = null, back: (() -> Unit)? = null,
                   action: (() -> Unit)? = null, actionDescription: String = "Abrir calendário") {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) {
            Surface(Modifier.size(48.dp).clickable(onClick = back), color = ApPalette.White,
                shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(24.dp), ApPalette.Primary) }
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, fontWeight = FontWeight.Black, fontSize = if (back == null) 27.sp else 22.sp,
                lineHeight = 30.sp, color = ApPalette.Navy, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = ApPalette.Navy.copy(alpha = .72f),
                fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2)
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Surface(Modifier.size(48.dp).semantics { contentDescription = actionDescription }.clickable(onClick = action),
                color = ApPalette.White, shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, ApPalette.Outline)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.CALENDAR, Modifier.size(25.dp), ApPalette.Primary) }
            }
        }
    }
}

@Composable
fun PlanningTabs(selected: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = ApPalette.White, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ApPalette.Outline)) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Dia", "Semana", "Mês").forEach { option ->
                val current = selected == option
                Surface(Modifier.weight(1f).heightIn(min = 48.dp).clickable { onSelect(option) },
                    color = if (current) ApPalette.Primary else ApPalette.LightSurface,
                    shape = RoundedCornerShape(15.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = if (current) Color.White else ApPalette.Navy,
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
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
    ApCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Surface(Modifier.size(52.dp), color = ApPalette.LightSurface, shape = RoundedCornerShape(15.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(28.dp), ApPalette.Primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(lesson.subject.ifBlank { lesson.title }, color = ApPalette.Navy,
                    fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text("$classroomName · ${lesson.time}", color = ApPalette.Navy.copy(alpha = .74f), fontSize = 12.sp)
                if (lesson.title != lesson.subject) Text(lesson.title, color = ApPalette.Navy,
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            ApGlyph(ApGlyphKind.OPEN, Modifier.size(21.dp), ApPalette.Primary)
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = ApPalette.LightSurface, shape = RoundedCornerShape(50.dp)) {
                Text(if (archived) "Arquivado" else lesson.status.label,
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp,
                    color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ApPalette.Pressed)
            }
        }
    }
}

@Composable
fun PlanningDayEntry(time: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(time, Modifier.width(55.dp).padding(top = 17.dp), fontSize = 13.sp,
            color = ApPalette.Navy, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.width(17.dp).padding(top = 19.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(12.dp).background(ApPalette.Primary, RoundedCornerShape(50.dp)))
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(2.dp).height(93.dp).background(ApPalette.Outline))
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
fun PlanningInspiration(onExplore: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = ApPalette.Primary, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(19.dp)) {
            Text("Precisa de inspiração?", fontWeight = FontWeight.Black, fontSize = 21.sp,
                lineHeight = 26.sp, color = Color.White)
            Spacer(Modifier.height(5.dp))
            Text("Explore suas atividades e organize as próximas aulas.", color = Color.White,
                fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(13.dp))
            ApRaisedButton("Explorar atividades", onClick = onExplore, glyph = ApGlyphKind.DOCUMENT, secondary = true)
        }
    }
}
