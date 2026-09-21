package com.sayvabr.assistentepedagogico.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.ClassroomRules
import com.sayvabr.assistentepedagogico.data.TeacherStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entry UI. Remote authentication is deliberately unavailable until a provider is integrated. */
@Composable
fun FirstAccessFlow(store: TeacherStore) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("first_access", 0) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var entered by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(prefs.getString("step", "welcome") ?: "welcome") }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "") ?: "") }
    var stage by rememberSaveable { mutableStateOf(prefs.getString("stage", "") ?: "") }
    var classroom by rememberSaveable { mutableStateOf(prefs.getString("classroom", "") ?: "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        loading = true
        runCatching { withContext(Dispatchers.IO) { store.read().profile != null } }
            .onSuccess { entered = it }.onFailure { error = "Não foi possível abrir seus dados. Tente novamente." }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    fun go(next: String) {
        error = null
        step = next
        prefs.edit().putString("step", if (next in listOf("login", "signup", "done")) "welcome" else next).apply()
    }
    fun back() { go(when (step) { "stage" -> "name"; "class" -> "stage"; "name" -> "welcome"; else -> "welcome" }) }
    BackHandler(enabled = !entered && !loading && step != "welcome") { if (!busy && step != "done") back() }
    if (entered) { TeacherApp(store); return }
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                EntryArt(ApGlyphKind.DOCUMENT)
                Text("Assistente\nPedagógico", color = ApColors.Navy, fontSize = 30.sp,
                    lineHeight = 35.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                CircularProgressIndicator(Modifier.size(24.dp), color = ApColors.Pressed)
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().background(ApColors.Sky)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step != "welcome" && step != "done") TextButton(onClick = { back() }, enabled = !busy) {
                ApGlyph(ApGlyphKind.BACK, Modifier.size(20.dp)); Text("Voltar", color = ApColors.Navy)
            } else Spacer(Modifier.width(12.dp))
            Spacer(Modifier.weight(1f))
            Text("ASSISTENTE PEDAGÓGICO", fontSize = 11.sp, letterSpacing = 1.sp,
                fontWeight = FontWeight.ExtraBold, color = ApColors.Navy)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).fillMaxWidth()
            .widthIn(max = 520.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (step in listOf("name", "stage", "class")) {
                val index = listOf("name", "stage", "class").indexOf(step)
                Text("SEU ESPAÇO · ${index + 1} DE 3", color = ApColors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { n -> Box(Modifier.weight(1f).height(6.dp).background(
                        if (n <= index) ApColors.Pressed else ApPalette.Outline, RoundedCornerShape(6.dp))) }
                }
            }
            when (step) {
                "welcome" -> {
                    EntryArt(ApGlyphKind.DOCUMENT)
                    EntryTitle("Menos tempo organizando.\nMais clareza para ensinar.")
                    Text("Suas turmas, aulas e ideias encontram um lugar. Comece com o que você precisa hoje.", color = ApColors.Navy, fontSize = 17.sp, lineHeight = 25.sp)
                    ApCard {
                        EntryBenefit(ApGlyphKind.CALENDAR, "Planeje com clareza", "Sua rotina organizada, no seu ritmo.")
                        Spacer(Modifier.height(16.dp))
                        EntryBenefit(ApGlyphKind.USERS, "Cuide de cada turma", "Chamada e registros sempre por perto.")
                    }
                    ApRaisedButton("Preparar meu espaço", { go("name") })
                    TextButton(onClick = { go("signup") }, modifier = Modifier.fillMaxWidth()) { Text("Criar conta", color = ApColors.Navy) }
                    TextButton(onClick = { go("login") }, modifier = Modifier.fillMaxWidth()) { Text("Já tenho uma conta", color = ApColors.Navy) }
                    Text("Você pode começar neste aparelho sem criar uma conta.", color = ApColors.Navy, fontSize = 13.sp)
                }
                "login", "signup" -> EntryAuth(
                    signup = step == "signup",
                    switch = { go(if (step == "signup") "login" else "signup") },
                    local = { localName ->
                        if (!localName.isNullOrBlank()) {
                            name = localName
                            prefs.edit().putString("name", localName).apply()
                        }
                        go("name")
                    },
                )
                "name" -> {
                    EntryArt(ApGlyphKind.USERS)
                    EntryTitle("Como podemos\nchamar você?")
                    Text("Seu nome profissional deixa este espaço com a sua cara. Você pode alterá-lo depois.", color = ApColors.Navy)
                    EntryField("Seu nome", name) { name = it; prefs.edit().putString("name", it).apply() }
                    ApRaisedButton("Continuar", { go("stage") }, enabled = name.trim().length >= 2)
                    TextButton(onClick = { name = "Professor(a)"; prefs.edit().putString("name", name).apply(); go("stage") }) { Text("Definir meu nome depois", color = ApColors.Navy) }
                }
                "stage" -> {
                    EntryTitle("Em qual etapa\nvocê ensina?")
                    Text("Vamos usar essa escolha na sua primeira turma. Outras turmas podem ter etapas diferentes.", color = ApColors.Navy)
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Educação Infantil", "Ensino Fundamental", "Ensino Médio").forEach { option ->
                            Surface(color = ApColors.White, shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(if (stage == option) 2.dp else 1.dp, if (stage == option) ApColors.Pressed else ApPalette.Outline),
                                modifier = Modifier.fillMaxWidth().selectable(stage == option, role = Role.RadioButton,
                                    onClick = { stage = option; prefs.edit().putString("stage", option).apply() })) {
                                Row(Modifier.padding(18.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(option, Modifier.weight(1f), color = ApColors.Navy, fontWeight = FontWeight.Bold)
                                    RadioButton(selected = stage == option, onClick = null)
                                }
                            }
                        }
                    }
                    ApRaisedButton("Continuar", { go("class") }, enabled = stage.isNotEmpty())
                }
                "class" -> {
                    EntryArt(ApGlyphKind.USERS)
                    EntryTitle("Sua primeira turma.\nSeu primeiro passo.")
                    Text("Os alunos podem ser adicionados depois. Agora, basta dar um nome à turma.", color = ApColors.Navy)
                    EntryField("Nome da turma", classroom) { classroom = it; prefs.edit().putString("classroom", it).apply() }
                    Text(stage, color = ApColors.Navy, fontWeight = FontWeight.Bold)
                    ApRaisedButton(if (busy) "Preparando…" else "Criar meu espaço", {
                        busy = true; error = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                val db = store.writableDatabase
                                db.beginTransaction()
                                try {
                                    check(store.read().profile == null) { "Seu perfil já existe. Reabra o aplicativo." }
                                    store.saveProfile(name)
                                    store.createClass(classroom, stage, ClassroomRules.unspecifiedShift)
                                    db.setTransactionSuccessful()
                                } finally { db.endTransaction() }
                            } }.onSuccess { prefs.edit().clear().apply(); go("done") }
                                .onFailure { error = it.message ?: "Não foi possível salvar. Seus campos foram preservados." }
                            busy = false
                        }
                    }, enabled = !busy && classroom.isNotBlank() && name.trim().length >= 2 && stage.isNotBlank())
                    Text("Seu espaço será salvo neste aparelho. Nenhum aluno precisa ser cadastrado agora.", color = ApColors.Navy, fontSize = 13.sp)
                }
                "done" -> {
                    EntryArt(ApGlyphKind.CHECK)
                    EntryTitle("Tudo pronto,\n${name.substringBefore(' ')}.")
                    Text("Sua turma foi salva. Você já pode organizar a primeira aula e adicionar seus alunos.", color = ApColors.Navy, fontSize = 17.sp, lineHeight = 25.sp)
                    ApCard { Text(classroom, color = ApColors.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(stage, color = ApColors.Navy) }
                    ApRaisedButton("Entrar no meu espaço", { entered = true })
                }
            }
            error?.let { message -> ApCard {
                Text(message, color = ApColors.Navy)
                if (step == "welcome") TextButton(onClick = { scope.launch { error = null; load() } }) { Text("Tentar novamente") }
            } }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EntryAuth(signup: Boolean, switch: () -> Unit, local: (String?) -> Unit) {
    // Credentials never enter saved state, preferences, logs or a simulated authentication session.
    // The provider is intentionally not faked: this Android-only, local-first release has no
    // account server yet. The form still validates inputs and gives the user a clear next step.
    var fullName by rememberSaveable(signup) { mutableStateOf("") }
    var email by rememberSaveable(signup) { mutableStateOf("") }
    var password by rememberSaveable(signup) { mutableStateOf("") }
    var passwordVisible by rememberSaveable(signup) { mutableStateOf(false) }
    var submitted by rememberSaveable(signup) { mutableStateOf(false) }
    var remoteMessage by remember { mutableStateOf<String?>(null) }

    val normalizedEmail = email.trim()
    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
    val passwordValid = password.length >= 8
    val canSubmit = emailValid && passwordValid && (!signup || fullName.trim().length >= 2)
    val emailError = submitted && !emailValid
    val passwordError = submitted && !passwordValid
    val nameError = submitted && signup && fullName.trim().length < 2

    EntryTitle(if (signup) "Criar sua conta" else "Bem-vinda de volta!")
    Text(
        if (signup) "É rápido e gratuito" else "Que bom ter você por aqui.",
        color = ApColors.Navy,
        fontSize = 16.sp,
    )
    if (signup) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; submitted = false },
            label = { Text("Nome completo") },
            placeholder = { Text("Seu nome") },
            singleLine = true,
            isError = nameError,
            supportingText = if (nameError) ({ Text("Informe seu nome completo.") }) else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
    }
    OutlinedTextField(
        value = email,
        onValueChange = { email = it; submitted = false },
        label = { Text("E-mail") },
        placeholder = { Text("seu@email.com") },
        singleLine = true,
        isError = emailError,
        supportingText = if (emailError) ({ Text("Digite um e-mail válido.") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; submitted = false },
        label = { Text("Senha") },
        placeholder = { Text(if (signup) "Crie uma senha" else "Sua senha") },
        singleLine = true,
        isError = passwordError,
        supportingText = if (passwordError) ({ Text("Use pelo menos 8 caracteres.") }) else null,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "Ocultar" else "Mostrar", fontSize = 12.sp, color = ApColors.Pressed)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
    if (!signup) {
        TextButton(
            onClick = { remoteMessage = "A recuperação de senha online será disponibilizada quando o provedor de contas for integrado. Seus dados locais continuam acessíveis sem login." },
            modifier = Modifier.align(Alignment.End),
        ) { Text("Esqueceu a senha?", color = ApColors.Pressed, fontSize = 13.sp) }
    }
    ApRaisedButton(
        label = if (signup) "Criar conta" else "Entrar",
        onClick = {
            submitted = true
            if (canSubmit) {
                remoteMessage = "Contas online ainda não estão disponíveis nesta versão. Para continuar agora, use o botão abaixo e prepare seu espaço neste aparelho."
            }
        },
        enabled = !submitted || canSubmit,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = ApPalette.Outline)
        Text("ou", Modifier.padding(horizontal = 12.dp), color = ApColors.Navy.copy(alpha = .65f), fontSize = 13.sp)
        HorizontalDivider(Modifier.weight(1f), color = ApPalette.Outline)
    }
    ApRaisedButton("Continuar neste aparelho", { local(fullName.trim().ifBlank { null }) }, secondary = true)
    TextButton(
        onClick = { remoteMessage = "Login com Google estará disponível quando as contas online forem integradas. Você pode começar sem conta neste aparelho." },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continuar com Google", color = ApColors.Pressed) }
    Text(
        "Este app funciona no Android sem conta e sem enviar dados pedagógicos para servidores.",
        color = ApColors.Navy.copy(alpha = .72f),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = switch, modifier = Modifier.fillMaxWidth()) {
        Text(if (signup) "Já tem uma conta? Entrar" else "Criar uma conta", color = ApColors.Pressed)
    }
    remoteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { remoteMessage = null },
            title = { Text("Contas online") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { remoteMessage = null }) { Text("Entendi") } },
        )
    }
}

