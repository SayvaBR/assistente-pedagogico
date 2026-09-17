package com.sayvabr.assistentepedagogico.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftNavigationGuardTest {
    @Test fun cleanDraftNavigatesImmediately() {
        val guard = DraftNavigationGuard().apply { begin("titulo=A|data=2026-09-17") }
        assertEquals(
            NavigationDecision.Navigate(PendingNavigation("planning", "2026-09-18")),
            guard.request("planning", "2026-09-18"),
        )
    }

    @Test fun dirtyDraftBlocksTabOrDateNavigationUntilExplicitDiscard() {
        val guard = DraftNavigationGuard().apply {
            begin("titulo=A|data=2026-09-17")
            update("titulo=Aula alterada|data=2026-09-17")
        }
        assertTrue(guard.isDirty)
        assertEquals(NavigationDecision.ConfirmDiscard, guard.request("planning", "2026-09-18"))
        assertTrue(guard.hasPendingNavigation)
        assertEquals(
            NavigationDecision.Navigate(PendingNavigation("planning", "2026-09-18")),
            guard.discardAndContinue(),
        )
        assertFalse(guard.isDirty)
    }

    @Test fun keepEditingCancelsPendingNavigationWithoutLosingDraft() {
        val guard = DraftNavigationGuard().apply {
            begin("observacao=original")
            update("observacao=rascunho importante")
        }
        assertEquals(NavigationDecision.ConfirmDiscard, guard.request("home"))
        guard.keepEditing()
        assertFalse(guard.hasPendingNavigation)
        assertTrue(guard.isDirty)
        assertEquals(NavigationDecision.Stay, guard.discardAndContinue())
    }

    @Test fun successfulSaveBecomesNewBaseline() {
        val guard = DraftNavigationGuard().apply {
            begin("nome=Ana")
            update("nome=Ana Maria")
            markSaved()
        }
        assertFalse(guard.isDirty)
        assertEquals(
            NavigationDecision.Navigate(PendingNavigation("classes")),
            guard.request("classes"),
        )
    }
}
