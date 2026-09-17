package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.LibraryFile
import com.sayvabr.assistentepedagogico.data.SavedFile

/** Actual offline file catalog: one activity-owned store, never a copied/deleted source document. */
@Composable
fun FileCatalogScreen(
    files: List<SavedFile>,
    importFile: () -> Unit,
    openFile: (SavedFile) -> Unit,
    renameFile: (SavedFile, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") removeFile: (SavedFile) -> Unit,
) {
    val store = LocalTeacherStore.current
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var view by rememberSaveable { mutableStateOf("Todos") }
    var alphabetical by rememberSaveable { mutableStateOf(false) }
    var folderFilter by rememberSaveable { mutableLongStateOf(-2L) }
    var selectedId by rememberSaveable { mutableLongStateOf(-1L) }
    var renameOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var purgeOpen by remember { mutableStateOf(false) }
    var folderChooser by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var folderDeleteDialog by remember { mutableStateOf(false) }
    var editingFolderId by remember { mutableLongStateOf(-1L) }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    // The app's SAF import still calls TeacherApp.commit: changes to `files` invalidate this
    // read. Local catalog operations also increment revision, so trash/folders update at once.
    val catalog = remember(store, files, revision) { store.libraryFiles() }
    val folders = remember(store, files, revision) { store.fileFolders() }
    val selected = catalog.firstOrNull { it.id == selectedId }
    val active = catalog.filter { it.trashedAt == null }
    val matches = catalog.filter { file ->
        val inView = when (view) {
            "Favoritos" -> file.trashedAt == null && file.favorite
            "Lixeira" -> file.trashedAt != null
            else -> file.trashedAt == null
        }
        val inFolder = view == "Lixeira" || folderFilter == -2L ||
            (folderFilter == -1L && file.folderId == null) || file.folderId == folderFilter
        inView && inFolder && file.name.contains(query.trim(), ignoreCase = true)
    }.let { result -> if (alphabetical) result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }) else result.sortedByDescending { it.id } }

    fun saved(file: LibraryFile) = SavedFile(file.id, file.name, file.uri, file.folderId, file.favorite, file.trashedAt, file.accessState)
    fun perform(action: () -> Unit): Boolean = runCatching(action).fold(
        onSuccess = { revision++; true },
        onFailure = { message = it.message ?: "Não foi possível concluir. Seus documentos foram preservados."; false },
    )

    BackHandler(enabled = selected != null && !renameOpen && !trashOpen && !purgeOpen && !folderChooser) { selectedId = -1L }

    if (selected == null) {
        ApEyebrow("Seu espaço de trabalho")
        Spacer(Modifier.height(4.dp))
        Text("Arquivos", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp)
        Spacer(Modifier.height(3.dp))
        Text("Organize seus materiais sem alterar os originais.", color = ApColors.Navy, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(17.dp))
        Surface(Modifier.fillMaxWidth(), color = ApColors.Primary, shape = RoundedCornerShape(ApShapeToken.Hero)) {
            Column(Modifier.padding(ApSpace.Lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(53.dp).background(ApColors.White, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                        ApGlyph(ApGlyphKind.FOLDER, Modifier.size(32.dp), ApColors.Pressed)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SUA BIBLIOTECA", color = ApColors.White, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text("${active.size} ${if (active.size == 1) "documento" else "documentos"}", color = ApColors.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Pastas, favoritos e lixeira ficam no aparelho. Os documentos continuam no local original.",
                    color = ApColors.White, fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(15.dp))
                ApRaisedButton("Importar documento", onClick = importFile, glyph = ApGlyphKind.IMPORT, secondary = true)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Todos", "Favoritos", "Lixeira").forEach { label ->
                val chosen = view == label
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = ApSizeToken.MinTouchTarget).clickable {
                        view = label
                        folderFilter = -2L
                    }.semantics { contentDescription = "Exibir $label" },
                    shape = RoundedCornerShape(15.dp),
                    color = if (chosen) ApColors.Primary else ApColors.White,
                    border = BorderStroke(1.dp, if (chosen) ApColors.Pressed else ApPalette.Outline),
                ) {
                    Row(Modifier.padding(horizontal = 5.dp, vertical = 13.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (label == "Favoritos") {
                            ApGlyph(ApGlyphKind.STAR, Modifier.size(16.dp), if (chosen) ApColors.White else ApColors.Pressed)
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(label, color = if (chosen) ApColors.White else ApColors.Navy,
                            fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        if (view != "Lixeira") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pastas", modifier = Modifier.weight(1f), fontSize = 20.sp, color = ApColors.Navy, fontWeight = FontWeight.Black)
                TextButton(onClick = { editingFolderId = -1L; nameDraft = ""; folderDialog = true }) {
                    ApGlyph(ApGlyphKind.PLUS, Modifier.size(18.dp), ApColors.Pressed)
                    Spacer(Modifier.width(3.dp))
                    Text("Nova pasta", color = ApColors.Pressed, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = folderFilter == -2L, onClick = { folderFilter = -2L }, label = { Text("Todas") })
                FilterChip(selected = folderFilter == -1L, onClick = { folderFilter = -1L }, label = { Text("Sem pasta") })
                folders.forEach { folder ->
                    FilterChip(selected = folderFilter == folder.id, onClick = { folderFilter = folder.id }, label = { Text(folder.name) })
                }
            }
            if (folderFilter > 0L && folders.any { it.id == folderFilter }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        editingFolderId = folderFilter
                        nameDraft = folders.first { it.id == folderFilter }.name
                        folderDialog = true
                    }) { Text("Renomear pasta") }
                    TextButton(onClick = { folderDeleteDialog = true }) { Text("Excluir pasta") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar documentos") }, singleLine = true,
            leadingIcon = { ApGlyph(ApGlyphKind.SEARCH, Modifier.size(21.dp), ApColors.Pressed) },
            shape = RoundedCornerShape(ApShapeToken.Medium),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ApColors.White,
                unfocusedContainerColor = ApColors.White, focusedBorderColor = ApColors.Primary,
                unfocusedBorderColor = ApPalette.Outline),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${matches.size} ${if (matches.size == 1) "resultado" else "resultados"}", color = ApColors.Navy, fontSize = 13.sp)
            TextButton(onClick = { alphabetical = !alphabetical }) {
                Text(if (alphabetical) "Ordem: A–Z" else "Ordem: recentes", color = ApColors.Pressed, fontSize = 13.sp)
            }
        }
        if (matches.isEmpty()) ApCard {
            Box(Modifier.size(54.dp).background(ApColors.Sky, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                ApGlyph(if (query.isBlank()) ApGlyphKind.FOLDER else ApGlyphKind.SEARCH, Modifier.size(30.dp), ApColors.Pressed)
            }
            Spacer(Modifier.height(10.dp))
            Text(when {
                query.isNotBlank() -> "Nenhum documento encontrado"
                view == "Lixeira" -> "Sua lixeira está vazia"
                view == "Favoritos" -> "Seus favoritos aparecerão aqui"
                else -> "Ainda não há documentos nesta seleção"
            }, color = ApColors.Navy, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text("Seus materiais permanecem no provedor de origem.", fontSize = 13.sp, color = ApColors.Navy)
            if (query.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Limpar pesquisa", onClick = { query = "" }, secondary = true)
            }
        }
        matches.forEach { file ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { selectedId = file.id }
                    .semantics { contentDescription = "Ver detalhes de ${file.name}" },
                color = ApColors.White, shape = RoundedCornerShape(ApShapeToken.Card),
                border = BorderStroke(1.dp, ApPalette.Outline),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(ApColors.Sky, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(28.dp), ApColors.Pressed)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, color = ApColors.Navy,
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(when {
                            file.trashedAt != null -> "Na lixeira · restaurável"
                            file.accessState == "revoked" -> "Acesso indisponível · vínculo preservado"
                            else -> folders.firstOrNull { it.id == file.folderId }?.name ?: "Sem pasta"
                        }, color = ApColors.Navy, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (file.favorite && file.trashedAt == null) ApGlyph(ApGlyphKind.STAR, Modifier.size(19.dp), ApColors.Pressed)
                    Spacer(Modifier.width(8.dp))
                    ApGlyph(ApGlyphKind.OPEN, Modifier.size(20.dp), ApColors.Pressed)
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("A lixeira exclui apenas referências locais, nunca o arquivo original.", color = ApColors.Navy, fontSize = 12.sp)
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp).clickable { selectedId = -1L }.semantics { contentDescription = "Voltar à biblioteca" },
                color = ApColors.White, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
                Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, color = ApColors.Pressed) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                ApEyebrow("Biblioteca / documento")
                Text("Detalhes", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        ApCard {
            Box(Modifier.size(66.dp).background(ApColors.Sky, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(37.dp), ApColors.Pressed)
            }
            Spacer(Modifier.height(14.dp))
            Text(selected.name, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("Pasta: ${folders.firstOrNull { it.id == selected.folderId }?.name ?: "Sem pasta"}", color = ApColors.Navy, fontSize = 13.sp)
            if (selected.accessState == "revoked") {
                Spacer(Modifier.height(10.dp))
                Text("Acesso indisponível. Sua referência foi preservada; importe o documento novamente para recuperar a permissão.",
                    color = ApColors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(13.dp))
            Text("A biblioteca guarda apenas o vínculo local. O documento original permanece no provedor Android.",
                color = ApColors.Navy, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(17.dp))
            if (selected.trashedAt == null) {
                ApRaisedButton("Abrir documento", onClick = { openFile(saved(selected)) }, glyph = ApGlyphKind.OPEN)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton(if (selected.favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    onClick = { perform { store.setFileFavorite(selected.id, !selected.favorite) } },
                    glyph = ApGlyphKind.STAR, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Mover para pasta", onClick = { folderChooser = true }, glyph = ApGlyphKind.FOLDER, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Renomear no catálogo", onClick = { nameDraft = selected.name; renameOpen = true }, glyph = ApGlyphKind.EDIT, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Verificar acesso", onClick = {
                    perform {
                        val accessible = runCatching {
                            context.contentResolver.openFileDescriptor(Uri.parse(selected.uri), "r")?.use {} ?: error("Documento indisponível")
                        }.isSuccess
                        store.setFileAccessState(selected.id, if (accessible) "available" else "revoked")
                        message = if (accessible) "Acesso confirmado." else "Acesso indisponível. Seus dados foram preservados."
                    }
                }, glyph = ApGlyphKind.CHECK, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Enviar à lixeira", onClick = { trashOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            } else {
                ApRaisedButton("Restaurar documento", onClick = {
                    if (perform { store.restoreFile(selected.id) }) { selectedId = -1L; view = "Todos" }
                }, glyph = ApGlyphKind.RESTORE)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Excluir vínculo definitivamente", onClick = { purgeOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            }
        }
    }

    if (renameOpen && selected != null) AlertDialog(
        onDismissRequest = { renameOpen = false }, title = { Text("Renomear no catálogo") },
        text = { OutlinedTextField(nameDraft, { if (it.length <= 180) nameDraft = it }, label = { Text("Nome exibido") }, singleLine = true) },
        confirmButton = { TextButton(enabled = nameDraft.isNotBlank(), onClick = {
            if (nameDraft.isNotBlank()) { renameOpen = false; renameFile(saved(selected), nameDraft) }
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Cancelar") } },
    )
    if (trashOpen && selected != null) AlertDialog(
        onDismissRequest = { trashOpen = false }, title = { Text("Enviar à lixeira?") },
        text = { Text("Você pode restaurar este vínculo depois. O documento e a permissão de acesso serão preservados.") },
        confirmButton = { TextButton(onClick = {
            if (perform { store.trashFile(selected.id) }) { trashOpen = false; selectedId = -1L; view = "Lixeira" }
        }) { Text("Enviar à lixeira") } },
        dismissButton = { TextButton(onClick = { trashOpen = false }) { Text("Cancelar") } },
    )
    if (purgeOpen && selected != null) AlertDialog(
        onDismissRequest = { purgeOpen = false }, title = { Text("Excluir vínculo definitivamente?") },
        text = { Text("Essa ação não pode ser desfeita no aplicativo. O documento original continuará no provedor.") },
        confirmButton = { TextButton(onClick = {
            if (perform {
                store.permanentlyRemoveFileReference(selected.id)
                runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(selected.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }) { purgeOpen = false; selectedId = -1L }
        }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { purgeOpen = false }) { Text("Cancelar") } },
    )
    if (folderChooser && selected != null) AlertDialog(
        onDismissRequest = { folderChooser = false }, title = { Text("Mover para pasta") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = { if (perform { store.moveFile(selected.id, null) }) folderChooser = false }) { Text("Sem pasta") }
                folders.forEach { folder ->
                    TextButton(onClick = { if (perform { store.moveFile(selected.id, folder.id) }) folderChooser = false }) { Text(folder.name) }
                }
                if (folders.isEmpty()) Text("Crie uma pasta para organizar este documento.")
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = { folderChooser = false }) { Text("Cancelar") } },
    )
    if (folderDialog) AlertDialog(
        onDismissRequest = { folderDialog = false }, title = { Text(if (editingFolderId > 0L) "Renomear pasta" else "Nova pasta") },
        text = { OutlinedTextField(nameDraft, { if (it.length <= 80) nameDraft = it }, label = { Text("Nome da pasta") }, singleLine = true) },
        confirmButton = { TextButton(enabled = nameDraft.isNotBlank(), onClick = {
            if (perform { if (editingFolderId > 0L) store.renameFolder(editingFolderId, nameDraft) else store.createFolder(nameDraft) }) folderDialog = false
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { folderDialog = false }) { Text("Cancelar") } },
    )
    if (folderDeleteDialog && folderFilter > 0L) AlertDialog(
        onDismissRequest = { folderDeleteDialog = false }, title = { Text("Excluir pasta?") },
        text = { Text("Os documentos serão movidos para Sem pasta. Nenhum documento original será excluído.") },
        confirmButton = { TextButton(onClick = {
            if (perform { store.deleteFolder(folderFilter) }) { folderDeleteDialog = false; folderFilter = -2L }
        }) { Text("Excluir pasta") } },
        dismissButton = { TextButton(onClick = { folderDeleteDialog = false }) { Text("Cancelar") } },
    )
    message?.let { content -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Biblioteca") },
        text = { Text(content) }, confirmButton = { TextButton(onClick = { message = null }) { Text("Entendi") } }) }
}
