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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.Appointment
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The calendar and every card read the actual SQLite snapshot, never illustrative entries. */
private val monthLabel = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR"))
private val completeDayLabel = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))
private val weekNames = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
private fun capitalized(value: String) = value.replaceFirstChar { it.uppercase() }

@Composable
private fun DateNavigator(focus: LocalDate, mode: String, onPick: (String) -> Unit) {
    val label = when (mode) {
        "Mês" -> capitalized(focus.format(monthLabel))
        "Semana" -> "${PlanningCalendarPolicy.weekStart(focus).format(DateTimeFormatter.ofPattern("dd/MM"))} – ${PlanningCalendarPolicy.weekStart(focus).plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
        else -> capitalized(focus.format(completeDayLabel))
    }
    val previous = when (mode) {
        "Mês" -> PlanningCalendarPolicy.shiftMonth(focus, -1)
        "Semana" -> PlanningCalendarPolicy.shiftWeek(focus, -1)
        else -> focus.minusDays(1)
    }
    val next = when (mode) {
        "Mês" -> PlanningCalendarPolicy.shiftMonth(focus, 1)
        "Semana" -> PlanningCalendarPolicy.shiftWeek(focus, 1)
        else -> focus.plusDays(1)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(48.dp).semantics { contentDescription = "Período anterior" }.clickable { onPick(previous.toString()) },
            color = ApPalette.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
            Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(21.dp), ApPalette.Primary) }
        }
        Text(label, Modifier.weight(1f).padding(horizontal = 5.dp), color = ApPalette.Navy,
            fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
        Surface(Modifier.size(48.dp).semantics { contentDescription = "Próximo período" }.clickable { onPick(next.toString()) },
            color = ApPalette.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
            Box(contentAlignment = Alignment.Center) { Text("›", fontSize = 31.sp, color = ApPalette.Primary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MonthGrid(selected: LocalDate, lessonDays: Set<String>, appointmentDays: Set<String>, onPick: (String) -> Unit) {
    val firstMonth = selected.month
    ApCard {
        Row(Modifier.fillMaxWidth()) {
            weekNames.forEach { label -> Text(label, Modifier.weight(1f), textAlign = TextAlign.Center,
                color = ApPalette.Navy.copy(alpha = .72f), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(9.dp))
        PlanningCalendarPolicy.monthCells(selected).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    val active = date == selected
                    val dimmed = date.month != firstMonth
                    val iso = date.toString()
                    val hasLesson = iso in lessonDays
                    val hasAppointment = iso in appointmentDays
                    val description = "${date.dayOfMonth}/${date.monthValue}/${date.year}" +
                        (if (hasLesson) ", aula planejada" else "") + (if (hasAppointment) ", compromisso" else "")
                    Surface(Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = description }.clickable { onPick(iso) },
                        color = if (active) ApPalette.Primary else Color.White,
                        shape = RoundedCornerShape(14.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("${date.dayOfMonth}", fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
                                color = if (active) Color.White else if (dimmed) ApPalette.Navy.copy(alpha = .38f) else ApPalette.Navy,
                                fontSize = 15.sp)
                            Row(Modifier.height(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (hasLesson) Box(Modifier.size(5.dp).background(if (active) Color.White else ApPalette.Primary, RoundedCornerShape(50.dp)))
                                if (hasAppointment) Box(Modifier.size(5.dp).background(if (active) Color.White else ApPalette.Pressed, RoundedCornerShape(50.dp)))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("• Aulas e compromissos cadastrados", fontSize = 11.sp,
            color = ApPalette.Navy.copy(alpha = .65f))
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, lessonDays: Set<String>, appointmentDays: Set<String>, onPick: (String) -> Unit) {
    ApCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            PlanningCalendarPolicy.weekDays(selected).forEachIndexed { index, date ->
                val active = date == selected
                val iso = date.toString()
                Surface(Modifier.weight(1f).heightIn(min = 65.dp)
                    .semantics { contentDescription = "${weekNames[index]}, ${date.dayOfMonth}/${date.monthValue}" }
                    .clickable { onPick(iso) }, color = if (active) ApPalette.Primary else ApPalette.LightSurface,
                    shape = RoundedCornerShape(13.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(weekNames[index], color = if (active) Color.White else ApPalette.Navy,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${date.dayOfMonth}", color = if (active) Color.White else ApPalette.Navy,
                            fontSize = 16.sp, fontWeight = FontWeight.Black)
                        if (iso in lessonDays || iso in appointmentDays) Box(Modifier.size(5.dp)
                            .background(if (active) Color.White else ApPalette.Primary, RoundedCornerShape(50.dp)))
                    }
                }
            }
        }
    }
}

/** Month overview, daily timeline and archived plans all remain connected to real data. */
@Composable
fun PlanningWorkspace(
    data: TeacherSnapshot,
    classroom: Classroom?,
    day: String,
    onDay: (String) -> Unit,
    go: (String) -> Unit,
    openLesson: (Long) -> Unit,
    restoreLesson: (Lesson) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf("Mês") }
    var backupExpanded by rememberSaveable { mutableStateOf(false) }
    var showWholeMonth by rememberSaveable { mutableStateOf(false) }
    val focus = LocalDate.parse(day)
    val ownLessons = data.lessons.filter { it.classroomId == classroom?.id && !it.archived }
    val lessonDays = ownLessons.map { it.date }.toSet()
    val relevantAppointments = data.appointments.filter { it.classroomId == null || it.classroomId == classroom?.id }
    val appointmentDays = relevantAppointments.map { it.date }.toSet()
    PlanningTopBar("Planejamento", classroom?.name ?: "Organize suas aulas e sua rotina",
        action = { if (mode == "Mês") onDay(LocalDate.now().toString()) else mode = "Mês" },
        actionDescription = if (mode == "Mês") "Ir para o mês atual" else "Abrir calendário mensal")
    Spacer(Modifier.height(13.dp))
    PlanningTabs(mode) { mode = it; showWholeMonth = false }
    Spacer(Modifier.height(13.dp))
    if (mode != "Arquivados") {
        DateNavigator(focus, mode, onDay)
        Spacer(Modifier.height(12.dp))
        when (mode) {
            "Mês" -> MonthGrid(focus, lessonDays, appointmentDays, onDay)
            "Semana" -> WeekStrip(focus, lessonDays, appointmentDays, onDay)
        }
        Spacer(Modifier.height(18.dp))
    }

    // Monthly overview highlights the picked day but lets the teacher reach every plan in the month.
    val inPeriod = data.lessons.filter { lesson ->
        lesson.classroomId == classroom?.id && when (mode) {
            "Arquivados" -> lesson.archived
            else -> !lesson.archived && PlanningCalendarPolicy.inPeriod(lesson.date, focus, mode)
        }
    }.sortedWith(compareBy<Lesson> { it.date }.thenBy { it.time })
    val visible = if (mode == "Mês" && !showWholeMonth) inPeriod.filter { it.date == day } else inPeriod
    val shownAppointments = if (mode == "Arquivados") emptyList() else relevantAppointments
        .filter { PlanningCalendarPolicy.inPeriod(it.date, focus, mode) }
        .sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
    val caption = when (mode) {
        "Arquivados" -> "Planos arquivados"
        "Semana" -> "Aulas desta semana"
        "Mês" -> if (showWholeMonth) "Aulas do mês" else "Aulas do dia ${focus.dayOfMonth}"
        else -> "${capitalized(focus.format(completeDayLabel))}"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(caption, Modifier.weight(1f), color = ApPalette.Navy, fontSize = 20.sp,
            lineHeight = 25.sp, fontWeight = FontWeight.Black)
        if (mode == "Mês") TextButton(onClick = {
            if (showWholeMonth) mode = "Dia" else showWholeMonth = true
        }) { Text(if (showWholeMonth) "Ver dia" else "Ver mês (${inPeriod.size})",
            color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
    }
    Spacer(Modifier.height(8.dp))
    if (classroom == null) {
        ApCard {
            Text("Seu planejamento começa com uma turma.", color = ApPalette.Navy,
                fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text("Crie uma turma ou recupere seus dados para organizar as aulas.", color = ApPalette.Navy)
        }
        Spacer(Modifier.height(10.dp))
        ApRaisedButton("Gerenciar turmas", onClick = { go("classes") }, glyph = ApGlyphKind.USERS)
    } else {
        if (visible.isEmpty()) {
            ApCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(50.dp), color = ApPalette.LightSurface, shape = RoundedCornerShape(15.dp)) {
                        Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.CALENDAR, Modifier.size(26.dp), ApPalette.Primary) }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(if (mode == "Arquivados") "Nenhum plano arquivado" else "Ainda não há aulas aqui",
                            color = ApPalette.Navy, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(if (mode == "Arquivados") "Seus planos arquivados aparecerão neste espaço."
                            else "Escolha outra data ou crie seu primeiro plano.",
                            color = ApPalette.Navy.copy(alpha = .72f), fontSize = 12.sp)
                    }
                }
            }
        }
        visible.forEach { lesson ->
            Spacer(Modifier.height(7.dp))
            if (mode == "Dia") PlanningDayEntry(lesson.time) {
                PlanningLessonTile(lesson, classroom.name,
                    onOpen = { if (lesson.archived) restoreLesson(lesson) else openLesson(lesson.id) },
                    archived = lesson.archived)
            } else PlanningLessonTile(lesson, classroom.name,
                onOpen = { if (lesson.archived) restoreLesson(lesson) else openLesson(lesson.id) },
                archived = lesson.archived)
        }
        if (mode != "Arquivados") {
            Spacer(Modifier.height(12.dp))
            ApRaisedButton("Adicionar aula", onClick = { go("newLesson") }, glyph = ApGlyphKind.PLUS)
            Spacer(Modifier.height(12.dp))
            if (mode == "Dia") PlanningInspiration { go("activities") }
            Spacer(Modifier.height(17.dp))
            Text("Compromissos do período (${shownAppointments.size})", color = ApPalette.Navy,
                fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            if (shownAppointments.isEmpty()) ApCard {
                Text("Nenhum compromisso cadastrado para este período.", color = ApPalette.Navy,
                    fontSize = 13.sp)
            }
            shownAppointments.forEach { appointment ->
                Spacer(Modifier.height(7.dp))
                ApCard(modifier = Modifier.clickable { onDay(appointment.date); go("agenda") }) {
                    Text("${appointment.date} · ${appointment.time}–${appointment.endTime} · ${appointment.type}",
                        color = ApPalette.Pressed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(appointment.title, color = ApPalette.Navy, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Abrir agenda ›", color = ApPalette.Pressed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            ApRaisedButton("Ver compromissos", onClick = { go("agenda") },
                glyph = ApGlyphKind.CALENDAR, secondary = true)
            Spacer(Modifier.height(8.dp))
            ApRaisedButton("Atividades da turma", onClick = { go("activities") },
                glyph = ApGlyphKind.DOCUMENT, secondary = true)
        }
    }
    Spacer(Modifier.height(16.dp))
    TextButton(onClick = { mode = if (mode == "Arquivados") "Mês" else "Arquivados" }) {
        ApGlyph(ApGlyphKind.FOLDER, Modifier.size(18.dp), ApPalette.Primary)
        Spacer(Modifier.width(8.dp))
        Text(if (mode == "Arquivados") "Voltar ao calendário" else "Ver planos arquivados",
            color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold)
    }
    // Backup stays reachable even on an empty installation and never touches a teacher's data silently.
    TextButton(onClick = { backupExpanded = !backupExpanded }) {
        ApGlyph(ApGlyphKind.RESTORE, Modifier.size(18.dp), ApPalette.Primary)
        Spacer(Modifier.width(8.dp))
        Text(if (backupExpanded) "Ocultar backup e recuperação" else "Backup e recuperação",
            color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold)
    }
    if (backupExpanded) { Spacer(Modifier.height(8.dp)); PlanningBackupPanel() }
}

private data class AgendaEntry(val day: String, val time: String, val title: String, val detail: String,
    val lessonId: Long? = null, val appointmentId: Long? = null)

/** Agenda preserves separate appointment and lesson destinations and the shared date. */
@Composable
fun PlanningAgendaScreen(
    data: TeacherSnapshot, day: String, onDay: (String) -> Unit,
    add: () -> Unit, openAppointment: (Long) -> Unit, openLesson: (Long) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf("Dia") }
    val focus = LocalDate.parse(day)
    val activeLessons = data.lessons.filter { !it.archived && data.classrooms.any { room -> room.id == it.classroomId && !room.archived } }
    val lessonDays = activeLessons.map { it.date }.toSet()
    val appointmentDays = data.appointments.map { it.date }.toSet()
    PlanningTopBar("Compromissos", "Organize sua rotina e foque no que importa",
        action = add, actionDescription = "Novo compromisso")
    Spacer(Modifier.height(12.dp))
    PlanningTabs(mode) { mode = it }
    Spacer(Modifier.height(12.dp))
    DateNavigator(focus, mode, onDay)
    Spacer(Modifier.height(10.dp))
    when (mode) {
        "Mês" -> MonthGrid(focus, lessonDays, appointmentDays, onDay)
        "Semana" -> WeekStrip(focus, lessonDays, appointmentDays, onDay)
    }
    Spacer(Modifier.height(15.dp))
    val inPeriod: (String) -> Boolean = { iso -> PlanningCalendarPolicy.inPeriod(iso, focus, mode) }
    val entries = buildList {
        data.appointments.filter { inPeriod(it.date) }.forEach { appointment: Appointment ->
            val room = appointment.classroomId?.let { id -> data.classrooms.firstOrNull { it.id == id }?.name ?: "Turma indisponível" } ?: "Geral"
            val detail = "${appointment.type} · ${appointment.time}–${appointment.endTime} · $room"
            add(AgendaEntry(appointment.date, appointment.time, appointment.title, detail, appointmentId = appointment.id))
        }
        activeLessons.filter { inPeriod(it.date) }.forEach { lesson ->
            val classroomName = data.classrooms.firstOrNull { it.id == lesson.classroomId }?.name ?: "Turma indisponível"
            add(AgendaEntry(lesson.date, lesson.time, lesson.title, "Aula · $classroomName · ${lesson.subject} · ${lesson.status.label}", lessonId = lesson.id))
        }
    }.sortedWith(compareBy<AgendaEntry> { it.day }.thenBy { it.time }.thenBy { it.title })
    val periodTitle = when (mode) {
        "Mês" -> "Agenda de ${capitalized(focus.format(monthLabel))}"
        "Semana" -> "Agenda da semana"
        else -> capitalized(focus.format(completeDayLabel))
    }
    Text("$periodTitle (${entries.size})", color = ApPalette.Navy, fontWeight = FontWeight.Black, fontSize = 19.sp)
    if (entries.isEmpty()) {
        Spacer(Modifier.height(10.dp))
        ApCard { Text("Nenhum compromisso ou aula neste período.", color = ApPalette.Navy) }
    }
    entries.forEach { entry ->
        Spacer(Modifier.height(9.dp))
        PlanningDayEntry(entry.time) {
            ApCard(modifier = Modifier.clickable {
                entry.lessonId?.let(openLesson) ?: entry.appointmentId?.let(openAppointment)
            }) {
                Text(entry.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = ApPalette.Navy)
                Text("${entry.day} · ${entry.detail}", fontSize = 12.sp, color = ApPalette.Navy)
                Spacer(Modifier.height(5.dp))
                Text(if (entry.lessonId != null) "Consultar plano e gerenciar estado" else "Editar compromisso",
                    color = ApPalette.Pressed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    ApRaisedButton("Novo compromisso", onClick = add, glyph = ApGlyphKind.PLUS)
}