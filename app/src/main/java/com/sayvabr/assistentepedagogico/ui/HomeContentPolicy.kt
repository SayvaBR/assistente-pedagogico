package com.sayvabr.assistentepedagogico.ui

import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.Observation
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import java.time.LocalDate

/** A dashboard projection must never read another classroom's pedagogical records. */
internal data class HomeOverview(
    val lesson: Lesson?,
    val recentObservations: List<Observation>,
    val studentCount: Int,
    val attendanceMarked: Int,
)

/** Pure, testable selection policy: classroom identity is its ID, never its display name. */
internal object HomeContentPolicy {
    fun forClass(data: TeacherSnapshot, classroom: Classroom, day: String): HomeOverview {
        LocalDate.parse(day)
        require(data.classrooms.any { it.id == classroom.id && !it.archived }) {
            "A turma precisa estar ativa para aparecer na página inicial."
        }
        val students = data.students.filter { it.classroomId == classroom.id }
        val studentIds = students.map { it.id }.toSet()
        return HomeOverview(
            lesson = data.lessons.asSequence()
                .filter { it.classroomId == classroom.id && it.date == day && !it.archived }
                .minWithOrNull(compareBy<Lesson> { it.time }.thenBy { it.id }),
            recentObservations = data.observations.asSequence()
                .filter { it.classroomId == classroom.id }
                .sortedByDescending { it.id }
                .take(3)
                .toList(),
            studentCount = students.size,
            attendanceMarked = data.attendance.count {
                it.classroomId == classroom.id && it.date == day && it.studentId in studentIds &&
                    (it.status == "P" || it.status == "F")
            },
        )
    }
}
