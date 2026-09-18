package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/** Calendars derive all counts and cards from SQLite snapshots; never from demo data. */
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(onClick = {
            val next = when (mode) {
                "Mês" -> PlanningCalendarPolicy.shiftMonth(focus, -1)
                "Semana" -> PlanningCalendarPolicy.shiftWeek(focus, -1)
                else -> focus.minusDays(1)
            }
            onPick(next.toString())
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text("‹ Anterior", color = ApColors.Primary, fontWeight = FontWeight.ExtraBold) }
        Text(label, Modifier.weight(1f), color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
        OutlinedButton(onClick = {
            val next = when (mode) {
                "Mês" -> PlanningCalendarPolicy.shiftMonth(focus, 1)
                "Semana" -> PlanningCalendarPolicy.shiftWeek(focus, 1)
                else -> focus.plusDays(1)
            }
            onPick(next.toString())
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Próximo ›", color = ApColors.Primary, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun MonthGrid(selected: LocalDate, lessonDays: Set<String>, appointmentDays: Set<String>, onPick: (String) -> Unit) {
    val firstMonth = selected.month
    ApCard {
        Row(Modifier.fillMaxWidth()) {
            weekNames.forEach { label -> Text(label, Modifier.weight(1f), textAlign = TextAlign.Center,
                color = ApColors.Navy, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
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
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp)
                            .semantics { contentDescription = description }
                            .clickable { onPick(iso) },
                        color = if (active) ApColors.Primary else Color.White,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("${date.dayOfMonth}", fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
                                color = if (active) Color.White else if (dimmed) ApColors.Navy.copy(alpha = .45f) else ApColors.Navy,
                                fontSize = 15.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (hasLesson) Box(Modifier.size(5.dp).background(if (active) Color.White else ApColors.Primary, RoundedCornerShape(3.dp)))
                                if (hasAppointment) Box(Modifier.size(5.dp).background(if (active) Color.White else ApColors.Pressed, RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text("Pontos indicam aulas e compromissos cadastrados. Toque em um dia para consultar.",
            fontSize = 11.sp, color = ApColors.Navy)
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, lessonDays: Set<String>, appointmentDays: Set<String>, onPick: (String) -> Unit) {
    ApCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            PlanningCalendarPolicy.weekDays(selected).forEachIndexed { index, date ->
                val active = date == selected
                val iso = date.toString()
                Surface(
                    Modifier.weight(1f).heightIn(min = 67.dp)
                        .semantics { contentDescription = "${weekNames[index]}, ${date.dayOfMonth}/${date.monthValue}" }
                        .clickable { onPick(iso) },
                    color = if (active) ApColors.Primary else ApPalette.LightSurface,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(weekNames[index], color = if (active) Color.White else ApColors.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${date.dayOfMonth}", color = if (active) Color.White else ApColors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        if (iso in lessonDays || iso in appointmentDays) Box(Modifier.size(5.dp).background(if (active) Color.White else ApColors.Primary, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

/** Main Planning screen; day is shared with lesson/appointment editors and the SAF backup. */
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
    var mode by rememberSaveable { mutableStateOf("Dia") }
    var backupExpanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalDate.parse(day)
    val ownLessons = data.lessons.filter { it.classroomId == classroom?.id && !it.archived }
    val lessonDays = ownLessons.map { it.date }.toSet()
    val relevantAppointments = data.appointments.filter { it.classroomId == null || it.classroomId == classroom?.id }
    val appointmentDays = relevantAppointments.map { it.date }.toSet()
    ApSectionHeading("Planejamento", classroom?.let { "${it.name} · aulas reais do seu calendário" } ?: "Organize suas turmas e aulas")
    Spacer(Modifier.height(12.dp))
    ApSegmentedControl(listOf("Dia", "Semana", "Mês", "Arquivados"), mode) { mode = it }
    Spacer(Modifier.height(12.dp))
    // Accessible even with zero classes, so a new installation can restore its original data.
    ApRaisedButton(if (backupExpanded) "Ocultar backup e recuperação" else "Backup e recuperação",
        onClick = { backupExpanded = !backupExpanded }, secondary = true)
    if (backupExpanded) {
        Spacer(Modifier.height(10.dp))
        PlanningBackupPanel()
    }
    Spacer(Modifier.height(12.dp))
    if (mode != "Arquivados") {
        DateNavigator(focus, mode, onDay)
        Spacer(Modifier.height(9.dp))
        when (mode) {
            "Mês" -> MonthGrid(focus, lessonDays, appointmentDays, onDay)
            "Semana" -> WeekStrip(focus, lessonDays, appointmentDays, onDay)
        }
        Spacer(Modifier.height(15.dp))
    }
    val visible = data.lessons.filter { lesson ->
        lesson.classroomId == classroom?.id && when (mode) {
            "Arquivados" -> lesson.archived
            else -> !lesson.archived && PlanningCalendarPolicy.inPeriod(lesson.date, focus, mode)
        }
    }.sortedWith(compareBy<Lesson> { it.date }.thenBy { it.time })
    val shownAppointments = if (mode == "Arquivados") emptyList() else relevantAppointments
        .filter { PlanningCalendarPolicy.inPeriod(it.date, focus, mode) }
        .sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
    val caption = when (mode) {
        "Arquivados" -> "Planos arquivados"
        "Semana" -> "Aulas desta semana"
        "Mês" -> "Aulas de ${capitalized(focus.format(monthLabel))}"
        else -> "${capitalized(focus.format(completeDayLabel))} · aulas"
    }
    Text("$caption (${visible.size})", color = ApColors.Navy, fontSize = 19.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(10.dp))
    if (classroom == null) {
        ApCard { Text("Cadastre ou restaure uma turma para começar a planejar.", color = ApColors.Navy) }
        Spacer(Modifier.height(12.dp))
        ApRaisedButton("Gerenciar turmas", onClick = { go("classes") })
        return
    }
    if (visible.isEmpty()) ApCard {
        Text(if (mode == "Arquivados") "Nenhum plano arquivado nesta turma." else "Nenhuma aula planejada para este período.", color = ApColors.Navy)
        if (mode != "Arquivados") Text("Escolha outra data ou adicione uma aula.", color = ApColors.Navy, fontSize = 12.sp)
    }
    visible.forEach { lesson ->
        Spacer(Modifier.height(9.dp))
        ApCard {
            Text("${lesson.time}  ·  ${lesson.subject}  ·  ${lesson.date}", color = ApColors.Pressed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(lesson.title, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 17.sp)
            if (lesson.content.isNotBlank()) Text(lesson.content, color = ApColors.Navy, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            ApRaisedButton(if (lesson.archived) "Restaurar plano" else "Abrir e editar plano",
                onClick = { if (lesson.archived) restoreLesson(lesson) else openLesson(lesson.id) }, secondary = true)
        }
    }
    if (mode != "Arquivados") {
        Spacer(Modifier.height(15.dp))
        Text("Compromissos do período (${shownAppointments.size})", color = ApColors.Navy, fontSize = 19.sp, fontWeight = FontWeight.Black)
        if (shownAppointments.isEmpty()) {
            Spacer(Modifier.height(9.dp))
            ApCard { Text("Nenhum compromisso cadastrado para este período.", color = ApColors.Navy) }
        }
        shownAppointments.forEach { appointment ->
            Spacer(Modifier.height(9.dp))
            ApCard {
                Text("${appointment.date} · ${appointment.time}–${appointment.endTime} · ${appointment.type}",
                    color = ApColors.Pressed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(appointment.title, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 17.sp)
                ApRaisedButton("Abrir agenda", onClick = { onDay(appointment.date); go("agenda") }, secondary = true)
            }
        }
        Spacer(Modifier.height(14.dp))
        ApRaisedButton("Adicionar aula", onClick = { go("newLesson") }, glyph = ApGlyphKind.PLUS)
        Spacer(Modifier.height(9.dp))
        ApRaisedButton("Ver compromissos", onClick = { go("agenda") }, glyph = ApGlyphKind.CALENDAR, secondary = true)
        Spacer(Modifier.height(9.dp))
        ApRaisedButton("Atividades da turma", onClick = { go("activities") }, glyph = ApGlyphKind.DOCUMENT, secondary = true)
    }
}

private data class AgendaEntry(val day: String, val time: String, val title: String, val detail: String,
    val lessonId: Long? = null, val appointmentId: Long? = null)

/** Day/week/month agenda uses the same persisted lessons and appointments, never demo cards. */
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
    ApSectionHeading("Compromissos", "Suas aulas e compromissos organizados por data e horário")
    Spacer(Modifier.height(12.dp))
    ApSegmentedControl(listOf("Dia", "Semana", "Mês"), mode) { mode = it }
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
            add(AgendaEntry(lesson.date, lesson.time, lesson.title, "Aula · $classroomName · ${lesson.subject}", lessonId = lesson.id))
        }
    }.sortedWith(compareBy<AgendaEntry> { it.day }.thenBy { it.time }.thenBy { it.title })
    val periodTitle = when (mode) {
        "Mês" -> "Agenda de ${capitalized(focus.format(monthLabel))}"
        "Semana" -> "Agenda da semana"
        else -> capitalized(focus.format(completeDayLabel))
    }
    Text("$periodTitle (${entries.size})", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 19.sp)
    if (entries.isEmpty()) {
        Spacer(Modifier.height(10.dp))
        ApCard { Text("Nenhum compromisso ou aula neste período.", color = ApColors.Navy) }
    }
    entries.forEach { entry ->
        Spacer(Modifier.height(9.dp))
        ApCard {
            Text("${entry.day} · ${entry.time} · ${entry.detail}", fontSize = 12.sp, color = ApColors.Pressed, fontWeight = FontWeight.Bold)
            Text(entry.title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
            Spacer(Modifier.height(8.dp))
            ApRaisedButton("Abrir e editar", onClick = {
                entry.lessonId?.let(openLesson) ?: entry.appointmentId?.let(openAppointment)
            }, secondary = true)
        }
    }
    Spacer(Modifier.height(14.dp))
    ApRaisedButton("Novo compromisso", onClick = add, glyph = ApGlyphKind.PLUS)
}
