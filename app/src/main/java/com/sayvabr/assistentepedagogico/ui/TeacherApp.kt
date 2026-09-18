package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/** Working offline product slice; navigation points only to implemented screens. */
@Composable
fun TeacherApp(store: TeacherStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<TeacherSnapshot?>(null) }
    var screen by rememberSaveable { mutableStateOf("home") }
    var tab by rememberSaveable { mutableStateOf("Início") }
    var selectedClass by rememberSaveable { mutableLongStateOf(-1L) }
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
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ApSpace.Base)) {
                Spacer(Modifier.height(18.dp))
                when (screen) {
                    "home" -> HomeScreen(snapshot, currentClass, { destination ->
                        if (destination in setOf("attendance", "newLesson", "planning", "agenda")) selectedDay = today()
                        requestNavigate(destination)
                    }, { selectedClass = it })
                    "classes" -> ClassesScreen(snapshot, currentClass,
                        onPick = { selectedClass = it; requestNavigate("classDetail") },
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
                    "classDetail" -> if (currentClass != null) ClassDetail(snapshot, currentClass, { requestNavigate(it) },
                        onStudent = { selectedStudent = it; requestNavigate("editStudent") },
                        onObservation = { selectedObservation = it; requestNavigate("editObservation") })
                    "editStudent" -> if (currentClass != null) snapshot.students.firstOrNull {
                        it.id == selectedStudent && it.classroomId == currentClass.id
                    }?.let { student -> StudentEditForm(student, currentClass,
                        back = { requestBack() },
                        save = { name -> commit("classDetail") { store.updateStudent(currentClass.id, student.id, name) } },
                        delete = { commit("classDetail") { store.deleteStudent(currentClass.id, student.id) } },
                        onDirty = { formDirty = true }) }
                    "addStudent" -> if (currentClass != null) StudentForm(currentClass, { requestBack() },
                        { name -> commit("classDetail") { store.addStudent(currentClass.id, name) } }, onDirty = { formDirty = true })
                    "attendanceHistory" -> if (currentClass != null) AttendanceHistoryScreen(
                        snapshot, currentClass, back = { back() },
                        openDay = { day -> selectedDay = day; navigate("attendance") },
                        startAttendance = { selectedDay = today(); navigate("attendance") })
                    "attendance" -> if (currentClass != null) AttendanceEditor(
                        snapshot, currentClass, selectedDay,
                        onDay = { requestDay(it) }, onBack = { requestBack() },
                        onAddStudent = { requestNavigate("addStudent") },
                        onSave = { marks -> commit("classDetail") { store.saveAttendance(currentClass.id, selectedDay, marks) } },
                        onDirty = { formDirty = true })
                    "observationHistory" -> if (currentClass != null) ObservationHistoryScreen(
                        snapshot, currentClass, onBack = { back() },
                        onNew = { requestNavigate("observation") },
                        onOpen = { selectedObservation = it; requestNavigate("editObservation") })
                    "observation" -> if (currentClass != null) ObservationForm(snapshot, currentClass, { requestBack() },
                        { studentId, kind, body, share -> commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") {
                            store.addObservation(currentClass.id, studentId, kind, body, share)
                        } }, onDirty = { formDirty = true })
                    "editObservation" -> if (currentClass != null) snapshot.observations.firstOrNull {
                        it.id == selectedObservation && it.classroomId == currentClass.id
                    }?.let { observation ->
                        ObservationForm(snapshot, currentClass, { requestBack() }, { studentId, kind, body, share ->
                            commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") {
                                store.updateObservation(currentClass.id, observation.id, studentId, kind, body, share)
                            }
                        }, initial = observation,
                            delete = { commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") {
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
        if (screen !in listOf("createClass", "editClass", "archiveClass", "addStudent", "editStudent", "newLesson", "editLesson", "observation", "editObservation", "newAppointment", "editAppointment", "profile")) {
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

@Composable private fun PrimaryButton(label: String, secondary: Boolean = false, click: () -> Unit) {
    ApRaisedButton(label = label, onClick = click, secondary = secondary)
}

@Composable private fun Heading(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) {
            Surface(
                modifier = Modifier.size(46.dp).clickable(onClick = back),
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
@Composable private fun Choices(options: List<String>, current: String, pick: (String) -> Unit) {
    options.chunked(3).forEach { chunk ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chunk.forEach { option ->
                val chosen = option == current
                Surface(modifier = Modifier.weight(1f).heightIn(min = ApSizeToken.MinTouchTarget).clickable { pick(option) },
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
    var shift by rememberSaveable(initial?.id) { mutableStateOf(initial?.shift ?: "Matutino") }
    Heading(if (initial == null) "Vamos criar sua turma?" else "Editar turma", if (initial == null) "Você poderá adicionar alunos depois." else "Atualize os dados da turma.", onBack)
    Panel {
        Input("Nome da turma (ex.: 5º Ano A)", name, { if (it != name) { name = it; onDirty() } })
        Text("Etapa de ensino", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Choices(listOf("Educação Infantil", "Ensino Fundamental", "Ensino Médio"), stage) { if (it != stage) { stage = it; onDirty() } }
        Text("Turno", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Choices(listOf("Matutino", "Vespertino", "Noturno"), shift) { if (it != shift) { shift = it; onDirty() } }
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
    ActionTile(ApGlyphKind.USERS, "Fazer chamada", "${classroom.name} • ${overview.attendanceMarked}/${overview.studentCount} registros hoje") { go("attendance") }
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
    Heading("Suas turmas", "Organize suas classes e alunos")
    val active = data.classrooms.filterNot { it.archived }
    if (active.isEmpty()) Panel { Text("Nenhuma turma ativa. Crie uma ou restaure uma arquivada.", color = ink) }
    active.forEach { classroom ->
        ActionTile(ApGlyphKind.USERS, classroom.name, "${classroom.stage} • ${data.students.count { it.classroomId == classroom.id }} alunos") { onPick(classroom.id) }
    }
    Spacer(Modifier.height(10.dp))
    if (active.size < 2) PrimaryButton("+ Nova turma") { onAdd() }
    else Panel { Text("Você atingiu o limite gratuito de 2 turmas ativas. Arquive uma turma antes de criar ou restaurar outra.", color = ink) }
    Spacer(Modifier.height(9.dp))
    Text("Até 2 turmas ativas no plano gratuito; alunos ilimitados. Arquivar preserva os registros.", fontSize = 12.sp, color = ink.copy(alpha = .7f))
    val archived = data.classrooms.filter { it.archived }
    if (archived.isNotEmpty()) {
        Spacer(Modifier.height(20.dp)); Subtitle("Turmas arquivadas")
        archived.forEach { classroom -> ActionTile(ApGlyphKind.RESTORE, classroom.name, "Restaurar turma e seus registros") { onRestore(classroom.id) } }
    }
}

@Composable private fun ClassDetail(data: TeacherSnapshot, classroom: Classroom, go: (String) -> Unit, onStudent: (Long) -> Unit, onObservation: (Long) -> Unit) {
    Heading(classroom.name, "${data.students.count { it.classroomId == classroom.id }} alunos • ${classroom.stage}", { go("classes") })
    Subtitle("Sua turma")
    ActionTile(ApGlyphKind.EDIT, "Editar turma", "Nome, etapa e turno") { go("editClass") }
    ActionTile(ApGlyphKind.USERS, "Alunos", "Adicionar e consultar alunos") { go("addStudent") }
    ActionTile(ApGlyphKind.CHECK, "Frequência", "Registrar presenças e faltas") { go("attendance") }
    ActionTile(ApGlyphKind.CALENDAR, "Histórico de frequência", "Consultar e corrigir chamadas anteriores") { go("attendanceHistory") }
    ActionTile(ApGlyphKind.DOCUMENT, "Registros", "Nova observação pedagógica") { go("observation") }
    ActionTile(ApGlyphKind.DOCUMENT, "Histórico de registros", "Consultar e editar todas as observações") { go("observationHistory") }
    Spacer(Modifier.height(17.dp)); Subtitle("Alunos")
    val students = data.students.filter { it.classroomId == classroom.id }
    if (students.isEmpty()) Panel { Text("Esta turma ainda não tem alunos.", color = ink); Spacer(Modifier.height(9.dp)); PrimaryButton("Adicionar primeiro aluno") { go("addStudent") } }
    students.forEach { student -> ActionTile(ApGlyphKind.USERS, student.name, "Consultar e editar cadastro") { onStudent(student.id) } }
    Spacer(Modifier.height(14.dp)); Subtitle("Registros recentes")
    data.observations.filter { it.classroomId == classroom.id }.take(5).forEach { note ->
        Surface(Modifier.fillMaxWidth().clickable { onObservation(note.id) }, shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, outline)) {
            Column(Modifier.padding(15.dp)) {
                Text("${note.kind} • ${note.date}", color = blue, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp)); Text(note.body, color = ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp)); Text("Toque para consultar ou editar", color = ink.copy(alpha = .65f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(16.dp)); PrimaryButton("Arquivar turma", secondary = true) { go("archiveClass") }
}

@Composable private fun ArchiveClassScreen(classroom: Classroom, back: () -> Unit, archive: () -> Unit) {
    Heading("Arquivar turma?", classroom.name, back)
    Panel {
        Text("Arquivar preserva alunos, frequências, observações e planos. A turma deixa de contar no limite de turmas ativas e poderá ser restaurada em Suas turmas.", color = ink)
        Spacer(Modifier.height(14.dp)); PrimaryButton("Confirmar arquivamento") { archive() }
        Spacer(Modifier.height(10.dp)); PrimaryButton("Cancelar", secondary = true) { back() }
    }
}

@Composable private fun StudentEditForm(student: Student, classroom: Classroom, back: () -> Unit, save: (String) -> Unit, delete: () -> Unit, onDirty: () -> Unit = {}) {
    var name by rememberSaveable(student.id) { mutableStateOf(student.name) }
    var confirming by remember { mutableStateOf(false) }
    Heading("Editar aluno", classroom.name, back)
    Panel {
        Input("Nome completo do aluno", name, { if (it != name) { name = it; onDirty() } })
        PrimaryButton("Salvar alterações") { save(name) }
        Spacer(Modifier.height(14.dp)); PrimaryButton("Excluir aluno", secondary = true) { confirming = true }
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false }, title = { Text("Excluir este aluno?") },
        text = { Text("A exclusão é permanente. As frequências deste aluno também serão apagadas, e observações vinculadas ficarão sem aluno identificado. Confirme apenas se tiver certeza.") },
        confirmButton = { TextButton(onClick = { confirming = false; delete() }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } })
}

@Composable private fun StudentForm(classroom: Classroom, back: () -> Unit, save: (String) -> Unit, onDirty: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    Heading("Adicionar aluno", classroom.name, back)
    Panel { Input("Nome completo do aluno", name, { if (it != name) { name = it; onDirty() } }); PrimaryButton("Salvar aluno") { save(name) } }
}

@Composable private fun DaySwitch(day: String, onDay: (String) -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(Modifier.size(42.dp).clickable { onDay(LocalDate.parse(day).minusDays(1).toString()) }, color = pale, shape = RoundedCornerShape(13.dp)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, Modifier.size(20.dp), blue) }
            }
            Text(friendlyDay(day), color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Surface(Modifier.size(42.dp).clickable { onDay(LocalDate.parse(day).plusDays(1).toString()) }, color = pale, shape = RoundedCornerShape(13.dp)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.OPEN, Modifier.size(18.dp), blue) }
            }
        }
    }
    Spacer(Modifier.height(15.dp))
}

@Composable private fun AttendanceEditor(data: TeacherSnapshot, classroom: Classroom, day: String, onDay: (String) -> Unit, onBack: () -> Unit, onAddStudent: () -> Unit, onSave: (Map<Long, String>) -> Unit, onDirty: () -> Unit = {}) {
    val students = data.students.filter { it.classroomId == classroom.id }
    val draft = remember(classroom.id, day, data.attendance) {
        mutableStateMapOf<Long, String>().apply {
            students.forEach { student -> this[student.id] = data.attendance.firstOrNull { it.studentId == student.id && it.date == day }?.status ?: "?" }
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
