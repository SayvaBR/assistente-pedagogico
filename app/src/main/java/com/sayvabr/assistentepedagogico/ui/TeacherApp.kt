package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ink = ApColors.Navy
private val blue = ApColors.Primary
private val canvas = ApColors.Sky
private val pale = ApPalette.LightSurface
private val outline = ApPalette.Outline
private val green = ApColors.Pressed
private val red = ApColors.Navy
private val brDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))
private fun friendlyDay(day: String) = LocalDate.parse(day).format(brDate).replaceFirstChar { it.uppercase() }
private fun today() = LocalDate.now().toString()
private fun studentLabel(allStudents: List<Student>, student: Student): String =
    if (allStudents.count { it.classroomId == student.classroomId && it.name.equals(student.name, ignoreCase = true) } > 1) {
        "${student.name} · cadastro ${student.id}"
    } else student.name

/** Working offline product slice; navigation points only to implemented screens. */
@Composable
fun TeacherApp(store: TeacherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<TeacherSnapshot?>(null) }
    var screen by rememberSaveable { mutableStateOf("home") }
    var tab by rememberSaveable { mutableStateOf("Início") }
    var selectedClass by rememberSaveable { mutableLongStateOf(-1L) }
    var classDetailSection by rememberSaveable { mutableStateOf("Visão do dia") }
    var classStudentSearch by rememberSaveable { mutableStateOf("") }
    var selectedStudent by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedDay by rememberSaveable { mutableStateOf(today()) }
    var selectedAppointment by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedLesson by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedObservation by rememberSaveable { mutableLongStateOf(-1L) }
    var backStack by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var confirmExit by remember { mutableStateOf(false) }
    var formDirty by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingDiscardAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val screenScrollState = rememberSaveable(screen, classDetailSection, saver = ScrollState.Saver) { ScrollState(0) }

    val rootDestinations = setOf("home", "planning", "classes", "files", "more")
    fun synchronizeTab(destination: String) {
        tab = when (destination) {
            "home" -> "Início"
            "planning" -> "Planejamento"
            "classes" -> "Turmas"
            "files" -> "Arquivos"
            "more" -> "Mais"
            else -> tab
        }
    }
    fun navigate(destination: String, root: Boolean = false) {
        if (destination == screen && !root) return
        backStack = if (root || destination in rootDestinations) arrayListOf()
            else ArrayList(backStack + screen)
        screen = destination
        synchronizeTab(destination)
    }
    fun back() {
        when {
            confirmExit -> confirmExit = false
            error != null -> error = null
            busy -> Unit
            backStack.isNotEmpty() -> {
                screen = backStack.last()
                backStack = ArrayList(backStack.dropLast(1))
                synchronizeTab(screen)
            }
            screen != "home" -> navigate("home", root = true)
            else -> confirmExit = true
        }
    }
    fun guardDiscard(action: () -> Unit) {
        if (formDirty) {
            pendingDiscardAction = action
            confirmDiscard = true
        } else action()
    }
    fun requestBack() = guardDiscard { back() }
    fun requestNavigate(destination: String, root: Boolean = false) = guardDiscard { navigate(destination, root) }
    fun requestDay(day: String) = guardDiscard { selectedDay = day }
    fun returnToPreviousScreen(defaultDestination: String) = backStack.lastOrNull() ?: defaultDestination

    LaunchedEffect(screen) {
        formDirty = false
        confirmDiscard = false
        pendingDiscardAction = null
    }

    fun afterSave(destination: String) {
        when {
            destination == "createClass" -> {
                backStack = arrayListOf("home")
                screen = destination
            }
            destination in rootDestinations -> navigate(destination, root = true)
            backStack.lastOrNull() == destination -> {
                screen = destination
                backStack = ArrayList(backStack.dropLast(1))
                synchronizeTab(destination)
            }
            destination == "classDetail" -> {
                backStack = arrayListOf("classes")
                screen = destination
                synchronizeTab("classes")
            }
            else -> navigate(destination)
        }
    }

    LaunchedEffect(store) {
        try { data = withContext(Dispatchers.IO) { store.read() } }
        catch (e: Exception) { error = e.message ?: "Não foi possível abrir seus dados." }
    }

    fun commit(next: String? = null, onSuccess: (() -> Unit)? = null, operation: () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                val fresh = withContext(Dispatchers.IO) { operation(); store.read() }
                data = fresh
                formDirty = false
                onSuccess?.invoke()
                if (next != null) afterSave(next)
            } catch (e: Exception) { error = e.message ?: "Não foi possível salvar. Tente novamente." }
            finally { busy = false }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: (uri.lastPathSegment ?: "Arquivo")
                commit("files") { store.addFile(fileName, uri.toString()) }
            } catch (e: Exception) { error = "Não foi possível importar: ${e.message}" }
        }
    }

    val snapshot = data
    BackHandler(enabled = snapshot?.profile != null) { requestBack() }
    if (snapshot == null) {
        Box(Modifier.fillMaxSize().background(canvas), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ApIconBadge(ApGlyphKind.DOCUMENT, Modifier.size(72.dp), emphasized = true)
                Spacer(Modifier.height(ApSpace.Base))
                Text("Assistente Pedagógico", fontSize = 25.sp, fontWeight = FontWeight.Black, color = ink)
                Spacer(Modifier.height(20.dp))
                if (error == null) CircularProgressIndicator(color = blue) else Text(error!!, color = red)
            }
        }
        return
    }
    val currentClass = snapshot.classrooms.firstOrNull { it.id == selectedClass && !it.archived }
        ?: snapshot.classrooms.firstOrNull { !it.archived }
    if (snapshot.profile == null) {
        FirstRun(onFinish = { name -> commit("createClass") { store.saveProfile(name) } })
        if (busy) SavingOverlay()
        error?.let { ErrorOverlay(it) { error = null } }
        return
    }

    Column(Modifier.fillMaxSize().background(canvas)) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(screenScrollState).padding(horizontal = ApSpace.Base)) {
                Spacer(Modifier.height(18.dp))
                when (screen) {
                    "home" -> HomeScreen(snapshot, currentClass, { destination ->
                        if (destination in setOf("attendance", "newLesson", "planning", "agenda")) selectedDay = today()
                        requestNavigate(destination)
                    }, { selectedClass = it })
                    "classes" -> ClassesScreen(snapshot, currentClass,
                        onPick = { selectedClass = it; classDetailSection = "Visão do dia"; classStudentSearch = ""; requestNavigate("classDetail") },
                        onAdd = { requestNavigate("createClass") },
                        onRestore = { id -> commit("classes") { store.setClassArchived(id, false) } })
                    "createClass" -> ClassForm(
                        onBack = { requestBack() },
                        onSave = { name, stage, shift -> commit("classes") { store.createClass(name, stage, shift) } },
                        onDirty = { formDirty = true })
                    "editClass" -> if (currentClass != null) ClassForm(
                        onBack = { requestBack() },
                        onSave = { name, stage, shift -> commit("classDetail") { store.updateClass(currentClass.id, name, stage, shift) } },
                        initial = currentClass,
                        onDirty = { formDirty = true })
                    "archiveClass" -> if (currentClass != null) ArchiveClassScreen(currentClass,
                        back = { back() }, archive = { commit("classes") { store.setClassArchived(currentClass.id, true) } })
                    "classDetail" -> if (currentClass != null) ClassDetail(snapshot, currentClass,
                        section = classDetailSection,
                        onSectionChange = { classDetailSection = it },
                        studentSearch = classStudentSearch,
                        onStudentSearchChange = { classStudentSearch = it },
                        back = { requestBack() },
                        go = {
                            classDetailSection = when (it) {
                                "attendance", "attendanceHistory" -> "Frequência"
                                "addStudent", "addStudents", "editStudent" -> "Alunos"
                                "observation", "observationHistory", "editObservation" -> "Registros"
                                else -> "Visão do dia"
                            }
                            if (it == "attendance") selectedDay = today()
                            requestNavigate(it)
                        },
                        onStudent = { selectedStudent = it; classDetailSection = "Alunos"; requestNavigate("editStudent") },
                        onObservation = { selectedObservation = it; classDetailSection = "Registros"; requestNavigate("editObservation") })
                    "editStudent" -> if (currentClass != null) snapshot.students.firstOrNull {
                        it.id == selectedStudent && it.classroomId == currentClass.id
                    }?.let { student -> StudentEditForm(student, currentClass, displayName = studentLabel(snapshot.students, student),
                        back = { requestBack() },
                        save = { name -> commit("classDetail") { store.updateStudent(currentClass.id, student.id, name) } },
                        delete = { commit("classDetail") { store.deleteStudent(currentClass.id, student.id) } },
                        onDirty = { formDirty = true }) }
                    "addStudent" -> if (currentClass != null) StudentForm(currentClass, { requestBack() },
                        { name -> commit(returnToPreviousScreen("classDetail")) { store.addStudent(currentClass.id, name) } }, onDirty = { formDirty = true })
                    "addStudents" -> if (currentClass != null) StudentRosterImportForm(currentClass, { requestBack() },
                        { names -> commit(returnToPreviousScreen("classDetail")) { store.addStudents(currentClass.id, names) } },
                        onDirty = { formDirty = true })
                    "attendanceHistory" -> if (currentClass != null) AttendanceHistoryScreen(
                        snapshot, currentClass, back = { back() },
                        openDay = { day -> selectedDay = day; navigate("attendance") },
                        startAttendance = { selectedDay = today(); navigate("attendance") })
                    "attendance" -> if (currentClass != null) AttendanceEditor(
                        snapshot, currentClass, selectedDay,
                        onDay = { requestDay(it) }, onBack = { requestBack() },
                        onAddStudent = { requestNavigate("addStudent") },
                        onSave = { marks -> commit(returnToPreviousScreen("classDetail")) { store.saveAttendance(currentClass.id, selectedDay, marks) } },
                        onDirty = { formDirty = true })
                    "observationHistory" -> if (currentClass != null) ObservationHistoryScreen(
                        snapshot, currentClass, onBack = { back() },
                        onNew = { requestNavigate("observation") },
                        onOpen = { selectedObservation = it; requestNavigate("editObservation") })
                    "observation" -> if (currentClass != null) ObservationForm(snapshot, currentClass, { requestBack() },
                        { studentId, kind, body, share -> commit(returnToPreviousScreen("classDetail")) {
                            store.addObservation(currentClass.id, studentId, kind, body, share)
                        } }, onDirty = { formDirty = true })
                    "editObservation" -> if (currentClass != null) snapshot.observations.firstOrNull {
                        it.id == selectedObservation && it.classroomId == currentClass.id
                    }?.let { observation ->
                        ObservationForm(snapshot, currentClass, { requestBack() }, { studentId, kind, body, share ->
                            commit(returnToPreviousScreen("classDetail")) {
                                store.updateObservation(currentClass.id, observation.id, studentId, kind, body, share)
                            }
                        }, initial = observation,
                            delete = { commit(returnToPreviousScreen("classDetail")) {
                                store.deleteObservation(currentClass.id, observation.id)
                            } }, onDirty = { formDirty = true })
                    }
                    "planning" -> PlanningScreen(snapshot, currentClass, selectedDay, { selectedDay = it }, { requestNavigate(it) },
                        openLesson = { lessonId -> selectedLesson = lessonId; requestNavigate("editLesson") },
                        restoreLesson = { lesson -> commit("planning") { store.setLessonArchived(lesson.classroomId, lesson.id, false) } })
                    "activities" -> PlanningActivitiesScreen(
                        store = store,
                        classroom = currentClass,
                        lessons = snapshot.lessons,
                        back = { requestBack() },
                        onDirty = { formDirty = it },
                    )
                    "newLesson" -> if (currentClass != null) LessonEditorV6(
                        classroom = currentClass, initialDay = selectedDay, back = { requestBack() },
                        save = { input -> commit("planning", onSuccess = { selectedDay = input.day }) {
                            store.saveLesson(input, currentClass.id)
                        } }, onDirty = { formDirty = true })
                    "editLesson" -> snapshot.lessons.firstOrNull { it.id == selectedLesson && !it.archived }?.let { lesson ->
                        snapshot.classrooms.firstOrNull { it.id == lesson.classroomId && !it.archived }?.let { lessonClass ->
                            LessonEditorV6(
                                classroom = lessonClass, initialDay = lesson.date, initial = lesson, back = { requestBack() },
                                save = { input -> commit("planning", onSuccess = { selectedDay = input.day }) {
                                    store.updateLesson(lessonClass.id, lesson.id, input)
                                } },
                                archive = { commit("planning") { store.setLessonArchived(lessonClass.id, lesson.id, true) } },
                                duplicate = { commit("planning", onSuccess = { selectedDay = lesson.date }) {
                                    store.duplicateLesson(lessonClass.id, lesson.id)
                                } },
                                onDirty = { formDirty = true })
                        }
                    } ?: Panel { Text("Plano indisponível. Retorne ao Planejamento.", color = ink); PrimaryButton("Voltar ao planejamento") { navigate("planning", root = true) } }
                    "agenda" -> AgendaScreen(snapshot, selectedDay, { selectedDay = it }, { requestNavigate("newAppointment") }, { appointmentId ->
                        selectedAppointment = appointmentId; requestNavigate("editAppointment")
                    }, { lessonId -> selectedLesson = lessonId; requestNavigate("editLesson") })
                    "newAppointment" -> AppointmentEditorV5(initialDay = selectedDay, classes = snapshot.classrooms,
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
                    }
                    "files" -> FileCatalogScreen(
                        files = snapshot.files,
                        importFile = { filePicker.launch(arrayOf("*/*")) },
                        openFile = { file ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(file.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (e: Exception) { error = "Não foi possível abrir este arquivo. Verifique se ele ainda existe e se há um aplicativo compatível." }
                        },
                        renameFile = { file, newName -> commit("files") { store.renameFile(file.id, newName) } })
                    "more" -> MoreScreen(snapshot, { requestNavigate(it) })
                    "profile" -> ProfileForm(snapshot.profile.name, { requestBack() },
                        { name -> commit("more") { store.saveProfile(name) } }, onDirty = { formDirty = true })
                    else -> HomeScreen(snapshot, currentClass, { requestNavigate(it) }, { selectedClass = it })
                }
                Spacer(Modifier.height(ApSpace.Xl))
            }
        }
        if (screen !in listOf("createClass", "editClass", "archiveClass", "addStudent", "addStudents", "editStudent", "newLesson", "editLesson", "observation", "editObservation", "newAppointment", "editAppointment", "profile")) {
            BottomBar(tab) { destination ->
                requestNavigate(when (destination) {
                    "Início" -> "home"; "Planejamento" -> "planning"; "Turmas" -> "classes"; "Arquivos" -> "files"; else -> "more"
                }, root = true)
            }
        }
    }
    if (busy) SavingOverlay()
    error?.let { ErrorOverlay(it) { error = null } }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("Sair do Assistente Pedagógico?") },
        text = { Text("Seus registros já salvos permanecem neste aparelho. Para sair, confirme abaixo.") },
        confirmButton = { TextButton(onClick = { confirmExit = false; (context as? android.app.Activity)?.finish() }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Continuar no app") } })
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false; pendingDiscardAction = null },
        title = { Text("Descartar alterações?") },
        text = { Text("Há informações nesta tela que ainda não foram salvas. Se continuar, essas alterações serão perdidas.") },
        confirmButton = { TextButton(onClick = {
            val action = pendingDiscardAction
            pendingDiscardAction = null
            confirmDiscard = false
            formDirty = false
            action?.invoke()
        }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false; pendingDiscardAction = null }) { Text("Continuar editando") } })
}

