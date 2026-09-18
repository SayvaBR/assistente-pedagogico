#!/usr/bin/env python3
"""Apply one scoped integration only to pinned source blobs; workflow removes this script."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
data_path = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/data/TeacherStore.kt'
app_path = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt'
calendar_path = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/PlanningWorkspace.kt'
data = data_path.read_text(encoding='utf-8')
app = app_path.read_text(encoding='utf-8')
calendar = calendar_path.read_text(encoding='utf-8')


def replace_once(source, old, new, path):
    count = source.count(old)
    if count != 1:
        raise SystemExit(f'ABORT {path}: expected one exact anchor, got {count}: {old[:90]!r}')
    return source.replace(old, new, 1)


data = replace_once(data,
    'data class Appointment(val id: Long, val title: String, val date: String, val time: String)',
    'data class Appointment(val id: Long, val title: String, val date: String, val time: String,\n'
    '    val endTime: String = time, val type: String = "Outro", val classroomId: Long? = null)', 'TeacherStore Appointment')
data = replace_once(data,
    'SQLiteOpenHelper(context.applicationContext, "pedagogico.db", null, FileLibraryV4.VERSION)',
    'SQLiteOpenHelper(context.applicationContext, "pedagogico.db", null, AppointmentV5.VERSION)', 'TeacherStore version')
data = replace_once(data,
    '        FileLibraryV4.migrate(db)\n    }\n\n    /** SQLiteOpenHelper',
    '        FileLibraryV4.migrate(db)\n        AppointmentV5.migrate(db)\n    }\n\n    /** SQLiteOpenHelper', 'TeacherStore onCreate')
data = replace_once(data,
    'require(oldVersion in 1..3 && newVersion == FileLibraryV4.VERSION)',
    'require(oldVersion in 1..4 && newVersion == AppointmentV5.VERSION)', 'TeacherStore onUpgrade gate')
data = replace_once(data,
    '        if (oldVersion < 4) FileLibraryV4.migrate(db)\n    }',
    '        if (oldVersion < 4) FileLibraryV4.migrate(db)\n'
    '        if (oldVersion < 5) AppointmentV5.migrate(db)\n    }', 'TeacherStore cumulative upgrade')
data = replace_once(data,
    'db.rawQuery("SELECT id,title,day,time FROM appointments ORDER BY day,time", null).use { c -> while (c.moveToNext()) appointments += Appointment(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)) }',
    'db.rawQuery("SELECT id,title,day,time,end_time,type,classroom_id FROM appointments ORDER BY day,time", null).use { c ->\n'
    '            while (c.moveToNext()) appointments += Appointment(c.getLong(0), c.getString(1), c.getString(2), c.getString(3),\n'
    '                if (c.isNull(4)) c.getString(3) else c.getString(4), c.getString(5), if (c.isNull(6)) null else c.getLong(6))\n'
    '        }', 'TeacherStore read')
data = replace_once(data,
    '    fun addAppointment(title: String, day: String, time: String) {',
    '    /** v5 writes are validated, scoped, and transactional; callers use Activity-owned helper. */\n'
    '    fun addAppointment(input: AppointmentV5.Input): Long = AppointmentV5.create(writableDatabase, input)\n'
    '    fun updateAppointment(appointmentId: Long, input: AppointmentV5.Input) = AppointmentV5.update(writableDatabase, appointmentId, input)\n\n'
    '    /** Legacy API retained for binary/source compatibility with earlier tests. */\n'
    '    fun addAppointment(title: String, day: String, time: String) {', 'TeacherStore API')

old_new = '''"newAppointment" -> AppointmentForm(selectedDay, { requestBack() }, { title, day, time ->
                        commit("agenda", onSuccess = { selectedDay = day }) { store.addAppointment(title, day, time) }
                    }, onDirty = { formDirty = true })
                    "editAppointment" -> snapshot.appointments.firstOrNull { it.id == selectedAppointment }?.let { appointment ->
                        AppointmentForm(appointment.date, { requestBack() }, { title, day, time ->
                            commit("agenda", onSuccess = { selectedDay = day }) { store.updateAppointment(appointment.id, title, day, time) }
                        }, initial = appointment, delete = { commit("agenda") { store.deleteAppointment(appointment.id) } },
                            onDirty = { formDirty = true })
                    }'''
replacement = '''"newAppointment" -> AppointmentEditorV5(initialDay = selectedDay, classes = snapshot.classrooms,
                        back = { requestBack() }, save = { input ->
                            commit("agenda", onSuccess = { selectedDay = input.day }) { store.addAppointment(input) }
                        }, onDirty = { formDirty = true })
                    "editAppointment" -> snapshot.appointments.firstOrNull { it.id == selectedAppointment }?.let { appointment ->
                        AppointmentEditorV5(initialDay = appointment.date, classes = snapshot.classrooms,
                            initial = appointment, back = { requestBack() }, save = { input ->
                                commit("agenda", onSuccess = { selectedDay = input.day }) {
                                    store.updateAppointment(appointment.id, input)
                                }
                            }, delete = { commit("agenda") { store.deleteAppointment(appointment.id) } },
                            onDirty = { formDirty = true })
                    }'''
app = replace_once(app, old_new, replacement, 'TeacherApp v5 routing')
start = '@Composable private fun AppointmentForm(initialDay: String, back: () -> Unit, save: (String, String, String) -> Unit,'
end = '@Composable private fun MoreScreen(data: TeacherSnapshot, go: (String) -> Unit) {'
if app.count(start) != 1 or app.count(end) != 1 or app.index(start) > app.index(end):
    raise SystemExit('ABORT: cannot isolate legacy AppointmentForm')
app = app[:app.index(start)] + app[app.index(end):]
calendar = replace_once(calendar,
    '            add(AgendaEntry(appointment.date, appointment.time, appointment.title, "Compromisso", appointmentId = appointment.id))',
    '            val room = appointment.classroomId?.let { id -> data.classrooms.firstOrNull { it.id == id }?.name ?: "Turma indisponível" } ?: "Geral"\n'
    '            val detail = "${appointment.type} · ${appointment.time}–${appointment.endTime} · $room"\n'
    '            add(AgendaEntry(appointment.date, appointment.time, appointment.title, detail, appointmentId = appointment.id))', 'Agenda details')

for anchor in ('AppointmentV5.migrate(db)', 'fun addAppointment(input: AppointmentV5.Input)', 'fun updateAppointment(appointmentId: Long, input: AppointmentV5.Input)'):
    if anchor not in data:
        raise SystemExit(f'ABORT: missing store anchor {anchor}')
for anchor in ('"newAppointment" -> AppointmentEditorV5(', '"editAppointment" ->', '"newLesson" ->', '"editLesson" ->'):
    if anchor not in app:
        raise SystemExit(f'ABORT: missing navigation anchor {anchor}')
if 'private fun AppointmentForm(' in app:
    raise SystemExit('ABORT: legacy form not removed')

for path, content in ((data_path, data), (app_path, app), (calendar_path, calendar)):
    path.write_text(content, encoding='utf-8')
print('Connected v5 migration, read/write and real editor; existing lessons/files/home untouched.')
