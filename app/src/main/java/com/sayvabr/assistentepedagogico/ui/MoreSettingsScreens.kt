package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.MoreSettings

@Composable
fun MoreSettingsIndex(
    profileName: String,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
) {
    SettingsHeading("Mais", "Ferramentas e recursos extras")
    SettingsProfileCard(profileName, onClick = { onOpen("profile") })
    Spacer(Modifier.height(10.dp))
    SettingsRow(ApGlyphKind.USERS, "Conta e perfil", "Seus dados e preferências") { onOpen("profile") }
    SettingsRow(ApGlyphKind.NOTE, "Notificações", "Alertas, lembretes e comunicados") { onOpen("notifications") }
    SettingsRow(ApGlyphKind.USERS, "Turmas", "Gerenciar turmas e disciplinas") { onOpen("classes") }
    SettingsRow(ApGlyphKind.STAR, "Aparência", "Tema, cores e tamanho da fonte") { onOpen("appearance") }
    SettingsRow(ApGlyphKind.CALENDAR, "Idioma", "Português (Brasil)") { onOpen("language") }
    SettingsRow(ApGlyphKind.CHECK, "Privacidade e segurança", "Seus dados sempre protegidos") { onOpen("privacy") }
    SettingsRow(ApGlyphKind.IMPORT, "Sincronização e backup", "Seus dados em segurança") { onOpen("backup") }
    SettingsRow(ApGlyphKind.NOTE, "Sobre o app", "Versão 0.0.1") { onOpen("about") }
    Spacer(Modifier.height(8.dp))
    SettingsActionButton("Sair da conta", ApGlyphKind.OPEN, onLogout)
}

@Composable
fun NotificationsSettings(initial: MoreSettings, onBack: () -> Unit, onSave: (MoreSettings) -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(initial.notifications) }
    var reminders by rememberSaveable { mutableStateOf(initial.reminders) }
    var summary by rememberSaveable { mutableStateOf(initial.dailySummary) }
    var updates by rememberSaveable { mutableStateOf(initial.productUpdates) }
    SettingsHeading("Notificações", "Alertas, lembretes e comunicados", onBack)
    SettingsCard {
        SettingsSwitch("Notificações", "Permitir avisos do aplicativo", enabled) {
            enabled = it
            if (!it) { reminders = false; summary = false; updates = false }
        }
        SettingsDivider()
        SettingsSwitch("Lembretes de compromissos", "Avise quando uma aula estiver próxima", reminders, enabled) { reminders = it }
        SettingsDivider()
        SettingsSwitch("Resumo diário", "Receba um resumo da sua rotina", summary, enabled) { summary = it }
        SettingsDivider()
        SettingsSwitch("Novidades do app", "Comunicados sobre melhorias", updates, enabled) { updates = it }
    }
    Spacer(Modifier.height(18.dp))
    SettingsPrimaryButton("Salvar preferências") {
        onSave(initial.copy(notifications = enabled, reminders = reminders, dailySummary = summary, productUpdates = updates))
    }
}

@Composable
fun AppearanceSettings(initial: MoreSettings, onBack: () -> Unit, onSave: (MoreSettings) -> Unit) {
    var theme by rememberSaveable { mutableStateOf(initial.theme) }
    var fontSize by rememberSaveable { mutableStateOf(initial.fontSize) }
    var reduceMotion by rememberSaveable { mutableStateOf(initial.reduceMotion) }
    SettingsHeading("Aparência", "Tema, cores e tamanho da fonte", onBack)
    SettingsCard {
        SettingsLabel("Tema")
        SettingsChoices(listOf("Claro", "Sistema", "Escuro"), theme) { theme = it }
        Spacer(Modifier.height(16.dp))
        SettingsLabel("Tamanho da fonte")
        SettingsChoices(listOf("Padrão", "Grande"), fontSize) { fontSize = it }
        Spacer(Modifier.height(8.dp))
        SettingsSwitch("Reduzir animações", "Diminui transições e movimento", reduceMotion) { reduceMotion = it }
    }
    Spacer(Modifier.height(18.dp))
    SettingsPrimaryButton("Salvar aparência") { onSave(initial.copy(theme = theme, fontSize = fontSize, reduceMotion = reduceMotion)) }
    Spacer(Modifier.height(8.dp))
    Text("As opções são salvas neste aparelho e podem ser alteradas a qualquer momento.", color = ApColors.Navy.copy(alpha = .7f), fontSize = 13.sp)
}

@Composable
fun LanguageSettings(initial: MoreSettings, onBack: () -> Unit, onSave: (MoreSettings) -> Unit) {
    var language by rememberSaveable { mutableStateOf(initial.language) }
    SettingsHeading("Idioma", "Escolha o idioma do aplicativo", onBack)
    SettingsCard {
        SettingsChoiceRow("Português (Brasil)", "Idioma atual", language == "Português (Brasil)") { language = "Português (Brasil)" }
        SettingsDivider()
        SettingsChoiceRow("English", "Disponível em uma próxima versão", false, enabled = false) { }
    }
    Spacer(Modifier.height(18.dp))
    SettingsPrimaryButton("Salvar idioma") { onSave(initial.copy(language = language)) }
}

