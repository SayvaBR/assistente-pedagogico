package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.PlanBlock
import com.sayvabr.assistentepedagogico.data.PlanBlockKind
import com.sayvabr.assistentepedagogico.data.PlanComposition
import com.sayvabr.assistentepedagogico.data.PlanTemplate
import java.util.UUID

/** A genuinely ordered editor: each canonical field is rendered in its chosen section position. */
@Composable
fun PlanComposerPanel(
    layout: PlanComposition,
    enabled: Boolean,
    templates: List<Pair<Long, PlanTemplate>>,
    onChange: (PlanComposition) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onRemoveTemplate: (Long) -> Unit,
    sectionContent: @Composable (PlanBlock) -> Unit,
) {
    var addMenu by remember { mutableStateOf(false) }
    var templateName by rememberSaveable { mutableStateOf("") }
    var pendingTemplate by remember { mutableStateOf<Pair<Long, PlanTemplate>?>(null) }
    var pendingRemoval by remember { mutableStateOf<Long?>(null) }
    val mandatory = setOf(PlanBlockKind.IDENTIFICATION, PlanBlockKind.OBJECTIVES,
        PlanBlockKind.CONTENT, PlanBlockKind.METHODOLOGY)
    val addable = PlanBlockKind.entries.filter { kind ->
        kind != PlanBlockKind.IDENTIFICATION && kind != PlanBlockKind.CUSTOM &&
            kind != PlanBlockKind.RESOURCES && layout.blocks.none { it.kind == kind }
    }

    ApCard {
        ApEyebrow("Montar meu plano", onPrimary = false)
        Text("Organize as seções, escreva nelas e salve tudo junto com o plano. Os campos essenciais continuam obrigatórios.",
            color = ApColors.Navy)
        Spacer(Modifier.height(8.dp))
        Box {
            ApRaisedButton("Adicionar seção", enabled = enabled && layout.blocks.size < 40,
                glyph = ApGlyphKind.PLUS, onClick = { addMenu = true })
            DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                DropdownMenuItem(text = { Text("Seção personalizada") }, onClick = {
                    addMenu = false
                    onChange(layout.add(PlanBlock("custom_${UUID.randomUUID().toString().replace("-", "")}",
                        PlanBlockKind.CUSTOM, "Nova seção")))
                })
                addable.forEach { kind ->
                    DropdownMenuItem(text = { Text(kind.defaultTitle) }, onClick = {
                        addMenu = false
                        onChange(layout.add(PlanBlock(kind.name.lowercase(), kind)))
                    })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(templateName, { templateName = it.take(80) },
            label = { Text("Nome do modelo da escola") }, enabled = enabled,
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        ApRaisedButton("Salvar estrutura como modelo", onClick = { onSaveTemplate(templateName) },
            enabled = enabled && templateName.trim().length >= 2,
            glyph = ApGlyphKind.CHECK, secondary = true)
        if (templates.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Text("Meus modelos", fontWeight = FontWeight.Bold, color = ApColors.Navy)
            templates.forEach { template ->
                Text(template.second.name, color = ApColors.Navy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { pendingTemplate = template }, enabled = enabled) { Text("Aplicar") }
                    TextButton(onClick = { pendingRemoval = template.first }, enabled = enabled) { Text("Excluir modelo") }
                }
            }
        }
        Text("Modelos guardam somente a estrutura, nunca o texto pessoal de uma aula. Remover uma seção opcional a oculta do documento, sem apagar os campos originais da aula.",
            color = ApColors.Navy)
    }

    layout.blocks.forEachIndexed { index, block ->
        Spacer(Modifier.height(12.dp))
        ApCard {
            Text("Seção ${index + 1} de ${layout.blocks.size}", color = ApColors.Pressed,
                fontWeight = FontWeight.Bold)
            OutlinedTextField(block.title, { title ->
                if (enabled && title.length <= 100 && title.trim().length >= 2) onChange(layout.update(block.copy(title = title)))
            }, label = { Text("Título da seção") }, enabled = enabled,
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { onChange(layout.move(block.id, -1)) },
                    enabled = enabled && index > 0) { Text("Subir") }
                TextButton(onClick = { onChange(layout.move(block.id, 1)) },
                    enabled = enabled && index < layout.blocks.lastIndex) { Text("Descer") }
                TextButton(onClick = { onChange(layout.remove(block.id)) },
                    enabled = enabled && block.kind !in mandatory) { Text("Remover") }
            }
            if (block.kind == PlanBlockKind.CUSTOM) {
                OutlinedTextField(block.body, { value ->
                    if (enabled && value.length <= 20_000) onChange(layout.update(block.copy(body = value)))
                }, label = { Text("Texto da seção personalizada") }, enabled = enabled,
                    minLines = 3, modifier = Modifier.fillMaxWidth())
            } else {
                sectionContent(block)
            }
        }
    }

    pendingTemplate?.let { selected ->
        AlertDialog(onDismissRequest = { pendingTemplate = null },
            title = { Text("Aplicar modelo ${selected.second.name}?") },
            text = { Text("A estrutura atual e o texto de seções personalizadas serão substituídos. O título, objetivo, conteúdo e demais campos da aula permanecem armazenados.") },
            confirmButton = { TextButton(onClick = {
                pendingTemplate = null
                onChange(selected.second.instantiate())
            }) { Text("Aplicar modelo") } },
            dismissButton = { TextButton(onClick = { pendingTemplate = null }) { Text("Cancelar") } })
    }
    pendingRemoval?.let { id ->
        AlertDialog(onDismissRequest = { pendingRemoval = null },
            title = { Text("Excluir este modelo?") },
            text = { Text("Planos que já usam este modelo não serão modificados.") },
            confirmButton = { TextButton(onClick = { pendingRemoval = null; onRemoveTemplate(id) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") } })
    }
}
