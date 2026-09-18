"""Fail fast if the real Compose dashboard bypasses its classroom privacy projection."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui"


class HomeScopeContractTest(unittest.TestCase):
    def test_compose_home_consumes_class_scoped_projection(self):
        source = (APP / "TeacherApp.kt").read_text(encoding="utf-8")
        section = source.split("@Composable private fun HomeScreen(", 1)[1].split(
            "@Composable private fun ClassesScreen(", 1
        )[0]
        self.assertIn("HomeContentPolicy.forClass(data, classroom, homeDay)", section)
        self.assertIn("overview.recentObservations.forEach", section)
        self.assertIn("overview.recentObservations.isEmpty()", section)
        self.assertNotIn("data.observations.take(", section)
        self.assertNotIn("data.observations.isEmpty()", section)
        self.assertNotIn("activeClasses.firstOrNull { it.name ==", section)
        self.assertIn("onClick = { select(option.id) }", section)
        self.assertIn("val todayLesson = overview.lesson", section)
        self.assertIn("overview.attendanceMarked", section)

    def test_domain_projection_requires_active_class_and_uses_ids(self):
        projection = (APP / "HomeContentPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("it.id == classroom.id && !it.archived", projection)
        self.assertIn("it.classroomId == classroom.id", projection)
        self.assertIn("recentObservations", projection)
        self.assertIn("it.studentId in studentIds", projection)
        self.assertIn(".take(3)", projection)
        self.assertNotIn("it.name == classroom.name", projection)

    def test_home_actions_reset_stale_dates(self):
        app = (APP / "TeacherApp.kt").read_text(encoding="utf-8")
        self.assertIn('if (destination in setOf("attendance", "newLesson", "planning", "agenda")) selectedDay = today()', app)


if __name__ == "__main__":
    unittest.main()