@Composable private fun SavingOverlay() {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(ApShapeToken.Card), color = Color.White) {
            Row(Modifier.padding(ApSpace.Xl), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(25.dp), color = blue)
                Spacer(Modifier.width(14.dp)); Text("Salvando seus dados...", color = ink)
            }
        }
    }
}
@Composable private fun ErrorOverlay(message: String, dismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = dismiss), contentAlignment = Alignment.Center) {
        Panel {
            Text("Não foi possível concluir", fontWeight = FontWeight.Black, color = ink, fontSize = 21.sp)
            Spacer(Modifier.height(8.dp)); Text(message, color = red)
            Spacer(Modifier.height(18.dp)); PrimaryButton("Entendi", click = dismiss)
        }
    }
}

@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) = ApCard(content = content)

@Composable private fun PrimaryButton(label: String, secondary: Boolean = false, enabled: Boolean = true, click: () -> Unit) {
    ApRaisedButton(label = label, onClick = click, secondary = secondary, enabled = enabled)
}

@Composable private fun Heading(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) {
            Surface(
                modifier = Modifier.size(48.dp)
                    .clickable(role = Role.Button, onClick = back)
                    .semantics { contentDescription = "Voltar" },
                color = ApColors.White,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, outline),
            ) { Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(24.dp), blue) } }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = ink, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
            if (subtitle != null) Text(subtitle, color = ink.copy(alpha = .72f), fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(20.dp))
}
@Composable private fun Subtitle(label: String) { Text(label, color = ink, fontSize = 20.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)) }
@Composable private fun Input(label: String, value: String, change: (String) -> Unit, multiline: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        shape = RoundedCornerShape(ApShapeToken.Medium), minLines = if (multiline) 3 else 1, maxLines = if (multiline) 6 else 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = blue, unfocusedBorderColor = outline, focusedLabelColor = blue,
            focusedContainerColor = ApColors.White, unfocusedContainerColor = ApColors.White))
    Spacer(Modifier.height(10.dp))
}
@Composable private fun Choices(options: List<String>, current: String, columns: Int = 3, pick: (String) -> Unit) {
    options.chunked(columns).forEach { chunk ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chunk.forEach { option ->
                val chosen = option == current
                Surface(modifier = Modifier.weight(1f).heightIn(min = ApSizeToken.MinTouchTarget)
                    .selectable(selected = chosen, role = Role.RadioButton) { pick(option) },
                    shape = RoundedCornerShape(13.dp), color = if (chosen) blue else Color.White,
                    border = BorderStroke(1.dp, if (chosen) blue else outline)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, Modifier.padding(horizontal = 6.dp, vertical = 12.dp), textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold, color = if (chosen) Color.White else ink, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
@Composable private fun NavItem(label: String, glyph: ApGlyphKind, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    Column(modifier.heightIn(min = 64.dp).clickable(onClick = click).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ApGlyph(glyph, Modifier.size(25.dp), if (selected) blue else ink.copy(alpha = .48f))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) blue else ink.copy(alpha = .62f), maxLines = 1)
    }
}
@Composable private fun BottomBar(selected: String, pick: (String) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), border = BorderStroke(1.dp, outline)) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                Triple("Início", ApGlyphKind.HOME, selected == "Início"),
                Triple("Planejamento", ApGlyphKind.CALENDAR, selected == "Planejamento"),
                Triple("Turmas", ApGlyphKind.USERS, selected == "Turmas"),
                Triple("Arquivos", ApGlyphKind.FOLDER, selected == "Arquivos"),
                Triple("Mais", ApGlyphKind.MORE, selected == "Mais"),
            ).forEach { (label, glyph, active) -> NavItem(label, glyph, active, Modifier.weight(1f)) { pick(label) } }
        }
    }
}
@Composable private fun ActionTile(glyph: ApGlyphKind, title: String, detail: String = "", click: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = click), shape = RoundedCornerShape(ApShapeToken.Card),
        color = Color.White, border = BorderStroke(1.dp, outline)) {
        Row(Modifier.padding(ApSpace.Base), verticalAlignment = Alignment.CenterVertically) {
            ApIconBadge(glyph)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                if (detail.isNotEmpty()) Text(detail, color = ink.copy(alpha = .68f), fontSize = 12.sp)
            }
            ApGlyph(ApGlyphKind.OPEN, Modifier.size(20.dp), blue)
        }
    }
    Spacer(Modifier.height(9.dp))
}

