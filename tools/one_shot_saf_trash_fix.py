#!/usr/bin/env python3
"""Scoped two-file patch, pinned by Git blob SHA in its self-cleaning workflow."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui'
app_path = ui / 'TeacherApp.kt'
files_path = ui / 'FileCatalogScreen.kt'
app = app_path.read_text(encoding='utf-8')
files = files_path.read_text(encoding='utf-8')

old = '''                        renameFile = { file, newName -> commit("files") { store.renameFile(file.id, newName) } },
                        removeFile = { file -> commit("files") {
                            store.removeFile(file.id)
                            runCatching { context.contentResolver.releasePersistableUriPermission(android.net.Uri.parse(file.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        } })'''
new = '''                        renameFile = { file, newName -> commit("files") { store.renameFile(file.id, newName) } })'''
old_param = '    @Suppress("UNUSED_PARAMETER") removeFile: (SavedFile) -> Unit,\n'
if app.count(old) != 1 or files.count(old_param) != 1:
    raise SystemExit('ABORT: expected SAF legacy callback anchors changed; no files modified')
app = app.replace(old, new, 1)
files = files.replace(old_param, '', 1)
if 'releasePersistableUriPermission' in app or 'removeFile:' in files:
    raise SystemExit('ABORT: stale release callback or parameter remains')
if not all(token in files for token in ('store.trashFile(selected.id)', 'store.restoreFile(selected.id)', 'store.permanentlyRemoveFileReference(selected.id)', 'releasePersistableUriPermission')):
    raise SystemExit('ABORT: actual trash/restore/purge operations changed')
app_path.write_text(app, encoding='utf-8')
files_path.write_text(files, encoding='utf-8')
print('Removed unused permission-releasing legacy callback; only confirmed purge releases SAF permission.')
