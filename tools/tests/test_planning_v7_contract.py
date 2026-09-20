"""Integration regression guard; Android CI additionally compiles and exercises real SQLite."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/data"
UI = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui"
TESTS = ROOT / "app/src/androidTest/java/com/sayvabr/assistentepedagogico/data"


class PlanningActivityV7ContractTest(unittest.TestCase):
    def test_activities_are_migrated_in_primary_database_and_use_shared_store(self):
        store = (DATA / "TeacherStore.kt").read_text(encoding="utf-8")
        self.assertIn('"pedagogico.db", null, AttendanceV10.VERSION', store)
        self.assertIn('LessonActivityV7.migrate(db)', store)
        self.assertIn('if (oldVersion < 7) LessonActivityV7.migrate(db)', store)
        self.assertIn('if (oldVersion < 8) LessonStatusV8.migrate(db)', store)
        self.assertIn('if (oldVersion < 9) PlanLayoutV9.migrate(db)', store)
        self.assertIn('AttendanceV10.migrate(db,', store)
        self.assertIn('fun saveActivity(input: LessonActivityV7.Input)', store)
        self.assertIn('fun listActivities(classroomId: Long', store)
        self.assertIn('fun duplicateActivity(classroomId: Long', store)
        self.assertIn('fun updateActivity(activityId: Long', store)
        self.assertIn('fun deleteActivity(classroomId: Long', store)
        self.assertNotIn('"pedagogico.db", null, LessonPlanV6.VERSION', store)

    def test_full_ui_route_and_real_editor_are_connected(self):
        app = (UI / "TeacherApp.kt").read_text(encoding="utf-8")
        planning = (UI / "PlanningWorkspace.kt").read_text(encoding="utf-8")
        editor = (UI / "PlanningActivitiesScreen.kt").read_text(encoding="utf-8")
        self.assertIn('"activities" -> PlanningActivitiesScreen(', app)
        self.assertIn('store = store,', app)
        self.assertIn('onDirty = { formDirty = it }', app)
        self.assertIn('go("activities")', planning)
        for action in ('store.listActivities(', 'store.saveActivity(', 'store.updateActivity(',
                       'store.duplicateActivity(', 'store.deleteActivity('):
            self.assertIn(action, editor)
        self.assertIn('withContext(Dispatchers.IO)', editor)
        self.assertIn('confirmDelete', editor)
        self.assertNotIn('TeacherStore(', editor)

    def test_legacy_and_fresh_install_have_android_persistence_regressions(self):
        legacy = (TESTS / "TeacherStoreV7MigrationTest.kt").read_text(encoding="utf-8")
        fresh = (TESTS / "TeacherStoreV7FreshInstallTest.kt").read_text(encoding="utf-8")
        self.assertIn('db.version = LessonPlanV6.VERSION', legacy)
        self.assertIn('assertEquals(AttendanceV10.VERSION', legacy)
        self.assertIn('first.saveActivity(', legacy)
        self.assertIn('reopened.listActivities(', legacy)
        self.assertIn('first.duplicateActivity(', fresh)
        self.assertIn('first.updateActivity(', fresh)
        self.assertIn('first.deleteActivity(', fresh)
        self.assertFalse((ROOT / '.github/workflows/one-shot-activity-store-v7.yml').exists())
        self.assertFalse((ROOT / '.github/workflows/one-shot-wire-activity-ui-v7.yml').exists())
        self.assertFalse((ROOT / 'tools/_once_integrate_activity_store_v7.py').exists())
        self.assertFalse((ROOT / 'tools/_once_wire_activity_ui_v7.py').exists())


if __name__ == "__main__":
    unittest.main()
