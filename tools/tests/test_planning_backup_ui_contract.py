"""The emulator tests document I/O; this contract protects the destructive restore UI and stream limits."""
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
        streams = (UI / "PlanningBackupDocumentIo.kt").read_text(encoding="utf-8")
        for part in (
            'ActivityResultContracts.CreateDocument("application/json")',
            "ActivityResultContracts.OpenDocument()",
            "PlanningBackupDocumentIo.write(context.contentResolver, uri, store.exportBackupPayload())",
            "PlanningBackupDocumentIo.read(context.contentResolver, uri)",
            "TeacherBackupRestore.preview(text)",
            "AlertDialog(",
            "Substituir todos os dados deste aparelho?",
            "store.restoreBackupAfterConfirmation(payload, confirmed = true)",
            "activity.recreate()",
            "NÃO copia os PDFs originais",
        ):
            self.assertIn(part, panel)
        self.assertGreater(panel.index("store.restoreBackupAfterConfirmation"), panel.index("confirmButton ="))
        self.assertNotIn("openOrCreateDatabase", panel)
        # Follow the extracted production implementation rather than checking for a buffer in the UI.
        for part in (
            "10 * 1024 * 1024",
            "ByteArrayOutputStream()",
            "openInputStream(uri)",
            'openOutputStream(uri, "wt")',
            "contents.size() + count <= MAX_BYTES",
            "bytes.size <= MAX_BYTES",
        ):
            self.assertIn(part, streams)
        self.assertNotIn("TeacherStore(", streams)
        self.assertNotIn("restoreBackupAfterConfirmation", streams)


if __name__ == "__main__":
    unittest.main()
