package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.PlanBlockKind
import com.sayvabr.assistentepedagogico.data.PlanComposition
import com.sayvabr.assistentepedagogico.data.PlanLayoutV9
import com.sayvabr.assistentepedagogico.data.PlanTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI behavior with invented text only; does not open or delete a real teacher database. */
@RunWith(AndroidJUnit4::class)
class PlanComposerPanelInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun teacherCanWriteMoveAndSavePrivacySafeTemplate() {
        val current = mutableStateOf(PlanComposition.standard())
        var saved: PlanTemplate? = null
        compose.setContent {
            ApTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlanComposerPanel(
                        layout = current.value,
                        enabled = true,
                        templates = emptyList(),
                        onChange = { current.value = it },
                        onSaveTemplate = { saved = current.value.saveAsTemplate(it) },
                        onRemoveTemplate = {},
                        sectionContent = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Adicionar seção").performClick()
        compose.onNodeWithText("Seção personalizada").performClick()
        compose.onNodeWithText("Seção 12 de 12").assertExists()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Conteúdo fictício que não pode ir ao modelo")
        compose.runOnIdle {
            assertEquals(PlanBlockKind.CUSTOM, current.value.blocks.last().kind)
            assertEquals("Conteúdo fictício que não pode ir ao modelo", current.value.blocks.last().body)
        }
        compose.onAllNodesWithText("Subir").onLast().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(PlanBlockKind.CUSTOM, current.value.blocks[current.value.blocks.lastIndex - 1].kind) }
        compose.onNodeWithText("Nome do modelo da escola").performScrollTo().performTextInput("Modelo fictício")
        compose.onNodeWithText("Salvar estrutura como modelo").performScrollTo().performClick()
        compose.runOnIdle {
            val template = requireNotNull(saved)
            assertEquals("Modelo fictício", template.name)
            assertTrue(template.blocks.all { it.body.isEmpty() && it.minutes == null })
            assertEquals(current.value.blocks.map { it.id }, template.blocks.map { it.id })
            assertFalse(current.value.blocks.all { it.body.isEmpty() })
        }
    }

    @Test fun unsavedCustomSectionSurvivesAndroidStateRestoration() {
        var observed: PlanComposition? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var payload by rememberSaveable { mutableStateOf(PlanLayoutV9.encode(PlanComposition.standard())) }
            val layout = PlanLayoutV9.decode(payload)
            SideEffect { observed = layout }
            ApTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlanComposerPanel(
                        layout = layout,
                        enabled = true,
                        templates = emptyList(),
                        onChange = { payload = PlanLayoutV9.encode(it) },
                        onSaveTemplate = {},
                        onRemoveTemplate = {},
                        sectionContent = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Adicionar seção").performClick()
        compose.onNodeWithText("Seção personalizada").performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Rascunho sintético não salvo")
        compose.runOnIdle { assertEquals("Rascunho sintético não salvo", observed?.blocks?.last()?.body) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals("Rascunho sintético não salvo", observed?.blocks?.last()?.body)
            assertEquals(12, observed?.blocks?.size)
        }
    }
}
