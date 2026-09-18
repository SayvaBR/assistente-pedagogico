package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.*
import org.junit.Test

class PlanCompositionTest {
    private fun custom(id: String, body: String = "") = PlanBlock(
        id, PlanBlockKind.CUSTOM, "Seção da escola", body
    )

    @Test fun orderCanChangeWithoutChangingIdentityOrLessonContent() {
        val original = PlanComposition.standard()
            .add(custom("school-guidance", "Texto criado pelo professor"), afterId = "objectives")
        val moved = original.move("school-guidance", 1).move("school-guidance", 1)
        assertEquals("school-guidance", original.blocks[2].id)
        assertEquals("school-guidance", moved.blocks[4].id)
        assertEquals("Texto criado pelo professor", moved.blocks[4].body)
        assertEquals(PlanBlockKind.IDENTIFICATION, moved.blocks.first().kind)
        assertEquals(original.blocks.map { it.id }.toSet(), moved.blocks.map { it.id }.toSet())
        assertSame(original, original.move("identification", -1))
    }

    @Test fun identificationCanMoveButCannotBeRemovedOrDuplicated() {
        val moved = PlanComposition.standard().move("identification", 1)
        assertEquals(PlanBlockKind.IDENTIFICATION, moved.blocks[1].kind)
        assertThrows(IllegalArgumentException::class.java) { moved.remove("identification") }
        assertThrows(IllegalArgumentException::class.java) {
            moved.add(PlanBlock("extra-identity", PlanBlockKind.IDENTIFICATION))
        }
        assertThrows(IllegalArgumentException::class.java) {
            moved.add(PlanBlock("objectives", PlanBlockKind.CUSTOM))
        }
    }

    @Test fun customSectionsCanBeAddedRenamedAndRemovedWithoutChangingBuiltInFields() {
        val original = PlanComposition.standard()
        val extended = original.add(custom("school-1"), "methodology")
            .add(custom("school-2", "Conteúdo livre"), "school-1")
            .update(custom("school-1").copy(title = "Intervenção pedagógica", body = "Adaptação local"))
        assertEquals("Intervenção pedagógica", extended.blocks.first { it.id == "school-1" }.title)
        assertEquals("Conteúdo livre", extended.blocks.first { it.id == "school-2" }.body)
        assertEquals(original.blocks, extended.remove("school-1").remove("school-2").blocks)
        assertThrows(IllegalArgumentException::class.java) {
            extended.update(PlanBlock("school-1", PlanBlockKind.BNCC))
        }
    }

    @Test fun reusableModelNeverCopiesContentOrTimeFromAnExistingLessonByDefault() {
        val composition = PlanComposition.standard()
            .update(PlanBlock("opening", PlanBlockKind.OPENING, body = "Atividade com a turma", minutes = 10))
            .add(custom("school-1", "Observação pessoal"), "opening")
        val template = composition.saveAsTemplate("Modelo da minha escola")
        assertEquals("Modelo da minha escola", template.name)
        assertTrue(template.blocks.all { it.body.isEmpty() && it.minutes == null })
        val fresh = template.instantiate()
        assertEquals(composition.blocks.map { it.id }, fresh.blocks.map { it.id })
        assertEquals(composition.blocks.map { it.kind }, fresh.blocks.map { it.kind })
        assertTrue(fresh.blocks.all { it.body.isEmpty() && it.minutes == null })
        assertEquals("Observação pessoal", composition.blocks.first { it.id == "school-1" }.body)
    }

    @Test fun invalidOrDangerousModelsFailRatherThanSilentlyCorruptingLayout() {
        val base = PlanComposition.standard()
        assertThrows(IllegalArgumentException::class.java) { base.add(custom("bad id")) }
        assertThrows(IllegalArgumentException::class.java) { base.move("bncc", 4) }
        assertThrows(IllegalArgumentException::class.java) { base.remove("absent") }
        assertThrows(IllegalArgumentException::class.java) {
            base.add(PlanBlock("bncc-other", PlanBlockKind.BNCC))
        }
        assertThrows(IllegalArgumentException::class.java) {
            base.add(custom("school-1").copy(minutes = 15))
        }
        assertThrows(IllegalArgumentException::class.java) { base.saveAsTemplate(" ") }
        assertEquals(PlanBlockKind.IDENTIFICATION, base.blocks.first().kind)
    }
}
