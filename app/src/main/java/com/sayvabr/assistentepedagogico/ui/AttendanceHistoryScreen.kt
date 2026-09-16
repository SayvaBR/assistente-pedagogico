package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Only saved attendance entries appear here; an unmarked student remains pending. */
@Composable
fun AttendanceHistoryScreen(
    snapshot: TeacherSnapshot,
    classroom: Classroom,
    back: () -> Unit,
    openDay: (String) -> Unit,
    startAttendance: () -> Unit,
) {
    val students = snapshot.students.filter { it.classroomId == classroom.id }
    val studentIds = students.mapTo(mutableSetOf()) { it.id }
    val sessions = snapshot.attendance
        .filter { it.classroomId == classroom.id && it.studentId in studentIds }
        .groupBy { it.date }
        .toSortedMap(reverseOrder())

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        TextButton(onClick = back) { Text("Voltar", color = ApColors.Primary) }
    }
    Text("Histórico de frequência", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 25.sp)
    Spacer(Modifier.height(5.dp))
    Text(classroom.name, color = ApColors.Navy, fontSize = 14.sp)
    Spacer(Modifier.height(18.dp))

    if (students.isEmpty()) {
        Surface(color = ApColors.White, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Cadastre os alunos primeiro", color = ApColors.Navy, fontWeight = FontWeight.Black)
                Text("O histórico será formado quando você salvar uma chamada.", color = ApColors.Navy)
            }
        }
    } else if (sessions.isEmpty()) {
        Surface(color = ApColors.White, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Nenhuma chamada salva", color = ApColors.Navy, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Faça a chamada da turma para começar o histórico.", color = ApColors.Navy)
                Spacer(Modifier.height(16.dp))
                Button(onClick = startAttendance, colors = ButtonDefaults.buttonColors(containerColor = ApColors.Primary)) {
                    Text("Fazer chamada")
                }
            }
        }
    }

    sessions.forEach { (day, marks) ->
        val present = marks.count { it.status == "P" }
        val absent = marks.count { it.status == "F" }
        val pending = (students.size - marks.map { it.studentId }.distinct().size).coerceAtLeast(0)
        val label = runCatching { LocalDate.parse(day).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(day)
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { openDay(day) },
            shape = RoundedCornerShape(19.dp), color = ApColors.White,
            border = BorderStroke(1.dp, ApColors.Primary.copy(alpha = .22f)),
        ) {
            Column(Modifier.padding(17.dp)) {
                Text(label, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("$present presentes · $absent faltas · $pending pendentes", color = ApColors.Navy, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp))
                Text("Abrir chamada para consultar ou corrigir", color = ApColors.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(14.dp))
    if (students.isNotEmpty()) {
        Button(onClick = startAttendance, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = ApColors.Primary)) {
            Text("Fazer chamada de outro dia", fontWeight = FontWeight.Bold)
        }
    }
}
