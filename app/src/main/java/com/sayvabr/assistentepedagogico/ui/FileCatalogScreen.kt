package com.sayvabr.assistentepedagogico.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.SavedFile

/** Document links obtained through SAF; files still belong to their original providers. */
@Composable
fun FileCatalogScreen(
    files: List<SavedFile>,
    importFile: () -> Unit,
    openFile: (SavedFile) -> Unit,
    renameFile: (SavedFile, String) -> Unit,
    removeFile: (SavedFile) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var alphabetical by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableLongStateOf(-1L) }
    var renaming by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf("") }
    val selected = files.firstOrNull { it.id == selectedId }
    val matches = files.filter { it.name.contains(query.trim(), ignoreCase = true) }
        .let { result ->
            if (alphabetical) result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            else result.sortedByDescending { it.id }
        }

    BackHandler(enabled = selected != null && !renaming && !removing) { selectedId = -1L }
    if (selected != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(43.dp).clickable { selectedId = -1L }
                    .semantics { contentDescription = "Voltar ao catálogo" },
                shape = RoundedCornerShape(14.dp), color = ApColors.White,
                border = BorderStroke(1.dp, Color(0xFFCDE8F8)),
            ) { Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, color = ApColors.Primary) } }
            Spacer(Modifier.width(12.dp))
            Column {
                ApEyebrow("Biblioteca / documento")
                Text("Detalhes do arquivo", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 23.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        ApCard {
            Box(
                Modifier.size(68.dp).background(ApColors.Sky, RoundedCornerShape(19.dp)),
                contentAlignment = Alignment.Center,
            ) { ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(39.dp), color = ApColors.Pressed) }
            Spacer(Modifier.height(17.dp))
            Text(selected.name, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 23.sp, lineHeight = 28.sp)
            Spacer(Modifier.height(8.dp))
            ApEyebrow("Referência do documento")
            Spacer(Modifier.height(9.dp))
            Text(
                "Este aplicativo guarda o vínculo com o documento original. Não cria uma cópia nem envia o conteúdo à nuvem.",
                color = ApColors.Navy, fontSize = 14.sp, lineHeight = 21.sp,
            )
            Spacer(Modifier.height(21.dp))
            ApRaisedButton("Abrir documento", onClick = { openFile(selected) }, glyph = ApGlyphKind.OPEN)
            Spacer(Modifier.height(10.dp))
            ApRaisedButton("Renomear no catálogo", onClick = {
                draftName = selected.name
                renaming = true
            }, glyph = ApGlyphKind.EDIT, secondary = true)
            Spacer(Modifier.height(10.dp))
            ApRaisedButton("Remover vínculo", onClick = { removing = true }, glyph = ApGlyphKind.TRASH, secondary = true)
        }
        Spacer(Modifier.height(14.dp))
        Text("Remover um vínculo nunca apaga o arquivo original.", fontSize = 12.sp, color = ApColors.Navy)
    } else {
        ApEyebrow("Seu espaço de trabalho")
        Spacer(Modifier.height(4.dp))
        Text("Arquivos", fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
        Spacer(Modifier.height(4.dp))
        Text("Tudo à mão para preparar suas aulas.", fontSize = 14.sp, color = ApColors.Navy)
        Spacer(Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(), color = ApColors.Primary,
            shape = RoundedCornerShape(23.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(51.dp).background(ApColors.White, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center,
                    ) { ApGlyph(ApGlyphKind.FOLDER, Modifier.size(29.dp), color = ApColors.Pressed) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SUA BIBLIOTECA", fontSize = 11.sp, color = ApColors.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("${files.size} ${if (files.size == 1) "documento vinculado" else "documentos vinculados"}",
                            fontWeight = FontWeight.Black, fontSize = 18.sp, color = ApColors.White)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Text("Adicione materiais de qualquer pasta acessível pelo Android. Seus originais continuam onde estão.",
                    color = ApColors.White, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(17.dp))
                ApRaisedButton("Importar documento", onClick = importFile, glyph = ApGlyphKind.IMPORT, secondary = true)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Explorar documentos", fontWeight = FontWeight.Black, fontSize = 20.sp, color = ApColors.Navy, modifier = Modifier.weight(1f))
            Surface(color = ApColors.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFCDE8F8))) {
                Text("${matches.size}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = ApColors.Pressed,
                    fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            leadingIcon = { ApGlyph(ApGlyphKind.SEARCH, Modifier.size(22.dp), color = ApColors.Pressed) },
            label = { Text("Buscar por nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ApColors.Primary,
                unfocusedBorderColor = Color(0xFFCDE8F8),
                focusedContainerColor = ApColors.White,
                unfocusedContainerColor = ApColors.White,
            ),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { alphabetical = !alphabetical }, modifier = Modifier.align(Alignment.End)) {
            Text(if (alphabetical) "Ordem: A–Z · alterar" else "Ordem: recentes · alterar", color = ApColors.Navy,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))

        if (matches.isEmpty()) {
            ApCard {
                Box(Modifier.size(55.dp).background(ApColors.Sky, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    ApGlyph(if (query.isBlank()) ApGlyphKind.DOCUMENT else ApGlyphKind.SEARCH, Modifier.size(30.dp), color = ApColors.Pressed)
                }
                Spacer(Modifier.height(14.dp))
                Text(if (query.isBlank()) "Sua biblioteca começa aqui" else "Nenhum arquivo encontrado",
                    color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(if (query.isBlank()) "Toque em Importar documento para vincular seu primeiro material."
                    else "Tente outro nome ou limpe sua pesquisa.", color = ApColors.Navy, fontSize = 14.sp, lineHeight = 20.sp)
                if (query.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    ApRaisedButton("Limpar pesquisa", onClick = { query = "" }, secondary = true)
                }
            }
        }
        matches.forEach { file ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { selectedId = file.id }
                    .semantics { contentDescription = "Ver detalhes de ${file.name}" },
                color = ApColors.White, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFCDE8F8)),
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(53.dp).background(ApColors.Sky, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(29.dp), color = ApColors.Pressed)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ApColors.Navy)
                        Spacer(Modifier.height(4.dp))
                        Text("Toque para abrir e organizar", fontSize = 12.sp, color = ApColors.Navy.copy(alpha = .75f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = ApColors.Primary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Os documentos originais permanecem no provedor de origem, mesmo ao remover um vínculo daqui.",
            fontSize = 12.sp, color = ApColors.Navy, lineHeight = 17.sp)
    }

    if (renaming && selected != null) AlertDialog(
        onDismissRequest = { renaming = false },
        title = { Text("Renomear no catálogo") },
        text = {
            Column {
                OutlinedTextField(value = draftName, onValueChange = { if (it.length <= 180) draftName = it },
                    label = { Text("Nome exibido") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("O nome do arquivo original não será alterado.", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (draftName.isNotBlank()) { renaming = false; renameFile(selected, draftName) }
            }, enabled = draftName.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancelar") } },
    )
    if (removing && selected != null) AlertDialog(
        onDismissRequest = { removing = false },
        title = { Text("Remover do aplicativo?") },
        text = { Text("Só o vínculo será removido do catálogo. O arquivo original NÃO será apagado. Você poderá importá-lo novamente.") },
        confirmButton = { TextButton(onClick = {
            removing = false
            selectedId = -1L
            removeFile(selected)
        }) { Text("Remover vínculo") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancelar") } },
    )
}
