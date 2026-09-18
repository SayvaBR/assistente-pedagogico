"""Static regression guard for the integrated Planning UI; Kotlin/Android CI checks runtime compilation."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui'


class PlanningContractTest(unittest.TestCase):
    def test_both_calendar_screens_are_wired_to_real_routes(self):
        app = (UI / 'TeacherApp.kt').read_text(encoding='utf-8')
        workspace = (UI / 'PlanningWorkspace.kt').read_text(encoding='utf-8')
        kit = (UI / 'PlanningVisualKit.kt').read_text(encoding='utf-8')
        self.assertIn('PlanningWorkspace(data, classroom, day, onDay, go, openLesson, restoreLesson)', app)
        self.assertIn('PlanningAgendaScreen(data, day, onDay, add, open, openLesson)', app)
        self.assertIn('selectedLesson = lessonId; requestNavigate("editLesson")', app)
        for route in ('newLesson', 'editLesson', 'newAppointment', 'editAppointment'):
            self.assertIn(f'"{route}" ->', app)
        self.assertIn('data.lessons.filter', workspace)
        self.assertIn('data.appointments.filter', workspace)
        self.assertIn('MonthGrid(focus, lessonDays, appointmentDays, onDay)', workspace)
        self.assertGreaterEqual(workspace.count('PlanningCalendarPolicy.inPeriod('), 3)
        # The monthly list uses the whole month for discovery, but highlights the selected day.
        self.assertIn('"Mês" -> if (showWholeMonth) "Aulas do mês"', workspace)
        self.assertIn('inPeriod.filter { it.date == day }', workspace)
        self.assertIn('"Mês" -> "Agenda de', workspace)
        self.assertIn('shownAppointments.forEach', workspace)
        self.assertIn('PlanningLessonTile(lesson', workspace)
        self.assertIn('onOpen: () -> Unit', kit)
        self.assertNotIn('1000273178', workspace)  # Reference image is design input, never demo data.

    def test_old_patch_workflow_removed_and_date_rules_have_tests(self):
        self.assertFalse((ROOT / '.github/workflows/one-shot-planning-integration.yml').exists())
        self.assertFalse((ROOT / 'tools/one_shot_planning_integration.py').exists())
        policy = (UI / 'PlanningCalendarPolicy.kt').read_text(encoding='utf-8')
        unit = (ROOT / 'app/src/test/java/com/sayvabr/assistentepedagogico/ui/PlanningCalendarPolicyTest.kt').read_text(encoding='utf-8')
        self.assertIn('fun monthCells', policy)
        self.assertIn('fun shiftMonth', policy)
        self.assertIn('fun inPeriod', policy)
        self.assertIn('"Mês" -> matchesMonth', policy)
        self.assertIn('leapFebruaryHasAllDaysAndMondayFirstCells', unit)
        self.assertIn('monthsThatNeedSixRowsDoNotLoseDates', unit)
        self.assertIn('monthListingIncludesEntireSelectedMonthButNeverAdjacentCalendarCells', unit)


if __name__ == '__main__':
    unittest.main()
