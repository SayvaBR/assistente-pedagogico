package com.sayvabr.assistentepedagogico.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.Attendance
import com.sayvabr.assistentepedagogico.data.AttendanceSession
import com.sayvabr.assistentepedagogico.data.AttendanceSessionMember
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.Observation
import com.sayvabr.assistentepedagogico.data.Student
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** In-memory synthetic fixtures only: no real student records, database or storage access. */
@RunWith(AndroidJUnit4::class)
class HomeContentPolicyInstrumentedTest {
    private val first = Classroom(1L, "5º Ano A", "Ensino Fundamental", "Matutino")
    private val homonym = Classroom(2L, "5º Ano A", "Ensino Fundamental", "Vespertino")
    private val day = "2026-09-17"

    private fun lesson(id: Long, classroomId: Long, day: String = this.day, archived: Boolean = false) =
        Lesson(id, classroomId, "Aula $id", "Ciências", day, "08:00", "Objetivo sintético", "Conteúdo", "Método", archived)

    private fun observation(id: Long, classroomId: Long) =
        Observation(id, classroomId, null, "Participação", "Registro sintético $id", day, false)

    private fun fixture() = TeacherSnapshot(
        profile = null,
        classrooms = listOf(first, homonym),
        students = listOf(Student(10L, 1L, "Estudante A"), Student(20L, 2L, "Estudante B")),
        lessons = listOf(lesson(1L, 2L), lesson(2L, 1L), lesson(3L, 1L, archived = true)),
        attendance = listOf(
            Attendance(1L, 10L, day, "P"), Attendance(2L, 20L, day, "F"),
            Attendance(1L, 20L, day, "P"), // Corrupt fixture: do not count a foreign student.
        ),
        observations = listOf(observation(999L, 2L), observation(14L, 1L), observation(12L, 1L)),
        appointments = emptyList(), files = emptyList(),
    )

    @Test fun homonymousClassesAreSeparatedByStableId() {
        val data = fixture()
        val own = HomeContentPolicy.forClass(data, first, day)
        val other = HomeContentPolicy.forClass(data, homonym, day)
        assertEquals(2L, own.lesson?.id)
        assertEquals(1L, other.lesson?.id)
        assertEquals(listOf(14L, 12L), own.recentObservations.map { it.id })
        assertEquals(listOf(999L), other.recentObservations.map { it.id })
        assertEquals(1, own.studentCount)
        assertEquals(1, own.attendanceMarked)
        assertEquals(1, other.attendanceMarked)
    }

    @Test fun foreignHistoryNeverMasksAnEmptyOwnHistoryAndArchivedLessonsAreIgnored() {
        val source = fixture()
        val data = source.copy(
            observations = listOf(observation(77L, 2L)),
            lessons = listOf(lesson(33L, 1L, archived = true), lesson(34L, 1L, day = "2026-09-18")),
        )
        val overview = HomeContentPolicy.forClass(data, first, day)
        assertTrue(overview.recentObservations.isEmpty())
        assertNull(overview.lesson)
    }

    @Test fun observationsAreOrderedByStableIdAndLimitedToThree() {
        val data = fixture().copy(observations = listOf(
            observation(5L, 1L), observation(8L, 1L), observation(7L, 1L),
            observation(6L, 1L), observation(100L, 2L),
        ))
        assertEquals(listOf(8L, 7L, 6L), HomeContentPolicy.forClass(data, first, day).recentObservations.map { it.id })
    }

    @Test fun currentClassRosterChangesDoNotRewriteSavedCallSummary() {
        val source = fixture()
        val data = source.copy(
            students = source.students + Student(11L, first.id, "Adicionado depois"),
            attendanceSessions = listOf(AttendanceSession(
                id = 55L,
                classroomId = first.id,
                date = day,
                rosterComplete = true,
                members = listOf(
                    AttendanceSessionMember(10L, "Estudante A", "P"),
                    AttendanceSessionMember(12L, "Estudante removido", "?"),
                ),
            )),
        )
        val overview = HomeContentPolicy.forClass(data, first, day)
        assertEquals(2, overview.studentCount)
        assertEquals(1, overview.attendanceMarked)
        assertEquals(true, overview.attendanceRosterComplete)
    }

    @Test fun archivedOrUnknownClassCannotBeProjected() {
        val archived = first.copy(archived = true)
        val source = fixture().copy(classrooms = listOf(archived, homonym))
        assertFails { HomeContentPolicy.forClass(source, archived, day) }
        assertFails { HomeContentPolicy.forClass(source, first.copy(id = 999L), day) }
        assertFails { HomeContentPolicy.forClass(source, homonym, "invalid date") }
    }

    private inline fun assertFails(action: () -> Unit) {
        var failed = false
        try { action() } catch (_: Exception) { failed = true }
        assertTrue("Out-of-scope dashboard projection must fail closed", failed)
    }
}
