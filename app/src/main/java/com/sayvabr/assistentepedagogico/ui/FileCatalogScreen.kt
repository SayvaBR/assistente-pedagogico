package com.sayvabr.assistentepedagogico.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.SavedFile

/** Local references to SAF documents, not a private copy or a cloud library. */
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
    // File details is an actual nested route inside this tab, not a dead tile.
    BackHandler(enabled = selected != null && !renaming && !removing) { selectedId = -1L }
    if (selected != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { selectedId = -1L }) { Text("Voltar", color = ApColors.Primary) }
            Spacer(Modifier.width(8.dp))
            Text("Detalhes do arquivo", color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 21.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(19.dp), color = ApColors.White,
            border = BorderStroke(1.dp, ApColors.Primary.copy(alpha = .15f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(selected.name, color = ApColors.Navy, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text("Documento vinculado ao aparelho", color = ApColors.Navy, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("O aplicativo guarda uma referência ao documento original; não cria uma cópia nem envia à nuvem.",
                    color = ApColors.Navy, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { openFile(selected) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ApColors.Primary)) { Text("Abrir documento") }
                Spacer(Modifier.height(9.dp))
                OutlinedButton(onClick = { draftName = selected.name; renaming = true }, modifier = Modifier.fillMaxWidth()) { Text("Renomear no catálogo") }
                OutlinedButton(onClick = { removing = true }, modifier = Modifier.fillMaxWidth()) { Text("Remover do aplicativo") }
            }
        }
    } else {
        Text("Arquivos", fontSize = 27.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
        Text("Seus documentos vinculados em um só lugar", fontSize = 14.sp, color = ApColors.Navy)
        Spacer(Modifier.height(17.dp))
        Button(onClick = importFile, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ApColors.Primary),
            shape = RoundedCornerShape(18.dp)) { Text("Importar documento", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar arquivos por nome") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { alphabetical = !alphabetical }) {
            Text(if (alphabetical) "Ordem: nome (A–Z) • mudar" else "Ordem: recentes • mudar", color = ApColors.Navy)
        }
        Spacer(Modifier.height(8.dp))
        val matches = files.filter { it.name.contains(query.trim(), ignoreCase = true) }
            .let { if (alphabetical) it.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { file -> file.name }) else it.sortedByDescending { file -> file.id } }
        if (matches.isEmpty()) {
            Surface(shape = RoundedCornerShape(19.dp), color = ApColors.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(if (query.isBlank()) "Nenhum documento importado" else "Nenhum resultado para sua busca", fontWeight = FontWeight.Black, color = ApColors.Navy)
                    Spacer(Modifier.height(7.dp))
                    Text(if (query.isBlank()) "Importe um arquivo do aparelho para começar." else "Tente outra palavra ou limpe a pesquisa.", color = ApColors.Navy)
                }
            }
        }
        matches.forEach { file ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { selectedId = file.id }, color = ApColors.White,
                shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, ApColors.Primary.copy(alpha = .15f))) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = ApColors.Sky) {
                        Text("DOC", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold, color = ApColors.Navy)
                        Text("Abrir detalhes e ações", fontSize = 12.sp, color = ApColors.Navy)
                    }
                    Text("Ver", color = ApColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Os documentos originais continuam no aparelho, mesmo se você remover um item daqui.", fontSize = 12.sp, color = ApColors.Navy)
    }
    if (renaming && selected != null) AlertDialog(
        onDismissRequest = { renaming = false },
        title = { Text("Renomear no catálogo") },
        text = { Column {
            OutlinedTextField(value = draftName, onValueChange = { if (it.length <= 180) draftName = it }, label = { Text("Nome exibido") }, singleLine = true)
            Text("Isso não renomeia o arquivo original no seu aparelho.", fontSize = 12.sp)
        } },
        confirmButton = { TextButton(onClick = { if (draftName.isNotBlank()) { renaming = false; renameFile(selected, draftName) } }, enabled = draftName.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancelar") } },
    )
    if (removing && selected != null) AlertDialog(
        onDismissRequest = { removing = false },
        title = { Text("Remover do aplicativo?") },
        text = { Text("Somente a referência será removida do catálogo. O arquivo original NÃO será apagado. Para adicioná-lo novamente, use Importar documento.") },
        confirmButton = { TextButton(onClick = { removing = false; selectedId = -1L; removeFile(selected) }) { Text("Remover referência") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancelar") } },
    )
}
