"""Contract guard for Android SAF: trash must stay reversible without losing URI access."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui'


class SafTrashContractTest(unittest.TestCase):
    def test_only_permanent_purge_releases_uri_permission(self):
        activity = (UI / 'TeacherApp.kt').read_text(encoding='utf-8')
        catalog = (UI / 'FileCatalogScreen.kt').read_text(encoding='utf-8')
        self.assertNotIn('releasePersistableUriPermission', activity)
        self.assertNotIn('removeFile =', activity)
        self.assertNotIn('removeFile: (SavedFile)', catalog)
        self.assertIn('store.trashFile(selected.id)', catalog)
        self.assertIn('store.restoreFile(selected.id)', catalog)
        trash = catalog.split('if (trashOpen && selected != null)', 1)[1].split(
            'if (purgeOpen && selected != null)', 1)[0]
        self.assertNotIn('releasePersistableUriPermission', trash)
        purge = catalog.split('if (purgeOpen && selected != null)', 1)[1].split(
            'if (folderChooser && selected != null)', 1)[0]
        self.assertIn('store.permanentlyRemoveFileReference(selected.id)', purge)
        self.assertIn('releasePersistableUriPermission', purge)
        self.assertLess(purge.index('store.permanentlyRemoveFileReference(selected.id)'),
                        purge.index('releasePersistableUriPermission'))


if __name__ == '__main__':
    unittest.main()