@Composable private fun FirstRun(onFinish: (String) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var confirmOnboardingExit by remember { mutableStateOf(false) }
    val onboardingContext = LocalContext.current
    BackHandler {
        if (confirmOnboardingExit) confirmOnboardingExit = false
        else if (step > 0) step--
        else confirmOnboardingExit = true
    }
    if (confirmOnboardingExit) AlertDialog(
        onDismissRequest = { confirmOnboardingExit = false },
        title = { Text("Sair da configuração inicial?") },
        text = { Text("Você poderá continuar a apresentação quando abrir o aplicativo novamente.") },
        confirmButton = { TextButton(onClick = { confirmOnboardingExit = false; (onboardingContext as? android.app.Activity)?.finish() }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirmOnboardingExit = false }) { Text("Continuar") } })
    var name by rememberSaveable { mutableStateOf("") }
    val messages = listOf(
        Triple("Tudo o que você precisa em um só lugar", "Planeje aulas, registre sua turma e acompanhe sua rotina.", ApGlyphKind.DOCUMENT),
        Triple("Planejamento que se adapta a você", "Dia, semana e mês: suas aulas reunidas com clareza.", ApGlyphKind.CALENDAR),
        Triple("Mais tempo para o que importa", "Seus registros ficam neste aparelho. Você começa sem criar conta.", ApGlyphKind.CHECK),
    )
    Column(Modifier.fillMaxSize().background(canvas).verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(50.dp))
        Text("Assistente Pedagógico", color = ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(36.dp))
        if (step < messages.size) {
            val (headline, detail, icon) = messages[step]
            Surface(Modifier.size(180.dp), color = ApColors.White, shape = RoundedCornerShape(48.dp), border = BorderStroke(1.dp, outline)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(icon, Modifier.size(94.dp), blue) }
            }
            Spacer(Modifier.height(34.dp))
            Text(headline, color = ink, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 29.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Text(detail, color = ink, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(33.dp))
            Text("${step + 1} / 3", color = blue, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(26.dp))
            PrimaryButton(if (step == 2) "Começar agora" else "Continuar →") { step++ }
            Spacer(Modifier.height(12.dp))
            if (step < 2) TextButton(onClick = { step = 3 }) { Text("Pular apresentação", color = ink) }
        } else {
            Text("Conte um pouco sobre você", color = ink, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("Vamos preparar seu espaço de trabalho.", color = ink)
            Spacer(Modifier.height(28.dp))
            Panel {
                Input("Seu nome", name, { name = it })
                Text("Seus dados ficam neste aparelho. Não há login ou sincronização com a nuvem nesta versão.", color = ink.copy(alpha = .7f), fontSize = 12.sp)
                Spacer(Modifier.height(15.dp))
                PrimaryButton("Continuar →") { onFinish(name) }
            }
        }
    }
}

