"""Static architectural regression checks; Android instrumentation checks actual DB behavior."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico"


class FileStoreContractTest(unittest.TestCase):
    def test_files_uses_activity_owned_helper(self):
        activity = (SOURCE / "MainActivity.kt").read_text(encoding="utf-8")
        catalog = (SOURCE / "ui/FileCatalogScreen.kt").read_text(encoding="utf-8")
        provider = (SOURCE / "ui/LocalTeacherStore.kt").read_text(encoding="utf-8")
        self.assertIn("CompositionLocalProvider(LocalTeacherStore provides store)", activity)
        self.assertIn("val store = LocalTeacherStore.current", catalog)
        self.assertNotRegex(catalog, r"\bTeacherStore\s*\(")
        self.assertNotIn("onDispose { store.close() }", catalog)
        self.assertIn("staticCompositionLocalOf<TeacherStore>", provider)

    def test_reimport_never_silently_ignores_conflicts(self):
        store = (SOURCE / "data/TeacherStore.kt").read_text(encoding="utf-8")
        policy = (SOURCE / "data/FileImportPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("FileImportPolicy.importReference(writableDatabase, name, uri)", store)
        self.assertNotRegex(store, r"insertWithOnConflict\(\s*\"saved_files\"[^\n]*CONFLICT_IGNORE")
        self.assertIn("db.beginTransaction()", policy)
        self.assertIn("db.setTransactionSuccessful()", policy)
        self.assertIn('putNull("trashed_at")', policy)
        self.assertIn('put("access_state", "available")', policy)
        self.assertNotIn('put("favorite"', policy)
        self.assertNotIn('put("folder_id"', policy)


if __name__ == "__main__":
    unittest.main()