@Composable
fun PrivacySettings(onBack: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    SettingsHeading("Privacidade e segurança", "Seus dados ficam sob seu controle", onBack)
    SettingsCard {
        SettingsInfoRow(ApGlyphKind.CHECK, "Dados locais", "Seus registros ficam no armazenamento privado do aparelho.")
        SettingsDivider()
        SettingsInfoRow(ApGlyphKind.IMPORT, "Sem sincronização automática", "O app não envia nomes, frequência ou observações para servidores.")
        SettingsDivider()
        SettingsInfoRow(ApGlyphKind.CHECK, "Backup sob seu comando", "Você escolhe quando exportar ou restaurar seus dados.")
    }
    Spacer(Modifier.height(18.dp))
    SettingsCard {
        Text("Excluir dados locais", color = Color(0xFFBA3045), fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(5.dp))
        Text("Remove turmas, alunos, planos, registros e preferências deste aparelho.", color = ApColors.Navy.copy(alpha = .72f), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        SettingsActionButton("Excluir tudo", ApGlyphKind.TRASH) { confirmDelete = true }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Excluir todos os dados?") },
        text = { Text("Esta ação remove os dados locais e não pode ser desfeita. Exporte um backup antes de continuar.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Excluir tudo") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
    )
}

@Composable
fun BackupSettings(onBack: () -> Unit) {
    SettingsHeading("Sincronização e backup", "Seus dados em segurança", onBack)
    PlanningBackupPanel()
}

@Composable
fun AboutSettings(onBack: () -> Unit) {
    SettingsHeading("Sobre o app", "Assistente Pedagógico", onBack)
    SettingsCard {
        Text("Assistente Pedagógico", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text("Versão 0.0.1", color = ApColors.Navy.copy(alpha = .7f), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        Text("Uma ferramenta local para organizar aulas, turmas, frequência e registros pedagógicos.", color = ApColors.Navy, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(14.dp))
        SettingsInfoRow(ApGlyphKind.CHECK, "Privacidade", "Sem anúncios e sem coleta remota de dados pedagógicos.")
        SettingsDivider()
        SettingsInfoRow(ApGlyphKind.NOTE, "Suporte", "Consulte a documentação e mantenha o app atualizado.")
    }
}

@Composable
private fun SettingsHeading(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) {
            Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = back)
                .semantics { contentDescription = "Voltar" }, contentAlignment = Alignment.Center) {
                ApGlyph(ApGlyphKind.BACK, Modifier.size(25.dp), ApColors.Navy)
            }
            Spacer(Modifier.size(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = ApColors.Navy, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black)
            subtitle?.let { Text(it, color = ApColors.Navy.copy(alpha = .72f), fontSize = 14.sp) }
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SettingsProfileCard(name: String, onClick: () -> Unit) {
    SettingsCard(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ApIconBadge(ApGlyphKind.USERS, Modifier.size(58.dp), emphasized = true)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("Professor(a)", color = ApColors.Navy.copy(alpha = .72f), fontSize = 13.sp)
                Text("Dados e preferências", color = ApColors.Navy.copy(alpha = .72f), fontSize = 12.sp)
            }
            ApGlyph(ApGlyphKind.CHEVRON, Modifier.size(20.dp), ApColors.Navy.copy(alpha = .55f))
        }
    }
}

@Composable
private fun SettingsRow(glyph: ApGlyphKind, title: String, detail: String, onClick: () -> Unit) {
    SettingsCard(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ApIconBadge(glyph, Modifier.size(44.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(detail, color = ApColors.Navy.copy(alpha = .68f), fontSize = 12.sp)
            }
            ApGlyph(ApGlyphKind.CHEVRON, Modifier.size(18.dp), ApColors.Navy.copy(alpha = .52f))
        }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun SettingsActionButton(label: String, glyph: ApGlyphKind, onClick: () -> Unit) {
    SettingsCard(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            ApGlyph(glyph, Modifier.size(20.dp), ApPalette.Action)
            Spacer(Modifier.size(8.dp))
            Text(label, color = ApPalette.Action, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SettingsPrimaryButton(label: String, onClick: () -> Unit) {
    ApRaisedButton(label, onClick, glyph = ApGlyphKind.CHECK)
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Surface(modifier.fillMaxWidth(), color = ApColors.White,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsSwitch(label: String, detail: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = ApColors.Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(detail, color = ApColors.Navy.copy(alpha = .68f), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SettingsLabel(label: String) { Text(label, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 14.sp) }

@Composable
private fun SettingsChoices(options: List<String>, current: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option == current
            androidx.compose.material3.Surface(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable { onPick(option) },
                color = if (selected) ApColors.Primary else ApColors.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (selected) ApColors.Primary else ApPalette.Outline),
            ) { Box(contentAlignment = Alignment.Center) { Text(option, color = if (selected) ApColors.White else ApColors.Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
        }
    }
}

@Composable
private fun SettingsChoiceRow(label: String, detail: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = enabled, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = ApColors.Navy.copy(alpha = if (enabled) 1f else .45f), fontWeight = FontWeight.Bold)
            Text(detail, color = ApColors.Navy.copy(alpha = if (enabled) .68f else .4f), fontSize = 12.sp)
        }
        Text(if (selected) "✓" else "", color = ApPalette.Action, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

@Composable
private fun SettingsInfoRow(glyph: ApGlyphKind, title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        ApIconBadge(glyph, Modifier.size(40.dp))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ApColors.Navy, fontWeight = FontWeight.Black)
            Text(detail, color = ApColors.Navy.copy(alpha = .7f), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun SettingsDivider() { HorizontalDivider(color = ApPalette.Outline, modifier = Modifier.padding(vertical = 7.dp)) }
