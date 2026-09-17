package com.sayvabr.assistentepedagogico.ui

/**
 * Pure navigation policy for editor screens.
 *
 * Keeps Android Back, bottom-tab changes and date changes from silently discarding
 * unsaved form state. The UI owns the confirmation dialog; this policy only decides
 * whether navigation may happen immediately or must be confirmed first.
 */
internal class DraftNavigationGuard {
    private var baseline: String = ""
    private var current: String = ""
    private var pending: PendingNavigation? = null

    val isDirty: Boolean get() = current != baseline
    val hasPendingNavigation: Boolean get() = pending != null

    fun begin(initialState: String) {
        baseline = initialState
        current = initialState
        pending = null
    }

    fun update(state: String) {
        current = state
    }

    fun markSaved(state: String = current) {
        baseline = state
        current = state
        pending = null
    }

    fun request(destination: String, day: String? = null): NavigationDecision {
        val target = PendingNavigation(destination, day)
        return if (isDirty) {
            pending = target
            NavigationDecision.ConfirmDiscard
        } else {
            pending = null
            NavigationDecision.Navigate(target)
        }
    }

    fun keepEditing() {
        pending = null
    }

    fun discardAndContinue(): NavigationDecision {
        val target = pending ?: return NavigationDecision.Stay
        current = baseline
        pending = null
        return NavigationDecision.Navigate(target)
    }
}

internal data class PendingNavigation(val destination: String, val day: String? = null)

internal sealed interface NavigationDecision {
    data object Stay : NavigationDecision
    data object ConfirmDiscard : NavigationDecision
    data class Navigate(val target: PendingNavigation) : NavigationDecision
}
