"""Static integration guard. Android instrumentation verifies actual SQLite behavior separately."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/data"
UI = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui"
ANDROID = ROOT / "app/src/androidTest/java/com/sayvabr/assistentepedagogico/data"


class LessonStatusV8ContractTest(unittest.TestCase):
    def test_versioned_additive_schema_and_observable_store_snapshot(self):
        status = (DATA / "LessonStatusV8.kt").read_text(encoding="utf-8")
        store = (DATA / "TeacherStore.kt").read_text(encoding="utf-8")
        for state in ('DRAFT("draft"', 'READY("ready"', 'COMPLETED("completed"', 'ARCHIVED("archived"'):
            self.assertIn(state, status)
        self.assertIn('ALTER TABLE lessons ADD COLUMN pedagogical_status', status)
        self.assertIn('ALTER TABLE lessons ADD COLUMN status_before_archive', status)
        self.assertIn('db.beginTransaction()', status)
        self.assertIn('db.setTransactionSuccessful()', status)
        self.assertIn('"pedagogico.db", null, LessonStatusV8.VERSION', store)
        self.assertIn('if (oldVersion < 8) LessonStatusV8.migrate(db)', store)
        self.assertIn('LessonStatusV8.migrate(db)', store)
        self.assertIn('pedagogical_status FROM lessons ORDER BY day,time', store)
        self.assertIn('LessonStatus.parse(c.getString(22))', store)
        self.assertIn('LessonStatusV8.transition(writableDatabase, classroomId, lessonId, target)', store)
        self.assertIn('"closing" to value.closing, "closing_minutes" to value.closingMinutes', store)

    def test_editor_actions_go_through_existing_activity_commit_not_a_local_store(self):
        editor = (UI / "LessonEditorV6.kt").read_text(encoding="utf-8")
        app = (UI / "TeacherApp.kt").read_text(encoding="utf-8")
        planner = (UI / "PlanningWorkspace.kt").read_text(encoding="utf-8")
        self.assertIn('save(editorInput().copy(statusTransition = target))', editor)
        for action in ('Marcar como pronto', 'Marcar como concluído', 'Reabrir como rascunho', 'Reabrir como pronto'):
            self.assertIn(action, editor)
        self.assertIn('fieldsDirty', editor)
        self.assertIn('onDirty()', editor)
        self.assertNotIn('TeacherStore(', editor)
        self.assertIn('store.updateLesson(lessonClass.id, lesson.id, input)', app)
        self.assertIn('operation(); store.read()', app)
        self.assertIn('Estado: ${lesson.status.label}', planner)
        self.assertIn('"Arquivados" -> lesson.archived', planner)

    def test_backup_and_instrumentation_preserve_status_and_archival_origin(self):
        codec = (DATA / "TeacherBackupCodec.kt").read_text(encoding="utf-8")
        export = (DATA / "TeacherBackupExport.kt").read_text(encoding="utf-8")
        restore = (DATA / "TeacherBackupRestore.kt").read_text(encoding="utf-8")
        integrity = (DATA / "TeacherBackupIntegrity.kt").read_text(encoding="utf-8")
        tests = (ANDROID / "LessonStatusV8InstrumentedTest.kt").read_text(encoding="utf-8")
        self.assertIn('pedagogicalStatus', codec)
        self.assertIn('statusBeforeArchive', codec)
        self.assertIn('status_before_archive', export)
        self.assertIn('"pedagogical_status" to status.value', restore)
        self.assertIn('"status_before_archive" to previous.value', restore)
        self.assertIn('status == LessonStatus.ARCHIVED', integrity)
        self.assertIn('db.version = LessonActivityV7.VERSION', tests)
        self.assertIn('backupRestoresDraftReadyCompletedAndArchivedPreviousState', tests)
        self.assertIn('invalidOrSqlFailedTransitionDoesNotPartiallyChangeThePlan', tests)


if __name__ == "__main__":
    unittest.main()
