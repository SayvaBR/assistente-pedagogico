package com.sayvabr.assistentepedagogico.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.TeacherBackupRestore
import com.sayvabr.assistentepedagogico.data.exportBackupPayload
import com.sayvabr.assistentepedagogico.data.restoreBackupAfterConfirmation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** SAF flows use the Activity-owned database. Never upload backups or copy external PDF bytes. */
@Composable
fun PlanningBackupPanel() {
    val context = LocalContext.current
    val store = LocalTeacherStore.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var pendingPayload by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<TeacherBackupRestore.Preview?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            feedback = null
            try {
                withContext(Dispatchers.IO) {
                    val bytes = store.exportBackupPayload().toByteArray(Charsets.UTF_8)
                    require(bytes.size <= 10 * 1024 * 1024) { "Backup excede o limite de 10 MB." }
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Não foi possível abrir o destino selecionado.")
                    stream.use { it.write(bytes); it.flush() }
                }
                feedback = "Backup exportado no documento escolhido. Guarde-o em um local seguro."
            } catch (e: Exception) {
                feedback = "Falha ao exportar: ${e.message ?: "verifique o destino e tente novamente"}."
            } finally { busy = false }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            feedback = null
            pendingPayload = null
            preview = null
            try {
                val (payload, summary) = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Não foi possível ler o documento selecionado.")
                    val buffer = ByteArray(8192)
                    val contents = ByteArrayOutputStream()
                    stream.use { input ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(contents.size() + count <= 10 * 1024 * 1024) {
                                "Backup excede o limite de 10 MB."
                            }
                            contents.write(buffer, 0, count)
                        }
                    }
                    val text = contents.toString(Charsets.UTF_8.name())
                    text to TeacherBackupRestore.preview(text)
                }
                pendingPayload = payload
                preview = summary
            } catch (e: Exception) {
                feedback = "Não foi possível preparar a restauração: ${e.message ?: "backup inválido"}. Seus dados atuais não foram alterados."
            } finally { busy = false }
        }
    }

    ApCard {
        Text("Backup e recuperação", color = ApColors.Navy, fontWeight = FontWeight.Black)
        Text("Exporte seus registros para um documento JSON. Nenhum dado é enviado a servidores pelo aplicativo.", color = ApColors.Navy)
        Spacer(Modifier.height(8.dp))
        Text("Atenção: o backup guarda referências e organização dos arquivos, mas NÃO copia os PDFs originais. Após restaurar, selecione novamente os documentos para autorizar o acesso.", color = ApColors.Pressed)
        Spacer(Modifier.height(12.dp))
        ApRaisedButton("Exportar backup", onClick = {
            if (!busy) exporter.launch("assistente-pedagogico-${LocalDate.now()}.json")
        }, secondary = true)
        Spacer(Modifier.height(8.dp))
        ApRaisedButton("Importar e recuperar", onClick = {
            if (!busy) importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
        }, secondary = true)
        if (busy) {
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = ApColors.Primary)
        }
        feedback?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = ApColors.Navy)
        }
    }

    val payload = pendingPayload
    val summary = preview
    if (payload != null && summary != null) AlertDialog(
        onDismissRequest = { if (!busy) { pendingPayload = null; preview = null } },
        title = { Text("Substituir todos os dados deste aparelho?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Prévia do backup selecionado:")
                Text("${summary.classrooms} turmas · ${summary.students} alunos · ${summary.lessons} planos")
                Text("${summary.appointments} compromissos · ${summary.attendance} presenças")
                Text("${summary.observations} observações · ${summary.files} referências de arquivos")
                Spacer(Modifier.height(8.dp))
                Text("Esta operação SUBSTITUI todos os registros atuais. Não combina bancos e não pode ser desfeita pelo aplicativo. Exporte um backup dos dados atuais antes de continuar.", fontWeight = FontWeight.Bold)
                Text("Os PDFs originais não estão incluídos. Autorizações de acesso aos documentos precisarão ser renovadas.")
                if (busy) CircularProgressIndicator(color = ApColors.Primary)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    feedback = null
                    try {
                        // The only callsite of this destructive API is the explicit dialog confirmation.
                        withContext(Dispatchers.IO) { store.restoreBackupAfterConfirmation(payload, confirmed = true) }
                        pendingPayload = null
                        preview = null
                        // Recreating closes this helper and refreshes ALL TeacherApp snapshots and routes.
                        val activity = context as? Activity
                        if (activity != null) activity.recreate()
                        else feedback = "Dados recuperados. Reinicie o aplicativo para atualizar todas as telas."
                    } catch (e: Exception) {
                        feedback = "Falha ao restaurar: ${e.message ?: "verifique o backup"}. O banco anterior foi preservado."
                        pendingPayload = null
                        preview = null
                    } finally { busy = false }
                }
            }) { Text("Substituir e restaurar") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { pendingPayload = null; preview = null }) { Text("Cancelar") } },
    )
}
