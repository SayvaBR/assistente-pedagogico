"""Protect the integrated lesson-duplication workflow from becoming a disconnected stub."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class LessonDuplicationContractTest(unittest.TestCase):
    def test_duplicate_button_is_connected_to_real_store_operation(self):
        editor = (ROOT / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/LessonEditorV6.kt').read_text()
        app = (ROOT / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt').read_text()
        self.assertIn('Duplicar plano e atividades', editor)
        self.assertIn('confirmDuplicate && duplicate != null', editor)
        self.assertIn('Alterações ainda não salvas nesta tela não serão copiadas.', editor)
        self.assertIn('duplicate = { commit("planning"', app)
        self.assertIn('store.duplicateLesson(lessonClass.id, lesson.id)', app)

    def test_duplication_is_atomic_class_scoped_and_clones_activities(self):
        code = (ROOT / 'app/src/main/java/com/sayvabr/assistentepedagogico/data/LessonPlanDuplication.kt').read_text()
        for contract in (
            'db.beginTransaction()',
            'db.setTransactionSuccessful()',
            'db.endTransaction()',
            'classroom_id=? AND archived=0',
            'id=? AND classroom_id=? AND archived=0',
            'INSERT INTO lesson_activities',
            'WHERE classroom_id=? AND lesson_id=?',
            'put("archived", 0)',
        ):
            with self.subTest(contract=contract):
                self.assertIn(contract, code)

    def test_no_temporary_write_workflow_is_left_in_repository(self):
        self.assertFalse((ROOT / '.github/workflows/one-shot-lesson-duplicate.yml').exists())
        self.assertFalse((ROOT / 'tools/one_shot_lesson_duplicate.py').exists())


if __name__ == '__main__':
    unittest.main()