@Composable private fun ClassForm(onBack: () -> Unit, onSave: (String, String, String) -> Unit, initial: Classroom? = null, onDirty: () -> Unit = {}) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var stage by rememberSaveable(initial?.id) { mutableStateOf(initial?.stage ?: "Ensino Fundamental") }
    var shift by rememberSaveable(initial?.id) { mutableStateOf(initial?.shift ?: ClassroomRules.unspecifiedShift) }
    Heading(if (initial == null) "Vamos criar sua turma?" else "Editar turma", if (initial == null) "Você poderá adicionar alunos depois." else "Atualize os dados da turma.", onBack)
    Panel {
        Input("Nome da turma (ex.: 5º Ano A)", name, { if (it != name) { name = it; onDirty() } })
        Text("Etapa de ensino", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Choices(ClassroomRules.stages, stage) { if (it != stage) { stage = it; onDirty() } }
        Text("Turno", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Choices(ClassroomRules.shifts, shift, columns = 2) { if (it != shift) { shift = it; onDirty() } }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(if (initial == null) "Criar turma →" else "Salvar alterações") { onSave(name, stage, shift) }
    }
}

@Composable private fun HomeScreen(data: TeacherSnapshot, classroom: Classroom?, go: (String) -> Unit, select: (Long) -> Unit) {
    Heading("Olá, ${data.profile?.name?.substringBefore(' ') ?: "Professor(a)"}!", "Que bom ter você aqui hoje")
    if (classroom == null) {
        Panel { Text("Nenhuma turma ativa. Crie uma turma ou restaure uma turma arquivada.", color = ink) }
        Spacer(Modifier.height(12.dp)); PrimaryButton("Gerenciar turmas") { go("classes") }; return
    }
    val activeClasses = data.classrooms.filterNot { it.archived }
    val homeDay = today()
    val overview = HomeContentPolicy.forClass(data, classroom, homeDay)
    if (activeClasses.size > 1) {
        Text("Turma em foco", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeClasses.forEach { option ->
                val chosen = option.id == classroom.id
                val sameNameAndShift = activeClasses.count { it.name == option.name && it.shift == option.shift } > 1
                val label = "${option.name} · ${option.shift}" + if (sameNameAndShift) " · turma ${option.id}" else ""
                OutlinedButton(
                    onClick = { select(option.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(ApShapeToken.Medium),
                    border = BorderStroke(1.dp, if (chosen) blue else outline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (chosen) blue else Color.White),
                ) { Text(label, color = if (chosen) Color.White else ink, fontWeight = FontWeight.ExtraBold) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    val todayLesson = overview.lesson
    Surface(shape = RoundedCornerShape(ApShapeToken.Hero), color = blue, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(21.dp)) {
            ApEyebrow("Aula em foco", Modifier)
            Spacer(Modifier.height(10.dp))
            Text(todayLesson?.title ?: "Seu próximo planejamento começa aqui", color = Color.White, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(todayLesson?.objective ?: "Crie um plano de aula e veja sua rotina ganhar forma.", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (todayLesson == null) "Criar plano de aula →" else "Ver planejamento →", secondary = true) { go(if (todayLesson == null) "newLesson" else "planning") }
        }
    }
    Spacer(Modifier.height(20.dp))
    Subtitle("Para resolver agora")
    val attendanceSummary = if (overview.attendanceRosterComplete == false) {
        "${classroom.name} • ${overview.attendanceMarked} registros preservados · lista incompleta"
    } else {
        "${classroom.name} • ${overview.attendanceMarked}/${overview.studentCount} registros hoje"
    }
    ActionTile(ApGlyphKind.USERS, "Fazer chamada", attendanceSummary) { go("attendance") }
    ActionTile(ApGlyphKind.DOCUMENT, "Registrar observação", "Guarde o contexto da aula") { go("observation") }
    ActionTile(ApGlyphKind.CALENDAR, "Planejar aula", "Organize seu dia") { go("planning") }
    ActionTile(ApGlyphKind.CALENDAR, "Ver compromissos", "Agenda do dia") { go("agenda") }
    Spacer(Modifier.height(15.dp))
    Subtitle("Sua agenda de hoje")
    val events = data.appointments.filter { it.date == homeDay }
    if (events.isEmpty()) Panel { Text("Nenhum compromisso cadastrado para hoje.", color = ink) }
    events.forEach { item -> ActionTile(ApGlyphKind.CALENDAR, item.title, item.time) { go("agenda") } }
    Spacer(Modifier.height(12.dp))
    Subtitle("Atividade recente")
    overview.recentObservations.forEach { note ->
        ActionTile(ApGlyphKind.DOCUMENT, note.kind, note.body.take(75)) { go("observationHistory") }
    }
    if (overview.recentObservations.isEmpty()) Panel {
        Text("Nenhum registro nesta turma. Suas observações de outras turmas permanecem separadas.", color = ink)
    }
}

@Composable private fun ClassesScreen(data: TeacherSnapshot, current: Classroom?, onPick: (Long) -> Unit, onAdd: () -> Unit, onRestore: (Long) -> Unit) {
    Heading("Suas turmas", "Organize suas turmas e alunos")
    var search by rememberSaveable { mutableStateOf("") }
    val active = data.classrooms.filterNot { it.archived }
    val archived = data.classrooms.filter { it.archived }
    fun matches(classroom: Classroom) = search.isBlank() ||
        "${classroom.name} ${classroom.stage} ${classroom.shift}".contains(search.trim(), ignoreCase = true)
    val visibleActive = active.filter(::matches)
    val visibleArchived = archived.filter(::matches)
    if (active.size < 2) {
        ApRaisedButton("Nova turma", onAdd, glyph = ApGlyphKind.PLUS)
        Spacer(Modifier.height(12.dp))
    } else {
        Panel {
            Text("Você já tem 2 turmas ativas", color = ink, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp)); Text("Arquive uma turma para criar ou restaurar outra. Seus registros continuam preservados.", color = ink.copy(alpha = .72f))
        }
        Spacer(Modifier.height(12.dp))
    }
    if (data.classrooms.size >= 5) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar turma por nome, etapa ou turno") },
            singleLine = true,
            shape = RoundedCornerShape(ApShapeToken.Medium),
            trailingIcon = if (search.isNotEmpty()) ({
                TextButton(onClick = { search = "" }) { Text("Limpar") }
            }) else null,
        )
        Spacer(Modifier.height(10.dp))
    }
    if (active.isEmpty()) Panel { Text("Nenhuma turma ativa. Crie uma ou restaure uma arquivada.", color = ink) }
    else if (visibleActive.isEmpty() && search.isNotBlank()) Text("Nenhuma turma ativa corresponde à busca.", color = ink)
    visibleActive.forEach { classroom ->
        ActionTile(ApGlyphKind.USERS, classroom.name, "${classroom.stage} • ${classroom.shift} • ${data.students.count { it.classroomId == classroom.id }} alunos") { onPick(classroom.id) }
    }
    if (archived.isNotEmpty()) {
        Spacer(Modifier.height(20.dp)); Subtitle("Turmas arquivadas")
        if (visibleArchived.isEmpty() && search.isNotBlank()) Text("Nenhuma turma arquivada corresponde à busca.", color = ink)
        visibleArchived.forEach { classroom ->
            if (active.size < 2) {
                ActionTile(ApGlyphKind.RESTORE, classroom.name, "Restaurar turma e seus registros") { onRestore(classroom.id) }
            } else {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(ApShapeToken.Card), color = Color.White, border = BorderStroke(1.dp, outline)) {
                    Row(Modifier.padding(ApSpace.Base), verticalAlignment = Alignment.CenterVertically) {
                        ApIconBadge(ApGlyphKind.RESTORE)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(classroom.name, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Libere uma vaga ativa para restaurar esta turma.", color = ink.copy(alpha = .68f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable private fun ClassDetail(
    data: TeacherSnapshot,
    classroom: Classroom,
    section: String,
    onSectionChange: (String) -> Unit,
    studentSearch: String,
    onStudentSearchChange: (String) -> Unit,
    back: () -> Unit,
    go: (String) -> Unit,
    onStudent: (Long) -> Unit,
    onObservation: (Long) -> Unit,
) {
    val students = data.students.filter { it.classroomId == classroom.id }
    val sessions = data.attendanceSessions.filter { it.classroomId == classroom.id }.sortedByDescending { it.date }
    val observations = data.observations.filter { it.classroomId == classroom.id }.sortedByDescending { it.date }
    val visibleStudents = if (studentSearch.isBlank()) students else students.filter {
        it.name.contains(studentSearch.trim(), ignoreCase = true)
    }
    Heading(classroom.name, "${classroom.stage} • ${classroom.shift}", back)
    ClassDetailTabs(section, onSectionChange)
    Spacer(Modifier.height(16.dp))

    when (section) {
        "Alunos" -> {
            Text("${students.size} ${if (students.size == 1) "aluno" else "alunos"}", color = ink.copy(alpha = .72f), fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            if (students.isEmpty()) {
                Panel {
                    Text("Sua lista começa aqui", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp)); Text("Adicione os alunos agora ou cole uma lista. Você poderá editar os cadastros depois.", color = ink.copy(alpha = .72f))
                    Spacer(Modifier.height(14.dp)); ApRaisedButton("Adicionar primeiro aluno", { go("addStudent") }, glyph = ApGlyphKind.PLUS)
                    TextButton(onClick = { go("addStudents") }, modifier = Modifier.fillMaxWidth().heightIn(min = ApSizeToken.MinTouchTarget)) {
                        Text("Colar uma lista de alunos", color = ApPalette.Action, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ApRaisedButton("Adicionar aluno", { go("addStudent") }, Modifier.weight(1f), ApGlyphKind.PLUS)
                    ApRaisedButton("Colar lista", { go("addStudents") }, Modifier.weight(1f), ApGlyphKind.IMPORT, secondary = true)
                }
                if (students.size >= 8) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = studentSearch,
                        onValueChange = onStudentSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar aluno pelo nome") },
                        singleLine = true,
                        shape = RoundedCornerShape(ApShapeToken.Medium),
                        trailingIcon = if (studentSearch.isNotEmpty()) ({
                            TextButton(onClick = { onStudentSearchChange("") }) { Text("Limpar") }
                        }) else null,
                    )
                }
                Spacer(Modifier.height(10.dp))
                when {
                    visibleStudents.isEmpty() -> Panel { Text("Nenhum aluno corresponde a “${studentSearch.trim()}”. Limpe a busca para ver a lista completa.", color = ink) }
                    else -> visibleStudents.forEach { student ->
                        ActionTile(ApGlyphKind.USERS, studentLabel(data.students, student), "Consultar e editar cadastro") { onStudent(student.id) }
                    }
                }
            }
        }

        "Frequência" -> {
            Subtitle("Chamada da turma")
            if (students.isEmpty()) {
                Panel {
                    Text("Adicione alunos antes de iniciar a chamada.", color = ink)
                    Spacer(Modifier.height(12.dp)); ApRaisedButton("Adicionar primeiro aluno", { go("addStudent") }, glyph = ApGlyphKind.PLUS)
                }
            } else {
                ApRaisedButton("Fazer chamada de hoje", { go("attendance") }, glyph = ApGlyphKind.CALENDAR)
            }
            Spacer(Modifier.height(10.dp))
            ActionTile(ApGlyphKind.CALENDAR, "Histórico de frequência", "Consultar e corrigir chamadas anteriores") { go("attendanceHistory") }
            Spacer(Modifier.height(8.dp)); Subtitle("Última chamada")
            val latest = sessions.firstOrNull()
            if (latest == null) {
                Panel { Text("O histórico desta turma aparecerá aqui depois da primeira chamada salva.", color = ink) }
            } else {
                val present = latest.members.count { it.status == "P" }
                val absent = latest.members.count { it.status == "F" }
                val pending = latest.members.count { it.status == "?" }
                Panel {
                    Text(friendlyDay(latest.date), color = ink, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp)); Text(
                        "$present ${if (present == 1) "presente" else "presentes"} • " +
                            "$absent ${if (absent == 1) "falta" else "faltas"} • " +
                            "$pending ${if (pending == 1) "pendente" else "pendentes"}",
                        color = ink.copy(alpha = .78f),
                    )
                    if (!latest.rosterComplete) {
                        Spacer(Modifier.height(8.dp)); Text("Registro histórico parcial: a lista completa de alunos não foi preservada nesta chamada.", color = ink.copy(alpha = .72f), fontSize = 13.sp)
                    }
                }
            }
        }

        "Registros" -> {
            Subtitle("Acompanhamento pedagógico")
            ApRaisedButton("Novo registro", { go("observation") }, glyph = ApGlyphKind.DOCUMENT)
            Spacer(Modifier.height(9.dp))
            ActionTile(ApGlyphKind.DOCUMENT, "Histórico de registros", "Consultar e editar as observações desta turma") { go("observationHistory") }
            Spacer(Modifier.height(8.dp)); Subtitle("Mais recentes")
            if (observations.isEmpty()) {
                Panel { Text("Os registros desta turma aparecerão aqui depois que você salvar a primeira observação.", color = ink) }
            } else observations.take(5).forEach { note -> ObservationPreview(note) { onObservation(note.id) } }
        }

        else -> {
            if (students.isEmpty()) {
                Panel {
                    Text("Prepare sua turma", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp)); Text("Cadastre a lista uma vez. Depois, a chamada fica a um toque de distância.", color = ink.copy(alpha = .72f))
                    Spacer(Modifier.height(14.dp)); ApRaisedButton("Adicionar primeiro aluno", { go("addStudent") }, glyph = ApGlyphKind.PLUS)
                    TextButton(onClick = { go("addStudents") }, modifier = Modifier.fillMaxWidth().heightIn(min = ApSizeToken.MinTouchTarget)) {
                        Text("Colar uma lista de alunos", color = ApPalette.Action, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                ApRaisedButton("Fazer chamada de hoje", { go("attendance") }, glyph = ApGlyphKind.CALENDAR)
                Spacer(Modifier.height(14.dp))
                Subtitle("Resumo da turma")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClassMetric("${students.size}", "Alunos", Modifier.weight(1f))
                    ClassMetric("${sessions.size}", "Chamadas", Modifier.weight(1f))
                    ClassMetric("${observations.size}", "Registros", Modifier.weight(1f))
                }
                Spacer(Modifier.height(17.dp)); Subtitle("Acessos rápidos")
                ActionTile(ApGlyphKind.CALENDAR, "Planejar aula", "Criar um plano para esta turma") { go("newLesson") }
                ActionTile(ApGlyphKind.DOCUMENT, "Novo registro", "Adicionar uma observação pedagógica") { go("observation") }
                if (sessions.isNotEmpty()) ActionTile(ApGlyphKind.CHECK, "Histórico de frequência", "${sessions.size} ${if (sessions.size == 1) "chamada salva" else "chamadas salvas"}") { go("attendanceHistory") }
            }
            Spacer(Modifier.height(12.dp)); Subtitle("Gerenciar turma")
            ActionTile(ApGlyphKind.EDIT, "Editar turma", "Nome, etapa e turno") { go("editClass") }
            PrimaryButton("Arquivar turma", secondary = true) { go("archiveClass") }
        }
    }
}

@Composable private fun ClassDetailTabs(selected: String, onSelect: (String) -> Unit) {
    val sections = listOf("Visão do dia", "Alunos", "Frequência", "Registros")
    Row(
        Modifier.fillMaxWidth().selectableGroup().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sections.forEach { label ->
            val active = selected == label
            Surface(
                modifier = Modifier.heightIn(min = ApSizeToken.MinTouchTarget)
                    .selectable(selected = active, role = Role.Tab) { onSelect(label) },
                shape = RoundedCornerShape(ApShapeToken.Pill),
                color = if (active) ApPalette.Action else Color.White,
                border = BorderStroke(1.dp, if (active) ApPalette.Action else outline),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (active) Color.White else ink, fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun ClassMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier.heightIn(min = 78.dp), color = Color.White, shape = RoundedCornerShape(ApShapeToken.Medium), border = BorderStroke(1.dp, outline)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center) {
            Text(value, color = ApPalette.Action, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black)
            Text(label, color = ink.copy(alpha = .76f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun ObservationPreview(note: Observation, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(ApShapeToken.Card), color = Color.White, border = BorderStroke(1.dp, outline)) {
        Column(Modifier.padding(ApSpace.Base)) {
            Text("${note.kind} • ${note.date}", color = ApPalette.Action, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp)); Text(note.body, color = ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp)); Text("Toque para consultar ou editar", color = ink.copy(alpha = .68f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable private fun ArchiveClassScreen(classroom: Classroom, back: () -> Unit, archive: () -> Unit) {
    Heading("Arquivar turma?", classroom.name, back)
    Panel {
        Text("Arquivar preserva alunos, frequências, observações e planos. A turma deixa de contar no limite de turmas ativas e poderá ser restaurada em Suas turmas.", color = ink)
        Spacer(Modifier.height(14.dp)); PrimaryButton("Confirmar arquivamento") { archive() }
        Spacer(Modifier.height(10.dp)); PrimaryButton("Cancelar", secondary = true) { back() }
    }
}

@Composable private fun StudentEditForm(student: Student, classroom: Classroom, displayName: String = student.name, back: () -> Unit, save: (String) -> Unit, delete: () -> Unit, onDirty: () -> Unit = {}) {
    var name by rememberSaveable(student.id) { mutableStateOf(student.name) }
    var confirming by remember { mutableStateOf(false) }
    Heading("Editar aluno", classroom.name, back)
    Panel {
        Input("Nome completo do aluno", name, { if (it != name) { name = it; onDirty() } })
        PrimaryButton("Salvar alterações") { save(name) }
        Spacer(Modifier.height(14.dp)); PrimaryButton("Excluir aluno", secondary = true) { confirming = true }
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false }, title = { Text("Excluir $displayName?") },
        text = { Text("A exclusão do cadastro é permanente. As chamadas já salvas permanecem no histórico com o nome registrado; observações vinculadas também permanecem, sem identificação do aluno.") },
        confirmButton = { TextButton(onClick = { confirming = false; delete() }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } })
}

@Composable private fun StudentForm(classroom: Classroom, back: () -> Unit, save: (String) -> Unit, onDirty: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    Heading("Adicionar aluno", classroom.name, back)
    Panel { Input("Nome completo do aluno", name, { if (it != name) { name = it; onDirty() } }); PrimaryButton("Salvar aluno") { save(name) } }
}

@Composable private fun StudentRosterImportForm(classroom: Classroom, back: () -> Unit, save: (List<String>) -> Unit, onDirty: () -> Unit = {}) {
    var pastedNames by rememberSaveable(classroom.id) { mutableStateOf("") }
    val parsed = remember(pastedNames) { runCatching { StudentRosterImport.parse(pastedNames) } }
    val names = parsed.getOrNull().orEmpty()
    Heading("Adicionar lista de alunos", classroom.name, back)
    Panel {
        Text("Cole somente os nomes, um por linha. Linhas vazias são ignoradas; homônimos continuam como cadastros separados.", color = ink)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pastedNames,
            onValueChange = { if (it != pastedNames) { pastedNames = it; onDirty() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nomes dos alunos") },
            placeholder = { Text("Ana Silva\nBruno Souza") },
            minLines = 8,
            maxLines = 14,
            shape = RoundedCornerShape(ApShapeToken.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = blue, unfocusedBorderColor = outline, focusedLabelColor = blue,
                focusedContainerColor = ApColors.White, unfocusedContainerColor = ApColors.White,
            ),
        )
        Spacer(Modifier.height(8.dp))
        if (parsed.isSuccess) {
            Text("${names.size} ${if (names.size == 1) "aluno pronto" else "alunos prontos"} para adicionar", color = ink, fontWeight = FontWeight.Bold)
        } else if (pastedNames.isNotBlank()) {
            Text(parsed.exceptionOrNull()?.message.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            if (names.size == 1) "Adicionar 1 aluno" else "Adicionar ${names.size} alunos",
            enabled = names.isNotEmpty(),
        ) { save(names) }
        Spacer(Modifier.height(9.dp))
        PrimaryButton("Cancelar", secondary = true) { back() }
    }
}

@Composable private fun DaySwitch(day: String, onDay: (String) -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(Modifier.size(48.dp).clickable(role = Role.Button) { onDay(LocalDate.parse(day).minusDays(1).toString()) }
                .semantics { contentDescription = "Dia anterior" }, color = pale, shape = RoundedCornerShape(13.dp)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(20.dp), blue) }
            }
            Text(friendlyDay(day), color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Surface(Modifier.size(48.dp).clickable(role = Role.Button) { onDay(LocalDate.parse(day).plusDays(1).toString()) }
                .semantics { contentDescription = "Próximo dia" }, color = pale, shape = RoundedCornerShape(13.dp)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.OPEN, Modifier.size(18.dp), blue) }
            }
        }
    }
    Spacer(Modifier.height(15.dp))
}

@Composable private fun AttendanceEditor(data: TeacherSnapshot, classroom: Classroom, day: String, onDay: (String) -> Unit, onBack: () -> Unit, onAddStudent: () -> Unit, onSave: (Map<Long, String>) -> Unit, onDirty: () -> Unit = {}) {
    val enrolledStudents = data.students.filter { it.classroomId == classroom.id }
    val session = data.attendanceSessions.firstOrNull { it.classroomId == classroom.id && it.date == day }
    val students = session?.members?.map { Student(it.studentId, classroom.id, it.studentName) } ?: enrolledStudents
    val draft = remember(classroom.id, day, session, data.attendance, students) {
        mutableStateMapOf<Long, String>().apply {
            if (session != null) session.members.forEach { this[it.studentId] = it.status }
            else students.forEach { student -> this[student.id] = "?" }
        }
    }
    Heading("Frequência", classroom.name, onBack)
    DaySwitch(day, onDay)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(Triple("Presentes", draft.values.count { it == "P" }, green), Triple("Faltas", draft.values.count { it == "F" }, red), Triple("Pendentes", draft.values.count { it == "?" }, blue)).forEach { (label, count, tint) ->
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(15.dp), color = Color.White, border = BorderStroke(1.dp, outline)) {
                Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 11.sp, color = ink, fontWeight = FontWeight.Bold)
                    Text("$count", fontSize = 27.sp, color = tint, fontWeight = FontWeight.Black)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (students.isNotEmpty() && students.any { draft[it.id] != "P" }) {
        TextButton(
            onClick = {
                students.forEach { draft[it.id] = "P" }
                onDirty()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Marcar todos como presentes", color = blue, fontWeight = FontWeight.Bold) }
    }
    if (session != null && !session.rosterComplete) {
        Panel {
            Text("Esta chamada antiga não guardava a lista completa. Os nomes e as marcações abaixo são os únicos dados históricos preservados.", color = ink)
        }
        Spacer(Modifier.height(12.dp))
    }
    if (students.isEmpty()) {
        Panel { Text("Cadastre alunos nesta turma antes de fazer a chamada.", color = ink) }
        Spacer(Modifier.height(12.dp)); PrimaryButton("Adicionar alunos", click = onAddStudent)
    } else {
        students.forEach { student ->
            Panel {
                Text(student.name, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Choices(listOf("Presente", "Falta", "Pendente"), when(draft[student.id]) { "P" -> "Presente"; "F" -> "Falta"; else -> "Pendente" }) { label ->
                    val next = when(label) { "Presente" -> "P"; "Falta" -> "F"; else -> "?" }
                    if (draft[student.id] != next) { draft[student.id] = next; onDirty() }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp)); PrimaryButton("Salvar frequência") { onSave(draft.toMap()) }
        Spacer(Modifier.height(5.dp)); Text("As alterações só são gravadas ao tocar em Salvar frequência.", fontSize = 12.sp, color = ink.copy(alpha = .7f))
    }
}

@Composable private fun ObservationForm(data: TeacherSnapshot, classroom: Classroom, back: () -> Unit,
    save: (Long?, String, String, Boolean) -> Unit, initial: Observation? = null, delete: (() -> Unit)? = null, onDirty: () -> Unit = {}) {
    var studentId by rememberSaveable(initial?.id) { mutableLongStateOf(initial?.studentId ?: -1L) }
    var kind by rememberSaveable(initial?.id) { mutableStateOf(initial?.kind ?: "Participação") }
    var body by rememberSaveable(initial?.id) { mutableStateOf(initial?.body.orEmpty()) }
    var approved by rememberSaveable(initial?.id) { mutableStateOf(initial?.shareApproved ?: false) }
    var confirmDeletion by remember { mutableStateOf(false) }
    var studentPickerExpanded by remember { mutableStateOf(false) }
    val classroomStudents = data.students.filter { it.classroomId == classroom.id }
    val selectedStudent = classroomStudents.firstOrNull { it.id == studentId }
    fun studentLabel(student: Student): String = if (classroomStudents.count { it.name == student.name } > 1) "${student.name} · cadastro ${student.id}" else student.name
    Heading(if (initial == null) "Nova observação" else "Editar observação", if (initial == null) "Registre uma observação sobre a turma" else "Registro de ${initial.date}", back)
    Panel {
        Text("Aluno (opcional)", fontWeight = FontWeight.Bold, color = ink); Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { studentPickerExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(selectedStudent?.let(::studentLabel) ?: "Turma inteira", maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = studentPickerExpanded, onDismissRequest = { studentPickerExpanded = false }) {
                DropdownMenuItem(text = { Text("Turma inteira") }, onClick = {
                    if (studentId != -1L) { studentId = -1L; onDirty() }; studentPickerExpanded = false
                })
                classroomStudents.forEach { student -> DropdownMenuItem(text = { Text(studentLabel(student)) }, onClick = {
                    if (studentId != student.id) { studentId = student.id; onDirty() }; studentPickerExpanded = false
                }) }
            }
        }
    }
    Spacer(Modifier.height(11.dp))
    Panel {
        Text("Tipo de observação", fontWeight = FontWeight.Bold, color = ink); Spacer(Modifier.height(9.dp))
        Choices(listOf("Comportamento", "Participação", "Aprendizagem", "Outro"), kind) { if (it != kind) { kind = it; onDirty() } }
        Input("Descrição objetiva e respeitosa", body, { if (it.length <= 500 && it != body) { body = it; onDirty() } }, multiline = true)
        Text("${body.length}/500", fontSize = 12.sp, color = ink.copy(alpha = .7f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Autorizar compartilhamento futuro", color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Nenhuma informação é enviada a famílias nesta versão.", fontSize = 11.sp, color = ink.copy(alpha = .7f))
            }
            Switch(approved, { if (it != approved) { approved = it; onDirty() } })
        }
        Spacer(Modifier.height(12.dp)); PrimaryButton(if (initial == null) "Salvar observação" else "Salvar alterações") { save(studentId.takeIf { it != -1L }, kind, body, approved) }
        if (delete != null) { Spacer(Modifier.height(12.dp)); PrimaryButton("Excluir observação", secondary = true) { confirmDeletion = true } }
    }
    if (confirmDeletion && delete != null) AlertDialog(
        onDismissRequest = { confirmDeletion = false }, title = { Text("Excluir observação?") },
        text = { Text("Esta exclusão é permanente e afeta somente este registro pedagógico.") },
        confirmButton = { TextButton(onClick = { confirmDeletion = false; delete() }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { confirmDeletion = false }) { Text("Cancelar") } })
}

@Composable private fun PlanningScreen(data: TeacherSnapshot, classroom: Classroom?, day: String, onDay: (String) -> Unit,
    go: (String) -> Unit, openLesson: (Long) -> Unit, restoreLesson: (Lesson) -> Unit) {
    PlanningWorkspace(data, classroom, day, onDay, go, openLesson, restoreLesson)
}

@Composable private fun LessonForm(classroom: Classroom, initialDay: String, back: () -> Unit,
    save: (String, String, String, String, String, String, String) -> Unit,
    initial: Lesson? = null, archive: (() -> Unit)? = null, onDirty: () -> Unit = {}) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var subject by rememberSaveable(initial?.id) { mutableStateOf(initial?.subject.orEmpty()) }
    var day by rememberSaveable(initial?.id) { mutableStateOf(initial?.date ?: initialDay) }
    var time by rememberSaveable(initial?.id) { mutableStateOf(initial?.time ?: "08:00") }
    var objective by rememberSaveable(initial?.id) { mutableStateOf(initial?.objective.orEmpty()) }
    var content by rememberSaveable(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    var method by rememberSaveable(initial?.id) { mutableStateOf(initial?.method.orEmpty()) }
    var confirmArchive by remember { mutableStateOf(false) }
    fun setIfChanged(old: String, next: String, setter: (String) -> Unit) { if (old != next) { setter(next); onDirty() } }
    Heading(if (initial == null) "Novo plano de aula" else "Editar plano de aula", classroom.name, back)
    Panel {
        Input("Título da aula", title, { setIfChanged(title, it) { v -> title = v } })
        Input("Disciplina / componente curricular", subject, { setIfChanged(subject, it) { v -> subject = v } })
        Input("Data (AAAA-MM-DD)", day, { setIfChanged(day, it) { v -> day = v } })
        Input("Horário (HH:MM)", time, { setIfChanged(time, it) { v -> time = v } })
        Input("Objetivo da aula", objective, { setIfChanged(objective, it) { v -> objective = v } }, multiline = true)
        Input("Conteúdo / objeto de conhecimento", content, { setIfChanged(content, it) { v -> content = v } }, multiline = true)
        Input("Metodologia", method, { setIfChanged(method, it) { v -> method = v } }, multiline = true)
        PrimaryButton(if (initial == null) "Salvar plano de aula" else "Salvar alterações") { save(title, subject, day, time, objective, content, method) }
        if (archive != null) { Spacer(Modifier.height(12.dp)); PrimaryButton("Arquivar plano", secondary = true) { confirmArchive = true } }
    }
    if (confirmArchive && archive != null) AlertDialog(
        onDismissRequest = { confirmArchive = false }, title = { Text("Arquivar plano?") },
        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados.") },
        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } })
}

@Composable private fun AgendaScreen(data: TeacherSnapshot, day: String, onDay: (String) -> Unit,
    add: () -> Unit, open: (Long) -> Unit, openLesson: (Long) -> Unit) {
    PlanningAgendaScreen(data, day, onDay, add, open, openLesson)
}

@Composable private fun MoreScreen(data: TeacherSnapshot, go: (String) -> Unit) {
    Heading("Mais", "Ferramentas e recursos extras")
    ActionTile(ApGlyphKind.USERS, "Perfil profissional", data.profile?.name.orEmpty()) { go("profile") }
    ActionTile(ApGlyphKind.USERS, "Gerenciar turmas", "Criar, consultar e organizar") { go("classes") }
    ActionTile(ApGlyphKind.CALENDAR, "Planejamento", "Suas aulas") { go("planning") }
    ActionTile(ApGlyphKind.FOLDER, "Arquivos", "Documentos no aparelho") { go("files") }
    Spacer(Modifier.height(15.dp))
    Panel {
        Text("Privacidade primeiro", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Os registros estão armazenados localmente. Conta na nuvem, assinaturas e sincronização ainda não estão ativadas; não coletamos dados dos estudantes em serviços externos.", color = ink, fontSize = 13.sp)
    }
}

@Composable private fun ProfileForm(original: String, back: () -> Unit, save: (String) -> Unit, onDirty: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf(original) }
    Heading("Perfil profissional", "Seus dados e preferências", back)
    Panel { Input("Nome", name, { if (it != name) { name = it; onDirty() } }); PrimaryButton("Salvar perfil") { save(name) } }
}
