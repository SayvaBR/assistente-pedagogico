package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.AttendanceSessionMember
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Observation
import com.sayvabr.assistentepedagogico.data.Student
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Student-scoped view built only from saved records belonging to this classroom. */
@Composable
fun StudentOverviewScreen(
    snapshot: TeacherSnapshot,
    classroom: Classroom,
    student: Student,
    displayName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNewObservation: () -> Unit,
    onAllObservations: () -> Unit,
    onOpenObservation: (Long) -> Unit,
) {
    val sessions = snapshot.attendanceSessions
        .asSequence()
        .filter { it.classroomId == classroom.id }
        .sortedWith(compareByDescending<com.sayvabr.assistentepedagogico.data.AttendanceSession> { it.date }.thenByDescending { it.id })
        .mapNotNull { session -> session.members.firstOrNull { it.studentId == student.id }?.let { session.date to it } }
        .toList()
    val present = sessions.count { it.second.status == "P" }
    val absent = sessions.count { it.second.status == "F" }
    val pending = sessions.count { it.second.status == "?" }
    val partialHistory = snapshot.attendanceSessions.any { it.classroomId == classroom.id && !it.rosterComplete }
    val observations = snapshot.observations
        .filter { it.classroomId == classroom.id && it.studentId == student.id }
        .sortedWith(compareByDescending<Observation> { it.date }.thenByDescending { it.id })

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Voltar à turma" },
            shape = RoundedCornerShape(16.dp),
            color = ApPalette.White,
            border = BorderStroke(1.dp, ApPalette.Outline),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                ApGlyph(ApGlyphKind.BACK, Modifier.size(24.dp), ApPalette.Action)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(classroom.name, color = ApPalette.Action, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(displayName, color = ApPalette.Navy, fontSize = 23.sp, lineHeight = 29.sp,
                fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${classroom.stage} · ${classroom.shift}", color = ApPalette.Navy.copy(alpha = .7f), fontSize = 14.sp)
        }
    }

    Spacer(Modifier.height(16.dp))
    ApRaisedButton("Editar cadastro", onEdit, glyph = ApGlyphKind.EDIT, secondary = true)
    Spacer(Modifier.height(22.dp))

    val attendanceSummary = "Frequência salva: $present ${if (present == 1) "presente" else "presentes"}, " +
        "$absent ${if (absent == 1) "falta" else "faltas"}, $pending ${if (pending == 1) "pendente" else "pendentes"} " +
        "em ${sessions.size} ${if (sessions.size == 1) "chamada" else "chamadas"}"
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = attendanceSummary
    }, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        StudentMetric("$present", "Presentes", Modifier.weight(1f))
        StudentMetric("$absent", "Faltas", Modifier.weight(1f))
        StudentMetric("$pending", "Pendentes", Modifier.weight(1f))
    }
    Text("Em ${sessions.size} chamadas salvas", color = ApPalette.Navy.copy(alpha = .7f), fontSize = 13.sp,
        modifier = Modifier.padding(top = 8.dp))
    if (partialHistory) {
        Spacer(Modifier.height(8.dp))
        ApCard {
            Text("Parte do histórico antigo não preservou a lista completa de alunos.",
                color = ApPalette.Navy.copy(alpha = .7f), fontSize = 13.sp)
        }
    }

    Spacer(Modifier.height(24.dp))
    ApSectionHeading("Frequência recente")
    Spacer(Modifier.height(10.dp))
    if (sessions.isEmpty()) {
        ApCard { Text("Ainda não há chamadas salvas para este aluno.", color = ApPalette.Navy.copy(alpha = .7f)) }
    } else {
        sessions.take(5).forEach { (date, member) ->
            AttendanceRecordRow(date, member)
            Spacer(Modifier.height(8.dp))
        }
        if (sessions.size > 5) {
            Text("Mostrando as 5 chamadas mais recentes.", color = ApPalette.Navy.copy(alpha = .7f), fontSize = 13.sp)
        }
    }

    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        ApSectionHeading("Registros", modifier = Modifier.weight(1f))
        TextButton(onClick = onAllObservations, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Ver todos", color = ApPalette.Action, fontWeight = FontWeight.Bold)
        }
    }
    Text("Observações vinculadas somente a este aluno.", color = ApPalette.Navy.copy(alpha = .7f), fontSize = 13.sp)
    Spacer(Modifier.height(10.dp))
    ApRaisedButton("Novo registro", onNewObservation, glyph = ApGlyphKind.DOCUMENT)
    Spacer(Modifier.height(12.dp))
    if (observations.isEmpty()) {
        ApCard {
            Text("Nenhum registro individual", color = ApPalette.Navy, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Observações sobre a turma inteira não aparecem como registros individuais.",
                color = ApPalette.Navy.copy(alpha = .7f), fontSize = 14.sp)
        }
    } else {
        observations.take(5).forEach { note ->
            ObservationRecordRow(note, onClick = { onOpenObservation(note.id) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StudentMetric(value: String, label: String, modifier: Modifier) {
    Surface(modifier.heightIn(min = 76.dp), shape = RoundedCornerShape(18.dp), color = ApPalette.White,
        border = BorderStroke(1.dp, ApPalette.Outline)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.Center) {
            Text(value, color = ApPalette.Action, fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black)
            Text(label, color = ApPalette.Navy.copy(alpha = .7f), fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AttendanceRecordRow(date: String, member: AttendanceSessionMember) {
    val status = when (member.status) {
        "P" -> "Presente"
        "F" -> "Falta"
        else -> "Pendente"
    }
    ApCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formattedStudentDate(date), color = ApPalette.Navy, fontWeight = FontWeight.ExtraBold)
                Text("Chamada da turma", color = ApPalette.Navy.copy(alpha = .7f), fontSize = 13.sp)
            }
            Text(status, color = if (member.status == "F") Color(0xFFBA3045) else ApPalette.Action,
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ObservationRecordRow(note: Observation, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${note.kind}, ${formattedStudentDate(note.date)}. Abrir registro" },
        shape = RoundedCornerShape(20.dp), color = ApPalette.White,
        border = BorderStroke(1.dp, ApPalette.Outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.kind, color = ApPalette.Action, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(formattedStudentDate(note.date), color = ApPalette.Navy.copy(alpha = .7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(note.body, color = ApPalette.Navy, fontSize = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text("Consultar ou editar", color = ApPalette.Action, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formattedStudentDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR")))
}.getOrDefault(value)