@Composable private fun EntryTitle(text: String) {
    Text(text, Modifier.semantics { heading() }, color = ApColors.Navy, fontSize = 28.sp,
        lineHeight = 34.sp, fontWeight = FontWeight.Bold)
}

@Composable private fun EntryField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().background(ApColors.White, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp))
}

@Composable private fun EntryArt(glyph: ApGlyphKind) {
    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(104.dp, 96.dp).background(ApPalette.Outline, RoundedCornerShape(28.dp)))
        Surface(Modifier.offset(y = (-6).dp).size(104.dp, 96.dp), color = ApColors.White,
            shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
            Box(contentAlignment = Alignment.Center) { ApGlyph(glyph, Modifier.size(48.dp), ApColors.Pressed) }
        }
        Surface(Modifier.align(Alignment.BottomEnd).padding(end = 64.dp, bottom = 4.dp).size(32.dp),
            color = ApColors.Primary, shape = RoundedCornerShape(14.dp)) {
            Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.CHECK, Modifier.size(18.dp), ApColors.Navy) }
        }
    }
}

@Composable private fun EntryBenefit(glyph: ApGlyphKind, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ApGlyph(glyph, Modifier.size(30.dp), ApColors.Pressed)
        Column { Text(title, color = ApColors.Navy, fontWeight = FontWeight.Bold); Text(detail, color = ApColors.Navy, fontSize = 13.sp) }
    }
}
