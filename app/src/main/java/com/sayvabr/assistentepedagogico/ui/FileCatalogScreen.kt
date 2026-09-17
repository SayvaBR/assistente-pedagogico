package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.FileFolder
import com.sayvabr.assistentepedagogico.data.LibraryFile
import com.sayvabr.assistentepedagogico.data.SavedFile
import com.sayvabr.assistentepedagogico.data.TeacherStore

/**
 * Real offline SAF catalog: organizing never copies/deletes a provider document. The existing
 * callbacks remain compatible with TeacherApp; destructive actions use the v4 store directly so
 * a reversible trip to the trash NEVER releases the persisted SAF permission prematurely.
 * TODO: move these operations into TeacherApp's shared store/commit flow during PR integration.
 */
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
    var folderFilter by rememberSaveable { mutableLongStateOf(-2L) } // -2 all, -1 unfiled, >0 folder
    var selectedId by rememberSaveable { mutableLongStateOf(-1L) }
    var renameOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var purgeOpen by remember { mutableStateOf(false) }
    var folderChooser by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var folderDeleteDialog by remember { mutableStateOf(false) }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val library = remember(files, revision) { store.libraryFiles() }
    val folders = remember(files, revision) { store.fileFolders() }
    val selected = library.firstOrNull { it.id == selectedId }
    val active = library.filter { it.trashedAt == null }
    val matches = library.filter { file ->
        val viewMatches = when (view) {
            "Favoritos" -> file.trashedAt == null && file.favorite
            "Lixeira" -> file.trashedAt != null
            else -> file.trashedAt == null
        }
        val folderMatches = view == "Lixeira" || folderFilter == -2L ||
            (folderFilter == -1L && file.folderId == null) || file.folderId == folderFilter
        viewMatches && folderMatches && file.name.contains(query.trim(), ignoreCase = true)
    }.let { result ->
        if (alphabetical) result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        else result.sortedByDescending { it.id }
    }

    fun perform(action: () -> Unit) {
        runCatching(action).onSuccess { revision++ }.onFailure {
            message = it.message ?: "Não foi possível concluir esta ação. Seus arquivos foram preservados."
        }
    }
    fun original(file: LibraryFile) = SavedFile(
        file.id, file.name, file.uri, file.folderId, file.favorite, file.trashedAt, file.accessState,
    )

    BackHandler(enabled = selected != null && !renameOpen && !trashOpen && !purgeOpen && !folderChooser) {
        selectedId = -1L
    }

    if (selected != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(48.dp).clickable { selectedId = -1L }
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
            Text(selected.name, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
            Spacer(Modifier.height(8.dp))
            val folderName = folders.firstOrNull { it.id == selected.folderId }?.name ?: "Sem pasta"
            Text("Pasta: $folderName", color = ApColors.Navy, fontSize = 13.sp)
            if (selected.accessState == "revoked") {
                Spacer(Modifier.height(9.dp))
                Text("Acesso indisponível. O vínculo foi preservado; importe novamente para conceder acesso.",
                    color = ApColors.Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(13.dp))
            Text("O documento original continua no provedor Android. As ações desta biblioteca alteram só a referência local.",
                color = ApColors.Navy, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(17.dp))
            if (selected.trashedAt == null) {
                ApRaisedButton("Abrir documento", onClick = { openFile(original(selected)) }, glyph = ApGlyphKind.OPEN)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton(if (selected.favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    onClick = { perform { store.setFileFavorite(selected.id, !selected.favorite) } }, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Mover para pasta", onClick = { folderChooser = true }, glyph = ApGlyphKind.FOLDER, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Renomear no catálogo", onClick = {
                    nameDraft = selected.name
                    renameOpen = true
                }, glyph = ApGlyphKind.EDIT, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Verificar acesso", onClick = {
                    perform {
                        val available = runCatching {
                            context.contentResolver.openFileDescriptor(Uri.parse(selected.uri), "r")?.use { } ?: error("Documento indisponível")
                        }.isSuccess
                        store.setFileAccessState(selected.id, if (available) "available" else "revoked")
                        message = if (available) "Acesso ao documento confirmado." else "Acesso indisponível. O vínculo foi preservado."
                    }
                }, secondary = true)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Enviar à lixeira", onClick = { trashOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            } else {
                ApRaisedButton("Restaurar documento", onClick = {
                    perform { store.restoreFile(selected.id) }
                    selectedId = -1L
                    view = "Todos"
                }, glyph = ApGlyphKind.BACK)
                Spacer(Modifier.height(9.dp))
                ApRaisedButton("Excluir vínculo definitivamente", onClick = { purgeOpen = true }, glyph = ApGlyphKind.TRASH, secondary = true)
            }
        }
    } else {
        ApEyebrow("Seu espaço de trabalho")
        Spacer(Modifier.height(3.dp))
        Text("Arquivos", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
        Text("Organize seus materiais sem alterar os originais.", fontSize = 14.sp, color = ApColors.Navy)
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
                Text("Pastas, favoritos e lixeira ficam neste aparelho. Os arquivos continuam no provedor original.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = ApColors.White)
                Spacer(Modifier.height(14.dp))
                ApRaisedButton("Importar documento", onClick = importFile, glyph = ApGlyphKind.IMPORT, secondary = true)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todos", "Favoritos", "Lixeira").forEach { option ->
                FilterChip(
                    selected = view == option, onClick = { view = option; folderFilter = -2L },
                    label = { Text(option, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (view != "Lixeira") {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pastas", fontSize = 19.sp, fontWeight = FontWeight.Black, color = ApColors.Navy, modifier = Modifier.weight(1f))
                TextButton(onClick = { nameDraft = ""; folderDialog = true }) { Text("+ Nova pasta", color = ApColors.Pressed) }
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
                        nameDraft = folders.firstOrNull { it.id == folderFilter }?.name.orEmpty()
                        folderDialog = true
                    }) { Text("Renomear pasta") }
                    TextButton(onClick = { folderDeleteDialog = true }) { Text("Excluir pasta") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar documento") }, singleLine = true,
            leadingIcon = { ApGlyph(ApGlyphKind.SEARCH, Modifier.size(22.dp), ApColors.Pressed) },
            shape = RoundedCornerShape(18.dp),
        )
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
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { selectedId = file.id }
                    .semantics { contentDescription = "Ver detalhes de ${file.name}" },
                color = ApColors.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ApPalette.Outline),
            ) {
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
            if (nameDraft.isNotBlank()) { renameOpen = false; renameFile(original(selected), nameDraft) }
        }, enabled = nameDraft.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Cancelar") } },
    )
    if (trashOpen && selected != null) AlertDialog(
        onDismissRequest = { trashOpen = false }, title = { Text("Enviar à lixeira?") },
        text = { Text("Você pode restaurar o vínculo depois. O documento original e sua permissão de acesso serão preservados.") },
        confirmButton = { TextButton(onClick = {
            trashOpen = false
            perform { store.trashFile(selected.id) }
            selectedId = -1L
            view = "Lixeira"
        }) { Text("Enviar à lixeira") } },
        dismissButton = { TextButton(onClick = { trashOpen = false }) { Text("Cancelar") } },
    )
    if (purgeOpen && selected != null) AlertDialog(
        onDismissRequest = { purgeOpen = false }, title = { Text("Excluir vínculo definitivamente?") },
        text = { Text("Esta ação não poderá ser desfeita no aplicativo. O arquivo original continuará no provedor Android.") },
        confirmButton = { TextButton(onClick = {
            purgeOpen = false
            perform {
                store.permanentlyRemoveFileReference(selected.id)
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(Uri.parse(selected.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            selectedId = -1L
        }) { Text("Excluir definitivamente") } },
        dismissButton = { TextButton(onClick = { purgeOpen = false }) { Text("Cancelar") } },
    )
    if (folderChooser && selected != null) AlertDialog(
        onDismissRequest = { folderChooser = false }, title = { Text("Mover para pasta") },
        text = {
            Column {
                TextButton(onClick = {
                    perform { store.moveFile(selected.id, null) }
                    folderChooser = false
                }) { Text("Sem pasta") }
                folders.forEach { folder ->
                    TextButton(onClick = {
                        perform { store.moveFile(selected.id, folder.id) }
                        folderChooser = false
                    }) { Text(folder.name) }
                }
                if (folders.isEmpty()) Text("Crie uma pasta na biblioteca para organizar este documento.")
            }
        }, confirmButton = {},
        dismissButton = { TextButton(onClick = { folderChooser = false }) { Text("Cancelar") } },
    )
    if (folderDialog) AlertDialog(
        onDismissRequest = { folderDialog = false },
        title = { Text(if (folderFilter > 0L) "Renomear pasta" else "Nova pasta") },
        text = { OutlinedTextField(nameDraft, { if (it.length <= 80) nameDraft = it }, label = { Text("Nome da pasta") }, singleLine = true) },
        confirmButton = { TextButton(enabled = nameDraft.isNotBlank(), onClick = {
            val target = folderFilter
            perform {
                if (target > 0L) store.renameFolder(target, nameDraft)
                else store.createFolder(nameDraft)
            }
            folderDialog = false
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { folderDialog = false }) { Text("Cancelar") } },
    )
    if (folderDeleteDialog && folderFilter > 0L) AlertDialog(
        onDismissRequest = { folderDeleteDialog = false }, title = { Text("Excluir pasta?") },
        text = { Text("Os vínculos serão movidos para Sem pasta. Nenhum arquivo original será excluído.") },
        confirmButton = { TextButton(onClick = {
            val target = folderFilter
            perform { store.deleteFolder(target) }
            folderDeleteDialog = false
            folderFilter = -2L
        }) { Text("Excluir pasta") } },
        dismissButton = { TextButton(onClick = { folderDeleteDialog = false }) { Text("Cancelar") } },
    )
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Biblioteca") },
            text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("Entendi") } })
    }
}
