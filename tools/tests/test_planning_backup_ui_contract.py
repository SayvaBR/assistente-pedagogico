"""The Android emulator tests data round-trips; this contract prevents destructive UI rewiring."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui"


class PlanningBackupUiContract(unittest.TestCase):
    def test_backup_accessible_without_a_class_and_uses_single_store(self):
        workspace = (UI / "PlanningWorkspace.kt").read_text(encoding="utf-8")
        panel = (UI / "PlanningBackupPanel.kt").read_text(encoding="utf-8")
        self.assertIn("PlanningBackupPanel()", workspace)
        self.assertLess(workspace.index("PlanningBackupPanel()"), workspace.index("if (classroom == null)"))
        self.assertIn("LocalTeacherStore.current", panel)
        self.assertNotIn("TeacherStore(", panel)

    def test_destructive_restore_requires_user_dialog_and_preview(self):
        panel = (UI / "PlanningBackupPanel.kt").read_text(encoding="utf-8")
        for part in (
            'ActivityResultContracts.CreateDocument("application/json")',
            "ActivityResultContracts.OpenDocument()",
            "TeacherBackupRestore.preview(text)",
            "AlertDialog(",
            "Substituir todos os dados deste aparelho?",
            "store.restoreBackupAfterConfirmation(payload, confirmed = true)",
            "activity.recreate()",
            "NÃO copia os PDFs originais",
            "ByteArrayOutputStream()",
            "10 * 1024 * 1024",
        ):
            self.assertIn(part, panel)
        self.assertGreater(panel.index("store.restoreBackupAfterConfirmation"), panel.index("confirmButton ="))
        self.assertNotIn("openOrCreateDatabase", panel)


if __name__ == "__main__":
    unittest.main()
