package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sayvabr.assistentepedagogico.data.TeacherStore

/** Offline catalog. A second short-lived helper is used until the navigation PR shares its store. */
@Composable
fun FileCatalogScreen(
    files: List<SavedFile>,
    importFile: () -> Unit,
    openFile: (SavedFile) -> Unit,
    renameFile: (SavedFile, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") removeFile: (SavedFile) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { TeacherStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    var revision by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var alphabetical by rememberSaveable { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf("Todos") }
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

    val catalog = remember(files, revision) { store.libraryFiles() }
    val folders = remember(files, revision) { store.fileFolders() }
    val selected = catalog.firstOrNull { it.id == selectedId }
    val active = catalog.filter { it.trashedAt == null }
    val matches = catalog.filter { file ->
        val visible = when (view) {
            "Favoritos" -> file.trashedAt == null && file.favorite
            "Lixeira" -> file.trashedAt != null
            else -> file.trashedAt == null
        }
        val folderMatches = view == "Lixeira" || folderFilter == -2L ||
            (folderFilter == -1L && file.folderId == null) || file.folderId == folderFilter
        visible && folderMatches && file.name.contains(query.trim(), ignoreCase = true)
    }.let { found ->
        if (alphabetical) found.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        else found.sortedByDescending { it.id }
    }

    fun perform(action: () -> Unit): Boolean = runCatching(action).fold(
        onSuccess = { revision++; true },
        onFailure = { message = it.message ?: "Não foi possível concluir. Os vínculos foram preservados."; false },
    )
    fun saved(file: LibraryFile) = SavedFile(
        file.id, file.name, file.uri, file.folderId, file.favorite, file.trashedAt, file.accessState,
    )

    BackHandler(enabled = selected != null && !renameOpen && !trashOpen && !purgeOpen && !folderChooser) { selectedId = -1L }
    if (selected != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp).clickable { selectedId = -1L }
                    .semantics { contentDescription = "Voltar à biblioteca" },
                shape = RoundedCornerShape(15.dp), color = ApColors.White,
                border = BorderStroke(1.dp, ApPalette.Outline),
            ) { Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, color = ApColors.Primary) } }
            Spacer(Modifier.width(12.dp))
            Column {
                ApEyebrow("Biblioteca / documento")
                Text("Detalhes", color = ApColors.Navy, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(17.dp))
        ApCard {
            Box(Modifier.size(65.dp).background(ApColors.Sky, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(38.dp), ApColors.Pressed)
            }
            Spacer(Modifier.height(12.dp))
            Text(selected.name, color = ApColors.Navy, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Pasta: ${folders.firstOrNull { it.id == selected.folderId }?.name ?: "Sem pasta"}",
                color = ApColors.Navy, fontSize = 13.sp)
            if (selected.accessState == "revoked") {
                Spacer(Modifier.height(10.dp))
                Text("Acesso indisponível. O vínculo foi preservado; importe novamente para conceder acesso.",
                    color = ApColors.Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text("Este aplicativo organiza referências locais. O documento original continua no provedor Android.",
                color = ApColors.Navy, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(17.dp))
            if (selected.trashedAt == null) {
                ApRaisedButton("Abrir documento", onClick = { openFile(saved(selected)) }, glyph = ApGlyphKind.OPEN)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton(if (selected.favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    onClick = { perform { store.setFileFavorite(selected.id, !selected.favorite) } }, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Mover para pasta", onClick = { folderChooser = true }, glyph = ApGlyphKind.FOLDER, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Renomear no catálogo", onClick = { nameDraft = selected.name; renameOpen = true },
                    glyph = ApGlyphKind.EDIT, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Verificar acesso", onClick = {
                    perform {
                        val accessible = runCatching {
                            context.contentResolver.openFileDescriptor(Uri.parse(selected.uri), "r")?.use { } ?: error("Documento indisponível")
                        }.isSuccess
                        store.setFileAccessState(selected.id, if (accessible) "available" else "revoked")
                        message = if (accessible) "Acesso ao documento confirmado." else "Acesso indisponível; seu vínculo foi preservado."
                    }
                }, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Enviar à lixeira", onClick = { trashOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            } else {
                ApRaisedButton("Restaurar documento", onClick = {
                    if (perform { store.restoreFile(selected.id) }) { selectedId = -1L; view = "Todos" }
                }, glyph = ApGlyphKind.BACK)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Excluir vínculo definitivamente", onClick = { purgeOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            }
        }
    } else {
        ApEyebrow("Seu espaço de trabalho")
        Spacer(Modifier.height(3.dp))
        Text("Arquivos", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
        Text("Organize materiais sem alterar os originais.", color = ApColors.Navy, fontSize = 14.sp)
        Spacer(Modifier.height(17.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = ApColors.Primary) {
            Column(Modifier.padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(ApColors.White, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                        ApGlyph(ApGlyphKind.FOLDER, Modifier.size(31.dp), ApColors.Pressed)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SUA BIBLIOTECA", fontSize = 11.sp, color = ApColors.White, fontWeight = FontWeight.Black)
                        Text("${active.size} ${if (active.size == 1) "documento" else "documentos"}",
                            fontSize = 20.sp, fontWeight = FontWeight.Black, color = ApColors.White)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Text("Pastas, favoritos e lixeira ficam no seu aparelho. Os originais continuam no provedor.",
                    color = ApColors.White, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(14.dp))
                ApRaisedButton("Importar documento", onClick = importFile, glyph = ApGlyphKind.IMPORT, secondary = true)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todos", "Favoritos", "Lixeira").forEach { option ->
                FilterChip(selected = view == option, onClick = { view = option; folderFilter = -2L },
                    label = { Text(option, fontSize = 12.sp, fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
            }
        }
        if (view != "Lixeira") {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pastas", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { editingFolderId = -1L; nameDraft = ""; folderDialog = true }) {
                    Text("+ Nova pasta", color = ApColors.Pressed)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = folderFilter == -2L, onClick = { folderFilter = -2L }, label = { Text("Todas") })
                FilterChip(selected = folderFilter == -1L, onClick = { folderFilter = -1L }, label = { Text("Sem pasta") })
                folders.forEach { folder ->
                    FilterChip(selected = folderFilter == folder.id, onClick = { folderFilter = folder.id }, label = { Text(folder.name) })
                }
            }
            if (folderFilter > 0L) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        editingFolderId = folderFilter
                        nameDraft = folders.firstOrNull { it.id == folderFilter }?.name.orEmpty()
                        folderDialog = true
                    }) { Text("Renomear pasta") }
                    TextButton(onClick = { folderDeleteDialog = true }) { Text("Excluir pasta") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar documento") }, singleLine = true,
            leadingIcon = { ApGlyph(ApGlyphKind.SEARCH, Modifier.size(22.dp), ApColors.Pressed) },
            shape = RoundedCornerShape(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${matches.size} ${if (matches.size == 1) "resultado" else "resultados"}", color = ApColors.Navy, fontSize = 13.sp)
            TextButton(onClick = { alphabetical = !alphabetical }) {
                Text(if (alphabetical) "Ordem: A–Z" else "Ordem: recentes", color = ApColors.Pressed, fontSize = 12.sp)
            }
        }
        if (matches.isEmpty()) ApCard {
            ApGlyph(if (query.isBlank()) ApGlyphKind.FOLDER else ApGlyphKind.SEARCH, Modifier.size(40.dp), ApColors.Pressed)
            Spacer(Modifier.height(8.dp))
            Text(when {
                query.isNotBlank() -> "Nenhum resultado para sua busca."
                view == "Lixeira" -> "A lixeira está vazia."
                view == "Favoritos" -> "Seus favoritos aparecerão aqui."
                else -> "Ainda não há documentos nesta seleção."
            }, fontWeight = FontWeight.ExtraBold, color = ApColors.Navy)
            if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Limpar pesquisa") }
        }
        matches.forEach { file ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { selectedId = file.id }
                    .semantics { contentDescription = "Ver detalhes de ${file.name}" },
                color = ApColors.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ApPalette.Outline)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(51.dp).background(ApColors.Sky, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(29.dp), ApColors.Pressed)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, color = ApColors.Navy,
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(when {
                            file.trashedAt != null -> "Na lixeira · toque para restaurar"
                            file.accessState == "revoked" -> "Acesso indisponível · vínculo preservado"
                            file.favorite -> "Favorito · toque para organizar"
                            else -> folders.firstOrNull { it.id == file.folderId }?.name ?: "Sem pasta"
                        }, color = ApColors.Navy, fontSize = 12.sp)
                    }
                    Text("›", color = ApColors.Pressed, fontSize = 25.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Excluir um vínculo daqui nunca apaga o documento original.", color = ApColors.Navy, fontSize = 12.sp)
    }

    if (renameOpen && selected != null) AlertDialog(
        onDismissRequest = { renameOpen = false }, title = { Text("Renomear no catálogo") },
        text = { OutlinedTextField(nameDraft, { if (it.length <= 180) nameDraft = it }, label = { Text("Nome exibido") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            if (nameDraft.isNotBlank()) { renameOpen = false; renameFile(saved(selected), nameDraft) }
        }, enabled = nameDraft.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Cancelar") } },
    )
    if (trashOpen && selected != null) AlertDialog(
        onDismissRequest = { trashOpen = false }, title = { Text("Enviar à lixeira?") },
        text = { Text("Você poderá restaurar o vínculo depois. O documento e sua permissão de acesso serão preservados.") },
        confirmButton = { TextButton(onClick = {
            trashOpen = false
            if (perform { store.trashFile(selected.id) }) { selectedId = -1L; view = "Lixeira" }
        }) { Text("Enviar à lixeira") } },
        dismissButton = { TextButton(onClick = { trashOpen = false }) { Text("Cancelar") } },
    )
    if (purgeOpen && selected != null) AlertDialog(
        onDismissRequest = { purgeOpen = false }, title = { Text("Excluir vínculo definitivamente?") },
        text = { Text("Não poderá ser desfeito no aplicativo. O arquivo original continuará no provedor Android.") },
        confirmButton = { TextButton(onClick = {
            purgeOpen = false
            if (perform {
                store.permanentlyRemoveFileReference(selected.id)
                runCatching { context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(selected.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }) selectedId = -1L
        }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { purgeOpen = false }) { Text("Cancelar") } },
    )
    if (folderChooser && selected != null) AlertDialog(
        onDismissRequest = { folderChooser = false }, title = { Text("Mover para pasta") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = {
                    if (perform { store.moveFile(selected.id, null) }) folderChooser = false
                }) { Text("Sem pasta") }
                folders.forEach { folder ->
                    TextButton(onClick = {
                        if (perform { store.moveFile(selected.id, folder.id) }) folderChooser = false
                    }) { Text(folder.name) }
                }
                if (folders.isEmpty()) Text("Crie uma pasta na biblioteca para organizar este documento.")
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = { folderChooser = false }) { Text("Cancelar") } },
    )
    if (folderDialog) AlertDialog(
        onDismissRequest = { folderDialog = false },
        title = { Text(if (editingFolderId > 0L) "Renomear pasta" else "Nova pasta") },
        text = { OutlinedTextField(nameDraft, { if (it.length <= 80) nameDraft = it }, label = { Text("Nome da pasta") }, singleLine = true) },
        confirmButton = { TextButton(enabled = nameDraft.isNotBlank(), onClick = {
            val id = editingFolderId
            if (perform { if (id > 0L) store.renameFolder(id, nameDraft) else store.createFolder(nameDraft) }) folderDialog = false
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { folderDialog = false }) { Text("Cancelar") } },
    )
    if (folderDeleteDialog && folderFilter > 0L) AlertDialog(
        onDismissRequest = { folderDeleteDialog = false }, title = { Text("Excluir pasta?") },
        text = { Text("Os vínculos serão movidos para Sem pasta; nenhum documento original será excluído.") },
        confirmButton = { TextButton(onClick = {
            val target = folderFilter
            if (perform { store.deleteFolder(target) }) { folderDeleteDialog = false; folderFilter = -2L }
        }) { Text("Excluir pasta") } },
        dismissButton = { TextButton(onClick = { folderDeleteDialog = false }) { Text("Cancelar") } },
    )
    message?.let { text -> AlertDialog(onDismissRequest = { message = null },
        title = { Text("Biblioteca") }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("Entendi") } }) }
}
