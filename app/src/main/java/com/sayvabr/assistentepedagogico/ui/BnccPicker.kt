package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sayvabr.assistentepedagogico.data.BnccCatalog

/** Offline picker. This independent catalogue is attributed, not an official MEC service. */
@Composable
fun BnccPicker(
    catalog: BnccCatalog,
    initialCodes: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableStateOf("TODAS") }
    var year by rememberSaveable { mutableStateOf("") }
    // A plain String can be saved by Android's registry. The previous remember-only
    // SnapshotStateList lost the teacher's unconfirmed choices on configuration changes.
    var selectedRaw by rememberSaveable(initialCodes.joinToString("|")) {
        mutableStateOf(initialCodes.distinct().joinToString(","))
    }
    val selected = BnccCatalog.parseCodes(selectedRaw)
    fun select(code: String) {
        selectedRaw = (if (code in selected) selected.filterNot { it == code } else selected + code)
            .joinToString(",")
    }
    val matches = remember(catalog, query, stage, year) {
        catalog.search(
            query = query,
            stage = stage.takeIf { it in setOf("EI", "EF", "EM") },
            // A year chosen for Fundamental must not leak into Infantil/Computação/Médio.
            year = year.toIntOrNull().takeIf { stage == "EF" },
            supplementOnly = stage == "CO",
            limit = 100,
        )
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Selecionar habilidades BNCC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${catalog.entries.size} aprendizagens disponíveis offline · ${catalog.version}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar código, habilidade ou tema") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(listOf("TODAS" to "Todas", "EI" to "Infantil", "EF" to "Fundamental", "EM" to "Médio", "CO" to "Computação")) { option ->
                        FilterChip(selected = stage == option.first, onClick = {
                            if (stage != option.first) { stage = option.first; year = "" }
                        }, label = { Text(option.second) })
                    }
                }
                if (stage == "EF") {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(1) },
                        label = { Text("Ano do Fundamental (1–9, opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                Text("${selected.size} selecionada(s) · ${matches.size} resultado(s) exibidos", style = MaterialTheme.typography.labelMedium)
                val unavailable = selected.filter { catalog.find(it) == null }
                if (unavailable.isNotEmpty()) {
                    Text("Códigos anteriores não encontrados: remova-os ou substitua-os por habilidades cadastradas.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(unavailable) { code ->
                            TextButton(onClick = { select(code) }) { Text("Remover $code") }
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(matches, key = { it.code }) { entry ->
                        val checked = entry.code in selected
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { select(entry.code) }
                                .padding(vertical = 8.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.code + " · " + entry.component, fontWeight = FontWeight.Bold, color = ApColors.Navy)
                                Text(entry.text, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                val context = listOfNotNull(
                                    entry.ageGroup.takeIf(String::isNotBlank),
                                    entry.years.takeIf(List<Int>::isNotEmpty)?.joinToString(", ") { "${it}º ano" },
                                    entry.sourcePage.takeIf(String::isNotBlank),
                                ).joinToString(" · ")
                                if (context.isNotBlank()) Text(context, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                if (matches.size == 100) Text("Mostrando os primeiros 100 resultados. Refine a busca para encontrar outros.", style = MaterialTheme.typography.labelSmall)
                Text("Fonte independente: ${catalog.attribution} · CC BY 4.0 · referência para planejar, sem vínculo ou homologação pelo MEC. Consulte os documentos oficiais quando precisar de conferência normativa.", style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = unavailable.isEmpty(), onClick = { onConfirm(selected.sorted()) }) {
                        Text("Usar selecionadas")
                    }
                }
            }
        }
    }
}
